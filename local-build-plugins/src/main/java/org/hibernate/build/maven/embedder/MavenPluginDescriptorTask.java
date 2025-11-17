/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.build.maven.embedder;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.services.ServiceReference;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

/**
 * Task which "mirrors" the Maven task/goal/mojo for generating
 * plugin descriptors.
 *
 * @author Steve Ebersole
 */
public abstract class MavenPluginDescriptorTask extends DefaultTask {
	// This property provides access to the service instance
	@ServiceReference
	abstract Property<MavenEmbedderService> getMavenEmbedderService();

	@OutputDirectory
	abstract DirectoryProperty getDescriptorDirectory();

	public MavenPluginDescriptorTask() {
		// todo : what else is the descriptor/pom dependent upon?
		getInputs().property( "project-version", getProject().getVersion() );
	}

	@TaskAction
	public void generateDescriptor() {
		performDescriptorGeneration();
	}

	private void performDescriptorGeneration() {
		getMavenEmbedderService().get().execute( "plugin:descriptor" );
	}

}
