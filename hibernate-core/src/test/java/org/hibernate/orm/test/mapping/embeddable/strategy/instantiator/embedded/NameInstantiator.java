/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.embeddable.strategy.instantiator.embedded;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.spi.EmbeddableInstantiator;
import org.hibernate.metamodel.spi.ValueAccess;

/**
 * @author Steve Ebersole
 */
//tag::embeddable-instantiator-impl[]
public class NameInstantiator implements EmbeddableInstantiator {
	@Override
	public Object instantiate(ValueAccess valueAccess, SessionFactoryImplementor sessionFactory) {
		// alphabetical
		final String first = valueAccess.getValue( 0, String.class );
		final String last = valueAccess.getValue( 1, String.class );
		return new Name( first, last );
	}

	// ...

//end::embeddable-instantiator-impl[]

	@Override
	public boolean isInstance(Object object, SessionFactoryImplementor sessionFactory) {
		return object instanceof Name;
	}

	@Override
	public boolean isSameClass(Object object, SessionFactoryImplementor sessionFactory) {
		return object.getClass().equals( Name.class );
	}
//tag::embeddable-instantiator-impl[]
}
//end::embeddable-instantiator-impl[]
