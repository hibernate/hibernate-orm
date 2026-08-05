/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.post;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;

import static org.hibernate.orm.post.SpiModel.ApiStatus.API;
import static org.hibernate.orm.post.SpiModel.ApiStatus.NON_API;
import static org.hibernate.orm.post.SpiModel.ApiStatus.UNKNOWN;
import static org.hibernate.orm.post.SpiModel.ElementKind.TYPE;
import static org.hibernate.orm.post.SpiModel.OriginKind.DIRECT;
import static org.hibernate.orm.post.SpiModel.OriginKind.ENCLOSING_TYPE;
import static org.hibernate.orm.post.SpiModel.OriginKind.EXACT_SPI_PACKAGE;
import static org.hibernate.orm.post.SpiModel.OriginKind.PACKAGE;
import static org.hibernate.orm.post.SpiModel.Role.IMPLEMENT;
import static org.hibernate.orm.post.SpiModel.Role.SUPPLY;
import static org.hibernate.orm.post.SpiModel.Role.USE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Tests the deterministic human-readable and machine-readable SPI reports.
///
/// @author Steve Ebersole
public class SpiReportTests {
	private final SpiReportRenderer renderer = new SpiReportRenderer();

	@Test
	public void reportsMatchGoldenFiles() throws IOException {
		final SpiModel model = reportModel();
		assertEquals( golden( "spi-report.adoc" ), renderer.renderAsciiDoc( model ) );
		assertEquals( golden( "spi-report.json" ), renderer.renderJson( model ) );
	}

	@Test
	@SuppressWarnings("unchecked")
	public void jsonConformsToVersionOneContract() throws Exception {
		final String report = renderer.renderJson( reportModel() );
		final Map<String, Object> root;
		try ( Jsonb jsonb = JsonbBuilder.create() ) {
			root = jsonb.fromJson( report, Map.class );
		}

		assertEquals( SpiReportRenderer.JSON_SCHEMA, root.get( "schema" ) );
		assertEquals( SpiReportRenderer.JSON_SCHEMA_VERSION, ((Number) root.get( "schemaVersion" )).intValue() );
		assertEquals( "SPI_PROVIDER", root.get( "audience" ) );

		final Map<String, List<Map<String, Object>>> independent =
				(Map<String, List<Map<String, Object>>>) root.get( "independent" );
		assertEquals( 7, independent.size() );
		assertTrue(
				independent.keySet().containsAll(
						Arrays.asList(
								"USE",
								"IMPLEMENT",
								"SUPPLY",
								"USE_IMPLEMENT",
								"USE_SUPPLY",
								"IMPLEMENT_SUPPLY",
								"USE_IMPLEMENT_SUPPLY"
						)
				)
		);
		for ( List<Map<String, Object>> bucket : independent.values() ) {
			assertEquals( 1, bucket.size() );
			assertEquals( "INDEPENDENT", bucket.get( 0 ).get( "classification" ) );
		}
		final List<Map<String, Object>> derived = (List<Map<String, Object>>) root.get( "signatureDerived" );
		assertEquals( 1, derived.size() );
		assertEquals( "SIGNATURE_DERIVED", derived.get( 0 ).get( "classification" ) );
	}

	@Test
	public void renderingIsByteForByteDeterministic() {
		final SpiModel model = reportModel();
		assertEquals( renderer.renderAsciiDoc( model ), renderer.renderAsciiDoc( model ) );
		assertEquals( renderer.renderJson( model ), renderer.renderJson( model ) );
	}

	@Test
	public void aggregateReportTaskIncludesSpiReport() {
		final Project project = ProjectBuilder.builder().build();
		new ReportGenerationPlugin().apply( project );

		final Task spiReport = project.getTasks().getByName( "generateSpiReport" );
		assertNotNull( spiReport );
		assertSame( SpiReportTask.class, spiReport.getClass().getSuperclass() );
		final Task aggregate = project.getTasks().getByName( "generateReports" );
		assertTrue( aggregate.getTaskDependencies().getDependencies( aggregate ).contains( spiReport ) );
	}

