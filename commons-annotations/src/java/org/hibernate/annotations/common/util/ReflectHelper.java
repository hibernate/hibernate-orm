//$Id: $
package org.hibernate.annotations.common.util;

import java.lang.reflect.Method;
import java.lang.reflect.Member;
import java.lang.reflect.Modifier;

import org.hibernate.annotations.common.AssertionFailure;

/**
 * @author Emmanuel Bernard
 */

public final class ReflectHelper {

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

	public static boolean isAbstractClass(Class clazz) {
		int modifier = clazz.getModifiers();
		return Modifier.isAbstract(modifier) || Modifier.isInterface(modifier);
	}

	public static boolean isFinalClass(Class clazz) {
		return Modifier.isFinal( clazz.getModifiers() );
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

