/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.property.access.spi;

import java.io.ObjectStreamException;
import java.io.Serial;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Map;

import org.hibernate.Internal;
import org.hibernate.PropertyAccessException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.internal.util.collections.ArrayHelper;

import org.checkerframework.checker.nullness.qual.Nullable;

import static org.hibernate.internal.CoreLogging.messageLogger;

/**
 * @author Steve Ebersole
 */
@Internal
public class GetterMethodImpl implements Getter {
	private static final CoreMessageLogger LOG = messageLogger( GetterMethodImpl.class );

	private final Class<?> containerClass;
	private final String propertyName;
	private final Method getterMethod;

	public GetterMethodImpl(Class<?> containerClass, String propertyName, Method getterMethod) {
		this.containerClass = containerClass;
		this.propertyName = propertyName;
		this.getterMethod = getterMethod;
	}

	@Override
	public @Nullable Object get(Object owner) {
		try {
			return getterMethod.invoke( owner, ArrayHelper.EMPTY_OBJECT_ARRAY );
		}
		catch (InvocationTargetException ite) {
			final Throwable cause = ite.getCause();
			if ( cause instanceof Error error ) {
				// HHH-16403 Don't wrap Error
				throw error;
			}
			throw new PropertyAccessException(
					cause,
					"Exception occurred inside",
					false,
					containerClass,
					propertyName
			);
		}
		catch (IllegalAccessException iae) {
			throw new PropertyAccessException(
					iae,
					"IllegalAccessException occurred while calling",
					false,
					containerClass,
					propertyName
			);
			//cannot occur
		}
		catch (IllegalArgumentException iae) {
			LOG.illegalPropertyGetterArgument( containerClass.getName(), propertyName );
			throw new PropertyAccessException(
					iae,
					"IllegalArgumentException occurred calling",
					false,
					containerClass,
					propertyName
			);
		}
	}

	@Override
	public @Nullable Object getForInsert(Object owner, Map<Object, Object> mergeMap, SharedSessionContractImplementor session) {
		return get( owner );
	}

	@Override
	public Class<?> getReturnTypeClass() {
		return getterMethod.getReturnType();
	}

	@Override
	public Type getReturnType() {
		return getterMethod.getGenericReturnType();
	}

	@Override
	public Member getMember() {
		return getterMethod;
	}

	@Override
	public String getMethodName() {
		return getterMethod.getName();
	}

	@Override
	public Method getMethod() {
		return getterMethod;
	}

	@Serial
	private Object writeReplace() throws ObjectStreamException {
		return new SerialForm( containerClass, propertyName, getterMethod );
	}

	private static class SerialForm implements Serializable {
		private final Class<?> containerClass;
		private final String propertyName;

		private final Class<?> declaringClass;
		private final String methodName;

		private SerialForm(Class<?> containerClass, String propertyName, Method method) {
			this.containerClass = containerClass;
			this.propertyName = propertyName;
			this.declaringClass = method.getDeclaringClass();
			this.methodName = method.getName();
		}

		@Serial
		private Object readResolve() {
			return new GetterMethodImpl( containerClass, propertyName, resolveMethod() );
		}

		private Method resolveMethod() {
			try {
				final Method method = declaringClass.getDeclaredMethod( methodName );
				ReflectHelper.ensureAccessibility( method );
				return method;
			}
			catch (NoSuchMethodException e) {
				throw new PropertyAccessSerializationException(
						"Unable to resolve getter method on deserialization: " + declaringClass.getName() + "#" + methodName
				);
			}
		}
	}
}
