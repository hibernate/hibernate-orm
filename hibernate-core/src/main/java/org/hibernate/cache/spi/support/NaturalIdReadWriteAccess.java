/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.cache.spi.support;

import java.util.Comparator;

import org.hibernate.cache.cfg.spi.NaturalIdDataCachingConfig;
import org.hibernate.cache.spi.CacheKeysFactory;
import org.hibernate.cache.spi.DomainDataRegion;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cache.spi.access.NaturalIdDataAccess;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.entity.EntityPersister;

/**
 * Standard support for {@link org.hibernate.cache.spi.access.NaturalIdDataAccess}
 * using the {@link org.hibernate.cache.spi.access.AccessType#READ_WRITE} access type.
 *
 * @author Steve Ebersole
 */
public class NaturalIdReadWriteAccess extends AbstractReadWriteAccess implements NaturalIdDataAccess {
	private final CacheKeysFactory keysFactory;

	public NaturalIdReadWriteAccess(
			DomainDataRegion region,
			CacheKeysFactory keysFactory,
			DomainDataStorageAccess storageAccess,
			NaturalIdDataCachingConfig naturalIdDataCachingConfig) {
		super( region, storageAccess );
		this.keysFactory = keysFactory;
	}

	@Override
	protected AccessedDataClassification getAccessedDataClassification() {
		return AccessedDataClassification.NATURAL_ID;
	}

	@Override
	public AccessType getAccessType() {
		return AccessType.READ_WRITE;
	}

	@Override
	protected Comparator getVersionComparator() {
		// natural-id has no comparator
		return null;
	}

	@Override
	public Object generateCacheKey(
			Object[] naturalIdValues,
			EntityPersister rootEntityDescriptor,
			SharedSessionContractImplementor session) {
		return keysFactory.createNaturalIdKey( naturalIdValues, rootEntityDescriptor, session );
	}

	@Override
	public Object[] getNaturalIdValues(Object cacheKey) {
		return keysFactory.getNaturalIdValues( cacheKey );
	}

	@Override
	public boolean insert(SharedSessionContractImplementor session, Object key, Object value) {
		return false;
	}

	@Override
	public boolean afterInsert(SharedSessionContractImplementor session, Object key, Object value) {
		try {
			writeLock().lock();
			Lockable item = (Lockable) getStorageAccess().getFromCache( key, session );
			if ( item == null ) {
				getStorageAccess().putIntoCache(
						key,
						new Item( value, null, getRegion().getRegionFactory().nextTimestamp() ),
						session
				);
				return true;
			}
			else {
				return false;
			}
		}
		finally {
			writeLock().unlock();
		}
	}

	@Override
	public boolean update(SharedSessionContractImplementor session, Object key, Object value) {
		return false;
	}

	@Override
	public boolean afterUpdate(SharedSessionContractImplementor session, Object key, Object value, SoftLock lock) {
		try {
			writeLock().lock();
			Lockable item = (Lockable) getStorageAccess().getFromCache( key, session );

			if ( item != null && item.isUnlockable( lock ) ) {
				SoftLockImpl lockItem = (SoftLockImpl) item;
				if ( lockItem.wasLockedConcurrently() ) {
					decrementLock( session, key, lockItem );
					return false;
				}
				else {
					getStorageAccess().putIntoCache(
							key,
							new Item( value, null, getRegion().getRegionFactory().nextTimestamp() ),
							session
					);
					return true;
				}
			}
			else {
				handleLockExpiry( session, key, item );
				return false;
			}
		}
		finally {
			writeLock().unlock();
		}
	}
}
