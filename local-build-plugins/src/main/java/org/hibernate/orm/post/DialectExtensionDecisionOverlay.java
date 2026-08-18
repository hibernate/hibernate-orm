/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.post;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// Temporary, human-maintained decisions joined to generated Dialect facts by
/// stable element identifier.
///
/// @author Steve Ebersole
final class DialectExtensionDecisionOverlay {
	static final String HEADER = "elementId\treviewGroup\tdecisionStatus\ttargetCategory\troles\tdisposition\trationale\treplacement\t"
			+ "migrationGuideAnchor\townerPhase\tnotes";

	private final Map<String, Decision> decisions;
	private final boolean loaded;

	private DialectExtensionDecisionOverlay(Map<String, Decision> decisions, boolean loaded) {
		this.decisions = Collections.unmodifiableMap( new LinkedHashMap<>( decisions ) );
		this.loaded = loaded;
	}

	static DialectExtensionDecisionOverlay empty() {
		return new DialectExtensionDecisionOverlay( Collections.emptyMap(), false );
	}

	static DialectExtensionDecisionOverlay read(Path path) {
		final List<String> lines;
		try {
			lines = Files.readAllLines( path, StandardCharsets.UTF_8 );
		}
		catch (IOException e) {
			throw new IllegalArgumentException( "Unable to read Dialect decision overlay " + path, e );
		}
		if ( lines.isEmpty() || !HEADER.equals( lines.get( 0 ) ) ) {
			throw new IllegalArgumentException( "Unexpected Dialect decision overlay header in " + path );
		}
		final Map<String, Decision> decisions = new LinkedHashMap<>();
		for ( int i = 1; i < lines.size(); i++ ) {
			if ( lines.get( i ).isBlank() ) {
				continue;
			}
			final String[] cells = lines.get( i ).split( "\\t", -1 );
			if ( cells.length != 11 ) {
				throw new IllegalArgumentException(
						"Expected 11 cells at " + path + ':' + (i + 1) + " but found " + cells.length
				);
			}
			final Decision decision = new Decision( cells );
			if ( decisions.put( decision.getElementId(), decision ) != null ) {
				throw new IllegalArgumentException( "Duplicate Dialect decision for " + decision.getElementId() );
			}
		}
		return new DialectExtensionDecisionOverlay( decisions, true );
	}

	boolean isLoaded() {
		return loaded;
	}

	Decision decision(DialectExtensionInventory.SurfaceDeclaration declaration) {
		final Decision existing = decisions.get( declaration.getElementId() );
		return existing == null ? Decision.empty( declaration ) : existing;
	}

	void validate(DialectExtensionInventory inventory) {
		final Map<String, DialectExtensionInventory.SurfaceDeclaration> declarations = new LinkedHashMap<>();
		for ( DialectExtensionInventory.SurfaceDeclaration declaration : inventory.getDialectSurface() ) {
			declarations.put( declaration.getElementId(), declaration );
		}
		for ( Map.Entry<String, Decision> entry : decisions.entrySet() ) {
			final String elementId = entry.getKey();
			final Decision decision = entry.getValue();
			final DialectExtensionInventory.SurfaceDeclaration declaration = declarations.get( elementId );
			if ( declaration == null ) {
				if ( decision.isCompletedSurfaceRemoval() ) {
					continue;
				}
				throw new IllegalArgumentException( "Dialect decision does not match the current surface: " + elementId );
			}
			if ( "FIELD".equals( declaration.getKind() )
					&& decision.hasRole( "IMPLEMENT" ) ) {
				throw new IllegalArgumentException( "IMPLEMENT is not valid for a field: " + elementId );
			}
			if ( "CONSTRUCTOR".equals( declaration.getKind() )
					&& decision.hasRole( "SUPPLY" ) ) {
				throw new IllegalArgumentException( "SUPPLY is not valid for a constructor: " + elementId );
			}
		}
	}

	String write(DialectExtensionInventory inventory) {
		validate( inventory );
		final StringBuilder result = new StringBuilder( HEADER ).append( '\n' );
		for ( DialectExtensionInventory.SurfaceDeclaration declaration : inventory.getDialectSurface() ) {
			result.append( decision( declaration ).tsv() ).append( '\n' );
		}
		return result.toString();
	}

