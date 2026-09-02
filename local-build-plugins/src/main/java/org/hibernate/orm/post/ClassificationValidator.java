/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.post;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.hibernate.orm.post.ClassificationModel.Category.API;
import static org.hibernate.orm.post.ClassificationModel.Category.INTERNAL;
import static org.hibernate.orm.post.ClassificationModel.Category.SPI;
import static org.hibernate.orm.post.ClassificationModel.ClassificationStatus.CONFLICTING;
import static org.hibernate.orm.post.ClassificationModel.ClassificationStatus.RESOLVED;
import static org.hibernate.orm.post.ClassificationModel.ClassificationStatus.UNCLASSIFIED;
import static org.hibernate.orm.post.ClassificationModel.ElementKind.PACKAGE;
import static org.hibernate.orm.post.ClassificationModel.ReferenceTarget.HIBERNATE;

/// Applies the approved category, dependency, reachability, and consumer
/// boundary rules to the canonical classification model.
///
/// @author Steve Ebersole
public final class ClassificationValidator {
	public ValidationResult validate(
			ClassificationModel model,
			ValidationAllowlist allowlist) {
		return validate( model, allowlist, ClassificationValidationScope.platformOnly() );
	}

	public ValidationResult validate(
			ClassificationModel model,
			ValidationAllowlist allowlist,
			ClassificationValidationScope scope) {
		final List<ValidationDiagnostic> diagnostics = new ArrayList<>();
		final ClassificationGraph graph = new ClassificationGraph( model );
		validateClassificationStatus( model, diagnostics );
		validateDependencies( model, graph, scope, diagnostics );
		validateReachability( model, graph, diagnostics );
		return ValidationResult.complete( diagnostics, allowlist, ValidationCause.Domain.CLASSIFICATION );
	}

	private static void validateClassificationStatus(
			ClassificationModel model,
			List<ValidationDiagnostic> diagnostics) {
		for ( ClassificationModel.Element element : model.getElements() ) {
			if ( element.getClassificationStatus() == CONFLICTING ) {
				diagnostics.add(
						declarationDiagnostic(
								ValidationCause.CONFLICTING_CLASSIFICATION,
								element,
								"Conflicting category evidence: " + element.getCategoryEvidence()
						)
				);
			}
			else if ( element.getClassificationStatus() == UNCLASSIFIED && element.getKind() != PACKAGE ) {
				diagnostics.add(
						declarationDiagnostic(
								ValidationCause.UNCLASSIFIED_HIBERNATE_DECLARATION,
								element,
								"Hibernate declaration retained in the reportable surface has no effective category"
						)
				);
			}
		}
	}

	private static void validateDependencies(
			ClassificationModel model,
			ClassificationGraph graph,
			ClassificationValidationScope scope,
			List<ValidationDiagnostic> diagnostics) {
		for ( ClassificationModel.Element source : model.getElements() ) {
			if ( source.getClassificationStatus() != RESOLVED ) {
				continue;
			}
			for ( ClassificationModel.Reference reference : source.getReferences() ) {
				if ( reference.getTarget() != HIBERNATE ) {
					continue;
				}
				final ClassificationModel.Element target = model.getElement( reference.getTargetElementId() );
				if ( target == null || target.getClassificationStatus() != RESOLVED ) {
					continue;
				}
				final String violation = forbiddenRelationship( source, target, reference, scope );
				if ( violation == null ) {
					continue;
				}
				final List<String> path = new ArrayList<>( graph.shortestCategoryPath( source.getCategory(), source.getId() ) );
				if ( path.isEmpty() ) {
					path.add( source.getId() );
				}
				path.add( target.getId() );
				diagnostics.add(
						new ValidationDiagnostic(
								ValidationCause.FORBIDDEN_CATEGORY_DEPENDENCY,
								source.getId(),
								target.getId(),
								source.getCategory(),
								target.getCategory(),
								reference.getKind().name(),
								source.getEffectiveRoles(),
								path,
								violation
						)
				);
			}
		}
	}

	private static void validateReachability(
			ClassificationModel model,
			ClassificationGraph graph,
			List<ValidationDiagnostic> diagnostics) {
		for ( ClassificationModel.Element target : model.getElements() ) {
			if ( target.getClassificationStatus() != RESOLVED
					|| (target.getCategory() != API && target.getCategory() != SPI)
					|| isRoot( target )
					|| !graph.shortestCategoryPath( target.getCategory(), target.getId() ).isEmpty() ) {
				continue;
			}
			final List<String> path = graph.shortestPathFromAnyRoot( target.getId() );
			final List<String> usefulPath = path.isEmpty() ? Collections.singletonList( target.getId() ) : path;
			final String sourceId = usefulPath.size() < 2
					? target.getId()
					: usefulPath.get( usefulPath.size() - 2 );
			final ClassificationModel.Element source = model.getElement( sourceId );
			final ClassificationGraph.Edge crossing = graph.edge( sourceId, target.getId() );
			diagnostics.add(
					new ValidationDiagnostic(
							ValidationCause.INVALID_CATEGORY_REACHABILITY,
							sourceId,
							target.getId(),
							source == null ? null : source.getCategory(),
							target.getCategory(),
							crossing == null ? "DECLARATION" : crossing.getKind(),
							target.getEffectiveRoles(),
							usefulPath,
							"Non-root " + target.getCategory() + " declaration is not reachable from a "
									+ target.getCategory() + " root without crossing a forbidden category"
					)
			);
		}
	}

	private static boolean isForbidden(
			ClassificationModel.Category source,
			ClassificationModel.Category target) {
		return source == API && (target == SPI || target == INTERNAL)
				|| source == SPI && target == INTERNAL;
	}

	private static String forbiddenRelationship(
			ClassificationModel.Element source,
			ClassificationModel.Element target,
			ClassificationModel.Reference reference,
			ClassificationValidationScope scope) {
		if ( scope.isProviderToPlatform( source, target ) ) {
			return target.getCategory() == INTERNAL
					? "Provider artifact contract -> upstream platform INTERNAL is forbidden"
					: null;
		}
		if ( scope.isPlatformToProvider( source, target ) ) {
			return "Platform contract -> downstream provider artifact is forbidden";
		}
		if ( source.getCategory() == API
				&& target.getCategory() == SPI
				&& reference.getKind() == ClassificationModel.ReferenceKind.ANNOTATION_CLASS_SELECTION ) {
			return null;
		}
		return isForbidden( source.getCategory(), target.getCategory() )
				? source.getCategory() + " -> " + target.getCategory() + " is forbidden"
				: null;
	}

	private static boolean isRoot(ClassificationModel.Element element) {
		for ( ClassificationModel.ClassificationOrigin origin : element.getClassificationOrigins() ) {
			if ( origin.getCategory() == element.getCategory()
					&& origin.getSourceElementId().equals( element.getId() ) ) {
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
				"CLASSIFICATION",
				element.getEffectiveRoles(),
				Collections.singletonList( element.getId() ),
				message
		);
	}
}
