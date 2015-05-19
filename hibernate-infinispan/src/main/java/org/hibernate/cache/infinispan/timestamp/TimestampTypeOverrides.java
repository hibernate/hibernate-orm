/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.infinispan.timestamp;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.infinispan.TypeOverrides;

import org.infinispan.configuration.cache.Configuration;
import org.infinispan.eviction.EvictionStrategy;

/**
 * TimestampTypeOverrides.
 *
 * @author Galder Zamarreño
 * @since 3.5
 */
public class TimestampTypeOverrides extends TypeOverrides {

	@Override
	public void validateInfinispanConfiguration(Configuration cfg) throws CacheException {
		if ( cfg.clustering().cacheMode().isInvalidation() ) {
			throw new CacheException( "Timestamp cache cannot be configured with invalidation" );
		}
		final EvictionStrategy strategy = cfg.eviction().strategy();
		if ( !strategy.equals( EvictionStrategy.NONE ) ) {
			throw new CacheException( "Timestamp cache cannot be configured with eviction" );
		}
	}

}
