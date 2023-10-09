@Library('hibernate-jenkins-pipeline-helpers@1.5') _

// Avoid running the pipeline on branch indexing
if (currentBuild.getBuildCauses().toString().contains('BranchIndexingCause')) {
  	print "INFO: Build skipped due to trigger being Branch Indexing"
	currentBuild.result = 'NOT_BUILT'
  	return
}

pipeline {
    agent {
        label 'LongDuration'
    }
    tools {
        jdk 'OpenJDK 17 Latest'
    }
    options {
        buildDiscarder(logRotator(numToKeepStr: '3', artifactNumToKeepStr: '3'))
        disableConcurrentBuilds(abortPrevious: true)
    }
	environment {
		MAVEN_OPTS = '-Xmx2g -XX:MaxMetaspaceSize=1g'
	}
    stages {
        stage('Build') {
        	steps {
				script {
					sh './gradlew publishToMavenLocal -PmavenMirror=nexus-load-balancer-c4cf05fd92f43ef8.elb.us-east-1.amazonaws.com --no-daemon'
					script {
						env.HIBERNATE_VERSION = sh (
								script: "grep hibernateVersion gradle/version.properties|cut -d'=' -f2",
								returnStdout: true
						).trim()
					}
					dir('.release/quarkus') {
// 						checkout scmGit(branches: [[name: '*/orm-update']], extensions: [], userRemoteConfigs: [[credentialsId: 'ed25519.Hibernate-CI.github.com', url: 'https://github.com/beikov/quarkus.git']])
						checkout scmGit(branches: [[name: '*/main']], extensions: [], userRemoteConfigs: [[credentialsId: 'ed25519.Hibernate-CI.github.com', url: 'https://github.com/quarkusio/quarkus.git']])
						sh "sed -i 's@<hibernate-orm.version>.*</hibernate-orm.version>@<hibernate-orm.version>${env.HIBERNATE_VERSION}</hibernate-orm.version>@' bom/application/pom.xml"
						sh './mvnw -Dquickly install'
						sh './mvnw -pl :quarkus-hibernate-orm -amd -pl "!integration-tests/kafka-oauth-keycloak" verify -Dstart-containers -Dtest-containers'
					}
				}
			}
		}
    }
    post {
        always {
    		configFileProvider([configFile(fileId: 'job-configuration.yaml', variable: 'JOB_CONFIGURATION_FILE')]) {
            	notifyBuildResult maintainers: (String) readYaml(file: env.JOB_CONFIGURATION_FILE).notification?.email?.recipients
            }
        }
    }
}