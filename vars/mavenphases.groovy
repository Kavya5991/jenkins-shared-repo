def call(String repositoryUrl, String projectKey, String sonarToken, String sonarHostUrl) {
  pipeline {
    agent any
    stages {
      stage('Create Docker Image') {
        steps {
          writeFile file: 'Dockerfile', text: '''
            FROM ubuntu:22.04
            RUN apt-get update && apt-get install -y wget openjdk-11-jdk
            RUN apt-get install -y git
            RUN wget -q https://dlcdn.apache.org/maven/maven-3/3.9.3/binaries/apache-maven-3.9.3-bin.tar.gz
            RUN tar xf apache-maven-3.9.3-bin.tar.gz -C /opt
            RUN rm apache-maven-3.9.3-bin.tar.gz
            ENV MAVEN_HOME=/opt/apache-maven-3.9.3
            ENV PATH=$MAVEN_HOME/bin:$PATH
            WORKDIR /workspace
            COPY . /workspace
          '''
        }
      }
      stage('Build Docker Image') {
        steps {
          // Build Docker image
          sh 'docker build -t maven_phases -f Dockerfile .'
        }
      }
      stage('Run Container') {
        steps {
          sh "docker run -dt --rm --name Maven_Phasess maven_phases"
          sh "docker cp settings.xml  Maven_Phasess:/opt/apache-maven-3.9.3/conf/"
          sh "docker exec Maven_Phasess git config --global user.email 'kavyakolla98@gmail.com'"
          sh "docker exec Maven_Phasess git config --global user.name 'Kavya5991'"
        }
        }
      }
      stage("Maven install") {
        steps {
          sh "docker exec Maven_Phasess mvn clean install"
        }
      }
      stage("Code Quality Check") {
        steps {
          sh "docker exec Maven_Phasess mvn clean verify sonar:sonar -Dsonar.projectKey='${projectKey}' -Dsonar.login='${sonarToken}' -Dsonar.host.url='${sonarHostUrl}'"
        }
      }
      stage("Snapshot Deploy") {
        steps {
          sh "docker exec Maven_Phasess mvn deploy -s /opt/apache-maven-3.9.3/conf/settings.xml"
        }
      }
      stage("Release Deploy") {
        steps {
          sh "docker exec Maven_Phasess mvn release:clean release:prepare"
          sh "docker exec Maven_Phasess mvn release:perform"
          sh "docker container stop Maven_Phasess"
        }
      }
    }
  }

