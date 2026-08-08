/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.post;

import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.hibernate.orm.post.ClassificationModel.Category.SPI;
import static org.hibernate.orm.post.ClassificationModel.ElementKind.CONSTRUCTOR;
import static org.hibernate.orm.post.ClassificationModel.ElementKind.FIELD;
import static org.hibernate.orm.post.ClassificationModel.ElementKind.METHOD;
import static org.hibernate.orm.post.ClassificationModel.ElementKind.TYPE;
import static org.hibernate.orm.post.ClassificationModel.OriginKind.DIRECT;
import static org.hibernate.orm.post.ClassificationModel.OriginKind.ENCLOSING_TYPE;
import static org.hibernate.orm.post.ClassificationModel.Role.IMPLEMENT;
import static org.hibernate.orm.post.ClassificationModel.Role.SUPPLY;
import static org.hibernate.orm.post.ClassificationModel.Role.USE;
import static org.hibernate.orm.post.ValidationRule.SPI001;
import static org.hibernate.orm.post.ValidationRule.SPI002;
import static org.hibernate.orm.post.ValidationRule.SPI003;
import static org.hibernate.orm.post.ValidationRule.SPI004;
import static org.hibernate.orm.post.ValidationRule.SPI005;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Isolated role, implementation-point, source-evidence, compatibility, and
/// supply-documentation rule tests.
///
/// @author Steve Ebersole
public class SpiValidatorTests {
	private final SpiValidator validator = new SpiValidator();

	@Test
	public void spi001RejectsEmptyAndInvalidRoleTargets() {
		final ClassificationModel.Builder builder = ClassificationModel.builder();
		spiElement( builder, "type:fixture.Empty", TYPE, null, Modifier.PUBLIC, false );
		spiElement( builder, "field:fixture.Contract#hook", FIELD, "type:fixture.Contract", Modifier.PUBLIC, false, IMPLEMENT );
		spiElement(
				builder,
				"constructor:fixture.Contract#<init>()",
				CONSTRUCTOR,
				"type:fixture.Contract",
				Modifier.PUBLIC,
				false,
				SUPPLY
		);
		final ValidationResult result = validate( builder.build(), ValidationEvidence.NONE );
		assertDiagnostic( result, SPI001, "type:fixture.Empty" );
		assertDiagnostic( result, SPI001, "field:fixture.Contract#hook" );
		assertDiagnostic( result, SPI001, "constructor:fixture.Contract#<init>()" );
	}

	@Test
	public void spi002RejectsInvalidImplementationPoints() {
		final ClassificationModel.Builder builder = ClassificationModel.builder();
		spiElement(
				builder,
				"type:fixture.FinalContract",
				TYPE,
				null,
				Modifier.PUBLIC | Modifier.FINAL,
				false,
				IMPLEMENT
		);
		spiElement(
				builder,
				"type:fixture.MethodContract",
				TYPE,
				null,
				Modifier.PUBLIC | Modifier.ABSTRACT,
				true,
				IMPLEMENT
		);
		spiElement(
				builder,
				"method:fixture.MethodContract#hook()",
				METHOD,
				"type:fixture.MethodContract",
				Modifier.PUBLIC | Modifier.STATIC,
				false,
				IMPLEMENT
		);
		spiElement(
				builder,
				"type:fixture.NoConstructorContract",
				TYPE,
				null,
				Modifier.PUBLIC,
				false,
				IMPLEMENT
		);
		spiElement(
				builder,
				"method:fixture.MethodContract#packageHook()",
				METHOD,
				"type:fixture.MethodContract",
				0,
				false,
				IMPLEMENT
		);

		final ValidationResult result = validate( builder.build(), ValidationEvidence.NONE );
		assertDiagnostic( result, SPI002, "type:fixture.FinalContract" );
		assertDiagnostic( result, SPI002, "method:fixture.MethodContract#hook()" );
		assertDiagnostic( result, SPI002, "method:fixture.MethodContract#packageHook()" );
		assertDiagnostic( result, SPI002, "type:fixture.NoConstructorContract" );
	}

	@Test
	public void spi003ConsumesOverridePointEvidenceAsWarning() {
		final ValidationEvidence evidence = evidence(
				SPI003,
				"method:fixture.Contract#hook()",
				"External override point lacks IMPLEMENT",
				IMPLEMENT
		);
		final ValidationResult result = validate( ClassificationModel.builder().build(), evidence );
		assertEquals( ValidationRule.Severity.WARNING, assertDiagnostic( result, SPI003, "method:fixture.Contract#hook()" ).getSeverity() );
		assertFalse( result.hasFailures() );
	}

	@Test
	public void spi004ConsumesCompatibilityRegressionEvidence() {
		final ValidationEvidence evidence = evidence(
				SPI004,
				"method:fixture.Contract#operation()",
				"IMPLEMENT method removed from compatibility baseline",
				IMPLEMENT
		);
		final ValidationResult result = validate( ClassificationModel.builder().build(), evidence );
		assertDiagnostic( result, SPI004, "method:fixture.Contract#operation()" );
		assertTrue( result.hasFailures() );
	}

