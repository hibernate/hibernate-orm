/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.post;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import static org.hibernate.orm.post.ClassificationMigrationValidator.FindingCause.CLASSIFICATION_REMOVED;
import static org.hibernate.orm.post.ClassificationMigrationValidator.FindingCause.SPI_ROLE_REMOVED;
import static org.hibernate.orm.post.ClassificationMigrationValidator.Severity.REVIEW;
import static org.hibernate.orm.post.ClassificationMigrationValidator.Surface.API;
import static org.hibernate.orm.post.ClassificationMigrationValidator.Surface.SPI;
import static org.hibernate.orm.post.ClassificationModel.Category.INTERNAL;
import static org.hibernate.orm.post.ClassificationModel.ElementKind.METHOD;
import static org.hibernate.orm.post.ClassificationModel.ElementKind.TYPE;
import static org.hibernate.orm.post.ClassificationModel.OriginKind.DIRECT;
import static org.hibernate.orm.post.ClassificationModel.Role.IMPLEMENT;
import static org.hibernate.orm.post.ClassificationModel.Role.USE;
import static org.hibernate.orm.post.JavaMigrationCompatibilityAnalyzer.Cause.ABSTRACT_METHOD_ADDED;
import static org.hibernate.orm.post.JavaMigrationCompatibilityAnalyzer.Cause.DEFAULT_METHOD_ADDED;
import static org.hibernate.orm.post.JavaMigrationCompatibilityAnalyzer.Cause.METHOD_REMOVED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Classification, role, lifecycle, and release-horizon migration tests.
///
/// @author Steve Ebersole
public class ClassificationMigrationValidatorTests {
	@TempDir
	Path temporaryDirectory;

	private final ClassificationMigrationValidator validator = new ClassificationMigrationValidator();

	@Test
	public void apiIsProtectedAcrossMinorFamiliesWhileSpiIsNot() throws IOException {
		final JavaMigrationCompatibilityAnalyzer.Analysis analysis = removedMethodAnalysis();
		final ClassificationMetadata apiBaseline = metadata( "8.0", "8.0.4", categoryModel( org.hibernate.orm.post.ClassificationModel.Category.API, Collections.emptyList(), true ) );
		final ClassificationMetadata apiCurrent = metadata( "8.1", "8.1.0", categoryModel( org.hibernate.orm.post.ClassificationModel.Category.API, Collections.emptyList(), false ) );
		final ClassificationMigrationValidator.Result apiResult = validator.validate( apiBaseline, apiCurrent, analysis );
		assertTrue( apiResult.isApiEnforced() );
		assertFalse( apiResult.isSpiEnforced() );
		assertDiagnostic( apiResult, API, METHOD_REMOVED );
		assertTrue(
				new ClassificationMigrationReportRenderer().render( apiResult )
						.contains( "[ERROR] API_COMPATIBILITY_REGRESSION" )
		);

		final ClassificationMetadata spiBaseline = metadata( "8.0", "8.0.4", categoryModel( org.hibernate.orm.post.ClassificationModel.Category.SPI, List.of( USE ), true ) );
		final ClassificationMetadata spiCurrent = metadata( "8.1", "8.1.0", categoryModel( org.hibernate.orm.post.ClassificationModel.Category.SPI, List.of( USE ), false ) );
		final ClassificationMigrationValidator.Result spiResult = validator.validate( spiBaseline, spiCurrent, analysis );
		assertTrue( spiResult.isApiEnforced() );
		assertFalse( spiResult.isSpiEnforced() );
		assertFalse( spiResult.hasFailures() );
	}

	@Test
	public void neitherSurfaceIsProtectedAcrossMajorFamilies() throws IOException {
		final ClassificationMigrationValidator.Result result = validator.validate(
				metadata( "8.1", "8.1.9", categoryModel( org.hibernate.orm.post.ClassificationModel.Category.API, Collections.emptyList(), true ) ),
				metadata( "9.0", "9.0.0", categoryModel( org.hibernate.orm.post.ClassificationModel.Category.API, Collections.emptyList(), false ) ),
				removedMethodAnalysis()
		);
		assertFalse( result.isApiEnforced() );
		assertFalse( result.isSpiEnforced() );
		assertFalse( result.hasFailures() );
	}

