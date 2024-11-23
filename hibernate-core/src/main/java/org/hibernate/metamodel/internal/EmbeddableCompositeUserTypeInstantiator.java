/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.internal;

import org.hibernate.engine.spi.SessionFactoryImplementor;
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
	public Object instantiate(ValueAccess valuesAccess, SessionFactoryImplementor sessionFactory) {
		return userType.instantiate( valuesAccess, sessionFactory );
	}

	@Override
	public boolean isInstance(Object object, SessionFactoryImplementor sessionFactory) {
		return userType.returnedClass().isInstance( object );
	}

	@Override
	public boolean isSameClass(Object object, SessionFactoryImplementor sessionFactory) {
		return object.getClass().equals( userType.returnedClass() );
	}
}
