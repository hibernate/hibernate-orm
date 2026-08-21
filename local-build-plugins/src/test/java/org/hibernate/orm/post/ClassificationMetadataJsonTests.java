/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.post;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.hibernate.orm.post.ClassificationModel.Category.API;
import static org.hibernate.orm.post.ClassificationModel.Category.INTERNAL;
import static org.hibernate.orm.post.ClassificationModel.Category.SPI;
import static org.hibernate.orm.post.ClassificationModel.ElementKind.PACKAGE;
import static org.hibernate.orm.post.ClassificationModel.ElementKind.TYPE;
import static org.hibernate.orm.post.ClassificationModel.LifecycleOriginKind.DIRECT;
import static org.hibernate.orm.post.ClassificationModel.LifecycleState.INCUBATING;
import static org.hibernate.orm.post.ClassificationModel.OriginKind.ORDINARY_API;
import static org.hibernate.orm.post.ClassificationModel.ReferenceKind.SUPERCLASS;
import static org.hibernate.orm.post.ClassificationModel.ReferenceTarget.EXTERNAL;
import static org.hibernate.orm.post.ClassificationModel.Role.IMPLEMENT;
import static org.hibernate.orm.post.ClassificationModel.Role.SUPPLY;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Tests the versioned classification metadata contract and task integration.
///
/// @author Steve Ebersole
public class ClassificationMetadataJsonTests {
	private static final String HIBERNATE_VERSION = "9.0";
	private static final String SOURCE_VERSION = "9.0.0-test";

	private final ClassificationMetadataJson json = new ClassificationMetadataJson();

	@Test
	public void metadataRoundTripsWithoutLosingCanonicalFacts(@TempDir Path temporaryDirectory) throws Exception {
		final ClassificationMetadata metadata = metadata();
		final String serialized = json.write( metadata );
		final ClassificationMetadata reconstructed = json.read( serialized );

		assertEquals( HIBERNATE_VERSION, reconstructed.getHibernateVersion() );
		assertEquals( SOURCE_VERSION, reconstructed.getSourceVersion() );
		assertEquals( metadata.getModel().snapshot(), reconstructed.getModel().snapshot() );
		assertEquals( serialized, json.write( reconstructed ) );

		final byte[] compressed = json.gzip( serialized );
		assertArrayEquals( compressed, json.gzip( serialized ) );
		final Path rawFile = temporaryDirectory.resolve( "classifications.json" );
		final Path compressedFile = temporaryDirectory.resolve( "classifications.json.gz" );
		Files.writeString( rawFile, serialized, StandardCharsets.UTF_8 );
		Files.write( compressedFile, compressed );
		assertEquals( metadata.getModel().snapshot(), json.read( rawFile ).getModel().snapshot() );
		assertEquals( metadata.getModel().snapshot(), json.read( compressedFile ).getModel().snapshot() );
	}

	@Test
	@SuppressWarnings("unchecked")
	public void versionOneContractContainsAllCategoriesAndDirectEdges() throws Exception {
		final String serialized = json.write( metadata() );
		final Map<String, Object> root;
		try ( Jsonb jsonb = JsonbBuilder.create() ) {
			root = jsonb.fromJson( serialized, Map.class );
		}

		assertEquals( ClassificationMetadata.SCHEMA, root.get( "schema" ) );
		assertEquals( ClassificationMetadata.SCHEMA_VERSION, ((Number) root.get( "schemaVersion" )).intValue() );
		assertEquals( HIBERNATE_VERSION, root.get( "hibernateVersion" ) );
		assertEquals( SOURCE_VERSION, root.get( "sourceVersion" ) );

		final List<Map<String, Object>> elements = (List<Map<String, Object>>) root.get( "elements" );
		assertEquals( 3, elements.size() );
		assertTrue( elements.stream().allMatch( (element) -> "RESOLVED".equals( element.get( "classificationStatus" ) ) ) );
		assertEquals( List.of( "API", "INTERNAL", "SPI" ), elements.stream().map( (element) -> element.get( "category" ).toString() ).sorted().toList() );
		final Map<String, Object> spi = elements.stream()
				.filter( (element) -> "SPI".equals( element.get( "category" ) ) )
				.findFirst()
				.orElseThrow();
		assertEquals( List.of( "IMPLEMENT", "SUPPLY" ), spi.get( "spiRoles" ) );
		assertEquals( "hibernate-core", spi.get( "artifact" ) );
		assertFalse( ((List<?>) spi.get( "references" )).isEmpty() );
		assertNotNull( spi.get( "structure" ) );
		assertNotNull( spi.get( "lifecycle" ) );
		assertFalse( serialized.contains( "reachabilityPaths" ) );
		assertFalse( serialized.contains( "applicationApiStatus" ) );
		assertFalse( serialized.contains( "SIGNATURE_DERIVED" ) );
	}

