/*
 * See https://github.com/hibernate/hibernate-jenkins-pipeline-helpers
 */
@Library('hibernate-jenkins-pipeline-helpers@1.13') _

// Avoid running the pipeline on branch indexing
if (currentBuild.getBuildCauses().toString().contains('BranchIndexingCause')) {
  	print "INFO: Build skipped due to trigger being Branch Indexing"
	currentBuild.result = 'NOT_BUILT'
  	return
}

pipeline {
    agent {
        label 'Fedora'
    }
    tools {
        jdk 'OpenJDK 11 Latest'
    }
    options {
  		rateLimitBuilds(throttle: [count: 1, durationName: 'hour', userBoost: true])
        buildDiscarder(logRotator(numToKeepStr: '3', artifactNumToKeepStr: '3'))
        disableConcurrentBuilds(abortPrevious: true)
    }
	stages {
        stage('Checkout') {
        	steps {
				checkout scm
			}
		}
		stage('Publish') {
			steps {
				withCredentials([
					usernamePassword(credentialsId: 'ossrh.sonatype.org', usernameVariable: 'hibernatePublishUsername', passwordVariable: 'hibernatePublishPassword'),
					usernamePassword(credentialsId: 'plugins.gradle.org', usernameVariable: 'hibernatePluginPortalUsername', passwordVariable: 'hibernatePluginPortalPassword'),
					string(credentialsId: 'ge.hibernate.org-access-key', variable: 'DEVELOCITY_ACCESS_KEY'),
					string(credentialsId: 'release.gpg.passphrase', variable: 'SIGNING_PASS'),
					file(credentialsId: 'release.gpg.private-key', variable: 'SIGNING_KEYRING')
				]) {
					sh '''./gradlew clean publish \
						-PhibernatePublishUsername=$hibernatePublishUsername \
						-PhibernatePublishPassword=$hibernatePublishPassword \
						-Pgradle.publish.key=$hibernatePluginPortalUsername \
						-Pgradle.publish.secret=$hibernatePluginPortalPassword \
						--no-scan \
						-DsigningPassword=$SIGNING_PASS \
						-DsigningKeyFile=$SIGNING_KEYRING \
					'''
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