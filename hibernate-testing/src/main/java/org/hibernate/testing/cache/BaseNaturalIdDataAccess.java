/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.cache;

import org.hibernate.cache.spi.access.NaturalIdDataAccess;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.cache.spi.support.DomainDataRegionImpl;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.model.domain.spi.EntityHierarchy;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("WeakerAccess")
public abstract class BaseNaturalIdDataAccess extends AbstractCachedDomainDataAccess implements NaturalIdDataAccess {
	private final EntityHierarchy entityHierarchy;

	public BaseNaturalIdDataAccess(
			DomainDataRegionImpl region,
			EntityHierarchy entityHierarchy) {
		super( region );
		this.entityHierarchy = entityHierarchy;
	}

	@SuppressWarnings("unused")
	public EntityHierarchy getEntityHierarchy() {
		return entityHierarchy;
	}

	@Override
	public Object generateCacheKey(
			Object[] naturalIdValues,
			EntityHierarchy entityHierarchy,
			SharedSessionContractImplementor session) {
		return getRegion().getEffectiveKeysFactory().createNaturalIdKey( naturalIdValues, entityHierarchy, session );
	}

	@Override
	public Object[] getNaturalIdValues(Object cacheKey) {
		return getRegion().getEffectiveKeysFactory().getNaturalIdValues( cacheKey );
	}




	@Override
	public boolean insert(SharedSessionContractImplementor session, Object key, Object value) {
		addToCache( key, value );
		return true;
	}

	@Override
	public boolean afterInsert(SharedSessionContractImplementor session, Object key, Object value) {
		return false;
	}

	@Override
	public boolean update(SharedSessionContractImplementor session, Object key, Object value) {
		addToCache( key, value );
		return true;
	}

	@Override
	public boolean afterUpdate(SharedSessionContractImplementor session, Object key, Object value, SoftLock lock) {
		return false;
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
