pipeline {
  agent any

  environment {
    AWS_REGION = 'ap-northeast-2'
    ACCOUNT_ID = sh(script: "aws sts get-caller-identity --query Account --output text --region ${AWS_REGION}", returnStdout: true).trim()
    ECR_REPO   = 'refit-spring'
    ECR_URI    = "${ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/${ECR_REPO}"

    GIT_SHA    = sh(script: "git rev-parse --short HEAD", returnStdout: true).trim()
    TS         = sh(script: "date +%Y%m%d%H%M%S", returnStdout: true).trim()
    IMAGE_TAG  = "${TS}-${GIT_SHA}"
    IMAGE_URI  = "${ECR_URI}:${IMAGE_TAG}"

    SSM_PARAM  = '/refit/backend/image_uri'
    ASG_NAME   = 'asg-spring-dev'
  }

  options { timestamps(); ansiColor('xterm') }

  stages {
    stage('Checkout'){ steps { checkout scm } }

    stage('Build JAR (JDK 21)'){
      steps {
        sh '''
          chmod +x gradlew || true
          ./gradlew -Dorg.gradle.jvmargs="-Xmx512m" clean bootJar -x test
        '''
      }
    }

    stage('Docker Login & Build & Push'){
      steps {
        sh '''
          aws ecr describe-repositories --repository-names ${ECR_REPO} --region ${AWS_REGION} >/dev/null 2>&1 \
          || aws ecr create-repository --repository-name ${ECR_REPO} --region ${AWS_REGION}

          aws ecr get-login-password --region ${AWS_REGION} \
          | docker login --username AWS --password-stdin ${ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com

          export DOCKER_BUILDKIT=1
          docker build --memory=1.5g -t ${IMAGE_URI} .
          docker push ${IMAGE_URI}
        '''
      }
    }

    stage('Update SSM (image_uri)'){
      steps {
        sh '''
          aws ssm put-parameter --region ${AWS_REGION} \
            --name "${SSM_PARAM}" --type "String" \
            --value "${IMAGE_URI}" --overwrite
        '''
      }
    }

    stage('ASG Instance Refresh (rolling)'){
      steps {
        sh '''
          REFRESH_ID=$(aws autoscaling start-instance-refresh \
            --region ${AWS_REGION} \
            --auto-scaling-group-name ${ASG_NAME} \
            --preferences MinHealthyPercentage=100,InstanceWarmup=120 \
            --query "InstanceRefreshId" --output text)
          echo "InstanceRefreshId: ${REFRESH_ID}"

          for i in $(seq 1 120); do
            STATUS=$(aws autoscaling describe-instance-refreshes \
              --region ${AWS_REGION} \
              --auto-scaling-group-name ${ASG_NAME} \
              --query "InstanceRefreshes[?InstanceRefreshId=='${REFRESH_ID}'].Status" \
              --output text)
            echo "Status: ${STATUS}"
            [ "$STATUS" = "Successful" ] && exit 0
            [ "$STATUS" = "Failed" ] || [ "$STATUS" = "Cancelled" ] && exit 1
            sleep 10
          done
          echo "Timeout"; exit 1
        '''
      }
    }
  }

  post {
    success { echo "Deployed: ${IMAGE_URI}" }
    failure { echo "Failed — check Jenkins logs & ASG refresh status" }
  }
}
