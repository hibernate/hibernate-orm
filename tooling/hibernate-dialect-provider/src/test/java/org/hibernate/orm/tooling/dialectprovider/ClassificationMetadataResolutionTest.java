/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.tooling.dialectprovider;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.concurrent.atomic.AtomicInteger;

import com.sun.net.httpserver.HttpServer;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/// Tests authenticated release-family classification metadata resolution.
///
/// @author Steve Ebersole
public class ClassificationMetadataResolutionTest {
	@TempDir
	Path temporaryDirectory;

	@Test
	void localMetadataTakesPrecedenceAndAllowsSnapshots() throws Exception {
		final Path local = writeMetadata( temporaryDirectory.resolve( "local.json" ), "8.1", "8.1.0-SNAPSHOT" );
		final ResolveDialectProviderClassificationMetadata task = task( "8.1.0-SNAPSHOT" );
		task.getClassificationMetadataFile().set( local.toFile() );
		task.getClassificationMetadataBaseUrl().set( "http://127.0.0.1:1/unreachable" );

		task.resolve();

		assertEquals( Files.readString( local ), Files.readString( output() ) );
	}

	@Test
	void rejectsMetadataFromAnotherFamily() throws Exception {
		final Path local = writeMetadata( temporaryDirectory.resolve( "wrong-family.json" ), "8.0", "8.0.9" );
		final ResolveDialectProviderClassificationMetadata task = task( "8.1.2" );
		task.getClassificationMetadataFile().set( local.toFile() );

		assertThrows( GradleException.class, task::resolve );
	}

	@Test
	void rejectsUnpublishedSnapshotWithoutAnExplicitSource() {
		final ResolveDialectProviderClassificationMetadata task = task( "8.1.0-SNAPSHOT" );

		assertThrows( GradleException.class, task::resolve );
	}

	@Test
	void resolvesSnapshotMetadataFromAConfiguredMirror() throws Exception {
		final byte[] metadata = metadata( "8.1", "8.1.0-SNAPSHOT" ).getBytes( StandardCharsets.UTF_8 );
		final HttpServer server = server( metadata, checksum( metadata ) );
		try {
			final ResolveDialectProviderClassificationMetadata task = task( "8.1.0-SNAPSHOT" );
			task.getClassificationMetadataBaseUrl().set( "http://127.0.0.1:" + server.getAddress().getPort() );

			task.resolve();

			assertEquals( new String( metadata, StandardCharsets.UTF_8 ), Files.readString( output() ) );
		}
		finally {
			server.stop( 0 );
		}
	}

	@Test
	void authenticatesMetadataFromAConfiguredMirror() throws Exception {
		final byte[] metadata = metadata( "8.1", "8.1.3" ).getBytes( StandardCharsets.UTF_8 );
		final HttpServer server = server( metadata, checksum( metadata ) );
		try {
			final ResolveDialectProviderClassificationMetadata task = task( "8.1.2" );
			task.getClassificationMetadataBaseUrl().set( "http://127.0.0.1:" + server.getAddress().getPort() );

			task.resolve();

			assertEquals( new String( metadata, StandardCharsets.UTF_8 ), Files.readString( output() ) );
			assertEquals( "8.1.3", new org.hibernate.orm.tooling.dialectprovider.internal.ClassificationMetadataReader()
					.read( output() ).sourceVersion() );
		}
		finally {
			server.stop( 0 );
		}
	}

	@Test
	void rejectsAMirrorChecksumMismatchWithoutCachingIt() throws Exception {
		final byte[] metadata = metadata( "8.1", "8.1.3" ).getBytes( StandardCharsets.UTF_8 );
		final HttpServer server = server( metadata, "0".repeat( 64 ) );
		try {
			final ResolveDialectProviderClassificationMetadata task = task( "8.1.2" );
			task.getClassificationMetadataBaseUrl().set( "http://127.0.0.1:" + server.getAddress().getPort() );

			assertThrows( GradleException.class, task::resolve );
		}
		finally {
			server.stop( 0 );
		}
	}

	@Test
	void reusesAValidatedFamilyCacheOffline() throws Exception {
		final byte[] metadata = seedCache();
		final ResolveDialectProviderClassificationMetadata task = task( "8.1.2" );
		task.getOffline().set( true );

		task.resolve();

		assertEquals( new String( metadata, StandardCharsets.UTF_8 ), Files.readString( output() ) );
	}

