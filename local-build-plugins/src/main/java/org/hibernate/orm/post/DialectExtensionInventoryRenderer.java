/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.post;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbConfig;

/// Renders the representative generated-facts, decision-overlay, and review
/// summary formats for Phase 0. These formats remain provisional until the
/// Phase 0 checkpoint accepts them.
///
/// @author Steve Ebersole
final class DialectExtensionInventoryRenderer {
	String json(DialectExtensionInventory inventory, String hibernateVersion, String sourceVersion) {
		final Map<String, Object> root = new LinkedHashMap<>();
		root.put( "schema", DialectExtensionInventory.SCHEMA );
		root.put( "schemaVersion", DialectExtensionInventory.SCHEMA_VERSION );
		root.put( "hibernateVersion", hibernateVersion );
		root.put( "sourceVersion", sourceVersion );
		root.put( "dialectSurface", surface( inventory ) );
		root.put( "overrides", relationships( inventory.getOverrides() ) );
		root.put( "dialectHierarchy", relationships( inventory.getDialectHierarchy() ) );
		root.put( "dialectCalls", links( inventory.getDialectCalls() ) );
		root.put( "communityInternalDependencies", dependencies( inventory ) );
		root.put( "communityExtensionUses", extensionUses( inventory ) );
		root.put( "selectionMetadataElements", inventory.getSelectionMetadataElements() );
		root.put( "selectionMechanisms", selectionMechanisms( inventory ) );
		root.put( "dialectSelections", dialectSelections( inventory ) );
		root.put( "familyCandidates", familyCandidates( inventory ) );
		try ( Jsonb jsonb = JsonbBuilder.create( new JsonbConfig().withNullValues( true ) ) ) {
			return jsonb.toJson( root ) + '\n';
		}
		catch (Exception e) {
			throw new IllegalStateException( "Unable to write Dialect extension inventory", e );
		}
	}

	String decisionOverlay(
			DialectExtensionInventory inventory,
			DialectExtensionDecisionOverlay decisions) {
		return decisions.write( inventory );
	}

	String review(
			DialectExtensionInventory inventory,
			DialectExtensionDecisionOverlay decisions,
			String hibernateVersion,
			String sourceVersion) {
		decisions.validate( inventory );
		final Map<String, List<DialectExtensionInventory.Relationship>> overrides = overridesByTarget( inventory );
		final Map<String, List<BytecodeLinkageAnalyzer.Link>> calls = callsByDialectDeclaration( inventory );
		final Map<String, List<DialectExtensionInventory.SurfaceDeclaration>> reviewGroups = reviewGroups( inventory, decisions );
		int proposed = 0;
		int approved = 0;
		for ( DialectExtensionInventory.SurfaceDeclaration declaration : inventory.getDialectSurface() ) {
			final DialectExtensionDecisionOverlay.Decision decision = decisions.decision( declaration );
			if ( decision.isProposed() ) {
				proposed++;
			}
			else if ( decision.isApproved() ) {
				approved++;
			}
		}

		final StringBuilder result = new StringBuilder();
		result.append( "= Dialect Extension Surface Review\n" )
				.append( ":toc2:\n:toclevels: 3\n:sectanchors:\n\n" )
				.append( "Hibernate compatibility level:: `" ).append( hibernateVersion ).append( "`\n" )
				.append( "Source version:: `" ).append( sourceVersion ).append( "`\n" )
				.append( "Decision overlay:: " )
				.append( decisions.isLoaded() ? "Loaded" : "Not yet checked in; showing the generated scaffold" )
				.append( "\n\n" )
				.append( "This generated report joins current compiled facts with the temporary, " )
				.append( "human-maintained decision overlay. Generated facts are refreshed on every run; " )
				.append( "only human decisions and their rationale belong in the overlay.\n\n" )
				.append( "[cols=\"60,20\",options=\"header\"]\n|===\n|Review state |Count\n" )
				.append( "|Total declarations |" ).append( inventory.getDialectSurface().size() ).append( '\n' )
				.append( "|Proposed, awaiting review |" ).append( proposed ).append( '\n' )
				.append( "|Approved |" ).append( approved ).append( '\n' )
				.append( "|Undecided |" ).append( inventory.getDialectSurface().size() - proposed - approved ).append( '\n' )
				.append( "|===\n\n" );

		for ( Map.Entry<String, List<DialectExtensionInventory.SurfaceDeclaration>> reviewGroup : reviewGroups.entrySet() ) {
			result.append( "== " ).append( title( reviewGroup.getKey() ) ).append( "\n\n" );
			for ( DialectExtensionInventory.SurfaceDeclaration declaration : reviewGroup.getValue() ) {
				appendDeclaration(
						result,
						declaration,
						decisions.decision( declaration ),
						overrides.getOrDefault( declaration.getElementId(), List.of() ),
						calls.getOrDefault( declaration.getElementId(), List.of() )
				);
			}
		}
		return result.toString();
	}

