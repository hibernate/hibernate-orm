/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.tooling.dialectprovider.internal;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import com.sun.net.httpserver.HttpServer;
import org.hibernate.orm.tooling.classification.internal.ClassificationMetadataResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Exercises the shared resolver's cache isolation, concurrent writers, and
/// validation-before-publication guarantees independently of Gradle tasks.
///
/// @author Steve Ebersole
class ClassificationMetadataResolverTest {
	@TempDir
	Path directory;

	@Test
	void cachesAreIsolatedByOriginAndFamily() throws Exception {
		final Path first = ClassificationMetadataResolver.cacheDirectory( directory, "https://first.example/orm", "8.0" );
		assertEquals( first, ClassificationMetadataResolver.cacheDirectory( directory, "https://first.example/orm/", "8.0" ) );
		assertTrue( !first.equals( ClassificationMetadataResolver.cacheDirectory( directory, "https://second.example/orm", "8.0" ) ) );
		assertTrue( !first.equals( ClassificationMetadataResolver.cacheDirectory( directory, "https://first.example/orm", "8.1" ) ) );
		final AtomicReference<String> document = new AtomicReference<>( "valid" );
		final HttpServer server = server( document );
		try {
			resolver( server ).resolve( directory.resolve( "first.json" ), false, false, false, this::validate, this::unexpectedWarning );
			assertThrows( java.io.IOException.class, () -> new ClassificationMetadataResolver( directory, "https://second.example", "8.0" )
					.resolve( directory.resolve( "second.json" ), true, false, false, this::validate, this::unexpectedWarning ) );
		}
		finally {
			server.stop( 0 );
		}
	}

	@Test
	void bootstrapDoesNotTreatAPartiallyPublishedBaselineAsAbsent() throws Exception {
		for ( boolean checksumPresent : new boolean[] { true, false } ) {
			final HttpServer server = HttpServer.create( new InetSocketAddress( "127.0.0.1", 0 ), 0 );
			server.createContext( "/8.0/metadata/classifications.json.gz", exchange -> {
				exchange.sendResponseHeaders( checksumPresent ? 404 : 200, -1 );
				exchange.close();
			} );
			server.createContext( "/8.0/metadata/classifications.json.gz.sha256", exchange -> {
				if ( checksumPresent ) {
					final byte[] bytes = "0".repeat( 64 ).getBytes( StandardCharsets.UTF_8 );
					exchange.sendResponseHeaders( 200, bytes.length );
					exchange.getResponseBody().write( bytes );
				}
				else {
					exchange.sendResponseHeaders( 404, -1 );
				}
				exchange.close();
			} );
			server.start();
			try {
				final Class<? extends Exception> expected = checksumPresent ? java.io.IOException.class : IllegalArgumentException.class;
				assertThrows( expected,
						() -> resolver( server ).resolve( directory.resolve( "bootstrap.json" ),
								false, true, true, this::validate, this::unexpectedWarning ) );
			}
			finally {
				server.stop( 0 );
			}
		}
	}

	@Test
	void concurrentResolversPublishConsistentOutputs() throws Exception {
		final HttpServer server = server( new AtomicReference<>( "valid" ) );
		final var executor = Executors.newFixedThreadPool( 4 );
		try {
			final var jobs = new ArrayList<Callable<Boolean>>();
			for ( int i = 0; i < 12; i++ ) {
				final Path output = directory.resolve( "output-" + i + ".json" );
				jobs.add( () -> {
					resolver( server ).resolve( output, false, true, false, this::validate, this::unexpectedWarning );
					return "valid".equals( Files.readString( output ) );
				} );
			}
			for ( var result : executor.invokeAll( jobs ) ) {
				assertTrue( result.get() );
			}
			resolver( server ).resolve( directory.resolve( "offline.json" ), true, false, false, this::validate, this::unexpectedWarning );
			assertEquals( "valid", Files.readString( directory.resolve( "offline.json" ) ) );
			try ( var files = Files.walk( directory ) ) {
				assertTrue( files.noneMatch( file -> file.getFileName().toString().endsWith( ".tmp" )
						|| file.getFileName().toString().startsWith( "candidate-" ) ) );
			}
		}
		finally {
			executor.shutdownNow();
			server.stop( 0 );
		}
	}

	@Test
	void invalidDownloadedSchemaDoesNotReplaceValidatedCache() throws Exception {
		final AtomicReference<String> document = new AtomicReference<>( "valid" );
		final HttpServer server = server( document );
		try {
			final Path output = directory.resolve( "metadata.json" );
			resolver( server ).resolve( output, false, false, false, this::validate, this::unexpectedWarning );
			document.set( "invalid" );
			assertThrows( IllegalArgumentException.class,
					() -> resolver( server ).resolve( output, false, false, false, this::validate, this::unexpectedWarning ) );
			resolver( server ).resolve( output, true, false, false, this::validate, this::unexpectedWarning );
			assertEquals( "valid", Files.readString( output ) );
		}
		finally {
			server.stop( 0 );
		}
	}

	private ClassificationMetadataResolver resolver(HttpServer server) {
		return new ClassificationMetadataResolver( directory, "http://127.0.0.1:" + server.getAddress().getPort(), "8.0" );
	}

	private void validate(Path path) {
		try {
			if ( !"valid".equals( Files.readString( path ) ) ) {
				throw new IllegalArgumentException( "Invalid schema" );
			}
		}
		catch (java.io.IOException e) {
			throw new IllegalArgumentException( e );
		}
	}

	private void unexpectedWarning(String message) {
		throw new AssertionError( message );
	}

	private static HttpServer server(AtomicReference<String> document) throws Exception {
		final HttpServer server = HttpServer.create( new InetSocketAddress( "127.0.0.1", 0 ), 0 );
		server.createContext( "/8.0/metadata/classifications.json.gz", exchange -> {
			final byte[] bytes = document.get().getBytes( StandardCharsets.UTF_8 );
			exchange.sendResponseHeaders( 200, bytes.length );
			exchange.getResponseBody().write( bytes );
			exchange.close();
		} );
		server.createContext( "/8.0/metadata/classifications.json.gz.sha256", exchange -> {
			try {
				final byte[] checksum = HexFormat.of().formatHex( MessageDigest.getInstance( "SHA-256" )
						.digest( document.get().getBytes( StandardCharsets.UTF_8 ) ) ).getBytes( StandardCharsets.UTF_8 );
				exchange.sendResponseHeaders( 200, checksum.length );
				exchange.getResponseBody().write( checksum );
			}
			catch (java.security.NoSuchAlgorithmException e) {
				throw new AssertionError( e );
			}
			finally {
				exchange.close();
			}
		} );
		server.start();
		return server;
	}
}
