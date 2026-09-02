/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.post;

import java.io.File;
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
import static org.hibernate.orm.post.ClassificationModel.ReferenceKind.ANNOTATION_CLASS_SELECTION;
import static org.hibernate.orm.post.ClassificationModel.ReferenceTarget.HIBERNATE;
import static org.hibernate.orm.post.ClassificationModel.Role.USE;
import static org.hibernate.orm.post.ValidationCause.CONFLICTING_CLASSIFICATION;
import static org.hibernate.orm.post.ValidationCause.FORBIDDEN_CATEGORY_DEPENDENCY;
import static org.hibernate.orm.post.ValidationCause.INVALID_CATEGORY_REACHABILITY;
import static org.hibernate.orm.post.ValidationCause.UNCLASSIFIED_HIBERNATE_DECLARATION;
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
	public void conflictingClassificationRejectsConflictingCategoryEvidence() {
		final ClassificationModel.Builder builder = ClassificationModel.builder();
		declaration( builder, "type:fixture.Conflict", null, Modifier.PUBLIC );
		origin( builder, "type:fixture.Conflict", API, DIRECT, "type:fixture.Conflict" );
		origin( builder, "type:fixture.Conflict", SPI, DIRECT, "type:fixture.Conflict", USE );
		assertDiagnostic( validate( builder.build() ), CONFLICTING_CLASSIFICATION, "type:fixture.Conflict", "type:fixture.Conflict" );
	}

	@Test
	public void forbiddenCategoryDependencyRejectsEachForbiddenCategoryBoundary() {
		final ClassificationModel.Builder builder = ClassificationModel.builder();
		root( builder, "type:fixture.Api", API );
		root( builder, "type:fixture.Spi", SPI, USE );
		root( builder, "type:fixture.Internal", INTERNAL );
		reference( builder, "type:fixture.Api", "type:fixture.Spi" );
		reference( builder, "type:fixture.Api", "type:fixture.Internal" );
		reference( builder, "type:fixture.Spi", "type:fixture.Internal" );

		final ValidationResult result = validate( builder.build() );
		assertDiagnostic( result, FORBIDDEN_CATEGORY_DEPENDENCY, "type:fixture.Api", "type:fixture.Spi" );
		assertDiagnostic( result, FORBIDDEN_CATEGORY_DEPENDENCY, "type:fixture.Api", "type:fixture.Internal" );
		final ValidationDiagnostic spiBoundary = assertDiagnostic(
				result,
				FORBIDDEN_CATEGORY_DEPENDENCY,
				"type:fixture.Spi",
				"type:fixture.Internal"
		);
		assertEquals( METHOD_RETURN.name(), spiBoundary.getEdgeKind() );
		assertEquals( List.of( "type:fixture.Spi", "type:fixture.Internal" ), spiBoundary.getPath() );
	}

	@Test
	public void apiAnnotationMaySelectSpiButNotInternal() {
		final ClassificationModel.Builder builder = ClassificationModel.builder();
		root( builder, "type:fixture.ApiAnnotation", API );
		root( builder, "type:fixture.Spi", SPI, USE );
		root( builder, "type:fixture.Internal", INTERNAL );
		reference( builder, "type:fixture.ApiAnnotation", "type:fixture.Spi", ANNOTATION_CLASS_SELECTION );
		reference( builder, "type:fixture.ApiAnnotation", "type:fixture.Internal", ANNOTATION_CLASS_SELECTION );

		final ValidationResult result = validate( builder.build() );
		assertFalse(
				result.getDiagnostics().stream().anyMatch(
						diagnostic -> diagnostic.getCause() == FORBIDDEN_CATEGORY_DEPENDENCY
								&& diagnostic.getTargetElementId().equals( "type:fixture.Spi" )
				)
		);
		assertDiagnostic( result, FORBIDDEN_CATEGORY_DEPENDENCY, "type:fixture.ApiAnnotation", "type:fixture.Internal" );
	}

	@Test
	public void ordinaryApiGenericBoundToSpiRemainsForbidden() {
		final ClassificationModel.Builder builder = ClassificationModel.builder();
		root( builder, "type:fixture.Api", API );
		root( builder, "type:fixture.Spi", SPI, USE );
		reference( builder, "type:fixture.Api", "type:fixture.Spi", ClassificationModel.ReferenceKind.GENERIC_BOUND );
		assertDiagnostic( validate( builder.build() ), FORBIDDEN_CATEGORY_DEPENDENCY, "type:fixture.Api", "type:fixture.Spi" );
	}

	@Test
	public void invalidCategoryReachabilityRejectsCategoryReachabilityOnlyThroughInternal() {
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
				INVALID_CATEGORY_REACHABILITY,
				"type:fixture.InternalRoot",
				"type:fixture.OrphanSpi"
		);
		assertEquals( "OWNERSHIP", diagnostic.getEdgeKind() );
		assertEquals( List.of( "type:fixture.InternalRoot", "type:fixture.OrphanSpi" ), diagnostic.getPath() );

		declaration( builder, "type:fixture.OrphanApi", "type:fixture.InternalRoot", Modifier.PUBLIC );
		origin( builder, "type:fixture.OrphanApi", API, ENCLOSING_TYPE, "type:fixture.InternalRoot" );
		assertDiagnostic(
				validate( builder.build() ),
				INVALID_CATEGORY_REACHABILITY,
				"type:fixture.InternalRoot",
				"type:fixture.OrphanApi"
		);
	}

	@Test
	public void unclassifiedDeclarationRejectsExternallyAccessibleElement() {
		final ClassificationModel.Builder builder = ClassificationModel.builder();
		declaration( builder, "type:fixture.Unclassified", null, Modifier.PUBLIC );
		assertDiagnostic(
				validate( builder.build() ),
				UNCLASSIFIED_HIBERNATE_DECLARATION,
				"type:fixture.Unclassified",
				"type:fixture.Unclassified"
		);
	}

	@Test
	public void unclassifiedDeclarationRejectsNonPublicElementRetainedBySupportedSignature() {
		final ClassificationModel.Builder builder = ClassificationModel.builder();
		root( builder, "type:fixture.Api", API );
		declaration( builder, "type:fixture.Hidden", null, 0 );
		reference( builder, "type:fixture.Api", "type:fixture.Hidden" );
		assertDiagnostic(
				validate( builder.build() ),
				UNCLASSIFIED_HIBERNATE_DECLARATION,
				"type:fixture.Hidden",
				"type:fixture.Hidden"
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
	public void providerArtifactUsesUpstreamApiAndSpiWithoutBecomingSpi() {
		final ClassificationModel.Builder builder = ClassificationModel.builder();
		root( builder, "type:fixture.ProviderApi", API, "provider.jar" );
		root( builder, "type:fixture.PlatformApi", API, "platform.jar" );
		root( builder, "type:fixture.PlatformSpi", SPI, "platform.jar", USE );
		root( builder, "type:fixture.PlatformInternal", INTERNAL, "platform.jar" );
		reference( builder, "type:fixture.ProviderApi", "type:fixture.PlatformApi" );
		reference( builder, "type:fixture.ProviderApi", "type:fixture.PlatformSpi" );
		reference( builder, "type:fixture.ProviderApi", "type:fixture.PlatformInternal" );

		final ValidationResult result = validator.validate(
				builder.build(),
				ValidationAllowlist.empty(),
				ClassificationValidationScope.withProviderArtifacts( List.of( new File( "provider.jar" ) ) )
		);
		assertEquals(
				1,
				result.getDiagnostics().stream()
						.filter( diagnostic -> diagnostic.getCause() == FORBIDDEN_CATEGORY_DEPENDENCY )
						.count()
		);
		assertDiagnostic(
				result,
				FORBIDDEN_CATEGORY_DEPENDENCY,
				"type:fixture.ProviderApi",
				"type:fixture.PlatformInternal"
		);
	}

	@Test
	public void platformContractCannotExposeProviderArtifact() {
		final ClassificationModel.Builder builder = ClassificationModel.builder();
		root( builder, "type:fixture.PlatformApi", API, "platform.jar" );
		root( builder, "type:fixture.ProviderApi", API, "provider.jar" );
		reference( builder, "type:fixture.PlatformApi", "type:fixture.ProviderApi" );

		assertDiagnostic(
				validator.validate(
						builder.build(),
						ValidationAllowlist.empty(),
						ClassificationValidationScope.withProviderArtifacts( List.of( new File( "provider.jar" ) ) )
				),
				FORBIDDEN_CATEGORY_DEPENDENCY,
				"type:fixture.PlatformApi",
				"type:fixture.ProviderApi"
		);
	}

	@Test
	public void providerArtifactIdentityMustBeUnambiguous() {
		assertThrows(
				IllegalArgumentException.class,
				() -> ClassificationValidationScope.withProviderArtifacts(
						List.of( new File( "first/provider.jar" ), new File( "second/provider.jar" ) )
				)
		);
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
				entry( FORBIDDEN_CATEGORY_DEPENDENCY, "type:fixture.Api", "type:fixture.Spi", METHOD_RETURN.name() )
		);
		final ValidationResult result = validator.validate( builder.build(), allowlist );
		assertNotNull(
				assertDiagnostic( result, FORBIDDEN_CATEGORY_DEPENDENCY, "type:fixture.Api", "type:fixture.Spi" ).getAllowlistMatch()
		);
		assertTrue( result.hasFailures(), "The API -> INTERNAL violation must remain unallowlisted" );
	}

	@Test
	public void unusedAndMalformedAllowlistEntriesFail(@TempDir Path temporaryDirectory) throws IOException {
		final ValidationAllowlist unused = allowlist(
				temporaryDirectory,
				entry( CONFLICTING_CLASSIFICATION, "type:fixture.Removed", "type:fixture.Removed", "CLASSIFICATION" )
		);
		assertTrue( validator.validate( resolvedModel(), unused ).hasFailures() );

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
	public void versionTwoAllowlistRequiresKnownSemanticCause(@TempDir Path temporaryDirectory) throws IOException {
		final Path legacyField = temporaryDirectory.resolve( "legacy-field.json" );
		Files.writeString(
				legacyField,
				"{\"schema\":\"hibernate-orm-classification-validation-allowlist\",\"schemaVersion\":2,"
						+ "\"entries\":[{\"rule\":\"CLS002\"}]}",
				StandardCharsets.UTF_8
		);
		final IllegalArgumentException missingCause = assertThrows(
				IllegalArgumentException.class,
				() -> ValidationAllowlist.read( legacyField.toFile() )
		);
		assertTrue( missingCause.getMessage().contains( "Missing allowlist field cause" ) );

		final Path unknownCause = temporaryDirectory.resolve( "unknown-cause.json" );
		Files.writeString(
				unknownCause,
				"{\"schema\":\"hibernate-orm-classification-validation-allowlist\",\"schemaVersion\":2,"
						+ "\"entries\":[{\"cause\":\"UNKNOWN_CAUSE\"}]}",
				StandardCharsets.UTF_8
		);
		final IllegalArgumentException unknown = assertThrows(
				IllegalArgumentException.class,
				() -> ValidationAllowlist.read( unknownCause.toFile() )
		);
		assertTrue( unknown.getMessage().contains( "Unknown allowlist cause UNKNOWN_CAUSE" ) );
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
		assertTrue( report.contains( "Diagnostics: 1; FORBIDDEN_CATEGORY_DEPENDENCY=1" ) );
		assertTrue( report.contains( "[ERROR] FORBIDDEN_CATEGORY_DEPENDENCY" ) );
		assertFalse( report.matches( "(?s).*(CLS|SPI)\\d{3}.*" ) );
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
		assertTrue( report.contains( "Diagnostics: 1; CONFLICTING_CLASSIFICATION=1" ) );
		assertFalse( report.contains( "  Edge:" ) );
	}

	private ValidationResult validate(ClassificationModel model) {
		return validator.validate( model, ValidationAllowlist.empty() );
	}

	private static ClassificationModel resolvedModel() {
		final ClassificationModel.Builder builder = ClassificationModel.builder();
		root( builder, "type:fixture.Spi", SPI, USE );
		return builder.build();
	}

	private static ValidationDiagnostic assertDiagnostic(
			ValidationResult result,
			ValidationCause cause,
			String source,
			String target) {
		return result.getDiagnostics().stream()
				.filter(
						(diagnostic) -> diagnostic.getCause() == cause
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
		declaration( builder, id, ownerId, modifiers, "test" );
	}

	private static void declaration(
			ClassificationModel.Builder builder,
			String id,
			String ownerId,
			int modifiers,
			String artifact) {
		builder.declaration(
				id,
				TYPE,
				ownerId,
				new ClassificationModel.Structure( modifiers, false, false ),
				artifact
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

	private static void root(
			ClassificationModel.Builder builder,
			String id,
			ClassificationModel.Category category,
			String artifact,
			ClassificationModel.Role... roles) {
		declaration( builder, id, null, Modifier.PUBLIC, artifact );
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
		reference( builder, source, target, METHOD_RETURN );
	}

	private static void reference(
			ClassificationModel.Builder builder,
			String source,
			String target,
			ClassificationModel.ReferenceKind kind) {
		builder.addReference( source, new ClassificationModel.Reference( kind, target, HIBERNATE ) );
	}

	private static ValidationAllowlist allowlist(Path directory, String entries) throws IOException {
		final Path file = directory.resolve( "allowlist-" + Math.abs( entries.hashCode() ) + ".json" );
		Files.writeString(
				file,
				"{\n  \"schema\": \"hibernate-orm-classification-validation-allowlist\",\n"
						+ "  \"schemaVersion\": 2,\n  \"entries\": [" + entries + "]\n}\n",
				StandardCharsets.UTF_8
		);
		return ValidationAllowlist.read( file.toFile() );
	}

	private static String entry(ValidationCause cause, String element, String target, String edgeKind) {
		return "{\"cause\":\"" + cause + "\",\"element\":\"" + element
				+ "\",\"target\":\"" + target + "\",\"edgeKind\":\"" + edgeKind
				+ "\",\"owner\":\"HHH-00001\",\"reason\":\"migration\",\"removalRelease\":\"8.2\"}";
	}
}
