/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.cache.spi.support;

import java.util.Comparator;

import org.hibernate.cache.cfg.spi.CollectionDataCachingConfig;
import org.hibernate.cache.spi.CacheKeysFactory;
import org.hibernate.cache.spi.DomainDataRegion;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cache.spi.access.CollectionDataAccess;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.persister.collection.CollectionPersister;
/**
 * Standard support for {@link org.hibernate.cache.spi.access.CollectionDataAccess}
 * using the {@link org.hibernate.cache.spi.access.AccessType#READ_WRITE} access type.
 *
 * @author Chris Cranford
 * @author Steve Ebersole
 */
public class CollectionReadWriteAccess extends AbstractReadWriteAccess implements CollectionDataAccess {
	private final NavigableRole collectionRole;
	private final Comparator versionComparator;
	private final CacheKeysFactory keysFactory;

	public CollectionReadWriteAccess(
			DomainDataRegion region,
			CacheKeysFactory keysFactory,
			DomainDataStorageAccess storageAccess,
			CollectionDataCachingConfig config) {
		super( region, storageAccess );
		this.keysFactory = keysFactory;
		this.collectionRole = config.getNavigableRole();
		this.versionComparator = config.getOwnerVersionComparator();
	}

	@Override
	protected AccessedDataClassification getAccessedDataClassification() {
		return AccessedDataClassification.COLLECTION;
	}

	@Override
	public AccessType getAccessType() {
		return AccessType.READ_WRITE;
	}

	@Override
	public Object generateCacheKey(
			Object id,
			CollectionPersister collectionDescriptor,
			SessionFactoryImplementor factory,
			String tenantIdentifier) {
		return keysFactory.createCollectionKey( id, collectionDescriptor, factory, tenantIdentifier );
	}

	@Override
	public Object getCacheKeyId(Object cacheKey) {
		return keysFactory.getCollectionId( cacheKey );
	}

	@Override
	protected Comparator getVersionComparator() {
		return versionComparator;
	}

	@Override
	public Object get(SharedSessionContractImplementor session, Object key) {
		return super.get( session, key );
	}

	@Override
	public boolean putFromLoad(SharedSessionContractImplementor session, Object key, Object value, Object version) {
		return super.putFromLoad( session, key, value, version );
	}

	@Override
	public SoftLock lockItem(
			SharedSessionContractImplementor session, Object key, Object version) {
		return super.lockItem( session, key, version );
	}

	@Override
	public void unlockItem(
			SharedSessionContractImplementor session, Object key, SoftLock lock) {
		super.unlockItem( session, key, lock );
	}
}
