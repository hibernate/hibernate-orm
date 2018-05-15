/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.cache;

import org.hibernate.cache.spi.access.CollectionDataAccess;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.cache.spi.support.DomainDataRegionImpl;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("WeakerAccess")
public abstract class BaseCollectionDataAccess
		extends AbstractCachedDomainDataAccess
		implements CollectionDataAccess {
	private final PersistentCollectionDescriptor collectionDescriptor;

	public BaseCollectionDataAccess(
			DomainDataRegionImpl region,
			PersistentCollectionDescriptor collectionDescriptor) {
		super( region );
		this.collectionDescriptor = collectionDescriptor;
	}

	@SuppressWarnings("unused")
	public PersistentCollectionDescriptor getCollectionDescriptor() {
		return collectionDescriptor;
	}

	@Override
	public Object generateCacheKey(Object id, PersistentCollectionDescriptor persister, SessionFactoryImplementor factory, String tenantIdentifier) {
		return getRegion().getEffectiveKeysFactory().createCollectionKey( id, persister, factory, tenantIdentifier );
	}

	@Override
	public Object getCacheKeyId(Object cacheKey) {
		return getRegion().getEffectiveKeysFactory().getCollectionId( cacheKey );
	}

	@Override
	public SoftLock lockItem(SharedSessionContractImplementor session, Object key, Object version) {
		return null;
	}

	@Override
	public void unlockItem(SharedSessionContractImplementor session, Object key, SoftLock lock) {

	}

	@Override
	public SoftLock lockRegion() {
		return null;
	}

	@Override
	public void unlockRegion(SoftLock lock) {
		clearCache();
	}
}
