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

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("unused")
public class EntityNonStrictReadWriteAccess extends BaseEntityDataAccess {
	public EntityNonStrictReadWriteAccess(DomainDataRegionImpl domainDataRegion, EntityHierarchy entityHierarchy) {
		super( domainDataRegion, entityHierarchy );
	}

	@Override
	public AccessType getAccessType() {
		return AccessType.NONSTRICT_READ_WRITE;
	}

	@Override
	public boolean insert(
			SharedSessionContractImplementor session,
			Object key,
			Object value,
			Object version) {
		return false;
	}

	@Override
	public boolean afterInsert(SharedSessionContractImplementor session, Object key, Object value, Object version) {
		return false;
	}

	@Override
	public boolean update(
			SharedSessionContractImplementor session,
			Object key,
			Object value,
			Object currentVersion,
			Object previousVersion) {
		evict( key );
		return false;
	}

	@Override
	public boolean afterUpdate(
			SharedSessionContractImplementor session,
			Object key,
			Object value,
			Object currentVersion,
			Object previousVersion,
			SoftLock lock) {
		unlockItem( session, key, lock );
		return false;
	}
}
