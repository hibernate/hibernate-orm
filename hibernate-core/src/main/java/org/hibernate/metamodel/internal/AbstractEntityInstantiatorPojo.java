/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.internal;


import org.hibernate.bytecode.enhance.spi.interceptor.LazyAttributeLoadingInterceptor;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.metamodel.spi.EntityInstantiator;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.descriptor.java.JavaType;

import static org.hibernate.engine.internal.ManagedTypeHelper.asPersistentAttributeInterceptable;
import static org.hibernate.engine.internal.ManagedTypeHelper.isPersistentAttributeInterceptableType;

/**
 * Base support for instantiating entity values as POJO representation
 *
 * @author Steve Ebersole
 */
public abstract class AbstractEntityInstantiatorPojo extends AbstractPojoInstantiator implements EntityInstantiator {

	private final Class<?> proxyInterface;
	private final boolean applyBytecodeInterception;
	private final LazyAttributeLoadingInterceptor.EntityRelatedState loadingInterceptorState;

	public AbstractEntityInstantiatorPojo(
			EntityPersister persister,
			PersistentClass persistentClass,
			JavaType<?> javaType) {
		super( javaType.getJavaTypeClass() );
		proxyInterface = persistentClass.getProxyInterface();
		//TODO this PojoEntityInstantiator appears to not be reused ?!
		applyBytecodeInterception =
				isPersistentAttributeInterceptableType( persistentClass.getMappedClass() );
		if ( applyBytecodeInterception ) {
			loadingInterceptorState = new LazyAttributeLoadingInterceptor.EntityRelatedState(
					persister.getEntityName(),
					persister.getBytecodeEnhancementMetadata()
						.getLazyAttributesMetadata()
						.getLazyAttributeNames()
			);
		}
		else {
			loadingInterceptorState = null;
		}
	}

	protected Object applyInterception(Object entity) {
		if ( applyBytecodeInterception ) {
			asPersistentAttributeInterceptable( entity )
					.$$_hibernate_setInterceptor( new LazyAttributeLoadingInterceptor(
							loadingInterceptorState,
							null,
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

	/*
	 * Used by Hibernate Reactive
	 */
	protected boolean isApplyBytecodeInterception() {
		return applyBytecodeInterception;
	}

	/*
	 * Used by Hibernate Reactive
	 */
	protected LazyAttributeLoadingInterceptor.EntityRelatedState getLoadingInterceptorState() {
		return loadingInterceptorState;
	}
}
