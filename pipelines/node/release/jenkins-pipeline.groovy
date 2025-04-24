// Jenkins Pipeline for Node.js Release
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
        NPM_TOKEN = credentials('npm-token')
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
        
        stage('Publish to NPM') {
            when {
                expression { return env.TAG_NAME =~ /^v\d+\.\d+\.\d+$/ }
            }
            steps {
                sh '''
                    echo "//registry.npmjs.org/:_authToken=${NPM_TOKEN}" > .npmrc
                    npm publish --access public
                '''
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