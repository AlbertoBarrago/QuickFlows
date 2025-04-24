// Jenkins Pipeline for Python Release
pipeline {
    agent {
        docker {
            image 'python:3.9-slim'
            args '-v /tmp/pip-cache:/root/.cache/pip'
        }
    }
    
    environment {
        PIP_CACHE_DIR = '/root/.cache/pip'
        PYPI_CREDENTIALS = credentials('pypi-credentials')
    }
    
    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        
        stage('Setup') {
            steps {
                sh '''
                    python -m pip install --upgrade pip
                    pip install build twine wheel pytest pytest-cov
                    if [ -f requirements.txt ]; then pip install -r requirements.txt; fi
                '''
            }
        }
        
        stage('Test') {
            steps {
                sh 'pytest --junitxml=test-results.xml --cov=./ --cov-report=xml'
            }
            post {
                always {
                    junit 'test-results.xml'
                    recordCoverage(
                        tools: [[parser: 'COBERTURA', pattern: 'coverage.xml']],
                        qualityGates: [
                            [threshold: 70.0, metric: 'LINE', unstable: true],
                            [threshold: 60.0, metric: 'LINE', unstable: false]
                        ]
                    )
                }
            }
        }
        
        stage('Build Package') {
            steps {
                sh 'python -m build'
                archiveArtifacts artifacts: 'dist/*', fingerprint: true
            }
        }
        
        stage('Publish to PyPI') {
            when {
                expression { return env.TAG_NAME =~ /^v\d+\.\d+\.\d+$/ }
            }
            steps {
                withCredentials([usernamePassword(credentialsId: 'pypi-credentials', usernameVariable: 'TWINE_USERNAME', passwordVariable: 'TWINE_PASSWORD')]) {
                    sh '''
                        python -m twine upload dist/*
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