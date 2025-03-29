/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.build.maven.embedder;

import org.gradle.api.Project;
import org.gradle.api.file.DirectoryProperty;

import javax.inject.Inject;

/**
 * Gradle DSL extension for configuring the {@linkplain MavenEmbedderPlugin maven-embedder plugin}
 *
 * @author Steve Ebersole
 */
public class MavenEmbedderConfig {
	private DirectoryProperty localRepositoryDirectory;

	@Inject
	public MavenEmbedderConfig(Project project) {
		localRepositoryDirectory = project.getObjects().directoryProperty();
		localRepositoryDirectory.convention( project.getLayout().getBuildDirectory().dir( "maven-embedder/maven-local" ) );
	}

	public DirectoryProperty getLocalRepositoryDirectory() {
		return localRepositoryDirectory;
	}

	public void setLocalRepositoryDirectory(DirectoryProperty localRepositoryDirectory) {
		this.localRepositoryDirectory = localRepositoryDirectory;
	}
}
