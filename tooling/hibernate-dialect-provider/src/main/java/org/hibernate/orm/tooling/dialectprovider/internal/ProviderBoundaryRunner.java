/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.tooling.dialectprovider.internal;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/// Process entry point for the Gradle-independent provider-boundary engine and
/// generated contract-test bridge.
///
/// @author Steve Ebersole
public final class ProviderBoundaryRunner {
	private ProviderBoundaryRunner() {
	}

	public static void main(String[] arguments) throws Exception {
		if ( arguments.length < 1 ) {
			throw new IllegalArgumentException( "Expected generate or validate command" );
		}
		if ( "generate".equals( arguments[0] ) ) {
			ContractTestGenerator.generate( Path.of( arguments[1] ), List.of( arguments[2] ) );
			return;
		}
		if ( !"validate".equals( arguments[0] ) ) {
			throw new IllegalArgumentException( "Invalid provider-boundary runner arguments" );
		}
		Path metadataPath = null;
		Path textReport = null;
		Path jsonReport = null;
		final List<String> providerPackages = new ArrayList<>();
		final List<Path> providers = new ArrayList<>();
		final List<Path> upstream = new ArrayList<>();
		boolean warningsAsErrors = false;
		for ( int i = 1; i < arguments.length; i += 2 ) {
			if ( i + 1 >= arguments.length ) {
				throw new IllegalArgumentException( "Missing value for " + arguments[i] );
			}
			final String value = arguments[i + 1];
			switch ( arguments[i] ) {
				case "--metadata" -> metadataPath = Path.of( value );
				case "--provider-package" -> providerPackages.add( value );
				case "--text-report" -> textReport = Path.of( value );
				case "--json-report" -> jsonReport = Path.of( value );
				case "--provider" -> providers.add( Path.of( value ) );
				case "--upstream" -> upstream.add( Path.of( value ) );
				case "--warnings-as-errors" -> warningsAsErrors = booleanValue( arguments[i], value );
				default -> throw new IllegalArgumentException( "Unknown validation option " + arguments[i] );
			}
		}
		if ( metadataPath == null || textReport == null || jsonReport == null
				|| providerPackages.isEmpty() || providers.isEmpty() || upstream.isEmpty() ) {
			throw new IllegalArgumentException( "Incomplete provider-boundary validation arguments" );
		}
		final ClassificationMetadata metadata = new ClassificationMetadataReader().read( metadataPath );
		final ProviderBoundaryAnalyzer.Result result = new ProviderBoundaryAnalyzer().analyze(
				providers, upstream, providerPackages, metadata
		);
		final ProviderBoundaryReports reports = new ProviderBoundaryReports();
		write( textReport, reports.text( metadata, result, warningsAsErrors ) );
		write( jsonReport, reports.json( metadata, result, warningsAsErrors ) );
		if ( result.hasWarnings() ) {
			System.err.println(
					"Dialect provider has " + result.warningCount() + " boundary warning(s); see "
							+ textReport + " and " + jsonReport
			);
		}
		if ( result.fails( warningsAsErrors ) ) {
			throw new IllegalStateException(
					"Dialect provider has " + result.errorCount() + " boundary error(s) and "
							+ result.warningCount() + " warning(s)"
							+ ( warningsAsErrors ? " with warnings-as-errors enabled" : "" ) + "; see "
							+ textReport + " and " + jsonReport
			);
		}
	}

	private static boolean booleanValue(String option, String value) {
		if ( "true".equalsIgnoreCase( value ) ) {
			return true;
		}
		if ( "false".equalsIgnoreCase( value ) ) {
			return false;
		}
		throw new IllegalArgumentException( "Expected true or false for " + option + ", but found " + value );
	}

	private static void write(Path path, String value) throws Exception {
		Files.createDirectories( path.getParent() );
		Files.writeString( path, value, StandardCharsets.UTF_8 );
	}
}