	String summary(DialectExtensionInventory inventory) {
		final Map<String, Integer> surfaceReviewGroups = new LinkedHashMap<>();
		final Map<String, Integer> surfaceCategories = new LinkedHashMap<>();
		int publicDeclarations = 0;
		int protectedDeclarations = 0;
		int overridableDeclarations = 0;
		for ( DialectExtensionInventory.SurfaceDeclaration declaration : inventory.getDialectSurface() ) {
			surfaceReviewGroups.merge( declaration.getReviewGroup(), 1, Integer::sum );
			surfaceCategories.merge( declaration.getCategory(), 1, Integer::sum );
			if ( "PUBLIC".equals( declaration.getVisibility() ) ) {
				publicDeclarations++;
			}
			else {
				protectedDeclarations++;
			}
			if ( declaration.isOverridable() ) {
				overridableDeclarations++;
			}
		}
		final Map<String, Integer> extensionFamilies = new LinkedHashMap<>();
		for ( DialectExtensionInventory.ExtensionUse use : inventory.getCommunityExtensionUses() ) {
			extensionFamilies.merge( use.getFamily(), 1, Integer::sum );
		}
		int documentedDialects = 0;
		int shortNames = 0;
		int automaticMappings = 0;
		for ( DialectExtensionInventory.DialectSelection dialect : inventory.getDialectSelections() ) {
			if ( !dialect.getDocumentationSources().isEmpty() ) {
				documentedDialects++;
			}
			shortNames += dialect.getShortNames().size();
			automaticMappings += dialect.getAutomaticResolution().size();
		}

		final StringBuilder result = new StringBuilder();
		result.append( "= Dialect Extension Inventory\n\n" )
				.append( "This is a generated, migration-scoped Phase 0 review artifact.\n\n" )
				.append( "[cols=\"60,20\",options=\"header\"]\n|===\n|Fact |Count\n" )
				.append( "|Public/protected `Dialect` declarations |" ).append( inventory.getDialectSurface().size() ).append( '\n' )
				.append( "|Public `Dialect` declarations |" ).append( publicDeclarations ).append( '\n' )
				.append( "|Protected `Dialect` declarations |" ).append( protectedDeclarations ).append( '\n' )
				.append( "|Overridable `Dialect` methods |" ).append( overridableDeclarations ).append( '\n' )
				.append( "|Observed overrides |" ).append( inventory.getOverrides().size() ).append( '\n' )
				.append( "|Dialect inheritance edges |" ).append( inventory.getDialectHierarchy().size() ).append( '\n' )
				.append( "|Direct calls to `Dialect` surface |" ).append( inventory.getDialectCalls().size() ).append( '\n' )
				.append( "|Community dependencies on internal contracts |" )
				.append( inventory.getCommunityInternalDependencies().size() ).append( '\n' )
				.append( "|Community extension-family uses |" ).append( inventory.getCommunityExtensionUses().size() ).append( '\n' )
				.append( "|Selection metadata elements |" ).append( inventory.getSelectionMetadataElements().size() ).append( '\n' )
				.append( "|Selection mechanisms |" ).append( inventory.getSelectionMechanisms().size() ).append( '\n' )
				.append( "|Concrete Dialects in the selection matrix |" ).append( inventory.getDialectSelections().size() ).append( '\n' )
				.append( "|Dialects in generated support tables |" ).append( documentedDialects ).append( '\n' )
				.append( "|Short-name and legacy-alias registrations |" ).append( shortNames ).append( '\n' )
				.append( "|Automatic resolution candidates |" ).append( automaticMappings ).append( '\n' )
				.append( "|===\n\n== Dialect surface review groups\n\n" );
		for ( Map.Entry<String, Integer> entry : surfaceReviewGroups.entrySet() ) {
			result.append( "* `" ).append( entry.getKey() ).append( "`: " ).append( entry.getValue() ).append( '\n' );
		}
		result.append( "\n== Current Dialect classifications\n\n" );
		for ( Map.Entry<String, Integer> entry : surfaceCategories.entrySet() ) {
			result.append( "* `" ).append( entry.getKey() ).append( "`: " ).append( entry.getValue() ).append( '\n' );
		}
		result.append( "\n== Community extension-family uses\n\n" );
		for ( Map.Entry<String, Integer> entry : extensionFamilies.entrySet() ) {
			result.append( "* `" ).append( entry.getKey() ).append( "`: " ).append( entry.getValue() ).append( '\n' );
		}
		return result.toString();
	}

