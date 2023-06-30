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
            sh "docker run -dt --rm --name Maven_Docker_custom maven_generic_final"
            sh "docker exec Maven_Docker_custom mkdir /workspace"
            sh "docker cp pom.xml  Maven_Docker_custom :/workspace/"
            sh "docker cp settings.xml  Maven_Docker_custom:/opt/apache-maven-3.9.3/conf/"
            sh "docker exec  Maven_Docker_custom mvn -f /workspace/pom.xml --version"
            sh "docker exec  Maven_Docker_custom mvn -f /workspace/pom.xml clean install -s /opt/apache-maven-3.9.3/conf/settings.xml"
            sh "docker container stop \"${containerName}\" "
          }
        }
      }
    }
  }
}
