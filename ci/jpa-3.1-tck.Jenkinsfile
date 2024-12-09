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
    agent {
        label 'LongDuration'
    }
    tools {
        jdk 'OpenJDK 11 Latest'
    }
    options {
        buildDiscarder(logRotator(numToKeepStr: '3', artifactNumToKeepStr: '3'))
        disableConcurrentBuilds(abortPrevious: true)
    }
    parameters {
        choice(name: 'IMAGE_JDK', choices: ['jdk11'], description: 'The JDK base image version to use for the TCK image.')
        string(name: 'TCK_VERSION', defaultValue: '3.1.6', description: 'The version of the Jakarta JPA TCK i.e. `2.2.0` or `3.0.1`')
        string(name: 'TCK_SHA', defaultValue: '790ca7a2a95ea098cfedafa2689c0d7a379fa62c74fed9505dd23191292f59fe', description: 'The SHA256 of the Jakarta JPA TCK that is distributed under https://download.eclipse.org/jakartaee/persistence/3.1/jakarta-persistence-tck-${TCK_VERSION}.zip.sha256')
		string(name: 'TCK_URL', defaultValue: '', description: 'The URL from which to download the TCK ZIP file. Only needed for testing staged builds. Ensure the TCK_VERSION variable matches the ZIP file name suffix.')
        booleanParam(name: 'NO_SLEEP', defaultValue: true, description: 'Whether the NO_SLEEP patch should be applied to speed up the TCK execution')
	}
    stages {
        stage('Build') {
        	steps {
                requireApprovalForPullRequest 'hibernate'
				script {
					docker.withRegistry('https://index.docker.io/v1/', 'hibernateci.hub.docker.com') {
						docker.image('openjdk:11-jdk').pull()
					}
				}
				dir('hibernate') {
					checkout scm
					sh './gradlew publishToMavenLocal -PmavenMirror=nexus-load-balancer-c4cf05fd92f43ef8.elb.us-east-1.amazonaws.com -DjakartaJpaVersion=3.1.0'
					script {
						env.HIBERNATE_VERSION = sh (
							script: "grep hibernateVersion gradle/version.properties|cut -d'=' -f2",
							returnStdout: true
						).trim()
					}
				}
				dir('tck') {
					checkout changelog: false, poll: false, scm: [$class: 'GitSCM', branches: [[name: '*/main']], extensions: [], userRemoteConfigs: [[url: 'https://github.com/hibernate/jakarta-tck-runner.git']]]
					script {
						if ( params.TCK_URL == null || params.TCK_URL.isEmpty() ) {
							sh "cd jpa-3.1; docker build -f Dockerfile.${params.IMAGE_JDK} -t jakarta-tck-runner --build-arg TCK_VERSION=${params.TCK_VERSION} --build-arg TCK_SHA=${params.TCK_SHA} ."
						}
						else {
							sh "cd jpa-3.1; docker build -f Dockerfile.${params.IMAGE_JDK} -t jakarta-tck-runner --build-arg TCK_VERSION=${params.TCK_VERSION} --build-arg TCK_SHA=${params.TCK_SHA} --build-arg TCK_URL=${params.TCK_URL} ."
						}
					}
				}
			}
		}
		stage('Run TCK') {
			steps {
				sh """ \
					rm -Rf ./results
					docker rm -f tck || true
                    docker volume rm -f tck-vol || true
                    docker volume create tck-vol
					docker run -v ~/.m2/repository/org/hibernate:/root/.m2/repository/org/hibernate:z -v tck-vol:/tck/persistence-tck/tmp/:z -e NO_SLEEP=${params.NO_SLEEP} -e HIBERNATE_VERSION=$HIBERNATE_VERSION --name tck jakarta-tck-runner
					docker cp tck:/tck/persistence-tck/tmp/ ./results
				"""
				archiveArtifacts artifacts: 'results/**'
				script {
					failures = sh (
						script: """ \
						    set +x
							while read line; do
							  if [[ "\$line" != *"Passed." ]]; then
								echo "\$line"
							  fi
							done <results/JTreport/text/summary.txt
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
    		configFileProvider([configFile(fileId: 'job-configuration.yaml', variable: 'JOB_CONFIGURATION_FILE')]) {
            	notifyBuildResult maintainers: (String) readYaml(file: env.JOB_CONFIGURATION_FILE).notification?.email?.recipients
            }
        }
    }
}