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
	private final Map<String, EntityIdentifierBinding> entityIdentifierBindingsByEntityName = new LinkedHashMap<>();
	private final Map<EntityTypeMetadata, VersionBinding> versionBindings = new LinkedHashMap<>();
	private final Map<EntityTypeMetadata, TenantIdBinding> tenantIdBindings = new LinkedHashMap<>();
	private final List<NaturalIdContribution> naturalIdContributions = new ArrayList<>();
	private final List<CollationContribution> collationContributions = new ArrayList<>();
	private final List<MappedSuperclassContribution> mappedSuperclassContributions = new ArrayList<>();
	private final Map<String, Map<String, AttributeUsageBinding>> appliedMappedSuperclassAttributeUsages =
			new LinkedHashMap<>();
	private final List<EmbeddableContribution> embeddableContributions = new ArrayList<>();
	private final Map<Object, EmbeddableContribution> embeddableContributionsByComponent = new LinkedHashMap<>();

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
		indexEntityIdentifierBinding( rootType.getEntityName(), entityIdentifierBinding );
		indexEntityIdentifierBinding( rootType.getJpaEntityName(), entityIdentifierBinding );
		indexEntityIdentifierBinding( rootType.getClassDetails().getName(), entityIdentifierBinding );
		indexEntityIdentifierBinding( rootType.getClassDetails().getClassName(), entityIdentifierBinding );
	}

	public @Nullable EntityIdentifierBinding getEntityIdentifierBinding(EntityTypeMetadata rootType) {
		return entityIdentifierBindings.get( rootType );
	}

	public @Nullable EntityIdentifierBinding findEntityIdentifierBinding(String entityName) {
		return entityIdentifierBindingsByEntityName.get( entityName );
	}

	private void indexEntityIdentifierBinding(String entityName, EntityIdentifierBinding entityIdentifierBinding) {
		if ( entityName != null ) {
			entityIdentifierBindingsByEntityName.put( entityName, entityIdentifierBinding );
		}
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

	/**
	 * Registers a mapped-superclass contribution.
	 * <p>
	 * The contribution owns the ordered provenance/list state.  This aggregate
	 * root owns indexes used to resolve applied usage metadata by legacy runtime
	 * handoff inputs, so any usages already present on the contribution are
	 * indexed as part of registration.
	 */
	public void addMappedSuperclassContribution(MappedSuperclassContribution contribution) {
		mappedSuperclassContributions.add( contribution );
		contribution.appliedAttributeUsages()
				.forEach( (attributeUsage) -> indexAppliedMappedSuperclassAttributeUsage(
						contribution,
						attributeUsage
				) );
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

	/**
	 * Registers an applied mapped-superclass attribute usage and updates the
	 * lookup index.
	 * <p>
	 * Callers should use this method instead of mutating
	 * {@link MappedSuperclassContribution} directly so that lookup by nearest
	 * entity consumer name remains consistent.
	 */
	public AttributeUsageBinding addAppliedMappedSuperclassAttributeUsage(
			MappedSuperclassContribution contribution,
			AttributeUsageBinding attributeUsage) {
		final var addedUsage = contribution.addAppliedAttributeUsage( attributeUsage );
		indexAppliedMappedSuperclassAttributeUsage( contribution, addedUsage );
		return addedUsage;
	}

	/**
	 * Finds an applied mapped-superclass attribute usage by nearest concrete
	 * entity consumer name and attribute name.
	 * <p>
	 * The index accepts entity-name, JPA entity-name, {@code ClassDetails#getName()}
	 * and {@code ClassDetails#getClassName()} forms for the nearest entity
	 * consumer, matching the compatibility behavior of the previous scan-based
	 * lookup.
	 */
	public @Nullable AttributeUsageBinding findAppliedMappedSuperclassAttributeUsage(
			String nearestEntityConsumerName,
			String attributeName) {
		final var entityUsages = appliedMappedSuperclassAttributeUsages.get( nearestEntityConsumerName );
		return entityUsages == null ? null : entityUsages.get( attributeName );
	}

	private void indexAppliedMappedSuperclassAttributeUsage(
			MappedSuperclassContribution contribution,
			AttributeUsageBinding attributeUsage) {
		final var nearestEntityConsumer = contribution.nearestEntityConsumer();
		indexAppliedMappedSuperclassAttributeUsage( nearestEntityConsumer.getEntityName(), attributeUsage );
		indexAppliedMappedSuperclassAttributeUsage( nearestEntityConsumer.getJpaEntityName(), attributeUsage );
		final var classDetails = contribution.nearestEntityConsumer().getClassDetails();
		indexAppliedMappedSuperclassAttributeUsage( classDetails.getName(), attributeUsage );
		indexAppliedMappedSuperclassAttributeUsage( classDetails.getClassName(), attributeUsage );
	}

	private void indexAppliedMappedSuperclassAttributeUsage(
			String nearestEntityConsumerName,
			AttributeUsageBinding attributeUsage) {
		if ( nearestEntityConsumerName == null ) {
			return;
		}
		appliedMappedSuperclassAttributeUsages.computeIfAbsent(
				nearestEntityConsumerName,
				(name) -> new LinkedHashMap<>()
		).putIfAbsent( attributeUsage.attributeName(), attributeUsage );
	}

	public void addEmbeddableContribution(EmbeddableContribution contribution) {
		embeddableContributions.add( contribution );
	}

	public void addEmbeddableComponentHandoff(EmbeddableContributionView contribution, Object component) {
		embeddableContributionsByComponent.put( component, contribution.contribution() );
	}

	public List<EmbeddableContribution> embeddableContributions() {
		return List.copyOf( embeddableContributions );
	}

	public EmbeddableContributionView embeddableContributionView(EmbeddableContribution contribution) {
		return new EmbeddableContributionView( contribution );
	}

	public @Nullable EmbeddableContributionView findEmbeddableContribution(Object component) {
		final EmbeddableContribution contribution = embeddableContributionsByComponent.get( component );
		return contribution == null ? null : embeddableContributionView( contribution );
	}

	public @Nullable ComponentMemberBinding findEmbeddableMemberBinding(Object component, String attributeName) {
		final EmbeddableContribution contribution = embeddableContributionsByComponent.get( component );
		if ( contribution == null ) {
			return null;
		}
		for ( ComponentMemberBinding member : contribution.members() ) {
			if ( member.attributeName().equals( attributeName ) ) {
				return member;
			}
		}
		return null;
	}
}
