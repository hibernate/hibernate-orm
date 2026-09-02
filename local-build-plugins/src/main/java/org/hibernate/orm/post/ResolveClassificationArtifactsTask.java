/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.post;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.Set;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;

/// Resolves the exact artifact manifest carried by classification metadata.
///
/// @author Steve Ebersole
/// @since 8.0
@DisableCachingByDefault(because = "Delegates exact module resolution to Gradle's dependency cache")
public abstract class ResolveClassificationArtifactsTask extends DefaultTask {
	@InputFile
	@Optional
	@PathSensitive(PathSensitivity.NONE)
	public abstract RegularFileProperty getClassificationMetadataFile();

	@OutputDirectory
	public abstract DirectoryProperty getArtifactsDirectory();

	@TaskAction
	public void resolve() {
		final ClassificationMetadata metadata = new ClassificationMetadataJson().read(
				getClassificationMetadataFile().get().getAsFile().toPath()
		);
		if ( metadata.getArtifacts().isEmpty() ) {
			throw new GradleException(
					"Classification metadata for Hibernate ORM " + metadata.getHibernateVersion()
							+ " does not contain a migration artifact manifest"
			);
		}
		final Set<String> coordinates = new HashSet<>();
		final Set<String> files = new HashSet<>();
		for ( ClassificationMetadata.Artifact artifact : metadata.getArtifacts() ) {
			if ( !coordinates.add( artifact.getCoordinates() ) ) {
				throw new GradleException( "Duplicate classification artifact coordinates " + artifact.getCoordinates() );
			}
			if ( !files.add( artifact.getFileName() ) ) {
				throw new GradleException( "Duplicate classification artifact file " + artifact.getFileName() );
			}
			if ( artifact.isHibernateOrmModule() && !metadata.getSourceVersion().equals( artifact.getVersion() ) ) {
				throw new GradleException(
						"Hibernate ORM artifact " + artifact.getCoordinates()
								+ " does not match metadata source version " + metadata.getSourceVersion()
				);
			}
		}

		final File output = getArtifactsDirectory().get().getAsFile();
		getProject().delete( output );
		if ( !output.mkdirs() && !output.isDirectory() ) {
			throw new GradleException( "Unable to create classification artifact directory " + output );
		}
		for ( ClassificationMetadata.Artifact artifact : metadata.getArtifacts() ) {
			final Configuration detached = getProject().getConfigurations().detachedConfiguration(
					getProject().getDependencies().create( artifact.getCoordinates() )
			);
			detached.setTransitive( false );
			final Set<File> resolved = detached.resolve();
			if ( resolved.size() != 1 ) {
				throw new GradleException(
						"Expected one artifact for " + artifact.getCoordinates() + " but resolved " + resolved.size()
				);
			}
			final File source = resolved.iterator().next();
			if ( !artifact.getFileName().equals( source.getName() ) ) {
				throw new GradleException(
						"Resolved " + artifact.getCoordinates() + " as " + source.getName()
								+ " but metadata requires " + artifact.getFileName()
				);
			}
			try {
				Files.copy(
						source.toPath(),
						Path.of( output.getAbsolutePath(), artifact.getFileName() ),
						StandardCopyOption.REPLACE_EXISTING
				);
			}
			catch (IOException e) {
				throw new GradleException( "Unable to stage classification artifact " + artifact.getCoordinates(), e );
			}
		}
	}
}
