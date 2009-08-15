/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
package org.hibernate.annotations.common.util;

import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.hibernate.annotations.common.AssertionFailure;

/**
 * Complete duplication of {@link org.hibernate.util.ReflectHelper}.
 *
 * @author Emmanuel Bernard
 */
public final class ReflectHelper {

	public static final Class[] NO_PARAM_SIGNATURE = new Class[0];
	public static final Object[] NO_PARAMS = new Object[0];

	public static final Class[] SINGLE_OBJECT_PARAM_SIGNATURE = new Class[] { Object.class };

	private static final Method OBJECT_EQUALS;
	private static final Method OBJECT_HASHCODE;

	static {
		Method eq;
		Method hash;
		try {
			eq = extractEqualsMethod( Object.class );
			hash = extractHashCodeMethod( Object.class );
		}
		catch ( Exception e ) {
			throw new AssertionFailure( "Could not find Object.equals() or Object.hashCode()", e );
		}
		OBJECT_EQUALS = eq;
		OBJECT_HASHCODE = hash;
	}

	/**
	 * Disallow instantiation of ReflectHelper.
	 */
	private ReflectHelper() {
	}

	/**
	 * Encapsulation of getting hold of a class's {@link Object#equals equals}  method.
	 *
	 * @param clazz The class from which to extract the equals method.
	 * @return The equals method reference
	 * @throws NoSuchMethodException Should indicate an attempt to extract equals method from interface.
	 */
	public static Method extractEqualsMethod(Class clazz) throws NoSuchMethodException {
		return clazz.getMethod( "equals", SINGLE_OBJECT_PARAM_SIGNATURE );
	}

	/**
	 * Encapsulation of getting hold of a class's {@link Object#hashCode hashCode} method.
	 *
	 * @param clazz The class from which to extract the hashCode method.
	 * @return The hashCode method reference
	 * @throws NoSuchMethodException Should indicate an attempt to extract hashCode method from interface.
	 */
	public static Method extractHashCodeMethod(Class clazz) throws NoSuchMethodException {
		return clazz.getMethod( "hashCode", NO_PARAM_SIGNATURE );
	}

	/**
	 * Determine if the given class defines an {@link Object#equals} override.
	 *
	 * @param clazz The class to check
	 * @return True if clazz defines an equals override.
	 */
	public static boolean overridesEquals(Class clazz) {
		Method equals;
		try {
			equals = extractEqualsMethod( clazz );
		}
		catch ( NoSuchMethodException nsme ) {
			return false; //its an interface so we can't really tell anything...
		}
		return !OBJECT_EQUALS.equals( equals );
	}

	/**
	 * Determine if the given class defines a {@link Object#hashCode} override.
	 *
	 * @param clazz The class to check
	 * @return True if clazz defines an hashCode override.
	 */
	public static boolean overridesHashCode(Class clazz) {
		Method hashCode;
		try {
			hashCode = extractHashCodeMethod( clazz );
		}
		catch ( NoSuchMethodException nsme ) {
			return false; //its an interface so we can't really tell anything...
		}
		return !OBJECT_HASHCODE.equals( hashCode );
	}

	/**
	 * Perform resolution of a class name.
	 * <p/>
	 * Here we first check the context classloader, if one, before delegating to
	 * {@link Class#forName(String, boolean, ClassLoader)} using the caller's classloader
	 *
	 * @param name The class name
	 * @param caller The class from which this call originated (in order to access that class's loader).
	 * @return The class reference.
	 * @throws ClassNotFoundException From {@link Class#forName(String, boolean, ClassLoader)}.
	 */
	public static Class classForName(String name, Class caller) throws ClassNotFoundException {
		try {
			ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
			if ( contextClassLoader != null ) {
				return contextClassLoader.loadClass( name );
			}
		}
		catch ( Throwable ignore ) {
		}
		return Class.forName( name, true, caller.getClassLoader() );
	}

	/**
	 * Perform resolution of a class name.
	 * <p/>
	 * Same as {@link #classForName(String, Class)} except that here we delegate to
	 * {@link Class#forName(String)} if the context classloader lookup is unsuccessful.
	 *
	 * @param name The class name
	 * @return The class reference.
	 * @throws ClassNotFoundException From {@link Class#forName(String)}.
	 */
	public static Class classForName(String name) throws ClassNotFoundException {
		try {
			ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
			if ( contextClassLoader != null ) {
				return contextClassLoader.loadClass(name);
			}
		}
		catch ( Throwable ignore ) {
		}
		return Class.forName( name );
	}

	/**
	 * Is this member publicly accessible.
	 *
	 * @param clazz The class which defines the member
	 * @param member The memeber.
	 * @return True if the member is publicly accessible, false otherwise.
	 */
	public static boolean isPublic(Class clazz, Member member) {
		return Modifier.isPublic( member.getModifiers() ) && Modifier.isPublic( clazz.getModifiers() );
	}

	/**
	 * Resolve a constant to its actual value.
	 *
	 * @param name The name
	 * @return The value
	 */
	public static Object getConstantValue(String name) {
		Class clazz;
		try {
			clazz = classForName( StringHelper.qualifier( name ) );
		}
		catch ( Throwable t ) {
			return null;
		}
		try {
			return clazz.getField( StringHelper.unqualify( name ) ).get( null );
		}
		catch ( Throwable t ) {
			return null;
		}
	}

	/**
	 * Determine if the given class is declared abstract.
	 *
	 * @param clazz The class to check.
	 * @return True if the class is abstract, false otherwise.
	 */
	public static boolean isAbstractClass(Class clazz) {
		int modifier = clazz.getModifiers();
		return Modifier.isAbstract(modifier) || Modifier.isInterface(modifier);
	}

	/**
	 * Determine is the given class is declared final.
	 *
	 * @param clazz The class to check.
	 * @return True if the class is final, flase otherwise.
	 */
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

}

