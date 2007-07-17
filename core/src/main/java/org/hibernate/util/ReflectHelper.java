//$Id: ReflectHelper.java 10109 2006-07-12 15:39:59Z steve.ebersole@jboss.com $
package org.hibernate.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.hibernate.AssertionFailure;
import org.hibernate.MappingException;
import org.hibernate.PropertyNotFoundException;
import org.hibernate.property.BasicPropertyAccessor;
import org.hibernate.property.DirectPropertyAccessor;
import org.hibernate.property.Getter;
import org.hibernate.property.PropertyAccessor;
import org.hibernate.type.PrimitiveType;
import org.hibernate.type.Type;


public final class ReflectHelper {

	//TODO: this dependency is kinda Bad
	private static final PropertyAccessor BASIC_PROPERTY_ACCESSOR = new BasicPropertyAccessor();
	private static final PropertyAccessor DIRECT_PROPERTY_ACCESSOR = new DirectPropertyAccessor();

	private static final Class[] NO_CLASSES = new Class[0];
	private static final Class[] OBJECT = new Class[] { Object.class };
	private static final Method OBJECT_EQUALS;
	private static final Class[] NO_PARAM = new Class[] { };

	private static final Method OBJECT_HASHCODE;
	static {
		Method eq;
		Method hash;
		try {
			eq = Object.class.getMethod("equals", OBJECT);
			hash = Object.class.getMethod("hashCode", NO_PARAM);
		}
		catch (Exception e) {
			throw new AssertionFailure("Could not find Object.equals() or Object.hashCode()", e);
		}
		OBJECT_EQUALS = eq;
		OBJECT_HASHCODE = hash;
	}

	public static boolean overridesEquals(Class clazz) {
		Method equals;
		try {
			equals = clazz.getMethod("equals", OBJECT);
		}
		catch (NoSuchMethodException nsme) {
			return false; //its an interface so we can't really tell anything...
		}
		return !OBJECT_EQUALS.equals(equals);
	}

	public static boolean overridesHashCode(Class clazz) {
		Method hashCode;
		try {
			hashCode = clazz.getMethod("hashCode", NO_PARAM);
		}
		catch (NoSuchMethodException nsme) {
			return false; //its an interface so we can't really tell anything...
		}
		return !OBJECT_HASHCODE.equals(hashCode);
	}

	public static Class reflectedPropertyClass(String className, String name) throws MappingException {
		try {
			Class clazz = ReflectHelper.classForName(className);
			return getter(clazz, name).getReturnType();
		}
		catch (ClassNotFoundException cnfe) {
			throw new MappingException("class " + className + " not found while looking for property: " + name, cnfe);
		}
	}

	private static Getter getter(Class clazz, String name) throws MappingException {
		try {
			return BASIC_PROPERTY_ACCESSOR.getGetter(clazz, name);
		}
		catch (PropertyNotFoundException pnfe) {
			return DIRECT_PROPERTY_ACCESSOR.getGetter(clazz, name);
		}
	}

	public static Getter getGetter(Class theClass, String name) throws MappingException {
		return BASIC_PROPERTY_ACCESSOR.getGetter(theClass, name);
	}

	public static Class classForName(String name) throws ClassNotFoundException {
		try {
			ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
			if ( contextClassLoader != null ) {
				return contextClassLoader.loadClass(name);
			}
		}
		catch ( Throwable t ) {
		}
		return Class.forName( name );
	}

	public static Class classForName(String name, Class caller) throws ClassNotFoundException {
		try {
			ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
			if ( contextClassLoader != null ) {
				return contextClassLoader.loadClass( name );
			}
		}
		catch ( Throwable e ) {
		}
		return Class.forName( name, true, caller.getClassLoader() );
	}

	public static boolean isPublic(Class clazz, Member member) {
		return Modifier.isPublic( member.getModifiers() ) && Modifier.isPublic( clazz.getModifiers() );
	}

	public static Object getConstantValue(String name) {
		Class clazz;
		try {
			clazz = classForName( StringHelper.qualifier( name ) );
		}
		catch ( Throwable t ) {
			return null;
		}
		try {
			return clazz.getField( StringHelper.unqualify( name ) ).get(null);
		}
		catch ( Throwable t ) {
			return null;
		}
	}

	public static Constructor getDefaultConstructor(Class clazz) throws PropertyNotFoundException {

		if ( isAbstractClass(clazz) ) return null;

		try {
			Constructor constructor = clazz.getDeclaredConstructor(NO_CLASSES);
			if ( !isPublic(clazz, constructor) ) {
				constructor.setAccessible(true);
			}
			return constructor;
		}
		catch (NoSuchMethodException nme) {
			throw new PropertyNotFoundException(
				"Object class " + clazz.getName() +
				" must declare a default (no-argument) constructor"
			);
		}

	}

	public static boolean isAbstractClass(Class clazz) {
		int modifier = clazz.getModifiers();
		return Modifier.isAbstract(modifier) || Modifier.isInterface(modifier);
	}
	
	public static boolean isFinalClass(Class clazz) {
		return Modifier.isFinal( clazz.getModifiers() );
	}

	public static Constructor getConstructor(Class clazz, Type[] types) throws PropertyNotFoundException {
		final Constructor[] candidates = clazz.getConstructors();
		for ( int i=0; i<candidates.length; i++ ) {
			final Constructor constructor = candidates[i];
			final Class[] params = constructor.getParameterTypes();
			if ( params.length==types.length ) {
				boolean found = true;
				for ( int j=0; j<params.length; j++ ) {
					final boolean ok = params[j].isAssignableFrom( types[j].getReturnedClass() ) || (
						types[j] instanceof PrimitiveType &&
						params[j] == ( (PrimitiveType) types[j] ).getPrimitiveClass()
					);
					if (!ok) {
						found = false;
						break;
					}
				}
				if (found) {
					if ( !isPublic(clazz, constructor) ) constructor.setAccessible(true);
					return constructor;
				}
			}
		}
		throw new PropertyNotFoundException( "no appropriate constructor in class: " + clazz.getName() );
	}
	
	public static Method getMethod(Class clazz, Method method) {
		try {
			return clazz.getMethod( method.getName(), method.getParameterTypes() );
		}
		catch (Exception e) {
			return null;
		}
	}

	private ReflectHelper() {}

}
