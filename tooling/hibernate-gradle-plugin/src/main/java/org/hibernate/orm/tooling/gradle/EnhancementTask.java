/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.tooling.gradle;

import javax.inject.Inject;

import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.SkipWhenEmpty;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.Incremental;
import org.gradle.work.InputChanges;

import org.hibernate.Internal;

/**
 * @author Steve Ebersole
 */
public class EnhancementTask extends DefaultTask {
	public static final String DSL_NAME = "hibernateEnhance";

	public static void apply(HibernateOrmSpec ormDsl, SourceSet mainSourceSet, Project project) {
		final EnhancementTask enhancementTask = project.getTasks().create( DSL_NAME, EnhancementTask.class, ormDsl.getEnhancementDsl(), mainSourceSet );

		final String compileJavaTaskName = mainSourceSet.getCompileJavaTaskName();
		final Task compileJavaTask = project.getTasks().getByName( compileJavaTaskName );
		enhancementTask.dependsOn( compileJavaTask );
		compileJavaTask.finalizedBy( enhancementTask );
	}

	private final EnhancementSpec enhancementDsl;
	private final DirectoryProperty javaCompileOutputDirectory;

	@Inject
	public EnhancementTask(EnhancementSpec enhancementDsl, SourceSet mainSourceSet) {
		this.enhancementDsl = enhancementDsl;
		javaCompileOutputDirectory = mainSourceSet.getJava().getDestinationDirectory();
	}

	@Internal
	@InputDirectory
	@SkipWhenEmpty
	@Incremental
	public DirectoryProperty getJavaCompileDirectory() {
		return javaCompileOutputDirectory;
	}

	@OutputDirectory
	public DirectoryProperty getOutputDirectory() {
		return enhancementDsl.getOutputDirectory();
	}

	@TaskAction
	public void enhanceClasses(InputChanges inputChanges) {
		if ( !enhancementDsl.hasAnythingToDo() ) {
			return;
		}

		if ( !inputChanges.isIncremental() ) {
			getProject().getLogger().debug( "EnhancementTask inputs were not incremental" );
		}

		if ( enhancementDsl.getEnableExtendedEnhancement().get() ) {
			// for extended enhancement, we may need to enhance everything...
			//		for now, assume we don't
			getProject().getLogger().info( "Performing extended enhancement" );
		}

		EnhancementHelper.enhance( javaCompileOutputDirectory, inputChanges, enhancementDsl, getProject() );
	}
}
