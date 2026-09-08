/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.post;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.hibernate.orm.tooling.classification.internal.ClassificationMetadataResolver;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.LocalState;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;

/// Resolves and authenticates one release-family classification document.
///
/// @author Steve Ebersole
/// @since 8.0
@DisableCachingByDefault(because = "Resolves remote metadata into a shared conditional cache")
public abstract class ResolveClassificationMetadataTask extends DefaultTask {
	public ResolveClassificationMetadataTask() {
		// Remote family documents are mutable. Always check their checksum;
		// a configured local file still benefits from Gradle input tracking.
		getOutputs().upToDateWhen( task -> getClassificationMetadataFile().isPresent() );
	}

	@Input
	public abstract Property<String> getFamily();

	@Input
	public abstract Property<String> getClassificationMetadataBaseUrl();

	@InputFile
	@Optional
	@PathSensitive(PathSensitivity.NONE)
	public abstract RegularFileProperty getClassificationMetadataFile();

	@Input
	public abstract Property<Boolean> getOffline();

	@Input
	public abstract Property<Boolean> getRefreshDependencies();

	@Input
	public abstract Property<Boolean> getAllowMissing();

	@LocalState
	public abstract DirectoryProperty getSharedCacheDirectory();

	@OutputFile
	public abstract RegularFileProperty getResolvedMetadataFile();

	@TaskAction
	public void resolve() {
		final String family = MigrationCompatibilityFamilies.requireFamily( getFamily().get() );
		final Path output = getResolvedMetadataFile().get().getAsFile().toPath();
		try {
			Files.createDirectories( output.getParent() );
			Files.deleteIfExists( output );
			if ( getClassificationMetadataFile().isPresent() ) {
				final Path local = getClassificationMetadataFile().get().getAsFile().toPath();
				validate( local, family );
				Files.copy( local, output, StandardCopyOption.REPLACE_EXISTING );
				return;
			}
			resolveRemote( family, output );
		}
		catch (IOException e) {
			throw new GradleException(
					"Unable to resolve Hibernate ORM " + family + " classification metadata: " + e.getMessage(),
					e
			);
		}
	}

	private void resolveRemote(String family, Path output) throws IOException {
		try {
			final boolean resolved = new ClassificationMetadataResolver(
					getSharedCacheDirectory().get().getAsFile().toPath(),
					getClassificationMetadataBaseUrl().get(),
					family
			).resolve(
					output, getOffline().get(), getRefreshDependencies().get(), getAllowMissing().get(),
					file -> validate( file, family ), message -> getLogger().warn( message )
			);
			if ( !resolved ) {
				getLogger().lifecycle( "No published classification baseline exists for Hibernate ORM {}", family );
			}
		}
		catch (IllegalArgumentException e) {
			throw new GradleException( e.getMessage(), e );
		}
	}

	private static void validate(Path metadataFile, String expectedFamily) {
		final ClassificationMetadata metadata = new ClassificationMetadataJson().read( metadataFile );
		if ( !expectedFamily.equals( metadata.getHibernateVersion() ) ) {
			throw new GradleException(
					"Classification metadata belongs to Hibernate ORM " + metadata.getHibernateVersion()
							+ " but " + expectedFamily + " was requested"
			);
		}
		if ( metadata.getSourceVersion() == null || metadata.getSourceVersion().isBlank() ) {
			throw new GradleException( "Classification metadata does not identify its exact source version" );
		}
	}
}
