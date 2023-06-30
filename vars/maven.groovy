def call(String repositoryUrl,String containerName) {
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
          sh 'docker build -t maven_generic_final -f Dockerfile .'
        }
      }
      stage('Run Container') {
        steps {
          script {
            sh "docker run -dt --rm --name \"${containerName}\" maven_generic_final"
            sh "docker exec \"${containerName}\" mkdir /workspace"
            sh "docker cp pom.xml \"${containerName}\":/workspace/"
            sh "docker cp settings.xml \"${containerName}\":/opt/apache-maven-3.9.3/conf/"
            sh "docker exec \"${containerName}\" mvn -f /workspace/pom.xml --version"
            sh "docker exec \"${containerName}\" mvn -f /workspace/pom.xml clean install -s settings.xml"
            //sh "docker container stop \"${containerName}\" "
          }
        }
      }
    }
  }
}
