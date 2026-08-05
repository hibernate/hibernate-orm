/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.post;

import java.util.Set;

/// Renders deterministic, actionable SPI validation diagnostics.
///
/// @author Steve Ebersole
public final class SpiValidationReportRenderer {
	public String render(SpiValidator.Result result) {
		final StringBuilder report = new StringBuilder();
		report.append( "Hibernate ORM SPI validation: " )
				.append( result.hasFailures() ? "FAILED" : "PASSED" )
				.append( '\n' )
				.append( "Rule severities: " );
		for ( int i = 0; i < SpiValidator.Rule.values().length; i++ ) {
			if ( i > 0 ) {
				report.append( ", " );
			}
			final SpiValidator.Rule rule = SpiValidator.Rule.values()[ i ];
			report.append( rule.getId() ).append( '=' ).append( rule.getSeverity() );
		}
		report.append( "\nEvidence-backed rules SPI006, SPI008, SPI009, and SPI010 evaluate only evidence supplied " )
				.append( "by source, provider-boundary, and compatibility adapters.\n\n" );

		if ( !result.getConfigurationErrors().isEmpty() ) {
			report.append( "Configuration errors:\n" );
			for ( String error : result.getConfigurationErrors() ) {
				report.append( "- " ).append( error ).append( '\n' );
			}
			report.append( '\n' );
		}

		if ( result.getDiagnostics().isEmpty() ) {
			report.append( "No SPI validation diagnostics.\n" );
			return report.toString();
		}

		for ( SpiValidator.Diagnostic diagnostic : result.getDiagnostics() ) {
			report.append( '[' ).append( diagnostic.getSeverity() ).append( "] " )
					.append( diagnostic.getRule().getId() ).append( ' ' )
					.append( diagnostic.getElementId() ).append( '\n' )
					.append( "  Message: " ).append( diagnostic.getMessage() ).append( '\n' )
					.append( "  Roles: " ).append( formatRoles( diagnostic.getRoles() ) ).append( '\n' )
					.append( "  Origins: " );
		if ( diagnostic.getOrigins().isEmpty() ) {
			report.append( "none" );
		}
		else {
			boolean first = true;
			for ( SpiModel.Origin origin : diagnostic.getOrigins() ) {
				if ( !first ) {
					report.append( "; " );
				}
				report.append( origin );
				first = false;
			}
		}
		report.append( '\n' )
				.append( "  Path: " ).append( String.join( " -> ", diagnostic.getPath() ) ).append( '\n' );
		if ( diagnostic.getAllowlistMatch() == null ) {
			report.append( "  Allowlist: none\n" );
		}
		else {
			final SpiValidationAllowlist.Entry match = diagnostic.getAllowlistMatch();
			report.append( "  Allowlist: matched; owner=" ).append( match.getOwner() )
					.append( "; removalRelease=" ).append( match.getRemovalRelease() )
					.append( "; reason=" ).append( match.getReason() ).append( '\n' );
		}
		report.append( "  Remediation: " ).append( diagnostic.getRemediation() ).append( "\n\n" );
		}
		return report.toString();
	}

	private static String formatRoles(Set<SpiModel.Role> roles) {
		if ( roles.isEmpty() ) {
			return "none";
		}
		final StringBuilder text = new StringBuilder();
		for ( SpiModel.Role role : SpiModel.Role.values() ) {
			if ( roles.contains( role ) ) {
				if ( text.length() > 0 ) {
					text.append( '+' );
				}
				text.append( role );
			}
		}
		return text.toString();
	}
}
