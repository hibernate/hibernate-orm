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
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.OutputDirectory;

/**
 * @author Steve Ebersole
 */
public abstract class XjcExtension {
	private final DirectoryProperty outputDirectory;
	private final Property<String> jaxbBasicsVersion;
	private final NamedDomainObjectContainer<SchemaDescriptor> schemas;

	@Inject
	public XjcExtension(Task groupingTask, Project project) {
		outputDirectory = project.getObjects().directoryProperty();
		outputDirectory.convention( project.getLayout().getBuildDirectory().dir( "generated/sources/xjc/main" ) );

		jaxbBasicsVersion = project.getObjects().property( String.class );
		jaxbBasicsVersion.convention( "2.2.1" );

		// Create a dynamic container for SchemaDescriptor definitions by the user.
		// 		- for each schema they define, create a Task to perform the "compilation"
		schemas = project.container( SchemaDescriptor.class, new SchemaDescriptorFactory( this, groupingTask, project ) );
	}

	@OutputDirectory
	public DirectoryProperty getOutputDirectory() {
		return outputDirectory;
	}

	public Property<String> getJaxbBasicsVersion() {
		return jaxbBasicsVersion;
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
