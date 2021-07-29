/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

import groovy.transform.Field

/*
 * See https://github.com/hibernate/hibernate-jenkins-pipeline-helpers
 */
@Library('hibernate-jenkins-pipeline-helpers@1.5') _
import org.hibernate.jenkins.pipeline.helpers.job.JobHelper

@Field final String NODE_PATTERN_BASE = 'Worker&&Containers'
@Field List<BuildEnvironment> environments

this.helper = new JobHelper(this)

helper.runWithNotification {

stage('Configure') {
	this.environments = [
		buildEnv('8', 'h2'),
		buildEnv('8', 'hsqldb'),
		buildEnv('8', 'derby'),
		buildEnv('8', 'mysql8'),
		buildEnv('8', 'mariadb'),
		buildEnv('8', 'postgresql'),
		buildEnv('8', 'oracle'),
		buildEnv('8', 'oracle_ee'),
		buildEnv('8', 'db2'),
		buildEnv('8', 'mssql'),
		buildEnv('8', 'sybase'),
		buildEnv('8', 'hana', 'HANA'),
		// Disable EDB for now as the image is not available anymore
// 		buildEnv('8', 'edb')
	];

	helper.configure {
		file 'job-configuration.yaml'
		// We don't require the following, but the build helper plugin apparently does
		jdk {
			defaultTool 'OpenJDK 8 Latest'
		}
		maven {
			defaultTool 'Apache Maven 3.8'
		}
	}
	properties([
			buildDiscarder(
					logRotator(daysToKeepStr: '90')
			),
			helper.generateNotificationProperty()
	])
}

stage('Build') {
	Map<String, Closure> executions = [:]
	environments.each { BuildEnvironment buildEnv ->
		executions.put(buildEnv.tag, {
			runBuildOnNode(buildEnv.node) {
				def containerName = null
				env.JAVA_HOME="${tool buildEnv.buildJdkTool}"
				env.PATH="${env.JAVA_HOME}/bin:${env.PATH}"
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
								containerName = "mysql"
								break;
							case "mariadb":
								docker.withRegistry('https://index.docker.io/v1/', 'hibernateci.hub.docker.com') {
									docker.image('mariadb:10.5.8').pull()
								}
								sh "./docker_db.sh mariadb"
								containerName = "mariadb"
								break;
							case "postgresql":
								docker.withRegistry('https://index.docker.io/v1/', 'hibernateci.hub.docker.com') {
									docker.image('postgres:9.5').pull()
								}
								sh "./docker_db.sh postgresql_9_5"
								containerName = "postgres"
								break;
							case "oracle":
								docker.withRegistry('https://index.docker.io/v1/', 'hibernateci.hub.docker.com') {
									docker.image('quillbuilduser/oracle-18-xe').pull()
								}
								sh "./docker_db.sh oracle"
								containerName = "oracle"
								break;
							case "db2":
								docker.withRegistry('https://index.docker.io/v1/', 'hibernateci.hub.docker.com') {
									docker.image('ibmcom/db2:11.5.5.0').pull()
								}
								sh "./docker_db.sh db2"
								containerName = "db2"
								break;
							case "mssql":
								docker.image('mcr.microsoft.com/mssql/server:2017-CU13').pull()
								sh "./docker_db.sh mssql"
								containerName = "mssql"
								break;
							case "sybase":
								docker.withRegistry('https://index.docker.io/v1/', 'hibernateci.hub.docker.com') {
									docker.image('nguoianphu/docker-sybase').pull()
								}
								sh "./docker_db.sh sybase"
								containerName = "sybase"
								break;
							case "edb":
								docker.withRegistry('https://containers.enterprisedb.com', 'hibernateci.containers.enterprisedb.com') {
	// 							withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: 'hibernateci.containers.enterprisedb.com',
	// 								usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD']]) {
	// 							  	sh 'docker login -u "$USERNAME" -p "$PASSWORD" https://containers.enterprisedb.com'
									docker.image('containers.enterprisedb.com/edb/edb-as-lite:v11').pull()
								}
								sh "./docker_db.sh edb"
								containerName = "edb"
								break;
						}
					}
					stage('Test') {
						// Clean by default otherwise the PackagedEntityManager tests fail on a node that previously ran a different DB
						boolean clean = true;
						String goal;
						String lockableResource;
						switch (buildEnv.dbName) {
							case "h2":
							case "derby":
							case "hsqldb":
								goal = "-Pdb=${buildEnv.dbName}"
								break;
							case "mysql8":
								goal = "-Pdb=mysql_ci"
								break;
							case "postgresql":
								goal = "-Pdb=pgsql_ci"
								break;
							case "oracle":
								goal = "-Pdb=oracle_ci -PexcludeTests=**.LockTest.testQueryTimeout*"
								break;
							case "oracle_ee":
								goal = "-Pdb=oracle_jenkins"
								lockableResource = 'ORACLE_RDS'
								break;
							case "hana":
								// For HANA we have to also clean because this is a shared VM and the compile cache can become a problem
								clean = true;
								goal = "-Pdb=hana_jenkins"
								break;
							case "edb":
								goal = "-Pdb=edb_ci -DdbHost=localhost:5433"
								break;
							default:
								goal = "-Pdb=${buildEnv.dbName}_ci"
								break;
						}
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
							junit '**/target/test-results/test/*.xml'
						}
					}
				}
				finally {
					if ( containerName != null ) {
						sh "docker rm -f ${containerName}"
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
	return new BuildEnvironment( version, dbName, NODE_PATTERN_BASE );
}

BuildEnvironment buildEnv(String version, String dbName, String node) {
	return new BuildEnvironment( version, dbName, node );
}

public class BuildEnvironment {
	private String version;
	private String buildJdkTool;
	private String testJdkTool;
	private String dbName;
	private String node;

	public BuildEnvironment(String version, String dbName, String node) {
		this.version = version;
		this.dbName = dbName;
		this.node = node;
		String buildJdkTool;
		String testJdkTool;
		switch ( version ) {
			case "8":
				buildJdkTool = testJdkTool = "OpenJDK 8 Latest";
				break;
			case "11":
				buildJdkTool = testJdkTool = "OpenJDK 11 Latest";
				break;
			default:
				throw new IllegalArgumentException( "Unsupported version: ${version}" );
		}
		this.buildJdkTool = buildJdkTool;
		this.testJdkTool = testJdkTool;
	}
	String toString() { getTag() }
	String getTag() { "jdk-$version-$dbName" }
	String getNode() { node }
}

void runBuildOnNode(String label, Closure body) {
	node( label ) {
		pruneDockerContainers()
        try {
        	timeout( [time: 90, unit: 'MINUTES'], body )
        }
        finally {
        	pruneDockerContainers()
        }
	}
}
void pruneDockerContainers() {
	if ( !sh( script: 'which docker', returnStdout: true ).trim().isEmpty() ) {
		sh 'docker container prune -f || true'
		sh 'docker image prune -f || true'
		sh 'docker network prune -f || true'
		sh 'docker volume prune -f || true'
	}
}