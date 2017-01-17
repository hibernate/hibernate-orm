/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.internal;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;

import org.hibernate.persister.common.spi.ManagedTypeImplementor;
import org.hibernate.type.spi.Type;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class JavaTypeHelper {
	private static final Logger log = Logger.getLogger( JavaTypeHelper.class );

	private JavaTypeHelper() {
	}

	public static Member resolveAttributeMember(
			ManagedTypeImplementor managedType,
			String attributeName,
			Type attributeType) {
		if ( managedType == null ) {
			return null;
		}

		return resolveAttributeMember( managedType.getJavaType(), attributeName, attributeType );
	}

	public static Member resolveAttributeMember(Class javaType, String name, Type type) {
		if ( javaType == null ) {
			// means we have a de-typed (EntityMode.MAP) managed type...
			// there is no Member
			return null;
		}

		final Method method = locateMethod( javaType, name, type );
		if ( method != null ) {
			return null;
		}

		final Field field = locateField( javaType, name, type );
		if ( field != null ) {
			return field;
		}

		throw new RuntimeException(
				String.format(
						"Could not resolve AttributeMember : %s.%s(%s)",
						javaType.getName(),
						name,
						type.getJavaType() == null ? "?" : javaType
				)
		);

	}

	@SuppressWarnings("unchecked")
	private static Field locateField(Class declaringJavaType, String name, Type type) {
		assert declaringJavaType != null;

		try {
			final Field field = declaringJavaType.getField( name );
			if ( field != null ) {
				if ( type.getJavaType() != null ) {
					if ( type.getJavaType().isAssignableFrom( field.getType() ) ) {
						return field;
					}
				}
			}
		}
		catch (NoSuchFieldException e) {
			log.debugf( "NoSuchFieldException" );
		}

		log.debugf( "Could not find field named [%s] typed [%s]", name, type.getJavaType() );
		return null;
	}

	@SuppressWarnings("unchecked")
	private static Method locateMethod(Class javaType, String name, Type type) {
		try {
			final BeanInfo beanInfo = Introspector.getBeanInfo( javaType );
			for ( PropertyDescriptor propertyDescriptor : beanInfo.getPropertyDescriptors() ) {
				if ( !name.equals( propertyDescriptor.getName() ) ) {
					continue;
				}

				if ( type.getJavaType() != null ) {
					if ( !type.getJavaType().isAssignableFrom( propertyDescriptor.getReadMethod().getReturnType() ) ) {
						continue;
					}
				}
				return propertyDescriptor.getReadMethod();
			}
		}
		catch (IntrospectionException e) {
			log.debugf( "Error using java.beans.Introspector : " + e.getMessage() );
		}

		log.debugf( "Could not find getter named [%s] typed [%s]", name, type.getJavaType() );
		return null;
	}
}
