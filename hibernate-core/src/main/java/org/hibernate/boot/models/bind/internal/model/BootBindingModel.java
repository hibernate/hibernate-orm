/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal.model;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.hibernate.boot.models.bind.internal.view.EntityView;
import org.hibernate.boot.models.bind.internal.view.IdentifierContributionView;
import org.hibernate.boot.models.categorize.spi.EntityTypeMetadata;
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
}
