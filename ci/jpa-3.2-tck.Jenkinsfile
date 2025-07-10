@Library('hibernate-jenkins-pipeline-helpers') _

// Avoid running the pipeline on branch indexing
if (currentBuild.getBuildCauses().toString().contains('BranchIndexingCause')) {
  	print "INFO: Build skipped due to trigger being Branch Indexing"
	currentBuild.result = 'NOT_BUILT'
  	return
}
def throttleCount
// Don't build the TCK on PRs, unless they use the tck label
if ( env.CHANGE_ID != null ) {
	if ( !pullRequest.labels.contains( 'tck' ) ) {
		print "INFO: Build skipped because pull request doesn't have 'tck' label"
		return
	}
	throttleCount = 20
}
else {
	throttleCount = 1
}

pipeline {
    agent none
    tools {
        jdk 'OpenJDK 17 Latest'
    }
    options {
  		rateLimitBuilds(throttle: [count: throttleCount, durationName: 'day', userBoost: true])
        buildDiscarder(logRotator(numToKeepStr: '3', artifactNumToKeepStr: '3'))
        disableConcurrentBuilds(abortPrevious: true)
    }
    parameters {
        choice(name: 'IMAGE_JDK', choices: ['jdk17', 'jdk21'], description: 'The JDK base image version to use for the TCK image.')
        string(name: 'TCK_VERSION', defaultValue: '3.2.0', description: 'The version of the Jakarta JPA TCK i.e. `2.2.0` or `3.0.1`')
        string(name: 'TCK_SHA', defaultValue: '', description: 'The SHA256 of the Jakarta JPA TCK that is distributed under https://download.eclipse.org/jakartaee/persistence/3.1/jakarta-persistence-tck-${TCK_VERSION}.zip.sha256')
		string(name: 'TCK_URL', defaultValue: 'https://www.eclipse.org/downloads/download.php?file=/ee4j/jakartaee-tck/jakartaee11/staged/eftl/jakarta-persistence-tck-3.2.0.zip&mirror_id=1', description: 'The URL from which to download the TCK ZIP file. Only needed for testing staged builds. Ensure the TCK_VERSION variable matches the ZIP file name suffix.')
        choice(name: 'RDBMS', choices: ['postgresql','mysql','mssql','oracle','db2','sybase'], description: 'The JDK base image version to use for the TCK image.')
	}
    stages {
        stage('Checks') {
        	steps {
                requireApprovalForPullRequest 'hibernate'
            }
        }
        stage('TCK') {
            agent {
                label 'LongDuration'
            }
            stages {
                stage('Build') {
                    steps {
                        script {
                            docker.image('openjdk:17-jdk').pull()
                        }
                        dir('hibernate') {
                            checkout scm
                            withEnv([
                                    "DISABLE_REMOTE_GRADLE_CACHE=true"
                            ]) {
                                sh './gradlew clean publishToMavenLocal -x test --no-scan --no-daemon --no-build-cache --stacktrace -PmavenMirror=nexus-load-balancer-c4cf05fd92f43ef8.elb.us-east-1.amazonaws.com'
                                // For some reason, Gradle does not publish hibernate-platform and hibernate-testing
                                // to the local maven repository with the previous command,
                                // but requires an extra run instead
                                sh './gradlew :hibernate-testing:publishToMavenLocal :hibernate-platform:publishToMavenLocal -x test --no-scan --no-daemon --no-build-cache --stacktrace -PmavenMirror=nexus-load-balancer-c4cf05fd92f43ef8.elb.us-east-1.amazonaws.com'
                            }
                            script {
                                env.HIBERNATE_VERSION = sh (
                                    script: "grep hibernateVersion gradle/version.properties|cut -d'=' -f2",
                                    returnStdout: true
                                ).trim()
                                switch (params.RDBMS) {
                                    case "mysql":
                                        docker.image('mysql:8.2.0').pull()
                                        sh "./docker_db.sh mysql"
                                        break;
                                    case "mssql":
                                        docker.image('mcr.microsoft.com/mssql/server@sha256:5439be9edc3b514cf647bcd3651779fa13f487735a985f40cbdcfecc60fea273').pull()
                                        sh "./docker_db.sh mssql"
                                        break;
                                    case "oracle":
                                        docker.image('gvenzl/oracle-free:23').pull()
                                        sh "./docker_db.sh oracle"
                                        break;
                                    case "postgresql":
                                        docker.image('postgis/postgis:16-3.4').pull()
                                        sh "./docker_db.sh postgresql"
                                        break;
                                    case "db2":
                                        docker.image('icr.io/db2_community/db2:11.5.9.0').pull()
                                        sh "./docker_db.sh db2"
                                        break;
                                    case "sybase":
                                        docker.image('nguoianphu/docker-sybase').pull()
                                        sh "./docker_db.sh sybase"
                                        break;
                                }
                            }
                        }
                        dir('tck') {
                            checkout changelog: false, poll: false, scm: [$class: 'GitSCM', branches: [[name: '*/main']], extensions: [], userRemoteConfigs: [[url: 'https://github.com/hibernate/jakarta-tck-runner.git']]]
                            script {
                                withCredentials([file(credentialsId: 'sybase-jconnect-driver', variable: 'jconnect_driver')]) {
                                    sh 'cp -f $jconnect_driver ./jpa-3.2/jconn42.jar'
                                    if ( params.TCK_URL == null || params.TCK_URL.isEmpty() ) {
                                        sh "cd jpa-3.2; docker build -f Dockerfile.${params.IMAGE_JDK} -t jakarta-tck-runner --build-arg TCK_VERSION=${params.TCK_VERSION} --build-arg TCK_SHA=${params.TCK_SHA} ."
                                    }
                                    else {
                                        sh "cd jpa-3.2; docker build -f Dockerfile.${params.IMAGE_JDK} -t jakarta-tck-runner --build-arg TCK_VERSION=${params.TCK_VERSION} --build-arg TCK_SHA=${params.TCK_SHA} --build-arg 'TCK_URL=${params.TCK_URL}' ."
                                    }
                                }
                            }
                        }
                    }
                }
                stage('Run TCK') {
                    steps {
                        script {
                            def containerName
                            if ( params.RDBMS == 'postgresql' ) {
                                containerName = 'postgres'
                            }
                            else {
                                containerName = params.RDBMS
                            }
                            def dockerRunOptions = "--network=tck-net -e DB_HOST=${containerName}"
                            sh """ \
                                while IFS= read -r container; do
                                    docker network disconnect tck-net \$container || true
                                done <<< \$(docker network inspect tck-net --format '{{range \$k, \$v := .Containers}}{{print \$k}}{{end}}' 2>/dev/null || true)
                                docker network rm -f tck-net
                                docker network create tck-net
                                docker network connect tck-net ${containerName}
                            """
                            sh """ \
                                rm -Rf ./results
                                docker rm -f tck || true
                                docker run -v ~/.m2/repository:/home/jenkins/.m2/repository:z ${dockerRunOptions} -e MAVEN_OPTS=-Dmaven.repo.local=/home/jenkins/.m2/repository -e RDBMS=${params.RDBMS} -e HIBERNATE_VERSION=$HIBERNATE_VERSION --name tck jakarta-tck-runner || true
                                docker cp tck:/tck/persistence-tck/bin/target/failsafe-reports ./results
                                docker cp tck:/tck/persistence-tck/bin/target/test-reports ./results
                            """
                        }
                        archiveArtifacts artifacts: 'results/**'
                        script {
                            def failures = sh (
                                script: """ \
                                    while read line; do
                                      if [[ "\$line" = *'-error" style="display:none;">' ]]; then
                                        prefix1='<tr class="a" id="'
                                        prefix2='<tr class="b" id="'
                                        suffix='-error" style="display:none;">'
                                        line=\${line#"\$prefix1"}
                                        line=\${line#"\$prefix2"}
                                        test=\${line%"\$suffix"}
                                        echo "\$test"
                                      fi
                                    done <results/test-reports/failsafe-report.html
                                """,
                                returnStdout: true
                            ).trim()
                            if ( !failures.isEmpty() ) {
                                echo "Some TCK tests failed:"
                                echo failures
                                currentBuild.result = 'FAILURE'
                            }
                        }
                    }
                }
            }
            post {
                always {
                    script {
                        def containerName
                        if ( params.RDBMS == 'postgresql' ) {
                            containerName = 'postgres'
                        }
                        else {
                            containerName = params.RDBMS
                        }
                        sh "docker rm -f ${containerName}"
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
