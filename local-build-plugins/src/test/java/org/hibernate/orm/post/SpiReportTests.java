/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.post;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.hibernate.orm.post.ClassificationModel.Category.API;
import static org.hibernate.orm.post.ClassificationModel.Category.INTERNAL;
import static org.hibernate.orm.post.ClassificationModel.Category.SPI;
import static org.hibernate.orm.post.ClassificationModel.ElementKind.METHOD;
import static org.hibernate.orm.post.ClassificationModel.ElementKind.TYPE;
import static org.hibernate.orm.post.ClassificationModel.OriginKind.DIRECT;
import static org.hibernate.orm.post.ClassificationModel.OriginKind.ENCLOSING_TYPE;
import static org.hibernate.orm.post.ClassificationModel.OriginKind.ORDINARY_API;
import static org.hibernate.orm.post.ClassificationModel.Role.IMPLEMENT;
import static org.hibernate.orm.post.ClassificationModel.Role.SUPPLY;
import static org.hibernate.orm.post.ClassificationModel.Role.USE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Tests the concise SPI projection of canonical classification metadata.
///
/// @author Steve Ebersole
public class SpiReportTests {
	private static final String HIBERNATE_VERSION = "9.0";
	private static final String SOURCE_VERSION = "9.0.0-test";

	private final SpiReportRenderer renderer = new SpiReportRenderer();

	@Test
	public void reportMatchesGoldenFileAndOmitsClassifierInternals() throws IOException {
		final String report = renderer.render( reportMetadata() );
		assertEquals( golden( "spi-report.adoc" ), report );
		assertFalse( report.contains( "classificationOrigins" ) );
		assertFalse( report.contains( "reachability" ) );
		assertFalse( report.contains( "applicationApiStatus" ) );
		assertFalse( report.contains( "category" ) );
		assertFalse( report.contains( "Unresolved" ) );
		assertFalse( report.contains( "InheritedUse" ) );
		assertFalse( report.contains( "ApiType" ) );
		assertFalse( report.contains( "Conflicting" ) );
	}

	@Test
	public void renderingIsByteForByteDeterministic() {
		final ClassificationMetadata metadata = reportMetadata();
		assertEquals( renderer.render( metadata ), renderer.render( metadata ) );
	}

	@Test
	public void reportTaskConsumesClassificationMetadata(@TempDir Path projectDirectory) throws Exception {
		final Project project = ProjectBuilder.builder().withProjectDir( projectDirectory.toFile() ).build();
		project.getLayout().getBuildDirectory().set( project.file( "target" ) );
		new ReportGenerationPlugin().apply( project );

		final Task metadataTask = project.getTasks().getByName( "generateClassificationMetadata" );
		final ClassificationReportsTask reports = (ClassificationReportsTask) project.getTasks()
				.getByName( "generateClassificationReports" );
		final Task spiReportAlias = project.getTasks().getByName( "generateSpiReport" );
		assertSame( ClassificationReportsTask.class, reports.getClass().getSuperclass() );
		assertTrue( reports.getTaskDependencies().getDependencies( reports ).contains( metadataTask ) );
		assertTrue( spiReportAlias.getTaskDependencies().getDependencies( spiReportAlias ).contains( reports ) );
		assertTrue(
				reports.getClassificationMetadataFileReference().get().getAsFile().toPath().endsWith(
						"target/orm/reports/classifications.json"
				)
		);
		assertTrue(
				reports.getSpiReportFileReference().get().getAsFile().toPath().endsWith(
						"target/orm/reports/spi.txt"
				)
		);

		final Path metadataFile = reports.getClassificationMetadataFileReference().get().getAsFile().toPath();
		Files.createDirectories( metadataFile.getParent() );
		Files.writeString(
				metadataFile,
				new ClassificationMetadataJson().write( reportMetadata() ),
				StandardCharsets.UTF_8
		);
		reports.generateReports();
		assertEquals(
				golden( "spi-report.adoc" ),
				Files.readString( reports.getSpiReportFileReference().get().getAsFile().toPath() )
		);

		final Task aggregate = project.getTasks().getByName( "generateReports" );
		assertTrue( aggregate.getTaskDependencies().getDependencies( aggregate ).contains( reports ) );
	}

