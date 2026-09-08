/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.tooling.dialectprovider;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;

import org.hibernate.orm.tooling.classification.internal.ClassificationMetadataResolver;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.LocalState;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;
import org.hibernate.orm.tooling.dialectprovider.internal.ClassificationMetadata;
import org.hibernate.orm.tooling.dialectprovider.internal.ClassificationMetadataReader;
import org.hibernate.orm.tooling.dialectprovider.internal.HibernateVersions;

/// Resolves and authenticates the classification metadata for the Hibernate
/// ORM release family used by a Dialect provider.
///
/// @author Steve Ebersole
/// @since 8.0
@DisableCachingByDefault(because = "Resolves authenticated remote metadata into a shared conditional cache")
public abstract class ResolveDialectProviderClassificationMetadata extends DefaultTask {
	public ResolveDialectProviderClassificationMetadata() {
		// Remote family documents are mutable. Always check their checksum;
		// a configured local file still benefits from Gradle input tracking.
		getOutputs().upToDateWhen( task -> getClassificationMetadataFile().isPresent() );
	}

	@Input
	public abstract Property<String> getHibernateVersion();

	@Input
	public abstract Property<String> getResolvedCoreVersion();

	@Input
	public abstract Property<String> getPluginVersion();

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

	@LocalState
	public abstract DirectoryProperty getSharedCacheDirectory();

	@OutputFile
	public abstract RegularFileProperty getResolvedMetadataFile();

	@Internal
	public final String getResolvedFamily() {
		return HibernateVersions.family( getHibernateVersion().get() );
	}

	@TaskAction
	public void resolve() {
		final String requestedVersion = getHibernateVersion().get();
		final String resolvedVersion = getResolvedCoreVersion().get();
		if ( !requestedVersion.equals( resolvedVersion ) ) {
			throw new GradleException(
					"Configured Hibernate ORM version " + requestedVersion
							+ " does not agree with resolved hibernate-core " + resolvedVersion
			);
		}
		HibernateVersions.verifyFamily( getPluginVersion().get(), resolvedVersion );
		final String family = HibernateVersions.family( resolvedVersion );
		final Path output = getResolvedMetadataFile().get().getAsFile().toPath();
		try {
			Files.createDirectories( output.getParent() );
			if ( getClassificationMetadataFile().isPresent() ) {
				final Path local = getClassificationMetadataFile().get().getAsFile().toPath();
				validate( local, family );
				Files.copy( local, output, StandardCopyOption.REPLACE_EXISTING );
				return;
			}
			if ( requestedVersion.toUpperCase( Locale.ROOT ).contains( "SNAPSHOT" )
					&& "https://docs.hibernate.org/orm".equals( trimSlash(
							getClassificationMetadataBaseUrl().get()
					) ) ) {
				throw new GradleException(
						"Snapshot Hibernate ORM versions require classificationMetadataFile or a configured published mirror"
				);
			}
			resolveRemote( family, output );
		}
		catch (IOException e) {
			throw new GradleException( "Unable to resolve Hibernate Dialect-provider classification metadata", e );
		}
	}

	private void resolveRemote(String family, Path output) throws IOException {
		try {
			new ClassificationMetadataResolver(
					getSharedCacheDirectory().get().getAsFile().toPath(),
					getClassificationMetadataBaseUrl().get(),
					family
			).resolve(
					output, getOffline().get(), getRefreshDependencies().get(), false,
					file -> validate( file, family ), message -> getLogger().warn( message )
			);
		}
		catch (IllegalArgumentException e) {
			throw new GradleException( e.getMessage(), e );
		}
	}

	private static void validate(Path metadataFile, String expectedFamily) {
		final ClassificationMetadata metadata = new ClassificationMetadataReader().read( metadataFile );
		if ( !expectedFamily.equals( metadata.family() ) ) {
			throw new GradleException(
					"Classification metadata belongs to Hibernate ORM " + metadata.family()
							+ " but the provider uses " + expectedFamily
			);
		}
		if ( metadata.sourceVersion() == null || metadata.sourceVersion().isBlank() ) {
			throw new GradleException( "Classification metadata does not identify its exact source version" );
		}
	}
	private static String trimSlash(String value) {
		return value.replaceAll( "/+$", "" );
	}

}
