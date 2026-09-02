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
import static org.hibernate.orm.post.ValidationCause.INVALID_SPI_IMPLEMENTATION_POINT;
import static org.hibernate.orm.post.ValidationCause.INVALID_SPI_ROLE_DECLARATION;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Isolated SPI role and implementation-point rule tests.
///
/// @author Steve Ebersole
public class SpiValidatorTests {
	private final SpiValidator validator = new SpiValidator();

	@Test
	public void invalidSpiRoleDeclarationRejectsEmptyAndInvalidRoleTargets() {
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
		final ValidationResult result = validate( builder.build() );
		assertDiagnostic( result, INVALID_SPI_ROLE_DECLARATION, "type:fixture.Empty" );
		assertDiagnostic( result, INVALID_SPI_ROLE_DECLARATION, "field:fixture.Contract#hook" );
		assertDiagnostic( result, INVALID_SPI_ROLE_DECLARATION, "constructor:fixture.Contract#<init>()" );
	}

	@Test
	public void invalidSpiImplementationPointRejectsInvalidShapes() {
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

		final ValidationResult result = validate( builder.build() );
		assertDiagnostic( result, INVALID_SPI_IMPLEMENTATION_POINT, "type:fixture.FinalContract" );
		assertDiagnostic( result, INVALID_SPI_IMPLEMENTATION_POINT, "method:fixture.MethodContract#hook()" );
		assertDiagnostic( result, INVALID_SPI_IMPLEMENTATION_POINT, "method:fixture.MethodContract#packageHook()" );
		assertDiagnostic( result, INVALID_SPI_IMPLEMENTATION_POINT, "type:fixture.NoConstructorContract" );
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

		final ValidationResult result = validate( builder.build() );
		assertTrue( result.getDiagnostics().isEmpty(), new ValidationReportRenderer().render( "test", result ) );
	}

	private ValidationResult validate(ClassificationModel model) {
		return validator.validate( model, ValidationAllowlist.empty() );
	}

	private static ValidationDiagnostic assertDiagnostic(
			ValidationResult result,
			ValidationCause cause,
			String element) {
		return result.getDiagnostics().stream()
				.filter(
						(diagnostic) -> diagnostic.getCause() == cause
								&& diagnostic.getSourceElementId().equals( element )
				)
				.findFirst()
				.orElseThrow( () -> new AssertionError( new ValidationReportRenderer().render( "test", result ) ) );
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
