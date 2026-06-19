pipeline {
    agent any
    
    options {
        quietPeriod(120)
        // Log-rotator instellingen overgenomen uit de oude XML (40 dagen bewaren, max 69 builds)
        buildDiscarder(logRotator(daysToKeepStr: '40', numToKeepStr: '69'))
    }
    
    triggers {
        githubPush()
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
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
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