	private static ClassificationMetadata reportMetadata() {
		final ClassificationModel.Builder builder = ClassificationModel.builder();
		spiType( builder, "fixture.Use", roles( USE ) );
		spiType( builder, "fixture.Implement", roles( IMPLEMENT ) );
		spiType( builder, "fixture.Supply", roles( SUPPLY ) );
		spiType( builder, "fixture.UseImplement", roles( USE, IMPLEMENT ) );
		spiType( builder, "fixture.UseSupply", roles( USE, SUPPLY ) );
		spiType( builder, "fixture.ImplementSupply", roles( IMPLEMENT, SUPPLY ) );
		spiType( builder, "fixture.AllRoles", roles( USE, IMPLEMENT, SUPPLY ) );

		spiMember( builder, "fixture.Use", "direct()", roles( USE ), roles( USE ), DIRECT );
		spiMember( builder, "fixture.Use", "InheritedUse()", roles( USE ), Collections.emptySet(), ENCLOSING_TYPE );
		spiMember( builder, "fixture.Use", "exception()", roles( USE, SUPPLY ), Collections.emptySet(), ENCLOSING_TYPE );

		declaration( builder, "type:fixture.ApiType", TYPE, "package:fixture", "fixture.ApiType" );
		builder.addClassificationOrigin(
				"type:fixture.ApiType",
				new ClassificationModel.ClassificationOrigin(
						API,
						ORDINARY_API,
						"type:fixture.ApiType",
						Collections.emptySet()
				),
				Collections.emptySet()
		);

		declaration( builder, "type:fixture.Conflicting", TYPE, "package:fixture", "fixture.Conflicting" );
		builder.addClassificationOrigin(
				"type:fixture.Conflicting",
				new ClassificationModel.ClassificationOrigin( SPI, DIRECT, "type:fixture.Conflicting", roles( USE ) ),
				roles( USE )
		);
		builder.addClassificationOrigin(
				"type:fixture.Conflicting",
				new ClassificationModel.ClassificationOrigin(
						INTERNAL,
						DIRECT,
						"type:fixture.Conflicting",
						Collections.emptySet()
				),
				Collections.emptySet()
		);

		return new ClassificationMetadata( HIBERNATE_VERSION, SOURCE_VERSION, builder.build() );
	}

	private static void spiType(
			ClassificationModel.Builder builder,
			String className,
			Set<ClassificationModel.Role> roles) {
		final String id = "type:" + className;
		declaration( builder, id, TYPE, "package:fixture", className );
		builder.addClassificationOrigin(
				id,
				new ClassificationModel.ClassificationOrigin( SPI, DIRECT, id, roles ),
				roles
		);
	}

	private static void spiMember(
			ClassificationModel.Builder builder,
			String className,
			String member,
			Set<ClassificationModel.Role> effectiveRoles,
			Set<ClassificationModel.Role> declaredRoles,
			ClassificationModel.OriginKind originKind) {
		final String id = "method:" + className + '#' + member;
		final String owner = "type:" + className;
		declaration( builder, id, METHOD, owner, member );
		builder.addClassificationOrigin(
				id,
				new ClassificationModel.ClassificationOrigin( SPI, originKind, owner, effectiveRoles ),
				declaredRoles
		);
	}

	private static void declaration(
			ClassificationModel.Builder builder,
			String id,
			ClassificationModel.ElementKind kind,
			String owner,
			String signature) {
		builder.declaration(
				id,
				kind,
				owner,
				"fixture",
				signature,
				ClassificationModel.Structure.UNKNOWN,
				"hibernate-core"
		);
	}

	private static Set<ClassificationModel.Role> roles(ClassificationModel.Role... roles) {
		return EnumSet.copyOf( java.util.Arrays.asList( roles ) );
	}

	private static String golden(String name) throws IOException {
		try ( InputStream stream = SpiReportTests.class.getResourceAsStream( "/org/hibernate/orm/post/" + name ) ) {
			assertNotNull( stream, "Missing golden report " + name );
			return new String( stream.readAllBytes(), StandardCharsets.UTF_8 );
		}
	}
}
