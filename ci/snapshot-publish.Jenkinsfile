@Library('hibernate-jenkins-pipeline-helpers@1.5') _

// Avoid running the pipeline on branch indexing
if (currentBuild.getBuildCauses().toString().contains('BranchIndexingCause')) {
  print "INFO: Build skipped due to trigger being Branch Indexing"
  currentBuild.result = 'ABORTED'
  return
}

this.helper = new JobHelper(this)

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
    }
    triggers {
      cron 'H * * * *'
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
					string(credentialsId: 'ge.hibernate.org-access-key', variable: 'GRADLE_ENTERPRISE_ACCESS_KEY'),
					string(credentialsId: 'release.gpg.passphrase', variable: 'SIGNING_PASS'),
					file(credentialsId: 'release.gpg.private-key', variable: 'SIGNING_KEYRING')
				]) {
					sh '''./gradlew clean publish \
						-PhibernatePublishUsername=$hibernatePublishUsername \
						-PhibernatePublishPassword=$hibernatePublishPassword \
						-PhibernatePluginPortalUsername=$hibernatePluginPortalUsername \
						-PhibernatePluginPortalPassword=$hibernatePluginPortalPassword \
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
            notifyBuildResult
        }
    }
}