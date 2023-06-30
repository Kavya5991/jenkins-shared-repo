def call(String repositoryUrl){
pipeline {
    agent any
    stages {
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
                    ADD "${repositoryUrl}" $MAVEN_HOME
                    RUN mvn --version
                '''
            }
        }
         stage('Build Docker Image') {
             steps {
                     // Build Docker image
                sh 'docker build -t maven_generic -f Dockerfile .'
            }
        }
      stage('Code Checkout') {
        steps {
          git branch:'main',url:"${repositoryUrl}"
        }
      }
        stage('Run Container') {
            steps {
                script
                {
                    sh 'docker run -dt --rm --name maven_docker_Pipeline_Generic maven_generic'
                    sh 'docker exec maven_docker_Pipeline_Generic mvn -version'
                    sh 'docker exec maven_docker_Pipeline_Generic mvn clean install'
                    
                  

                }    
            }
        }
       
    }
}
}
