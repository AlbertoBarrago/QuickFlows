// Jenkins Pipeline for Java Maven Tagging
pipeline {
    agent {
        docker {
            image 'maven:3.8-openjdk-11'
            args '-v /root/.m2:/root/.m2'
        }
    }
    
    environment {
        MAVEN_OPTS = '-Dmaven.repo.local=/root/.m2/repository'
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
        
        stage('Update Version') {
            steps {
                sh '''
                    # Update version in pom.xml
                    mvn versions:set -DnewVersion=${VERSION} -DgenerateBackupPoms=false
                '''
            }
        }
        
        stage('Validate') {
            steps {
                sh 'mvn validate'
            }
        }
        
        stage('Compile') {
            steps {
                sh 'mvn compile'
            }
        }
        
        stage('Test') {
            steps {
                sh 'mvn test'
            }
            post {
                always {
                    junit 'target/surefire-reports/**/*.xml'
                }
            }
        }
        
        stage('Package') {
            steps {
                sh 'mvn package -DskipTests'
                archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
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