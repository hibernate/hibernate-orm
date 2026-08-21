/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.post;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.hibernate.orm.post.ClassificationModel.Category.API;
import static org.hibernate.orm.post.ClassificationModel.Category.INTERNAL;
import static org.hibernate.orm.post.ClassificationModel.Category.SPI;
import static org.hibernate.orm.post.ClassificationModel.ElementKind.TYPE;
import static org.hibernate.orm.post.ClassificationModel.OriginKind.DIRECT;
import static org.hibernate.orm.post.ClassificationModel.OriginKind.ENCLOSING_TYPE;
import static org.hibernate.orm.post.ClassificationModel.OriginKind.ORDINARY_API;
import static org.hibernate.orm.post.ClassificationModel.ReferenceKind.METHOD_RETURN;
import static org.hibernate.orm.post.ClassificationModel.ReferenceTarget.HIBERNATE;
import static org.hibernate.orm.post.ClassificationModel.Role.USE;
import static org.hibernate.orm.post.ValidationRule.CLS001;
import static org.hibernate.orm.post.ValidationRule.CLS002;
import static org.hibernate.orm.post.ValidationRule.CLS003;
import static org.hibernate.orm.post.ValidationRule.CLS004;
import static org.hibernate.orm.post.ValidationRule.CLS005;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Isolated classification-rule, graph-path, allowlist, and task tests.
///
/// @author Steve Ebersole
public class ClassificationValidatorTests {
	private final ClassificationValidator validator = new ClassificationValidator();

	@Test
	public void cls001RejectsConflictingCategoryEvidence() {
		final ClassificationModel.Builder builder = ClassificationModel.builder();
		declaration( builder, "type:fixture.Conflict", null, Modifier.PUBLIC );
		origin( builder, "type:fixture.Conflict", API, DIRECT, "type:fixture.Conflict" );
		origin( builder, "type:fixture.Conflict", SPI, DIRECT, "type:fixture.Conflict", USE );
		assertDiagnostic( validate( builder.build() ), CLS001, "type:fixture.Conflict", "type:fixture.Conflict" );
	}

	@Test
	public void cls002RejectsEachForbiddenCategoryBoundary() {
		final ClassificationModel.Builder builder = ClassificationModel.builder();
		root( builder, "type:fixture.Api", API );
		root( builder, "type:fixture.Spi", SPI, USE );
		root( builder, "type:fixture.Internal", INTERNAL );
		reference( builder, "type:fixture.Api", "type:fixture.Spi" );
		reference( builder, "type:fixture.Api", "type:fixture.Internal" );
		reference( builder, "type:fixture.Spi", "type:fixture.Internal" );

		final ValidationResult result = validate( builder.build() );
		assertDiagnostic( result, CLS002, "type:fixture.Api", "type:fixture.Spi" );
		assertDiagnostic( result, CLS002, "type:fixture.Api", "type:fixture.Internal" );
		final ValidationDiagnostic spiBoundary = assertDiagnostic(
				result,
				CLS002,
				"type:fixture.Spi",
				"type:fixture.Internal"
		);
		assertEquals( METHOD_RETURN.name(), spiBoundary.getEdgeKind() );
		assertEquals( List.of( "type:fixture.Spi", "type:fixture.Internal" ), spiBoundary.getPath() );
	}

	@Test
	public void cls003RejectsCategoryReachabilityOnlyThroughInternal() {
		final ClassificationModel.Builder builder = ClassificationModel.builder();
		root( builder, "type:fixture.InternalRoot", INTERNAL );
		declaration( builder, "type:fixture.OrphanSpi", "type:fixture.InternalRoot", Modifier.PUBLIC );
		origin(
				builder,
				"type:fixture.OrphanSpi",
				SPI,
				ENCLOSING_TYPE,
				"type:fixture.InternalRoot",
				USE
		);

		final ValidationDiagnostic diagnostic = assertDiagnostic(
				validate( builder.build() ),
				CLS003,
				"type:fixture.InternalRoot",
				"type:fixture.OrphanSpi"
		);
		assertEquals( "OWNERSHIP", diagnostic.getEdgeKind() );
		assertEquals( List.of( "type:fixture.InternalRoot", "type:fixture.OrphanSpi" ), diagnostic.getPath() );

		declaration( builder, "type:fixture.OrphanApi", "type:fixture.InternalRoot", Modifier.PUBLIC );
		origin( builder, "type:fixture.OrphanApi", API, ENCLOSING_TYPE, "type:fixture.InternalRoot" );
		assertDiagnostic(
				validate( builder.build() ),
				CLS003,
				"type:fixture.InternalRoot",
				"type:fixture.OrphanApi"
		);
	}

	@Test
	public void cls004RejectsExternallyAccessibleUnclassifiedElement() {
		final ClassificationModel.Builder builder = ClassificationModel.builder();
		declaration( builder, "type:fixture.Unclassified", null, Modifier.PUBLIC );
		assertDiagnostic(
				validate( builder.build() ),
				CLS004,
				"type:fixture.Unclassified",
				"type:fixture.Unclassified"
		);
	}

