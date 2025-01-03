/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

import groovy.transform.Field
import io.jenkins.blueocean.rest.impl.pipeline.PipelineNodeGraphVisitor
import io.jenkins.blueocean.rest.impl.pipeline.FlowNodeWrapper
import org.jenkinsci.plugins.workflow.support.steps.build.RunWrapper

/*
 * See https://github.com/hibernate/hibernate-jenkins-pipeline-helpers
 */
@Library('hibernate-jenkins-pipeline-helpers@1.13') _
import org.hibernate.jenkins.pipeline.helpers.job.JobHelper

@Field final String DEFAULT_JDK_VERSION = '11'
@Field final String DEFAULT_JDK_TOOL = "OpenJDK ${DEFAULT_JDK_VERSION} Latest"
@Field final String NODE_PATTERN_BASE = 'Worker&&Containers'
@Field List<BuildEnvironment> environments

this.helper = new JobHelper(this)

helper.runWithNotification {
stage('Configure') {
	requireApprovalForPullRequest 'hibernate'

	this.environments = [
		new BuildEnvironment( node: 's390x' ),
		new BuildEnvironment( dbName: 'sybase_jconn' ),
		new BuildEnvironment( testJdkVersion: '17' ),
		new BuildEnvironment( testJdkVersion: '21' ),
		// We want to enable preview features when testing newer builds of OpenJDK:
		// even if we don't use these features, just enabling them can cause side effects
		// and it's useful to test that.
		new BuildEnvironment( testJdkVersion: '23', testJdkLauncherArgs: '--enable-preview' ),
		new BuildEnvironment( testJdkVersion: '24', testJdkLauncherArgs: '--enable-preview' ),
		// The following JDKs aren't supported by Hibernate ORM out-of-the box yet:
		// they require the use of -Dnet.bytebuddy.experimental=true.
		// Make sure to remove that argument as soon as possible
		// -- generally that requires upgrading bytebuddy after the JDK goes GA.
		new BuildEnvironment( testJdkVersion: '25', testJdkLauncherArgs: '--enable-preview -Dnet.bytebuddy.experimental=true' )
	];

	helper.configure {
		file 'job-configuration.yaml'
		// We don't require the following, but the build helper plugin apparently does
		jdk {
			defaultTool DEFAULT_JDK_TOOL
		}
		maven {
			defaultTool 'Apache Maven 3.8'
		}
	}
	properties([
			buildDiscarder(
					logRotator(daysToKeepStr: '30', numToKeepStr: '10')
			),
			// If two builds are about the same branch or pull request,
			// the older one will be aborted when the newer one starts.
			disableConcurrentBuilds(abortPrevious: true),
			helper.generateNotificationProperty()
	])
}

// Avoid running the pipeline on branch indexing
if (currentBuild.getBuildCauses().toString().contains('BranchIndexingCause')) {
  	print "INFO: Build skipped due to trigger being Branch Indexing"
	currentBuild.result = 'NOT_BUILT'
  	return
}
// This is a limited maintenance branch, so don't run this on pushes to the branch, only on PRs
if ( !env.CHANGE_ID ) {
	print "INFO: Build skipped because this job should only run for pull request, not for branch pushes"
	currentBuild.result = 'NOT_BUILT'
	return
}

stage('Build') {
	Map<String, Closure> executions = [:]
	Map<String, Map<String, String>> state = [:]
	environments.each { BuildEnvironment buildEnv ->
		state[buildEnv.tag] = [:]
		executions.put(buildEnv.tag, {
			runBuildOnNode(buildEnv.node ?: NODE_PATTERN_BASE) {
				def testJavaHome
				if ( buildEnv.testJdkVersion ) {
					testJavaHome = tool(name: "OpenJDK ${buildEnv.testJdkVersion} Latest", type: 'jdk')
				}
				def javaHome = tool(name: DEFAULT_JDK_TOOL, type: 'jdk')
				// Use withEnv instead of setting env directly, as that is global!
				// See https://github.com/jenkinsci/pipeline-plugin/blob/master/TUTORIAL.md
				withEnv(["JAVA_HOME=${javaHome}", "PATH+JAVA=${javaHome}/bin"]) {
					state[buildEnv.tag]['additionalOptions'] = '-PmavenMirror=nexus-load-balancer-c4cf05fd92f43ef8.elb.us-east-1.amazonaws.com'
					if ( testJavaHome ) {
						state[buildEnv.tag]['additionalOptions'] = state[buildEnv.tag]['additionalOptions'] +
								" -Ptest.jdk.version=${buildEnv.testJdkVersion} -Porg.gradle.java.installations.paths=${javaHome},${testJavaHome}"
					}
					if ( buildEnv.testJdkLauncherArgs ) {
						state[buildEnv.tag]['additionalOptions'] = state[buildEnv.tag]['additionalOptions'] +
								" -Ptest.jdk.launcher.args='${buildEnv.testJdkLauncherArgs}'"
					}
					if ( buildEnv.node ) {
						state[buildEnv.tag]['additionalOptions'] = state[buildEnv.tag]['additionalOptions'] +
								" -Pci.node=${buildEnv.node}"
					}
					state[buildEnv.tag]['containerName'] = null;
					stage('Checkout') {
						checkout scm
					}
					tryFinally({
						stage('Start database') {
							switch (buildEnv.dbName) {
								case "edb":
									docker.image('quay.io/enterprisedb/edb-postgres-advanced:15.4-3.3-postgis').pull()
									sh "./docker_db.sh edb"
									state[buildEnv.tag]['containerName'] = "edb"
									break;
								case "sybase_jconn":
									docker.withRegistry('https://index.docker.io/v1/', 'hibernateci.hub.docker.com') {
										docker.image('nguoianphu/docker-sybase').pull()
									}
									sh "./docker_db.sh sybase"
									state[buildEnv.tag]['containerName'] = "sybase"
									break;
								case "cockroachdb":
									docker.withRegistry('https://index.docker.io/v1/', 'hibernateci.hub.docker.com') {
										docker.image('cockroachdb/cockroach:v23.1.12').pull()
									}
									sh "./docker_db.sh cockroachdb"
									state[buildEnv.tag]['containerName'] = "cockroach"
									break;
							}
						}
						stage('Test') {
							String args = "${buildEnv.additionalOptions ?: ''} ${state[buildEnv.tag]['additionalOptions'] ?: ''}"
							withEnv(["RDBMS=${buildEnv.dbName}"]) {
								tryFinally({
									if (buildEnv.dbLockableResource == null) {
										withCredentials([file(credentialsId: 'sybase-jconnect-driver', variable: 'jconnect_driver')]) {
											sh 'cp -f $jconnect_driver ./drivers/jconn4.jar'
											timeout( [time: buildEnv.longRunning ? 480 : 120, unit: 'MINUTES'] ) {
												ciBuild buildEnv, args
											}
										}
									}
									else {
										lock(label: buildEnv.dbLockableResource, quantity: 1, variable: 'LOCKED_RESOURCE') {
											if ( buildEnv.dbLockResourceAsHost ) {
												args += " -DdbHost=${LOCKED_RESOURCE}"
											}
											timeout( [time: buildEnv.longRunning ? 480 : 120, unit: 'MINUTES'] ) {
												ciBuild buildEnv, args
											}
										}
									}
								}, {
									junit '**/target/test-results/test/*.xml,**/target/test-results/testKitTest/*.xml'
								})
							}
						}
					}, { // Finally
						if ( state[buildEnv.tag]['containerName'] != null ) {
							sh "docker rm -f ${state[buildEnv.tag]['containerName']}"
						}
						// Skip this for PRs
						if ( !env.CHANGE_ID && buildEnv.notificationRecipients != null ) {
							handleNotifications(currentBuild, buildEnv)
						}
					})
				}
			}
		})
	}
	executions.put('Hibernate Search Update Dependency', {
		build job: '/hibernate-search-dependency-update/7.2', propagate: true, parameters: [string(name: 'UPDATE_JOB', value: 'orm6.6'), string(name: 'ORM_REPOSITORY', value: helper.scmSource.remoteUrl), string(name: 'ORM_PULL_REQUEST_ID', value: helper.scmSource.pullRequest.id)]
	})
	parallel(executions)
}

} // End of helper.runWithNotification