	@Test
	public void stableSpiIsProtectedWithinItsMinorFamily() throws IOException {
		final ClassificationMigrationValidator.Result result = validator.validate(
				metadata( "8.1", "8.1.0", categoryModel( org.hibernate.orm.post.ClassificationModel.Category.SPI, List.of( USE ), true ) ),
				metadata( "8.1", "8.1.1", categoryModel( org.hibernate.orm.post.ClassificationModel.Category.SPI, List.of( USE ), false ) ),
				removedMethodAnalysis()
		);
		assertTrue( result.isSpiEnforced() );
		assertDiagnostic( result, SPI, METHOD_REMOVED );
		assertEquals( Collections.singleton( USE ), result.getDiagnostics().get( 0 ).getRoles() );
		assertTrue( result.hasFailures() );
		assertTrue(
				new ClassificationMigrationReportRenderer().render( result )
						.contains( "[ERROR] SPI_COMPATIBILITY_REGRESSION" )
		);
	}

	@Test
	public void implementRoleObservesNewAbstractMethodButUseDoesNot() throws IOException {
		final JavaMigrationCompatibilityAnalyzer.Analysis analysis = addedAbstractMethodAnalysis();
		final ClassificationMigrationValidator.Result implementResult = validator.validate(
				metadata( "8.1", "8.1.0", typeOnlySpiModel( IMPLEMENT, false ) ),
				metadata( "8.1", "8.1.1", typeOnlySpiModel( IMPLEMENT, true ) ),
				analysis
		);
		assertDiagnostic( implementResult, SPI, ABSTRACT_METHOD_ADDED );
		assertEquals( Collections.singleton( IMPLEMENT ), implementResult.getDiagnostics().get( 0 ).getRoles() );

		final ClassificationMigrationValidator.Result useResult = validator.validate(
				metadata( "8.1", "8.1.0", typeOnlySpiModel( USE, false ) ),
				metadata( "8.1", "8.1.1", typeOnlySpiModel( USE, true ) ),
				analysis
		);
		assertFalse( useResult.hasFailures() );
	}

	@Test
	public void contextDependentBreaksRequireReviewWithoutFailingAlone() throws IOException {
		final Path baseline = temporaryDirectory.resolve( "default-baseline" );
		final Path current = temporaryDirectory.resolve( "default-current" );
		writeContract( baseline, false, false, false );
		writeContract( current, false, false, true );
		final ClassificationMigrationValidator.Result result = validator.validate(
				metadata( "8.1", "8.1.0", typeOnlySpiModel( IMPLEMENT, false ) ),
				metadata( "8.1", "8.1.1", typeOnlySpiModel( IMPLEMENT, false ) ),
				analyze( baseline, current )
		);
		assertDiagnostic( result, SPI, DEFAULT_METHOD_ADDED );
		assertEquals( REVIEW, result.getDiagnostics().get( 0 ).getSeverity() );
		assertFalse( result.hasFailures() );
		final String report = new ClassificationMigrationReportRenderer().render( result );
		assertTrue( report.contains( "SPI X.Y-family compatibility: ENFORCED" ) );
		assertTrue( report.contains( "Diagnostics: 1; ERROR=0; REVIEW=1" ) );
	}

	@Test
	public void unclassifiedPrivateMemberChangesDoNotInheritTheOwnerPromise() throws IOException {
		final Path baseline = temporaryDirectory.resolve( "private-baseline" );
		final Path current = temporaryDirectory.resolve( "private-current" );
		writeClassWithPrivateMethod( baseline, true );
		writeClassWithPrivateMethod( current, false );
		final ClassificationMigrationValidator.Result result = validator.validate(
				metadata( "8.1", "8.1.0", categoryModel( org.hibernate.orm.post.ClassificationModel.Category.API, Collections.emptyList(), false ) ),
				metadata( "8.1", "8.1.1", categoryModel( org.hibernate.orm.post.ClassificationModel.Category.API, Collections.emptyList(), false ) ),
				analyze( baseline, current )
		);
		assertFalse( result.hasFailures() );
		assertTrue( result.getDiagnostics().isEmpty() );
	}

	@Test
	public void classificationAndRoleRemovalRemainVisible() {
		final ClassificationModel baseline = typeOnlySpiModel( USE, false );
		final ClassificationModel.Builder internalBuilder = ClassificationModel.builder();
		declareType( internalBuilder );
		classify( internalBuilder, "type:fixture.Contract", INTERNAL, Collections.emptyList() );
		final ClassificationMigrationValidator.Result categoryResult = validator.validate(
				metadata( "8.1", "8.1.0", baseline ),
				metadata( "8.1", "8.1.1", internalBuilder.build() ),
				emptyAnalysis()
		);
		assertEquals( CLASSIFICATION_REMOVED, categoryResult.getDiagnostics().get( 0 ).getFindingCause() );

		final ClassificationMigrationValidator.Result roleResult = validator.validate(
				metadata( "8.1", "8.1.0", typeOnlySpiModel( USE, false ) ),
				metadata( "8.1", "8.1.1", typeOnlySpiModel( IMPLEMENT, false ) ),
				emptyAnalysis()
		);
		assertEquals( SPI_ROLE_REMOVED, roleResult.getDiagnostics().get( 0 ).getFindingCause() );
		assertEquals( Collections.singleton( USE ), roleResult.getDiagnostics().get( 0 ).getRoles() );
	}