	String selectionMatrices(DialectExtensionInventory inventory) {
		final StringBuilder result = new StringBuilder();
		result.append( "= Dialect Selection Evidence\n" )
				.append( ":toc2:\n:toclevels: 2\n\n" )
				.append( "This generated Phase 0 artifact records current selection behavior. " )
				.append( "It does not decide which mechanisms remain supported or whether their registries should be unified.\n\n" )
				.append( "== Selection mechanism matrix\n\n" )
				.append( "[cols=\"18,10,18,18,16,18,18,16\",options=\"header\"]\n|===\n" )
				.append( "|Mechanism |Mode |Trigger |Reference |Extension point |Discovery |Precedence |Audience\n" );
		for ( DialectExtensionInventory.SelectionMechanism mechanism : inventory.getSelectionMechanisms() ) {
			result.append( '|' ).append( cell( mechanism.getId() ) )
					.append( '|' ).append( cell( mechanism.getMode() ) )
					.append( '|' ).append( cell( mechanism.getTrigger() ) )
					.append( '|' ).append( cell( mechanism.getReference() ) )
					.append( '|' ).append( cell( mechanism.getExtensionPoint() ) )
					.append( '|' ).append( cell( mechanism.getDiscovery() ) )
					.append( '|' ).append( cell( mechanism.getPrecedence() ) )
					.append( '|' ).append( cell( mechanism.getAudience() ) ).append( '\n' );
		}
		result.append( "|===\n\n== Concrete Dialect matrix\n\n" )
				.append( "A missing value means only that the corresponding current source contains no registration; " )
				.append( "it is not a proposed compatibility decision.\n\n" )
				.append( "[cols=\"28,15,8,12,16,22,22\",options=\"header\"]\n|===\n" )
				.append( "|Dialect |Artifact |Deprecated |Documentation |Configuration construction |Short names |Automatic-resolution candidates\n" );
		for ( DialectExtensionInventory.DialectSelection dialect : inventory.getDialectSelections() ) {
			result.append( '|' ).append( cell( dialect.getDialectClass() ) )
					.append( '|' ).append( cell( dialect.getArtifact() ) )
					.append( '|' ).append( dialect.isDeprecated() ? "yes" : "no" )
					.append( '|' ).append( cell( values( dialect.getDocumentationSources() ) ) )
					.append( '|' ).append( cell( values( dialect.getConfigurationConstructors() ) ) )
					.append( '|' ).append( cell( registrations( dialect.getShortNames() ) ) )
					.append( '|' ).append( cell( registrations( dialect.getAutomaticResolution() ) ) ).append( '\n' );
		}
		result.append( "|===\n" );
		return result.toString();
	}

	String familyInventory(DialectExtensionInventory inventory) {
		return familyInventory( inventory, null );
	}

