/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.post;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.HexFormat;
import java.util.jar.JarOutputStream;

import com.sun.net.httpserver.HttpServer;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS;

/// Authenticated family metadata and exact artifact-resolution coverage.
///
/// @author Steve Ebersole
/// @since 8.0
public class ClassificationMigrationResolutionTests {
	@TempDir
	Path temporaryDirectory;

	@Test
	public void resolvesAuthenticatedRemoteMetadata() throws Exception {
		final byte[] metadata = compressedMetadata( "8.0", "8.0.7.Final", Collections.emptyList() );
		final HttpServer server = server( metadata, true );
		try {
			final ResolveClassificationMetadataTask task = metadataTask(
					"http://127.0.0.1:" + server.getAddress().getPort(),
					false
			);
			task.resolve();
			assertArrayEquals( metadata, Files.readAllBytes( task.getResolvedMetadataFile().get().getAsFile().toPath() ) );
		}
		finally {
			server.stop( 0 );
		}
	}

	@Test
	public void offlineResolutionUsesOnlyAnAuthenticatedCache() throws Exception {
		final byte[] metadata = compressedMetadata( "8.0", "8.0.7.Final", Collections.emptyList() );
		final HttpServer server = server( metadata, true );
		final ResolveClassificationMetadataTask task;
		try {
			task = metadataTask( "http://127.0.0.1:" + server.getAddress().getPort(), false );
			task.resolve();
		}
		finally {
			server.stop( 0 );
		}
		Files.delete( task.getResolvedMetadataFile().get().getAsFile().toPath() );
		task.getOffline().set( true );
		task.resolve();
		assertArrayEquals( metadata, Files.readAllBytes( task.getResolvedMetadataFile().get().getAsFile().toPath() ) );
	}

	@Test
	public void localMetadataMustMatchSelectedFamily() throws Exception {
		final Path local = temporaryDirectory.resolve( "wrong-family.json.gz" );
		Files.write( local, compressedMetadata( "8.1", "8.1.0.Final", Collections.emptyList() ) );
		final ResolveClassificationMetadataTask task = metadataTask( "https://unused.example", false );
		task.getClassificationMetadataFile().set( local.toFile() );
		assertThrows( GradleException.class, task::resolve );
	}

	@Test
	public void rejectsChecksumMismatch() throws Exception {
		final byte[] metadata = compressedMetadata( "8.0", "8.0.7.Final", Collections.emptyList() );
		final HttpServer server = server( metadata, false );
		try {
			final ResolveClassificationMetadataTask task = metadataTask(
					"http://127.0.0.1:" + server.getAddress().getPort(),
					false
			);
			assertThrows( GradleException.class, task::resolve );
		}
		finally {
			server.stop( 0 );
		}
	}

	@Test
	public void guardedBootstrapAllowsAbsentRemoteMetadata() throws Exception {
		final HttpServer server = HttpServer.create( new InetSocketAddress( "127.0.0.1", 0 ), 0 );
		server.createContext( "/8.0/metadata/classifications.json.gz.sha256", exchange -> {
			exchange.sendResponseHeaders( 404, -1 );
			exchange.close();
		} );
		server.start();
		try {
			final ResolveClassificationMetadataTask task = metadataTask(
					"http://127.0.0.1:" + server.getAddress().getPort(),
					true
			);
			task.resolve();
			assertFalse( task.getResolvedMetadataFile().get().getAsFile().isFile() );
		}
		finally {
			server.stop( 0 );
		}
	}

	@Test
	public void guardedBootstrapAllowsValidationWithoutResolvedBaseline() throws Exception {
		final HttpServer server = HttpServer.create( new InetSocketAddress( "127.0.0.1", 0 ), 0 );
		server.createContext( "/8.0/metadata/classifications.json.gz.sha256", exchange -> {
			exchange.sendResponseHeaders( 404, -1 );
			exchange.close();
		} );
		server.start();
		try {
			Files.writeString( temporaryDirectory.resolve( "settings.gradle" ), "" );
			final Path currentMetadata = temporaryDirectory.resolve( "build/orm/reports/classifications.json" );
			Files.createDirectories( currentMetadata.getParent() );
			Files.writeString( currentMetadata, "{}", StandardCharsets.UTF_8 );
			Files.writeString(
					temporaryDirectory.resolve( "build.gradle" ),
					"plugins { id 'org.hibernate.orm.build.reports' }\n"
							+ "migrationCompatibility {\n"
							+ "    baselineFamily = '8.0'\n"
							+ "    bootstrapBaseline = true\n"
							+ "    classificationMetadataBaseUrl = 'http://127.0.0.1:"
							+ server.getAddress().getPort()
							+ "'\n"
							+ "}\n"
							+ "tasks.named('resolveMigrationCompatibilityBaselineMetadata') {\n"
							+ "    sharedCacheDirectory = project.layout.projectDirectory.dir('classification-cache')\n"
							+ "}\n",
					StandardCharsets.UTF_8
			);

			final BuildResult result = GradleRunner.create()
					.withProjectDir( temporaryDirectory.toFile() )
					.withPluginClasspath()
					.withArguments(
							"validateMigrationCompatibility",
							"-x",
							"generateClassificationMetadata",
							"--stacktrace"
					)
					.build();

			assertEquals( SUCCESS, result.task( ":validateMigrationCompatibility" ).getOutcome() );
			assertTrue(
					Files.readString(
							temporaryDirectory.resolve(
									"build/orm/reports/migration-compatibility-validation.txt"
							)
					).contains( "migration compatibility: NOT_APPLICABLE" )
			);
		}
		finally {
			server.stop( 0 );
		}
	}

