/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cache.spi.support;

import org.hibernate.cache.cfg.spi.NaturalIdDataCachingConfig;
import org.hibernate.cache.spi.CacheKeysFactory;
import org.hibernate.cache.spi.DomainDataRegion;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * Standard support for {@link org.hibernate.cache.spi.access.NaturalIdDataAccess}
 * using the {@link AccessType#NONSTRICT_READ_WRITE} access type.
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
	public AccessType getAccessType() {
		return AccessType.NONSTRICT_READ_WRITE;
	}

	@Override
	public void unlockItem(SharedSessionContractImplementor session, Object key, SoftLock lock) {
		getStorageAccess().removeFromCache( key, session );
	}

	@Override
	public void remove(SharedSessionContractImplementor session, Object key) {
		getStorageAccess().removeFromCache( key, session );
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
		getStorageAccess().removeFromCache( key, session );
		return false;
	}
}
