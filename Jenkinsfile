pipeline {
    agent any
    options {
        disableConcurrentBuilds()
    }
    stages {
        stage('Checkout') {
            steps {
                checkout changelog: true, poll: true, scm: [$class: 'GitSCM', branches: [[name: '*/1.2-dev']]]
            }
        }

        stage('Build') {
            steps {
                sh './gradlew build --stacktrace'
            }
        }
        stage('Archive artifacts') {
            steps {
                archiveArtifacts artifacts: 'plugin/build/libs/necrify*.jar, paper-extension/build/libs/necrify-*.jar'
            }
        }
    }
}