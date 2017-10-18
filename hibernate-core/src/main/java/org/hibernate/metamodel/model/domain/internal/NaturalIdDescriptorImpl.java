/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal;

import java.util.List;

import org.hibernate.cache.spi.access.NaturalIdDataAccess;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.model.domain.spi.NaturalIdDescriptor;
import org.hibernate.metamodel.model.domain.spi.NonIdPersistentAttribute;

/**
 * @author Steve Ebersole
 */
public class NaturalIdDescriptorImpl implements NaturalIdDescriptor {
	private final NaturalIdDataAccess cacheRegionAccess;

	private List<NonIdPersistentAttribute> attributes;

	public NaturalIdDescriptorImpl(NaturalIdDataAccess cacheRegionAccess) {
		this.cacheRegionAccess = cacheRegionAccess;
	}

	public void injectAttributes(List<NonIdPersistentAttribute> attributes) {

	}

	@Override
	public List<NonIdPersistentAttribute> getPersistentAttributes() {
		return attributes;
	}

	@Override
	public boolean isMutable() {
		return false;
	}

	@Override
	public Object[] resolveSnapshot(Object entityId, SharedSessionContractImplementor session) {
		return new Object[0];
	}

	@Override
	public NaturalIdDataAccess getCacheAccess() {
		return cacheRegionAccess;
	}
}