	@Test
	public void cls005ConsumesConsumerBoundaryEvidence() {
		final ClassificationModel model = resolvedModel();
		final ValidationEvidence evidence = ValidationEvidence.builder()
				.add(
						CLS005,
						"consumer:application.Example",
						"type:fixture.Spi",
						null,
						SPI,
						"SOURCE_REFERENCE",
						Collections.emptySet(),
						List.of( "consumer:application.Example", "type:fixture.Spi" ),
						"Application code references provider SPI"
				)
				.build();
		assertDiagnostic(
				validator.validate( model, evidence, ValidationAllowlist.empty() ),
				CLS005,
				"consumer:application.Example",
				"type:fixture.Spi"
		);
	}

	@Test
	public void categoryMatrixAllowsSupportedDependencies() {
		final ClassificationModel.Builder builder = ClassificationModel.builder();
		root( builder, "type:fixture.Api", API );
		root( builder, "type:fixture.ApiValue", API );
		root( builder, "type:fixture.Spi", SPI, USE );
		root( builder, "type:fixture.SpiValue", SPI, USE );
		root( builder, "type:fixture.Internal", INTERNAL );
		reference( builder, "type:fixture.Api", "type:fixture.ApiValue" );
		reference( builder, "type:fixture.Spi", "type:fixture.ApiValue" );
		reference( builder, "type:fixture.Spi", "type:fixture.SpiValue" );
		reference( builder, "type:fixture.Internal", "type:fixture.ApiValue" );
		reference( builder, "type:fixture.Internal", "type:fixture.SpiValue" );
		reference( builder, "type:fixture.Internal", "type:fixture.Internal" );
		assertFalse( validate( builder.build() ).hasFailures() );
	}

	@Test
	public void sameCategoryOwnershipMakesNonRootReachable() {
		final ClassificationModel.Builder builder = ClassificationModel.builder();
		root( builder, "type:fixture.SpiRoot", SPI, USE );
		declaration( builder, "type:fixture.SpiChild", "type:fixture.SpiRoot", Modifier.PUBLIC );
		origin( builder, "type:fixture.SpiChild", SPI, ENCLOSING_TYPE, "type:fixture.SpiRoot", USE );
		assertTrue( validate( builder.build() ).getDiagnostics().isEmpty() );
	}

	@Test
	public void exactAllowlistDoesNotHideAnotherBoundary(@TempDir Path temporaryDirectory) throws IOException {
		final ClassificationModel.Builder builder = ClassificationModel.builder();
		root( builder, "type:fixture.Api", API );
		root( builder, "type:fixture.Spi", SPI, USE );
		root( builder, "type:fixture.Internal", INTERNAL );
		reference( builder, "type:fixture.Api", "type:fixture.Spi" );
		reference( builder, "type:fixture.Api", "type:fixture.Internal" );
		final ValidationAllowlist allowlist = allowlist(
				temporaryDirectory,
				entry( CLS002, "type:fixture.Api", "type:fixture.Spi", METHOD_RETURN.name() )
		);
		final ValidationResult result = validator.validate( builder.build(), ValidationEvidence.NONE, allowlist );
		assertNotNull(
				assertDiagnostic( result, CLS002, "type:fixture.Api", "type:fixture.Spi" ).getAllowlistMatch()
		);
		assertTrue( result.hasFailures(), "The API -> INTERNAL violation must remain unallowlisted" );
	}

	@Test
	public void unusedAndMalformedAllowlistEntriesFail(@TempDir Path temporaryDirectory) throws IOException {
		final ValidationAllowlist unused = allowlist(
				temporaryDirectory,
				entry( CLS001, "type:fixture.Removed", "type:fixture.Removed", "CLASSIFICATION" )
		);
		assertTrue( validator.validate( resolvedModel(), ValidationEvidence.NONE, unused ).hasFailures() );

		final Path malformed = temporaryDirectory.resolve( "malformed.json" );
		Files.writeString(
				malformed,
				"{\"schema\":\"hibernate-orm-classification-validation-allowlist\",\"schemaVersion\":1,"
						+ "\"entries\":[{\"rule\":\"CLS001\",\"element\":\"type:fixture.Bad\"}]}",
				StandardCharsets.UTF_8
		);
		assertThrows( IllegalArgumentException.class, () -> ValidationAllowlist.read( malformed.toFile() ) );
	}

	@Test
	public void bothValidationTasksConsumeClassificationMetadata() {
		final Project project = ProjectBuilder.builder().build();
		new ReportGenerationPlugin().apply( project );
		final Task classifications = project.getTasks().getByName( "validateClassifications" );
		final Task spi = project.getTasks().getByName( "validateSpi" );
		assertEquals( ClassificationValidationTask.class, classifications.getClass().getSuperclass() );
		assertEquals( SpiValidationTask.class, spi.getClass().getSuperclass() );
		assertTrue( spi.getTaskDependencies().getDependencies( spi ).contains( classifications ) );
		assertSame(
				((ClassificationValidationTask) classifications).getMetadataManager(),
				((SpiValidationTask) spi).getMetadataManager()
		);
	}

