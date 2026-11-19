package org.hibernate.metamodel.internal;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.metamodel.spi.EmbeddableInstantiator;
import org.hibernate.metamodel.spi.ValueAccess;
import org.hibernate.usertype.CompositeUserType;

/**
 * @author Christian Beikov
 */
public class EmbeddableCompositeUserTypeInstantiator implements EmbeddableInstantiator {

	private final CompositeUserType<Object> userType;

	public EmbeddableCompositeUserTypeInstantiator(@Nonnull CompositeUserType<Object> userType) {
		this.userType = userType;
	}

	@Override
	@Nullable
	public Object instantiate(@Nonnull ValueAccess valuesAccess) {
		return userType.instantiate( valuesAccess );
	}

	@Override
	public boolean isInstance(@Nullable Object object) {
		return userType.returnedClass().isInstance( object );
	}

	@Override
	public boolean isSameClass(@Nonnull Object object) {
		return object.getClass().equals( userType.returnedClass() );
	}
}
