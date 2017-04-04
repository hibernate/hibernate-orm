package org.hibernate.test.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.function.Supplier;

/**
 * @author Vlad Mihalcea
 */
public class ReflectionUtil {

	/**
	 * Get a field from a given class
	 * @param clazz clazz
	 * @param name field name
	 * @return field object
	 */
	public static Field getField(Class clazz, String name) {
		try {
			Field field = clazz.getDeclaredField( name );
			field.setAccessible( true );
			return field;
		}
		catch ( NoSuchFieldException e ) {
			throw new IllegalArgumentException( "Class " + clazz + " does not contain a " + name + " field", e);
		}
	}

	/**
	 * Get a field value from a given object
	 * @param target Object whose field is being read
	 * @param name field name
	 * @return field object
	 */
	public static <T> T getFieldValue(Object target, String name) {
		try {
			Field field = target.getClass().getDeclaredField( name );
			field.setAccessible( true );
			return (T) field.get( target );
		}
		catch ( NoSuchFieldException e ) {
			throw new IllegalArgumentException( "Class " + target.getClass() + " does not contain a " + name + " field", e);
		}
		catch ( IllegalAccessException e ) {
			throw new IllegalArgumentException( "Cannot set field " + name, e);
		}
	}

	/**
	 * Set target Object field to a certain value
	 * @param target Object whose field is being set
	 * @param field Object field to set
	 * @param value the new value for the given field
	 */
	public static void setField(Object target, Field field, Object value) {
		try {
			field.set( target, value );
		}
		catch ( IllegalAccessException e ) {
			throw new IllegalArgumentException("Field " + field + " could not be set",  e );
		}
	}

	/**
	 * New target Object instance using the given arguments
	 * @param constructorSupplier constructor supplier
	 * @param args Constructor arguments
	 * @return new Object instance
	 */
	public static <T> T newInstance(Supplier<Constructor<T>> constructorSupplier, Object... args) {
		try {
			Constructor constructor  = constructorSupplier.get();
			constructor.setAccessible( true );
			return (T) constructor.newInstance( args );
		}
		catch ( IllegalAccessException | InstantiationException | InvocationTargetException e ) {
			throw new IllegalArgumentException("Constructor could not be called",  e );
		}
	}

	/**
	 * Set target Object field to a certain value
	 * @param target Object whose field is being set
	 * @param fieldName Object field naem to set
	 * @param value the new value for the given field
	 */
	public static void setField(Object target, String fieldName, Object value) {
		try {
			Field field = getField(target.getClass(), fieldName);
			field.set( target, value );
		}
		catch ( IllegalAccessException e ) {
			throw new IllegalArgumentException("Field " + fieldName + " could not be set",  e );
		}
	}
}
