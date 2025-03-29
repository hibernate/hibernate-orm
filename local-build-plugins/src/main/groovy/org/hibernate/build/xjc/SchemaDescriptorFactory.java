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
	private final XjcExtension xjcExtension;
	private final Task groupingTask;
	private final Project project;

	public SchemaDescriptorFactory(XjcExtension xjcExtension, Task groupingTask, Project project) {
		this.xjcExtension = xjcExtension;
		this.groupingTask = groupingTask;
		this.project = project;
	}

	@Override
	public SchemaDescriptor create(String name) {
		final SchemaDescriptor schemaDescriptor = new SchemaDescriptor( name, project );

		final String taskName = determineXjcTaskName( schemaDescriptor );
		final Provider<Directory> taskOutputDirectory = xjcExtension.getOutputDirectory().dir( name );

		// register the XjcTask for the schema
		final TaskProvider<XjcTask> xjcTaskRef = project.getTasks().register( taskName, XjcTask.class, (task) -> {
			task.setGroup( "xjc" );
			task.setDescription( "XJC generation for the " + name + " descriptor" );

			// wire up the inputs and outputs
			task.getXsdFile().set( schemaDescriptor.getXsdFile() );
			task.getXjcBindingFile().set( schemaDescriptor.getXjcBindingFile() );
			task.getXjcExtensions().set( schemaDescriptor.___xjcExtensions() );
			task.getOutputDirectory().set( taskOutputDirectory );
		} );

		final SourceSetContainer sourceSets = project.getExtensions().getByType( SourceSetContainer.class );
		final SourceSet mainSourceSet = sourceSets.getByName( MAIN_SOURCE_SET_NAME );
		mainSourceSet.getJava().srcDir( xjcTaskRef );

		groupingTask.dependsOn( xjcTaskRef );

		return schemaDescriptor;
	}

	private static String determineXjcTaskName(SchemaDescriptor schemaDescriptor) {
		assert schemaDescriptor.getName() != null;

		final char initialLetterCap = Character.toUpperCase( schemaDescriptor.getName().charAt( 0 ) );
		final String rest = schemaDescriptor.getName().substring( 1 );

		return "xjc" + initialLetterCap + rest;
	}
}
