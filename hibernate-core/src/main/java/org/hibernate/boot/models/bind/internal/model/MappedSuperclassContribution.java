/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal.model;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.boot.models.categorize.spi.EntityTypeMetadata;
import org.hibernate.boot.models.categorize.spi.IdentifiableTypeMetadata;
import org.hibernate.boot.models.categorize.spi.MappedSuperclassTypeMetadata;

/// Binding-model contribution of one mapped-superclass declaration to one
/// consuming hierarchy boundary.
///
/// The declaration is the mapped superclass that owns the source members.  The
/// consumer is the nearest subtype currently receiving those members.  When that
/// consumer is an entity, compatibility materialization still applies the
/// contribution at the entity boundary and lets entity inheritance carry it from
/// there.
///
/// @since 9.0
/// @author Steve Ebersole
public class MappedSuperclassContribution {
	private final MappedSuperclassTypeMetadata declaration;
	private final IdentifiableTypeMetadata consumer;
	private final EntityTypeMetadata nearestEntityConsumer;
	private final List<String> appliedAttributeNames = new ArrayList<>();

	public MappedSuperclassContribution(
			MappedSuperclassTypeMetadata declaration,
			IdentifiableTypeMetadata consumer,
			EntityTypeMetadata nearestEntityConsumer) {
		this.declaration = declaration;
		this.consumer = consumer;
		this.nearestEntityConsumer = nearestEntityConsumer;
	}

	public MappedSuperclassTypeMetadata declaration() {
		return declaration;
	}

	public IdentifiableTypeMetadata consumer() {
		return consumer;
	}

	public EntityTypeMetadata nearestEntityConsumer() {
		return nearestEntityConsumer;
	}

	public void addAppliedAttributeName(String attributeName) {
		appliedAttributeNames.add( attributeName );
	}

	public List<String> appliedAttributeNames() {
		return List.copyOf( appliedAttributeNames );
	}
}
