pipeline {
  agent any
  options { timestamps() }

  environment {
    AWS_REGION = 'ap-northeast-2'
    ECR_REPO   = 'refit-spring'
    SSM_PARAM  = '/refit/backend/image_uri'
    ASG_NAME   = 'asg-spring-dev'
    APP_DIR    = '.'
  }

  stages {
    stage('Checkout'){
      steps{
        checkout scm
        sh '''
          set -e
          git config --local core.hooksPath .git/hooks || true
          git lfs install --local || true
          git lfs fetch --all || true
          git lfs checkout || true

          test -f gradlew || { echo "[ERROR] gradlew missing"; exit 1; }
          test -f gradle/wrapper/gradle-wrapper.properties || { echo "[ERROR] wrapper properties missing"; exit 1; }
          test -f gradle/wrapper/gradle-wrapper.jar || { echo "[ERROR] wrapper jar missing"; exit 1; }

          chmod +x gradlew || true
        '''
      }
    }

    stage('Prepare Vars') {
      steps {
        script {
          env.TS         = sh(script: "date +%Y%m%d%H%M%S", returnStdout: true).trim()
          env.GIT_SHA    = sh(script: "git rev-parse --short HEAD", returnStdout: true).trim()
          env.ACCOUNT_ID = sh(script: "aws sts get-caller-identity --query Account --output text --region ${env.AWS_REGION}", returnStdout: true).trim()
          env.ECR_URI    = "${env.ACCOUNT_ID}.dkr.ecr.${env.AWS_REGION}.amazonaws.com/${env.ECR_REPO}"
          env.IMAGE_TAG  = "${env.TS}-${env.GIT_SHA}"
          env.IMAGE_URI  = "${env.ECR_URI}:${env.IMAGE_TAG}"
        }
      }
    }

    stage('Build JAR (JDK 21)'){
      steps {
        dir("${APP_DIR}") {
          sh '''
            set -e
            rm -rf .gradle .gradle-ci .tmp || true
            mkdir -p .gradle-ci .tmp

            export HOME="$PWD"
            export GRADLE_USER_HOME="$PWD/.gradle-ci"
            export JAVA_TOOL_OPTIONS="-Djava.io.tmpdir=$PWD/.tmp"
            chmod +x gradlew || true

            ./gradlew --no-daemon \
              --project-cache-dir .gradle-ci \
              -Dorg.gradle.jvmargs="-Xmx512m" \
              clean bootJar -x test
          '''
        }
      }
    }

    stage('Docker Login & Build & Push'){
      steps {
        sh """
          set -e
          aws ecr describe-repositories --repository-names ${ECR_REPO} --region ${AWS_REGION} >/dev/null 2>&1 \
          || aws ecr create-repository --repository-name ${ECR_REPO} --region ${AWS_REGION}

          aws ecr get-login-password --region ${AWS_REGION} \
          | docker login --username AWS --password-stdin ${ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com

          export DOCKER_BUILDKIT=1
          docker build --memory=1.5g -t ${IMAGE_URI} -f ${APP_DIR}/Dockerfile ${APP_DIR}
          docker push ${IMAGE_URI}
        """
      }
    }

    stage('Update SSM (image_uri)'){
      steps {
        sh """
          set -e
          aws ssm put-parameter --region ${AWS_REGION} \
            --name "${SSM_PARAM}" --type "String" \
            --value "${IMAGE_URI}" --overwrite >/dev/null
        """
      }
    }

    stage('Deploy') {
      steps {
        sh '''
          set -euo pipefail

          AWS_REGION="${AWS_REGION}"
          ASG_NAME="${ASG_NAME}"
          SSM_PARAM="${SSM_PARAM}"

          INSTANCE_IDS=$(aws autoscaling describe-auto-scaling-groups \
            --region "$AWS_REGION" \
            --auto-scaling-group-name "$ASG_NAME" \
            --query "AutoScalingGroups[0].Instances[?LifecycleState=='InService'].InstanceId" \
            --output text)

          if [ -z "$INSTANCE_IDS" ]; then
            echo "[ERROR] No InService instances found in ASG: $ASG_NAME" >&2
            exit 1
          fi

          IMAGE_URI=$(aws ssm get-parameter \
            --region "$AWS_REGION" \
            --name "$SSM_PARAM" \
            --query "Parameter.Value" --output text)

          REGISTRY=$(echo "$IMAGE_URI" | cut -d'/' -f1)

          echo "[INFO] Deploying container on: $INSTANCE_IDS"

          aws ssm send-command \
            --region "$AWS_REGION" \
            --document-name "AWS-RunShellScript" \
            --comment "refit backend deploy" \
            --instance-ids ${INSTANCE_IDS} \
            --parameters commands="[ \\
              \\"set -e\\", \\
              \\"aws ecr get-login-password --region ${AWS_REGION} | docker login --username AWS --password-stdin ${REGISTRY}\\", \\
              \\"docker pull ${IMAGE_URI}\\", \\
              \\"mkdir -p /opt/config\\", \\
              \\"aws secretsmanager get-secret-value --region ${AWS_REGION} --secret-id /refit/spring/application_yml --query SecretString --output text > /opt/config/application.yml\\", \\
              \\"aws secretsmanager get-secret-value --region ${AWS_REGION} --secret-id /refit/firebase/adminsdk --query SecretString --output text > /opt/config/firebase-adminsdk.json\\", \\

              \\"docker network create refit-net || true\\", \\
              \\"docker rm -f redis || true\\", \\
              \\"docker run -d --name redis --network refit-net --restart=always -p 6379:6379 redis:7\\", \\
              \\"docker rm -f refit || true\\", \\
              \\"docker run -d --name refit --network refit-net --restart=always -p 8080:8080 \\
                    -v /opt/config/application.yml:/config/application.yml \\
                    -v /opt/config/firebase-adminsdk.json:/app/config/firebase-adminsdk.json \\
                    -v /home/ec2-user/oci-wallet:/app/oci-wallet \\
                    -e TNS_ADMIN=/app/oci-wallet ${IMAGE_URI}\\" \\
            ]" \
            --output text >/dev/null
        '''
      }
    }
  }

  post {
    always  { cleanWs() }
    success { echo "Deployed : ${IMAGE_URI}" }
    failure { echo "Failed — check Jenkins logs" }
  }
}
