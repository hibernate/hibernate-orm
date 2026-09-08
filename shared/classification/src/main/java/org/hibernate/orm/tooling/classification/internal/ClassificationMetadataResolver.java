/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.tooling.classification.internal;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.function.Consumer;

/// Shared transport and cache for family classification metadata. Both Gradle
/// builds compile this source so build bootstrapping needs no published tooling
/// dependency. Callers retain version selection and metadata-schema validation.
///
/// Cache entries are scoped by metadata origin and family. A file lock protects
/// the metadata/checksum pair across tasks and processes, and unique temporary
/// files ensure readers never observe a partially written output.
///
/// @author Steve Ebersole
public final class ClassificationMetadataResolver {
	private static final String METADATA = "classifications.json.gz";
	private static final String CHECKSUM = METADATA + ".sha256";
	private final String family;
	private final String base;
	private final Path cache;

	public ClassificationMetadataResolver(Path cacheRoot, String baseUrl, String family) {
		this.family = family;
		this.base = trimSlash( baseUrl ) + '/' + family + "/metadata/";
		this.cache = cacheDirectory( cacheRoot, baseUrl, family );
	}

	public static Path cacheDirectory(Path root, String baseUrl, String family) {
		return root.resolve( digest( trimSlash( baseUrl ).getBytes( StandardCharsets.UTF_8 ) ) ).resolve( family );
	}

	/// Resolve a validated document, returning false only for an explicitly
	/// allowed absent baseline. Refresh requests never fall back to cached data.
	public boolean resolve(
			Path output, boolean offline, boolean refresh, boolean allowMissing,
			Consumer<Path> validate, Consumer<String> warning) throws IOException {
		Files.createDirectories( cache );
		Files.createDirectories( output.getParent() );
		try ( FileChannel channel = FileChannel.open( cache.resolve( "metadata.lock" ),
				StandardOpenOption.CREATE, StandardOpenOption.WRITE );
				FileLock ignored = lock( channel ) ) {
			final Path metadata = cache.resolve( METADATA );
			final Path checksum = cache.resolve( CHECKSUM );
			if ( offline ) {
				if ( !validCache( metadata, checksum, validate ) ) {
					throw new IOException( "No validated classification metadata is cached for Hibernate ORM " + family
							+ "; run online or configure classificationMetadataFile" );
				}
				writeAtomically( output, Files.readAllBytes( metadata ) );
				return true;
			}
			try {
				final byte[] checksumBytes = download( URI.create( base + CHECKSUM ) );
				final String expected = parseChecksum( new String( checksumBytes, StandardCharsets.UTF_8 ) );
				if ( !refresh && Files.isRegularFile( metadata ) && expected.equals( digest( Files.readAllBytes( metadata ) ) ) ) {
					validate.accept( metadata );
					writeAtomically( checksum, checksumBytes );
					writeAtomically( output, Files.readAllBytes( metadata ) );
					return true;
				}
				final byte[] bytes = download( URI.create( base + METADATA ) );
				if ( !expected.equals( digest( bytes ) ) ) {
					throw new IllegalArgumentException( "Classification metadata checksum mismatch for Hibernate ORM " + family );
				}
				final Path candidate = Files.createTempFile( cache, "candidate-", ".json.gz" );
				try {
					Files.write( candidate, bytes );
					validate.accept( candidate );
					writeAtomically( metadata, bytes );
					writeAtomically( checksum, checksumBytes );
					writeAtomically( output, bytes );
				}
				finally {
					Files.deleteIfExists( candidate );
				}
				return true;
			}
			catch (IOException e) {
				if ( !refresh && validCache( metadata, checksum, validate ) ) {
					warning.accept( "Unable to refresh Hibernate ORM " + family
							+ " classification metadata; using the validated cached copy" );
					writeAtomically( output, Files.readAllBytes( metadata ) );
					return true;
				}
				if ( e instanceof MissingMetadataException && allowMissing
						&& ((MissingMetadataException) e).uri.equals( URI.create( base + CHECKSUM ) ) ) {
					if ( remoteExists( URI.create( base + METADATA ) ) ) {
						throw new IllegalArgumentException( "Classification metadata already exists for Hibernate ORM " + family
								+ " but its authenticated checksum is unavailable" );
					}
					Files.deleteIfExists( output );
					return false;
				}
				throw e;
			}
		}
	}

