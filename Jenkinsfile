pipeline {
    agent any

    tools {
        jdk 'JDK17'
        maven 'Maven3'
    }

    stages {

        stage('Build Project') {
            steps {
                bat 'mvn clean package'
            }
        }

        stage('Capture Current Screenshots') {
            steps {
                bat 'java -jar target/ui-change-detector-1.0-SNAPSHOT.jar current urls.txt v_latest'
            }
        }

        stage('Compare With Baseline') {
            steps {
                bat 'java -jar target/ui-change-detector-1.0-SNAPSHOT.jar compare v_latest'
            }
        }

        stage('Archive Reports') {
            steps {
                archiveArtifacts artifacts: 'ui-check-output/**', fingerprint: true
            }
        }
    }
}