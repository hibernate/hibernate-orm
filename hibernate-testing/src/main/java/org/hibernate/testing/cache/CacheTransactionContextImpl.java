/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.cache;

import org.hibernate.cache.spi.AbstractCacheTransactionContext;
import org.hibernate.cache.spi.RegionFactory;

/**
 * @author Steve Ebersole
 */
class CacheTransactionContextImpl extends AbstractCacheTransactionContext {
	CacheTransactionContextImpl(RegionFactory regionFactory) {
		super( regionFactory );
	}
}
