package org.hibernate.metamodel.internal;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.metamodel.spi.Instantiator;

import static org.hibernate.internal.util.ReflectHelper.isAbstractClass;

/**
 * Base support for POJO-based instantiation
 *
 * @author Steve Ebersole
 */
public abstract class AbstractPojoInstantiator implements Instantiator {
	private final Class<?> mappedPojoClass;
	private final boolean isAbstract;

	public AbstractPojoInstantiator(@Nonnull Class<?> mappedPojoClass) {
		this.mappedPojoClass = mappedPojoClass;
		this.isAbstract = isAbstractClass( mappedPojoClass );
	}

	@Nonnull
	public Class<?> getMappedPojoClass() {
		return mappedPojoClass;
	}

	public boolean isAbstract() {
		return isAbstract;
	}

	@Override
	public boolean isInstance(@Nullable Object object) {
		return mappedPojoClass.isInstance( object );
	}

	@Override
	public boolean isSameClass(@Nonnull Object object) {
		return object.getClass() == mappedPojoClass;
	}

}
