/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.testing.cache;

import java.util.Comparator;

import org.hibernate.cache.internal.DefaultCacheKeysFactory;
import org.hibernate.cache.spi.CollectionRegion;
import org.hibernate.cache.spi.access.CollectionRegionAccessStrategy;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.collection.CollectionPersister;

/**
 * @author Strong Liu
 */
class ReadWriteCollectionRegionAccessStrategy extends AbstractReadWriteAccessStrategy
		implements CollectionRegionAccessStrategy {

	private final CollectionRegionImpl region;

	ReadWriteCollectionRegionAccessStrategy(CollectionRegionImpl region) {
		this.region = region;
	}

	@Override
	Comparator getVersionComparator() {
		return region.getCacheDataDescription().getVersionComparator();
	}

	@Override
	protected BaseGeneralDataRegion getInternalRegion() {
		return region;
	}

	@Override
	protected boolean isDefaultMinimalPutOverride() {
		return region.getSettings().isMinimalPutsEnabled();
	}

	@Override
	public CollectionRegion getRegion() {
		return region;
	}

	@Override
	public Object generateCacheKey(Object id, CollectionPersister persister, SessionFactoryImplementor factory, String tenantIdentifier) {
		return region.getRegionFactory().getCacheKeysFactory().createCollectionKey( id, persister, factory, tenantIdentifier );
	}

	@Override
	public Object getCacheKeyId(Object cacheKey) {
		return region.getRegionFactory().getCacheKeysFactory().getCollectionId( cacheKey );
	}
}
