/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.ehcache.internal.regions;

import java.util.Properties;

import net.sf.ehcache.Ehcache;

import org.hibernate.cache.ehcache.internal.strategy.EhcacheAccessStrategyFactory;
import org.hibernate.cache.spi.QueryResultsRegion;

/**
 * A query results region specific wrapper around an Ehcache instance.
 *
 * @author Chris Dennis
 * @author Alex Snaps
 */
public class EhcacheQueryResultsRegion extends EhcacheGeneralDataRegion implements QueryResultsRegion {
	/**
	 * Constructs an EhcacheQueryResultsRegion around the given underlying cache.
	 *
	 * @param accessStrategyFactory The factory for building needed CollectionRegionAccessStrategy instance
	 * @param underlyingCache The ehcache cache instance
	 * @param properties Any additional[ properties
	 */
	public EhcacheQueryResultsRegion(
			EhcacheAccessStrategyFactory accessStrategyFactory,
			Ehcache underlyingCache,
			Properties properties) {
		super( accessStrategyFactory, underlyingCache, properties );
	}

}
