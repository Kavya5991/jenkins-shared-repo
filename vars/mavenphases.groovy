def call(String repositoryUrl,String projectKey,String sonarToken,String sonarHostUrl) {
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
          sh 'docker build -t maven_phases-f Dockerfile .'
        }
      }
      stage('Run Container') {
        steps {
          script {
            sh "docker run -dt --rm --name Maven_phases maven_phases"
            sh "docker exec Maven_phases mkdir /workspace"
            sh "docker cp pom.xml Maven_phases:/workspace/"
            sh "docker cp settings.xml  Maven_phases:/opt/apache-maven-3.9.3/conf/"
            sh "docker exec  Maven_phases mvn -f /workspace/pom.xml --version"
          }
        }
      }
      stage("Maven install"){
        steps{
          script {
            sh "docker exec  Maven_phases mvn -f /workspace/pom.xml clean install -s /opt/apache-maven-3.9.3/conf/settings.xml"
          }
        }
      }
       stage("Code Quality Check"){
        steps{
          script {  
            sh "docker exec  Maven_phases mvn -f /workspace/pom.xml clean verify sonar:sonar -Dsonar.projectKey="${projectKey}" -Dsonar.login="${sonarToken}" -Dsonar.host.url="{sonarHostUrl}""
          }
        }
       }
       stage("Snapshot Deploy"){
        steps{
          script {
            sh "docker exec  Maven_phases mvn -f /workspace/pom.xml deploy -s /opt/apache-maven-3.9.3/conf/settings.xml"
          }
        }
       }
       stage("Release Deploy"){
        steps{
          script {
            sh "docker exec  Maven_phases mvn -f /workspace/pom.xml release:clean release:prepare -s /opt/apache-maven-3.9.3/conf/settings.xml"
            sh "docker exec  Maven_phases mvn -f /workspace/pom.xml release:perform -s /opt/apache-maven-3.9.3/conf/settings.xml"
            sh "docker container stop Maven_phases "
          }
        }
       }
  }
  }
}
