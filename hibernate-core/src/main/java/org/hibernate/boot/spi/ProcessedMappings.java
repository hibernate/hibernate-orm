/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.spi;

import java.util.Collection;
import java.util.Set;

import org.hibernate.Incubating;

import jakarta.annotation.Nullable;

/// Immutable query view of mappings processed before an
/// [AdditionalMappingContributor] is invoked.
///
/// @since 9.0
/// @author Steve Ebersole
@Incubating
public interface ProcessedMappings {
	/**
	 * Hibernate entity names known before additional contributions are applied.
	 *
	 * @see ProcessedEntity#getEntityName()
	 */
	Set<String> getMappedEntityNames();

	/// Whether an entity binding with the given Hibernate entity name is known.
	default boolean hasEntityBinding(String hibernateEntityName) {
		return getMappedEntityNames().contains( hibernateEntityName );
	}

	/// Find the entity binding with the given Hibernate entity name.
	@Nullable
	ProcessedEntity getEntityBinding(String hibernateEntityName);

	/// The entity bindings known before additional contributions are applied.
	Collection<ProcessedEntity> getEntityBindings();
}
