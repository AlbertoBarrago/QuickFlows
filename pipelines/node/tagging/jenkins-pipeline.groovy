// Jenkins Pipeline for Node.js Tagging
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
        TAG_NAME = sh(script: 'git describe --tags --exact-match 2>/dev/null || echo ""', returnStdout: true).trim()
        VERSION = sh(script: 'echo ${TAG_NAME} | sed "s/^v//"', returnStdout: true).trim()
    }
    
    stages {
        stage('Validate Tag') {
            steps {
                script {
                    if (!env.TAG_NAME) {
                        error "No tag found. This pipeline should only run on tags."
                    }
                    
                    if (!(env.TAG_NAME ==~ /^v\d+\.\d+\.\d+$/)) {
                        error "Invalid tag format. Must be in format vX.Y.Z"
                    }
                }
            }
        }
        
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
        
        stage('Update Version') {
            steps {
                sh '''
                    # Update version in package.json
                    sed -i "s/\"version\": \".*\"/\"version\": \"${VERSION}\"/g" package.json
                '''
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
                archiveArtifacts artifacts: 'dist/**/*', fingerprint: true
            }
        }
        
        stage('Create Release Notes') {
            steps {
                sh '''
                    echo "# Release ${VERSION}" > RELEASE_NOTES.md
                    echo "" >> RELEASE_NOTES.md
                    echo "## Changes" >> RELEASE_NOTES.md
                    git log $(git describe --tags --abbrev=0 HEAD^)..HEAD --pretty=format:"* %s" >> RELEASE_NOTES.md
                '''
                archiveArtifacts artifacts: 'RELEASE_NOTES.md', fingerprint: true
            }
        }
        
        stage('Tag Repository') {
            steps {
                withCredentials([sshUserPrivateKey(credentialsId: 'git-credentials', keyFileVariable: 'SSH_KEY')]) {
                    sh '''
                        mkdir -p ~/.ssh
                        cp ${SSH_KEY} ~/.ssh/id_rsa
                        chmod 600 ~/.ssh/id_rsa
                        
                        git config user.name "Jenkins"
                        git config user.email "jenkins@example.com"
                        
                        # If we need to push version updates
                        git add -A
                        git diff --quiet && git diff --staged --quiet || git commit -m "Update version to ${VERSION}"
                        
                        # Push changes and tag
                        git push origin HEAD:${BRANCH_NAME}
                    '''
                }
            }
        }
    }
    
    post {
        always {
            cleanWs()
        }
        success {
            slackSend channel: '#releases',
                      color: 'good',
                      message: "Tagging ${TAG_NAME} - The pipeline ${currentBuild.fullDisplayName} completed successfully."
        }
        failure {
            slackSend channel: '#releases',
                      color: 'danger',
                      message: "Tagging ${TAG_NAME} - The pipeline ${currentBuild.fullDisplayName} failed."
        }
    }
}