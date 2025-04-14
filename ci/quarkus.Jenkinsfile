@Library('hibernate-jenkins-pipeline-helpers@1.13') _

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

pipeline {
    agent none
    tools {
        jdk 'OpenJDK 17 Latest'
    }
    options {
        buildDiscarder(logRotator(numToKeepStr: '3', artifactNumToKeepStr: '3'))
        disableConcurrentBuilds(abortPrevious: true)
        skipDefaultCheckout()
    }
    stages {
        stage('Checks') {
        	steps {
                requireApprovalForPullRequest 'hibernate'
            }
        }
        stage('Build') {
            agent {
                label 'LongDuration'
            }
        	steps {
                requireApprovalForPullRequest 'hibernate'
				script {
					dir('hibernate') {
						checkout scm
						sh "./gradlew clean publishToMavenLocal -x test --no-scan --no-daemon --no-build-cache --stacktrace -PmavenMirror=nexus-load-balancer-c4cf05fd92f43ef8.elb.us-east-1.amazonaws.com -Dmaven.repo.local=${env.WORKSPACE}/.m2repository"
						script {
							env.HIBERNATE_VERSION = sh (
									script: "grep hibernateVersion gradle/version.properties|cut -d'=' -f2",
									returnStdout: true
							).trim()
						}
					}
					dir('quarkus') {
						sh "git clone -b 3.15 --single-branch https://github.com/quarkusio/quarkus.git . || git reset --hard && git clean -fx && git pull"
						// This is a temporary workaround because Quarkus reverted the Hibernate ORM and Reactive upgrade.
						// Remove this once Quarkus upgrades the versions again
						sh "git reset --hard f42166ee7041ed09b7183d5dbf3ece2439b16676"
        				script {
							def sedStatus = sh (script: "sed -i 's@<hibernate-orm.version>.*</hibernate-orm.version>@<hibernate-orm.version>${env.HIBERNATE_VERSION}</hibernate-orm.version>@' pom.xml", returnStatus: true)
							if ( sedStatus != 0 ) {
								throw new IllegalArgumentException( "Unable to replace hibernate version in Quarkus pom. Got exit code $sedStatus" )
							}
						}
						// Need to override the default maven configuration this way, because there is no other way to do it
						sh "sed -i 's/-Xmx5g/-Xmx2048m/' ./.mvn/jvm.config"
						sh "echo -e '\\n-XX:MaxMetaspaceSize=1024m'>>./.mvn/jvm.config"
        		        withMaven(mavenLocalRepo: env.WORKSPACE + '/.m2repository', publisherStrategy:'EXPLICIT') {
							sh "./mvnw -pl !docs -Dquickly install"
							// Need to kill the gradle daemons started during the Maven install run
							sh "sudo pkill -f '.*GradleDaemon.*' || true"
							// Need to override the default maven configuration this way, because there is no other way to do it
							sh "sed -i 's/-Xmx2048m/-Xmx1340m/' ./.mvn/jvm.config"
							sh "sed -i 's/MaxMetaspaceSize=1024m/MaxMetaspaceSize=512m/' ./.mvn/jvm.config"
							def excludes = "'!integration-tests/kafka-oauth-keycloak,!integration-tests/kafka-sasl-elytron,!integration-tests/hibernate-search-orm-opensearch,!integration-tests/maven,!integration-tests/quartz,!integration-tests/reactive-messaging-kafka,!integration-tests/resteasy-reactive-kotlin/standard,!integration-tests/opentelemetry-reactive-messaging,!integration-tests/virtual-threads/kafka-virtual-threads,!integration-tests/smallrye-jwt-oidc-webapp,!extensions/oidc-db-token-state-manager/deployment,!docs'"
							sh "TESTCONTAINERS_RYUK_CONTAINER_PRIVILEGED=true ./mvnw -Dinsecure.repositories=WARN -pl :quarkus-hibernate-orm -amd -pl ${excludes} verify -Dstart-containers -Dtest-containers -Dskip.gradle.build"
						}
					}
				}
			}
		}
    }
    post {
        always {
            notifyBuildResult maintainers: "andrea@hibernate.org steve@hibernate.org christian.beikov@gmail.com mbellade@redhat.com"
        }
    }
}