	@Test
	void reportsAnOfflineCacheMiss() {
		final ResolveDialectProviderClassificationMetadata task = task( "8.1.2" );
		task.getOffline().set( true );

		assertThrows( GradleException.class, task::resolve );
	}

	@Test
	void rejectsAnUnsupportedLocalSchema() throws Exception {
		final Path local = temporaryDirectory.resolve( "unsupported.json" );
		Files.writeString( local, metadata( "8.1", "8.1.2" ).replace( "\"schemaVersion\":1", "\"schemaVersion\":99" ) );
		final ResolveDialectProviderClassificationMetadata task = task( "8.1.2" );
		task.getClassificationMetadataFile().set( local.toFile() );

		assertThrows( IllegalArgumentException.class, task::resolve );
	}

	@Test
	void rejectsMalformedJsonAndGzipDocuments() throws Exception {
		final Path malformedJson = temporaryDirectory.resolve( "malformed.json" );
		Files.writeString( malformedJson, "{" );
		final ResolveDialectProviderClassificationMetadata jsonTask = task( "8.1.2" );
		jsonTask.getClassificationMetadataFile().set( malformedJson.toFile() );
		assertThrows( IllegalArgumentException.class, jsonTask::resolve );

		final Path malformedGzip = temporaryDirectory.resolve( "malformed.json.gz" );
		Files.write( malformedGzip, new byte[] { 0x1f, (byte) 0x8b, 0 } );
		final ResolveDialectProviderClassificationMetadata gzipTask = task( "8.1.2" );
		gzipTask.getClassificationMetadataFile().set( malformedGzip.toFile() );
		assertThrows( IllegalArgumentException.class, gzipTask::resolve );
	}

	@Test
	void rejectsMissingFamilyAndSourceProvenance() throws Exception {
		final Path missingFamily = temporaryDirectory.resolve( "missing-family.json" );
		Files.writeString( missingFamily, metadata( "8.1", "8.1.2" ).replace( "\"hibernateVersion\":\"8.1\",", "" ) );
		final ResolveDialectProviderClassificationMetadata familyTask = task( "8.1.2" );
		familyTask.getClassificationMetadataFile().set( missingFamily.toFile() );
		assertThrows( IllegalArgumentException.class, familyTask::resolve );

		final Path missingSource = temporaryDirectory.resolve( "missing-source.json" );
		Files.writeString( missingSource, metadata( "8.1", "8.1.2" ).replace( "\"sourceVersion\":\"8.1.2\",", "" ) );
		final ResolveDialectProviderClassificationMetadata sourceTask = task( "8.1.2" );
		sourceTask.getClassificationMetadataFile().set( missingSource.toFile() );
		assertThrows( IllegalArgumentException.class, sourceTask::resolve );
	}

	@Test
	void transientFailureFallsBackToValidatedCacheUnlessRefreshWasExplicit() throws Exception {
		final byte[] metadata = seedCache();
		final HttpServer server = failingServer();
		try {
			final String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
			final ResolveDialectProviderClassificationMetadata ordinary = task( "8.1.2" );
			ordinary.getClassificationMetadataBaseUrl().set( baseUrl );
			ordinary.resolve();
			assertEquals( new String( metadata, StandardCharsets.UTF_8 ), Files.readString( output() ) );

			final ResolveDialectProviderClassificationMetadata refresh = task( "8.1.2" );
			refresh.getClassificationMetadataBaseUrl().set( baseUrl );
			refresh.getRefreshDependencies().set( true );
			assertThrows( GradleException.class, refresh::resolve );
		}
		finally {
			server.stop( 0 );
		}
	}

	@Test
	void matchingRemoteChecksumReusesCachedBytesWithoutDownloadingMetadata() throws Exception {
		final byte[] metadata = seedCache();
		final AtomicInteger metadataRequests = new AtomicInteger();
		final HttpServer server = server( metadata, checksum( metadata ), metadataRequests );
		try {
			final ResolveDialectProviderClassificationMetadata task = task( "8.1.2" );
			task.getClassificationMetadataBaseUrl().set( "http://127.0.0.1:" + server.getAddress().getPort() );
			task.resolve();

			assertEquals( 0, metadataRequests.get() );
		}
		finally {
			server.stop( 0 );
		}
	}

