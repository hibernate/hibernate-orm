/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.cache.spi.support;

import org.hibernate.cache.cfg.spi.EntityDataCachingConfig;
import org.hibernate.cache.spi.CacheKeysFactory;
import org.hibernate.cache.spi.DomainDataRegion;
import org.hibernate.cache.spi.SecondLevelCacheLogger;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

import org.jboss.logging.Logger;

/**
 * Standard support for {@link org.hibernate.cache.spi.access.EntityDataAccess}
 * using the {@link org.hibernate.cache.spi.access.AccessType#READ_ONLY} access type.
 *
 * @author Steve Ebersole
 */
public class EntityReadOnlyAccess extends AbstractEntityDataAccess {
	private static final Logger log = Logger.getLogger( EntityReadOnlyAccess.class );

	public EntityReadOnlyAccess(
			DomainDataRegion region,
			CacheKeysFactory cacheKeysFactory,
			DomainDataStorageAccess storageAccess,
			EntityDataCachingConfig config) {
		super( region, cacheKeysFactory, storageAccess );
		if ( config.isMutable() ) {
			SecondLevelCacheLogger.INSTANCE.readOnlyCachingMutableEntity( config.getNavigableRole() );
		}
	}

	@Override
	public AccessType getAccessType() {
		return AccessType.READ_ONLY;
	}

	@Override
	public boolean insert(SharedSessionContractImplementor session, Object key, Object value, Object version) {
		// wait until tx complete - see `#afterInsert`
		return false;
	}

	@Override
	public boolean afterInsert(SharedSessionContractImplementor session, Object key, Object value, Object version) {
		getStorageAccess().putIntoCache( key, value, session );
		return true;
	}

	@Override
	public void unlockItem(SharedSessionContractImplementor session, Object key, SoftLock lock) {
		evict( key );
	}

	@Override
	public boolean update(
			SharedSessionContractImplementor session,
			Object key,
			Object value,
			Object currentVersion,
			Object previousVersion) {
		log.debugf( "Illegal attempt to update item cached as read-only [%s]", key );
		throw new UnsupportedOperationException( "Can't update readonly object" );
	}

	@Override
	public boolean afterUpdate(
			SharedSessionContractImplementor session,
			Object key,
			Object value,
			Object currentVersion,
			Object previousVersion,
			SoftLock lock) {
		log.debugf( "Illegal attempt to update item cached as read-only [%s]", key );
		throw new UnsupportedOperationException( "Can't write to a readonly object" );
	}
}
