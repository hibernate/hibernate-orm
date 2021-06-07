@Library('hibernate-jenkins-pipeline-helpers@1.5') _

pipeline {
    agent {
        label 'LongDuration'
    }
    tools {
        jdk 'OpenJDK 8 Latest'
    }
    parameters {
        booleanParam(name: 'NO_SLEEP', defaultValue: true, description: 'Whether the NO_SLEEP patch should be applied to speed up the TCK execution')
	}
    stages {
        stage('Build') {
        	steps {
				script {
					docker.withRegistry('https://index.docker.io/v1/', 'hibernateci.hub.docker.com') {
						docker.image('openjdk:8-jdk').pull()
					}
				}
				dir('hibernate') {
					checkout scm
					sh """ \
						./gradlew publishToMavenLocal
					"""
					script {
						env.HIBERNATE_VERSION = sh (
							script: "grep hibernateVersion gradle/version.properties|cut -d'=' -f2",
							returnStdout: true
						).trim()
					}
				}
				dir('tck') {
					checkout changelog: false, poll: false, scm: [$class: 'GitSCM', branches: [[name: '*/main']], extensions: [], userRemoteConfigs: [[url: 'https://github.com/hibernate/jakarta-tck-runner.git']]]
					sh """ \
						cd jpa-2.2; docker build -t jakarta-tck-runner .
					"""
				}
			}
		}
		stage('Run TCK') {
			steps {
				sh """ \
					docker rm -f tck || true
                    docker rm -f tck-vol || true
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
            // Space-separated
            notifyBuildResult maintainers: 'christian.beikov@gmail.com'
        }
    }
}