def lintChecks() {
    sh '''  
            echo Lint Checks for ${COMPONENT}
            echo installing jslint
            # npm install jslint
            # ls -ltr node_modules/jslint/bin/
            # node_modules/jslint/bin/jslint.js server.js
            echo performing lint checks for ${COMPONENT}
            echo performing lint checks completed ${COMPONENT}
    ''' 
}

// Call is the default function which will be called when you call the fileName
def call() {
    pipeline {
        agent any 

        environment { 
            SONAR = credentials('SONAR') 
            NEXUS = credentials('NEXUS')
            SONAR_URL = "172.31.9.236"
            NEXUS_URL = "172.31.13.88"
        }

        stages {
            stage('Lint Checks') {
                steps {
                    script {
                        lintChecks()
                    }
                }
            }
            stage('Sonar Checks') {
                steps {
                    script {
                        env.ARGS="-Dsonar.sources=."
                        common.sonarChecks()
                    }
                }
            }

        stage('Test Cases') {
            parallel {
                stage('Unit Testing') {
                    steps {
                        // sh "npm test"
                        sh "echo Performing Unit Testing"
                    }
                }
                stage('Integration Testing') {
                    steps {
                        // sh "npm verify"
                        sh "echo Integration Unit Testing"
                    }
                }
                stage('Functional Testing') {
                    steps {
                        sh "echo Integration Unit Testing"
                        }
                    }
                }
            }

        stage('Checking the Artifact Version') {
            when { expression { env.TAG_NAME != null } }
            steps {
                script {
                        env.UPLOAD_STATUS=sh(returnStdout: true, script: 'curl -L -s http://172.31.13.88:8081/service/rest/repository/browse/${COMPONENT} | grep ${COMPONENT}-${TAG_NAME}.zip' || true)
                        print UPLOAD_STATUS
                    }
                }
            }

        stage('Prepare the artifacts') {
            when { expression { env.TAG_NAME != null } }
            steps {
                    sh "npm install"
                    sh "echo Preparing the artifacts"
                    sh "zip ${COMPONENT}-${TAG_NAME}.zip node_modules server.js"
                }
            }

        stage('Publish the artifacts') {
            when { expression { env.TAG_NAME != null } }
            steps {
                    sh "curl -f -v -u ${NEXUS_USR}:${NEXUS_PSW} --upload-file ${COMPONENT}-${TAG_NAME}.zip http://${NEXUS_URL}:8081/repository/${COMPONENT}/${COMPONENT}-${TAG_NAME}.zip"                
                }
            }
        }
    }
}