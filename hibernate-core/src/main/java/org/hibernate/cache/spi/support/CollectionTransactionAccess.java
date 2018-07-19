/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.cache.spi.support;

import org.hibernate.cache.cfg.spi.CollectionDataCachingConfig;
import org.hibernate.cache.spi.CacheKeysFactory;
import org.hibernate.cache.spi.DomainDataRegion;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cache.spi.support.AbstractCollectionDataAccess;
import org.hibernate.cache.spi.support.DomainDataStorageAccess;

/**
 * @author Steve Ebersole
 */
public class CollectionTransactionAccess extends AbstractCollectionDataAccess {
	public CollectionTransactionAccess(
			DomainDataRegion region,
			CacheKeysFactory keysFactory,
			DomainDataStorageAccess storageAccess,
			CollectionDataCachingConfig config) {
		super( region, keysFactory, storageAccess, config );
	}

	@Override
	public AccessType getAccessType() {
		return AccessType.TRANSACTIONAL;
	}
}
