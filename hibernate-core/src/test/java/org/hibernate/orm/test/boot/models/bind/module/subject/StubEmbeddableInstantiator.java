/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.bind.module.subject;

import org.hibernate.metamodel.spi.EmbeddableInstantiator;
import org.hibernate.metamodel.spi.ValueAccess;

public class StubEmbeddableInstantiator implements EmbeddableInstantiator {
	@Override
	public Object instantiate(ValueAccess valueAccess) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isInstance(Object object) {
		return object instanceof StubEmbeddable;
	}

	@Override
	public boolean isSameClass(Object object) {
		return StubEmbeddable.class.equals( object.getClass() );
	}
}
