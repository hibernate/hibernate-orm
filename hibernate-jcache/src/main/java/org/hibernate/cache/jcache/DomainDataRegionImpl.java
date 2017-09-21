/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.cache.jcache;

import org.hibernate.cache.cfg.spi.CollectionDataCachingConfig;
import org.hibernate.cache.cfg.spi.DomainDataRegionBuildingContext;
import org.hibernate.cache.cfg.spi.DomainDataRegionConfig;
import org.hibernate.cache.cfg.spi.EntityDataCachingConfig;
import org.hibernate.cache.cfg.spi.NaturalIdDataCachingConfig;
import org.hibernate.cache.spi.AbstractDomainDataRegion;
import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.cache.spi.access.CollectionDataAccess;
import org.hibernate.cache.spi.access.EntityDataAccess;
import org.hibernate.cache.spi.access.NaturalIdDataAccess;
import org.hibernate.NotYetImplementedFor6Exception;

/**
 * @author Steve Ebersole
 */
public class DomainDataRegionImpl extends AbstractDomainDataRegion {
	public DomainDataRegionImpl(
			DomainDataRegionConfig regionConfig,
			RegionFactory regionFactory,
			DomainDataRegionBuildingContext buildingContext) {
		super( regionConfig, regionFactory, buildingContext );
	}

	@Override
	protected EntityDataAccess generateEntityAccess(EntityDataCachingConfig entityAccessConfig) {
		throw new NotYetImplementedFor6Exception(  );
	}

	@Override
	protected CollectionDataAccess generateCollectionAccess(CollectionDataCachingConfig cachingConfig) {
		throw new NotYetImplementedFor6Exception(  );
	}

	@Override
	protected NaturalIdDataAccess generateNaturalIdAccess(NaturalIdDataCachingConfig naturalIdAccessConfig) {
		throw new NotYetImplementedFor6Exception(  );
	}
}
