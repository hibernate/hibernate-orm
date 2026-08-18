/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.post;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/// Immutable generated facts used to review the Dialect extension migration.
/// This model is deliberately separate from reviewed dispositions: regenerating
/// facts must never overwrite human decisions.
///
/// @author Steve Ebersole
final class DialectExtensionInventory {
	static final String SCHEMA = "hibernate-dialect-extension-inventory";
	static final int SCHEMA_VERSION = 2;

	private final List<SurfaceDeclaration> dialectSurface;
	private final List<Relationship> overrides;
	private final List<Relationship> dialectHierarchy;
	private final List<BytecodeLinkageAnalyzer.Link> dialectCalls;
	private final List<Dependency> communityInternalDependencies;
	private final List<ExtensionUse> communityExtensionUses;
	private final List<String> selectionMetadataElements;
	private final List<SelectionMechanism> selectionMechanisms;
	private final List<DialectSelection> dialectSelections;

	DialectExtensionInventory(
			Collection<SurfaceDeclaration> dialectSurface,
			Collection<Relationship> overrides,
			Collection<Relationship> dialectHierarchy,
			Collection<BytecodeLinkageAnalyzer.Link> dialectCalls,
			Collection<Dependency> communityInternalDependencies,
			Collection<ExtensionUse> communityExtensionUses,
			Collection<String> selectionMetadataElements,
			Collection<SelectionMechanism> selectionMechanisms,
			Collection<DialectSelection> dialectSelections) {
		this.dialectSurface = immutable( dialectSurface, Comparator.comparing( SurfaceDeclaration::getElementId ) );
		this.overrides = immutable( overrides, Relationship.ORDER );
		this.dialectHierarchy = immutable( dialectHierarchy, Relationship.ORDER );
		this.dialectCalls = immutable( dialectCalls, Comparator.naturalOrder() );
		this.communityInternalDependencies = immutable(
				communityInternalDependencies,
				Comparator.comparing( Dependency::getSourceElementId )
						.thenComparing( Dependency::getTargetElementId )
						.thenComparing( Dependency::getKind )
		);
		this.communityExtensionUses = immutable(
				communityExtensionUses,
				Comparator.comparing( ExtensionUse::getFamily )
						.thenComparing( ExtensionUse::getSourceElementId )
						.thenComparing( ExtensionUse::getTargetElementId )
		);
		final List<String> selection = new ArrayList<>( selectionMetadataElements );
		Collections.sort( selection );
		this.selectionMetadataElements = Collections.unmodifiableList( selection );
		this.selectionMechanisms = immutable( selectionMechanisms, Comparator.comparing( SelectionMechanism::getId ) );
		this.dialectSelections = immutable( dialectSelections, Comparator.comparing( DialectSelection::getDialectClass ) );
	}

	List<SurfaceDeclaration> getDialectSurface() {
		return dialectSurface;
	}

	List<Relationship> getOverrides() {
		return overrides;
	}

	List<Relationship> getDialectHierarchy() {
		return dialectHierarchy;
	}

	List<BytecodeLinkageAnalyzer.Link> getDialectCalls() {
		return dialectCalls;
	}

	List<Dependency> getCommunityInternalDependencies() {
		return communityInternalDependencies;
	}

	List<ExtensionUse> getCommunityExtensionUses() {
		return communityExtensionUses;
	}

	List<String> getSelectionMetadataElements() {
		return selectionMetadataElements;
	}

	List<SelectionMechanism> getSelectionMechanisms() {
		return selectionMechanisms;
	}

	List<DialectSelection> getDialectSelections() {
		return dialectSelections;
	}

	private static <T> List<T> immutable(Collection<T> values, Comparator<? super T> comparator) {
		final List<T> copy = new ArrayList<>( values );
		copy.sort( comparator );
		return Collections.unmodifiableList( copy );
	}