	@Test
	public void unsupportedSchemaIsDifferentFromAnEmptyDocument() {
		final ClassificationMetadata empty = new ClassificationMetadata(
				HIBERNATE_VERSION,
				SOURCE_VERSION,
				ClassificationModel.builder().build()
		);
		assertTrue( json.read( json.write( empty ) ).getModel().getElements().isEmpty() );

		assertThrows(
				ClassificationMetadataJson.UnsupportedSchemaException.class,
				() -> json.read(
						"{\"schema\":\"hibernate-orm-classifications\",\"schemaVersion\":2,"
								+ "\"hibernateVersion\":\"9.0\",\"sourceVersion\":\"9.0.0\",\"elements\":[]}"
				)
		);
		assertThrows( IllegalArgumentException.class, () -> json.read( "{}" ) );
	}

	@Test
	@SuppressWarnings("unchecked")
	public void unresolvedRecordsRoundTripWithoutInventingCategories() throws Exception {
		final ClassificationModel.Builder builder = ClassificationModel.builder();
		builder.declaration(
				"type:fixture.Conflict",
				TYPE,
				"package:fixture",
				"fixture",
				"fixture.Conflict",
				ClassificationModel.Structure.UNKNOWN,
				"hibernate-core"
		);
		builder.addClassificationOrigin(
				"type:fixture.Conflict",
				new ClassificationModel.ClassificationOrigin(
						SPI,
						ClassificationModel.OriginKind.DIRECT,
						"type:fixture.Conflict",
						EnumSet.of( ClassificationModel.Role.USE )
				),
				EnumSet.of( ClassificationModel.Role.USE )
		);
		builder.addClassificationOrigin(
				"type:fixture.Conflict",
				new ClassificationModel.ClassificationOrigin(
						INTERNAL,
						ClassificationModel.OriginKind.DIRECT,
						"type:fixture.Conflict",
						Collections.emptySet()
				),
				Collections.emptySet()
		);
		builder.declaration(
				"type:fixture.Unclassified",
				TYPE,
				"package:fixture",
				"fixture",
				"fixture.Unclassified",
				ClassificationModel.Structure.UNKNOWN,
				"hibernate-core"
		);

		final ClassificationMetadata metadata = new ClassificationMetadata(
				HIBERNATE_VERSION,
				SOURCE_VERSION,
				builder.build()
		);
		final String serialized = json.write( metadata );
		final ClassificationMetadata reconstructed = json.read( serialized );
		assertEquals( metadata.getModel().snapshot(), reconstructed.getModel().snapshot() );

		final Map<String, Object> root;
		try ( Jsonb jsonb = JsonbBuilder.create() ) {
			root = jsonb.fromJson( serialized, Map.class );
		}
		final List<Map<String, Object>> elements = (List<Map<String, Object>>) root.get( "elements" );
		final Map<String, Object> conflict = elements.stream()
				.filter( (element) -> "type:fixture.Conflict".equals( element.get( "id" ) ) )
				.findFirst()
				.orElseThrow();
		assertEquals( "CONFLICTING", conflict.get( "classificationStatus" ) );
		assertNull( conflict.get( "category" ) );
		assertEquals( 2, ((List<?>) conflict.get( "classificationOrigins" )).size() );

		final Map<String, Object> unclassified = elements.stream()
				.filter( (element) -> "type:fixture.Unclassified".equals( element.get( "id" ) ) )
				.findFirst()
				.orElseThrow();
		assertEquals( "UNCLASSIFIED", unclassified.get( "classificationStatus" ) );
		assertNull( unclassified.get( "category" ) );
		assertTrue( ((List<?>) unclassified.get( "classificationOrigins" )).isEmpty() );
	}

