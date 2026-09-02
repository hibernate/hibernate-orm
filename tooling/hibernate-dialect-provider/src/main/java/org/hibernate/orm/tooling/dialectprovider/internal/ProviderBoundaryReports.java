/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.tooling.dialectprovider.internal;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbConfig;

/// Renders deterministic human-readable and machine-readable provider-
/// boundary reports.
///
/// @author Steve Ebersole
public final class ProviderBoundaryReports {
	public String text(
			ClassificationMetadata metadata,
			ProviderBoundaryAnalyzer.Result result,
			boolean warningsAsErrors) {
		final StringBuilder text = new StringBuilder()
				.append( "Hibernate ORM Dialect provider-boundary validation\n" )
				.append( "Metadata family: " ).append( metadata.family() ).append( '\n' )
				.append( "Metadata source version: " ).append( metadata.sourceVersion() ).append( '\n' )
				.append( "Findings: " ).append( result.diagnostics().size() ).append( '\n' )
				.append( "Warnings: " ).append( result.warningCount() ).append( '\n' )
				.append( "Errors: " ).append( result.errorCount() ).append( '\n' )
				.append( "Warnings as errors: " ).append( warningsAsErrors ).append( '\n' )
				.append( "Failed: " ).append( result.fails( warningsAsErrors ) ).append( "\n\n" );
		ProviderBoundaryCause currentCause = null;
		String currentTargetType = null;
		for ( ProviderBoundaryAnalyzer.Diagnostic diagnostic : result.diagnostics() ) {
			if ( diagnostic.cause() != currentCause ) {
				currentCause = diagnostic.cause();
				currentTargetType = null;
				text.append( diagnostic.severity() ).append( " [" ).append( currentCause ).append( "]\n" )
						.append( "  " ).append( currentCause.message() ).append( '\n' )
						.append( "  Remediation: " ).append( currentCause.remediation() ).append( '\n' );
			}
			final String targetType = owner( diagnostic.target() );
			if ( !targetType.equals( currentTargetType ) ) {
				currentTargetType = targetType;
				text.append( "  " ).append( targetType ).append( '\n' );
			}
			text.append( "    " ).append( diagnostic.source() )
					.append( " --" ).append( diagnostic.edge() ).append( "--> " )
					.append( diagnostic.target() ).append( '\n' )
					.append( "      path: " ).append( String.join( " -> ", diagnostic.path() ) ).append( '\n' )
					.append( "      provider artifact: " ).append( diagnostic.providerArtifact() ).append( '\n' )
					.append( "      upstream artifact: " ).append( diagnostic.upstreamArtifact() ).append( '\n' );
		}
		if ( result.diagnostics().isEmpty() ) {
			text.append( "No provider-boundary findings.\n" );
		}
		return text.toString();
	}

	public String json(
			ClassificationMetadata metadata,
			ProviderBoundaryAnalyzer.Result result,
			boolean warningsAsErrors) {
		final Map<String, Object> root = new LinkedHashMap<>();
		root.put( "schema", "hibernate-dialect-provider-boundary-validation" );
		root.put( "schemaVersion", 1 );
		root.put( "metadataFamily", metadata.family() );
		root.put( "metadataSourceVersion", metadata.sourceVersion() );
		root.put( "diagnosticCount", result.diagnostics().size() );
		root.put( "warningCount", result.warningCount() );
		root.put( "errorCount", result.errorCount() );
		root.put( "warningsAsErrors", warningsAsErrors );
		root.put( "failed", result.fails( warningsAsErrors ) );
		final Map<String, Integer> causeCounts = new LinkedHashMap<>();
		for ( ProviderBoundaryCause cause : ProviderBoundaryCause.values() ) {
			causeCounts.put( cause.name(), (int) result.diagnostics().stream()
					.filter( diagnostic -> diagnostic.cause() == cause )
					.count() );
		}
		root.put( "causeCounts", causeCounts );
		final List<Map<String, Object>> diagnostics = new ArrayList<>();
		for ( ProviderBoundaryAnalyzer.Diagnostic diagnostic : result.diagnostics() ) {
			final Map<String, Object> item = new LinkedHashMap<>();
			item.put( "cause", diagnostic.cause().name() );
			item.put( "severity", diagnostic.severity().name() );
			item.put( "message", diagnostic.cause().message() );
			item.put( "remediation", diagnostic.cause().remediation() );
			item.put( "source", diagnostic.source() );
			item.put( "target", diagnostic.target() );
			item.put( "edge", diagnostic.edge() );
			item.put( "targetCategory", diagnostic.targetCategory() );
			item.put( "targetRoles", diagnostic.targetRoles() );
			item.put( "path", diagnostic.path() );
			item.put( "providerArtifact", diagnostic.providerArtifact() );
			item.put( "upstreamArtifact", diagnostic.upstreamArtifact() );
			diagnostics.add( item );
		}
		root.put( "diagnostics", diagnostics );
		try ( Jsonb jsonb = JsonbBuilder.create( new JsonbConfig().withFormatting( true ) ) ) {
			return jsonb.toJson( root ) + '\n';
		}
		catch (Exception e) {
			throw new IllegalStateException( "Unable to render provider-boundary JSON report", e );
		}
	}

	private static String owner(String id) {
		final int colon = id.indexOf( ':' );
		final int hash = id.indexOf( '#', colon + 1 );
		return id.substring( colon + 1, hash < 0 ? id.length() : hash );
	}
}
