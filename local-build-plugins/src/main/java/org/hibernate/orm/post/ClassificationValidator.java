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
			ValidationEvidence evidence,
			ValidationAllowlist allowlist) {
		final List<ValidationDiagnostic> diagnostics = new ArrayList<>();
		final ClassificationGraph graph = new ClassificationGraph( model );
		validateClassificationStatus( model, diagnostics );
		validateDependencies( model, graph, diagnostics );
		validateReachability( model, graph, diagnostics );
		for ( ValidationEvidence.Item item : evidence.get( ValidationRule.CLS005 ) ) {
			diagnostics.add( ValidationDiagnostic.fromEvidence( item ) );
		}
		return ValidationResult.complete( diagnostics, allowlist, ValidationRule.Domain.CLASSIFICATION );
	}

	private static void validateClassificationStatus(
			ClassificationModel model,
			List<ValidationDiagnostic> diagnostics) {
		for ( ClassificationModel.Element element : model.getElements() ) {
			if ( element.getClassificationStatus() == CONFLICTING ) {
				diagnostics.add(
						declarationDiagnostic(
								ValidationRule.CLS001,
								element,
								"Conflicting category evidence: " + element.getCategoryEvidence()
						)
				);
			}
			else if ( element.getClassificationStatus() == UNCLASSIFIED && element.getKind() != PACKAGE ) {
				diagnostics.add(
						declarationDiagnostic(
								ValidationRule.CLS004,
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
				if ( target == null || target.getClassificationStatus() != RESOLVED
						|| !isForbidden( source.getCategory(), target.getCategory() ) ) {
					continue;
				}
				final List<String> path = new ArrayList<>( graph.shortestCategoryPath( source.getCategory(), source.getId() ) );
				if ( path.isEmpty() ) {
					path.add( source.getId() );
				}
				path.add( target.getId() );
				diagnostics.add(
						new ValidationDiagnostic(
								ValidationRule.CLS002,
								source.getId(),
								target.getId(),
								source.getCategory(),
								target.getCategory(),
								reference.getKind().name(),
								source.getEffectiveRoles(),
								path,
								source.getCategory() + " -> " + target.getCategory() + " is forbidden"
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
							ValidationRule.CLS003,
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
			ValidationRule rule,
			ClassificationModel.Element element,
			String message) {
		return new ValidationDiagnostic(
				rule,
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
