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

import org.hibernate.boot.mapping.internal.jpa.JpaStaticMetamodelInjectionSource;
import org.hibernate.boot.mapping.internal.view.CollationContributionView;
import org.hibernate.boot.mapping.internal.view.EntityHierarchyView;
import org.hibernate.boot.mapping.internal.view.EntityView;
import org.hibernate.boot.mapping.internal.view.EntityIdentifierBindingView;
import org.hibernate.boot.mapping.internal.view.NaturalIdContributionView;
import org.hibernate.boot.mapping.internal.view.TenantIdBindingView;
import org.hibernate.boot.mapping.internal.view.VersionBindingView;
import org.hibernate.boot.models.AttributeNature;
import org.hibernate.boot.mapping.internal.categorize.EntityTypeMetadataImpl;
import org.hibernate.boot.mapping.internal.categorize.AbstractIdentifiableTypeMetadata;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.boot.mapping.spi.MappingRole;

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
	private final JpaStaticMetamodelInjectionSource.Builder staticMetamodelInjectionPlan =
			new JpaStaticMetamodelInjectionSource.Builder();
	private final Map<ClassDetails, ManagedTypeBinding> managedTypeBindings = new LinkedHashMap<>();
	private final Map<EntityTypeMetadataImpl, EntityHierarchyBinding> entityHierarchyBindings = new LinkedHashMap<>();
	private final Map<EntityTypeMetadataImpl, EntityIdentifierBinding> entityIdentifierBindings = new LinkedHashMap<>();
	private final Map<String, EntityIdentifierBinding> entityIdentifierBindingsByEntityName = new LinkedHashMap<>();
	private final Map<EntityTypeMetadataImpl, VersionBinding> versionBindings = new LinkedHashMap<>();
	private final Map<EntityTypeMetadataImpl, TenantIdBinding> tenantIdBindings = new LinkedHashMap<>();
	private final List<NaturalIdContribution> naturalIdContributions = new ArrayList<>();
	private final List<CollationContribution> collationContributions = new ArrayList<>();
	private final List<MappedSuperclassContribution> mappedSuperclassContributions = new ArrayList<>();
	private final Map<MappingRole, AppliedAttributeMapping> appliedAttributeMappings = new LinkedHashMap<>();
	private final Map<MappingRole, AppliedEmbeddableMapping> appliedEmbeddableMappings = new LinkedHashMap<>();
	private final List<EmbeddableContribution> embeddableContributions = new ArrayList<>();

	public void addManagedTypeBinding(ManagedTypeBinding binding) {
		managedTypeBindings.put( binding.classDetails(), binding );
		staticMetamodelInjectionPlan.addManagedType( binding );
	}

	public ManagedTypeBinding getManagedTypeBinding(ClassDetails classDetails) {
		return managedTypeBindings.get( classDetails );
	}

	public Collection<ManagedTypeBinding> managedTypeBindings() {
		return managedTypeBindings.values();
	}

	public void addDeclaredAttribute(
			ManagedTypeBinding declaringType,
			AttributeDeclarationBinding attributeDeclaration) {
		declaringType.addDeclaredAttribute( attributeDeclaration );
		staticMetamodelInjectionPlan.addDeclaredAttribute(
				declaringType,
				attributeDeclaration.attributeName()
		);
	}

	/// Registers one concrete attribute application by its intrinsic mapping
	/// role.
	public void addAppliedAttributeMapping(AppliedAttributeMapping appliedMapping) {
		final AppliedAttributeMapping previous = appliedAttributeMappings.putIfAbsent(
				appliedMapping.role(),
				appliedMapping
		);
		if ( previous != null && previous != appliedMapping ) {
			throw new IllegalStateException( "Duplicate applied attribute mapping role: " + appliedMapping.role() );
		}
	}

	public @Nullable AppliedAttributeMapping getAppliedAttributeMapping(MappingRole role) {
		return appliedAttributeMappings.get( role );
	}

	public Collection<AppliedAttributeMapping> appliedAttributeMappings() {
		return List.copyOf( appliedAttributeMappings.values() );
	}

	/// Registers one concrete embeddable application by its intrinsic mapping
	/// role.
	public void addAppliedEmbeddableMapping(AppliedEmbeddableMapping appliedMapping) {
		final AppliedEmbeddableMapping previous = appliedEmbeddableMappings.putIfAbsent(
				appliedMapping.role(),
				appliedMapping
		);
		if ( previous != null && previous != appliedMapping ) {
			throw new IllegalStateException( "Duplicate applied embeddable mapping role: " + appliedMapping.role() );
		}
		for ( AppliedAttributeMapping attribute : appliedMapping.attributes() ) {
			staticMetamodelInjectionPlan.addConcreteGenericAttribute(
					appliedMapping.componentType(),
					attribute
			);
		}
	}

	public @Nullable AppliedEmbeddableMapping getAppliedEmbeddableMapping(MappingRole role) {
		return appliedEmbeddableMappings.get( role );
	}

	public Collection<AppliedEmbeddableMapping> appliedEmbeddableMappings() {
		return List.copyOf( appliedEmbeddableMappings.values() );
	}

	public void addEntityHierarchyBinding(EntityTypeMetadataImpl rootType, EntityHierarchyBinding binding) {
		entityHierarchyBindings.put( rootType, binding );
		staticMetamodelInjectionPlan.addHierarchy( binding );
	}

	public @Nullable EntityHierarchyBinding getEntityHierarchyBinding(EntityTypeMetadataImpl rootType) {
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

	public @Nullable EntityHierarchyView getEntityHierarchyView(EntityTypeMetadataImpl rootType) {
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
		addDeclaredAttribute( managedTypeBinding, declarationBinding );
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

	public void addEntityIdentifierBinding(EntityTypeMetadataImpl rootType, EntityIdentifierBinding entityIdentifierBinding) {
		entityIdentifierBindings.put( rootType, entityIdentifierBinding );
		staticMetamodelInjectionPlan.addIdentifier( rootType.getClassDetails(), entityIdentifierBinding );
		indexEntityIdentifierBinding( rootType.getEntityName(), entityIdentifierBinding );
		indexEntityIdentifierBinding( rootType.getJpaEntityName(), entityIdentifierBinding );
		indexEntityIdentifierBinding( rootType.getClassDetails().getName(), entityIdentifierBinding );
		indexEntityIdentifierBinding( rootType.getClassDetails().getClassName(), entityIdentifierBinding );
	}

	public @Nullable EntityIdentifierBinding getEntityIdentifierBinding(EntityTypeMetadataImpl rootType) {
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

	public @Nullable EntityIdentifierBindingView getEntityIdentifierBindingView(EntityTypeMetadataImpl rootType) {
		final EntityIdentifierBinding binding = getEntityIdentifierBinding( rootType );
		return binding == null ? null : new EntityIdentifierBindingView( binding );
	}

	public @Nullable EntityView getEntityView(EntityTypeMetadataImpl rootType) {
		final ManagedTypeBinding binding = getManagedTypeBinding( rootType.getClassDetails() );
		if ( !( binding instanceof EntityTypeBinding entityBinding ) ) {
			return null;
		}
		return new EntityView( entityBinding, getEntityIdentifierBinding( rootType ) );
	}

	public void addVersionBinding(EntityTypeMetadataImpl rootType, VersionBinding versionBinding) {
		versionBindings.put( rootType, versionBinding );
		staticMetamodelInjectionPlan.addVersion( versionBinding );
	}

	/// Static-metamodel injection work accumulated alongside semantic binding
	/// and mapping materialization.
	public JpaStaticMetamodelInjectionSource staticMetamodelInjectionSource() {
		return staticMetamodelInjectionPlan.build();
	}

	public @Nullable VersionBinding getVersionBinding(EntityTypeMetadataImpl rootType) {
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

	public @Nullable VersionBindingView getVersionBindingView(EntityTypeMetadataImpl rootType) {
		final VersionBinding binding = getVersionBinding( rootType );
		return binding == null ? null : new VersionBindingView( binding );
	}

	public void addTenantIdBinding(EntityTypeMetadataImpl rootType, TenantIdBinding tenantIdBinding) {
		tenantIdBindings.put( rootType, tenantIdBinding );
	}

	public @Nullable TenantIdBinding getTenantIdBinding(EntityTypeMetadataImpl rootType) {
		return tenantIdBindings.get( rootType );
	}

	public @Nullable TenantIdBindingView getTenantIdBindingView(EntityTypeMetadataImpl rootType) {
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
			AbstractIdentifiableTypeMetadata owner,
			String attributeName) {
		final NaturalIdContribution contribution = getNaturalIdContribution( owner, attributeName );
		return contribution == null ? null : new NaturalIdContributionView( contribution );
	}

	private @Nullable NaturalIdContribution getNaturalIdContribution(
			AbstractIdentifiableTypeMetadata owner,
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
			AbstractIdentifiableTypeMetadata owner,
			String attributePath) {
		final CollationContribution contribution = getCollationContribution( owner, attributePath );
		return contribution == null ? null : new CollationContributionView( contribution );
	}

	private @Nullable CollationContribution getCollationContribution(
			AbstractIdentifiableTypeMetadata owner,
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
	}

	public List<MappedSuperclassContribution> mappedSuperclassContributions() {
		return List.copyOf( mappedSuperclassContributions );
	}

	/**
	 * Registers an applied mapped-superclass attribute mapping with its
	 * contribution.
	 * <p>
	 * Callers should use this method instead of mutating
	 * {@link MappedSuperclassContribution} directly.  The mapping must already
	 * be registered in the intrinsic role index while the attribute is
	 * materialized.
	 */
	public AppliedAttributeMapping addAppliedMappedSuperclassAttributeMapping(
			MappedSuperclassContribution contribution,
			AppliedAttributeMapping appliedMapping) {
		if ( getAppliedAttributeMapping( appliedMapping.role() ) != appliedMapping ) {
			throw new IllegalArgumentException(
					"Mapped-superclass attribute mapping is not registered for role " + appliedMapping.role()
			);
		}
		staticMetamodelInjectionPlan.addConcreteGenericAttribute(
				contribution.consumer().getClassDetails(),
				appliedMapping
		);
		return contribution.addAppliedAttributeMapping( appliedMapping );
	}

	public void addEmbeddableContribution(EmbeddableContribution contribution) {
		embeddableContributions.add( contribution );
	}

	public List<EmbeddableContribution> embeddableContributions() {
		return List.copyOf( embeddableContributions );
	}

}
