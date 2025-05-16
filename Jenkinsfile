pipeline {
    agent any
    stages {
        stage('Build Project') {
            steps {
                sh './gradlew clean alljavadoc --stacktrace -Pbuildnumber=$BUILD_NUMBER'
            }
        }
        stage('Publish to Hangar') {
            when {
                branch 'master'
            }
            steps {
                sh './gradlew publishAllPublicationsToHangar'
            }
        }
        stage('Publish via SSH') {
            steps {
                sshPublisher(
                    publishers: [
                        sshPublisherDesc(
                            configName: 'my-ssh-server',   // Defined in Jenkins config
                            transfers: [
                                sshTransfer(
                                    sourceFiles: 'build/docs/**',
                                    removePrefix: 'build/docs',      // Optional, removes this path prefix
                                    remoteDirectory: '/var/www/javadocs/myproject',
                                    execCommand: 'echo "Deployed!"' // Optional remote command
                                )
                            ],
                            usePromotionTimestamp: false,
                            verbose: true
                        )
                    ]
                )
            }
        }
    }
    post {
        success {
            archiveArtifacts artifacts: 'necrify-velocity/build/libs/Necrify-Velocity*.jar',
            allowEmptyArchive: true
            archiveArtifacts artifacts: 'necrify-paper/build/libs/Necrify-Paper*.jar', allowEmptyArchive: true
        }
    }
}