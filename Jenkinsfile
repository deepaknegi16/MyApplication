// CI pipeline: runs on every PR and branch build (see README "CI" section for the
// Jenkins-side setup — a Multibranch Pipeline job with the GitHub Branch Source plugin
// discovers PRs and runs this file automatically).
pipeline {
    // Reproducible build environment: Maven 3.9 + JDK 21 in a container.
    // If your Jenkins agents don't have Docker, replace with `agent any` and make sure
    // the agent has JDK 21 (the Maven wrapper takes care of Maven itself).
    agent {
        docker {
            image 'maven:3.9-eclipse-temurin-21'
            // Cache the local Maven repo between builds so dependencies aren't re-downloaded
            args '-v $HOME/.m2:/root/.m2'
        }
    }

    options {
        timestamps()
        timeout(time: 30, unit: 'MINUTES')
        // Abort a stale PR build when a new commit is pushed to the same PR
        disableConcurrentBuilds(abortPrevious: true)
    }

    stages {
        stage('Unit tests') {
            steps {
                sh './mvnw -B clean test'
            }
            post {
                always {
                    junit 'target/surefire-reports/*.xml'
                }
            }
        }

        stage('Build') {
            steps {
                // Tests already ran in the previous stage
                sh './mvnw -B -DskipTests package'
            }
            post {
                success {
                    archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
                }
            }
        }
    }

    post {
        failure {
            echo "Build failed — check the test report above."
        }
    }
}
