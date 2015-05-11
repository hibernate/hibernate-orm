/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.build

import org.gradle.api.GradleException
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.Test
import org.gradle.internal.jvm.Jvm

import org.hibernate.build.gradle.animalsniffer.AnimalSnifferExtension

/**
 * @author Steve Ebersole
 */
class HibernateBuildPlugin implements Plugin<Project> {
	@Override
	void apply(Project project) {
		if ( !JavaVersion.current().java8Compatible ) {
			throw new GradleException( "Gradle must be run with Java 8" )
		}

		project.apply( plugin: 'org.hibernate.build.gradle.animalSniffer' )

		final Jvm java6Home;
		if ( project.rootProject.extensions.extraProperties.has( 'java6Home' ) ) {
			java6Home = project.rootProject.extensions.extraProperties.get( 'java6Home' ) as Jvm
		}
		else {
			String java6HomeDirSetting = null;
			if ( project.hasProperty( "JAVA6_HOME" ) ) {
				java6HomeDirSetting = project.property( "JAVA6_HOME" ) as String;
			}
			if ( java6HomeDirSetting == null ) {
				java6HomeDirSetting = System.getProperty( "JAVA6_HOME" );
			}
			if ( java6HomeDirSetting == null ) {
				java6HomeDirSetting = System.getenv( "JAVA6_HOME" );
			}

			if ( java6HomeDirSetting != null ) {
				project.logger.info( "Using JAVA6_HOME setting [${java6HomeDirSetting}]" )

				final File specifiedJava6Home = project.file( java6HomeDirSetting );
				if ( specifiedJava6Home == null ) {
					throw new GradleException( "Could not resolve specified java home ${java6HomeDirSetting}" )
				}
				if ( !specifiedJava6Home.exists() ) {
					throw new GradleException( "Specified java home [${java6HomeDirSetting}] does not exist" )
				}
				if ( !specifiedJava6Home.isDirectory() ) {
					throw new GradleException( "Specified java home [${java6HomeDirSetting}] is not a directory" )
				}

				java6Home = Jvm.forHome( specifiedJava6Home ) as Jvm;

				if ( java6Home == null ) {
					throw new GradleException( "Could not resolve JAVA6_HOME [${java6HomeDirSetting}] to proper JAVA_HOME" );
				}

				project.rootProject.extensions.extraProperties.set( 'java6Home', java6Home )
			}
			else {
				project.logger.warn( "JAVA6_HOME setting not specified, some build features will be disabled" )
				java6Home = null;
			}
		}

		JavaTargetExtension javaTargetExtension = project.extensions.create( "javaTarget", JavaTargetExtension, project )
		MavenPublishingExtension publishingExtension = project.extensions.create( "mavenPom", MavenPublishingExtension )

		project.afterEvaluate {
			applyJavaTarget( javaTargetExtension, project, java6Home )
			applyPublishing( publishingExtension, project )
		}
	}

	def applyJavaTarget(JavaTargetExtension javaTargetExtension, Project project, Jvm java6Home) {

		project.logger.info( "Setting target Java version : ${javaTargetExtension.version} (${project.name})" )
		project.properties.put( 'sourceCompatibility', "${javaTargetExtension.version}" )
		project.properties.put( 'targetCompatibility', "${javaTargetExtension.version}" )


		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// apply AnimalSniffer

		if ( javaTargetExtension.version.java8Compatible ) {
			AnimalSnifferExtension animalSnifferExtension = project.extensions.findByType( AnimalSnifferExtension )
			if ( animalSnifferExtension == null ) {
				throw new GradleException( "Unable to locate AnimalSniffer extension" )
			}
			animalSnifferExtension.skip = true
		}
		else {
			// todo : we could really disable this if we set executable/bootClasspath below
			def sigConfig = project.configurations.animalSnifferSignature
			sigConfig.incoming.beforeResolve {
				sigConfig.dependencies.add( project.dependencies.create( 'org.codehaus.mojo.signature:java16:1.0@signature' ) )
			}
		}


		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Apply to compile task

		project.getConvention().findPlugin( JavaPluginConvention.class ).sourceSets.each { sourceSet ->
			JavaCompile javaCompileTask = project.tasks.findByName( sourceSet.compileJavaTaskName ) as JavaCompile

			// NOTE : this aptDir stuff is needed until we can have IntelliJ run annotation processors for us
			//		which cannot happen until we can fold hibernate-testing back into hibernate-core/src/test
			//		which cannot happen until... ugh
			File aptDir = project.file( "${project.buildDir}/generated-src/apt/${sourceSet.name}" )
			sourceSet.allJava.srcDir( aptDir )

			javaCompileTask.options.compilerArgs += [
					"-nowarn",
					"-encoding", "UTF-8",
					"-s", "${aptDir.absolutePath}"
			]
			javaCompileTask.doFirst {
				aptDir.mkdirs()
			}


			if ( sourceSet.name == 'main' ) {
				if ( javaTargetExtension.version.java8Compatible ) {
					javaCompileTask.options.compilerArgs += [
							"-source", '1.8',
							"-target", '1.8'
					]
				}
				else {
					javaCompileTask.options.compilerArgs += [
							"-source", '1.6',
							"-target", '1.6'
					]

					if ( java6Home != null ) {
						if ( javaTargetExtension.shouldApplyTargetToCompile ) {
							// Technically we need only one here between:
							//      1) setting the javac executable
							//      2) setting the bootClasspath
							// However, (1) requires fork=true whereas (2) does not.
							//					javaCompileTask.options.fork = true
							//					javaCompileTask.options.forkOptions.executable = java6Home.javacExecutable
							javaCompileTask.options.bootClasspath = java6Home.runtimeJar.absolutePath
						}
					}
				}
			}
		}
//
//
//		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
//		// Apply to test compile task
//
//		SourceSet testSourceSet = project.getConvention().findPlugin( JavaPluginConvention.class ).sourceSets.findByName( "test" )
//		JavaCompile compileTestTask = project.tasks.findByName( testSourceSet.compileJavaTaskName ) as JavaCompile
//
//		// NOTE : see the note above wrt aptDir
//		File testAptDir = project.file( "${project.buildDir}/generated-src/apt/test" )
//		testSourceSet.allJava.srcDir( testAptDir )
//
//		compileTestTask.options.compilerArgs += [
//				"-nowarn",
//				"-encoding", "UTF-8",
//				"-s", "${testAptDir.absolutePath}"
//		]
//		compileTestTask.doFirst {
//			testAptDir.mkdirs()
//		}

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Apply to test tasks

		project.tasks.withType( Test.class ).all { task->
			task.jvmArgs += ['-XX:+HeapDumpOnOutOfMemoryError', "-XX:HeapDumpPath=${project.file("${project.buildDir}/OOM-dump.hprof").absolutePath}"]

//			if ( !javaTargetExtension.version.java8Compatible && javaTargetExtension.shouldApplyTargetToTest ) {
//				// use Java 6 settings
//				task.executable = java6Home.javaExecutable
//				task.maxHeapSize = '2G'
//				task.jvmArgs += ['-XX:MaxPermGen=512M']
//			}
//			else {
				// use Java 8 settings
				task.maxHeapSize = '2G'
				task.jvmArgs += ['-XX:MetaspaceSize=512M']
//			}
		}
	}

