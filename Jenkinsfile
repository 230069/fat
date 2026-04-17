pipeline {
    agent any

    tools {
        maven 'Maven 3.9' // Must match the name in Jenkins Global Tool Configuration
        jdk 'Java 17'     // Must match the name in Jenkins Global Tool Configuration
    }

    environment {
        DOCKERHUB_CREDENTIALS = 'dockerhub-login' // ID of credentials stored in Jenkins
        DOCKER_IMAGE = "yourdockerusername/leave-management"
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build & Unit Tests') {
            steps {
                sh 'mvn clean package'
            }
            post {
                always {
                    junit '**/target/surefire-reports/*.xml'
                }
            }
        }

        stage('Docker Build & Push') {
            when {
                branch 'main'
            }
            steps {
                script {
                    dockerImage = docker.build("${DOCKER_IMAGE}:${env.BUILD_ID}")
                    docker.withRegistry('', DOCKERHUB_CREDENTIALS) {
                        dockerImage.push()
                        dockerImage.push('latest')
                    }
                }
            }
        }

        stage('Kubernetes Deploy') {
            when {
                branch 'main'
            }
            steps {
                // Requires the 'Kubernetes CLI' plugin
                withKubeConfig([credentialsId: 'k8s-config']) {
                    sh "sed -i 's|latest|${env.BUILD_ID}|g' k8s/deployment.yaml"
                    sh 'kubectl apply -f k8s/configmap.yaml'
                    sh 'kubectl apply -f k8s/secret.yaml'
                    sh 'kubectl apply -f k8s/mysql-deployment.yaml'
                    sh 'kubectl apply -f k8s/deployment.yaml'
                    sh 'kubectl apply -f k8s/service.yaml'
                }
            }
        }
    }
}