	@Test
	public void incubatingAndUnresolvedSurfacesAreHandledExplicitly() throws IOException {
		final ClassificationModel.Builder incubatingBuilder = categoryBuilder( org.hibernate.orm.post.ClassificationModel.Category.API, Collections.emptyList(), true );
		incubatingBuilder.addLifecycleOrigin(
				"method:fixture.Contract#operation()",
				new ClassificationModel.LifecycleOrigin(
						ClassificationModel.LifecycleState.INCUBATING,
						ClassificationModel.LifecycleOriginKind.DIRECT,
						"method:fixture.Contract#operation()"
				)
		);
		final ClassificationMigrationValidator.Result incubating = validator.validate(
				metadata( "8.1", "8.1.0", incubatingBuilder.build() ),
				metadata( "8.1", "8.1.1", categoryModel( org.hibernate.orm.post.ClassificationModel.Category.API, Collections.emptyList(), false ) ),
				removedMethodAnalysis()
		);
		assertFalse( incubating.hasFailures() );

		final ClassificationModel.Builder unresolved = ClassificationModel.builder();
		declareType( unresolved );
		assertThrows(
				IllegalArgumentException.class,
				() -> validator.validate(
						metadata( "8.1", "8.1.0", unresolved.build() ),
						metadata( "8.1", "8.1.1", typeOnlySpiModel( USE, false ) ),
						emptyAnalysis()
				)
		);
	}

	@Test
	public void declarationsOutsideTheArtifactManifestAreNotCompatibilityChecked() {
		final ClassificationModel.Builder baseline = ClassificationModel.builder();
		baseline.declaration(
				"type:fixture.Excluded",
				TYPE,
				null,
				new ClassificationModel.Structure( Modifier.PUBLIC, false, false ),
				"excluded.jar"
		);
		classify( baseline, "type:fixture.Excluded", org.hibernate.orm.post.ClassificationModel.Category.API, Collections.emptyList() );
		final ClassificationModel.Builder current = ClassificationModel.builder();
		current.declaration(
				"type:fixture.Excluded",
				TYPE,
				null,
				new ClassificationModel.Structure( Modifier.PUBLIC, false, false ),
				"excluded.jar"
		);
		classify( current, "type:fixture.Excluded", INTERNAL, Collections.emptyList() );

		final ClassificationMigrationValidator.Result result = validator.validate(
				scopedMetadata( "8.1", "8.1.0", baseline.build() ),
				scopedMetadata( "8.1", "8.1.1", current.build() ),
				emptyAnalysis()
		);

		assertTrue( result.getDiagnostics().isEmpty() );
	}

	private JavaMigrationCompatibilityAnalyzer.Analysis removedMethodAnalysis() throws IOException {
		final Path baseline = temporaryDirectory.resolve( "removed-baseline" );
		final Path current = temporaryDirectory.resolve( "removed-current" );
		writeContract( baseline, true, false );
		writeContract( current, false, false );
		return analyze( baseline, current );
	}

	private JavaMigrationCompatibilityAnalyzer.Analysis addedAbstractMethodAnalysis() throws IOException {
		final Path baseline = temporaryDirectory.resolve( "added-baseline" );
		final Path current = temporaryDirectory.resolve( "added-current" );
		writeContract( baseline, false, false );
		writeContract( current, false, true );
		return analyze( baseline, current );
	}

	private JavaMigrationCompatibilityAnalyzer.Analysis emptyAnalysis() {
		return new JavaMigrationCompatibilityAnalyzer().analyze( Collections.emptyList(), Collections.emptyList() );
	}

	private JavaMigrationCompatibilityAnalyzer.Analysis analyze(Path baseline, Path current) {
		return new JavaMigrationCompatibilityAnalyzer().analyze( List.of( baseline.toFile() ), List.of( current.toFile() ) );
	}

	private static ClassificationMetadata metadata(String family, String source, ClassificationModel model) {
		return new ClassificationMetadata( family, source, model );
	}

	private static ClassificationMetadata scopedMetadata(String family, String source, ClassificationModel model) {
		return new ClassificationMetadata(
				family,
				source,
				model,
				List.of( new ClassificationMetadata.Artifact( "included.jar", "org.hibernate.orm", "included", source, true ) )
		);
	}

