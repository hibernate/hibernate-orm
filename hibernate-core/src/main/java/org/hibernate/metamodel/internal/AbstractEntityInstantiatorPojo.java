/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.internal;


import org.hibernate.bytecode.enhance.spi.interceptor.LazyAttributeLoadingInterceptor;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.metamodel.spi.EntityInstantiator;
import org.hibernate.tuple.entity.EntityMetamodel;
import org.hibernate.type.descriptor.java.JavaType;

import static org.hibernate.engine.internal.ManagedTypeHelper.asPersistentAttributeInterceptable;
import static org.hibernate.engine.internal.ManagedTypeHelper.isPersistentAttributeInterceptableType;

/**
 * Base support for instantiating entity values as POJO representation
 *
 * @author Steve Ebersole
 */
public abstract class AbstractEntityInstantiatorPojo extends AbstractPojoInstantiator implements EntityInstantiator {
	private final EntityMetamodel entityMetamodel;
	private final Class<?> proxyInterface;
	private final boolean applyBytecodeInterception;

	public AbstractEntityInstantiatorPojo(
			EntityMetamodel entityMetamodel,
			PersistentClass persistentClass,
			JavaType<?> javaType) {
		super( javaType.getJavaTypeClass() );
		this.entityMetamodel = entityMetamodel;
		this.proxyInterface = persistentClass.getProxyInterface();
		//TODO this PojoEntityInstantiator appears to not be reused ?!
		this.applyBytecodeInterception =
				isPersistentAttributeInterceptableType( persistentClass.getMappedClass() );
	}

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
			|| proxyInterface!=null && proxyInterface.isInstance(object);
	}
}