	private static SpiModel reportModel() {
		final SpiModel.Builder builder = SpiModel.builder();
		classify(
				builder,
				"type:fixture.Use",
				roles( USE ),
				DIRECT,
				"type:fixture.Use",
				API,
				new SpiModel.Lifecycle( false, true, false )
		);
		classify(
				builder,
				"type:fixture.Implement",
				roles( IMPLEMENT ),
				PACKAGE,
				"package:fixture",
				NON_API,
				new SpiModel.Lifecycle( false, false, false )
		);
		classify(
				builder,
				"type:fixture.Supply",
				roles( SUPPLY ),
				ENCLOSING_TYPE,
				"type:fixture.Container",
				UNKNOWN,
				new SpiModel.Lifecycle( false, false, true )
		);
		classify(
				builder,
				"type:fixture.UseImplement",
				roles( USE, IMPLEMENT ),
				EXACT_SPI_PACKAGE,
				"package:fixture.spi",
				NON_API,
				new SpiModel.Lifecycle( false, false, false )
		);
		classify(
				builder,
				"type:fixture.UseSupply",
				roles( USE, SUPPLY ),
				DIRECT,
				"type:fixture.UseSupply",
				NON_API,
				new SpiModel.Lifecycle( false, false, false )
		);
		classify(
				builder,
				"type:fixture.ImplementSupply",
				roles( IMPLEMENT, SUPPLY ),
				DIRECT,
				"type:fixture.ImplementSupply",
				NON_API,
				new SpiModel.Lifecycle( false, false, false )
		);
		classify(
				builder,
				"type:fixture.AllRoles",
				roles( USE, IMPLEMENT, SUPPLY ),
				DIRECT,
				"type:fixture.AllRoles",
				NON_API,
				new SpiModel.Lifecycle( false, false, false )
		);

		final String derivedId = "type:fixture.SignatureCollaborator";
		builder.derived(
				derivedId,
				TYPE,
				"fixture",
				"fixture.SignatureCollaborator",
				NON_API,
				new SpiModel.Lifecycle( false, false, false ),
				"hibernate-core",
				Collections.emptySet()
		);
		builder.addReachabilityPath(
				derivedId,
				new SpiModel.ReachabilityPath( Arrays.asList( "type:fixture.Use", derivedId ) )
		);
		builder.addReachabilityPath(
				"type:fixture.Use",
				new SpiModel.ReachabilityPath( Collections.singletonList( "type:fixture.Use" ) )
		);
		builder.addReachabilityPath(
				"type:fixture.Use",
				new SpiModel.ReachabilityPath(
						Arrays.asList( "type:fixture.UseSupply", "type:fixture.Use" )
				)
		);
		return builder.build();
	}

	private static void classify(
			SpiModel.Builder builder,
			String id,
			Set<SpiModel.Role> roles,
			SpiModel.OriginKind originKind,
			String originSource,
			SpiModel.ApiStatus apiStatus,
			SpiModel.Lifecycle lifecycle) {
		builder.classify(
				id,
				TYPE,
				"fixture",
				id.substring( "type:".length() ),
				originKind == DIRECT ? roles : Collections.emptySet(),
				new SpiModel.Origin( originKind, originSource, roles ),
				apiStatus,
				lifecycle,
				"hibernate-core",
				id.endsWith( "Supply" ) ? Collections.singleton( "SPI-MIGRATION-1" ) : Collections.emptySet()
		);
	}

	private static Set<SpiModel.Role> roles(SpiModel.Role... roles) {
		return EnumSet.copyOf( Arrays.asList( roles ) );
	}

	private static String golden(String name) throws IOException {
		try ( InputStream stream = SpiReportTests.class.getResourceAsStream( "/org/hibernate/orm/post/" + name ) ) {
			assertNotNull( stream, "Missing golden report " + name );
			return new String( stream.readAllBytes(), StandardCharsets.UTF_8 );
		}
	}
}
