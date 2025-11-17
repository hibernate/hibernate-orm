/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.spi;

import java.util.function.Supplier;

import org.hibernate.Incubating;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.persister.entity.EntityPersister;

/**
 * Pluggable strategy handling resolution of ManagedTypeRepresentationStrategy to use.
 *
 * @apiNote Like ManagedTypeRepresentationStrategy itself, incubating because we
 * currently need to differentiate between strategy for entity/mapped-superclass
 * versus strategy for embeddables
 */
@Incubating
public interface ManagedTypeRepresentationResolver {
	/**
	 * Resolve the strategy to use for the given entity
	 */
	EntityRepresentationStrategy resolveStrategy(
			PersistentClass bootDescriptor,
			EntityPersister runtimeDescriptor,
			RuntimeModelCreationContext creationContext);

	/**
	 * Resolve the strategy to use for the given embeddable
	 */
	EmbeddableRepresentationStrategy resolveStrategy(
			Component bootDescriptor,
			Supplier<EmbeddableMappingType> runtimeDescriptor,
			RuntimeModelCreationContext creationContext);
}
