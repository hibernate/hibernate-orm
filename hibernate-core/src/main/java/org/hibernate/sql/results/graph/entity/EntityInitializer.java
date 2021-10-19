/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.entity;

import org.hibernate.engine.spi.EntityKey;
import org.hibernate.sql.results.graph.FetchParentAccess;
import org.hibernate.persister.entity.EntityPersister;

/**
 * Initializer implementation for initializing entity references.
 *
 * @author Steve Ebersole
 */
public interface EntityInitializer extends FetchParentAccess {

	/**
	 * Get the descriptor for the type of entity being initialized
	 */
	EntityPersister getEntityDescriptor();

	EntityPersister getConcreteDescriptor();

	@Override
	default FetchParentAccess findFirstEntityDescriptorAccess() {
		return this;
	}

	/**
	 * Get the entity instance for the currently processing "row".
	 *
	 * @apiNote Calling this method is only valid from the time
	 * {@link #resolveKey} has been called until {@link #finishUpRow}
	 * has been called for the currently processing row
	 */
	Object getEntityInstance();

	@Override
	default Object getInitializedInstance() {
		return getEntityInstance();
	}

	EntityKey getEntityKey();
}
