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
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.SkipWhenEmpty;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskAction;

/**
 * @author Steve Ebersole
 */
public class MetamodelGenTask extends DefaultTask {
	public static final String DSL_NAME = "generateJpaMetamodel";

	public static void apply(HibernateOrmSpec ormDsl, SourceSet mainSourceSet, Project project) {
		final MetamodelGenTask genTask = project.getTasks().create( DSL_NAME, MetamodelGenTask.class, ormDsl, mainSourceSet, project );

		final String compileJavaTaskName = mainSourceSet.getCompileJavaTaskName();
		final Task compileJavaTask = project.getTasks().getByName( compileJavaTaskName );
		genTask.dependsOn( compileJavaTask );

		final Task compileResourcesTask = project.getTasks().getByName( "processResources" );
		genTask.dependsOn( compileResourcesTask );
	}

	private final FileCollection javaClasses;
	private final FileCollection resources;
	private final DirectoryProperty outputDirectory;

	@Inject
	public MetamodelGenTask(HibernateOrmSpec ormDsl, SourceSet mainSourceSet, Project project) {
		javaClasses = mainSourceSet.getAllJava();
		resources = mainSourceSet.getResources();

		outputDirectory = project.getObjects().directoryProperty();
		outputDirectory.convention( ormDsl.getOutputDirectory().dir( "jpa-metamodel" ) );
	}

	@InputFiles
	@SkipWhenEmpty
	public FileCollection getJavaClasses() {
		return javaClasses;
	}

	@InputFiles
	@SkipWhenEmpty
	public FileCollection getResources() {
		// for access to XML mappings
		return resources;
	}

	@OutputDirectory
	public DirectoryProperty getOutputDirectory() {
		return outputDirectory;
	}

	@TaskAction
	public void generateJpaMetamodel() {
		getProject().getLogger().lifecycle( "{} task is not yet implemented - here for future", DSL_NAME );
	}
}
