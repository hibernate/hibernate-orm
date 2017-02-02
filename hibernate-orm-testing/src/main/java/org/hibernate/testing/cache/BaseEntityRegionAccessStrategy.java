/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.testing.cache;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.spi.EntityRegion;
import org.hibernate.cache.spi.access.EntityRegionAccessStrategy;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.entity.EntityPersister;

/**
 * @author Strong Liu
 */
class BaseEntityRegionAccessStrategy extends BaseRegionAccessStrategy implements EntityRegionAccessStrategy {

	private final EntityRegionImpl region;

	BaseEntityRegionAccessStrategy(EntityRegionImpl region) {
		this.region = region;
	}

	@Override
	public EntityRegion getRegion() {
		return region;
	}

	@Override
	public boolean insert(SharedSessionContractImplementor session, Object key, Object value, Object version) throws CacheException {
		return putFromLoad( session, key, value, 0, version );
	}

	@Override
	public boolean afterInsert(SharedSessionContractImplementor session, Object key, Object value, Object version) throws CacheException {
		return true;
	}

	@Override
	public boolean update(SharedSessionContractImplementor session, Object key, Object value, Object currentVersion, Object previousVersion)
			throws CacheException {
		return false;
	}

	@Override
	public boolean afterUpdate(SharedSessionContractImplementor session, Object key, Object value, Object currentVersion, Object previousVersion, SoftLock lock)
			throws CacheException {
		return false;
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
	public Object generateCacheKey(Object id, EntityPersister persister, SessionFactoryImplementor factory, String tenantIdentifier) {
		return region.getRegionFactory().getCacheKeysFactory().createEntityKey( id, persister, factory, tenantIdentifier );
	}

	@Override
	public Object getCacheKeyId(Object cacheKey) {
		return region.getRegionFactory().getCacheKeysFactory().getEntityId( cacheKey );
	}
}