// Job-specific helpers

class BuildEnvironment {
	String testJdkVersion
	String testJdkLauncherArgs
	String dbName = 'h2'
	String node
	String dbLockableResource
	boolean dbLockResourceAsHost
	String additionalOptions
	String notificationRecipients
	boolean longRunning

	String toString() { getTag() }
	String getTag() { "${node ? node + "_" : ''}${testJdkVersion ? 'jdk_' + testJdkVersion + '_' : '' }${dbName}" }
	String getRdbms() { dbName.contains("_") ? dbName.substring(0, dbName.indexOf('_')) : dbName }
}

void runBuildOnNode(String label, Closure body) {
	node( label ) {
		pruneDockerContainers()
        tryFinally(body, { // Finally
        	// If this is a PR, we clean the workspace at the end
        	if ( env.CHANGE_BRANCH != null ) {
        		cleanWs()
        	}
        	pruneDockerContainers()
        })
	}
}

void ciBuild(buildEnv, String args) {
	if ( !helper.scmSource.pullRequest ) {
		// Not a PR: we can pass credentials to the build, allowing it to populate the build cache
		// and to publish build scans directly.

		// On untrusted nodes, we use the same access key as for PRs:
		// it has limited access, essentially it can only push build scans.
		def develocityCredentialsId = buildEnv.node ? 'ge.hibernate.org-access-key-pr' : 'ge.hibernate.org-access-key'

		withCredentials([string(credentialsId: develocityCredentialsId,
				variable: 'DEVELOCITY_ACCESS_KEY')]) {
			withGradle { // withDevelocity, actually: https://plugins.jenkins.io/gradle/#plugin-content-capturing-build-scans-from-jenkins-pipeline
				sh "./ci/build.sh $args"
			}
		}
	}
	else if ( buildEnv.node != 's390x' ) { // We couldn't get the code below to work on s390x for some reason.
		// Pull request: we can't pass credentials to the build, since we'd be exposing secrets to e.g. tests.
		// We do the build first, then publish the build scan separately.
		tryFinally({
			sh "./ci/build.sh $args"
		}, { // Finally
			withCredentials([string(credentialsId: 'ge.hibernate.org-access-key-pr',
					variable: 'DEVELOCITY_ACCESS_KEY')]) {
				withGradle { // withDevelocity, actually: https://plugins.jenkins.io/gradle/#plugin-content-capturing-build-scans-from-jenkins-pipeline
					// Don't fail a build if publishing fails
					sh './gradlew buildScanPublishPrevious || true'
				}
			}
		})
	}
	else {
		// Don't do build scans
		sh "./ci/build.sh $args"
	}
}

