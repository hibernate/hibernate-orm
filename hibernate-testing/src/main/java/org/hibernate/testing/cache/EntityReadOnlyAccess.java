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
import org.hibernate.metamodel.model.domain.spi.EntityHierarchy;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("unused")
public class EntityReadOnlyAccess extends BaseEntityDataAccess {
	private static final Logger log = Logger.getLogger( EntityReadOnlyAccess.class );

	public EntityReadOnlyAccess(
			DomainDataRegionImpl region,
			EntityHierarchy entityHierarchy) {
		super( region, entityHierarchy );
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
		addToCache( key, value );
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
