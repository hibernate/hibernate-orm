/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.proxy.pojo.bytebuddy;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.proxy.ProxyConfiguration;
import org.hibernate.proxy.pojo.BasicLazyInitializer;
import org.hibernate.type.CompositeType;

import static org.hibernate.internal.CoreLogging.messageLogger;
import static org.hibernate.internal.util.ReflectHelper.isPublic;

public class ByteBuddyInterceptor
		extends BasicLazyInitializer
		implements ProxyConfiguration.Interceptor {

	private static final CoreMessageLogger LOG = messageLogger( ByteBuddyInterceptor.class );

	private final Class<?>[] interfaces;

	public ByteBuddyInterceptor(
			String entityName,
			Class<?> persistentClass,
			Class<?>[] interfaces,
			Object id,
			Method getIdentifierMethod,
			Method setIdentifierMethod,
			CompositeType componentIdType,
			SharedSessionContractImplementor session,
			boolean overridesEquals) {
		super( entityName, persistentClass, id, getIdentifierMethod, setIdentifierMethod, componentIdType, session, overridesEquals );
		this.interfaces = interfaces;
	}

	@Override
	public Object intercept(Object proxy, Method method, Object[] args) throws Throwable {
		final Object result = this.invoke( method, args, proxy );
		if ( result == INVOKE_IMPLEMENTATION ) {
			final Object target = getImplementation();
			final Object returnValue;
			try {
				if ( isPublic( persistentClass, method ) ) {
					if ( !method.getDeclaringClass().isInstance( target ) ) {
						throw new ClassCastException(
								target.getClass().getName()
										+ " incompatible with "
										+ method.getDeclaringClass().getName()
						);
					}
					returnValue = method.invoke( target, args );
				}
				else {
					method.setAccessible( true );
					returnValue = method.invoke( target, args );
				}

				if ( returnValue == target ) {
					final var returnValueClass = returnValue.getClass();
					if ( returnValueClass.isInstance( proxy ) ) {
						return proxy;
					}
					else {
						LOG.narrowingProxy( returnValueClass );
					}
				}
				return returnValue;
			}
			catch (InvocationTargetException ite) {
				throw ite.getTargetException();
			}
		}
		else {
			return result;
		}
	}

	@Override
	protected Object serializableProxy() {
		return new SerializableProxy(
				getEntityName(),
				persistentClass,
				interfaces,
				getInternalIdentifier(),
				isReadOnlySettingAvailable()
						? Boolean.valueOf( isReadOnly() )
						: isReadOnlyBeforeAttachedToSession(),
				getSessionFactoryUuid(),
				getSessionFactoryName(),
				isAllowLoadOutsideTransaction(),
				getIdentifierMethod,
				setIdentifierMethod,
				componentIdType
		);
	}
}
