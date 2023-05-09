/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.property.access.internal;

import java.beans.Introspector;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Locale;

import org.hibernate.MappingException;
import org.hibernate.PropertyNotFoundException;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Transient;

import static org.hibernate.internal.util.ReflectHelper.NO_PARAM_SIGNATURE;
import static org.hibernate.internal.util.ReflectHelper.findField;
import static org.hibernate.internal.util.ReflectHelper.isRecord;

/**
 * @author Steve Ebersole
 */
public class AccessStrategyHelper {
	public static Field fieldOrNull(Class<?> containerJavaType, String propertyName) {
		try {
			return findField( containerJavaType, propertyName );
		}
		catch (PropertyNotFoundException e) {
			return null;
		}
	}

	public static AccessType getAccessType(Class<?> containerJavaType, String propertyName) {
		final Field field = fieldOrNull( containerJavaType, propertyName );
		final AccessType explicitAccessType = getExplicitAccessType( containerJavaType, propertyName, field );
		if ( explicitAccessType != null ) {
			return explicitAccessType;
		}

		// No @Access on property or field; check to see if containerJavaType has an explicit @Access
		AccessType classAccessType = getAccessTypeOrNull( containerJavaType );
		if ( classAccessType != null ) {
			return classAccessType;
		}

		// prefer using the field for getting if we can
		return field != null ? AccessType.FIELD : AccessType.PROPERTY;
	}

	public static AccessType getExplicitAccessType(Class<?> containerClass, String propertyName, Field field) {
		if ( isRecord( containerClass ) ) {
			try {
				containerClass.getMethod( propertyName, NO_PARAM_SIGNATURE );
				return AccessType.PROPERTY;
			}
			catch (NoSuchMethodException e) {
				// Ignore
			}
		}

		if ( field != null
				&& field.isAnnotationPresent( Access.class )
				&& !field.isAnnotationPresent( Transient.class )
				&& !Modifier.isStatic( field.getModifiers() ) ) {
			return AccessType.FIELD;
		}

		for ( Method method : containerClass.getDeclaredMethods() ) {
			// if the method has parameters, skip it
			if ( method.getParameterCount() != 0 ) {
				continue;
			}

			// if the method is a "bridge", skip it
			if ( method.isBridge() ) {
				continue;
			}

			if ( method.isAnnotationPresent( Transient.class ) ) {
				continue;
			}

			if ( Modifier.isStatic( method.getModifiers() ) ) {
				continue;
			}

			final String methodName = method.getName();

			// try "get"
			if ( methodName.startsWith( "get" ) ) {
				final String stemName = methodName.substring( 3 );
				final String decapitalizedStemName = Introspector.decapitalize( stemName );
				if ( stemName.equals( propertyName ) || decapitalizedStemName.equals( propertyName ) ) {
					if ( method.isAnnotationPresent( Access.class ) ) {
						return AccessType.PROPERTY;
					}
					else {
						checkIsMethodVariant( containerClass, propertyName, method, stemName );
					}
				}
			}

			// if not "get", then try "is"
			if ( methodName.startsWith( "is" ) ) {
				final String stemName = methodName.substring( 2 );
				String decapitalizedStemName = Introspector.decapitalize( stemName );
				if ( stemName.equals( propertyName ) || decapitalizedStemName.equals( propertyName ) ) {
					if ( method.isAnnotationPresent( Access.class ) ) {
						return AccessType.PROPERTY;
					}
				}
			}
		}

		return null;
	}

	private static void checkIsMethodVariant(
			Class<?> containerClass,
			String propertyName,
			Method method,
			String stemName) {
		final Method isMethodVariant = findIsMethodVariant( containerClass, stemName );
		if ( isMethodVariant == null ) {
			return;
		}

		if ( !isMethodVariant.isAnnotationPresent( Access.class ) ) {
			throw new MappingException(
					String.format(
							Locale.ROOT,
							"In trying to locate getter for property [%s], Class [%s] defined " +
									"both a `get` [%s] and `is` [%s] variant",
							propertyName,
							containerClass.getName(),
							method.toString(),
							isMethodVariant.toString()
					)
			);
		}
	}

	public static Method findIsMethodVariant(Class<?> containerClass, String stemName) {
		// verify that the Class does not also define a method with the same stem name with 'is'
		try {
			final Method isMethod = containerClass.getDeclaredMethod( "is" + stemName );
			if ( !Modifier.isStatic( isMethod.getModifiers() ) && isMethod.getAnnotation( Transient.class ) == null ) {
				return isMethod;
			}
		}
		catch (NoSuchMethodException ignore) {
		}

		return null;
	}

	protected static AccessType getAccessTypeOrNull(AnnotatedElement element) {
		if ( element == null ) {
			return null;
		}
		Access elementAccess = element.getAnnotation( Access.class );
		return elementAccess == null ? null : elementAccess.value();
	}
}
