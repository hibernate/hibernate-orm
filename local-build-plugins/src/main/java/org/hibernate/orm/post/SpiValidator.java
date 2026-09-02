/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.post;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import static org.hibernate.orm.post.ClassificationModel.Category.SPI;
import static org.hibernate.orm.post.ClassificationModel.ClassificationStatus.RESOLVED;
import static org.hibernate.orm.post.ClassificationModel.ElementKind.ANNOTATION_TYPE;
import static org.hibernate.orm.post.ClassificationModel.ElementKind.CONSTRUCTOR;
import static org.hibernate.orm.post.ClassificationModel.ElementKind.FIELD;
import static org.hibernate.orm.post.ClassificationModel.ElementKind.METHOD;
import static org.hibernate.orm.post.ClassificationModel.ElementKind.TYPE;
import static org.hibernate.orm.post.ClassificationModel.OriginKind.DIRECT;
import static org.hibernate.orm.post.ClassificationModel.Role.IMPLEMENT;
import static org.hibernate.orm.post.ClassificationModel.Role.SUPPLY;

/// Applies the approved SPI role, implementation-point, documentation, and
/// compatibility rules to the canonical classification model.
///
/// @author Steve Ebersole
public final class SpiValidator {
	public ValidationResult validate(
			ClassificationModel model,
			ValidationAllowlist allowlist) {
		final List<ValidationDiagnostic> diagnostics = new ArrayList<>();
		for ( ClassificationModel.Element element : model.getElements() ) {
			validateRoleTarget( element, diagnostics );
			validateImplementationPoint( model, element, diagnostics );
		}
		return ValidationResult.complete( diagnostics, allowlist, ValidationCause.Domain.SPI );
	}

	private static void validateRoleTarget(
			ClassificationModel.Element element,
			List<ValidationDiagnostic> diagnostics) {
		if ( hasExplicitEmptyRoleDeclaration( element ) ) {
			diagnostics.add(
					declarationDiagnostic(
							ValidationCause.INVALID_SPI_ROLE_DECLARATION,
							element,
							"The declaration has an explicitly empty SPI role array"
					)
			);
		}
		if ( element.getDeclaredRoles().contains( IMPLEMENT ) && element.getKind() == FIELD ) {
			diagnostics.add(
					declarationDiagnostic(
							ValidationCause.INVALID_SPI_ROLE_DECLARATION,
							element,
							"IMPLEMENT is not valid on a field"
					)
			);
		}
		if ( element.getDeclaredRoles().contains( SUPPLY ) && element.getKind() == CONSTRUCTOR ) {
			diagnostics.add(
					declarationDiagnostic(
							ValidationCause.INVALID_SPI_ROLE_DECLARATION,
							element,
							"SUPPLY is not valid on a constructor"
					)
			);
		}
	}

	private static boolean hasExplicitEmptyRoleDeclaration(ClassificationModel.Element element) {
		if ( !element.getDeclaredRoles().isEmpty() ) {
			return false;
		}
		for ( ClassificationModel.ClassificationOrigin origin : element.getClassificationOrigins() ) {
			if ( origin.getCategory() == SPI
					&& origin.getKind() == DIRECT
					&& origin.getSourceElementId().equals( element.getId() )
					&& origin.getRoles().isEmpty() ) {
				return true;
			}
		}
		return false;
	}

	private static void validateImplementationPoint(
			ClassificationModel model,
			ClassificationModel.Element element,
			List<ValidationDiagnostic> diagnostics) {
		if ( element.getClassificationStatus() != RESOLVED
				|| element.getCategory() != SPI
				|| !element.getEffectiveRoles().contains( IMPLEMENT ) ) {
			return;
		}
		if ( (element.getKind() == TYPE || element.getKind() == ANNOTATION_TYPE)
				&& !element.getStructure().isExternallyAccessible() ) {
			diagnostics.add(
					declarationDiagnostic(
							ValidationCause.INVALID_SPI_IMPLEMENTATION_POINT,
							element,
							"An IMPLEMENT type is not externally accessible"
					)
			);
		}
		if ( (element.getKind() == TYPE || element.getKind() == ANNOTATION_TYPE)
				&& !element.getStructure().isInterfaceType() ) {
			if ( element.getStructure().isFinal() ) {
				diagnostics.add(
						declarationDiagnostic(
								ValidationCause.INVALID_SPI_IMPLEMENTATION_POINT,
								element,
								"An IMPLEMENT class is final"
						)
				);
			}
			if ( !hasSupportedSubclassConstructor( model, element ) ) {
				diagnostics.add(
						declarationDiagnostic(
								ValidationCause.INVALID_SPI_IMPLEMENTATION_POINT,
								element,
								"An IMPLEMENT class has no explicitly classified public or protected IMPLEMENT constructor"
						)
				);
			}
		}
		else if ( element.getKind() == METHOD
				&& element.getDeclaredRoles().contains( IMPLEMENT )
				&& (!element.getStructure().isExternallyAccessible()
						|| !element.getStructure().isOverridableMethod()) ) {
			diagnostics.add(
					declarationDiagnostic(
							ValidationCause.INVALID_SPI_IMPLEMENTATION_POINT,
							element,
							"A directly classified IMPLEMENT method is not overridable"
					)
			);
		}
	}

	private static boolean hasSupportedSubclassConstructor(
			ClassificationModel model,
			ClassificationModel.Element type) {
		for ( ClassificationModel.Element candidate : model.getElements() ) {
			if ( candidate.getKind() == CONSTRUCTOR
					&& type.getId().equals( candidate.getOwnerId() )
					&& candidate.getClassificationStatus() == RESOLVED
					&& candidate.getCategory() == SPI
					&& candidate.getDeclaredRoles().contains( IMPLEMENT )
					&& candidate.getStructure().isExternallyAccessible() ) {
				return true;
			}
		}
		return false;
	}

	private static ValidationDiagnostic declarationDiagnostic(
			ValidationCause cause,
			ClassificationModel.Element element,
			String message) {
		return new ValidationDiagnostic(
				cause,
				element.getId(),
				element.getId(),
				element.getCategory(),
				element.getCategory(),
				"DECLARATION",
				element.getEffectiveRoles().isEmpty()
						? Collections.emptySet()
						: EnumSet.copyOf( element.getEffectiveRoles() ),
				Collections.singletonList( element.getId() ),
				message
		);
	}
}
