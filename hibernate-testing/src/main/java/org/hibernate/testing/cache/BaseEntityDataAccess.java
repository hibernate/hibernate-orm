/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.cache;

import org.hibernate.cache.spi.access.EntityDataAccess;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.cache.spi.support.DomainDataRegionImpl;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.model.domain.spi.EntityHierarchy;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("WeakerAccess")
public abstract class BaseEntityDataAccess
		extends AbstractCachedDomainDataAccess
		implements EntityDataAccess {
	private final EntityHierarchy entityHierarchy;

	public BaseEntityDataAccess(DomainDataRegionImpl region, EntityHierarchy entityHierarchy) {
		super( region );
		this.entityHierarchy = entityHierarchy;
	}

	@SuppressWarnings("unused")
	protected EntityHierarchy getEntityHierarchy() {
		return entityHierarchy;
	}

	@Override
	public Object generateCacheKey(
			Object id,
			EntityHierarchy entityHierarchy,
			SessionFactoryImplementor factory,
			String tenantIdentifier) {
		return getRegion().getEffectiveKeysFactory().createEntityKey(
				id,
				entityHierarchy,
				factory,
				tenantIdentifier
		);
	}

	@Override
	public Object getCacheKeyId(Object cacheKey) {
		return getRegion().getEffectiveKeysFactory().getEntityId( cacheKey );
	}

	@Override
	public SoftLock lockRegion() {
		return null;
	}

	@Override
	public void unlockRegion(SoftLock lock) {
		clearCache();
	}

	public SoftLock lockItem(
			SharedSessionContractImplementor session,
			Object key,
			Object version) {
		return null;
	}

	@Override
	public void unlockItem(
			SharedSessionContractImplementor session,
			Object key,
			SoftLock lock) {
	}
}
