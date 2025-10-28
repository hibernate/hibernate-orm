/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.proxy.pojo;

import java.lang.reflect.Method;

import org.hibernate.LazyInitializationException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.MarkerObject;
import org.hibernate.proxy.AbstractLazyInitializer;
import org.hibernate.type.CompositeType;

import static java.lang.System.identityHashCode;

/**
 * Lazy initializer for plain Java objects.
 *
 * @author Gavin King
 */
public abstract class BasicLazyInitializer extends AbstractLazyInitializer {

	protected static final Object INVOKE_IMPLEMENTATION = new MarkerObject( "INVOKE_IMPLEMENTATION" );

	protected final Class<?> persistentClass;
	protected final Method getIdentifierMethod;
	protected final Method setIdentifierMethod;
	protected final boolean overridesEquals;
	protected final CompositeType componentIdType;

	private Object replacement;

	protected BasicLazyInitializer(
			String entityName,
			Class<?> persistentClass,
			Object id,
			Method getIdentifierMethod,
			Method setIdentifierMethod,
			CompositeType componentIdType,
			SharedSessionContractImplementor session,
			boolean overridesEquals) {
		super( entityName, id, session );
		this.persistentClass = persistentClass;
		this.getIdentifierMethod = getIdentifierMethod;
		this.setIdentifierMethod = setIdentifierMethod;
		this.componentIdType = componentIdType;
		this.overridesEquals = overridesEquals;
	}

	protected abstract Object serializableProxy();

	protected final Object invoke(Method method, Object[] args, Object proxy) throws Throwable {
		final String methodName = method.getName();
		switch ( args.length ) {
			case 0:
				if ( "writeReplace".equals( methodName ) ) {
					return getReplacement();
				}
				else if ( !overridesEquals && "hashCode".equals( methodName ) ) {
					return identityHashCode( proxy );
				}
				else if ( isUninitialized() && method.equals( getIdentifierMethod ) ) {
					return getIdentifier();
				}
				else if ( "getHibernateLazyInitializer".equals( methodName ) ) {
					return this;
				}
				break;
			case 1:
				if ( !overridesEquals && "equals".equals( methodName ) ) {
					return args[0] == proxy;
				}
				else if ( method.equals( setIdentifierMethod ) ) {
					initialize();
					setIdentifier( args[0] );
					return INVOKE_IMPLEMENTATION;
				}
				break;
		}

		//if it is a property of an embedded component, invoke on the "identifier"
		if ( componentIdType != null && componentIdType.isMethodOf( method ) ) {
			return method.invoke( getIdentifier(), args );
		}

		// otherwise:
		return INVOKE_IMPLEMENTATION;
	}

	private Object getReplacement() {
		// If the target has already been loaded somewhere, just not
		// set on the proxy, then use it to initialize the proxy so
		// that we will serialize that instead of the proxy.
		initializeWithoutLoadIfPossible();

		if ( isUninitialized() ) {
			if ( replacement == null ) {
				prepareForPossibleLoadingOutsideTransaction();
				replacement = serializableProxy();
			}
			return replacement;
		}
		else {
			return getTarget();
		}
	}

	@Override
	public final Class<?> getPersistentClass() {
		return persistentClass;
	}

	@Override
	public Class<?> getImplementationClass() {
		if ( !isUninitialized() ) {
			return getImplementation().getClass();
		}
		else if ( getSession() == null ) {
			throw new LazyInitializationException( "could not retrieve real entity class ["
							+ getEntityName() + "#" + getInternalIdentifier() + "] - no Session" );
		}
		else {
			return getEntityDescriptor().hasSubclasses()
					? getImplementation().getClass()
					: persistentClass;
		}
	}

}
