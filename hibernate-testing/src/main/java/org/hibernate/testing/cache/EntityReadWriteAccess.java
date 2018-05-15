/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.cache;

import java.util.Comparator;

import org.hibernate.cache.spi.access.EntityDataAccess;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.cache.spi.support.DomainDataRegionImpl;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.model.domain.spi.EntityHierarchy;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("unused")
public class EntityReadWriteAccess extends AbstractReadWriteAccess implements EntityDataAccess {
	private final EntityHierarchy entityHierarchy;

	protected EntityReadWriteAccess(DomainDataRegionImpl region, EntityHierarchy entityHierarchy) {
		super( region );
		this.entityHierarchy = entityHierarchy;
	}

	@Override
	protected Comparator getVersionComparator() {
		// todo (6.0) : need to define Comparator on VersionDescriptor or on VersionSupport
		//		Can't use the VersionDescriptor's JavaTypeDescriptor as it may not be the same (byte[])
		//
		// for now, use the JTD Comparator
		return entityHierarchy.getVersionDescriptor().getJavaTypeDescriptor().getComparator();
	}

	@Override
	public Object generateCacheKey(
			Object id,
			EntityHierarchy entityHierarchy,
			SessionFactoryImplementor factory,
			String tenantIdentifier) {
		return getRegion().getEffectiveKeysFactory().createEntityKey( id, entityHierarchy, factory, tenantIdentifier );
	}

	@Override
	public Object getCacheKeyId(Object cacheKey) {
		return getRegion().getEffectiveKeysFactory().getEntityId( cacheKey );
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
			Lockable item = (Lockable) getFromCache( key );
			if ( item == null ) {
				addToCache(  key, new Item( value, version, getRegion().getRegionFactory().nextTimestamp() ) );
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
			Lockable item = (Lockable) getFromCache( key );

			if ( item != null && item.isUnlockable( lock ) ) {
				SoftLockImpl lockItem = (SoftLockImpl) item;
				if ( lockItem.wasLockedConcurrently() ) {
					decrementLock( session, key, lockItem );
					return false;
				}
				else {
					addToCache( key, new Item( value, currentVersion, getRegion().getRegionFactory().nextTimestamp() ) );
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

	@Override
	public void unlockRegion(SoftLock lock) {

	}
}
