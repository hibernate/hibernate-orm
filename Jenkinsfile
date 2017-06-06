#!groovy​

String statusRecipients = 'andrea@hibernate.org, steve@hibernate.org, chris@hibernate.org, rus@gradle.com, lbbbarreiro@gmail.com, xiaobo.liao@gmail.com, dreborier@gmail.com, ckproductions.lab@gmail.com, fabio.t.ueno@gmail.com, nicholaskim94@gmail.com, buurman.sven@gmail.com, gbadner@redhat.com, galovicsarnold@gmail.com, gunnar.morling@googlemail.com, leonard.siu@gmail.com, jgigov@abv.bg, rvansa@redhat.com, mihalcea.vlad@gmail.com, al.stanislav@gmail.com, christian.beikov@gmail.com, jmartisk@redhat.com, candrews@integralblue.com, sanne@hibernate.org, rafael.wth@gmail.com, max.tian.23@gmail.com, stonio@github.map.fastly.net, brett@3riverdev.com, evandro@MacBook-Pro-de-Evandro.local'

String jdk9StatusRecipients = 'andrea@hibernate.org, steve@hibernate.org, chris@hibernate.org'

// TODO: timeout after 70 minutes
node('OS1') {
	try {
		stage('Checkout source') {
			checkout scm
		}
		
		// hibernate-orm-master-h2-main
		stage('Test H2') {
			withJavaEnv() {
				status = sh (script: "./gradlew clean test publish -Dmaven.settings=/home/jenkins/.m2/settings-orm-release.xml", returnStatus: true)
				step([$class: 'JUnitResultArchiver', testResults: '**/target/test-results/test/*.xml'])
				handleStatusCode(status, statusRecipients)
				stash "binary"
			}
		}

	} catch (exc) {
		onException(exc, statusRecipients)
	}
}

parallel (
	"H2 Check" : {
		node('OS1') {
			// Should this be run in parallel or just have a separte job?
			// TODO: timeout after 90 minutes
			try {
				// hibernate-orm-master-h2-check
				checkout scm
				withJavaEnv() {
					status = sh (script: "./gradlew clean check -x test -Dmaven.settings=/home/jenkins/.m2/settings-orm-release.xml", returnStatus: true)
					// TODO: handle checkstyle and findbugs
					// **/target/reports/checkstyle/*.xml
					// **/target/reports/findbugs/*.xml
					// TODO: actually don't send email in case of failure
					handleStatusCode(status, statusRecipients)
				}
			} catch (exc) {
				onException(exc, statusRecipients)
			}
		}
	},
	"Nightly docs" : {
		node('OS1') {
			// TODO: timeout after 60 minutes
			try {
				// hibernate-orm-master-nightly-docs
				checkout scm
				withJavaEnv() {
					status = sh (script: "./gradlew clean :release:assembleDocumentation -Dmaven.settings=/home/jenkins/.m2/settings-orm-release.xml", returnStatus: true)
					handleStatusCode(status, statusRecipients)
					// TODO: publish java docs
				}
			} catch (exc) {
				onException(exc, statusRecipients)
			}
		}
	},
	"H2 JDK9" : {
		node('OS1') {
			// TODO: timeout after 90 minutes
			try {
				// hibernate-orm-master-h2-JDK9
				checkout scm
				withJava9Env() {
					sh "java -version"
					status = sh (script: "./gradlew clean test", returnStatus: true)
					step([$class: 'JUnitResultArchiver', testResults: '**/target/test-results/test/*.xml'])
					handleStatusCode(status, jdk9StatusRecipients)
				}
			} catch (exc) {
				onException(exc, jdk9StatusRecipients)
			}
		}
	},
	"PostgreSQL" : {
		node('OS1') {
			try {
				unstash "binary"
				withJavaEnv() {
					status = sh (script: "./gradlew matrix_pgsql", returnStatus: true)
					step([$class: 'JUnitResultArchiver', testResults: '**/target/test-results/test/*.xml'])
					handleStatusCode(status, statusRecipients)
				}
			} catch (exc) {
				onException(exc, statusRecipients)
			}
		}
	}
)

void onException(exc, String recipient) {
	printException(exc)
	handleStatusCode(1, recipient)
}

void handleStatusCode(statusCode, String recipient) {
	if (statusCode == 0) {
		return
	}

    currentBuild.result = 'FAILURE'

	mail subject: "${env.JOB_NAME} (${env.BUILD_NUMBER}) failed",
			// TODO: add output and changes?
			body: "It appears that ${env.BUILD_URL} is failing, somebody should do something about that",
			to: recipient,
			replyTo: 'ci@hibernate.org',
			from: 'ci@hibernate.org'
	// TODO: send info to IRC and HipChat?
}

/* This code shame-lessly copied and pasted from some Jenkinsfile code abayer
   wrote for the jenkinsci/jenkins project */

void withJavaEnv(List envVars = [], def body) {
	String jdktool = tool name: "Oracle JDK 8", type: 'hudson.model.JDK'

	// Set JAVA_HOME, and special PATH variables for the tools we're
	// using.
	List javaEnv = ["PATH+JDK=${jdktool}/bin", "JAVA_HOME=${jdktool}"]

	// Add any additional environment variables.
	javaEnv.addAll(envVars)

	// Invoke the body closure we're passed within the environment we've created.
	withEnv(javaEnv) {
		body.call()
	}
}

void withJava9Env(List envVars = [], def body) {
	String jdktool = tool name: "Preview-JDK9", type: 'hudson.model.JDK'

	// Set JAVA_HOME, and special PATH variables for the tools we're
	// using.
	List javaEnv = ["PATH+JDK=${jdktool}/bin", "JAVA_HOME=${jdktool}"]

	// Add any additional environment variables.
	javaEnv.addAll(envVars)

	// Invoke the body closure we're passed within the environment we've created.
	withEnv(javaEnv) {
		body.call()
	}
}

void printException(exc) {
    def sw = new StringWriter()
    exc.printStackTrace(new PrintWriter(sw))
    echo sw.toString()
}