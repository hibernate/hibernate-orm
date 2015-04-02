/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2015, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.build.animalsniffer

import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.JavaPlugin

import groovy.transform.Canonical

import org.codehaus.mojo.animal_sniffer.ClassListBuilder;
import org.codehaus.mojo.animal_sniffer.SignatureChecker
import org.codehaus.mojo.animal_sniffer.logging.Logger
import org.slf4j.LoggerFactory

class AnimalSnifferPlugin implements Plugin<Project> {
	private org.slf4j.Logger logger = LoggerFactory.getLogger( this.class )

	@Override
	void apply(Project project) {
		project.configurations.maybeCreate( "animalSnifferSignature" )
		final AnimalSnifferExtension extension = project.extensions.create( "animalSniffer", AnimalSnifferExtension )

		project.tasks.findByName( JavaPlugin.CLASSES_TASK_NAME ).doLast(
				new Action<Task>() {
					@Override
					void execute(Task task) {
						if ( extension.skip ) {
							return;
						}

						def logger = new GradleLogger( logger )
						def signatures = project.configurations.animalSnifferSignature.resolvedConfiguration.resolvedArtifacts*.file

						signatures.each {
							task.logger.lifecycle( "Starting AnimalSniffer checks against [${it.name}]" )
							SignatureChecker signatureChecker = new SignatureChecker(
									it.newInputStream(),
									getIgnoredTypes( project ),
									logger
							)
							signatureChecker.setCheckJars( false );

							List<File> sourceDirs = new ArrayList<File>();
							sourceDirs.addAll( task.project.sourceSets.main.java.srcDirs )
							signatureChecker.setSourcePath( sourceDirs )

							signatureChecker.process( project.file( task.project.sourceSets.main.output.classesDir ) );

							if ( signatureChecker.isSignatureBroken() ) {
								throw new GradleException(
										"Signature errors found. Verify them and ignore them with the proper annotation if needed."
								);
							}
						}
					}
				}
		);
	}

	/**
	 * Returns all those types which may be referenced although they are not part of the given signature, i.e. the
	 * types of the currently compiled project and types contributed by compile/provided dependencies.
	 */
	private Set<String> getIgnoredTypes(Project project) {
		def logger = new GradleLogger( logger )
		def ClassListBuilder clb = new ClassListBuilder( logger )

		// Any references to classes from the current project are fine
		clb.process( project.file( project.sourceSets.main.output.classesDir ) )

		// Any references to classes from compile/provided dependencies are fine
		// TODO: This could be made configurable via includes/excludes as done for the Maven plug-in
		def dependencies = project.configurations.compile.resolvedConfiguration.resolvedArtifacts*.file +
			project.configurations.provided.resolvedConfiguration.resolvedArtifacts*.file

		dependencies.each {
			clb.process( it )
		}

		// it's ignored types actually, not packages
		return clb.getPackages()
	}
}

@Canonical
class GradleLogger implements Logger {
	@Delegate
	org.slf4j.Logger logger
}

class AnimalSnifferExtension {
	String signature = ""
	String[] signatures = []
	boolean skip = false
}