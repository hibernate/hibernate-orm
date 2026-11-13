package org.hibernate.metamodel.internal;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.lang.reflect.Constructor;

import org.hibernate.InstantiationException;
import org.hibernate.PropertyNotFoundException;
import org.hibernate.accessor.AccessorFactory;
import org.hibernate.accessor.Instantiator;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.descriptor.java.JavaType;

import static org.hibernate.internal.CoreMessageLogger.CORE_LOGGER;
import static org.hibernate.internal.util.ReflectHelper.getDefaultConstructor;

/**
 * Support for instantiating entity values as POJO representation
 */
public class EntityInstantiatorPojoStandard extends AbstractEntityInstantiatorPojo {

	private final Instantiator<?> instantiator;

	public EntityInstantiatorPojoStandard(
			@Nonnull EntityPersister persister,
			@Nonnull PersistentClass persistentClass,
			@Nonnull JavaType<?> javaType,
			@Nonnull AccessorFactory accessorFactory) {
		super( persister, persistentClass, javaType );
		final var constructor = isAbstract() ? null : resolveConstructor( getMappedPojoClass() );
		instantiator = constructor == null ? null : accessorFactory.instantiator( constructor );
	}

	@Nullable
	protected static Constructor<?> resolveConstructor(@Nonnull Class<?> mappedPojoClass) {
		try {
			return getDefaultConstructor( mappedPojoClass );
		}
		catch ( PropertyNotFoundException e ) {
			CORE_LOGGER.noDefaultConstructor( mappedPojoClass.getName() );
			return null;
		}
	}

	@Override
	public boolean canBeInstantiated() {
		return instantiator != null;
	}

	@Override
	@Nonnull
	public Object instantiate() {
		if ( isAbstract() ) {
			throw new InstantiationException( "Cannot instantiate abstract class or interface", getMappedPojoClass() );
		}
		else if ( instantiator == null ) {
			throw new InstantiationException( "No default constructor for entity", getMappedPojoClass() );
		}
		else {
			try {
				return applyInterception( instantiator.create() );
			}
			catch ( Exception e ) {
				throw new InstantiationException( "Could not instantiate entity", getMappedPojoClass(), e );
			}
		}
	}
}
