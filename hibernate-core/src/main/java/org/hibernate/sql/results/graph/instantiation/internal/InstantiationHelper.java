/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.graph.instantiation.internal;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.internal.util.beans.BeanInfoHelper;
import org.hibernate.type.spi.TypeConfiguration;
import org.jboss.logging.Logger;

import java.beans.BeanInfo;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.List;

import static org.hibernate.query.sqm.tree.expression.Compatibility.areAssignmentCompatible;

/**
 * @author Steve Ebersole
 * @author Gavin King
 */
public class InstantiationHelper {

	private static final Logger LOG = Logger.getLogger( InstantiationHelper.class );

	private InstantiationHelper() {
		// disallow direct instantiation
	}

	public static boolean isInjectionCompatible(Class<?> targetJavaType, List<String> aliases, List<Class<?>> argTypes) {
		return BeanInfoHelper.visitBeanInfo(
				targetJavaType,
				beanInfo -> {
					for ( int i = 0; i < aliases.size(); i++ ) {
						final String alias = aliases.get(i);
						final Class<?> argType = argTypes.get(i);
						if ( !checkArgument( targetJavaType, beanInfo, alias, argType ) ) {
							return false;
						}
					}
					return true;
				}
		);
	}

	private static boolean checkArgument(Class<?> targetJavaType, BeanInfo beanInfo, String alias, Class<?> argType) {
		for ( PropertyDescriptor propertyDescriptor : beanInfo.getPropertyDescriptors() ) {
			if ( propertyMatches( alias, argType, propertyDescriptor ) ) {
				return true;
			}
		}
		return findField( targetJavaType, alias, argType ) != null;
	}

	public static boolean isConstructorCompatible(Class<?> javaClass, List<Class<?>> argTypes, TypeConfiguration typeConfiguration) {
		return findMatchingConstructor( javaClass, argTypes, typeConfiguration ) != null;
	}

	public static <T> @Nullable Constructor<T> findMatchingConstructor(
			Class<T> type,
			List<Class<?>> argumentTypes,
			TypeConfiguration typeConfiguration) {
		for ( final Constructor<?> constructor : type.getDeclaredConstructors() ) {
			if ( isConstructorCompatible( constructor, argumentTypes, typeConfiguration ) ) {
				//noinspection unchecked
				return (Constructor<T>) constructor;
			}
		}
		return null;
	}

	public static boolean isConstructorCompatible(
			Constructor<?> constructor,
			List<Class<?>> argumentTypes,
			TypeConfiguration typeConfiguration) {
		final var genericParameterTypes = constructor.getGenericParameterTypes();
		if ( genericParameterTypes.length == argumentTypes.size() ) {
			for (int i = 0; i < argumentTypes.size(); i++ ) {
				final Type parameterType = genericParameterTypes[i];
				final var argumentType = argumentTypes.get( i );
				final var type =
						parameterType instanceof Class<?> classParameter
								? classParameter
								: typeConfiguration.getJavaTypeRegistry().getDescriptor( parameterType )
										.getJavaTypeClass();
				if ( !areAssignmentCompatible( type, argumentType ) ) {
					if ( LOG.isDebugEnabled() ) {
						LOG.debugf(
								"Skipping constructor for dynamic-instantiation match due to argument mismatch [%s] : %s -> %s",
								i,
								argumentType.getTypeName(),
								parameterType.getTypeName()
						);
					}
					return false;
				}
			}
			return true;
		}
		else {
			return false;
		}
	}

	static Field findField(Class<?> declaringClass, String name, Class<?> javaType) {
		try {
			final Field field = declaringClass.getDeclaredField( name );
			// field should never be null
			if ( areAssignmentCompatible( field.getType(), javaType ) ) {
				field.setAccessible( true );
				return field;
			}
		}
		catch (NoSuchFieldException ignore) {
			if ( declaringClass.getSuperclass() != null ) {
				return findField( declaringClass.getSuperclass(), name, javaType );
			}
		}

		return null;
	}

	static boolean propertyMatches(String alias, Class<?> argType, PropertyDescriptor propertyDescriptor) {
		return alias.equals( propertyDescriptor.getName() )
			&& propertyDescriptor.getWriteMethod() != null
			&& areAssignmentCompatible( propertyDescriptor.getWriteMethod().getParameterTypes()[0], argType );
	}
}
