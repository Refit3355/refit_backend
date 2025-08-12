pipeline {
  agent any
  options { timestamps() }

  environment {
    AWS_REGION = 'ap-northeast-2'
    ECR_REPO   = 'refit-spring'
    SSM_PARAM  = '/refit/backend/image_uri'
    ASG_NAME   = 'asg-spring-dev'
    APP_DIR    = 'app'
  }

  stages {
    stage('Checkout'){ steps { checkout scm } }

    stage('Prepare Vars') {
      steps {
        script {
          if (!fileExists("${env.APP_DIR}/gradlew")) {
            error "[ERROR] ${env.APP_DIR}/gradlew not found. 레포 구조 확인"
          }

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
            chmod +x gradlew || true
            ./gradlew -Dorg.gradle.jvmargs="-Xmx512m" clean bootJar -x test
          '''
        }
      }
    }

    stage('Docker Login & Build & Push'){
      steps {
        sh """
          aws ecr describe-repositories --repository-names ${ECR_REPO} --region ${AWS_REGION} >/dev/null 2>&1 \
          || aws ecr create-repository --repository-name ${ECR_REPO} --region ${AWS_REGION}

          aws ecr get-login-password --region ${AWS_REGION} \
          | docker login --username AWS --password-stdin ${ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com

          export DOCKER_BUILDKIT=1
          docker build --memory=1.5g -t ${IMAGE_URI} -f ${APP_DIR}/Dockerfile .
          docker push ${IMAGE_URI}
        """
      }
    }

    stage('Update SSM (image_uri)'){
      steps {
        sh """
          aws ssm put-parameter --region ${AWS_REGION} \
            --name "${SSM_PARAM}" --type "String" \
            --value "${IMAGE_URI}" --overwrite
        """
      }
    }

    stage('ASG Instance Refresh (rolling)'){
      steps {
        sh """
          REFRESH_ID=\$(aws autoscaling start-instance-refresh \
            --region ${AWS_REGION} \
            --auto-scaling-group-name ${ASG_NAME} \
            --preferences MinHealthyPercentage=100,InstanceWarmup=120 \
            --query "InstanceRefreshId" --output text)
          echo "InstanceRefreshId: \${REFRESH_ID}"

          for i in \$(seq 1 120); do
            STATUS=\$(aws autoscaling describe-instance-refreshes \
              --region ${AWS_REGION} \
              --auto-scaling-group-name ${ASG_NAME} \
              --query "InstanceRefreshes[?InstanceRefreshId=='\${REFRESH_ID}'].Status" \
              --output text)
            echo "Status: \${STATUS}"
            [ "\$STATUS" = "Successful" ] && exit 0
            [ "\$STATUS" = "Failed" ] || [ "\$STATUS" = "Cancelled" ] && exit 1
            sleep 10
          done
          echo "Timeout"; exit 1
        """
      }
    }
  }

  post {
    always  { cleanWs() }
    success { echo "Deployed: ${IMAGE_URI}" }
    failure { echo "Failed — check Jenkins logs & ASG refresh status" }
  }
}
