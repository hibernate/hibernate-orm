import groovy.transform.Field

@Library('hibernate-jenkins-pipeline-helpers') _

@Field final String ORM_JDK_VERSION = '21'
@Field final String QUARKUS_JDK_VERSION = '17'
@Field final String ORM_JDK_TOOL = "OpenJDK ${ORM_JDK_VERSION} Latest"
@Field final String QUARKUS_JDK_TOOL = "OpenJDK ${QUARKUS_JDK_VERSION} Latest"

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

void runBuildOnNode(String label, Closure body) {
	node( label ) {
		pruneDockerContainers()
    tryFinally(body, {
      cleanWs()
      pruneDockerContainers()
    })
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

class BuildConfiguration {
	String name
	String projects
	boolean nativeProfile = false
}

// See data category from https://github.com/quarkusio/quarkus/blob/main/.github/native-tests.json
def configurations = [
    new BuildConfiguration( name: "JVM test", projects: "!integration-tests/kafka-oauth-keycloak,!integration-tests/kafka-sasl-elytron,!integration-tests/hibernate-search-orm-opensearch,!integration-tests/hibernate-search-orm-elasticsearch-outbox-polling,!integration-tests/hibernate-search-orm-elasticsearch-tenancy,!integration-tests/maven,!integration-tests/quartz,!integration-tests/reactive-messaging-kafka,!integration-tests/resteasy-reactive-kotlin/standard,!integration-tests/opentelemetry-reactive-messaging,!integration-tests/virtual-threads/kafka-virtual-threads,!integration-tests/smallrye-jwt-oidc-webapp,!extensions/oidc-db-token-state-manager/deployment,!docs"),
    new BuildConfiguration( name: "Data1", nativeProfile: true, projects: "jpa-h2, jpa-h2-embedded, jpa-mariadb, jpa-mssql, jpa-without-entity, hibernate-orm-tenancy/datasource, hibernate-orm-tenancy/connection-resolver, hibernate-orm-tenancy/connection-resolver-legacy-qualifiers"),
    new BuildConfiguration( name: "Data2", nativeProfile: true, projects: "jpa, jpa-mapping-xml/legacy-app, jpa-mapping-xml/modern-app, jpa-mysql, jpa-db2, jpa-oracle"),
    new BuildConfiguration( name: "Data3", nativeProfile: true, projects: "flyway, hibernate-orm-panache, hibernate-orm-panache-kotlin, hibernate-orm-envers, liquibase, liquibase-mongodb"),
    new BuildConfiguration( name: "Data4", nativeProfile: true, projects: "mongodb-client, mongodb-devservices, mongodb-panache, mongodb-rest-data-panache, mongodb-panache-kotlin, redis-client, hibernate-orm-rest-data-panache"),
    new BuildConfiguration( name: "Data5", nativeProfile: true, projects: "jpa-postgresql, jpa-postgresql-withxml, narayana-stm, narayana-jta, reactive-pg-client, hibernate-reactive-postgresql, hibernate-orm-tenancy/schema, hibernate-orm-tenancy/schema-mariadb"),
    new BuildConfiguration( name: "Data6", nativeProfile: true, projects: "elasticsearch-rest-client, elasticsearch-java-client, hibernate-search-orm-elasticsearch, hibernate-search-orm-elasticsearch-tenancy, hibernate-search-orm-opensearch, hibernate-search-orm-elasticsearch-outbox-polling, hibernate-search-standalone-elasticsearch, hibernate-search-standalone-opensearch"),
    new BuildConfiguration( name: "Data7", nativeProfile: true, projects: "reactive-oracle-client, reactive-mysql-client, reactive-db2-client, hibernate-reactive-db2, hibernate-reactive-mariadb, hibernate-reactive-mssql, hibernate-reactive-mysql, hibernate-reactive-mysql-agroal-flyway, hibernate-reactive-panache, hibernate-reactive-panache-kotlin, hibernate-reactive-oracle")
]

pipeline {
    agent none
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
        stage('Build Hibernate ORM') {
            agent {
                label 'LongDuration'
            }
            tools {
                jdk ORM_JDK_TOOL
            }
            steps {
                script {
                    dir('hibernate') {
                        checkout scm
                        sh "./gradlew clean publishToMavenLocal -x test --no-scan --no-daemon --no-build-cache --stacktrace -PmavenMirror=nexus-load-balancer-c4cf05fd92f43ef8.elb.us-east-1.amazonaws.com -Dmaven.repo.local=${env.WORKSPACE_TMP}/.m2repository"
                        script {
                            env.HIBERNATE_VERSION = sh (
                                    script: "grep hibernateVersion gradle/version.properties|cut -d'=' -f2",
                                    returnStdout: true
                            ).trim()
                        }
                    }
                    dir(env.WORKSPACE_TMP) {
                        stash name: 'repository', includes: ".m2repository/"
                    }
                }
            }
        }
        stage('Build Quarkus') {
            agent {
                label 'LongDuration'
            }
            tools {
                jdk QUARKUS_JDK_TOOL
                maven 'Apache Maven 3.9'
            }
            steps {
                script {
                    Map<String, Closure> executions = [:]

                    configurations.each { BuildConfiguration configuration ->
                        executions.put(configuration.name, {
	                        node( 'LongDuration' ) {
                                dir(env.WORKSPACE_TMP) {
                                    unstash "repository"
                                }
                                // Workaround issues when path contains @ character
                                dir(env.WORKSPACE) {
                                    // Remove previous soft-link if it is around
                                    sh "rm .m2 || true"
                                    sh "ln -s ${env.WORKSPACE_TMP}/.m2repository .m2"
                                }
                                dir('quarkus') {
                                    def quarkusVersionToTest = 'orm-7-1-10-3.27'
                                    sh "git clone -b ${quarkusVersionToTest} --single-branch https://github.com/yrodiere/quarkus.git . || git reset --hard && git clean -fx && git pull"
                                    script {
                                        def sedStatus = sh (script: "sed -i 's@<hibernate-orm.version>.*</hibernate-orm.version>@<hibernate-orm.version>${env.HIBERNATE_VERSION}</hibernate-orm.version>@' pom.xml", returnStatus: true)
                                        if ( sedStatus != 0 ) {
                                            throw new IllegalArgumentException( "Unable to replace hibernate version in Quarkus pom. Got exit code $sedStatus" )
                                        }
                                    }
                                    // Need to override the default maven configuration this way, because there is no other way to do it
                                    sh "sed -i 's/-Xmx5g/-Xmx2048m/' ./.mvn/jvm.config"
                                    sh "echo -e '\\n-XX:MaxMetaspaceSize=1024m'>>./.mvn/jvm.config"
                                    withMaven(mavenLocalRepo: env.WORKSPACE + '/.m2', publisherStrategy: 'EXPLICIT') {
                                        def javaHome = tool(name: QUARKUS_JDK_TOOL, type: 'jdk')
                                        // to account for script-only maven wrapper use in Quarkus:
                                        withEnv(["JAVA_HOME=${javaHome}", "PATH+JAVA=${javaHome}/bin", "MAVEN_ARGS=${env.MAVEN_ARGS?:""} ${env.MAVEN_CONFIG}"]) {
                                            sh "./mvnw -pl !docs -Dquickly install"
                                            // Need to kill the gradle daemons started during the Maven install run
                                            sh "sudo pkill -f '.*GradleDaemon.*' || true"
                                            // Need to override the default maven configuration this way, because there is no other way to do it
                                            sh "sed -i 's/-Xmx2048m/-Xmx1340m/' ./.mvn/jvm.config"
                                            sh "sed -i 's/MaxMetaspaceSize=1024m/MaxMetaspaceSize=512m/' ./.mvn/jvm.config"
                                            def projects = configuration.projects
                                            def additionalArguments
                                            def additionalOptions
                                            if ( configuration.nativeProfile ) {
                                                additionalArguments = "-f integration-tests"
                                                additionalOptions = "-Dquarkus.native.native-image-xmx=6g -Dnative -Dnative.surefire.skip -Dno-descriptor-tests"
                                            }
                                            else {
                                                additionalArguments = "-pl :quarkus-hibernate-orm -amd"
                                                additionalOptions = ""
                                            }
                                            sh "TESTCONTAINERS_RYUK_CONTAINER_PRIVILEGED=true ./mvnw -Dinsecure.repositories=WARN ${additionalArguments} -pl '${projects}' verify -Dstart-containers -Dtest-containers -Dskip.gradle.build ${additionalOptions}"
                                        }
                                    }
                                }
                            }
                        })
                    }
                    parallel executions
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
