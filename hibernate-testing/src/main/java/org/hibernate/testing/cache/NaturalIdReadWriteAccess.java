/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.cache;

import java.util.Comparator;

import org.hibernate.cache.spi.access.NaturalIdDataAccess;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.cache.spi.support.DomainDataRegionImpl;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.model.domain.spi.EntityHierarchy;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("unused")
public class NaturalIdReadWriteAccess extends AbstractReadWriteAccess implements NaturalIdDataAccess {
	private final EntityHierarchy entityHierarchy;

	public NaturalIdReadWriteAccess(
			DomainDataRegionImpl region,
			EntityHierarchy entityHierarchy) {
		super( region );
		this.entityHierarchy = entityHierarchy;
	}

	public EntityHierarchy getEntityHierarchy() {
		return entityHierarchy;
	}

	@Override
	protected Comparator getVersionComparator() {
		// natural-id has no comparator
		return null;
	}

	@Override
	public Object generateCacheKey(
			Object[] naturalIdValues,
			EntityHierarchy entityHierarchy,
			SharedSessionContractImplementor session) {
		return getRegion().getEffectiveKeysFactory().createNaturalIdKey( naturalIdValues, entityHierarchy, session );
	}

	@Override
	public Object[] getNaturalIdValues(Object cacheKey) {
		return getRegion().getEffectiveKeysFactory().getNaturalIdValues( cacheKey );
	}

	@Override
	public boolean insert(SharedSessionContractImplementor session, Object key, Object value) {
		return false;
	}

	@Override
	public boolean afterInsert(SharedSessionContractImplementor session, Object key, Object value) {
		try {
			writeLock().lock();
			Lockable item = (Lockable) getFromCache( key );
			if ( item == null ) {
				addToCache( key, new Item( value, null, getRegion().getRegionFactory().nextTimestamp() ) );
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
			Lockable item = (Lockable) getFromCache( key );

			if ( item != null && item.isUnlockable( lock ) ) {
				SoftLockImpl lockItem = (SoftLockImpl) item;
				if ( lockItem.wasLockedConcurrently() ) {
					decrementLock( session, key, lockItem );
					return false;
				}
				else {
					addToCache( key, new Item( value, null, getRegion().getRegionFactory().nextTimestamp() ) );
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
