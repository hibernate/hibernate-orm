/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.model;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.boot.mapping.internal.categorize.EntityTypeMetadataImpl;
import org.hibernate.boot.mapping.internal.categorize.AbstractIdentifiableTypeMetadata;
import org.hibernate.boot.mapping.internal.categorize.MappedSuperclassTypeMetadataImpl;

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
	private final MappedSuperclassTypeMetadataImpl declaration;
	private final AbstractIdentifiableTypeMetadata consumer;
	private final EntityTypeMetadataImpl nearestEntityConsumer;
	private final List<AppliedAttributeMapping> appliedAttributeMappings = new ArrayList<>();

	public MappedSuperclassContribution(
			MappedSuperclassTypeMetadataImpl declaration,
			AbstractIdentifiableTypeMetadata consumer,
			EntityTypeMetadataImpl nearestEntityConsumer) {
		this.declaration = declaration;
		this.consumer = consumer;
		this.nearestEntityConsumer = nearestEntityConsumer;
	}

	public MappedSuperclassTypeMetadataImpl declaration() {
		return declaration;
	}

	public AbstractIdentifiableTypeMetadata consumer() {
		return consumer;
	}

	public EntityTypeMetadataImpl nearestEntityConsumer() {
		return nearestEntityConsumer;
	}

	/// Should not be called directly.  Instead, use [BootBindingModel#addAppliedMappedSuperclassAttributeMapping]
	AppliedAttributeMapping addAppliedAttributeMapping(AppliedAttributeMapping mapping) {
		appliedAttributeMappings.add( mapping );
		return mapping;
	}

	/// The concrete attribute applications contributed to the consuming entity,
	/// in declaration order.
	public List<AppliedAttributeMapping> appliedAttributeMappings() {
		return List.copyOf( appliedAttributeMappings );
	}

	public List<AttributeUsageBinding> appliedAttributeUsages() {
		return appliedAttributeMappings.stream()
				.map( AppliedAttributeMapping::usage )
				.toList();
	}

	public List<String> appliedAttributeNames() {
		return appliedAttributeMappings.stream()
				.map( mapping -> mapping.usage().attributeName() )
				.toList();
	}
}
