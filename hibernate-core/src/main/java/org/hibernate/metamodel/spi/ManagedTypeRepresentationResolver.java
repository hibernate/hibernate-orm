/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.spi;

import org.hibernate.Incubating;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.persister.entity.EntityPersister;

/**
 * Pluggable strategy handling resolution of ManagedTypeRepresentationStrategy to use.
 *
 * @apiNote Like ManagedTypeRepresentationStrategy itself, incubating because we
 * currently need to differentiate between strategy for entity/mapped-superclass
 * versus strategy for embeddables
 *
 * @author Steve Ebersole
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
			RuntimeModelCreationContext creationContext);
}
