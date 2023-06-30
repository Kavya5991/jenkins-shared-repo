def call(String repositoryUrl) {
  pipeline {
    agent any
    stages {
      stage('Clone Repository') {
        steps {
          git branch: 'master', url: "${repositoryUrl}"
        }
      }
      stage('Create Docker Image') {
        steps {
          writeFile file: 'Dockerfile', text: '''
            FROM ubuntu:22.04
            RUN apt-get update && apt-get install -y wget openjdk-11-jdk
            RUN wget -q https://dlcdn.apache.org/maven/maven-3/3.9.3/binaries/apache-maven-3.9.3-bin.tar.gz
            RUN tar xf apache-maven-3.9.3-bin.tar.gz -C /opt
            RUN rm apache-maven-3.9.3-bin.tar.gz
            ENV MAVEN_HOME=/opt/apache-maven-3.9.3
            ENV PATH=$MAVEN_HOME/bin:$PATH
          '''
        }
      }
      stage('Build Docker Image') {
        steps {
          // Build Docker image
          sh 'docker build -t maven_generic -f Dockerfile .'
        }
      }
      stage('Run Container') {
        steps {
          script {
            sh 'docker run -dt --rm --name Maven_Dockker_Pipeline_Generic maven_generic'
            sh 'docker exec Maven_Dockker_Pipeline_Generic mkdir /workspace'
            sh 'docker cp pom.xml Maven_Dockker_Pipeline_Generic:/workspace/'
            sh 'docker exec Maven_Dockker_Pipeline_Generic mvn -f /workspace/pom.xml --version'
            sh 'docker exec Maven_Dockker_Pipeline_Generic mvn -f /workspace/pom.xml clean install'
          }
        }
      }
    }
  }
}
