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
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * Standard support for {@link org.hibernate.cache.spi.access.NaturalIdDataAccess}
 * using the {@link org.hibernate.cache.spi.access.AccessType#NONSTRICT_READ_WRITE} access type.
 *
 * @author Steve Ebersole
 */
public class NaturalIdNonStrictReadWriteAccess extends AbstractNaturalIdDataAccess {
	public NaturalIdNonStrictReadWriteAccess(
			DomainDataRegion region,
			CacheKeysFactory keysFactory,
			DomainDataStorageAccess storageAccess,
			NaturalIdDataCachingConfig config) {
		super( region, keysFactory, storageAccess, config );
	}

	@Override
	public void unlockItem(SharedSessionContractImplementor session, Object key, SoftLock lock) {
		removeFromCache( key );
	}

	@Override
	public void remove(SharedSessionContractImplementor session, Object key) {
		removeFromCache( key );
	}

	@Override
	public boolean insert(SharedSessionContractImplementor session, Object key, Object value) {
		return false;
	}

	@Override
	public boolean afterInsert(SharedSessionContractImplementor session, Object key, Object value) {
		return false;
	}

	@Override
	public boolean update(SharedSessionContractImplementor session, Object key, Object value) {
		removeFromCache( key );
		return false;
	}
}
