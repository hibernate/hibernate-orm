/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.cache.spi.support;

import org.hibernate.cache.cfg.spi.NaturalIdDataCachingConfig;
import org.hibernate.cache.spi.CacheKeysFactory;
import org.hibernate.cache.spi.DomainDataRegion;
import org.hibernate.cache.spi.access.NaturalIdDataAccess;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.entity.EntityPersister;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractNaturalIdDataAccess extends AbstractCachedDomainDataAccess implements NaturalIdDataAccess {
	private final CacheKeysFactory keysFactory;

	public AbstractNaturalIdDataAccess(
			DomainDataRegion region,
			CacheKeysFactory keysFactory,
			DomainDataStorageAccess storageAccess,
			NaturalIdDataCachingConfig config) {
		super( region, storageAccess );
		this.keysFactory = keysFactory;
	}

	@Override
	public Object generateCacheKey(
			Object[] naturalIdValues,
			EntityPersister persister,
			SharedSessionContractImplementor session) {
		return keysFactory.createNaturalIdKey( naturalIdValues, persister, session );
	}

	@Override
	public Object[] getNaturalIdValues(Object cacheKey) {
		return keysFactory.getNaturalIdValues( cacheKey );
	}


	@Override
	public boolean insert(SharedSessionContractImplementor session, Object key, Object value) {
		getStorageAccess().putIntoCache( key, value, session );
		return true;
	}

	@Override
	public boolean afterInsert(SharedSessionContractImplementor session, Object key, Object value) {
		return false;
	}

	@Override
	public boolean update(SharedSessionContractImplementor session, Object key, Object value) {
		getStorageAccess().putIntoCache( key, value, session );
		return true;
	}

	@Override
	public boolean afterUpdate(SharedSessionContractImplementor session, Object key, Object value, SoftLock lock) {
		return false;
	}

	@Override
	public SoftLock lockRegion() {
		return null;
	}

	@Override
	public void unlockRegion(SoftLock lock) {
		clearCache();
	}

	public SoftLock lockItem(
			SharedSessionContractImplementor session,
			Object key,
			Object version) {
		return null;
	}

	@Override
	public void unlockItem(
			SharedSessionContractImplementor session,
			Object key,
			SoftLock lock) {
	}
}
