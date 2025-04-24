// Jenkins Pipeline for Java Maven Release
pipeline {
    agent {
        docker {
            image 'maven:3.8-openjdk-11'
            args '-v /root/.m2:/root/.m2'
        }
    }
    
    environment {
        MAVEN_OPTS = '-Dmaven.repo.local=/root/.m2/repository'
    }
    
    stages {
        stage('Checkout') {
            steps {
                checkout scm
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
        
        stage('Unit Tests') {
            steps {
                sh 'mvn test'
            }
            post {
                always {
                    junit 'target/surefire-reports/**/*.xml'
                    jacoco(
                        execPattern: 'target/jacoco.exec',
                        classPattern: 'target/classes',
                        sourcePattern: 'src/main/java',
                        exclusionPattern: 'src/test*'
                    )
                }
            }
        }
        
        stage('Integration Tests') {
            steps {
                sh 'mvn verify -DskipUnitTests'
            }
            post {
                always {
                    junit 'target/failsafe-reports/**/*.xml'
                }
            }
        }
        
        stage('Package') {
            steps {
                sh 'mvn package -DskipTests'
                archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
            }
        }
        
        stage('Deploy to Maven Repository') {
            when {
                expression { return env.TAG_NAME =~ /^v\d+\.\d+\.\d+$/ }
            }
            steps {
                withCredentials([usernamePassword(credentialsId: 'maven-repo-credentials', usernameVariable: 'MAVEN_USERNAME', passwordVariable: 'MAVEN_PASSWORD')]) {
                    sh '''
                        mvn deploy -DskipTests \
                          -Drepo.username=${MAVEN_USERNAME} \
                          -Drepo.password=${MAVEN_PASSWORD}
                    '''
                }
            }
        }
        
        stage('Create GitHub Release') {
            when {
                expression { return env.TAG_NAME =~ /^v\d+\.\d+\.\d+$/ }
            }
            steps {
                withCredentials([string(credentialsId: 'github-token', variable: 'GITHUB_TOKEN')]) {
                    sh '''
                        VERSION=${TAG_NAME#v}
                        REPO_FULL_NAME=$(git config --get remote.origin.url | sed 's/.*:\/\/github.com\///;s/.git$//')
                        
                        # Create a release
                        curl -X POST \
                          -H "Authorization: token ${GITHUB_TOKEN}" \
                          -H "Accept: application/vnd.github.v3+json" \
                          https://api.github.com/repos/${REPO_FULL_NAME}/releases \
                          -d "{\
                            \"tag_name\": \"${TAG_NAME}\",\
                            \"name\": \"Release ${VERSION}\",\
                            \"body\": \"Release of version ${VERSION}\",\
                            \"draft\": false,\
                            \"prerelease\": false\
                          }"
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
                      message: "Release ${env.TAG_NAME} - The pipeline ${currentBuild.fullDisplayName} completed successfully."
        }
        failure {
            slackSend channel: '#releases',
                      color: 'danger',
                      message: "Release ${env.TAG_NAME} - The pipeline ${currentBuild.fullDisplayName} failed."
        }
    }
}