// Jenkins Pipeline for Node.js Build
pipeline {
    agent {
        docker {
            image 'node:16-alpine'
            args '-v /root/.npm:/root/.npm'
        }
    }
    
    environment {
        CI = 'true'
        HOME = '${WORKSPACE}'
        NPM_CONFIG_CACHE = '${WORKSPACE}/.npm'
        HUSKY_SKIP_INSTALL = 'true' // Skip Husky install during CI
    }
    
    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        
        stage('Setup') {
            steps {
                sh 'npm ci'
            }
        }
        
        stage('Lint') {
            steps {
                sh 'npm run lint'
            }
        }
        
        stage('Test') {
            steps {
                sh 'npm test'
            }
            post {
                always {
                    junit 'test-results/*.xml'
                }
            }
        }
        
        stage('Build') {
            steps {
                sh 'npm run build'
            }
        }
        
        stage('SonarQube Analysis') {
            environment {
                scannerHome = tool 'SonarQubeScanner'
            }
            steps {
                withSonarQubeEnv('SonarQube') {
                    sh "${scannerHome}/bin/sonar-scanner \
                        -Dsonar.projectKey=node-project \
                        -Dsonar.projectName='Node.js Project' \
                        -Dsonar.sources=src \
                        -Dsonar.tests=test \
                        -Dsonar.javascript.lcov.reportPaths=coverage/lcov.info"
                }
                timeout(time: 10, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }
        
        stage('Archive Artifacts') {
            steps {
                archiveArtifacts artifacts: 'dist/**/*', fingerprint: true
            }
        }
    }
    
    post {
        always {
            cleanWs()
        }
        success {
            slackSend channel: '#builds',
                      color: 'good',
                      message: "The pipeline ${currentBuild.fullDisplayName} completed successfully."
        }
        failure {
            slackSend channel: '#builds',
                      color: 'danger',
                      message: "The pipeline ${currentBuild.fullDisplayName} failed."
        }
    }
}