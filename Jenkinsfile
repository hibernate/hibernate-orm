import groovy.transform.Field

@Library('hibernate-jenkins-pipeline-helpers@1.5') _

@Field final String NODE_PATTERN_BASE = 'Worker'
@Field List<BuildEnvironment> environments

this.helper = new JobHelper(this)

helper.runWithNotification {

stage('Configure') {
	this.environments = [
		new BuildEnvironment('8', 'h2'),
		new BuildEnvironment('8', 'hsqldb'),
		new BuildEnvironment('8', 'derby'),
		new BuildEnvironment('8', 'mysql8'),
		new BuildEnvironment('8', 'mariadb'),
		new BuildEnvironment('8', 'postgresql'),
		new BuildEnvironment('8', 'oracle'),
		new BuildEnvironment('8', 'oracle_ee'),
		new BuildEnvironment('8', 'db2'),
		new BuildEnvironment('8', 'mssql'),
		new BuildEnvironment('8', 'sybase'),
		new BuildEnvironment('8', 'hana', 'HANA'),
		new BuildEnvironment('8', 'edb')
	];

	helper.configure {
		file 'job-configuration.yaml'
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
				jdk {
					defaultTool buildEnv.buildJdkTool
				}
				stage('Checkout') {
					checkout scm
				}
				stage('Start database') {
					switch (buildEnv.dbName) {
						case "mysql8":
							docker.withRegistry('https://index.docker.io/v1/', 'hibernateci.hub.docker.com') {
								docker.image('mysql:8.0.21').pull()
							}
							sh "./docker_db.sh mysql_8_0"
							break;
						case "mariadb":
							docker.withRegistry('https://index.docker.io/v1/', 'hibernateci.hub.docker.com') {
								docker.image('mariadb:10.5.8').pull()
							}
							sh "./docker_db.sh mariadb"
							break;
						case "postgresql":
							docker.withRegistry('https://index.docker.io/v1/', 'hibernateci.hub.docker.com') {
								docker.image('postgres:9.5').pull()
							}
							sh "./docker_db.sh postgresql_9_5"
							break;
						case "oracle":
							docker.withRegistry('https://index.docker.io/v1/', 'hibernateci.hub.docker.com') {
								docker.image('quillbuilduser/oracle-18-xe').pull()
							}
							sh "./docker_db.sh oracle"
							break;
						case "db2":
							docker.withRegistry('https://index.docker.io/v1/', 'hibernateci.hub.docker.com') {
								docker.image('ibmcom/db2:11.5.5.0').pull()
							}
							sh "./docker_db.sh db2"
							break;
						case "mssql":
							docker.image('mcr.microsoft.com/mssql/server:2017-CU13').pull()
							sh "./docker_db.sh mssql"
							break;
						case "sybase":
							docker.withRegistry('https://index.docker.io/v1/', 'hibernateci.hub.docker.com') {
								docker.image('nguoianphu/docker-sybase').pull()
							}
							sh "./docker_db.sh sybase"
							break;
						case "edb":
							docker.withRegistry('https://containers.enterprisedb.com', 'hibernateci.containers.enterprisedb.com') {
								docker.image('containers.enterprisedb.com/edb/edb-as-lite:v11').pull()
							}
							sh "./docker_db.sh edb"
							break;
					}
				}
				stage('Test') {
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
  							goal = "-Pdb=hana_jenkins"
							break;
						default:
							goal = "-Pdb=${buildEnv.dbName}_ci"
							break;
					}
					String cmd = "./gradlew check ${goal} -Plog-test-progress=true --stacktrace";
					if (lockableResource == null) {
						sh cmd
					}
					else {
						lock(lockableResource) {
							sh cmd
						}
					}
					junit '**/test-reports/*.xml'
				}
			}
		})
	}
	parallel(executions)
}

} // End of helper.runWithNotification


class BuildEnvironment {
	String version
	String buildJdkTool
	String testJdkTool
	String dbName
	String node

	public BuildEnvironment(String version, String dbName) {
		this(version, dbName, NODE_PATTERN_BASE)
	}

	public BuildEnvironment(String version, String dbName, String node) {
		this.version = version;
		this.dbName = dbName;
		this.node = node;
		String buildJdkTool;
		String testJdkTool;
		switch (version) {
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

void runBuildOnNode(Closure body) {
	runBuildOnNode( NODE_PATTERN_BASE, body )
}

void runBuildOnNode(String label, Closure body) {
	node( label ) {
		pruneDockerContainers()
        try {
        	timeout( [time: 1, unit: 'HOURS'], body )
        }
        finally {
        	pruneDockerContainers()
        }
	}
}
void pruneDockerContainers() {
	sh 'docker container prune -f || true'
	sh 'docker image prune -f || true'
	sh 'docker network prune -f || true'
	sh 'docker volume prune -f || true'
}