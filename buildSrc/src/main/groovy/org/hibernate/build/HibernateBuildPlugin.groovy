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
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication

/**
 * @author Steve Ebersole
 */
class HibernateBuildPlugin implements Plugin<Project> {
	@Override
	void apply(Project project) {
		if ( !JavaVersion.current().java8Compatible ) {
			throw new GradleException( "Gradle must be run with Java 8" )
		}

		MavenPublishingExtension publishingExtension = project.extensions.create( "mavenPom", MavenPublishingExtension )

		project.afterEvaluate {
			applyPublishing( publishingExtension, project )
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
