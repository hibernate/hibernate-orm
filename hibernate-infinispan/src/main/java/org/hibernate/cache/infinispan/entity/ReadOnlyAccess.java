/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.infinispan.entity;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.infinispan.access.AccessDelegate;
import org.hibernate.cache.spi.DomainDataRegion;
import org.hibernate.cache.spi.access.EntityDataAccess;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.model.domain.spi.EntityHierarchy;
import org.hibernate.sql.NotYetImplementedException;

/**
 * A specialization of {@link ReadWriteAccess} that ensures we never update data.
 *
 * @author Chris Bredesen
 * @author Galder Zamarreño
 * @since 3.5
 */
class ReadOnlyAccess implements EntityDataAccess {

	protected final EntityRegionImpl region;
	protected final AccessDelegate delegate;

	ReadOnlyAccess(EntityRegionImpl region, AccessDelegate delegate) {
		this.region = region;
		this.delegate = delegate;
	}

	@Override
	public boolean contains(Object key) {
		throw new NotYetImplementedException(  );
	}

	public void evict(Object key) throws CacheException {
		delegate.evict( key );
	}

	public void evictAll() throws CacheException {
		delegate.evictAll();
	}

	@Override
	public Object get(SharedSessionContractImplementor session, Object key) {
		return delegate.get( session, key, session.getTransactionStartTimestamp() );
	}

	@Override
	public DomainDataRegion getRegion() {
		throw new NotYetImplementedException(  );
	}

	@Override
	public boolean putFromLoad(
			SharedSessionContractImplementor session,
			Object key,
			Object value,
			Object version) {
		return delegate.putFromLoad( session, key, value, session.getTransactionStartTimestamp(), version );
	}

	@Override
	public boolean putFromLoad(
			SharedSessionContractImplementor session,
			Object key,
			Object value,
			Object version,
			boolean minimalPutOverride) {
		return delegate.putFromLoad( session, key, value, session.getTransactionStartTimestamp(), version, minimalPutOverride );
	}

	public void remove(SharedSessionContractImplementor session, Object key) throws CacheException {
		delegate.remove ( session, key );
	}

	public void removeAll() throws CacheException {
		delegate.removeAll();
	}

	public boolean insert(SharedSessionContractImplementor session, Object key, Object value, Object version) throws CacheException {
		return delegate.insert( session, key, value, version );
	}

	@Override
	public boolean update(
			SharedSessionContractImplementor session, Object key, Object value, Object currentVersion,
			Object previousVersion) throws CacheException {
		throw new UnsupportedOperationException( "Illegal attempt to edit read only item" );
	}

	public SoftLock lockItem(SharedSessionContractImplementor session, Object key, Object version) throws CacheException {
		return null;
	}

	public SoftLock lockRegion() throws CacheException {
		return null;
	}

	public void unlockItem(SharedSessionContractImplementor session, Object key, SoftLock lock) throws CacheException {
		delegate.unlockItem( session, key );
	}

	public void unlockRegion(SoftLock lock) throws CacheException {
	}

	public boolean afterInsert(SharedSessionContractImplementor session, Object key, Object value, Object version) throws CacheException {
		return delegate.afterInsert( session, key, value, version );
	}

	@Override
	public boolean afterUpdate(
			SharedSessionContractImplementor session, Object key, Object value, Object currentVersion,
			Object previousVersion, SoftLock lock) throws CacheException {
		throw new UnsupportedOperationException( "Illegal attempt to edit read only item" );
	}

	@Override
	public Object generateCacheKey(
			Object id,
			EntityHierarchy entityHierarchy,
			SessionFactoryImplementor factory,
			String tenantIdentifier) {
		return region.getCacheKeysFactory().createEntityKey( id, entityHierarchy, factory, tenantIdentifier );
	}

	@Override
	public Object getCacheKeyId(Object cacheKey) {
		return region.getCacheKeysFactory().getEntityId(cacheKey);
	}
}
