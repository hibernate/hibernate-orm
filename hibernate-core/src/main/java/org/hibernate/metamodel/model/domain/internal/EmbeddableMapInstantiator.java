/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.model.domain.spi.EmbeddedTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class EmbeddableMapInstantiator extends AbstractMapInstantiator {
	public EmbeddableMapInstantiator(EmbeddedTypeDescriptor runtimeDescriptor) {
		super( runtimeDescriptor.getNavigableRole() );
	}

	@Override
	public Object instantiate(SharedSessionContractImplementor session) {
		return instantiateMap( session );
	}

	@Override
	protected boolean isInstanceByTypeValue(String extractedType) {
		return getNavigableRole().getFullPath().equals( extractedType );
	}
}
