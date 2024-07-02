pipeline {
    agent any
    options {
        disableConcurrentBuilds()
    }
    stages {
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