	@Test
	void explicitRefreshDownloadsMetadataEvenWhenTheChecksumMatchesTheCache() throws Exception {
		final byte[] metadata = seedCache();
		final AtomicInteger metadataRequests = new AtomicInteger();
		final HttpServer server = server( metadata, checksum( metadata ), metadataRequests );
		try {
			final ResolveDialectProviderClassificationMetadata task = task( "8.1.2" );
			task.getClassificationMetadataBaseUrl().set( "http://127.0.0.1:" + server.getAddress().getPort() );
			task.getRefreshDependencies().set( true );

			task.resolve();

			assertEquals( 1, metadataRequests.get() );
		}
		finally {
			server.stop( 0 );
		}
	}

	private ResolveDialectProviderClassificationMetadata task(String version) {
		final Project project = ProjectBuilder.builder().withProjectDir( temporaryDirectory.toFile() ).build();
		final ResolveDialectProviderClassificationMetadata task = project.getTasks().create(
				"resolveMetadata" + Math.abs( version.hashCode() ),
				ResolveDialectProviderClassificationMetadata.class
		);
		task.getHibernateVersion().set( version );
		task.getResolvedCoreVersion().set( version );
		task.getPluginVersion().set( "8.1.0" );
		task.getClassificationMetadataBaseUrl().set( "https://docs.hibernate.org/orm" );
		task.getOffline().set( false );
		task.getRefreshDependencies().set( false );
		task.getSharedCacheDirectory().set( temporaryDirectory.resolve( "cache" ).toFile() );
		task.getResolvedMetadataFile().set( output().toFile() );
		return task;
	}

	private Path output() {
		return temporaryDirectory.resolve( "output/classifications.json.gz" );
	}

	private static Path writeMetadata(Path path, String family, String sourceVersion) throws Exception {
		Files.writeString( path, metadata( family, sourceVersion ) );
		return path;
	}

	private static String metadata(String family, String sourceVersion) {
		return "{\"schema\":\"hibernate-orm-classifications\",\"schemaVersion\":1,"
				+ "\"hibernateVersion\":\"" + family + "\",\"sourceVersion\":\"" + sourceVersion
				+ "\",\"elements\":[]}";
	}

	private static String checksum(byte[] bytes) throws Exception {
		return HexFormat.of().formatHex( MessageDigest.getInstance( "SHA-256" ).digest( bytes ) );
	}

	private static HttpServer server(byte[] metadata, String checksum) throws Exception {
		return server( metadata, checksum, new AtomicInteger() );
	}

	private static HttpServer server(byte[] metadata, String checksum, AtomicInteger metadataRequests) throws Exception {
		final HttpServer server = HttpServer.create( new InetSocketAddress( "127.0.0.1", 0 ), 0 );
		server.createContext( "/8.1/metadata/classifications.json.gz", exchange -> {
			metadataRequests.incrementAndGet();
			exchange.sendResponseHeaders( 200, metadata.length );
			exchange.getResponseBody().write( metadata );
			exchange.close();
		} );
		final byte[] checksumBytes = ( checksum + "  classifications.json.gz\n" ).getBytes( StandardCharsets.UTF_8 );
		server.createContext( "/8.1/metadata/classifications.json.gz.sha256", exchange -> {
			exchange.sendResponseHeaders( 200, checksumBytes.length );
			exchange.getResponseBody().write( checksumBytes );
			exchange.close();
		} );
		server.start();
		return server;
	}

	private HttpServer failingServer() throws Exception {
		final HttpServer server = HttpServer.create( new InetSocketAddress( "127.0.0.1", 0 ), 0 );
		server.createContext( "/", exchange -> {
			exchange.sendResponseHeaders( 503, -1 );
			exchange.close();
		} );
		server.start();
		return server;
	}

	private byte[] seedCache() throws Exception {
		final byte[] metadata = metadata( "8.1", "8.1.4" ).getBytes( StandardCharsets.UTF_8 );
		final Path familyCache = temporaryDirectory.resolve( "cache/8.1" );
		Files.createDirectories( familyCache );
		Files.write( familyCache.resolve( "classifications.json.gz" ), metadata );
		Files.writeString(
				familyCache.resolve( "classifications.json.gz.sha256" ),
				checksum( metadata ) + "  classifications.json.gz\n"
		);
		return metadata;
	}
}
