// Jenkins Pipeline for Python PR Merge
pipeline {
    agent {
        docker {
            image 'python:3.9-slim'
            args '-v /tmp/pip-cache:/root/.cache/pip'
        }
    }
    
    environment {
        PIP_CACHE_DIR = '/root/.cache/pip'
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
                sh '''
                    python -m pip install --upgrade pip
                    if [ -f requirements.txt ]; then pip install -r requirements.txt; fi
                    if [ -f requirements-dev.txt ]; then pip install -r requirements-dev.txt; fi
                    pip install flake8 black isort mypy bandit safety pytest pytest-cov
                '''
            }
        }
        
        stage('Code Quality') {
            parallel {
                stage('Flake8') {
                    steps {
                        sh '''
                            flake8 . --count --select=E9,F63,F7,F82 --show-source --statistics
                            flake8 . --count --exit-zero --max-complexity=10 --max-line-length=127 --statistics
                        '''
                    }
                }
                
                stage('Black') {
                    steps {
                        sh 'black --check .'
                    }
                }
                
                stage('isort') {
                    steps {
                        sh 'isort --check-only --profile black .'
                    }
                }
                
                stage('mypy') {
                    steps {
                        sh 'mypy .'
                    }
                }
                
                stage('Security Scan') {
                    steps {
                        sh '''
                            bandit -r . -x tests/ -f json -o bandit-results.json
                            safety check -r requirements.txt --json > safety-results.json
                        '''
                        archiveArtifacts artifacts: '*-results.json', fingerprint: true
                    }
                }
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
        
        stage('SonarQube Analysis') {
            environment {
                scannerHome = tool 'SonarQubeScanner'
            }
            steps {
                withSonarQubeEnv('SonarQube') {
                    sh "${scannerHome}/bin/sonar-scanner \
                        -Dsonar.projectKey=python-project-pr \
                        -Dsonar.projectName='Python Project PR' \
                        -Dsonar.sources=. \
                        -Dsonar.python.coverage.reportPaths=coverage.xml \
                        -Dsonar.python.xunit.reportPath=test-results.xml \
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