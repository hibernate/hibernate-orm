/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.cache.spi.support;

import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * Specialization of StorageAccess for domain data regions
 *
 * @author Steve Ebersole
 */
public interface DomainDataStorageAccess extends StorageAccess {
	/**
	 * Specialized form of putting something into the cache
	 * in cases where the put is coming from a load (read) from
	 * the database
	 *
	 * @implNote the method default is to call {@link #putIntoCache}
	 */
	default void putFromLoad(Object key, Object value, SharedSessionContractImplementor session) {
		putIntoCache( key, value, session );
	}
}
