/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.embeddable.strategy.instantiator.embedded;

import org.hibernate.metamodel.spi.EmbeddableInstantiator;
import org.hibernate.metamodel.spi.ValueAccess;

/**
 * @author Steve Ebersole
 */
//tag::embeddable-instantiator-impl[]
public class NameInstantiator implements EmbeddableInstantiator {
	@Override
	public Object instantiate(ValueAccess valueAccess) {
		// alphabetical
		final String first = valueAccess.getValue( 0, String.class );
		final String last = valueAccess.getValue( 1, String.class );
		return new Name( first, last );
	}

	// ...

//end::embeddable-instantiator-impl[]

	@Override
	public boolean isInstance(Object object) {
		return object instanceof Name;
	}

	@Override
	public boolean isSameClass(Object object) {
		return object.getClass().equals( Name.class );
	}
//tag::embeddable-instantiator-impl[]
}
//end::embeddable-instantiator-impl[]
