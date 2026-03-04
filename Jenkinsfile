pipeline {
    agent any

    tools {
        jdk 'JDK17'
        maven 'Maven3'
    }

    environment {
        VERSION = "v${BUILD_NUMBER}"
    }

    stages {

        stage('Build Project') {
            steps {
                bat 'mvn clean package'
            }
        }

        stage('Capture Current Screenshots') {
            steps {
                bat "java -jar target/ui-change-detector-1.0-SNAPSHOT.jar current urls.txt %VERSION%"
            }
        }

        stage('Compare With Baseline') {
            steps {
                bat "java -jar target/ui-change-detector-1.0-SNAPSHOT.jar compare %VERSION%"
            }
        }

        stage('Archive Reports') {
            steps {
                archiveArtifacts artifacts: 'ui-check-output/**', fingerprint: true
            }
        }
    }
}