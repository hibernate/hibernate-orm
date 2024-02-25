/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.instantiation.internal;

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

	private static final Logger log = Logger.getLogger( InstantiationHelper.class );

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
		for ( Constructor<?> constructor : javaClass.getDeclaredConstructors() ) {
			if ( isConstructorCompatible( constructor, argTypes, typeConfiguration) ) {
				return true;
			}
		}
		return false;
	}

	public static boolean isConstructorCompatible(
			Constructor<?> constructor,
			List<Class<?>> argumentTypes,
			TypeConfiguration typeConfiguration) {
		final Type[] genericParameterTypes = constructor.getGenericParameterTypes();
		if ( genericParameterTypes.length == argumentTypes.size() ) {
			for (int i = 0; i < argumentTypes.size(); i++ ) {
				final Type parameterType = genericParameterTypes[i];
				final Class<?> argumentType = argumentTypes.get( i );
				final Class<?> type = parameterType instanceof Class<?>
						? (Class<?>) parameterType
						: typeConfiguration.getJavaTypeRegistry().resolveDescriptor( parameterType ).getJavaTypeClass();

				if ( !areAssignmentCompatible( type, argumentType ) ) {
					if ( log.isDebugEnabled() ) {
						log.debugf(
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