	@Test
	public void resolvesExactManifestArtifactsFromConfiguredRepository() throws Exception {
		final Path repository = temporaryDirectory.resolve( "repository" );
		final Path module = repository.resolve( "org/example/fixture/1.0" );
		Files.createDirectories( module );
		final Path jar = module.resolve( "fixture-1.0.jar" );
		try ( JarOutputStream ignored = new JarOutputStream( Files.newOutputStream( jar ) ) ) {
		}
		Files.writeString(
				module.resolve( "fixture-1.0.pom" ),
				"<project><modelVersion>4.0.0</modelVersion><groupId>org.example</groupId>"
						+ "<artifactId>fixture</artifactId><version>1.0</version></project>",
				StandardCharsets.UTF_8
		);
		final ClassificationMetadata.Artifact artifact = new ClassificationMetadata.Artifact(
				"fixture-1.0.jar",
				"org.example",
				"fixture",
				"1.0",
				false
		);
		final Path metadata = temporaryDirectory.resolve( "artifact-metadata.json" );
		Files.writeString(
				metadata,
				new ClassificationMetadataJson().write(
						new ClassificationMetadata(
								"8.0",
								"8.0.7.Final",
								ClassificationModel.builder().build(),
								Collections.singleton( artifact )
						)
				)
		);

		final Project project = ProjectBuilder.builder().withProjectDir( temporaryDirectory.toFile() ).build();
		project.getRepositories().maven( repositoryHandler -> repositoryHandler.setUrl( repository.toUri() ) );
		final ResolveClassificationArtifactsTask task = project.getTasks().create(
				"resolveFixtureArtifacts",
				ResolveClassificationArtifactsTask.class
		);
		task.getClassificationMetadataFile().set( metadata.toFile() );
		task.getArtifactsDirectory().set( temporaryDirectory.resolve( "resolved-artifacts" ).toFile() );
		task.resolve();
		assertTrue( temporaryDirectory.resolve( "resolved-artifacts/fixture-1.0.jar" ).toFile().isFile() );
	}

	private ResolveClassificationMetadataTask metadataTask(String baseUrl, boolean allowMissing) {
		final Project project = ProjectBuilder.builder().withProjectDir( temporaryDirectory.toFile() ).build();
		final ResolveClassificationMetadataTask task = project.getTasks().create(
				"resolveFixtureMetadata" + allowMissing,
				ResolveClassificationMetadataTask.class
		);
		task.getFamily().set( "8.0" );
		task.getClassificationMetadataBaseUrl().set( baseUrl );
		task.getOffline().set( false );
		task.getRefreshDependencies().set( false );
		task.getAllowMissing().set( allowMissing );
		task.getSharedCacheDirectory().set( temporaryDirectory.resolve( "cache-" + allowMissing ).toFile() );
		task.getResolvedMetadataFile().set( temporaryDirectory.resolve( "resolved-" + allowMissing + ".json.gz" ).toFile() );
		return task;
	}

	private static HttpServer server(byte[] metadata, boolean validChecksum) throws Exception {
		final HttpServer server = HttpServer.create( new InetSocketAddress( "127.0.0.1", 0 ), 0 );
		server.createContext( "/8.0/metadata/classifications.json.gz", exchange -> {
			exchange.sendResponseHeaders( 200, metadata.length );
			exchange.getResponseBody().write( metadata );
			exchange.close();
		} );
		final String digest = validChecksum
				? HexFormat.of().formatHex( MessageDigest.getInstance( "SHA-256" ).digest( metadata ) )
				: "0".repeat( 64 );
		final byte[] checksum = (digest + "  classifications.json.gz\n").getBytes( StandardCharsets.UTF_8 );
		server.createContext( "/8.0/metadata/classifications.json.gz.sha256", exchange -> {
			exchange.sendResponseHeaders( 200, checksum.length );
			exchange.getResponseBody().write( checksum );
			exchange.close();
		} );
		server.start();
		return server;
	}

	private static byte[] compressedMetadata(
			String family,
			String sourceVersion,
			java.util.List<ClassificationMetadata.Artifact> artifacts) throws IOException {
		final ClassificationMetadataJson json = new ClassificationMetadataJson();
		return json.gzip(
				json.write(
						new ClassificationMetadata(
								family,
								sourceVersion,
								ClassificationModel.builder().build(),
								artifacts
						)
				)
		);
	}
}
