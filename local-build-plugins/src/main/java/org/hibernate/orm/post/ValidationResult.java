/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.post;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/// Validation diagnostics plus migration-allowlist consistency failures.
///
/// @author Steve Ebersole
public final class ValidationResult {
	private final List<ValidationDiagnostic> diagnostics;
	private final List<String> configurationErrors;

	static ValidationResult complete(
			List<ValidationDiagnostic> diagnostics,
			ValidationAllowlist allowlist,
			ValidationRule.Domain domain) {
		diagnostics.sort( ValidationDiagnostic.ORDERING );
		final Set<ValidationAllowlist.Entry> usedEntries = new HashSet<>();
		for ( ValidationDiagnostic diagnostic : diagnostics ) {
			final ValidationAllowlist.Entry match = allowlist.find( diagnostic );
			if ( match != null ) {
				diagnostic.setAllowlistMatch( match );
				usedEntries.add( match );
			}
		}

		final List<String> configurationErrors = new ArrayList<>();
		for ( ValidationAllowlist.Entry entry : allowlist.getEntries() ) {
			if ( entry.getRule().getDomain() == domain && !usedEntries.contains( entry ) ) {
				configurationErrors.add( "Unused validation allowlist entry: " + entry );
			}
		}
		return new ValidationResult( diagnostics, configurationErrors );
	}

	private ValidationResult(List<ValidationDiagnostic> diagnostics, List<String> configurationErrors) {
		this.diagnostics = Collections.unmodifiableList( new ArrayList<>( diagnostics ) );
		this.configurationErrors = Collections.unmodifiableList( new ArrayList<>( configurationErrors ) );
	}

	public List<ValidationDiagnostic> getDiagnostics() {
		return diagnostics;
	}

	public List<String> getConfigurationErrors() {
		return configurationErrors;
	}

	public boolean hasFailures() {
		if ( !configurationErrors.isEmpty() ) {
			return true;
		}
		for ( ValidationDiagnostic diagnostic : diagnostics ) {
			if ( diagnostic.getSeverity() == ValidationRule.Severity.ERROR
					&& diagnostic.getAllowlistMatch() == null ) {
				return true;
			}
		}
		return false;
	}
}
