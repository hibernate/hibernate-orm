/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.post;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/// Reads and validates the compact, human-maintained Phase 1 family decisions.
/// Generated family facts remain in the inventory; this overlay contains only
/// proposed or approved review judgments.
///
/// @author Steve Ebersole
final class DialectFamilyDecisionOverlay {
	static final String HEADER = "candidateId\tdecisionStatus\tdisposition\tproposedContract\trationale";
	private static final Set<String> STATUSES = Set.of( "PROPOSED", "APPROVED" );
	private static final Set<String> DISPOSITIONS = Set.of(
			"SUPPORTED_FAMILY_BASE",
			"SUPPORTED_CONCRETE_PROVIDER_BASE",
			"COMPOSABLE_CAPABILITY_OR_STRATEGY",
			"NO_SUPPORTED_EXTENSION"
	);

	private final Map<String, Decision> decisions;

	private DialectFamilyDecisionOverlay(Map<String, Decision> decisions) {
		this.decisions = decisions;
	}

	static DialectFamilyDecisionOverlay read(Path path) {
		try {
			final java.util.List<String> lines = Files.readAllLines( path, StandardCharsets.UTF_8 );
			if ( lines.isEmpty() || !HEADER.equals( lines.get( 0 ) ) ) {
				throw new IllegalArgumentException( "Unexpected Dialect family decision header in " + path );
			}
			final Map<String, Decision> decisions = new LinkedHashMap<>();
			for ( int lineNumber = 1; lineNumber < lines.size(); lineNumber++ ) {
				final String line = lines.get( lineNumber );
				if ( line.isBlank() || line.startsWith( "#" ) ) {
					continue;
				}
				final String[] values = line.split( "\t", -1 );
				if ( values.length != 5 ) {
					throw new IllegalArgumentException(
							"Expected 5 Dialect family decision columns at " + path + ':' + (lineNumber + 1)
					);
				}
				final Decision decision = new Decision( values[0], values[1], values[2], values[3], values[4] );
				decision.validate( path, lineNumber + 1 );
				if ( decisions.put( decision.getCandidateId(), decision ) != null ) {
					throw new IllegalArgumentException( "Duplicate Dialect family decision " + decision.getCandidateId() );
				}
			}
			return new DialectFamilyDecisionOverlay( decisions );
		}
		catch (IOException e) {
			throw new IllegalStateException( "Unable to read Dialect family decisions " + path, e );
		}
	}

	void validate(DialectExtensionInventory inventory) {
		for ( DialectExtensionInventory.FamilyCandidate family : inventory.getFamilyCandidates() ) {
			requireDecision( family.getId() + ":DIALECT" );
			requireDecision( family.getId() + ":TRANSLATOR" );
		}
		if ( decisions.size() != inventory.getFamilyCandidates().size() * 2 ) {
			throw new IllegalArgumentException( "Dialect family decisions contain an unknown candidate" );
		}
	}

	Decision decision(String candidateId) {
		return requireDecision( candidateId );
	}

	private Decision requireDecision(String candidateId) {
		final Decision decision = decisions.get( candidateId );
		if ( decision == null ) {
			throw new IllegalArgumentException( "Missing Dialect family decision " + candidateId );
		}
		return decision;
	}

	static final class Decision {
		private final String candidateId;
		private final String decisionStatus;
		private final String disposition;
		private final String proposedContract;
		private final String rationale;

		private Decision(
				String candidateId,
				String decisionStatus,
				String disposition,
				String proposedContract,
				String rationale) {
			this.candidateId = candidateId;
			this.decisionStatus = decisionStatus;
			this.disposition = disposition;
			this.proposedContract = proposedContract;
			this.rationale = rationale;
		}

		private void validate(Path path, int lineNumber) {
			if ( !candidateId.matches( "[A-Z0-9_]+:(DIALECT|TRANSLATOR)" ) ) {
				throw invalid( path, lineNumber, "candidateId", candidateId );
			}
			if ( !STATUSES.contains( decisionStatus ) ) {
				throw invalid( path, lineNumber, "decisionStatus", decisionStatus );
			}
			if ( !DISPOSITIONS.contains( disposition ) ) {
				throw invalid( path, lineNumber, "disposition", disposition );
			}
			if ( proposedContract.isBlank() || rationale.isBlank() ) {
				throw new IllegalArgumentException( "Dialect family decision must include contract and rationale at " + path + ':' + lineNumber );
			}
		}

		String getCandidateId() {
			return candidateId;
		}

		String getDecisionStatus() {
			return decisionStatus;
		}

		String getDisposition() {
			return disposition;
		}

		String getProposedContract() {
			return proposedContract;
		}

		String getRationale() {
			return rationale;
		}

		private static IllegalArgumentException invalid(Path path, int lineNumber, String field, String value) {
			return new IllegalArgumentException(
					"Invalid " + field + " `" + value + "` at " + path + ':' + lineNumber
			);
		}
	}
}