	private static ClassificationModel categoryModel(
			ClassificationModel.Category category,
			Collection<ClassificationModel.Role> roles,
			boolean includeMethod) {
		return categoryBuilder( category, roles, includeMethod ).build();
	}

	private static ClassificationModel.Builder categoryBuilder(
			ClassificationModel.Category category,
			Collection<ClassificationModel.Role> roles,
			boolean includeMethod) {
		final ClassificationModel.Builder builder = ClassificationModel.builder();
		declareType( builder );
		classify( builder, "type:fixture.Contract", category, roles );
		if ( includeMethod ) {
			builder.declaration(
					"method:fixture.Contract#operation()",
					METHOD,
					"type:fixture.Contract",
					new ClassificationModel.Structure( Modifier.PUBLIC | Modifier.ABSTRACT, true, false ),
					"fixture"
			);
			classify( builder, "method:fixture.Contract#operation()", category, roles );
		}
		return builder;
	}

	private static ClassificationModel typeOnlySpiModel(ClassificationModel.Role role, boolean includeAddedMethod) {
		final ClassificationModel.Builder builder = ClassificationModel.builder();
		declareType( builder );
		classify( builder, "type:fixture.Contract", org.hibernate.orm.post.ClassificationModel.Category.SPI, List.of( role ) );
		if ( includeAddedMethod ) {
			builder.declaration(
					"method:fixture.Contract#required()",
					METHOD,
					"type:fixture.Contract",
					new ClassificationModel.Structure( Modifier.PUBLIC | Modifier.ABSTRACT, true, false ),
					"fixture"
			);
			classify( builder, "method:fixture.Contract#required()", org.hibernate.orm.post.ClassificationModel.Category.SPI, List.of( role ) );
		}
		return builder.build();
	}

	private static void declareType(ClassificationModel.Builder builder) {
		builder.declaration(
				"type:fixture.Contract",
				TYPE,
				null,
				new ClassificationModel.Structure( Modifier.PUBLIC | Modifier.ABSTRACT, true, false ),
				"fixture"
		);
	}

	private static void classify(
			ClassificationModel.Builder builder,
			String elementId,
			ClassificationModel.Category category,
			Collection<ClassificationModel.Role> roles) {
		builder.addClassificationOrigin(
				elementId,
				new ClassificationModel.ClassificationOrigin( category, DIRECT, elementId, roles ),
				roles
		);
	}

	private static void writeContract(Path root, boolean operation, boolean required) throws IOException {
		writeContract( root, operation, required, false );
	}

	private static void writeContract(
			Path root,
			boolean operation,
			boolean required,
			boolean defaultMethod) throws IOException {
		final ClassWriter writer = new ClassWriter( 0 );
		writer.visit(
				Opcodes.V17,
				Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT | Opcodes.ACC_INTERFACE,
				"fixture/Contract",
				null,
				"java/lang/Object",
				null
		);
		if ( operation ) {
			writer.visitMethod( Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT, "operation", "()V", null, null ).visitEnd();
		}
		if ( required ) {
			writer.visitMethod( Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT, "required", "()V", null, null ).visitEnd();
		}
		if ( defaultMethod ) {
			writer.visitMethod( Opcodes.ACC_PUBLIC, "optional", "()V", null, null ).visitEnd();
		}
		writer.visitEnd();
		final Path classFile = root.resolve( "fixture/Contract.class" );
		Files.createDirectories( classFile.getParent() );
		Files.write( classFile, writer.toByteArray() );
	}

	private static void writeClassWithPrivateMethod(Path root, boolean includeMethod) throws IOException {
		final ClassWriter writer = new ClassWriter( 0 );
		writer.visit(
				Opcodes.V17,
				Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT | Opcodes.ACC_INTERFACE,
				"fixture/Contract",
				null,
				"java/lang/Object",
				null
		);
		if ( includeMethod ) {
			writer.visitMethod( Opcodes.ACC_PRIVATE, "implementationDetail", "()V", null, null ).visitEnd();
		}
		writer.visitEnd();
		final Path classFile = root.resolve( "fixture/Contract.class" );
		Files.createDirectories( classFile.getParent() );
		Files.write( classFile, writer.toByteArray() );
	}

	private static void assertDiagnostic(
			ClassificationMigrationValidator.Result result,
			ClassificationMigrationValidator.Surface surface,
			JavaMigrationCompatibilityAnalyzer.Cause cause) {
		assertTrue(
				result.getDiagnostics().stream().anyMatch(
						diagnostic -> diagnostic.getSurface() == surface && diagnostic.getJavaCause() == cause
				),
				() -> "Missing " + surface + " diagnostic for " + cause
		);
	}
}