	def applyPublishing(MavenPublishingExtension publishingExtension, Project project) {
		PublishingExtension gradlePublishingExtension = project.extensions.getByType( PublishingExtension )

		// repos
		if ( gradlePublishingExtension.repositories.empty ) {
			if ( project.version.endsWith( 'SNAPSHOT' ) ) {
				gradlePublishingExtension.repositories.maven {
					name 'jboss-snapshots-repository'
					url 'https://repository.jboss.org/nexus/content/repositories/snapshots'
				}
			}
			else {
				gradlePublishingExtension.repositories.maven {
					name 'jboss-releases-repository'
					url 'https://repository.jboss.org/nexus/service/local/staging/deploy/maven2/'
				}
			}
		}

		// pom
		gradlePublishingExtension.publications.withType( MavenPublication ).all { pub->
			final boolean applyExtensionValues = publishingExtension.publications == null || publishingExtension.publications.length == 0 || pub in publishingExtension.publications;

			pom.withXml {
				if ( applyExtensionValues ) {
					asNode().appendNode( 'name', publishingExtension.name )
					asNode().appendNode( 'description', publishingExtension.description )
					Node licenseNode = asNode().appendNode( "licenses" ).appendNode( "license" )
					if ( publishingExtension.license == MavenPublishingExtension.License.APACHE2 ) {
						licenseNode.appendNode( 'name', 'Apache License, Version 2.0' )
						licenseNode.appendNode( 'url', 'http://www.apache.org/licenses/LICENSE-2.0.txt' )
					}
					else {
						licenseNode.appendNode( 'name', 'GNU Lesser General Public License' )
						licenseNode.appendNode( 'url', 'http://www.gnu.org/licenses/lgpl-2.1.html' )
						licenseNode.appendNode( 'comments', 'See discussion at http://hibernate.org/license for more details.' )
					}
					licenseNode.appendNode( 'distribution', 'repo' )
				}

				asNode().children().last() + {
					url 'http://hibernate.org'
					organization {
						name 'Hibernate.org'
						url 'http://hibernate.org'
					}
					issueManagement {
						system 'jira'
						url 'https://hibernate.atlassian.net/browse/HHH'
					}
					scm {
						url 'http://github.com/hibernate/hibernate-orm'
						connection 'scm:git:http://github.com/hibernate/hibernate-orm.git'
						developerConnection 'scm:git:git@github.com:hibernate/hibernate-orm.git'
					}
					developers {
						developer {
							id 'hibernate-team'
							name 'The Hibernate Development Team'
							organization 'Hibernate.org'
							organizationUrl 'http://hibernate.org'
						}
					}
				}

				// TEMPORARY : currently Gradle Publishing feature is exporting dependencies as 'runtime' scope,
				//      rather than 'compile'; fix that.
				if ( asNode().dependencies != null ) {
					asNode().dependencies[0].dependency.each {
						it.scope[0].value = 'compile'
					}
				}
			}
		}
	}
}
