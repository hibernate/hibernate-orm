/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.testing.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.Supplier;

/**
 * @author Vlad Mihalcea
 */
public class ReflectionUtil {

	/**
	 * Get a field from a given class
	 *
	 * @param clazz clazz
	 * @param name field name
	 *
	 * @return field object
	 */
	public static Field getField(Class clazz, String name) {
		try {
			Field field = clazz.getDeclaredField( name );
			field.setAccessible( true );
			return field;
		}
		catch (NoSuchFieldException e) {
			Class superClass = clazz.getSuperclass();
			if ( !clazz.equals( superClass ) ) {
				return getField( superClass, name );
			}
			throw new IllegalArgumentException( "Class " + clazz + " does not contain a " + name + " field", e );
		}
	}

	/**
	 * Get a field value from a given object
	 *
	 * @param target Object whose field is being read
	 * @param name field name
	 *
	 * @return field object
	 */
	public static <T> T getFieldValue(Object target, String name) {
		try {
			Field field = target.getClass().getDeclaredField( name );
			field.setAccessible( true );
			return (T) field.get( target );
		}
		catch (NoSuchFieldException e) {
			throw new IllegalArgumentException(
					"Class " + target.getClass() + " does not contain a " + name + " field",
					e
			);
		}
		catch (IllegalAccessException e) {
			throw new IllegalArgumentException( "Cannot set field " + name, e );
		}
	}

	/**
	 * Get a field value from a given class
	 *
	 * @param target Class whose field is being read
	 * @param name field name
	 *
	 * @return field value
	 */
	public static <T> T getStaticFieldValue(Class<?> target, String name) {
		try {
			Field field = getField( target, name );
			return (T) field.get( null );
		}
		catch (IllegalAccessException e) {
			throw new IllegalArgumentException( "Cannot set field " + name, e );
		}
	}

	/**
	 * Set target Object field to a certain value
	 *
	 * @param target Object whose field is being set
	 * @param field Object field to set
	 * @param value the new value for the given field
	 */
	public static void setField(Object target, Field field, Object value) {
		try {
			field.set( target, value );
		}
		catch (IllegalAccessException e) {
			throw new IllegalArgumentException( "Field " + field + " could not be set", e );
		}
	}

	/**
	 * Set target Object field to a certain value
	 *
	 * @param target Object whose field is being set
	 * @param fieldName Object field naem to set
	 * @param value the new value for the given field
	 */
	public static void setField(Object target, String fieldName, Object value) {
		try {
			Field field = getField( target.getClass(), fieldName );
			field.set( target, value );
		}
		catch (IllegalAccessException e) {
			throw new IllegalArgumentException( "Field " + fieldName + " could not be set", e );
		}
	}

	/**
	 * Set target Class field to a certain value
	 *
	 * @param target Class whose field is being set
	 * @param fieldName Class field name to set
	 * @param value the new value for the given field
	 */
	public static void setStaticField(Class<?> target, String fieldName, Object value) {
		try {
			Field field = getField( target, fieldName );
			field.set( null, value );
		}
		catch (IllegalAccessException e) {
			throw new IllegalArgumentException( "Field " + fieldName + " could not be set", e );
		}
	}

	/**
	 * New target Object instance using the given arguments
	 *
	 * @param constructorSupplier constructor supplier
	 * @param args Constructor arguments
	 *
	 * @return new Object instance
	 */
	public static <T> T newInstance(Supplier<Constructor<T>> constructorSupplier, Object... args) {
		try {
			Constructor constructor = constructorSupplier.get();
			constructor.setAccessible( true );
			return (T) constructor.newInstance( args );
		}
		catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
			throw new IllegalArgumentException( "Constructor could not be called", e );
		}
	}

	/**
	 * New target Object instance using the given Class name
	 *
	 * @param className class name
	 *
	 * @return new Object instance
	 */
	public static <T> T newInstance(String className) {
		try {
			return (T) Class.forName( className ).newInstance();
		}
		catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
			throw new IllegalArgumentException( "Constructor could not be called", e );
		}
	}

	/**
	 * Get setter method
	 *
	 * @param target target object
	 * @param property property
	 * @param parameterType setter parameter type
	 *
	 * @return setter method
	 */
	public static Method getSetter(Object target, String property, Class<?> parameterType) {
		String setterMethodName = "set" + property.substring( 0, 1 ).toUpperCase() + property.substring( 1 );
		Method setter = getMethod( target, setterMethodName, parameterType );
		setter.setAccessible( true );
		return setter;
	}

	/**
	 * Get target method
	 *
	 * @param target target object
	 * @param methodName method name
	 * @param parameterTypes method parameter types
	 *
	 * @return return value
	 */
	public static Method getMethod(Object target, String methodName, Class... parameterTypes) {
		try {
			return target.getClass().getMethod( methodName, parameterTypes );
		}
		catch (NoSuchMethodException e) {
			throw new IllegalArgumentException( e );
		}
	}

	/**
	 * Invoke setter method with the given parameter
	 *
	 * @param target target object
	 * @param property property
	 * @param parameter setter parameter
	 */
	public static void setProperty(Object target, String property, Object parameter) {
		Method setter = getSetter( target, property, parameter.getClass() );
		try {
			setter.invoke( target, parameter );
		}
		catch (IllegalAccessException | InvocationTargetException e) {
			throw new IllegalArgumentException( e );
		}
	}
}
