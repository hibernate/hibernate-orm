/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.post;

import java.util.EnumMap;
import java.util.Map;

/// Renders deterministic, focused classification and SPI validation
/// diagnostics.
///
/// @author Steve Ebersole
public final class ValidationReportRenderer {
	public String render(String title, ValidationResult result) {
		final StringBuilder report = new StringBuilder();
		report.append( title ).append( ": " ).append( result.hasFailures() ? "FAILED" : "PASSED" ).append( "\n\n" );
		final Map<ValidationCause, Integer> counts = new EnumMap<>( ValidationCause.class );
		for ( ValidationDiagnostic diagnostic : result.getDiagnostics() ) {
			counts.merge( diagnostic.getCause(), 1, Integer::sum );
		}
		report.append( "Diagnostics: " ).append( result.getDiagnostics().size() );
		for ( Map.Entry<ValidationCause, Integer> count : counts.entrySet() ) {
			report.append( "; " ).append( count.getKey().name() ).append( '=' ).append( count.getValue() );
		}
		report.append( "\n\n" );

		if ( !result.getConfigurationErrors().isEmpty() ) {
			report.append( "Configuration errors:\n" );
			for ( String error : result.getConfigurationErrors() ) {
				report.append( "- " ).append( error ).append( '\n' );
			}
			report.append( '\n' );
		}

		if ( result.getDiagnostics().isEmpty() ) {
			return report.append( "No validation diagnostics.\n" ).toString();
		}

		for ( ValidationDiagnostic diagnostic : result.getDiagnostics() ) {
			report.append( '[' ).append( diagnostic.getSeverity() ).append( "] " )
					.append( diagnostic.getCause().name() ).append( '\n' )
					.append( "  Source: " ).append( diagnostic.getSourceElementId() )
					.append( " [" ).append( category( diagnostic.getSourceCategory() ) ).append( "]\n" )
					.append( "  Target: " ).append( diagnostic.getTargetElementId() )
					.append( " [" ).append( category( diagnostic.getTargetCategory() ) ).append( "]\n" );
			if ( hasRelationship( diagnostic.getCause() ) ) {
				report.append( "  Edge: " ).append( diagnostic.getEdgeKind() ).append( '\n' );
			}
			report.append( "  Roles: " ).append( diagnostic.getRoles().isEmpty() ? "none" : diagnostic.getRoles() ).append( '\n' )
					.append( "  Message: " ).append( diagnostic.getMessage() ).append( '\n' )
					.append( "  Path: " ).append( String.join( " -> ", diagnostic.getPath() ) ).append( '\n' );
			if ( diagnostic.getAllowlistMatch() == null ) {
				report.append( "  Allowlist: none\n" );
			}
			else {
				final ValidationAllowlist.Entry match = diagnostic.getAllowlistMatch();
				report.append( "  Allowlist: matched; owner=" ).append( match.getOwner() )
						.append( "; removalRelease=" ).append( match.getRemovalRelease() )
						.append( "; reason=" ).append( match.getReason() ).append( '\n' );
			}
			report.append( "  Remediation: " ).append( diagnostic.getRemediation() ).append( "\n\n" );
		}
		return report.toString();
	}

	private static String category(ClassificationModel.Category category) {
		return category == null ? "UNRESOLVED_OR_EXTERNAL" : category.name();
	}

	private static boolean hasRelationship(ValidationCause cause) {
		return cause == ValidationCause.FORBIDDEN_CATEGORY_DEPENDENCY
				|| cause == ValidationCause.INVALID_CATEGORY_REACHABILITY;
	}
}
