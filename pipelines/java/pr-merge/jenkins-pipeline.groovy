// Jenkins Pipeline for Java Maven PR Merge
pipeline {
    agent {
        docker {
            image 'maven:3.8-openjdk-11'
            args '-v /root/.m2:/root/.m2'
        }
    }
    
    environment {
        MAVEN_OPTS = '-Dmaven.repo.local=/root/.m2/repository'
        CHANGE_BRANCH = "${env.CHANGE_BRANCH ?: env.BRANCH_NAME}"
        TARGET_BRANCH = "${env.CHANGE_TARGET ?: 'main'}"
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
        
        stage('Code Quality') {
            parallel {
                stage('Checkstyle') {
                    steps {
                        sh 'mvn checkstyle:check'
                    }
                }
                
                stage('PMD') {
                    steps {
                        sh 'mvn pmd:check'
                    }
                }
                
                stage('SpotBugs') {
                    steps {
                        sh 'mvn spotbugs:check'
                    }
                }
                
                stage('OWASP Dependency Check') {
                    steps {
                        sh 'mvn org.owasp:dependency-check-maven:check'
                    }
                }
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
        
        stage('SonarQube Analysis') {
            steps {
                withSonarQubeEnv('SonarQube') {
                    sh '''
                        mvn sonar:sonar \
                          -Dsonar.projectKey=java-project-pr \
                          -Dsonar.projectName='Java Project PR' \
                          -Dsonar.pullrequest.key=${CHANGE_ID} \
                          -Dsonar.pullrequest.branch=${CHANGE_BRANCH} \
                          -Dsonar.pullrequest.base=${TARGET_BRANCH}
                    '''
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