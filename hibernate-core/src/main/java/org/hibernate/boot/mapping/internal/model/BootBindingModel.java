/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.boot.mapping.internal.view.CollationContributionView;
import org.hibernate.boot.mapping.internal.view.EmbeddableContributionView;
import org.hibernate.boot.mapping.internal.view.EntityHierarchyView;
import org.hibernate.boot.mapping.internal.view.EntityView;
import org.hibernate.boot.mapping.internal.view.EntityIdentifierBindingView;
import org.hibernate.boot.mapping.internal.view.MappedSuperclassContributionView;
import org.hibernate.boot.mapping.internal.view.NaturalIdContributionView;
import org.hibernate.boot.mapping.internal.view.TenantIdBindingView;
import org.hibernate.boot.mapping.internal.view.VersionBindingView;
import org.hibernate.boot.models.AttributeNature;
import org.hibernate.boot.mapping.internal.categorize.EntityTypeMetadata;
import org.hibernate.boot.mapping.internal.categorize.IdentifiableTypeMetadata;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.MemberDetails;

import jakarta.annotation.Nullable;
import jakarta.persistence.AccessType;
import jakarta.persistence.Embeddable;
import jakarta.persistence.MappedSuperclass;

/// Aggregate root for the horizontal boot binding model.
///
/// The model collects the source facts and resolved boot-time interpretation for
/// the managed types known to a persistence unit.  It is populated from
/// categorized Java/XML sources and is organized around the domain declarations
/// themselves: managed types, attributes, identifiers, contributions, and the
/// ordering/correspondence facts needed by later boot phases.
///
/// @since 9.0
/// @author Steve Ebersole
public class BootBindingModel {
	private final Map<ClassDetails, ManagedTypeBinding> managedTypeBindings = new LinkedHashMap<>();
	private final Map<EntityTypeMetadata, EntityHierarchyBinding> entityHierarchyBindings = new LinkedHashMap<>();
	private final Map<EntityTypeMetadata, EntityIdentifierBinding> entityIdentifierBindings = new LinkedHashMap<>();
	private final Map<EntityTypeMetadata, VersionBinding> versionBindings = new LinkedHashMap<>();
	private final Map<EntityTypeMetadata, TenantIdBinding> tenantIdBindings = new LinkedHashMap<>();
	private final List<NaturalIdContribution> naturalIdContributions = new ArrayList<>();
	private final List<CollationContribution> collationContributions = new ArrayList<>();
	private final List<MappedSuperclassContribution> mappedSuperclassContributions = new ArrayList<>();
	private final List<EmbeddableContribution> embeddableContributions = new ArrayList<>();

	public void addManagedTypeBinding(ManagedTypeBinding binding) {
		managedTypeBindings.put( binding.classDetails(), binding );
	}

	public ManagedTypeBinding getManagedTypeBinding(ClassDetails classDetails) {
		return managedTypeBindings.get( classDetails );
	}

	public Collection<ManagedTypeBinding> managedTypeBindings() {
		return managedTypeBindings.values();
	}

	public void addEntityHierarchyBinding(EntityTypeMetadata rootType, EntityHierarchyBinding binding) {
		entityHierarchyBindings.put( rootType, binding );
	}

	public @Nullable EntityHierarchyBinding getEntityHierarchyBinding(EntityTypeMetadata rootType) {
		return entityHierarchyBindings.get( rootType );
	}

	public Collection<EntityHierarchyBinding> entityHierarchyBindings() {
		return entityHierarchyBindings.values();
	}

	public List<EntityHierarchyView> entityHierarchyViews() {
		return entityHierarchyBindings.values()
				.stream()
				.map( EntityHierarchyView::new )
				.toList();
	}

	public @Nullable EntityHierarchyView getEntityHierarchyView(EntityTypeMetadata rootType) {
		final EntityHierarchyBinding binding = getEntityHierarchyBinding( rootType );
		return binding == null ? null : new EntityHierarchyView( binding );
	}