	@Test
	public void renderedDiagnosticContainsBoundaryContext() {
		final ClassificationModel.Builder builder = ClassificationModel.builder();
		root( builder, "type:fixture.Api", API );
		root( builder, "type:fixture.Internal", INTERNAL );
		reference( builder, "type:fixture.Api", "type:fixture.Internal" );
		final String report = new ValidationReportRenderer().render( "test", validate( builder.build() ) );
		assertTrue( report.contains( "Diagnostics: 1; CLS002=1" ) );
		assertTrue( report.contains( "Source: type:fixture.Api [API]" ) );
		assertTrue( report.contains( "Target: type:fixture.Internal [INTERNAL]" ) );
		assertTrue( report.contains( "Edge: METHOD_RETURN" ) );
		assertTrue( report.contains( "Path: type:fixture.Api -> type:fixture.Internal" ) );
		assertTrue( report.contains( "Allowlist: none" ) );
		assertTrue( report.contains( "Remediation:" ) );
	}

	@Test
	public void renderedDeclarationDiagnosticOmitsSyntheticEdge() {
		final ClassificationModel.Builder builder = ClassificationModel.builder();
		declaration( builder, "type:fixture.Conflict", null, Modifier.PUBLIC );
		origin( builder, "type:fixture.Conflict", API, DIRECT, "type:fixture.Conflict" );
		origin( builder, "type:fixture.Conflict", SPI, DIRECT, "type:fixture.Conflict", USE );
		final String report = new ValidationReportRenderer().render( "test", validate( builder.build() ) );
		assertTrue( report.contains( "Diagnostics: 1; CLS001=1" ) );
		assertFalse( report.contains( "  Edge:" ) );
	}

	private ValidationResult validate(ClassificationModel model) {
		return validator.validate( model, ValidationEvidence.NONE, ValidationAllowlist.empty() );
	}

	private static ClassificationModel resolvedModel() {
		final ClassificationModel.Builder builder = ClassificationModel.builder();
		root( builder, "type:fixture.Spi", SPI, USE );
		return builder.build();
	}

	private static ValidationDiagnostic assertDiagnostic(
			ValidationResult result,
			ValidationRule rule,
			String source,
			String target) {
		return result.getDiagnostics().stream()
				.filter(
						(diagnostic) -> diagnostic.getRule() == rule
								&& diagnostic.getSourceElementId().equals( source )
								&& diagnostic.getTargetElementId().equals( target )
				)
				.findFirst()
				.orElseThrow( () -> new AssertionError( new ValidationReportRenderer().render( "test", result ) ) );
	}

	private static void declaration(
			ClassificationModel.Builder builder,
			String id,
			String ownerId,
			int modifiers) {
		builder.declaration(
				id,
				TYPE,
				ownerId,
				"fixture",
				id,
				new ClassificationModel.Structure( modifiers, false, false ),
				"test"
		);
	}

	private static void root(
			ClassificationModel.Builder builder,
			String id,
			ClassificationModel.Category category,
			ClassificationModel.Role... roles) {
		declaration( builder, id, null, Modifier.PUBLIC );
		origin( builder, id, category, category == API ? ORDINARY_API : DIRECT, id, roles );
	}

	private static void origin(
			ClassificationModel.Builder builder,
			String id,
			ClassificationModel.Category category,
			ClassificationModel.OriginKind kind,
			String source,
			ClassificationModel.Role... roles) {
		final EnumSet<ClassificationModel.Role> roleSet = roles.length == 0
				? EnumSet.noneOf( ClassificationModel.Role.class )
				: EnumSet.copyOf( List.of( roles ) );
		builder.addClassificationOrigin(
				id,
				new ClassificationModel.ClassificationOrigin( category, kind, source, roleSet ),
				kind == DIRECT ? roleSet : Collections.emptySet()
		);
	}

	private static void reference(ClassificationModel.Builder builder, String source, String target) {
		builder.addReference( source, new ClassificationModel.Reference( METHOD_RETURN, target, HIBERNATE ) );
	}

	private static ValidationAllowlist allowlist(Path directory, String entries) throws IOException {
		final Path file = directory.resolve( "allowlist-" + Math.abs( entries.hashCode() ) + ".json" );
		Files.writeString(
				file,
				"{\n  \"schema\": \"hibernate-orm-classification-validation-allowlist\",\n"
						+ "  \"schemaVersion\": 1,\n  \"entries\": [" + entries + "]\n}\n",
				StandardCharsets.UTF_8
		);
		return ValidationAllowlist.read( file.toFile() );
	}

	private static String entry(ValidationRule rule, String element, String target, String edgeKind) {
		return "{\"rule\":\"" + rule + "\",\"element\":\"" + element
				+ "\",\"target\":\"" + target + "\",\"edgeKind\":\"" + edgeKind
				+ "\",\"owner\":\"HHH-00001\",\"reason\":\"migration\",\"removalRelease\":\"8.2\"}";
	}
}