	static final class SurfaceDeclaration {
		private final String elementId;
		private final String kind;
		private final String signature;
		private final String visibility;
		private final boolean overridable;
		private final String category;
		private final List<String> roles;
		private final String artifact;
		private final String reviewGroup;
		private final List<String> signatureReferences;
		private final List<String> reachableSignatureElements;

		SurfaceDeclaration(
				ClassificationModel.Element element,
				String reviewGroup,
				Collection<String> reachableSignatureElements) {
			elementId = element.getId();
			kind = element.getKind().name();
			signature = element.getSignature();
			visibility = element.getStructure().isPublic() ? "PUBLIC" : "PROTECTED";
			overridable = element.getKind() == ClassificationModel.ElementKind.METHOD
					&& element.getStructure().isOverridableMethod();
			category = element.getCategory() == null ? element.getClassificationStatus().name() : element.getCategory().name();
			roles = names( element.getEffectiveRoles() );
			artifact = element.getArtifact();
			this.reviewGroup = reviewGroup;
			final List<String> references = new ArrayList<>();
			for ( ClassificationModel.Reference reference : element.getReferences() ) {
				references.add( reference.getKind() + "->" + reference.getTargetElementId() );
			}
			Collections.sort( references );
			signatureReferences = Collections.unmodifiableList( references );
			final List<String> reachable = new ArrayList<>( reachableSignatureElements );
			Collections.sort( reachable );
			this.reachableSignatureElements = Collections.unmodifiableList( reachable );
		}

		String getElementId() {
			return elementId;
		}

		String getKind() {
			return kind;
		}

		String getSignature() {
			return signature;
		}

		String getVisibility() {
			return visibility;
		}

		boolean isOverridable() {
			return overridable;
		}

		String getCategory() {
			return category;
		}

		List<String> getRoles() {
			return roles;
		}

		String getArtifact() {
			return artifact;
		}

		String getReviewGroup() {
			return reviewGroup;
		}

		List<String> getSignatureReferences() {
			return signatureReferences;
		}

		List<String> getReachableSignatureElements() {
			return reachableSignatureElements;
		}
	}

	static final class Relationship {
		private static final Comparator<Relationship> ORDER = Comparator
				.comparing( Relationship::getSourceElementId )
				.thenComparing( Relationship::getTargetElementId );

		private final String sourceElementId;
		private final String targetElementId;

		Relationship(String sourceElementId, String targetElementId) {
			this.sourceElementId = sourceElementId;
			this.targetElementId = targetElementId;
		}

		String getSourceElementId() {
			return sourceElementId;
		}

		String getTargetElementId() {
			return targetElementId;
		}
	}

	static final class Dependency {
		private final String sourceElementId;
		private final String targetElementId;
		private final String classificationElementId;
		private final String kind;
		private final String targetCategory;
		private final String artifact;

		Dependency(BytecodeLinkageAnalyzer.Link link, String targetElementId, String targetCategory) {
			sourceElementId = link.getSourceElementId();
			this.targetElementId = link.getTargetElementId();
			classificationElementId = targetElementId;
			kind = link.getKind();
			this.targetCategory = targetCategory;
			artifact = link.getArtifact();
		}

		String getSourceElementId() {
			return sourceElementId;
		}

		String getTargetElementId() {
			return targetElementId;
		}

		String getKind() {
			return kind;
		}

		String getClassificationElementId() {
			return classificationElementId;
		}

		String getTargetCategory() {
			return targetCategory;
		}

		String getArtifact() {
			return artifact;
		}
	}

	static final class ExtensionUse {
		private final String family;
		private final String sourceElementId;
		private final String targetElementId;
		private final String kind;

		ExtensionUse(String family, BytecodeLinkageAnalyzer.Link link) {
			this.family = family;
			sourceElementId = link.getSourceElementId();
			targetElementId = link.getTargetElementId();
			kind = link.getKind();
		}

		String getFamily() {
			return family;
		}

