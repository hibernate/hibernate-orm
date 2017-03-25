/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.testing.cache;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.spi.NaturalIdRegion;
import org.hibernate.cache.spi.access.NaturalIdRegionAccessStrategy;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.entity.EntityPersister;

/**
 * @author Eric Dalquist
 */
class BaseNaturalIdRegionAccessStrategy extends BaseRegionAccessStrategy implements NaturalIdRegionAccessStrategy {
	private final NaturalIdRegionImpl region;

	@Override
	protected BaseGeneralDataRegion getInternalRegion() {
		return region;
	}

	@Override
	protected boolean isDefaultMinimalPutOverride() {
		return region.getSettings().isMinimalPutsEnabled();
	}

	@Override
	public NaturalIdRegion getRegion() {
		return region;
	}

	@Override
	public boolean insert(SharedSessionContractImplementor session, Object key, Object value) throws CacheException {
		return putFromLoad( session, key, value, 0, null );
	}

	@Override
	public boolean afterInsert(SharedSessionContractImplementor session, Object key, Object value) throws CacheException {
		return false;
	}

	@Override
	public boolean update(SharedSessionContractImplementor session, Object key, Object value) throws CacheException {
		return putFromLoad( session, key, value, 0, null );
	}

	@Override
	public boolean afterUpdate(SharedSessionContractImplementor session, Object key, Object value, SoftLock lock) throws CacheException {
		return false;
	}

	BaseNaturalIdRegionAccessStrategy(NaturalIdRegionImpl region) {
		this.region = region;
	}

	@Override
	public Object generateCacheKey(Object[] naturalIdValues, EntityPersister persister, SharedSessionContractImplementor session) {
		return region.getRegionFactory().getCacheKeysFactory().createNaturalIdKey( naturalIdValues, persister, session );
	}

	@Override
	public Object[] getNaturalIdValues(Object cacheKey) {
		return region.getRegionFactory().getCacheKeysFactory().getNaturalIdValues( cacheKey );
	}
}
