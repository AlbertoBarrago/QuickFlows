// Jenkins Pipeline for Python Build
pipeline {
    agent {
        docker {
            image 'python:3.9-slim'
            args '-v /tmp/pip-cache:/root/.cache/pip'
        }
    }
    
    environment {
        PIP_CACHE_DIR = '/root/.cache/pip'
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
                    pip install pytest pytest-cov flake8 black isort mypy
                '''
            }
        }
        
        stage('Lint') {
            parallel {
                stage('Flake8') {
                    steps {
                        sh '''
                            # stop the build if there are Python syntax errors or undefined names
                            flake8 . --count --select=E9,F63,F7,F82 --show-source --statistics
                            # exit-zero treats all errors as warnings
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
                sh '''
                    pip install build wheel
                    python -m build
                '''
                archiveArtifacts artifacts: 'dist/*', fingerprint: true
            }
        }
        
        stage('SonarQube Analysis') {
            environment {
                scannerHome = tool 'SonarQubeScanner'
            }
            steps {
                withSonarQubeEnv('SonarQube') {
                    sh "${scannerHome}/bin/sonar-scanner \
                        -Dsonar.projectKey=python-project \
                        -Dsonar.projectName='Python Project' \
                        -Dsonar.sources=. \
                        -Dsonar.python.coverage.reportPaths=coverage.xml \
                        -Dsonar.python.xunit.reportPath=test-results.xml"
                }
                timeout(time: 10, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }
        
        stage('Security Scan') {
            steps {
                sh 'pip install bandit safety
                   bandit -r . -x tests/ -f json -o bandit-results.json
                   safety check -r requirements.txt --json > safety-results.json'
                archiveArtifacts artifacts: '*-results.json', fingerprint: true
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