	String familyInventory(
			DialectExtensionInventory inventory,
			DialectFamilyDecisionOverlay decisions) {
		if ( decisions != null ) {
			decisions.validate( inventory );
		}
		final StringBuilder result = new StringBuilder();
		result.append( "= Dialect and Translator Family Evidence\n" )
				.append( ":toc2:\n:toclevels: 2\n\n" )
				.append( "This generated Phase 1 artifact records current compiled evidence. " )
				.append( "Family membership represents compatible grammar or copied behavior and does not itself propose inheritance. " )
				.append( "The exposed-overridable count is an estimate of today's subclass-visible method surface, including inherited methods.\n\n" )
				.append( "External-provider survey:: Public subclass examples exist for several families, but no currently maintained external " )
				.append( "subclass was confirmed strongly enough to treat as compatibility evidence. The proposals therefore rely on " )
				.append( "compiled Core, community, and Spatial evidence; the survey is intentionally non-exhaustive.\n\n" );
		for ( DialectExtensionInventory.FamilyCandidate family : inventory.getFamilyCandidates() ) {
			result.append( "== " ).append( family.getTitle() ).append( "\n\n" );
			if ( decisions != null ) {
				appendFamilyDecisions( result, family, decisions );
			}
			result
					.append( "=== Dialect evidence\n\n" )
					.append( "[cols=\"34,16,30,8,10,10\",options=\"header\"]\n|===\n" )
					.append( "|Type |Artifact |Direct superclass |Abstract |Exposed overridable |Declared protected\n" );
			for ( DialectExtensionInventory.FamilyType type : family.getDialectTypes() ) {
				appendFamilyType( result, type );
			}
			result.append( "|===\n\n=== SQL AST translator evidence\n\n" )
					.append( "[cols=\"34,16,30,8,10,10\",options=\"header\"]\n|===\n" )
					.append( "|Type |Artifact |Direct superclass |Abstract |Exposed overridable |Declared protected\n" );
			for ( DialectExtensionInventory.FamilyType type : family.getTranslatorTypes() ) {
				appendFamilyType( result, type );
			}
			result.append( "|===\n\n" )
					.append( "Concrete Dialect dependencies from family translators:: " )
					.append( family.getConcreteDialectDependencies().size() ).append( "\n" );
			appendDependencyExamples( result, family.getConcreteDialectDependencies() );
			result.append( "\nShared declared translator hooks:: " )
					.append( family.getSharedTranslationHooks().size() ).append( "\n" );
			appendHookExamples( result, family.getSharedTranslationHooks() );
			result.append( '\n' );
		}
		return result.toString();
	}

	private static void appendFamilyDecisions(
			StringBuilder result,
			DialectExtensionInventory.FamilyCandidate family,
			DialectFamilyDecisionOverlay decisions) {
		result.append( "=== Proposed dispositions\n\n" )
				.append( "[cols=\"12,12,28,48\",options=\"header\"]\n|===\n" )
				.append( "|Subject |Status |Disposition and contract |Rationale\n" );
		appendFamilyDecision( result, "Dialect", decisions.decision( family.getId() + ":DIALECT" ) );
		appendFamilyDecision( result, "Translator", decisions.decision( family.getId() + ":TRANSLATOR" ) );
		result.append( "|===\n\n" );
	}

	private static void appendFamilyDecision(
			StringBuilder result,
			String subject,
			DialectFamilyDecisionOverlay.Decision decision) {
		result.append( '|' ).append( subject )
				.append( '|' ).append( decision.getDecisionStatus() )
				.append( '|' ).append( cell( decision.getDisposition() + ": " + decision.getProposedContract() ) )
				.append( '|' ).append( cell( decision.getRationale() ) ).append( '\n' );
	}

	private static void appendFamilyType(StringBuilder result, DialectExtensionInventory.FamilyType type) {
		result.append( '|' ).append( cell( type.getClassName() ) )
				.append( '|' ).append( cell( type.getArtifact() ) )
				.append( '|' ).append( cell( type.getDirectSuperClass() == null ? "—" : type.getDirectSuperClass() ) )
				.append( '|' ).append( type.isAbstractType() ? "yes" : "no" )
				.append( '|' ).append( type.getExposedOverridableMethods() )
				.append( '|' ).append( type.getDeclaredProtectedMethods() ).append( '\n' );
	}

	private static void appendDependencyExamples(
			StringBuilder result,
			List<DialectExtensionInventory.FamilyDependency> dependencies) {
		if ( dependencies.isEmpty() ) {
			return;
		}
		result.append( "+\n" );
		for ( int i = 0; i < Math.min( dependencies.size(), 12 ); i++ ) {
			final DialectExtensionInventory.FamilyDependency dependency = dependencies.get( i );
			result.append( "* `" ).append( dependency.getSourceElementId() ).append( "` -> `" )
					.append( dependency.getTargetElementId() ).append( "` (`" )
					.append( dependency.getKind() ).append( "`)\n" );
		}
		if ( dependencies.size() > 12 ) {
			result.append( "* _" ).append( dependencies.size() - 12 ).append( " more_\n" );
		}
	}

