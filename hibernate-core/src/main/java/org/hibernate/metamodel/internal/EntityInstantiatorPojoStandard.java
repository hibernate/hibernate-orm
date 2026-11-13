package org.hibernate.metamodel.internal;

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
			EntityPersister persister,
			PersistentClass persistentClass,
			JavaType<?> javaType,
			AccessorFactory accessorFactory) {
		super( persister, persistentClass, javaType );
		final var constructor = isAbstract() ? null : resolveConstructor( getMappedPojoClass() );
		instantiator = constructor == null ? null : accessorFactory.instantiator( constructor );
	}

	protected static Constructor<?> resolveConstructor(Class<?> mappedPojoClass) {
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
