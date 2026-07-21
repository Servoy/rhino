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
            regexpFilterExpression: "^refs/heads/${env.BRANCH}\$"
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
                    configFile(fileId: 'ba7b9372-76e5-4898-a2be-1dde60a0d6e3', variable: 'SETTINGS'),
                    configFile(fileId: '254658cc-4d79-45bf-ace9-28bd69fd403d', variable: 'TOOLCHAIN')
                ]) {
                    sh 'mvn -B -s "$SETTINGS" -t "$TOOLCHAIN" $goals'
                }
            }
        }
    }
    
    post {
        always {
            // Testresultaten verzamelen (AggregatedTestResultPublisher)
            junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml'
        }
        
        success {
            // Trigger downstream project 'build' bij succes
            build job: 'build', wait: false
        }
        
        failure {
            // Veilige Teams notificatie zonder quotes
            office365ConnectorSend webhookUrl: TEAMS_WEBHOOK, status: 'Failed'
        }
        
        unstable {
            office365ConnectorSend webhookUrl: TEAMS_WEBHOOK, status: 'Unstable'
        }
        
        fixed {
            office365ConnectorSend webhookUrl: TEAMS_WEBHOOK, status: 'Back to Normal'
        }
    }
}