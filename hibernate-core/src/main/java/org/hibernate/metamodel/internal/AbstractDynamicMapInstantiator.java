/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.internal;

import java.util.HashMap;
import java.util.Map;

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
			throw new IllegalArgumentException( "Role name passed to dynamic map instantiator cannot be null" );
		}
		this.roleName = roleName;
	}

	public String getRoleName() {
		return roleName;
	}

	@Override
	public boolean isInstance(Object object) {
		return object instanceof Map<?, ?> map
			&& isSameRole( (String) map.get( TYPE_KEY ) );
		// todo (6.0) : should this be an exception if there is no TYPE_KEY
	}

	protected boolean isSameRole(String type) {
		return roleName.equals( type );
	}

	@Override
	public boolean isSameClass(Object object) {
		return isInstance( object );
	}

	protected Map<String,?> generateDataMap() {
		final Map<String,Object> map = new HashMap<>();
		map.put( TYPE_KEY, roleName );
		return map;
	}
}
