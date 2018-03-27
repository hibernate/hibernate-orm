/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.cache.spi.support;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.cfg.spi.EntityDataCachingConfig;
import org.hibernate.cache.spi.CacheKeysFactory;
import org.hibernate.cache.spi.DomainDataRegion;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * Standard support for {@link org.hibernate.cache.spi.access.EntityDataAccess}
 * using the {@link org.hibernate.cache.spi.access.AccessType#NONSTRICT_READ_WRITE} access type.
 *
 * @author Steve Ebersole
 */
public class EntityNonStrictReadWriteAccess extends AbstractEntityDataAccess {
	public EntityNonStrictReadWriteAccess(
			DomainDataRegion domainDataRegion,
			CacheKeysFactory keysFactory,
			DomainDataStorageAccess storageAccess,
			EntityDataCachingConfig entityAccessConfig) {
		super( domainDataRegion, keysFactory, storageAccess );
	}

	@Override
	public AccessType getAccessType() {
		return AccessType.NONSTRICT_READ_WRITE;
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
		return false;
	}

	@Override
	public boolean update(
			SharedSessionContractImplementor session,
			Object key,
			Object value,
			Object currentVersion,
			Object previousVersion) {
		getStorageAccess().removeFromCache( key, session );
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
		unlockItem( session, key, lock );
		return false;
	}

	/**
	 * Since this is a non-strict read/write strategy item locking is not used.
	 */
	@Override
	public void unlockItem(SharedSessionContractImplementor session, Object key, SoftLock lock) throws CacheException {
		getStorageAccess().removeFromCache( key, session );
	}

	@Override
	public void remove(SharedSessionContractImplementor session, Object key) {
		getStorageAccess().removeFromCache( key, session );
	}
}
