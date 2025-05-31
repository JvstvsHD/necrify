pipeline {
    agent any
    stages {
        stage('Debug') {
            steps {
                echo "GIT_BRANCH: ${env.GIT_BRANCH}"
                echo "BRANCH_NAME: ${env.BRANCH_NAME}"
            }
        }
        stage('Check Committer') {
            steps {
                script {
                    def author = sh(script: "git log -1 --pretty=%an", returnStdout: true).trim()
                    if (author == 'Renovate Bot' || author == 'renovate[bot]') {
                        echo "Skipping pipeline: triggered by Renovate."
                        currentBuild.result = 'SUCCESS'
                        return
                    }
                }
            }
        }
        stage('Build Project') {
            steps {
                sh './gradlew clean build alljavadoc --stacktrace -Pbuildnumber=$BUILD_NUMBER'
            }
        }
        stage('Publish to Hangar and Modrinth') {
            when {
                expression {
                    return env.GIT_BRANCH == 'origin/master'
                }
            }
            steps {
                sh './gradlew publishAllPublicationsToHangar modrinth modrinthSyncBody'
            }
        }
        stage('Publish via SSH') {
            steps {
                sshPublisher(
                    publishers: [
                        sshPublisherDesc(
                            configName: 'server',
                            transfers: [
                                sshTransfer(
                                    sourceFiles: 'build/docs/javadoc/**',
                                    removePrefix: 'build/docs/javadoc',
                                    remoteDirectory: '/var/www/jd/necrify',
                                    execCommand: '''bash -c '
                                      cd /var/www/jd/necrify
                                      dir=$(<version.txt)
                                      if [ -d "$dir" ]; then
                                        touch "$dir"
                                      else
                                         echo "Directory does not exist: $dir" >&2
                                      fi
                                      touch "latest"
                                      rm version.txt
                                    ' ''',
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