	@Test
	public void pluginRegistersMetadataTaskWithConfiguredBuildDirectory() {
		final Project project = ProjectBuilder.builder().build();
		project.getLayout().getBuildDirectory().set( project.file( "target" ) );
		new ReportGenerationPlugin().apply( project );

		final Task index = project.getTasks().getByName( "buildAggregatedIndex" );
		final ClassificationMetadataTask metadata = (ClassificationMetadataTask) project.getTasks()
				.getByName( "generateClassificationMetadata" );
		final Task aggregate = project.getTasks().getByName( "generateReports" );

		assertTrue( metadata.getTaskDependencies().getDependencies( metadata ).contains( index ) );
		assertTrue( aggregate.getTaskDependencies().getDependencies( aggregate ).contains( metadata ) );
		assertTrue(
				metadata.getReportFileReference().get().getAsFile().toPath().endsWith(
						"target/orm/reports/classifications.json"
				)
		);
		assertTrue(
				metadata.getCompressedMetadataFileReference().get().getAsFile().toPath().endsWith(
						"target/orm/reports/classifications.json.gz"
				)
		);
		assertTrue(
				((IndexerTask) index).getArtifactFileReferenceAccess().get().getAsFile().toPath().endsWith(
						"target/orm/reports/indexing/artifacts.txt"
				)
		);
	}

	private static ClassificationMetadata metadata() {
		final ClassificationModel.Builder builder = ClassificationModel.builder();
		builder.declaration(
				"package:fixture",
				PACKAGE,
				null,
				"fixture",
				"fixture",
				ClassificationModel.Structure.UNKNOWN,
				"hibernate-core"
		);
		builder.addClassificationOrigin(
				"package:fixture",
				new ClassificationModel.ClassificationOrigin(
						API,
						ORDINARY_API,
						"package:fixture",
						Collections.emptySet()
				),
				Collections.emptySet()
		);

		builder.declaration(
				"type:fixture.Provider",
				TYPE,
				"package:fixture",
				"fixture",
				"fixture.Provider",
				new ClassificationModel.Structure( java.lang.reflect.Modifier.PUBLIC, false, false ),
				"hibernate-core"
		);
		builder.addClassificationOrigin(
				"type:fixture.Provider",
				new ClassificationModel.ClassificationOrigin(
						SPI,
						ClassificationModel.OriginKind.DIRECT,
						"type:fixture.Provider",
						EnumSet.of( IMPLEMENT, SUPPLY )
				),
				EnumSet.of( IMPLEMENT, SUPPLY )
		);
		builder.addLifecycleOrigin(
				"type:fixture.Provider",
				new ClassificationModel.LifecycleOrigin( INCUBATING, DIRECT, "type:fixture.Provider" )
		);
		builder.addReference(
				"type:fixture.Provider",
				new ClassificationModel.Reference( SUPERCLASS, "type:java.lang.Object", EXTERNAL )
		);

		builder.declaration(
				"type:fixture.internal.Helper",
				TYPE,
				"package:fixture.internal",
				"fixture.internal",
				"fixture.internal.Helper",
				new ClassificationModel.Structure( java.lang.reflect.Modifier.PUBLIC, false, false ),
				"hibernate-core"
		);
		builder.addClassificationOrigin(
				"type:fixture.internal.Helper",
				new ClassificationModel.ClassificationOrigin(
						INTERNAL,
						ClassificationModel.OriginKind.INTERNAL_PACKAGE,
						"package:fixture.internal",
						Collections.emptySet()
				),
				Collections.emptySet()
		);

		return new ClassificationMetadata( HIBERNATE_VERSION, SOURCE_VERSION, builder.build() );
	}
}