	private static FileLock lock(FileChannel channel) throws IOException {
		// Blocking FileChannel.lock() throws for overlapping locks in the same
		// JVM. Retry covers both separate Gradle daemons and plugin classloaders.
		while ( true ) {
			try {
				final FileLock lock = channel.tryLock();
				if ( lock != null ) {
					return lock;
				}
			}
			catch (OverlappingFileLockException ignored) {
			}
			try {
				Thread.sleep( 25 );
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new IOException( "Interrupted waiting for classification metadata cache", e );
			}
		}
	}

	private static boolean validCache(Path metadata, Path checksum, Consumer<Path> validate) {
		try {
			if ( !Files.isRegularFile( metadata ) || !Files.isRegularFile( checksum )
					|| !parseChecksum( Files.readString( checksum ) ).equals( digest( Files.readAllBytes( metadata ) ) ) ) {
				return false;
			}
			validate.accept( metadata );
			return true;
		}
		catch (RuntimeException | IOException e) {
			return false;
		}
	}

	private static byte[] download(URI uri) throws IOException {
		final HttpURLConnection connection = connection( uri );
		try {
			checkStatus( connection, uri );
			try ( InputStream input = connection.getInputStream() ) {
				return input.readAllBytes();
			}
		}
		finally {
			connection.disconnect();
		}
	}

	private static boolean remoteExists(URI uri) throws IOException {
		final HttpURLConnection connection = connection( uri );
		connection.setRequestMethod( "HEAD" );
		try {
			checkStatus( connection, uri );
			return true;
		}
		catch (MissingMetadataException e) {
			return false;
		}
		finally {
			connection.disconnect();
		}
	}

	private static HttpURLConnection connection(URI uri) throws IOException {
		final HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
		connection.setConnectTimeout( 10_000 );
		connection.setReadTimeout( 30_000 );
		connection.setRequestProperty( "Accept-Encoding", "identity" );
		return connection;
	}

	private static void checkStatus(HttpURLConnection connection, URI uri) throws IOException {
		final int status = connection.getResponseCode();
		if ( status == HttpURLConnection.HTTP_NOT_FOUND ) {
			throw new MissingMetadataException( uri );
		}
		if ( status < 200 || status >= 300 ) {
			throw new IOException( "HTTP " + status + " resolving " + uri );
		}
	}

	private static String parseChecksum(String contents) {
		final String[] parts = contents.trim().split( "\\s+", 2 );
		final String checksum = parts[0].toLowerCase( Locale.ROOT );
		if ( !checksum.matches( "[0-9a-f]{64}" ) ) {
			throw new IllegalArgumentException( "Malformed SHA-256 classification metadata checksum" );
		}
		if ( parts.length > 1 && !parts[1].equals( METADATA ) && !parts[1].equals( '*' + METADATA ) ) {
			throw new IllegalArgumentException( "Classification metadata checksum names an unexpected file" );
		}
		return checksum;
	}

	private static String digest(byte[] bytes) {
		try {
			return HexFormat.of().formatHex( MessageDigest.getInstance( "SHA-256" ).digest( bytes ) );
		}
		catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException( "SHA-256 is not available", e );
		}
	}

	private static void writeAtomically(Path target, byte[] bytes) throws IOException {
		final Path temporary = Files.createTempFile( target.getParent(), target.getFileName() + "-", ".tmp" );
		try {
			Files.write( temporary, bytes );
			try {
				Files.move( temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING );
			}
			catch (java.nio.file.AtomicMoveNotSupportedException e) {
				Files.move( temporary, target, StandardCopyOption.REPLACE_EXISTING );
			}
		}
		finally {
			Files.deleteIfExists( temporary );
		}
	}

	private static String trimSlash(String value) {
		return value.replaceAll( "/+$", "" );
	}

	private static final class MissingMetadataException extends IOException {
		private final URI uri;

		private MissingMetadataException(URI uri) {
			super( "HTTP 404 resolving " + uri );
			this.uri = uri;
		}
	}
}
