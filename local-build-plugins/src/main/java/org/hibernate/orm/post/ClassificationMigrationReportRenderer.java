/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.post;

import java.util.EnumMap;
import java.util.Map;

/// Renders a deterministic, review-oriented API and SPI migration report.
///
/// @author Steve Ebersole
public final class ClassificationMigrationReportRenderer {
	public String render(ClassificationMigrationValidator.Result result) {
		final StringBuilder report = new StringBuilder();
		report.append( "Hibernate ORM migration compatibility: " )
				.append( result.hasFailures() ? "FAILED" : "PASSED" )
				.append( "\n\n" )
				.append( "Baseline compatibility family: " ).append( result.getBaseline().getHibernateVersion() ).append( '\n' )
				.append( "Baseline source version: " ).append( result.getBaseline().getSourceVersion() ).append( '\n' )
				.append( "Current compatibility family: " ).append( result.getCurrent().getHibernateVersion() ).append( '\n' )
				.append( "Current source version: " ).append( result.getCurrent().getSourceVersion() ).append( '\n' )
				.append( "Classification schema: " ).append( ClassificationMetadata.SCHEMA )
				.append( " v" ).append( ClassificationMetadata.SCHEMA_VERSION ).append( '\n' )
				.append( "API major-family compatibility: " ).append( result.isApiEnforced() ? "ENFORCED" : "NOT_APPLICABLE" ).append( '\n' )
				.append( "SPI X.Y-family compatibility: " ).append( result.isSpiEnforced() ? "ENFORCED" : "NOT_APPLICABLE" ).append( "\n\n" );

		final Map<ClassificationMigrationValidator.Severity, Integer> counts = new EnumMap<>( ClassificationMigrationValidator.Severity.class );
		for ( ClassificationMigrationValidator.Diagnostic diagnostic : result.getDiagnostics() ) {
			counts.merge( diagnostic.getSeverity(), 1, Integer::sum );
		}
		report.append( "Diagnostics: " ).append( result.getDiagnostics().size() )
				.append( "; ERROR=" ).append( counts.getOrDefault( ClassificationMigrationValidator.Severity.ERROR, 0 ) )
				.append( "; REVIEW=" ).append( counts.getOrDefault( ClassificationMigrationValidator.Severity.REVIEW, 0 ) )
				.append( "\n\n" );

		if ( result.getDiagnostics().isEmpty() ) {
			return report.append( "No migration compatibility diagnostics.\n" ).toString();
		}
		for ( ClassificationMigrationValidator.Diagnostic diagnostic : result.getDiagnostics() ) {
			report.append( '[' ).append( diagnostic.getSeverity() ).append( "] " )
					.append( diagnostic.getSurface() == ClassificationMigrationValidator.Surface.API
							? "API_COMPATIBILITY_REGRESSION"
							: "SPI_COMPATIBILITY_REGRESSION" )
					.append( '\n' )
					.append( "  Element: " ).append( diagnostic.getElementId() ).append( '\n' )
					.append( "  Cause: " ).append( diagnostic.getJavaCause() == null
							? diagnostic.getFindingCause()
							: diagnostic.getJavaCause() ).append( '\n' )
					.append( "  Classification: " ).append( category( diagnostic.getBaselineCategory() ) )
					.append( " -> " ).append( category( diagnostic.getCurrentCategory() ) ).append( '\n' )
					.append( "  Roles: " ).append( diagnostic.getRoles().isEmpty() ? "none" : diagnostic.getRoles() ).append( '\n' )
					.append( "  Impacts: " ).append( diagnostic.getImpacts() ).append( '\n' )
					.append( "  Message: " ).append( diagnostic.getMessage() ).append( "\n\n" );
		}
		return report.toString();
	}

	private static String category(ClassificationModel.Category category) {
		return category == null ? "ABSENT_OR_UNSUPPORTED" : category.name();
	}
}