	private static void appendHookExamples(
			StringBuilder result,
			List<DialectExtensionInventory.SharedTranslationHook> hooks) {
		if ( hooks.isEmpty() ) {
			return;
		}
		result.append( "+\n" );
		for ( int i = 0; i < Math.min( hooks.size(), 20 ); i++ ) {
			final DialectExtensionInventory.SharedTranslationHook hook = hooks.get( i );
			result.append( "* `" ).append( hook.getSignature() ).append( "` — " )
					.append( hook.getDeclaringTypes().size() ).append( " translators: `" )
					.append( String.join( "`, `", hook.getDeclaringTypes() ) ).append( "`\n" );
		}
		if ( hooks.size() > 20 ) {
			result.append( "* _" ).append( hooks.size() - 20 ).append( " more_\n" );
		}
	}

	private static List<Map<String, Object>> surface(DialectExtensionInventory inventory) {
		final List<Map<String, Object>> result = new ArrayList<>();
		for ( DialectExtensionInventory.SurfaceDeclaration declaration : inventory.getDialectSurface() ) {
			final Map<String, Object> entry = new LinkedHashMap<>();
			entry.put( "elementId", declaration.getElementId() );
			entry.put( "kind", declaration.getKind() );
			entry.put( "signature", declaration.getSignature() );
			entry.put( "visibility", declaration.getVisibility() );
			entry.put( "overridable", declaration.isOverridable() );
			entry.put( "category", declaration.getCategory() );
			entry.put( "roles", declaration.getRoles() );
			entry.put( "artifact", declaration.getArtifact() );
			entry.put( "reviewGroup", declaration.getReviewGroup() );
			entry.put( "signatureReferences", declaration.getSignatureReferences() );
			entry.put( "reachableSignatureElements", declaration.getReachableSignatureElements() );
			result.add( entry );
		}
		return result;
	}

	private static List<Map<String, Object>> relationships(
			List<DialectExtensionInventory.Relationship> relationships) {
		final List<Map<String, Object>> result = new ArrayList<>();
		for ( DialectExtensionInventory.Relationship relationship : relationships ) {
			final Map<String, Object> entry = new LinkedHashMap<>();
			entry.put( "sourceElementId", relationship.getSourceElementId() );
			entry.put( "targetElementId", relationship.getTargetElementId() );
			result.add( entry );
		}
		return result;
	}

	private static List<Map<String, Object>> links(List<BytecodeLinkageAnalyzer.Link> links) {
		final List<Map<String, Object>> result = new ArrayList<>();
		for ( BytecodeLinkageAnalyzer.Link link : links ) {
			final Map<String, Object> entry = new LinkedHashMap<>();
			entry.put( "artifact", link.getArtifact() );
			entry.put( "sourceClass", link.getSourceClass() );
			entry.put( "sourceElementId", link.getSourceElementId() );
			entry.put( "kind", link.getKind() );
			entry.put( "targetElementId", link.getTargetElementId() );
			result.add( entry );
		}
		return result;
	}

	private static List<Map<String, Object>> dependencies(DialectExtensionInventory inventory) {
		final List<Map<String, Object>> result = new ArrayList<>();
		for ( DialectExtensionInventory.Dependency dependency : inventory.getCommunityInternalDependencies() ) {
			final Map<String, Object> entry = new LinkedHashMap<>();
			entry.put( "sourceElementId", dependency.getSourceElementId() );
			entry.put( "targetElementId", dependency.getTargetElementId() );
			entry.put( "classificationElementId", dependency.getClassificationElementId() );
			entry.put( "kind", dependency.getKind() );
			entry.put( "targetCategory", dependency.getTargetCategory() );
			entry.put( "artifact", dependency.getArtifact() );
			result.add( entry );
		}
		return result;
	}

	private static List<Map<String, Object>> extensionUses(DialectExtensionInventory inventory) {
		final List<Map<String, Object>> result = new ArrayList<>();
		for ( DialectExtensionInventory.ExtensionUse use : inventory.getCommunityExtensionUses() ) {
			final Map<String, Object> entry = new LinkedHashMap<>();
			entry.put( "family", use.getFamily() );
			entry.put( "sourceElementId", use.getSourceElementId() );
			entry.put( "targetElementId", use.getTargetElementId() );
			entry.put( "kind", use.getKind() );
			result.add( entry );
		}
		return result;
	}

