/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.tooling.gradle.enhance;

import java.util.concurrent.atomic.AtomicBoolean;
import javax.inject.Inject;

import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.execution.TaskExecutionAdapter;
import org.gradle.api.execution.TaskExecutionGraph;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskState;
import org.gradle.work.Incremental;
import org.gradle.work.InputChanges;

import org.hibernate.orm.tooling.gradle.HibernateOrmSpec;

import static org.hibernate.orm.tooling.gradle.HibernateOrmSpec.HIBERNATE;

/**
 * @author Steve Ebersole
 */
public class EnhancementTask extends DefaultTask {
	public static final String DSL_NAME = "hibernateEnhance";

	public static void apply(HibernateOrmSpec ormDsl, SourceSet mainSourceSet, Project project) {
		final EnhancementTask enhancementTask = project.getTasks().create(
				DSL_NAME,
				EnhancementTask.class,
				ormDsl,
				mainSourceSet,
				project
		);
		enhancementTask.setGroup( HIBERNATE );
		enhancementTask.setDescription( "Performs Hibernate ORM enhancement of the project's compiled classes" );

		final String compileJavaTaskName = mainSourceSet.getCompileJavaTaskName();
		final Task compileJavaTask = project.getTasks().getByName( compileJavaTaskName );
		enhancementTask.dependsOn( compileJavaTask );
		compileJavaTask.finalizedBy( enhancementTask );
	}

	private final EnhancementSpec enhancementDsl;
	private final DirectoryProperty javaCompileOutputDirectory;
	private final DirectoryProperty outputDirectory;

	@Inject
	@SuppressWarnings( "UnstableApiUsage" )
	public EnhancementTask(HibernateOrmSpec ormSpec, SourceSet mainSourceSet, Project project) {
		this.enhancementDsl = ormSpec.getEnhancementSpec();

		javaCompileOutputDirectory = mainSourceSet.getJava().getDestinationDirectory();

		outputDirectory = project.getObjects().directoryProperty();
		outputDirectory.set( project.getLayout().getBuildDirectory().dir( "tmp/hibernateEnhancement" ) );

		final AtomicBoolean didCompileRun = new AtomicBoolean( false );

		final TaskExecutionGraph taskGraph = project.getGradle().getTaskGraph();

		taskGraph.addTaskExecutionListener(
				new TaskExecutionAdapter() {
					@Override
					public void afterExecute(Task task, TaskState state) {
						super.afterExecute( task, state );
						if ( "compileJava".equals( task.getName() ) ) {
							if ( state.getDidWork() ) {
								didCompileRun.set( true );
							}
						}

						taskGraph.removeTaskExecutionListener( this );
					}
				}
		);

		getOutputs().upToDateWhen( (task) -> ! didCompileRun.get() );
	}

	@InputDirectory
	@Incremental
	public DirectoryProperty getJavaCompileDirectory() {
		return javaCompileOutputDirectory;
	}

	@OutputDirectory
	public DirectoryProperty getOutputDirectory() {
		return outputDirectory;
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