	static final class Decision {
		private final String elementId;
		private final String reviewGroup;
		private final String decisionStatus;
		private final String targetCategory;
		private final String roles;
		private final String disposition;
		private final String rationale;
		private final String replacement;
		private final String migrationGuideAnchor;
		private final String ownerPhase;
		private final String notes;

		private Decision(String[] cells) {
			elementId = required( cells[0], "elementId" );
			reviewGroup = required( cells[1], "reviewGroup for " + elementId );
			decisionStatus = decisionStatus( cells[2], elementId );
			targetCategory = cells[3];
			roles = cells[4];
			disposition = cells[5];
			rationale = cells[6];
			replacement = cells[7];
			migrationGuideAnchor = cells[8];
			ownerPhase = cells[9];
			notes = cells[10];
			if ( !decisionStatus.isBlank() ) {
				required( targetCategory, "targetCategory for " + elementId );
				required( disposition, "disposition for " + elementId );
				required( rationale, "rationale for " + elementId );
				if ( !"API".equals( targetCategory )
						&& !"SPI".equals( targetCategory )
						&& !"INTERNAL".equals( targetCategory ) ) {
					throw new IllegalArgumentException(
							"Unexpected targetCategory for " + elementId + ": " + targetCategory
					);
				}
				if ( !"SPI".equals( targetCategory ) && !roles.isBlank() ) {
					throw new IllegalArgumentException( "Only SPI decisions may define roles: " + elementId );
				}
			}
		}

		private static Decision empty(DialectExtensionInventory.SurfaceDeclaration declaration) {
			return new Decision(
					new String[] { declaration.getElementId(), declaration.getReviewGroup(), "", "", "", "", "", "", "", "", "" }
			);
		}

		String getElementId() {
			return elementId;
		}

		String getReviewGroup() {
			return reviewGroup;
		}

		String getDecisionStatus() {
			return decisionStatus;
		}

		String getTargetCategory() {
			return targetCategory;
		}

		String getRoles() {
			return roles;
		}

		String getDisposition() {
			return disposition;
		}

		String getRationale() {
			return rationale;
		}

		String getReplacement() {
			return replacement;
		}

		String getMigrationGuideAnchor() {
			return migrationGuideAnchor;
		}

		String getOwnerPhase() {
			return ownerPhase;
		}

		String getNotes() {
			return notes;
		}

		boolean hasDecision() {
			return !decisionStatus.isBlank();
		}

		boolean isProposed() {
			return "PROPOSED".equals( decisionStatus );
		}

		boolean isApproved() {
			return "APPROVED".equals( decisionStatus );
		}

		boolean isCompleted() {
			return "COMPLETED".equals( decisionStatus );
		}

		private boolean isCompletedSurfaceRemoval() {
			return isCompleted()
					&& ( "REMOVE".equals( disposition )
							|| "REPLACE".equals( disposition )
							|| "MOVE_BEHIND_INTERNAL_COLLABORATOR".equals( disposition ) );
		}

		private boolean hasRole(String role) {
			for ( String declaredRole : roles.split( "," ) ) {
				if ( role.equals( declaredRole ) ) {
					return true;
				}
			}
			return false;
		}

		private String tsv() {
			return cell( elementId ) + '\t' + cell( reviewGroup ) + '\t' + cell( decisionStatus ) + '\t'
					+ cell( targetCategory ) + '\t' + cell( roles ) + '\t'
					+ cell( disposition ) + '\t' + cell( rationale ) + '\t' + cell( replacement ) + '\t'
					+ cell( migrationGuideAnchor ) + '\t' + cell( ownerPhase ) + '\t' + cell( notes );
		}

		private static String decisionStatus(String value, String elementId) {
			if ( value.isBlank()
					|| "PROPOSED".equals( value )
					|| "APPROVED".equals( value )
					|| "COMPLETED".equals( value ) ) {
				return value;
			}
			throw new IllegalArgumentException( "Unexpected decisionStatus for " + elementId + ": " + value );
		}

		private static String cell(String value) {
			return value.replace( '\t', ' ' ).replace( '\n', ' ' ).replace( '\r', ' ' );
		}

		private static String required(String value, String description) {
			if ( value.isBlank() ) {
				throw new IllegalArgumentException( "Missing " + description + " in Dialect decision overlay" );
			}
			return value;
		}
	}
}
