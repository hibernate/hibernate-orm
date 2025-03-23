/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.internal;

import java.lang.reflect.Constructor;

import org.hibernate.InstantiationException;
import org.hibernate.PropertyNotFoundException;
import org.hibernate.bytecode.enhance.spi.interceptor.LazyAttributeLoadingInterceptor;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.tuple.entity.EntityMetamodel;
import org.hibernate.type.descriptor.java.JavaType;

import static org.hibernate.engine.internal.ManagedTypeHelper.asPersistentAttributeInterceptable;
import static org.hibernate.engine.internal.ManagedTypeHelper.isPersistentAttributeInterceptableType;
import static org.hibernate.internal.util.ReflectHelper.getDefaultConstructor;

/**
 * Support for instantiating entity values as POJO representation
 */
public class EntityInstantiatorPojoStandard extends AbstractEntityInstantiatorPojo {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( EntityInstantiatorPojoStandard.class );

	private final EntityMetamodel entityMetamodel;
	private final Class<?> proxyInterface;
	private final boolean applyBytecodeInterception;

	private final Constructor<?> constructor;

	public EntityInstantiatorPojoStandard(
			EntityMetamodel entityMetamodel,
			PersistentClass persistentClass,
			JavaType<?> javaType) {
		super( entityMetamodel, persistentClass, javaType );
		this.entityMetamodel = entityMetamodel;
		proxyInterface = persistentClass.getProxyInterface();
		constructor = isAbstract() ? null : resolveConstructor( getMappedPojoClass() );
		applyBytecodeInterception = isPersistentAttributeInterceptableType( persistentClass.getMappedClass() );
	}

	protected static Constructor<?> resolveConstructor(Class<?> mappedPojoClass) {
		try {
			return getDefaultConstructor( mappedPojoClass);
		}
		catch ( PropertyNotFoundException e ) {
			LOG.noDefaultConstructor( mappedPojoClass.getName() );
			return null;
		}
	}

	@Override
	public boolean canBeInstantiated() {
		return constructor != null;
	}

	@Override
	protected Object applyInterception(Object entity) {
		if ( applyBytecodeInterception ) {
			asPersistentAttributeInterceptable( entity )
					.$$_hibernate_setInterceptor( new LazyAttributeLoadingInterceptor(
							entityMetamodel.getName(),
							null,
							entityMetamodel.getBytecodeEnhancementMetadata()
									.getLazyAttributesMetadata()
									.getLazyAttributeNames(),
							null
					) );
		}
		return entity;

	}

	@Override
	public boolean isInstance(Object object) {
		return super.isInstance( object )
			// this one needed only for guessEntityMode()
			|| proxyInterface != null && proxyInterface.isInstance( object );
	}

	@Override
	public Object instantiate() {
		if ( isAbstract() ) {
			throw new InstantiationException( "Cannot instantiate abstract class or interface", getMappedPojoClass() );
		}
		else if ( constructor == null ) {
			throw new InstantiationException( "No default constructor for entity", getMappedPojoClass() );
		}
		else {
			try {
				return applyInterception( constructor.newInstance( (Object[]) null ) );
			}
			catch ( Exception e ) {
				throw new InstantiationException( "Could not instantiate entity", getMappedPojoClass(), e );
			}
		}
	}
}
