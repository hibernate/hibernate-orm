/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.cache.jcache.internal;

import javax.cache.Cache;

import org.hibernate.cache.cfg.spi.DomainDataRegionBuildingContext;
import org.hibernate.cache.cfg.spi.DomainDataRegionConfig;
import org.hibernate.cache.spi.CacheKeysFactory;
import org.hibernate.cache.spi.support.DomainDataRegionTemplate;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class DomainDataRegionImpl extends DomainDataRegionTemplate {
	private static final Logger log = Logger.getLogger( DomainDataRegionImpl.class );

	@SuppressWarnings("WeakerAccess")
	public DomainDataRegionImpl(
			DomainDataRegionConfig regionConfig,
			JCacheRegionFactory regionFactory,
			Cache underlyingCache,
			CacheKeysFactory cacheKeysFactory,
			DomainDataRegionBuildingContext buildingContext) {
		super(
				regionConfig,
				regionFactory,
				new DomainDataJCacheAccessImpl( underlyingCache ),
				cacheKeysFactory,
				buildingContext
		);
	}

	/**
	 * Public override for testing use only
	 */
	@Override
	public DomainDataJCacheAccessImpl getCacheStorageAccess() {
		return (DomainDataJCacheAccessImpl) super.getCacheStorageAccess();
	}
}
