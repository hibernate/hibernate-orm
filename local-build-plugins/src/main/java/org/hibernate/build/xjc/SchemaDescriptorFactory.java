/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.build.xjc;

import org.gradle.api.NamedDomainObjectFactory;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.Directory;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskProvider;

import static org.gradle.api.tasks.SourceSet.MAIN_SOURCE_SET_NAME;

/**
 * Used as the factory for instances added to the {@link XjcExtension#getSchemas()} container.
 * <p>
 * For each schema descriptor, an XjcTask is created and wired up.
 *
 * @author Steve Ebersole
 */
public class SchemaDescriptorFactory implements NamedDomainObjectFactory<SchemaDescriptor> {
	private final Provider<Directory> baseOutputDirectory;
	private final TaskProvider<Task> groupingTaskRef;
	private final Project project;

	public SchemaDescriptorFactory(Provider<Directory> baseOutputDirectory, TaskProvider<Task> groupingTaskRef, Project project) {
		this.baseOutputDirectory = baseOutputDirectory;
		this.groupingTaskRef = groupingTaskRef;
		this.project = project;
	}

	@Override
	public SchemaDescriptor create(String name) {
		final SchemaDescriptor schemaDescriptor = new SchemaDescriptor( name, project );

		final String taskName = determineXjcTaskName( schemaDescriptor );
		final Provider<Directory> taskOutputDirectory = baseOutputDirectory.map( directory -> directory.dir( name ) );

		// register the XjcTask for the schema
		final TaskProvider<XjcTask> xjcTaskRef = project.getTasks().register( taskName, XjcTask.class, (task) -> {
			task.setGroup( "xjc" );
			task.setDescription( "XJC generation for the " + name + " descriptor" );

			// wire up the inputs and outputs
			task.getSchemaName().convention( name );
			task.getXsdFile().convention( schemaDescriptor.getXsdFile() );
			task.getXjcBindingFile().convention( schemaDescriptor.getXjcBindingFile() );
			task.getXjcPlugins().convention( schemaDescriptor.getXjcPlugins() );
			task.getOutputDirectory().convention( taskOutputDirectory );
		} );

		final SourceSetContainer sourceSets = project.getExtensions().getByType( SourceSetContainer.class );
		final SourceSet mainSourceSet = sourceSets.getByName( MAIN_SOURCE_SET_NAME );
		mainSourceSet.getJava().srcDir( xjcTaskRef );

		groupingTaskRef.configure( (groupingTask) -> {
			groupingTask.dependsOn( xjcTaskRef );
		} );

		return schemaDescriptor;
	}

	private static String determineXjcTaskName(SchemaDescriptor schemaDescriptor) {
		final char initialLetterCap = Character.toUpperCase( schemaDescriptor.getName().charAt( 0 ) );
		final String rest = schemaDescriptor.getName().substring( 1 );

		return "xjc" + initialLetterCap + rest;
	}
}
