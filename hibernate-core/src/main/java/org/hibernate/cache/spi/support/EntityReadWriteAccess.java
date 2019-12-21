/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.cache.spi.support;

import java.util.Comparator;

import org.hibernate.cache.cfg.spi.EntityDataCachingConfig;
import org.hibernate.cache.spi.CacheKeysFactory;
import org.hibernate.cache.spi.DomainDataRegion;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cache.spi.access.EntityDataAccess;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.entity.EntityPersister;

/**
 * Standard support for {@link org.hibernate.cache.spi.access.EntityDataAccess}
 * using the {@link org.hibernate.cache.spi.access.AccessType#READ_WRITE} access type.
 *
 * @author Steve Ebersole
 */
public class EntityReadWriteAccess extends AbstractReadWriteAccess implements EntityDataAccess {
	private final CacheKeysFactory keysFactory;
	private final Comparator versionComparator;

	public EntityReadWriteAccess(
			DomainDataRegion domainDataRegion,
			CacheKeysFactory keysFactory,
			DomainDataStorageAccess storageAccess,
			EntityDataCachingConfig entityAccessConfig) {
		super( domainDataRegion, storageAccess );
		this.keysFactory = keysFactory;
		this.versionComparator = entityAccessConfig.getVersionComparatorAccess() == null
				? null
				: entityAccessConfig.getVersionComparatorAccess().get();
	}

	@Override
	public AccessType getAccessType() {
		return AccessType.READ_WRITE;
	}

	@Override
	protected AccessedDataClassification getAccessedDataClassification() {
		return AccessedDataClassification.ENTITY;
	}

	@Override
	protected Comparator getVersionComparator() {
		return versionComparator;
	}

	@Override
	public Object generateCacheKey(
			Object id,
			EntityPersister rootEntityDescriptor,
			SessionFactoryImplementor factory,
			String tenantIdentifier) {
		return keysFactory.createEntityKey( id, rootEntityDescriptor, factory, tenantIdentifier );
	}

	@Override
	public Object getCacheKeyId(Object cacheKey) {
		return keysFactory.getEntityId( cacheKey );
	}

	@Override
	public boolean insert(
			SharedSessionContractImplementor session,
			Object key,
			Object value,
			Object version) {
		return false;
	}

	@Override
	public boolean afterInsert(SharedSessionContractImplementor session, Object key, Object value, Object version) {
		try {
			writeLock().lock();
			Lockable item = (Lockable) getStorageAccess().getFromCache( key, session );
			if ( item == null ) {
				getStorageAccess().putIntoCache(
						key,
						new Item( value, version, getRegion().getRegionFactory().nextTimestamp() ),
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
	public boolean update(
			SharedSessionContractImplementor session,
			Object key,
			Object value,
			Object currentVersion,
			Object previousVersion) {
		return false;
	}

	@Override
	public boolean afterUpdate(
			SharedSessionContractImplementor session,
			Object key,
			Object value,
			Object currentVersion,
			Object previousVersion,
			SoftLock lock) {
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
							new Item( value, currentVersion, getRegion().getRegionFactory().nextTimestamp() ),
							session
					);
					return true;
				}
			}
			else {
				handleLockExpiry(session, key, item );
				return false;
			}
		}
		finally {
			writeLock().unlock();
		}
	}

	@Override
	public SoftLock lockRegion() {
		return null;
	}
}
