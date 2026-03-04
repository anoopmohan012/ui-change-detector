pipeline {
    agent any

    tools {
        jdk 'JDK17'
        maven 'Maven3'
    }

    parameters {
        choice(
            name: 'ACTION',
            choices: ['baseline', 'current', 'compare'],
            description: 'Select pipeline action'
        )
        string(name: 'VERSION', defaultValue: 'v1.0', description: 'UI version')
    }

    stages {

        stage('Pipeline Info') {
            steps {
                echo "Running ACTION: ${params.ACTION}"
                echo "VERSION: ${params.VERSION}"
            }
        }

        stage('Build Project') {
            steps {
                bat 'mvn clean package'
            }
        }

        stage('Capture Baseline') {
            when {
                expression { params.ACTION == 'baseline' }
            }
            steps {
                bat "java -jar target/ui-change-detector-1.0-SNAPSHOT.jar baseline urls.txt ${params.VERSION}"
            }
        }

        stage('Capture Current Screenshots') {
            when {
                expression { params.ACTION == 'current' }
            }
            steps {
                bat "java -jar target/ui-change-detector-1.0-SNAPSHOT.jar current urls.txt ${params.VERSION}"
            }
        }

        stage('Copy Baseline Artifacts') {
            when {
                expression { params.ACTION == 'compare' }
            }
            steps {
                copyArtifacts projectName: 'UI-Baseline', selector: lastSuccessful()
            }
        }

        stage('Copy Current Artifacts') {
            when {
                expression { params.ACTION == 'compare' }
            }
            steps {
                copyArtifacts projectName: 'UI-Current', selector: lastSuccessful()
            }
        }

        stage('Compare With Baseline') {
            when {
                expression { params.ACTION == 'compare' }
            }
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