/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.loader.spi;

import org.hibernate.LockOptions;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * Loader subtype for loading an entity by a single identifier value.
 *
 * @author Steve Ebersole
 */
public interface SingleIdEntityLoader<T> extends SingleEntityLoader<T> {
	/**
	 * Here the key is a primary key value
	 */
	@Override
	T load(Object pkValue, LockOptions lockOptions, SharedSessionContractImplementor session);

	Object[] loadDatabaseSnapshot(Object id, SharedSessionContractImplementor session);
}
