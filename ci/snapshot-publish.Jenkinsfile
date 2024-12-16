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
        label 'Release'
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
					usernamePassword(credentialsId: 'ossrh.sonatype.org', passwordVariable: 'OSSRH_PASSWORD', usernameVariable: 'OSSRH_USER'),
					usernamePassword(credentialsId: 'gradle-plugin-portal-api-key', passwordVariable: 'PLUGIN_PORTAL_PASSWORD', usernameVariable: 'PLUGIN_PORTAL_USERNAME'),
					file(credentialsId: 'release.gpg.private-key', variable: 'RELEASE_GPG_PRIVATE_KEY_PATH'),
					string(credentialsId: 'release.gpg.passphrase', variable: 'RELEASE_GPG_PASSPHRASE'),
					// https://github.com/gradle-nexus/publish-plugin#publishing-to-maven-central-via-sonatype-ossrh
					usernamePassword(credentialsId: 'ossrh.sonatype.org', passwordVariable: 'ORG_GRADLE_PROJECT_sonatypePassword', usernameVariable: 'ORG_GRADLE_PROJECT_sonatypeUsername'),
					// https://docs.gradle.org/current/userguide/publishing_gradle_plugins.html#account_setup
					usernamePassword(credentialsId: 'gradle-plugin-portal-api-key', passwordVariable: 'GRADLE_PUBLISH_SECRET', usernameVariable: 'GRADLE_PUBLISH_KEY'),
					file(credentialsId: 'release.gpg.private-key', variable: 'SIGNING_GPG_PRIVATE_KEY_PATH'),
					string(credentialsId: 'release.gpg.passphrase', variable: 'SIGNING_GPG_PASSPHRASE')
				]) {
					withEnv([
							"DISABLE_REMOTE_GRADLE_CACHE=true"
					]) {
						sh '''./gradlew clean publish -x test \
						--no-scan --no-daemon --no-build-cache --stacktrace
						'''
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