		String getSourceElementId() {
			return sourceElementId;
		}

		String getTargetElementId() {
			return targetElementId;
		}

		String getKind() {
			return kind;
		}
	}

	static final class SelectionMechanism {
		private final String id;
		private final String mode;
		private final String trigger;
		private final String reference;
		private final String extensionPoint;
		private final String discovery;
		private final String precedence;
		private final String audience;
		private final List<String> evidenceElements;

		SelectionMechanism(
				String id,
				String mode,
				String trigger,
				String reference,
				String extensionPoint,
				String discovery,
				String precedence,
				String audience,
				Collection<String> evidenceElements) {
			this.id = id;
			this.mode = mode;
			this.trigger = trigger;
			this.reference = reference;
			this.extensionPoint = extensionPoint;
			this.discovery = discovery;
			this.precedence = precedence;
			this.audience = audience;
			final List<String> evidence = new ArrayList<>( evidenceElements );
			Collections.sort( evidence );
			this.evidenceElements = Collections.unmodifiableList( evidence );
		}

		String getId() {
			return id;
		}

		String getMode() {
			return mode;
		}

		String getTrigger() {
			return trigger;
		}

		String getReference() {
			return reference;
		}

		String getExtensionPoint() {
			return extensionPoint;
		}

		String getDiscovery() {
			return discovery;
		}

		String getPrecedence() {
			return precedence;
		}

		String getAudience() {
			return audience;
		}

		List<String> getEvidenceElements() {
			return evidenceElements;
		}
	}

	static final class DialectSelection {
		private final String dialectClass;
		private final String artifact;
		private final boolean deprecated;
		private final List<String> documentationSources;
		private final List<String> configurationConstructors;
		private final List<SelectionRegistration> shortNames;
		private final List<SelectionRegistration> automaticResolution;

		DialectSelection(
				String dialectClass,
				String artifact,
				boolean deprecated,
				Collection<String> documentationSources,
				Collection<String> configurationConstructors,
				Collection<SelectionRegistration> shortNames,
				Collection<SelectionRegistration> automaticResolution) {
			this.dialectClass = dialectClass;
			this.artifact = artifact;
			this.deprecated = deprecated;
			final List<String> documentation = new ArrayList<>( documentationSources );
			Collections.sort( documentation );
			this.documentationSources = Collections.unmodifiableList( documentation );
			final List<String> constructors = new ArrayList<>( configurationConstructors );
			Collections.sort( constructors );
			this.configurationConstructors = Collections.unmodifiableList( constructors );
			this.shortNames = immutable( shortNames, SelectionRegistration.ORDER );
			this.automaticResolution = immutable( automaticResolution, SelectionRegistration.ORDER );
		}

		String getDialectClass() {
			return dialectClass;
		}

		String getArtifact() {
			return artifact;
		}

		boolean isDeprecated() {
			return deprecated;
		}

		List<String> getDocumentationSources() {
			return documentationSources;
		}

		List<String> getConfigurationConstructors() {
			return configurationConstructors;
		}

		List<SelectionRegistration> getShortNames() {
			return shortNames;
		}

		List<SelectionRegistration> getAutomaticResolution() {
			return automaticResolution;
		}
	}

	static final class SelectionRegistration {
		private static final Comparator<SelectionRegistration> ORDER = Comparator
				.comparing( SelectionRegistration::getName )
				.thenComparing( SelectionRegistration::getSource );

		private final String name;
		private final String source;

		SelectionRegistration(String name, String source) {
			this.name = name;
			this.source = source;
		}

		String getName() {
			return name;
		}

		String getSource() {
			return source;
		}
	}

	private static List<String> names(Collection<? extends Enum<?>> values) {
		final List<String> names = new ArrayList<>();
		for ( Enum<?> value : values ) {
			names.add( value.name() );
		}
		Collections.sort( names );
		return Collections.unmodifiableList( names );
	}
}
