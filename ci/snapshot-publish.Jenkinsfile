/*
 * See https://github.com/hibernate/hibernate-jenkins-pipeline-helpers
 */
@Library('hibernate-jenkins-pipeline-helpers') _

// Avoid running the pipeline on branch indexing
if (currentBuild.getBuildCauses().toString().contains('BranchIndexingCause')) {
  	print "INFO: Build skipped due to trigger being Branch Indexing"
	currentBuild.result = 'NOT_BUILT'
  	return
}

def checkoutReleaseScripts() {
    dir('.release/scripts') {
        checkout scmGit(branches: [[name: '*/main']], extensions: [],
                userRemoteConfigs: [[credentialsId: 'ed25519.Hibernate-CI.github.com',
                                     url: 'https://github.com/hibernate/hibernate-release-scripts.git']])
    }
}

pipeline {
    agent {
        label 'Release'
    }
    tools {
        jdk 'OpenJDK 17 Latest'
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
                script {
                    withCredentials([
                            // https://github.com/gradle-nexus/publish-plugin#publishing-to-maven-central-via-sonatype-ossrh
                            // TODO: HHH-19309:
                            //  Once we switch to maven-central publishing (from nexus2) we need to add a new credentials
                            //  to use the following env variable names to set the user/password:
                            //  - JRELEASER_MAVENCENTRAL_USERNAME
                            //  - JRELEASER_MAVENCENTRAL_TOKEN
                            //  Also use the new `credentialsId` for Maven Central, e.g.:
                            //  usernamePassword(credentialsId: '???????', passwordVariable: 'JRELEASER_MAVENCENTRAL_TOKEN', usernameVariable: 'JRELEASER_MAVENCENTRAL_USERNAME'),
                            usernamePassword(credentialsId: 'ossrh.sonatype.org', passwordVariable: 'JRELEASER_NEXUS2_PASSWORD', usernameVariable: 'JRELEASER_NEXUS2_USERNAME'),
                            string(credentialsId: 'Hibernate-CI.github.com', variable: 'JRELEASER_GITHUB_TOKEN'),
                            // https://docs.gradle.org/current/userguide/publishing_gradle_plugins.html#account_setup
                            usernamePassword(credentialsId: 'gradle-plugin-portal-api-key', passwordVariable: 'GRADLE_PUBLISH_SECRET', usernameVariable: 'GRADLE_PUBLISH_KEY'),
                            gitUsernamePassword(credentialsId: 'username-and-token.Hibernate-CI.github.com', gitToolName: 'Default')
                    ]) {
                        withEnv([
                                "DISABLE_REMOTE_GRADLE_CACHE=true"
                        ]) {
                            checkoutReleaseScripts()
                            def version = sh(
                                    script: ".release/scripts/determine-current-version.sh orm",
                                    returnStdout: true
                            ).trim()
                            echo "Current version: '${version}'"
                            sh "bash -xe .release/scripts/snapshot-deploy.sh orm ${version}"
                        }
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
