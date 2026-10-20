import groovy.transform.Field

/*
 * See https://github.com/hibernate/hibernate-jenkins-pipeline-helpers
 */
@Library('hibernate-jenkins-pipeline-helpers') _
import org.hibernate.jenkins.pipeline.helpers.job.JobHelper

@Field final String DEFAULT_JDK_VERSION = '25'
@Field final String DEFAULT_JDK_TOOL = "OpenJDK ${DEFAULT_JDK_VERSION} Latest"
@Field final String NODE_PATTERN_BASE = 'Worker&&Containers'
@Field List<BuildEnvironment> environments

this.helper = new JobHelper(this)

helper.runWithNotification {
env.COMMON_GRADLE_ARGS = '-Igradle/init.gradle'

stage('Configure') {
	this.environments = [
		// Most environments have been moved to GitHub Actions (nightly.yml).
		// This Jenkinsfile retains only jobs requiring Jenkins infrastructure:
		// - sybase_jconn: proprietary jConnect driver stored as Jenkins credential
		// - hana_cloud: HANA Cloud lockable resource for exclusive access coordination
		new BuildEnvironment( dbName: 'sybase_jconn' ),
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
								case "sybase_jconn":
									sh "./db.sh sybase"
									state[buildEnv.tag]['containerName'] = "sybase"
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
  def develocityCredentialsId = buildEnv.node ? 'develocity.commonhaus.dev-access-key-pr' : 'develocity.commonhaus.dev-access-key'

  ciBuild(develocityCredentialsId, args)
}

void ciBuild(String args) {
  ciBuild('develocity.commonhaus.dev-access-key-pr', args)
}

void ciBuild(String develocityCredentialsId, String args) {
  withCredentials([string(credentialsId: develocityCredentialsId,
      variable: 'DEVELOCITY_ACCESS_KEY')]) {
    withGradle { // withDevelocity, actually: https://plugins.jenkins.io/gradle/#plugin-content-capturing-build-scans-from-jenkins-pipeline
      sh "./ci/build.sh \$COMMON_GRADLE_ARGS $args"
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
