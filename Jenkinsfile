pipeline {
    agent any

    tools {
        jdk 'JDK17'
        maven 'Maven3'
    }

    parameters {
        string(name: 'VERSION', defaultValue: 'v4.0', description: 'UI comparison version')
    }

    stages {

        stage('Print Version') {
            steps {
                echo "Running build for version ${params.VERSION}"
            }
        }

        stage('Build Project') {
            steps {
                bat 'mvn clean package'
            }
        }

        stage('Capture Current Screenshots') {
            steps {
                bat "java -jar target/ui-change-detector-1.0-SNAPSHOT.jar current urls.txt ${params.VERSION}"
            }
        }

        stage('Compare With Baseline') {
            steps {
                bat "java -jar target/ui-change-detector-1.0-SNAPSHOT.jar compare ${params.VERSION}"
            }
        }

        stage('Archive Reports') {
            steps {
                archiveArtifacts artifacts: 'ui-check-output/**', fingerprint: true
            }
        }
    }
}