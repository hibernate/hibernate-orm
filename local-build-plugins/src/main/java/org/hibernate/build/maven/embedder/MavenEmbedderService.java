/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.build.maven.embedder;

import org.apache.maven.cli.MavenCli;
import org.gradle.api.GradleException;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.services.BuildService;
import org.gradle.api.services.BuildServiceParameters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Wrapper around the Maven Embedder as a Gradle build-service
 *
 * @author Steve Ebersole
 */
public abstract class MavenEmbedderService implements BuildService<MavenEmbedderService.Config> {
	interface Config extends BuildServiceParameters {
		Property<String> getProjectVersion();
		DirectoryProperty getWorkingDirectory();
		DirectoryProperty getMavenLocalDirectory();
	}

	private final MavenCli embedder;

	public MavenEmbedderService() {
		// this needs to be set for some reason.
		// NOTE : even though it is named "multi module", here we are only interested in the specific project
		System.setProperty( "maven.multiModuleProjectDirectory", getParameters().getWorkingDirectory().toString() );
		embedder = new MavenCli();
	}

	public void execute(String... tasksAndArgs) {
		final List<String> cml = new ArrayList<>();
		Collections.addAll( cml, tasksAndArgs );

		final Directory mavenLocalDirectory = getParameters().getMavenLocalDirectory().get();
		cml.add( "-Dmaven.repo.local=" + mavenLocalDirectory.getAsFile().getAbsolutePath() );
		cml.add( "-Dorm.project.version=" + getParameters().getProjectVersion().get() );

		final Directory workingDirectory = getParameters().getWorkingDirectory().get();
		final String workingDirectoryPath = workingDirectory.getAsFile().getAbsolutePath();

		// todo : consider bridging Maven out/err to Gradle logging

		final int resultCode = embedder.doMain( cml.toArray(new String[0]), workingDirectoryPath, System.out, System.err );
		if (resultCode != 0) {
			StringBuilder sb = new StringBuilder();
			for (String s : tasksAndArgs) {
				sb.append( s );
			}
			throw new GradleException("Maven execution has failed: " + sb);
		}
	}
}
