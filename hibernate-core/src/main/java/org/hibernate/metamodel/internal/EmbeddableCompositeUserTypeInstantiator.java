/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.internal;

import org.hibernate.metamodel.spi.EmbeddableInstantiator;
import org.hibernate.metamodel.spi.ValueAccess;
import org.hibernate.usertype.CompositeUserType;

/**
 * @author Christian Beikov
 */
public class EmbeddableCompositeUserTypeInstantiator implements EmbeddableInstantiator {

	private final CompositeUserType<Object> userType;

	public EmbeddableCompositeUserTypeInstantiator(CompositeUserType<Object> userType) {
		this.userType = userType;
	}

	@Override
	public Object instantiate(ValueAccess valuesAccess) {
		return userType.instantiate( valuesAccess );
	}

	@Override
	public boolean isInstance(Object object) {
		return userType.returnedClass().isInstance( object );
	}

	@Override
	public boolean isSameClass(Object object) {
		return object.getClass().equals( userType.returnedClass() );
	}
}
