// Jenkins Pipeline for Python Tagging
pipeline {
    agent {
        docker {
            image 'python:3.9-slim'
            args '-v /tmp/pip-cache:/root/.cache/pip'
        }
    }
    
    environment {
        PIP_CACHE_DIR = '/root/.cache/pip'
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
                sh '''
                    python -m pip install --upgrade pip
                    if [ -f requirements.txt ]; then pip install -r requirements.txt; fi
                    if [ -f requirements-dev.txt ]; then pip install -r requirements-dev.txt; fi
                '''
            }
        }
        
        stage('Update Version') {
            steps {
                sh '''
                    # Update version in setup.py if it exists
                    if [ -f setup.py ]; then
                        sed -i "s/version=\".*\"/version=\"${VERSION}\"/g" setup.py
                    fi
                    
                    # Update version in pyproject.toml if it exists
                    if [ -f pyproject.toml ]; then
                        sed -i "s/version = \".*\"/version = \"${VERSION}\"/g" pyproject.toml
                    fi
                    
                    # Update version in __init__.py if it exists
                    find . -name "__init__.py" -type f -exec sed -i "s/__version__ = \".*\"/__version__ = \"${VERSION}\"/g" {} \;
                '''
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