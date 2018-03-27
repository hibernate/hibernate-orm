/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.cache.spi;

import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * Specialized Region whose data is accessed directly (not requiring
 * key/item wrapping, e.g.
 *
 * @author Steve Ebersole
 */
public interface DirectAccessRegion extends Region {
	boolean contains(Object key);

	Object getFromCache(Object key, SharedSessionContractImplementor session);

	void putIntoCache(Object key, Object value, SharedSessionContractImplementor session);

	void removeFromCache(Object key, SharedSessionContractImplementor session);
}
