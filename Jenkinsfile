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
@Library('hibernate-jenkins-pipeline-helpers@1.5') _
import org.hibernate.jenkins.pipeline.helpers.job.JobHelper

@Field final String DEFAULT_JDK_VERSION = '11'
@Field final String DEFAULT_JDK_TOOL = "OpenJDK ${DEFAULT_JDK_VERSION} Latest"
@Field final String NODE_PATTERN_BASE = 'Worker&&Containers'
@Field List<BuildEnvironment> environments

this.helper = new JobHelper(this)

helper.runWithNotification {
def defaultJdk = '11'
stage('Configure') {
	this.environments = [
//		new BuildEnvironment( dbName: 'h2' ),
//		new BuildEnvironment( dbName: 'hsqldb' ),
//		new BuildEnvironment( dbName: 'derby' ),
//		new BuildEnvironment( dbName: 'mysql8' ),
//		new BuildEnvironment( dbName: 'mariadb' ),
//		new BuildEnvironment( dbName: 'postgresql_9_5' ),
//		new BuildEnvironment( dbName: 'postgresql_13' ),
//		new BuildEnvironment( dbName: 'oracle' ),
		new BuildEnvironment( dbName: 'oracle_ee' ),
//		new BuildEnvironment( dbName: 'db2' ),
//		new BuildEnvironment( dbName: 'mssql' ),
//		new BuildEnvironment( dbName: 'sybase' ),
		new BuildEnvironment( dbName: 'hana', node: 'HANA' ),
		new BuildEnvironment( dbName: 's390x', node: 's390x' ),
		new BuildEnvironment( dbName: 'tidb', node: 'tidb', notificationRecipients: 'tidb_hibernate@pingcap.com' ),
// Disable EDB for now as the image is not available anymore
//		new BuildEnvironment( dbName: 'edb' ),
		new BuildEnvironment( testJdkVersion: '17' ),
		new BuildEnvironment( testJdkVersion: '18' ),
		// We want to enable preview features when testing early-access builds of OpenJDK:
		// even if we don't use these features, just enabling them can cause side effects
		// and it's useful to test that.
		new BuildEnvironment( testJdkVersion: '19', testJdkLauncherArgs: '--enable-preview' ),
		new BuildEnvironment( testJdkVersion: '20', testJdkLauncherArgs: '--enable-preview' )
	];

	helper.configure {
		file 'job-configuration.yaml'
		// We don't require the following, but the build helper plugin apparently does
		jdk {
			defaultTool "OpenJDK ${defaultJdk} Latest"
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
  currentBuild.result = 'ABORTED'
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
				def javaHome = tool(name: "OpenJDK ${DEFAULT_JDK_VERSION} Latest", type: 'jdk')
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
					try {
						stage('Start database') {
							switch (buildEnv.dbName) {
								case "mysql8":
									docker.withRegistry('https://index.docker.io/v1/', 'hibernateci.hub.docker.com') {
										docker.image('mysql:8.0.21').pull()
									}
									sh "./docker_db.sh mysql_8_0"
									state[buildEnv.tag]['containerName'] = "mysql"
									break;
								case "mariadb":
									docker.withRegistry('https://index.docker.io/v1/', 'hibernateci.hub.docker.com') {
										docker.image('mariadb:10.7.5').pull()
									}
									sh "./docker_db.sh mariadb"
									state[buildEnv.tag]['containerName'] = "mariadb"
									break;
								case "postgresql_9_5":
									// use the postgis image to enable the PGSQL GIS (spatial) extension
									docker.withRegistry('https://index.docker.io/v1/', 'hibernateci.hub.docker.com') {
										docker.image('postgis/postgis:9.5-2.5').pull()
									}
									sh "./docker_db.sh postgresql_9_5"
									state[buildEnv.tag]['containerName'] = "postgres"
									break;
								case "postgresql_13":
									// use the postgis image to enable the PGSQL GIS (spatial) extension
									docker.withRegistry('https://index.docker.io/v1/', 'hibernateci.hub.docker.com') {
										docker.image('postgis/postgis:13-3.1').pull()
									}
									sh "./docker_db.sh postgresql_13"
									state[buildEnv.tag]['containerName'] = "postgres"
									break;
								case "oracle":
									docker.withRegistry('https://index.docker.io/v1/', 'hibernateci.hub.docker.com') {
										docker.image('gvenzl/oracle-xe:18.4.0-full').pull()
									}
									sh "./docker_db.sh oracle_18"
									state[buildEnv.tag]['containerName'] = "oracle"
									break;
								case "db2":
									docker.withRegistry('https://index.docker.io/v1/', 'hibernateci.hub.docker.com') {
										docker.image('ibmcom/db2:11.5.7.0').pull()
									}
									sh "./docker_db.sh db2"
									state[buildEnv.tag]['containerName'] = "db2"
									break;
								case "mssql":
									docker.image('mcr.microsoft.com/mssql/server@sha256:f54a84b8a802afdfa91a954e8ddfcec9973447ce8efec519adf593b54d49bedf').pull()
									sh "./docker_db.sh mssql"
									state[buildEnv.tag]['containerName'] = "mssql"
									break;
								case "sybase":
									docker.withRegistry('https://index.docker.io/v1/', 'hibernateci.hub.docker.com') {
										docker.image('nguoianphu/docker-sybase').pull()
									}
									sh "./docker_db.sh sybase"
									state[buildEnv.tag]['containerName'] = "sybase"
									break;
								case "edb":
									docker.withRegistry('https://containers.enterprisedb.com', 'hibernateci.containers.enterprisedb.com') {
		// 							withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: 'hibernateci.containers.enterprisedb.com',
		// 								usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD']]) {
		// 							  	sh 'docker login -u "$USERNAME" -p "$PASSWORD" https://containers.enterprisedb.com'
										docker.image('containers.enterprisedb.com/edb/edb-as-lite:v11').pull()
									}
									sh "./docker_db.sh edb"
									state[buildEnv.tag]['containerName'] = "edb"
									break;
							}
						}
						stage('Test') {
							switch (buildEnv.dbName) {
								case "h2":
								case "derby":
								case "hsqldb":
									runTest("-Pdb=${buildEnv.dbName}${state[buildEnv.tag]['additionalOptions']}")
									break;
								case "mysql8":
									runTest("-Pdb=mysql_ci${state[buildEnv.tag]['additionalOptions']}")
									break;
								case "tidb":
									runTest("-Pdb=tidb -DdbHost=localhost:4000${state[buildEnv.tag]['additionalOptions']}", 'TIDB')
									break;
								case "postgresql_9_5":
								case "postgresql_13":
									runTest("-Pdb=pgsql_ci${state[buildEnv.tag]['additionalOptions']}")
									break;
								case "oracle":
									runTest("-Pdb=oracle_ci -PexcludeTests=**.LockTest.testQueryTimeout*${state[buildEnv.tag]['additionalOptions']}")
									break;
								case "oracle_ee":
									runTest("-Pdb=oracle_jenkins${state[buildEnv.tag]['additionalOptions']}", 'ORACLE_RDS')
									break;
								case "hana":
									runTest("-Pdb=hana_jenkins${state[buildEnv.tag]['additionalOptions']}", 'HANA')
									break;
								case "edb":
									runTest("-Pdb=edb_ci -DdbHost=localhost:5433${state[buildEnv.tag]['additionalOptions']}")
									break;
								case "s390x":
									runTest("-Pdb=h2${state[buildEnv.tag]['additionalOptions']}")
									break;
								default:
									runTest("-Pdb=${buildEnv.dbName}_ci${state[buildEnv.tag]['additionalOptions']}")
									break;
							}
						}
					}
					finally {
						if ( state[buildEnv.tag]['containerName'] != null ) {
							sh "docker rm -f ${state[buildEnv.tag]['containerName']}"
						}
						// Skip this for PRs
						if ( !env.CHANGE_ID && buildEnv.notificationRecipients != null ) {
							handleNotifications(currentBuild, buildEnv)
						}
					}
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
	String notificationRecipients

	String toString() { getTag() }
	String getTag() { "${testJdkVersion ? 'jdk_' + testJdkVersion + '_' : '' }${dbName}" }
}

void runBuildOnNode(String label, Closure body) {
	node( label ) {
		pruneDockerContainers()
        try {
			timeout( [time: 120, unit: 'MINUTES'], body )
        }
        finally {
        	// If this is a PR, we clean the workspace at the end
        	if ( env.CHANGE_BRANCH != null ) {
        		cleanWs()
        	}
        	pruneDockerContainers()
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
// Clean by default otherwise the PackagedEntityManager tests fail on a node that previously ran a different DB
void runTest(String goal, String lockableResource = null, boolean clean = true) {
	String cmd = "./gradlew" + (clean ? " clean" : "") + " check ${goal} -Plog-test-progress=true --stacktrace";
	try {
		if (lockableResource == null) {
			sh cmd
		}
		else {
			lock(lockableResource) {
				sh cmd
			}
		}
	}
	finally {
		junit '**/target/test-results/test/*.xml,**/target/test-results/testKitTest/*.xml'
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