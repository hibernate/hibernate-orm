/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.cache;

import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.cache.spi.support.DomainDataRegionImpl;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("unused")
public class CollectionNonStrictReadWriteAccess extends BaseCollectionDataAccess {
	public CollectionNonStrictReadWriteAccess(
			DomainDataRegionImpl region,
			PersistentCollectionDescriptor collectionDescriptor) {
		super( region, collectionDescriptor );
	}

	@Override
	public AccessType getAccessType() {
		return AccessType.NONSTRICT_READ_WRITE;
	}

	@Override
	public void unlockItem(SharedSessionContractImplementor session, Object key, SoftLock lock) {
		removeFromCache( key );
	}
}