	private static List<Map<String, Object>> selectionMechanisms(DialectExtensionInventory inventory) {
		final List<Map<String, Object>> result = new ArrayList<>();
		for ( DialectExtensionInventory.SelectionMechanism mechanism : inventory.getSelectionMechanisms() ) {
			final Map<String, Object> entry = new LinkedHashMap<>();
			entry.put( "id", mechanism.getId() );
			entry.put( "mode", mechanism.getMode() );
			entry.put( "trigger", mechanism.getTrigger() );
			entry.put( "reference", mechanism.getReference() );
			entry.put( "extensionPoint", mechanism.getExtensionPoint() );
			entry.put( "discovery", mechanism.getDiscovery() );
			entry.put( "precedence", mechanism.getPrecedence() );
			entry.put( "audience", mechanism.getAudience() );
			entry.put( "evidenceElements", mechanism.getEvidenceElements() );
			result.add( entry );
		}
		return result;
	}

	private static List<Map<String, Object>> dialectSelections(DialectExtensionInventory inventory) {
		final List<Map<String, Object>> result = new ArrayList<>();
		for ( DialectExtensionInventory.DialectSelection dialect : inventory.getDialectSelections() ) {
			final Map<String, Object> entry = new LinkedHashMap<>();
			entry.put( "dialectClass", dialect.getDialectClass() );
			entry.put( "artifact", dialect.getArtifact() );
			entry.put( "deprecated", dialect.isDeprecated() );
			entry.put( "documentationSources", dialect.getDocumentationSources() );
			entry.put( "configurationConstructors", dialect.getConfigurationConstructors() );
			entry.put( "shortNames", registrationsJson( dialect.getShortNames() ) );
			entry.put( "automaticResolution", registrationsJson( dialect.getAutomaticResolution() ) );
			result.add( entry );
		}
		return result;
	}

	private static List<Map<String, Object>> familyCandidates(DialectExtensionInventory inventory) {
		final List<Map<String, Object>> result = new ArrayList<>();
		for ( DialectExtensionInventory.FamilyCandidate family : inventory.getFamilyCandidates() ) {
			final Map<String, Object> entry = new LinkedHashMap<>();
			entry.put( "id", family.getId() );
			entry.put( "title", family.getTitle() );
			entry.put( "dialectTypes", familyTypesJson( family.getDialectTypes() ) );
			entry.put( "translatorTypes", familyTypesJson( family.getTranslatorTypes() ) );
			final List<Map<String, Object>> dependencies = new ArrayList<>();
			for ( DialectExtensionInventory.FamilyDependency dependency : family.getConcreteDialectDependencies() ) {
				final Map<String, Object> dependencyEntry = new LinkedHashMap<>();
				dependencyEntry.put( "sourceElementId", dependency.getSourceElementId() );
				dependencyEntry.put( "targetElementId", dependency.getTargetElementId() );
				dependencyEntry.put( "kind", dependency.getKind() );
				dependencies.add( dependencyEntry );
			}
			entry.put( "concreteDialectDependencies", dependencies );
			final List<Map<String, Object>> hooks = new ArrayList<>();
			for ( DialectExtensionInventory.SharedTranslationHook hook : family.getSharedTranslationHooks() ) {
				final Map<String, Object> hookEntry = new LinkedHashMap<>();
				hookEntry.put( "signature", hook.getSignature() );
				hookEntry.put( "declaringTypes", hook.getDeclaringTypes() );
				hooks.add( hookEntry );
			}
			entry.put( "sharedTranslationHooks", hooks );
			result.add( entry );
		}
		return result;
	}

	private static List<Map<String, Object>> familyTypesJson(List<DialectExtensionInventory.FamilyType> types) {
		final List<Map<String, Object>> result = new ArrayList<>();
		for ( DialectExtensionInventory.FamilyType type : types ) {
			final Map<String, Object> entry = new LinkedHashMap<>();
			entry.put( "className", type.getClassName() );
			entry.put( "artifact", type.getArtifact() );
			entry.put( "directSuperClass", type.getDirectSuperClass() );
			entry.put( "abstract", type.isAbstractType() );
			entry.put( "exposedOverridableMethods", type.getExposedOverridableMethods() );
			entry.put( "declaredProtectedMethods", type.getDeclaredProtectedMethods() );
			result.add( entry );
		}
		return result;
	}

