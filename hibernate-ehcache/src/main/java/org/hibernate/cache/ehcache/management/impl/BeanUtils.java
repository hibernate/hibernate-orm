/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */

package org.hibernate.cache.ehcache.management.impl;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Reflective utilities for dealing with backward-incompatible change to statistics types in Hibernate 3.5.
 *
 * @author gkeim
 */
public class BeanUtils {
	/**
	 * Return the named getter method on the bean or null if not found.
	 *
	 * @param bean The bean
	 * @param propertyName The property to get the getter for
	 *
	 * @return the named getter method
	 */
	private static Method getMethod(Object bean, String propertyName) {
		final StringBuilder sb = new StringBuilder( "get" ).append( Character.toUpperCase( propertyName.charAt( 0 ) ) );
		if ( propertyName.length() > 1 ) {
			sb.append( propertyName.substring( 1 ) );
		}
		final String getterName = sb.toString();
		for ( Method m : bean.getClass().getMethods() ) {
			if ( getterName.equals( m.getName() ) && m.getParameterTypes().length == 0 ) {
				return m;
			}
		}
		return null;
	}

	private static Field getField(Object bean, String propertyName) {
		for ( Field f : bean.getClass().getDeclaredFields() ) {
			if ( propertyName.equals( f.getName() ) ) {
				return f;
			}
		}
		return null;
	}

	private static void validateArgs(Object bean, String propertyName) {
		if ( bean == null ) {
			throw new IllegalArgumentException( "bean is null" );
		}
		if ( propertyName == null ) {
			throw new IllegalArgumentException( "propertyName is null" );
		}
		if ( propertyName.trim().length() == 0 ) {
			throw new IllegalArgumentException( "propertyName is empty" );
		}
	}

	/**
	 * Retrieve a named bean property value.
	 *
	 * @param bean The bean instance
	 * @param propertyName The name of the property whose value to extract
	 *
	 * @return the property value
	 */
	public static Object getBeanProperty(Object bean, String propertyName) {
		validateArgs( bean, propertyName );

		// try getters first
		final Method getter = getMethod( bean, propertyName );
		if ( getter != null ) {
			try {
				return getter.invoke( bean );
			}
			catch (Exception e) {
				/**/
			}
		}

		// then try fields
		final Field field = getField( bean, propertyName );
		if ( field != null ) {
			try {
				field.setAccessible( true );
				return field.get( bean );
			}
			catch (Exception e) {
				/**/
			}
		}

		return null;
	}

	/**
	 * Retrieve a Long bean property value.
	 *
	 * @param bean The bean instance
	 * @param propertyName The name of the property whose value to extract
	 *
	 * @return long value
	 *
	 * @throws NoSuchFieldException If the value is null (wow)
	 */
	public static long getLongBeanProperty(final Object bean, final String propertyName) throws NoSuchFieldException {
		validateArgs( bean, propertyName );
		final Object o = getBeanProperty( bean, propertyName );
		if ( o == null ) {
			throw new NoSuchFieldException( propertyName );
		}
		else if ( !(o instanceof Number) ) {
			throw new IllegalArgumentException( propertyName + " not an Number" );
		}
		return ((Number) o).longValue();
	}

	private BeanUtils() {
	}
}
