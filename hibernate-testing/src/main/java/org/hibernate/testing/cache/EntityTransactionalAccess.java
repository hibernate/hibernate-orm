/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.cache;

import org.hibernate.cache.cfg.spi.EntityDataCachingConfig;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * @author Steve Ebersole
 */
public class EntityTransactionalAccess extends BaseEntityDataAccess {
	public EntityTransactionalAccess(DomainDataRegionImpl region, EntityDataCachingConfig entityAccessConfig) {
		super( region );
	}

	@Override
	public boolean insert(
			SharedSessionContractImplementor session,
			Object key,
			Object value,
			Object version) {
		addToCache( key, value );
		return true;
	}

	@Override
	public boolean afterInsert(
			SharedSessionContractImplementor session,
			Object key,
			Object value,
			Object version) {
		return false;
	}

	@Override
	public boolean update(
			SharedSessionContractImplementor session,
			Object key,
			Object value,
			Object currentVersion,
			Object previousVersion) {
		addToCache( key, value );
		return true;
	}

	@Override
	public boolean afterUpdate(
			SharedSessionContractImplementor session,
			Object key,
			Object value,
			Object currentVersion,
			Object previousVersion,
			SoftLock lock) {
		return false;
	}
}
