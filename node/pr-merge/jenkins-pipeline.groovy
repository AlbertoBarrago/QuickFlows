// Jenkins Pipeline for Node.js PR Merge
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
        CHANGE_BRANCH = "${env.CHANGE_BRANCH ?: env.BRANCH_NAME}"
        TARGET_BRANCH = "${env.CHANGE_TARGET ?: 'main'}"
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
        
        stage('Security Scan') {
            steps {
                sh '''
                    npm install -g snyk
                    snyk test --json > snyk-results.json || true
                    npm audit --json > npm-audit-results.json || true
                '''
                archiveArtifacts artifacts: '*-results.json', fingerprint: true
            }
        }
        
        stage('SonarQube Analysis') {
            environment {
                scannerHome = tool 'SonarQubeScanner'
            }
            steps {
                withSonarQubeEnv('SonarQube') {
                    sh "${scannerHome}/bin/sonar-scanner \
                        -Dsonar.projectKey=node-project-pr \
                        -Dsonar.projectName='Node.js Project PR' \
                        -Dsonar.sources=src \
                        -Dsonar.javascript.lcov.reportPaths=coverage/lcov.info \
                        -Dsonar.pullrequest.key=${env.CHANGE_ID} \
                        -Dsonar.pullrequest.branch=${CHANGE_BRANCH} \
                        -Dsonar.pullrequest.base=${TARGET_BRANCH}"
                }
                timeout(time: 10, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
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
                      message: "PR #${env.CHANGE_ID} - The pipeline ${currentBuild.fullDisplayName} completed successfully."
        }
        failure {
            slackSend channel: '#builds',
                      color: 'danger',
                      message: "PR #${env.CHANGE_ID} - The pipeline ${currentBuild.fullDisplayName} failed."
        }
    }
}