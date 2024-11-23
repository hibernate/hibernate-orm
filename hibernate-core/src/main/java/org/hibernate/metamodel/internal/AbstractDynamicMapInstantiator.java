/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.internal;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.spi.Instantiator;

/**
 * Base support for dynamic-map instantiators
 *
 * @author Steve Ebersole
 */
public abstract class AbstractDynamicMapInstantiator implements Instantiator {
	public static final String TYPE_KEY = "$type$";

	private final String roleName;

	public AbstractDynamicMapInstantiator(String roleName) {
		if ( roleName == null ) {
			throw new IllegalArgumentException( "`roleName` passed to dynamic-map instantiator cannot be null" );
		}
		this.roleName = roleName;
	}

	public String getRoleName() {
		return roleName;
	}

	@Override
	public boolean isInstance(Object object, SessionFactoryImplementor sessionFactory) {
		if ( object instanceof Map ) {
			//noinspection rawtypes
			final String type = (String) ( (Map) object ).get( TYPE_KEY );
			return isSameRole( type );
		}

		// todo (6.0) : should this be an exception instead?
		return false;
	}

	protected boolean isSameRole(String type) {
		return roleName.equals( type );
	}

	@Override
	public boolean isSameClass(Object object, SessionFactoryImplementor sessionFactory) {
		return isInstance( object, sessionFactory );
	}

	@SuppressWarnings("rawtypes")
	protected Map generateDataMap() {
		final Map map = new HashMap();
		//noinspection unchecked
		map.put( TYPE_KEY, roleName );
		return map;
	}
}
