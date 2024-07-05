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
	this.environments = [
		// Minimum supported versions
		new BuildEnvironment( dbName: 'hsqldb_2_6' ),
		new BuildEnvironment( dbName: 'mysql_8_0' ),
		new BuildEnvironment( dbName: 'mariadb_10_4' ),
		new BuildEnvironment( dbName: 'postgresql_12' ),
		new BuildEnvironment( dbName: 'edb_12' ),
		new BuildEnvironment( dbName: 'oracle_21' ), // Did not find an image for Oracle-XE 19c
		new BuildEnvironment( dbName: 'db2_10_5', longRunning: true ),
		new BuildEnvironment( dbName: 'mssql_2017' ), // Unfortunately there is no SQL Server 2008 image, so we have to test with 2017
// 		new BuildEnvironment( dbName: 'sybase_16' ), // There only is a Sybase ASE 16 image, so no pint in testing that nightly
		new BuildEnvironment( dbName: 'sybase_jconn' ),
		// Long running databases
		new BuildEnvironment( dbName: 'cockroachdb', node: 'cockroachdb', longRunning: true ),
		new BuildEnvironment( dbName: 'hana_cloud', dbLockableResource: 'hana-cloud', dbLockResourceAsHost: true )
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
			rateLimitBuilds(throttle: [count: 1, durationName: 'day', userBoost: true]),
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

stage('Build') {
	Map<String, Closure> executions = [:]
	Map<String, Map<String, String>> state = [:]
	environments.each { BuildEnvironment buildEnv ->
		// Don't build environments for newer JDKs when this is a PR
		if ( helper.scmSource.pullRequest && buildEnv.testJdkVersion ) {
			return
		}
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
					state[buildEnv.tag]['additionalOptions'] = ''
					if ( testJavaHome ) {
						state[buildEnv.tag]['additionalOptions'] = state[buildEnv.tag]['additionalOptions'] +
								" -Ptest.jdk.version=${buildEnv.testJdkVersion} -Porg.gradle.java.installations.paths=${javaHome},${testJavaHome}"
					}
					if ( buildEnv.testJdkLauncherArgs ) {
						state[buildEnv.tag]['additionalOptions'] = state[buildEnv.tag]['additionalOptions'] +
								" -Ptest.jdk.launcher.args=${buildEnv.testJdkLauncherArgs}"
					}
					state[buildEnv.tag]['containerName'] = null;
					stage('Checkout') {
						checkout scm
					}
					tryFinally({
						stage('Start database') {
							switch (buildEnv.dbName) {
								case "hsqldb_2_6":
									state[buildEnv.tag]['additionalOptions'] = state[buildEnv.tag]['additionalOptions'] +
										" -Pgradle.libs.versions.hsqldb=2.6.1"
									break;
								case "mysql_8_0":
									docker.withRegistry('https://index.docker.io/v1/', 'hibernateci.hub.docker.com') {
										docker.image('mysql:8.0.31').pull()
									}
									sh "./docker_db.sh mysql_8_0"
									state[buildEnv.tag]['containerName'] = "mysql"
									break;
								case "mariadb_10_4":
									docker.withRegistry('https://index.docker.io/v1/', 'hibernateci.hub.docker.com') {
										docker.image('mariadb:10.4.31').pull()
									}
									sh "./docker_db.sh mariadb_10_4"
									state[buildEnv.tag]['containerName'] = "mariadb"
									break;
								case "postgresql_12":
									// use the postgis image to enable the PGSQL GIS (spatial) extension
									docker.withRegistry('https://index.docker.io/v1/', 'hibernateci.hub.docker.com') {
										docker.image('postgis/postgis:12-3.4').pull()
									}
									sh "./docker_db.sh postgresql_12"
									state[buildEnv.tag]['containerName'] = "postgres"
									break;
								case "edb_12":
									docker.image('quay.io/enterprisedb/edb-postgres-advanced:12.16-3.3-postgis').pull()
									sh "./docker_db.sh edb_12"
									state[buildEnv.tag]['containerName'] = "edb"
									break;
								case "oracle_21":
									docker.withRegistry('https://index.docker.io/v1/', 'hibernateci.hub.docker.com') {
										docker.image('gvenzl/oracle-xe:21.3.0').pull()
									}
									sh "./docker_db.sh oracle_21"
									state[buildEnv.tag]['containerName'] = "oracle"
									break;
								case "db2_10_5":
									docker.withRegistry('https://index.docker.io/v1/', 'hibernateci.hub.docker.com') {
										docker.image('ibmoms/db2express-c@sha256:a499afd9709a1f69fb41703e88def9869955234c3525547e2efc3418d1f4ca2b').pull()
									}
									sh "./docker_db.sh db2_10_5"
									state[buildEnv.tag]['containerName'] = "db2"
									break;
								case "mssql_2017":
									docker.image('mcr.microsoft.com/mssql/server@sha256:7d194c54e34cb63bca083542369485c8f4141596805611e84d8c8bab2339eede').pull()
									sh "./docker_db.sh mssql_2017"
									state[buildEnv.tag]['containerName'] = "mssql"
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
								}, { // Finally
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
}

void runBuildOnNode(String label, Closure body) {
	node( label ) {
		pruneDockerContainers()
    tryFinally(body, {
      // If this is a PR, we clean the workspace at the end
      if ( env.CHANGE_BRANCH != null ) {
        cleanWs()
      }
      pruneDockerContainers()
    })
	}
}

void ciBuild(buildEnv, String args) {
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
def tryFinally(Closure main, Closure ... finallies) {
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