	private static List<Map<String, Object>> registrationsJson(
			List<DialectExtensionInventory.SelectionRegistration> registrations) {
		final List<Map<String, Object>> result = new ArrayList<>();
		for ( DialectExtensionInventory.SelectionRegistration registration : registrations ) {
			final Map<String, Object> entry = new LinkedHashMap<>();
			entry.put( "name", registration.getName() );
			entry.put( "source", registration.getSource() );
			result.add( entry );
		}
		return result;
	}

	private static String registrations(List<DialectExtensionInventory.SelectionRegistration> registrations) {
		if ( registrations.isEmpty() ) {
			return "—";
		}
		final List<String> values = new ArrayList<>();
		for ( DialectExtensionInventory.SelectionRegistration registration : registrations ) {
			values.add( registration.getName() + " (" + simpleName( registration.getSource() ) + ")" );
		}
		return String.join( ", ", values );
	}

	private static String values(List<String> values) {
		return values.isEmpty() ? "—" : String.join( ", ", values );
	}

	private static String cell(String value) {
		return value.replace( "|", "\\|" );
	}

	private static String simpleName(String value) {
		final int member = value.lastIndexOf( '#' );
		final int packageSeparator = value.lastIndexOf( '.', member < 0 ? value.length() : member );
		return value.substring( packageSeparator + 1 );
	}

	private static Map<String, List<DialectExtensionInventory.Relationship>> overridesByTarget(
			DialectExtensionInventory inventory) {
		final Map<String, List<DialectExtensionInventory.Relationship>> result = new HashMap<>();
		for ( DialectExtensionInventory.Relationship relationship : inventory.getOverrides() ) {
			result.computeIfAbsent( relationship.getTargetElementId(), ignored -> new ArrayList<>() ).add( relationship );
		}
		return result;
	}

	private static Map<String, List<BytecodeLinkageAnalyzer.Link>> callsByDialectDeclaration(
			DialectExtensionInventory inventory) {
		final Map<String, String> declarationsBySignature = new HashMap<>();
		for ( DialectExtensionInventory.SurfaceDeclaration declaration : inventory.getDialectSurface() ) {
			if ( declaration.getElementId().startsWith( "method:" ) ) {
				declarationsBySignature.put( memberSignature( declaration.getElementId() ), declaration.getElementId() );
			}
		}
		final Map<String, List<BytecodeLinkageAnalyzer.Link>> result = new HashMap<>();
		for ( BytecodeLinkageAnalyzer.Link call : inventory.getDialectCalls() ) {
			final String declarationId = declarationsBySignature.get( memberSignature( call.getTargetElementId() ) );
			if ( declarationId != null ) {
				result.computeIfAbsent( declarationId, ignored -> new ArrayList<>() ).add( call );
			}
		}
		return result;
	}

	private static Map<String, List<DialectExtensionInventory.SurfaceDeclaration>> reviewGroups(
			DialectExtensionInventory inventory,
			DialectExtensionDecisionOverlay decisions) {
		final Map<String, List<DialectExtensionInventory.SurfaceDeclaration>> result = new LinkedHashMap<>();
		for ( String reviewGroup : List.of(
				"CONSTRUCTION",
				"TRANSLATION",
				"SUPPLIED_STRATEGY",
				"DDL_AND_SCHEMA",
				"TYPE_AND_FUNCTION",
				"GENERAL_CAPABILITY" ) ) {
			result.put( reviewGroup, new ArrayList<>() );
		}
		for ( DialectExtensionInventory.SurfaceDeclaration declaration : inventory.getDialectSurface() ) {
			final String reviewGroup = decisions.decision( declaration ).getReviewGroup();
			result.computeIfAbsent( reviewGroup, ignored -> new ArrayList<>() ).add( declaration );
		}
		result.entrySet().removeIf( (entry) -> entry.getValue().isEmpty() );
		return result;
	}

