/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal.mode;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.metamodel.model.domain.spi.Instantiator;

/**
 * @author Steve Ebersole
 */
public class DynamicMapInstantiator implements Instantiator<Map> {
	public static final String KEY = "$type$";

	private final NavigableRole navigableRole;

	public DynamicMapInstantiator(NavigableRole navigableRole) {
		this.navigableRole = navigableRole;
	}

	@Override
	public Map instantiate(SharedSessionContractImplementor session) {
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
	public boolean isInstance(Object object, SessionFactoryImplementor sessionFactory) {
		return false;
	}
}
