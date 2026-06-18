/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.boot.models.bind.internal.view.EmbeddableContributionView;
import org.hibernate.boot.models.bind.internal.view.EntityView;
import org.hibernate.boot.models.bind.internal.view.IdentifierContributionView;
import org.hibernate.boot.models.bind.internal.view.MappedSuperclassContributionView;
import org.hibernate.boot.models.bind.internal.view.TenantIdContributionView;
import org.hibernate.boot.models.categorize.spi.EntityTypeMetadata;
import org.hibernate.boot.models.categorize.spi.IdentifiableTypeMetadata;
import org.hibernate.models.spi.ClassDetails;

import jakarta.annotation.Nullable;

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
	private final Map<EntityTypeMetadata, IdentifierContribution> identifierContributions = new LinkedHashMap<>();
	private final Map<EntityTypeMetadata, TenantIdContribution> tenantIdContributions = new LinkedHashMap<>();
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

	public void addIdentifierContribution(EntityTypeMetadata rootType, IdentifierContribution identifierContribution) {
		identifierContributions.put( rootType, identifierContribution );
	}

	public @Nullable IdentifierContribution getIdentifierContribution(EntityTypeMetadata rootType) {
		return identifierContributions.get( rootType );
	}

	public @Nullable IdentifierContributionView getIdentifierContributionView(EntityTypeMetadata rootType) {
		final IdentifierContribution contribution = getIdentifierContribution( rootType );
		return contribution == null ? null : new IdentifierContributionView( contribution );
	}

	public @Nullable EntityView getEntityView(EntityTypeMetadata rootType) {
		final ManagedTypeBinding binding = getManagedTypeBinding( rootType.getClassDetails() );
		if ( !( binding instanceof EntityTypeBinding entityBinding ) ) {
			return null;
		}
		return new EntityView( entityBinding, getIdentifierContribution( rootType ) );
	}

	public void addTenantIdContribution(EntityTypeMetadata rootType, TenantIdContribution tenantIdContribution) {
		tenantIdContributions.put( rootType, tenantIdContribution );
	}

	public @Nullable TenantIdContribution getTenantIdContribution(EntityTypeMetadata rootType) {
		return tenantIdContributions.get( rootType );
	}

	public @Nullable TenantIdContributionView getTenantIdContributionView(EntityTypeMetadata rootType) {
		final TenantIdContribution contribution = getTenantIdContribution( rootType );
		return contribution == null ? null : new TenantIdContributionView( contribution );
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
