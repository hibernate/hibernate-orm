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
import static org.hibernate.orm.post.ClassificationModel.ReferenceKind.ANNOTATION_CLASS_SELECTION;
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
		assertEquals( "org.hibernate.orm:hibernate-core:" + SOURCE_VERSION,
				reconstructed.getArtifacts().get( 0 ).getCoordinates() );
		assertEquals( 1, reconstructed.getModel().getElement( "type:fixture.Provider" ).getReferences().size() );
		assertEquals( serialized, json.write( reconstructed ) );

		final byte[] compressed = json.gzip( serialized );
		assertArrayEquals( compressed, json.gzip( serialized ) );
		final Path rawFile = temporaryDirectory.resolve( "classifications.json" );
		final Path compressedFile = temporaryDirectory.resolve( "classifications.json.gz" );
		Files.writeString( rawFile, serialized, StandardCharsets.UTF_8 );
		Files.write( compressedFile, compressed );
		assertEquals( reconstructed.getModel().snapshot(), json.read( rawFile ).getModel().snapshot() );
		assertEquals( reconstructed.getModel().snapshot(), json.read( compressedFile ).getModel().snapshot() );
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
		final List<Map<String, Object>> artifacts = (List<Map<String, Object>>) root.get( "artifacts" );
		assertEquals( 1, artifacts.size() );
		assertEquals( "hibernate-core", artifacts.get( 0 ).get( "module" ) );
		assertEquals( Boolean.TRUE, artifacts.get( 0 ).get( "hibernateOrmModule" ) );

		final List<Map<String, Object>> elements = (List<Map<String, Object>>) root.get( "elements" );
		assertEquals( 3, elements.size() );
		assertTrue( elements.stream().noneMatch( (element) -> element.containsKey( "classificationStatus" ) ) );
		assertTrue( elements.stream().noneMatch( (element) -> element.containsKey( "declaringPackage" ) ) );
		assertTrue( elements.stream().noneMatch( (element) -> element.containsKey( "signature" ) ) );
		assertTrue( elements.stream().noneMatch( (element) -> element.containsKey( "declaredSpiRoles" ) ) );
		assertEquals( List.of( "API", "INTERNAL", "SPI" ), elements.stream().map( (element) -> element.get( "category" ).toString() ).sorted().toList() );
		final Map<String, Object> spi = elements.stream()
				.filter( (element) -> "SPI".equals( element.get( "category" ) ) )
				.findFirst()
				.orElseThrow();
		assertEquals( List.of( "IMPLEMENT", "SUPPLY" ), spi.get( "spiRoles" ) );
		assertEquals( "hibernate-core", spi.get( "artifact" ) );
		final List<Map<String, Object>> references = (List<Map<String, Object>>) spi.get( "references" );
		assertEquals( 1, references.size() );
		assertEquals( "type:fixture.Provider", references.get( 0 ).get( "target" ) );
		assertFalse( references.get( 0 ).containsKey( "targetScope" ) );
		assertNotNull( spi.get( "structure" ) );
		assertEquals( List.of( "INCUBATING" ), spi.get( "lifecycle" ) );
		assertEquals( 2, elements.stream().filter( (element) -> !element.containsKey( "lifecycle" ) ).count() );
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
	public void artifactManifestDefinesMigrationCompatibilityScope() {
		final ClassificationModel.Builder builder = ClassificationModel.builder();
		builder.declaration(
				"type:fixture.Included",
				TYPE,
				null,
				ClassificationModel.Structure.UNKNOWN,
				"included.jar"
		);
		builder.declaration(
				"type:fixture.Excluded",
				TYPE,
				null,
				ClassificationModel.Structure.UNKNOWN,
				"excluded.jar"
		);
		builder.declaration(
				"type:fixture.Shared",
				TYPE,
				null,
				ClassificationModel.Structure.UNKNOWN,
				"excluded.jar,included.jar"
		);
		final ClassificationMetadata metadata = new ClassificationMetadata(
				HIBERNATE_VERSION,
				SOURCE_VERSION,
				builder.build(),
				List.of( new ClassificationMetadata.Artifact( "included.jar", "org.hibernate.orm", "included", SOURCE_VERSION, true ) )
		);

		assertTrue( metadata.isMigrationCompatibilityElement( metadata.getModel().getElement( "type:fixture.Included" ) ) );
		assertFalse( metadata.isMigrationCompatibilityElement( metadata.getModel().getElement( "type:fixture.Excluded" ) ) );
		assertTrue( metadata.isMigrationCompatibilityElement( metadata.getModel().getElement( "type:fixture.Shared" ) ) );
	}

	@Test
	@SuppressWarnings("unchecked")
	public void unresolvedRecordsRoundTripWithoutInventingCategories() throws Exception {
		final ClassificationModel.Builder builder = ClassificationModel.builder();
		builder.declaration(
				"type:fixture.Conflict",
				TYPE,
				"package:fixture",
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
		assertFalse( conflict.containsKey( "classificationStatus" ) );
		assertNull( conflict.get( "category" ) );
		assertEquals( 2, ((List<?>) conflict.get( "classificationOrigins" )).size() );
		assertEquals(
				ClassificationModel.ClassificationStatus.CONFLICTING,
				reconstructed.getModel().getElement( "type:fixture.Conflict" ).getClassificationStatus()
		);

		final Map<String, Object> unclassified = elements.stream()
				.filter( (element) -> "type:fixture.Unclassified".equals( element.get( "id" ) ) )
				.findFirst()
				.orElseThrow();
		assertFalse( unclassified.containsKey( "classificationStatus" ) );
		assertNull( unclassified.get( "category" ) );
		assertTrue( ((List<?>) unclassified.get( "classificationOrigins" )).isEmpty() );
		assertEquals(
				ClassificationModel.ClassificationStatus.UNCLASSIFIED,
				reconstructed.getModel().getElement( "type:fixture.Unclassified" ).getClassificationStatus()
		);
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
		builder.addReference(
				"type:fixture.Provider",
				new ClassificationModel.Reference( ANNOTATION_CLASS_SELECTION, "type:fixture.Provider", ClassificationModel.ReferenceTarget.HIBERNATE )
		);

		builder.declaration(
				"type:fixture.internal.Helper",
				TYPE,
				"package:fixture.internal",
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

		return new ClassificationMetadata(
				HIBERNATE_VERSION,
				SOURCE_VERSION,
				builder.build(),
				List.of(
						new ClassificationMetadata.Artifact(
								"hibernate-core-" + SOURCE_VERSION + ".jar",
								"org.hibernate.orm",
								"hibernate-core",
								SOURCE_VERSION,
								true
						)
				)
		);
	}
}
