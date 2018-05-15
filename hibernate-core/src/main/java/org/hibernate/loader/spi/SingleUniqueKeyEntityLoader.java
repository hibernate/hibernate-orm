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
 * Loader subtype for loading an entity by a single unique-key value.
 *
 * @author Steve Ebersole
 */
public interface SingleUniqueKeyEntityLoader<T> extends SingleEntityLoader {
	/**
	 * Here, the passed key is expected to be the uk value
	 */
	T load(Object ukValue, LockOptions lockOptions, SharedSessionContractImplementor session);
}
