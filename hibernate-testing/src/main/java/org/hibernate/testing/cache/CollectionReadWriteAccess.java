/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.cache;

import java.util.Comparator;

import org.hibernate.cache.cfg.spi.CollectionDataCachingConfig;
import org.hibernate.cache.spi.SecondLevelCacheLogger;
import org.hibernate.cache.spi.access.CollectionDataAccess;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.persister.collection.CollectionPersister;

/**
 * @author Chris Cranford
 */
public class CollectionReadWriteAccess extends AbstractReadWriteAccess implements CollectionDataAccess {
	private final NavigableRole collectionRole;
	private final Comparator versionComparator;

	public CollectionReadWriteAccess(
			DomainDataRegionImpl region,
			CollectionDataCachingConfig config) {
		super( region );
		this.collectionRole = config.getNavigableRole();
		this.versionComparator = config.getOwnerVersionComparator();
	}

	@Override
	protected SecondLevelCacheLogger.RegionAccessType getAccessType() {
		return SecondLevelCacheLogger.RegionAccessType.COLLECTION;
	}

	@Override
	public Object generateCacheKey(
			Object id,
			CollectionPersister collectionDescriptor,
			SessionFactoryImplementor factory,
			String tenantIdentifier) {
		return getRegion().getEffectiveKeysFactory().createCollectionKey( id, collectionDescriptor, factory, tenantIdentifier );
	}

	@Override
	public Object getCacheKeyId(Object cacheKey) {
		return getRegion().getEffectiveKeysFactory().getCollectionId( cacheKey );
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
