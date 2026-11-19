package org.hibernate.orm.integrationtest.java.module.test.annotation;

import org.hibernate.metamodel.spi.EmbeddableInstantiator;
import org.hibernate.metamodel.spi.ValueAccess;
import org.jspecify.annotations.NonNull;

public class StubEmbeddableInstantiator implements EmbeddableInstantiator {
	@Override
	public Object instantiate(@NonNull ValueAccess valueAccess) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isInstance(Object object) {
		return object instanceof StubEmbeddable;
	}

	@Override
	public boolean isSameClass(@NonNull Object object) {
		return StubEmbeddable.class.equals( object.getClass() );
	}
}