	@Test
	public void spi005AppliesOnlyToSupplyContracts() {
		final ClassificationModel.Builder builder = ClassificationModel.builder();
		spiElement( builder, "type:fixture.Supply", TYPE, null, Modifier.PUBLIC | Modifier.FINAL, false, SUPPLY );
		spiElement( builder, "type:fixture.Use", TYPE, null, Modifier.PUBLIC, true, USE );
		final ValidationEvidence evidence = ValidationEvidence.builder()
				.add(
						SPI005,
						"type:fixture.Supply",
						"type:fixture.Supply",
						SPI,
						SPI,
						"JAVADOC",
						EnumSet.of( SUPPLY ),
						List.of( "type:fixture.Supply" ),
						"Missing ownership documentation"
				)
				.add(
						SPI005,
						"type:fixture.Use",
						"type:fixture.Use",
						SPI,
						SPI,
						"JAVADOC",
						EnumSet.of( SUPPLY ),
						List.of( "type:fixture.Use" ),
						"Should be ignored"
				)
				.build();
		final ValidationResult result = validate( builder.build(), evidence );
		assertDiagnostic( result, SPI005, "type:fixture.Supply" );
		assertEquals( 1, result.getDiagnostics().size() );
		assertFalse( result.hasFailures() );
	}

	@Test
	public void positiveProviderShapesPass() {
		final ClassificationModel.Builder builder = ClassificationModel.builder();
		spiElement( builder, "type:fixture.UseInterface", TYPE, null, Modifier.PUBLIC | Modifier.ABSTRACT, true, USE );
		spiElement( builder, "type:fixture.SupplyValue", TYPE, null, Modifier.PUBLIC | Modifier.FINAL, false, SUPPLY );
		spiElement( builder, "type:fixture.ImplementInterface", TYPE, null, Modifier.PUBLIC | Modifier.ABSTRACT, true, IMPLEMENT );
		builder.addLifecycleOrigin(
				"type:fixture.ImplementInterface",
				new ClassificationModel.LifecycleOrigin(
						ClassificationModel.LifecycleState.INCUBATING,
						ClassificationModel.LifecycleOriginKind.DIRECT,
						"type:fixture.ImplementInterface"
				)
		);
		inheritedImplementMember(
				builder,
				"method:fixture.ImplementInterface#execute()",
				"type:fixture.ImplementInterface",
				Modifier.PUBLIC | Modifier.ABSTRACT
		);
		spiElement( builder, "type:fixture.Base", TYPE, null, Modifier.PUBLIC, false, IMPLEMENT );
		spiElement(
				builder,
				"constructor:fixture.Base#<init>()",
				CONSTRUCTOR,
				"type:fixture.Base",
				Modifier.PROTECTED,
				false,
				IMPLEMENT
		);
		inheritedImplementMember(
				builder,
				"method:fixture.Base#finalVocabulary()",
				"type:fixture.Base",
				Modifier.PUBLIC | Modifier.FINAL
		);
		inheritedImplementMember(
				builder,
				"method:fixture.Base#overridePoint()",
				"type:fixture.Base",
				Modifier.PROTECTED
		);

		final ValidationResult result = validate( builder.build(), ValidationEvidence.NONE );
		assertTrue( result.getDiagnostics().isEmpty(), new ValidationReportRenderer().render( "test", result ) );
	}

	private ValidationResult validate(ClassificationModel model, ValidationEvidence evidence) {
		return validator.validate( model, evidence, ValidationAllowlist.empty() );
	}

	private static ValidationDiagnostic assertDiagnostic(
			ValidationResult result,
			ValidationRule rule,
			String element) {
		return result.getDiagnostics().stream()
				.filter(
						(diagnostic) -> diagnostic.getRule() == rule
								&& diagnostic.getSourceElementId().equals( element )
				)
				.findFirst()
				.orElseThrow( () -> new AssertionError( new ValidationReportRenderer().render( "test", result ) ) );
	}

	private static ValidationEvidence evidence(
			ValidationRule rule,
			String element,
			String message,
			ClassificationModel.Role role) {
		return ValidationEvidence.builder()
				.add(
						rule,
						element,
						element,
						SPI,
						SPI,
						"DECLARATION",
						EnumSet.of( role ),
						List.of( element ),
						message
				)
				.build();
	}

	private static void spiElement(
			ClassificationModel.Builder builder,
			String id,
			ClassificationModel.ElementKind kind,
			String owner,
			int modifiers,
			boolean interfaceType,
			ClassificationModel.Role... roles) {
		builder.declaration(
				id,
				kind,
				owner,
				"fixture",
				id,
				new ClassificationModel.Structure( modifiers, interfaceType, false ),
				"test"
		);
		final EnumSet<ClassificationModel.Role> roleSet = roles.length == 0
				? EnumSet.noneOf( ClassificationModel.Role.class )
				: EnumSet.copyOf( List.of( roles ) );
		builder.addClassificationOrigin(
				id,
				new ClassificationModel.ClassificationOrigin( SPI, DIRECT, id, roleSet ),
				roleSet
		);
	}

	private static void inheritedImplementMember(
			ClassificationModel.Builder builder,
			String id,
			String owner,
			int modifiers) {
		builder.declaration(
				id,
				METHOD,
				owner,
				"fixture",
				id,
				new ClassificationModel.Structure( modifiers, false, false ),
				"test"
		);
		builder.addClassificationOrigin(
				id,
				new ClassificationModel.ClassificationOrigin( SPI, ENCLOSING_TYPE, owner, EnumSet.of( IMPLEMENT ) ),
				Collections.emptySet()
		);
	}
}