	public @Nullable AttributeDeclarationBinding findAttributeDeclaration(
			ClassDetails declaringType,
			String attributeName) {
		final ManagedTypeBinding managedTypeBinding = getManagedTypeBinding( declaringType );
		if ( managedTypeBinding == null ) {
			return null;
		}
		for ( AttributeDeclarationBinding declarationBinding : managedTypeBinding.declaredAttributes() ) {
			if ( declarationBinding.attributeName().equals( attributeName ) ) {
				return declarationBinding;
			}
		}
		return null;
	}

	public AttributeDeclarationBinding findOrCreateAttributeDeclaration(
			ClassDetails declaringType,
			MemberDetails member,
			AccessType accessType,
			AttributeNature nature) {
		final String attributeName = member.resolveAttributeName();
		final AttributeDeclarationBinding existing = findAttributeDeclaration( declaringType, attributeName );
		if ( existing != null ) {
			return existing;
		}

		ManagedTypeBinding managedTypeBinding = getManagedTypeBinding( declaringType );
		if ( managedTypeBinding == null ) {
			managedTypeBinding = createManagedTypeBinding( declaringType, accessType );
			addManagedTypeBinding( managedTypeBinding );
		}
		final AttributeDeclarationBinding declarationBinding = createAttributeDeclaration(
				attributeName,
				managedTypeBinding,
				member,
				accessType,
				nature
		);
		managedTypeBinding.addDeclaredAttribute( declarationBinding );
		return declarationBinding;
	}

	private AttributeDeclarationBinding createAttributeDeclaration(
			String attributeName,
			ManagedTypeBinding managedTypeBinding,
			MemberDetails member,
			AccessType accessType,
			AttributeNature nature) {
		if ( managedTypeBinding.kind() == ManagedTypeBinding.Kind.EMBEDDABLE ) {
			return new EmbeddableAttributeDeclarationBinding(
					attributeName,
					managedTypeBinding,
					member,
					accessType,
					nature
			);
		}
		return new IdentifiableAttributeDeclarationBinding(
				attributeName,
				null,
				managedTypeBinding,
				managedTypeBinding,
				member,
				accessType,
				nature,
				managedTypeBinding.classDetails().getName() + "." + attributeName,
				attributeName
		);
	}

	private ManagedTypeBinding createManagedTypeBinding(ClassDetails classDetails, AccessType accessType) {
		if ( classDetails.hasDirectAnnotationUsage( Embeddable.class ) ) {
			return new EmbeddableTypeBinding( classDetails, accessType );
		}
		if ( classDetails.hasDirectAnnotationUsage( MappedSuperclass.class ) ) {
			return new MappedSuperclassTypeBinding( classDetails, accessType );
		}
		return new ManagedTypeBinding( classDetails, ManagedTypeBinding.Kind.MAPPED_SUPERCLASS, accessType );
	}

	public void addEntityIdentifierBinding(EntityTypeMetadata rootType, EntityIdentifierBinding entityIdentifierBinding) {
		entityIdentifierBindings.put( rootType, entityIdentifierBinding );
	}

	public @Nullable EntityIdentifierBinding getEntityIdentifierBinding(EntityTypeMetadata rootType) {
		return entityIdentifierBindings.get( rootType );
	}

	public Collection<EntityIdentifierBinding> entityIdentifierBindings() {
		return entityIdentifierBindings.values();
	}

	public List<EntityIdentifierBindingView> entityIdentifierBindingViews() {
		return entityIdentifierBindings.values()
				.stream()
				.map( EntityIdentifierBindingView::new )
				.toList();
	}

	public @Nullable EntityIdentifierBindingView getEntityIdentifierBindingView(EntityTypeMetadata rootType) {
		final EntityIdentifierBinding binding = getEntityIdentifierBinding( rootType );
		return binding == null ? null : new EntityIdentifierBindingView( binding );
	}

	public @Nullable EntityView getEntityView(EntityTypeMetadata rootType) {
		final ManagedTypeBinding binding = getManagedTypeBinding( rootType.getClassDetails() );
		if ( !( binding instanceof EntityTypeBinding entityBinding ) ) {
			return null;
		}
		return new EntityView( entityBinding, getEntityIdentifierBinding( rootType ) );
	}

