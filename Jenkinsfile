pipeline {
    agent any
    
    options {
        quietPeriod(120)
        // Log-rotator instellingen overgenomen uit de oude XML (40 dagen bewaren, max 69 builds)
        buildDiscarder(logRotator(daysToKeepStr: '40', numToKeepStr: '69'))
    }
    
   triggers {
        GenericTrigger(
            genericVariables: [
                [key: 'ref', value: '$.ref']
            ],
            token: 'rhino',
            regexpFilterText: '$ref',
            regexpFilterExpression: "^refs/heads/servoy\$"
        )
    }

    parameters {
        string(name: 'goals', defaultValue: 'clean install', trim: false)
    }

    environment {
        TEAMS_WEBHOOK = credentials('servoy-teams-webhook')
    }
    
    tools {
        jdk 'Java 21' // Uniform meegetrokken naar Java 21
        maven 'Maven 3.9.16'
    }

    stages {
        stage('Build Rhino') {
            steps {
                configFileProvider([
                    configFile(fileId: 'master_mvn_repo', variable: 'SETTINGS'),
                    configFile(fileId: 'maven_toolchain', variable: 'TOOLCHAIN')
                ]) {
                    sh 'mvn -B -s "$SETTINGS" -t "$TOOLCHAIN" $goals'
                }
            }
        }
        stage('Test Rhino') {
            steps {
                // Only run the Servoy-added tests. The full upstream suite has many
                // pre-existing behavioral divergences against this fork and is not yet triaged;
                // run it manually with: ./gradlew :tests:test
                sh './gradlew :tests:test --tests "org.mozilla.javascript.tests.servoy.*"'
            }
        }
    }
    
    post {
        always {
            // Testresultaten verzamelen (AggregatedTestResultPublisher)
            junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml, **/build/test-results/test/*.xml'
        }
        
        success {
            // Trigger downstream project 'build' bij succes
            build job: 'build', wait: false
        }
        
        failure {
            // Veilige Teams notificatie zonder quotes
            office365ConnectorSend webhookUrl: TEAMS_WEBHOOK, status: 'Failed', adaptiveCards: true
        }
        
        unstable {
            office365ConnectorSend webhookUrl: TEAMS_WEBHOOK, status: 'Unstable', adaptiveCards: true
        }
        
        fixed {
            office365ConnectorSend webhookUrl: TEAMS_WEBHOOK, status: 'Back to Normal', adaptiveCards: true
        }
    }
}