	private static void appendDeclaration(
			StringBuilder result,
			DialectExtensionInventory.SurfaceDeclaration declaration,
			DialectExtensionDecisionOverlay.Decision decision,
			List<DialectExtensionInventory.Relationship> overrides,
			List<BytecodeLinkageAnalyzer.Link> calls) {
		result.append( "=== `" ).append( declaration.getElementId() ).append( "`\n\n" )
				.append( "Signature:: `" ).append( declaration.getSignature() ).append( "`\n" )
				.append( "Current classification:: `" ).append( declaration.getCategory() ).append( '`' );
		if ( !declaration.getRoles().isEmpty() ) {
			result.append( " with roles `" ).append( String.join( ", ", declaration.getRoles() ) ).append( '`' );
		}
		result.append( "\nVisibility and extension:: `" ).append( declaration.getVisibility() ).append( "`; " )
				.append( declaration.isOverridable() ? "overridable" : "not overridable" ).append( "\n" )
				.append( "Observed usage:: " ).append( overrides.size() ).append( " overrides; " )
				.append( calls.size() ).append( " direct call sites\n" );

		appendExamples( result, "Override examples", relationshipSources( overrides ) );
		appendExamples( result, "Call-site examples", callSources( calls ) );
		appendExamples( result, "Direct signature references", declaration.getSignatureReferences() );
		appendExamples(
				result,
				"Reachable signature elements",
				declaration.getReachableSignatureElements()
		);

		result.append( "\nDecision status:: " ).append( valueOrUndecided( decision.getDecisionStatus() ) )
				.append( "\nProposed classification:: " );
		if ( decision.hasDecision() ) {
			result.append( value( decision.getTargetCategory() ) );
			if ( !decision.getRoles().isBlank() ) {
				result.append( " with roles " ).append( value( decision.getRoles() ) );
			}
		}
		else {
			result.append( "_Undecided_" );
		}
		result.append( "\nDisposition:: " ).append( valueOrUnrecorded( decision.getDisposition() ) )
				.append( "\nRationale:: " ).append( textOrUnrecorded( decision.getRationale() ) )
				.append( "\nReplacement:: " ).append( valueOrUnrecorded( decision.getReplacement() ) )
				.append( "\nOwning phase:: " ).append( valueOrUnrecorded( decision.getOwnerPhase() ) )
				.append( "\nMigration Guide anchor:: " ).append( valueOrUnrecorded( decision.getMigrationGuideAnchor() ) )
				.append( "\nNotes:: " ).append( textOrUnrecorded( decision.getNotes() ) )
				.append( "\n\n" );
	}

	private static void appendExamples(StringBuilder result, String label, Collection<String> values) {
		result.append( label ).append( ":: " );
		if ( values.isEmpty() ) {
			result.append( "_None_\n" );
			return;
		}
		result.append( values.size() ).append( " total\n+\n" );
		int count = 0;
		for ( String value : values ) {
			if ( count++ == 5 ) {
				result.append( "* _" ).append( values.size() - 5 ).append( " more_\n" );
				break;
			}
			result.append( "* `" ).append( value ).append( "`\n" );
		}
	}

	private static Set<String> relationshipSources(List<DialectExtensionInventory.Relationship> relationships) {
		final Set<String> result = new LinkedHashSet<>();
		for ( DialectExtensionInventory.Relationship relationship : relationships ) {
			result.add( relationship.getSourceElementId() );
		}
		return result;
	}

	private static Set<String> callSources(List<BytecodeLinkageAnalyzer.Link> calls) {
		final Set<String> result = new LinkedHashSet<>();
		for ( BytecodeLinkageAnalyzer.Link call : calls ) {
			result.add( call.getSourceElementId() );
		}
		return result;
	}

	private static String memberSignature(String elementId) {
		final int separator = elementId.indexOf( '#' );
		return separator < 0 ? elementId : elementId.substring( separator + 1 );
	}

	private static String title(String reviewGroup) {
		final String normalized = reviewGroup.toLowerCase().replace( '_', ' ' );
		return Character.toUpperCase( normalized.charAt( 0 ) ) + normalized.substring( 1 );
	}

	private static String value(String value) {
		return '`' + value + '`';
	}

	private static String valueOrUnrecorded(String value) {
		return value.isBlank() ? "_Not yet recorded_" : value( value );
	}

	private static String valueOrUndecided(String value) {
		return value.isBlank() ? "_Undecided_" : value( value );
	}

	private static String textOrUnrecorded(String value) {
		return value.isBlank() ? "_Not yet recorded_" : value;
	}
}