	public void addVersionBinding(EntityTypeMetadata rootType, VersionBinding versionBinding) {
		versionBindings.put( rootType, versionBinding );
	}

	public @Nullable VersionBinding getVersionBinding(EntityTypeMetadata rootType) {
		return versionBindings.get( rootType );
	}

	public Collection<VersionBinding> versionBindings() {
		return versionBindings.values();
	}

	public List<VersionBindingView> versionBindingViews() {
		return versionBindings.values()
				.stream()
				.map( VersionBindingView::new )
				.toList();
	}

	public @Nullable VersionBindingView getVersionBindingView(EntityTypeMetadata rootType) {
		final VersionBinding binding = getVersionBinding( rootType );
		return binding == null ? null : new VersionBindingView( binding );
	}

	public void addTenantIdBinding(EntityTypeMetadata rootType, TenantIdBinding tenantIdBinding) {
		tenantIdBindings.put( rootType, tenantIdBinding );
	}

	public @Nullable TenantIdBinding getTenantIdBinding(EntityTypeMetadata rootType) {
		return tenantIdBindings.get( rootType );
	}

	public @Nullable TenantIdBindingView getTenantIdBindingView(EntityTypeMetadata rootType) {
		final TenantIdBinding binding = getTenantIdBinding( rootType );
		return binding == null ? null : new TenantIdBindingView( binding );
	}

	public void addNaturalIdContribution(NaturalIdContribution contribution) {
		naturalIdContributions.add( contribution );
	}

	public List<NaturalIdContribution> naturalIdContributions() {
		return List.copyOf( naturalIdContributions );
	}

	public @Nullable NaturalIdContributionView getNaturalIdContributionView(
			IdentifiableTypeMetadata owner,
			String attributeName) {
		final NaturalIdContribution contribution = getNaturalIdContribution( owner, attributeName );
		return contribution == null ? null : new NaturalIdContributionView( contribution );
	}

	private @Nullable NaturalIdContribution getNaturalIdContribution(
			IdentifiableTypeMetadata owner,
			String attributeName) {
		for ( NaturalIdContribution contribution : naturalIdContributions ) {
			if ( contribution.owner() == owner && contribution.attributeName().equals( attributeName ) ) {
				return contribution;
			}
		}
		return null;
	}

	public void addCollationContribution(CollationContribution contribution) {
		collationContributions.add( contribution );
	}

	public List<CollationContribution> collationContributions() {
		return List.copyOf( collationContributions );
	}

	public @Nullable CollationContributionView getCollationContributionView(
			IdentifiableTypeMetadata owner,
			String attributePath) {
		final CollationContribution contribution = getCollationContribution( owner, attributePath );
		return contribution == null ? null : new CollationContributionView( contribution );
	}

	private @Nullable CollationContribution getCollationContribution(
			IdentifiableTypeMetadata owner,
			String attributePath) {
		for ( CollationContribution contribution : collationContributions ) {
			if ( contribution.owner() == owner && contribution.attributePath().equals( attributePath ) ) {
				return contribution;
			}
		}
		return null;
	}

	public void addMappedSuperclassContribution(MappedSuperclassContribution contribution) {
		mappedSuperclassContributions.add( contribution );
	}

	public List<MappedSuperclassContribution> mappedSuperclassContributions() {
		return List.copyOf( mappedSuperclassContributions );
	}

	public List<MappedSuperclassContributionView> mappedSuperclassContributionViews(IdentifiableTypeMetadata consumer) {
		final ArrayList<MappedSuperclassContributionView> result = new ArrayList<>();
		for ( MappedSuperclassContribution contribution : mappedSuperclassContributions ) {
			if ( contribution.consumer() == consumer ) {
				result.add( new MappedSuperclassContributionView( contribution ) );
			}
		}
		return result;
	}

	public void addEmbeddableContribution(EmbeddableContribution contribution) {
		embeddableContributions.add( contribution );
	}

	public List<EmbeddableContribution> embeddableContributions() {
		return List.copyOf( embeddableContributions );
	}

	public EmbeddableContributionView embeddableContributionView(EmbeddableContribution contribution) {
		return new EmbeddableContributionView( contribution );
	}
}
