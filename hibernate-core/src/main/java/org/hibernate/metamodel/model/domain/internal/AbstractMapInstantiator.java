/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.model.domain.spi.Instantiator;
import org.hibernate.metamodel.model.domain.spi.NavigableRole;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractMapInstantiator implements Instantiator {
	public static final String KEY = "$type$";

	private final NavigableRole navigableRole;

	public AbstractMapInstantiator(NavigableRole navigableRole) {
		this.navigableRole = navigableRole;
	}

	protected NavigableRole getNavigableRole() {
		return navigableRole;
	}

	protected Map instantiateMap(SharedSessionContractImplementor session) {
		Map map = generateMap();
		if ( navigableRole != null ) {
			map.put( KEY, navigableRole.getFullPath() );
		}
		return map;
	}

	protected Map generateMap() {
		return new HashMap();
	}

	@Override
	public boolean isInstance(Object object, SharedSessionContractImplementor session) {
		if ( !Map.class.isInstance( object ) ) {
			return false;
		}

		final String extractedType = (String) ( (Map) object ).get( KEY );
		if ( extractedType == null ) {
			// we just do not know - safe to assume it is
			return true;
		}

		return isInstanceByTypeValue( extractedType );
	}

	protected abstract boolean isInstanceByTypeValue(String extractedType);
}
