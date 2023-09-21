@Library('hibernate-jenkins-pipeline-helpers@1.5') _

// Avoid running the pipeline on branch indexing
if (currentBuild.getBuildCauses().toString().contains('BranchIndexingCause')) {
  print "INFO: Build skipped due to trigger being Branch Indexing"
  currentBuild.result = 'ABORTED'
  return
}

pipeline {
    agent {
        label 'Worker&&Containers'
    }
    tools {
        jdk 'OpenJDK 11 Latest'
    }
    options {
  		rateLimitBuilds(throttle: [count: 1, durationName: 'day', userBoost: true])
        buildDiscarder(logRotator(numToKeepStr: '3', artifactNumToKeepStr: '3'))
        disableConcurrentBuilds(abortPrevious: true)
    }
    parameters {
        string(name: 'RELEASE_VERSION', defaultValue: '', description: 'The version to be released, e.g. 5.4.16.Final')
        string(name: 'DEVELOPMENT_VERSION', defaultValue: '', description: 'The next version to be used after the release, e.g. 5.4.17-SNAPSHOT')
		booleanParam(name: 'RELEASE_DRY_RUN', defaultValue: true, description: 'If true, just simulate the release, without pushing any commits or tags, and without uploading any artifacts or documentation.')
	}
    stages {
        stage('Release') {
        	steps {
				script {
					def JIRA_KEY = "HHH"
					def JIRA_PROJECT_ID = 10031
					def JIRA_CLOSE_TRANSITION_ID = 2
					def JIRA_REOPEN_TRANSITION_ID = 3
					// todo: API token
					def JIRA_API_TOKEN = 'email@example.com:<api_token>'
					def GITHUB_API_TOKEN = 'Bearer <YOUR-TOKEN>'

					dir('hibernate') {
						checkout scm
						// Determine version information for release process
						env.currentVersion = sh (
							script: "grep hibernateVersion gradle/version.properties|cut -d'=' -f2",
							returnStdout: true
						).trim()
						if ( params.RELEASE_VERSION == null || params.RELEASE_VERSION.isEmpty() ) {
							env.releaseVersion = env.currentVersion.substring( 0, env.currentVersion.indexOf( '-SNAPSHOT' ) ) + ".Final"
						}
						else {
							env.releaseVersion = params.RELEASE_VERSION
						}
						if ( params.DEVELOPMENT_VERSION == null || params.DEVELOPMENT_VERSION.isEmpty() ) {
							def baseVersion = env.releaseVersion.substring( 0, env.releaseVersion.indexOf( '.Final' ) )
							def dotIndex = baseVersion.lastIndexOf( '.' )
							def nextMicro = Integer.parseInt( baseVersion.substring( dotIndex + 1 ) ) + 1
							env.developmentVersion = baseVersion.substring( 0, dotIndex + 1 ) + nextMicro + "-SNAPSHOT"
						}
						else {
							env.developmentVersion = params.DEVELOPMENT_VERSION
						}
						env.versionBasis = env.releaseVersion.substring( 0, env.releaseVersion.indexOf( '.Final' ) )
						env.versionFamily = env.versionBasis.substring( 0, env.versionBasis.lastIndexOf( '.' ) )
						env.nextVersionBasis = env.developmentVersion.substring( 0, env.developmentVersion.indexOf( '-SNAPSHOT' ) )
						echo "Workspace version: ${env.currentVersion}"
						echo "Release version: ${env.releaseVersion}"
						echo "Development version: ${env.developmentVersion}"
						echo "Version family: ${env.versionFamily}"

						// Check if this commit hash succeeds tests
						// See https://docs.github.com/en/rest/actions/workflow-runs?apiVersion=2022-11-28#list-workflow-runs-for-a-repository
						// todo: this is just temporary, remove the ~1
// 						env.commitHash = sh(script: 'git rev-parse HEAD~1', returnStdout: true)
// 						def githubResponse = sh(script: "curl -L -H \"Accept: application/vnd.github+json\" -H \"X-GitHub-Api-Version: 2022-11-28\" \"https://api.github.com/repos/hibernate/hibernate-orm/actions/runs?branch=${env.versionFamily}&exclude_pull_requests=true&event=push\"", returnStdout: true)
// 						def githubResponseObject = readJSON text: githubResponse
// 						def workflowSuccess = false
//
// 						for ( int i = 0; i < githubResponseObject.workflow_runs.size(); i++ ) {
// 							def workflowRun = githubResponseObject.workflow_runs[i]
// 							if ( workflowRun.head_sha == env.commitHash && workflowRun.path == '.github/workflows/contributor-build.yml' ) {
// 								echo "Workflow run for commit conclusion: ${workflowRun.conclusion}"
// 								workflowSuccess = workflowRun.conclusion == 'success'
// 								break
// 							}
// 						}
// 						if ( !workflowSuccess ) {
// 							error( "Release failed because workflow run for commit ${env.commitHash} has not completed yet or was not successful. See logs" )
// 						}

						// Check if Jira version exists
						def jiraVersionsResponse = sh(script: "curl -L \"https://hibernate.atlassian.net/rest/api/2/project/${JIRA_KEY}/version?status=unreleased\"", returnStdout: true)
						def jiraVersionsResponseObject = readJSON text: jiraVersionsResponse
						env.jiraVersionId = null

						for ( int i = 0; i < jiraVersionsResponseObject.values.size(); i++ ) {
							def jiraVersion = jiraVersionsResponseObject.values[i]
							if ( jiraVersion.name == env.versionBasis ) {
								env.jiraVersionId = jiraVersion.id
								break
							}
						}
						if ( env.jiraVersionId == null ) {
							error( "Release failed because Jira version ${env.releaseVersion} does not exist" )
						}

						configFileProvider([configFile(fileId: 'release.config.ssh', targetLocation: '$HOME/.ssh/config'), configFile(fileId: 'release.config.ssh.knownhosts', targetLocation: '$HOME/.ssh/known_hosts')]) {
							withCredentials([
								usernamePassword(credentialsId: 'ossrh.sonatype.org', passwordVariable: 'OSSRH_PASSWORD', usernameVariable: 'OSSRH_USER'),
								usernamePassword(credentialsId: 'gradle-plugin-portal-api-key', passwordVariable: 'PLUGIN_PORTAL_PASSWORD', usernameVariable: 'PLUGIN_PORTAL_USERNAME'),
								file(credentialsId: 'release.gpg.private-key', variable: 'SIGNING_KEYRING'), string(credentialsId: 'release.gpg.passphrase', variable: 'SIGNING_PASS')
							]) {
								sshagent(['ed25519.Hibernate-CI.github.com', 'hibernate.filemgmt.jboss.org', 'hibernate-ci.frs.sourceforge.net']) {
									// set release version
									// update changelog from JIRA
									// tags the version
									// changes the version to the provided development version
									sh """
										./gradlew clean :release:gitPreparationForReleaseTask -x test --no-scan \
											-PreleaseVersion=${env.releaseVersion} -PdevelopmentVersion=${env.developmentVersion} -PgitRemote=origin -PgitBranch=${env.versionFamily} \
											-PSONATYPE_OSSRH_USER=$OSSRH_USER -PSONATYPE_OSSRH_PASSWORD=$OSSRH_PASSWORD \
											-Pgradle.publish.key=$PLUGIN_PORTAL_USERNAME -Pgradle.publish.secret=$PLUGIN_PORTAL_PASSWORD \
											-PhibernatePublishUsername=$OSSRH_USER -PhibernatePublishPassword=$OSSRH_PASSWORD \
											-DsigningPassword=$SIGNING_PASS -DsigningKeyFile=$SIGNING_KEYRING"""
								}
							}
						}
					}

					dir('in.relation.to') {
						checkout scmGit(branches: [[name: '*/production']], extensions: [], userRemoteConfigs: [[credentialsId: 'ed25519.Hibernate-CI.github.com', url: 'https://github.com/hibernate/in.relation.to.git']])
						def releaseDate = java.time.LocalDate.now().format( java.time.format.DateTimeFormatter.ofPattern( "yyyy/MM/dd" ) )
						def simpleVersion = env.versionBasis.replace(".", "")
						def announcementName = "hibernate-orm-${simpleVersion}-final"
						def announcementText = """
                        = "Hibernate ORM ${env.releaseVersion} released"
                        Bot
                        :awestruct-tags: ["Hibernate ORM", "Releases"]
                        :awestruct-layout: blog-post
						:family: ${env.versionFamily}
                        :version: ${env.releaseVersion}
                        :docs-url: https://docs.jboss.org/hibernate/orm/{family}
                        :javadocs-url: {docs-url}/javadocs
                        :migration-guide-url: {docs-url}/migration-guide/migration-guide.html
                        :intro-guide-url: {docs-url}/introduction/html_single/Hibernate_Introduction.html
                        :user-guide-url: {docs-url}/userguide/html_single/Hibernate_User_Guide.html

                        Today, we published a new maintenance release of Hibernate ORM ${env.versionFamily}: {version}.

                        == What's new

                        This release introduces a few minor improvements as well as bug fixes.

                        You can find the full list of {version} changes https://hibernate.atlassian.net/issues/?jql=project%20%3D%20HHH%20AND%20fixVersion%20%3D%20${env.releaseVersion}[here].

                        == Conclusion

                        For additional details, see:

                        - the https://hibernate.org/orm/releases/${env.versionFamily}/[release page]
                        - the link:{migration-guide-url}[Migration Guide]
                        - the link:{intro-guide-url}[Introduction Guide]
                        - the link:{user-guide-url}[User Guide]

                        See also the following resources related to supported APIs:

                        - the https://hibernate.org/community/compatibility-policy/[compatibility policy]
                        - the link:{docs-url}/incubating/incubating.txt[incubating API report] (`@Incubating`)
                        - the link:{docs-url}/deprecated/deprecated.txt[deprecated API report] (`@Deprecated` + `@Remove`)
                        - the link:{docs-url}/internals/internal.txt[internal API report] (internal packages, `@Internal`)

                        Visit the https://hibernate.org/community/[website] for details on getting in touch with us.""".stripIndent()
						dir('posts/Bot') {
							def fileName = releaseDate.replace("/","-") + "-" + announcementName + ".adoc"
							writeFile file: fileName, text: announcementText
							sh "git add ${fileName}"
							sh "git commit -m '[ORM] ${env.releaseVersion}'"
						}
						env.inRelationToLink = "https://in.relation.to/${releaseDate}/${announcementName}/"
					}
					dir('hibernate.org') {
						checkout scmGit(branches: [[name: '*/production']], extensions: [], userRemoteConfigs: [[credentialsId: 'ed25519.Hibernate-CI.github.com', url: 'https://github.com/hibernate/hibernate.org.git']])
						dir("_data/projects/orm/releases/${env.versionFamily}") {
							def fileName = "${env.releaseVersion}.yml"
							def releaseYmlContent = """
							date: ${releaseDate.replace("/","-")}
                            announcement_url: ${env.inRelationToLink}

                            summary: bug fixes""".stripIndent()
							writeFile file: fileName, text: releaseYmlContent
							sh "git add ${fileName}"
							sh "git commit -m '[ORM] ${env.releaseVersion}'"
						}
					}

					if ( params.RELEASE_DRY_RUN ) {
						echo "Would close Jira version: ${env.versionBasis}"
					}
					else {
						//  Mark the version as released in Jira
						final def (String jiraReleaseResponse, int jiraReleaseResponseCode) =
							sh(script: "curl -L -s -w '\n%{response_code}' -X PUT -u '${JIRA_API_TOKEN}' -H 'Accept: application/json' -H 'Content-Type: application/json' -d '{\"released\": true}' 'https://hibernate.atlassian.net/rest/api/2/version/${env.jiraVersionId}'", returnStdout: true)
								.trim()
								.tokenize( "\n" )
						if ( jiraReleaseResponseCode != 200 ) {
							echo jiraReleaseResponse
							error( "Release failed because Jira version ${env.versionBasis} could not be released" )
						}
					}

					if ( params.RELEASE_DRY_RUN ) {
						echo "Would create new Jira version: ${env.nextVersionBasis}"
					}
					else {
						//  Create next version
						final def (String jiraCreateVersionResponse, int jiraCreateVersionResponseCode) =
							sh(script: "curl -L -s -w '\n%{response_code}' -X POST -u '${JIRA_API_TOKEN}' -H 'Accept: application/json' -H 'Content-Type: application/json' -d '{\"name\": \"${env.nextVersionBasis}\", \"projectId\":${JIRA_PROJECT_ID}' 'https://hibernate.atlassian.net/rest/api/2/version'", returnStdout: true)
								.trim()
								.tokenize( "\n" )
						if ( jiraCreateVersionResponseCode != 201 ) {
							echo jiraCreateVersionResponse
							error( "Release failed because Jira version ${env.nextVersionBasis} could not be created" )
						}
					}

					// REST URL used for getting all issues of given release - see https://docs.atlassian.com/jira/REST/latest/#d2e2450
					def jiraIssuesResponse = sh(script: "curl -L \"https://hibernate.atlassian.net/rest/api/2/search/?jql=project%20%3D%20${JIRA_KEY}%20AND%20fixVersion%20%3D%20${env.releaseVersion}%20ORDER%20BY%20issuetype%20ASC&fields=issuetype,summary&maxResults=200\"", returnStdout: true)
					def jiraIssuesResponseObject = readJSON text: jiraIssuesResponse

					// Close resolved issues. Remove fix version from Done non-resolved issues.
					// Move issues Undone non-resolved issues to next version.
					for ( int i = 0; i < jiraIssuesResponseObject.issues.size(); i++ ) {
						def jiraIssueResponse = sh(script: "curl -L \"${jiraIssuesResponseObject.issues[i].self}\"", returnStdout: true)
						def jiraIssue = readJSON text: jiraIssueResponse
						if ( jiraIssue.fields.status.name == 'Resolved' ) {
							if ( params.RELEASE_DRY_RUN ) {
								echo "Would close Jira issue: ${jiraIssue.key}"
							}
							else {
								// Close issue
								final def (String jiraCloseIssueResponse, int jiraCloseIssueResponseCode) =
									sh(script: "curl -L -s -w '\n%{response_code}' -X POST -u '${JIRA_API_TOKEN}' -H 'Accept: application/json' -H 'Content-Type: application/json' -d '{\"transition\":{\"id\":\"${JIRA_CLOSE_TRANSITION_ID}\"}}' 'https://hibernate.atlassian.net/rest/api/2/issue/${jiraIssue.id}/transitions'", returnStdout: true)
										.trim()
										.tokenize( "\n" )
								if ( jiraCloseIssueResponseCode != 201 ) {
									echo jiraCloseIssueResponse
									error( "Release failed because Jira issue ${jiraIssue.key} could not be closed" )
								}
							}
						}
						else if ( jiraIssue.fields.status.name == 'Closed' ) {
							if ( jiraIssue.fields.resolution.name != 'Fixed' ) {
								if ( params.RELEASE_DRY_RUN ) {
									echo "Would remove fix version from Jira issue: ${jiraIssue.key}"
								}
								else {
									// Reopen issue to remove fix version on close
									final def (String jiraReopenIssueResponse, int jiraReopenIssueResponseCode) =
										sh(script: "curl -L -s -w '\n%{response_code}' -X POST -u '${JIRA_API_TOKEN}' -H 'Accept: application/json' -H 'Content-Type: application/json' -d '{\"transition\":{\"id\":\"${JIRA_REOPEN_TRANSITION_ID}\"}}' 'https://hibernate.atlassian.net/rest/api/2/issue/${jiraIssue.id}/transitions'", returnStdout: true)
											.trim()
											.tokenize( "\n" )
									if ( jiraReopenIssueResponseCode != 201 ) {
										echo jiraReopenIssueResponse
										error( "Release failed because Jira issue ${jiraIssue.key} could not be reopened" )
									}

									final def (String jiraCloseIssueResponse, int jiraCloseIssueResponseCode) =
										sh(script: "curl -L -s -w '\n%{response_code}' -X POST -u '${JIRA_API_TOKEN}' -H 'Accept: application/json' -H 'Content-Type: application/json' -d '{\"transition\":{\"id\":\"${JIRA_CLOSE_TRANSITION_ID}\"},\"update\":{\"fixVersions\":[{\"remove\":\"${env.versionBasis}\"}]},\"fields\":{\"resolution\":{\"name\":\"${jiraIssue.fields.resolution.name}\"}}}' 'https://hibernate.atlassian.net/rest/api/2/issue/${jiraIssue.id}/transitions'", returnStdout: true)
											.trim()
											.tokenize( "\n" )
									if ( jiraCloseIssueResponseCode != 201 ) {
										echo jiraCloseIssueResponse
										error( "Release failed because Jira issue ${jiraIssue.key} could not be closed" )
									}
								}
							}
							else {
								// Otherwise ignore the issue
								if ( params.RELEASE_DRY_RUN ) {
									echo "Would ignore already closed Jira issue: ${jiraIssue.key}"
								}
							}
						}
						else {
							if ( params.RELEASE_DRY_RUN ) {
								echo "Would move Jira issue to next fix version: ${jiraIssue.key}"
							}
							else {
								// Move issue to next fix version
								final def (String jiraMoveIssueResponse, int jiraMoveIssueResponseCode) =
									sh(script: "curl -L -s -w '\n%{response_code}' -X PUT -u '${JIRA_API_TOKEN}' -H 'Accept: application/json' -H 'Content-Type: application/json' -d '{\"update\":{\"fixVersions\":[{\"remove\":\"${env.versionBasis}\"},{\"add\":\"${env.nextVersionBasis}\"}]}}' 'https://hibernate.atlassian.net/rest/api/2/issue/${jiraIssue.id}'", returnStdout: true)
										.trim()
										.tokenize( "\n" )
								if ( jiraMoveIssueResponseCode != 201 ) {
									echo jiraMoveIssueResponse
									error( "Release failed because Jira issue ${jiraIssue.key} could not be moved to next version" )
								}
							}
						}
					}

					configFileProvider([configFile(fileId: 'release.config.ssh', targetLocation: '$HOME/.ssh/config'), configFile(fileId: 'release.config.ssh.knownhosts', targetLocation: '$HOME/.ssh/known_hosts')]) {
						withCredentials([
							usernamePassword(credentialsId: 'ossrh.sonatype.org', passwordVariable: 'OSSRH_PASSWORD', usernameVariable: 'OSSRH_USER'),
							usernamePassword(credentialsId: 'gradle-plugin-portal-api-key', passwordVariable: 'PLUGIN_PORTAL_PASSWORD', usernameVariable: 'PLUGIN_PORTAL_USERNAME'),
							file(credentialsId: 'release.gpg.private-key', variable: 'SIGNING_KEYRING'), string(credentialsId: 'release.gpg.passphrase', variable: 'SIGNING_PASS')
						]) {
							sshagent(['ed25519.Hibernate-CI.github.com', 'hibernate.filemgmt.jboss.org', 'hibernate-ci.frs.sourceforge.net']) {
								dir('in.relation.to') {
									if ( params.RELEASE_DRY_RUN ) {
										echo "Would push in.relation.to changes to production branch"
									}
									else {
										sh "git push"
									}
								}
								dir('hibernate.org') {
									if ( params.RELEASE_DRY_RUN ) {
										echo "Would push hibernate.org changes to production branch"
									}
									else {
										sh "git push"
									}
								}
								dir('hibernate') {
									if ( params.RELEASE_DRY_RUN ) {
										echo "Would push to GitHub and publish documentation and Sonatype release"
									}
									else {
										// performs documentation upload and Sonatype release
										// push to github
									sh """
										./gradlew ciRelease closeAndReleaseSonatypeStagingRepository -x test --no-scan \
											-PreleaseVersion=${env.releaseVersion} -PdevelopmentVersion=${env.developmentVersion} -PgitRemote=origin -PgitBranch=${env.versionFamily} \
											-PSONATYPE_OSSRH_USER=$OSSRH_USER -PSONATYPE_OSSRH_PASSWORD=$OSSRH_PASSWORD \
											-Pgradle.publish.key=$PLUGIN_PORTAL_USERNAME -Pgradle.publish.secret=$PLUGIN_PORTAL_PASSWORD \
											-PhibernatePublishUsername=$OSSRH_USER -PhibernatePublishPassword=$OSSRH_PASSWORD \
											-DsigningPassword=$SIGNING_PASS -DsigningKeyFile=$SIGNING_KEYRING"""
									}

									if ( params.RELEASE_DRY_RUN ) {
										echo "Would create a GitHub release"
									}
									else {
										// create GitHub release
										def releaseBody = "Read more about it on our blog: ${env.inRelationToLink}"
										final def (String githubCreateReleaseResponse, int githubCreateReleaseResponseCode) =
											sh(script: "curl -L -s -w '\n%{response_code}' -X POST -H \"Accept: application/vnd.github+json\" -H \"X-GitHub-Api-Version: 2022-11-28\" -u '${GITHUB_API_TOKEN}' -d '{\"tag_name\":\"${env.versionBasis}\",\"name\":\"Hibernate ORM ${env.versionBasis}\",\"make_latest\":\"legacy\",\"body\":\"${releaseBody}\"}' 'https://api.github.com/repos/hibernate/hibernate-orm/releases'", returnStdout: true)
												.trim()
												.tokenize( "\n" )
										if ( githubCreateReleaseResponseCode != 201 ) {
											echo githubCreateReleaseResponse
											error( "Release failed because GitHub release could not be created" )
										}
									}
								}

								def discoursePostTitle = "Hibernate ORM ${env.releaseVersion} has been released"
								def discoursePostBody = "Read more about it on our blog: ${env.inRelationToLink}"
								if ( params.RELEASE_DRY_RUN ) {
									echo "Would create a Discourse release announcement: ${discoursePostTitle}. ${discoursePostBody}"
								}
								else {
									// todo: create discourse post: https://docs.discourse.org/#tag/Posts/operation/createTopicPostPM
								}

								def xPost = "#HibernateORM ${env.releaseVersion} has been released. Read more about it on our blog: ${env.inRelationToLink}"
								if ( params.RELEASE_DRY_RUN ) {
									echo "Would create an X release announcement: ${xPost}"
								}
								else {
									// todo: create X post: https://developer.twitter.com/en/docs/twitter-api/tweets/manage-tweets/api-reference/post-tweets
								}

								// todo: Possibly update "current" symlink on the documentation server - only for a new minor or major release
							}
						}
					}
				}
			}
        }
    }
//     post {
//         always {
//     		configFileProvider([configFile(fileId: 'job-configuration.yaml', variable: 'JOB_CONFIGURATION_FILE')]) {
//             	notifyBuildResult maintainers: (String) readYaml(file: env.JOB_CONFIGURATION_FILE).notification?.email?.recipients
//             }
//         }
//     }
}