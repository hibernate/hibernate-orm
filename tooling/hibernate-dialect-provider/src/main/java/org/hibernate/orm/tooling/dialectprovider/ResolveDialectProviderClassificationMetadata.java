/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.tooling.dialectprovider;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;

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
		final Path familyCache = getSharedCacheDirectory().get().getAsFile().toPath().resolve( family );
		final Path cachedMetadata = familyCache.resolve( "classifications.json.gz" );
		final Path cachedChecksum = familyCache.resolve( "classifications.json.gz.sha256" );
		if ( getOffline().get() ) {
			if ( !validCache( cachedMetadata, cachedChecksum, family ) ) {
				throw new GradleException(
						"No validated classification metadata is cached for Hibernate ORM " + family
								+ "; run online or configure classificationMetadataFile"
				);
			}
			Files.copy( cachedMetadata, output, StandardCopyOption.REPLACE_EXISTING );
			return;
		}

		final String base = trimSlash( getClassificationMetadataBaseUrl().get() ) + "/" + family + "/metadata/";
		try {
			final byte[] checksumBytes = download( URI.create( base + "classifications.json.gz.sha256" ) );
			final String expected = parseChecksum( new String( checksumBytes, StandardCharsets.UTF_8 ) );
			if ( !getRefreshDependencies().get()
					&& Files.isRegularFile( cachedMetadata )
					&& expected.equals( digest( Files.readAllBytes( cachedMetadata ) ) ) ) {
				validate( cachedMetadata, family );
				Files.copy( cachedMetadata, output, StandardCopyOption.REPLACE_EXISTING );
				return;
			}

			final byte[] metadataBytes = download( URI.create( base + "classifications.json.gz" ) );
			final String actual = digest( metadataBytes );
			if ( !expected.equals( actual ) ) {
				throw new GradleException(
						"Classification metadata checksum mismatch for Hibernate ORM " + family
								+ ": expected " + expected + " but received " + actual
				);
			}
			Files.createDirectories( familyCache );
			writeAtomically( cachedMetadata, metadataBytes );
			writeAtomically( cachedChecksum, checksumBytes );
			validate( cachedMetadata, family );
			Files.copy( cachedMetadata, output, StandardCopyOption.REPLACE_EXISTING );
		}
		catch (IOException e) {
			if ( !getRefreshDependencies().get() && validCache( cachedMetadata, cachedChecksum, family ) ) {
				getLogger().warn(
						"Unable to refresh Hibernate ORM {} classification metadata; using the validated cached copy",
						family
				);
				Files.copy( cachedMetadata, output, StandardCopyOption.REPLACE_EXISTING );
				return;
			}
			throw e;
		}
	}

	private static boolean validCache(Path metadata, Path checksum, String family) {
		if ( !Files.isRegularFile( metadata ) || !Files.isRegularFile( checksum ) ) {
			return false;
		}
		try {
			final String expected = parseChecksum( Files.readString( checksum, StandardCharsets.UTF_8 ) );
			if ( !expected.equals( digest( Files.readAllBytes( metadata ) ) ) ) {
				return false;
			}
			validate( metadata, family );
			return true;
		}
		catch (RuntimeException | IOException e) {
			return false;
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

	private static byte[] download(URI uri) throws IOException {
		final HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
		connection.setConnectTimeout( 10_000 );
		connection.setReadTimeout( 30_000 );
		connection.setRequestProperty( "Accept-Encoding", "identity" );
		final int status = connection.getResponseCode();
		if ( status < 200 || status >= 300 ) {
			throw new IOException( "HTTP " + status + " resolving " + uri );
		}
		try ( InputStream input = connection.getInputStream() ) {
			return input.readAllBytes();
		}
		finally {
			connection.disconnect();
		}
	}

	private static String parseChecksum(String contents) {
		final String trimmed = contents.trim();
		final int separator = trimmed.indexOf( ' ' );
		final String digest = ( separator < 0 ? trimmed : trimmed.substring( 0, separator ) ).toLowerCase( Locale.ROOT );
		if ( digest.length() != 64 || !digest.matches( "[0-9a-f]{64}" ) ) {
			throw new GradleException( "Malformed SHA-256 classification metadata checksum" );
		}
		if ( separator >= 0 && !trimmed.substring( separator ).trim().endsWith( "classifications.json.gz" ) ) {
			throw new GradleException( "Classification metadata checksum names an unexpected file" );
		}
		return digest;
	}

	private static String digest(byte[] bytes) {
		try {
			return HexFormat.of().formatHex( MessageDigest.getInstance( "SHA-256" ).digest( bytes ) );
		}
		catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException( "SHA-256 is not available", e );
		}
	}

	private static void writeAtomically(Path target, byte[] contents) throws IOException {
		final Path temporary = target.resolveSibling( target.getFileName() + ".tmp" );
		Files.write( temporary, contents );
		try {
			Files.move( temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING );
		}
		catch (java.nio.file.AtomicMoveNotSupportedException e) {
			Files.move( temporary, target, StandardCopyOption.REPLACE_EXISTING );
		}
	}

	private static String trimSlash(String value) {
		int end = value.length();
		while ( end > 0 && value.charAt( end - 1 ) == '/' ) {
			end--;
		}
		return value.substring( 0, end );
	}
}