void pruneDockerContainers() {
	if ( !sh( script: 'command -v docker || true', returnStdout: true ).trim().isEmpty() ) {
		sh 'docker container prune -f || true'
		sh 'docker image prune -f || true'
		sh 'docker network prune -f || true'
		sh 'docker volume prune -f || true'
	}
}

void handleNotifications(currentBuild, buildEnv) {
	def currentResult = getParallelResult(currentBuild, buildEnv.tag)
	boolean success = currentResult == 'SUCCESS' || currentResult == 'UNKNOWN'
	def previousResult = currentBuild.previousBuild == null ? null : getParallelResult(currentBuild.previousBuild, buildEnv.tag)

	// Ignore success after success
	if ( !( success && previousResult == 'SUCCESS' ) ) {
		def subject
		def body
		if ( success ) {
			if ( previousResult != 'SUCCESS' && previousResult != null ) {
				subject = "${env.JOB_NAME} - Build ${env.BUILD_NUMBER} - Fixed"
				body = """<p>${env.JOB_NAME} - Build ${env.BUILD_NUMBER} - Fixed:</p>
					<p>Check console output at <a href='${env.BUILD_URL}'>${env.BUILD_URL}</a> to view the results.</p>"""
			}
			else {
				subject = "${env.JOB_NAME} - Build ${env.BUILD_NUMBER} - Success"
				body = """<p>${env.JOB_NAME} - Build ${env.BUILD_NUMBER} - Success:</p>
					<p>Check console output at <a href='${env.BUILD_URL}'>${env.BUILD_URL}</a> to view the results.</p>"""
			}
		}
		else if (currentBuild.rawBuild.getActions(jenkins.model.InterruptedBuildAction.class).isEmpty()) {
			// If there are interrupted build actions, this means the build was cancelled, probably superseded
			// Thanks to https://issues.jenkins.io/browse/JENKINS-43339 for the "hack" to determine this
			if ( currentResult == 'FAILURE' ) {
				if ( previousResult != null && previousResult == "FAILURE" ) {
					subject = "${env.JOB_NAME} - Build ${env.BUILD_NUMBER} - Still failing"
					body = """<p>${env.JOB_NAME} - Build ${env.BUILD_NUMBER} - Still failing:</p>
						<p>Check console output at <a href='${env.BUILD_URL}'>${env.BUILD_URL}</a> to view the results.</p>"""
				}
				else {
					subject = "${env.JOB_NAME} - Build ${env.BUILD_NUMBER} - Failure"
					body = """<p>${env.JOB_NAME} - Build ${env.BUILD_NUMBER} - Failure:</p>
						<p>Check console output at <a href='${env.BUILD_URL}'>${env.BUILD_URL}</a> to view the results.</p>"""
				}
			}
			else {
				subject = "${env.JOB_NAME} - Build ${env.BUILD_NUMBER} - ${currentResult}"
				body = """<p>${env.JOB_NAME} - Build ${env.BUILD_NUMBER} - ${currentResult}:</p>
					<p>Check console output at <a href='${env.BUILD_URL}'>${env.BUILD_URL}</a> to view the results.</p>"""
			}
		}

		emailext(
				subject: subject,
				body: body,
				to: buildEnv.notificationRecipients
		)
	}
}

@NonCPS
String getParallelResult( RunWrapper build, String parallelBranchName ) {
    def visitor = new PipelineNodeGraphVisitor( build.rawBuild )
    def branch = visitor.pipelineNodes.find{ it.type == FlowNodeWrapper.NodeType.PARALLEL && parallelBranchName == it.displayName }
    if ( branch == null ) {
    	echo "Couldn't find parallel branch name '$parallelBranchName'. Available parallel branch names:"
		visitor.pipelineNodes.findAll{ it.type == FlowNodeWrapper.NodeType.PARALLEL }.each{
			echo " - ${it.displayName}"
		}
    	return null;
    }
    return branch.status.result
}

// try-finally construct that properly suppresses exceptions thrown in the finally block.
static def tryFinally(Closure main, Closure ... finallies) {
	def mainFailure = null
	try {
		main()
	}
	catch (Throwable t) {
		mainFailure = t
		throw t
	}
	finally {
		finallies.each {it ->
			try {
				it()
			}
			catch (Throwable t) {
				if ( mainFailure ) {
					mainFailure.addSuppressed( t )
				}
				else {
					mainFailure = t
				}
			}
		}
	}
	if ( mainFailure ) { // We may reach here if only the "finally" failed
		throw mainFailure
	}
}
