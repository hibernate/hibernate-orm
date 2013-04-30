/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */

package org.hibernate.cache.ehcache.internal.regions;

import java.util.Properties;

import net.sf.ehcache.Ehcache;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.ehcache.internal.strategy.EhcacheAccessStrategyFactory;
import org.hibernate.cache.spi.CacheDataDescription;
import org.hibernate.cache.spi.CollectionRegion;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cache.spi.access.CollectionRegionAccessStrategy;
import org.hibernate.cfg.Settings;

/**
 * A collection region specific wrapper around an Ehcache instance.
 * <p/>
 * This implementation returns Ehcache specific access strategy instances for all the non-transactional access types. Transactional access
 * is not supported.
 *
 * @author Chris Dennis
 * @author Abhishek Sanoujam
 * @author Alex Snaps
 */
public class EhcacheCollectionRegion extends EhcacheTransactionalDataRegion implements CollectionRegion {
	/**
	 * Constructs an EhcacheCollectionRegion around the given underlying cache.
	 *
	 * @param accessStrategyFactory The factory for building needed CollectionRegionAccessStrategy instance
	 * @param underlyingCache The ehcache cache instance
	 * @param settings The Hibernate settings
	 * @param metadata Metadata about the data to be cached in this region
	 * @param properties Any additional[ properties
	 */
	public EhcacheCollectionRegion(
			EhcacheAccessStrategyFactory accessStrategyFactory,
			Ehcache underlyingCache,
			Settings settings,
			CacheDataDescription metadata,
			Properties properties) {
		super( accessStrategyFactory, underlyingCache, settings, metadata, properties );
	}

	@Override
	public CollectionRegionAccessStrategy buildAccessStrategy(AccessType accessType) throws CacheException {
		return getAccessStrategyFactory().createCollectionRegionAccessStrategy( this, accessType );
	}
}
