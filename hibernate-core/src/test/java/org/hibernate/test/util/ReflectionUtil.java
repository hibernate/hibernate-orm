package org.hibernate.test.util;

import java.lang.reflect.Field;

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
}
