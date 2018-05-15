/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal.collection;

import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * Delegate for creating a collection
 *
 * @author Steve Ebersole
 */
public interface CollectionCreationExecutor {
	/**
	 * A no-op instance
	 */
	CollectionCreationExecutor NO_OP = (collection, key, session) -> {};

	/**
	 * Execute the creation
	 */
	void create(
			PersistentCollection collection,
			Object key,
			SharedSessionContractImplementor session);
}
