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

@Field final String NODE_PATTERN_BASE = 'Worker&&Containers'
@Field List<BuildEnvironment> environments

this.helper = new JobHelper(this)

helper.runWithNotification {
def defaultJdk = '8'
stage('Configure') {
	this.environments = [
// 		buildEnv(defaultJdk, 'h2'),
// 		buildEnv(defaultJdk, 'hsqldb'),
// 		buildEnv(defaultJdk, 'derby'),
// 		buildEnv(defaultJdk, 'mysql8'),
// 		buildEnv(defaultJdk, 'mariadb'),
// 		buildEnv(defaultJdk, 'postgresql_9_5'),
// 		buildEnv(defaultJdk, 'postgresql_13'),
// 		buildEnv(defaultJdk, 'oracle'),
// 		buildEnv(defaultJdk, 'db2'),
// 		buildEnv(defaultJdk, 'mssql'),
// 		buildEnv(defaultJdk, 'sybase'),
		buildEnv(defaultJdk, 'hana', 'HANA'),
// 		buildEnv(defaultJdk, 's390x', 's390x'),
// 		buildEnv(defaultJdk, 'tidb', 'tidb', 'tidb_hibernate@pingcap.com'),
		// Disable EDB for now as the image is not available anymore
// 		buildEnv(defaultJdk, 'edb')
		jdkBuildEnv(defaultJdk, '11'),
		jdkBuildEnv(defaultJdk, '17'),
		jdkBuildEnv(defaultJdk, '18'),
		jdkBuildEnv(defaultJdk, '19'),
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
		if ( buildEnv.getVersion() != defaultJdk ) {
			if ( helper.scmSource.pullRequest ) {
				return
			}
		}
		state[buildEnv.tag] = [:]
		executions.put(buildEnv.tag, {
			runBuildOnNode(buildEnv.node) {
				// Use withEnv instead of setting env directly, as that is global!
				// See https://github.com/jenkinsci/pipeline-plugin/blob/master/TUTORIAL.md
				withEnv(["JAVA_HOME=${tool buildEnv.buildJdkTool}", "PATH+JAVA=${tool buildEnv.buildJdkTool}/bin", "TEST_JAVA_HOME=${tool buildEnv.testJdkTool}"]) {
					if ( buildEnv.getVersion() != defaultJdk ) {
						state[buildEnv.tag]['additionalOptions'] = " -Ptest.jdk.version=${buildEnv.getTestVersion()} -Porg.gradle.java.installations.paths=${JAVA_HOME},${TEST_JAVA_HOME}";
					}
					else {
						state[buildEnv.tag]['additionalOptions'] = "";
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
										docker.image('mariadb:10.5.8').pull()
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
										docker.image('quillbuilduser/oracle-18-xe').pull()
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
									docker.image('mcr.microsoft.com/mssql/server:2017-CU13').pull()
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

BuildEnvironment buildEnv(String version, String dbName) {
	return new BuildEnvironment( version, version, dbName, NODE_PATTERN_BASE, null );
}

BuildEnvironment buildEnv(String version, String dbName, String node) {
	return new BuildEnvironment( version, version, dbName, node, null );
}

BuildEnvironment buildEnv(String version, String dbName, String node, String notificationRecipients) {
	return new BuildEnvironment( version, version, dbName, node, notificationRecipients );
}

BuildEnvironment jdkBuildEnv(String version, String testVersion) {
	return new BuildEnvironment( version,testVersion, "h2", NODE_PATTERN_BASE, null );
}

BuildEnvironment jdkBuildEnv(String version, String testVersion, String notificationRecipients) {
	return new BuildEnvironment( version,testVersion, "h2", NODE_PATTERN_BASE, notificationRecipients );
}

public class BuildEnvironment {
	private String version;
	private String testVersion;
	private String buildJdkTool;
	private String testJdkTool;
	private String dbName;
	private String node;
	private String notificationRecipients;

	public BuildEnvironment(String version, String testVersion, String dbName, String node, String notificationRecipients) {
		this.version = version;
		this.testVersion = testVersion;
		this.dbName = dbName;
		this.node = node;
		this.notificationRecipients = notificationRecipients;
		this.buildJdkTool = "OpenJDK ${version} Latest";
		this.testJdkTool = "OpenJDK ${testVersion} Latest";
	}
	String toString() { getTag() }
	String getTag() { "jdk_${testVersion}_${dbName}" }
	String getNode() { node }
	String getVersion() { version }
	String getTestVersion() { testVersion }
	String getNotificationRecipients() { notificationRecipients }
}

void runBuildOnNode(String label, Closure body) {
	node( label ) {
		pruneDockerContainers()
        try {
			body()
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
			timeout( [time: 200, unit: 'MINUTES'] ) {
				sh cmd
			}
		}
		else {
			lock(lockableResource) {
				timeout( [time: 200, unit: 'MINUTES'] ) {
					sh cmd
				}
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
		else if ( currentResult == 'FAILURE' ) {
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