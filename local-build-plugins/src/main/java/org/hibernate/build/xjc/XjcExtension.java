/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.build.xjc;

import groovy.lang.Closure;
import jakarta.inject.Inject;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.tasks.TaskProvider;

/**
 * DSL extension for configuring XJC processing
 *
 * @author Steve Ebersole
 */
public abstract class XjcExtension {
	private final DirectoryProperty outputDirectory;
	private final NamedDomainObjectContainer<SchemaDescriptor> schemas;

	@Inject
	public XjcExtension(Project project) {
		// Create the xjc grouping task
		final TaskProvider<Task> groupingTaskRef = project.getTasks().register( "xjc", (groupingTask) -> {
			groupingTask.setGroup( "xjc" );
			groupingTask.setDescription( "Grouping task for executing one-or-more XJC compilations" );
		} );

		outputDirectory = project.getObjects().directoryProperty();
		outputDirectory.convention( project.getLayout().getBuildDirectory().dir( "generated/sources/xjc/main" ) );

		// create a dynamic container for SchemaDescriptor definitions by the user
		// 		- for each schema they define, create a Task to perform the "compilation"
		schemas = project.container(
				SchemaDescriptor.class,
				new SchemaDescriptorFactory( outputDirectory, groupingTaskRef, project )
		);
	}

	public DirectoryProperty getOutputDirectory() {
		return outputDirectory;
	}

	@SuppressWarnings("unused")
	public final NamedDomainObjectContainer<SchemaDescriptor> getSchemas() {
		return schemas;
	}

	@SuppressWarnings({ "unused", "rawtypes" })
	public NamedDomainObjectContainer<SchemaDescriptor> schemas(Closure closure) {
		return schemas.configure( closure );
	}

}
