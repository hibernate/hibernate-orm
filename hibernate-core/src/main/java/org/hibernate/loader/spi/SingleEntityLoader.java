/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.loader.spi;

import org.hibernate.LockOptions;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.model.domain.spi.EntityValuedNavigable;

/**
 * @author Steve Ebersole
 */
public interface SingleEntityLoader<T> extends Loader<T> {
	@Override
	EntityValuedNavigable<T> getLoadedNavigable();

	/**
	 * Load an entity by a primary/unique key value.
	 */
	T load(Object key, LockOptions lockOptions, SharedSessionContractImplementor session);
}
