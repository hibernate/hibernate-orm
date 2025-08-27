/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal.util;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Locale;
import java.util.function.Supplier;

import org.hibernate.AssertionFailure;
import org.hibernate.MappingException;
import org.hibernate.PropertyNotFoundException;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.internal.build.AllowReflection;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.property.access.internal.PropertyAccessStrategyGetterImpl;
import org.hibernate.property.access.spi.Getter;

import jakarta.persistence.Transient;

import static java.beans.Introspector.decapitalize;
import static java.lang.Thread.currentThread;

/**
 * Utility class for various reflection operations.
 *
 * @author Gavin King
 * @author Steve Ebersole
 * @author Chris Cranford
 */
public final class ReflectHelper {

	public static final Class<?>[] NO_PARAM_SIGNATURE = ArrayHelper.EMPTY_CLASS_ARRAY;

	public static final Class<?>[] SINGLE_OBJECT_PARAM_SIGNATURE = new Class[] { Object.class };

	private static final Method OBJECT_EQUALS;
	private static final Method OBJECT_HASHCODE;
	private static final Class<?> RECORD_CLASS;
	private static final Method GET_RECORD_COMPONENTS;
	private static final Method GET_NAME;
	private static final Method GET_TYPE;

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

		Class<?> recordClass = null;
		Method getRecordComponents = null;
		Method getName = null;
		Method getType = null;
		try {
			recordClass = Class.forName( "java.lang.Record" );
			getRecordComponents = Class.class.getMethod( "getRecordComponents" );
			final Class<?> recordComponentClass = Class.forName( "java.lang.reflect.RecordComponent" );
			getName = recordComponentClass.getMethod( "getName" );
			getType = recordComponentClass.getMethod( "getType" );
		}
		catch (Exception e) {
			// Ignore
		}
		RECORD_CLASS = recordClass;
		GET_RECORD_COMPONENTS = getRecordComponents;
		GET_NAME = getName;
		GET_TYPE = getType;
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
	public static Method extractEqualsMethod(Class<?> clazz) throws NoSuchMethodException {
		return clazz.getMethod( "equals", SINGLE_OBJECT_PARAM_SIGNATURE );
	}

	/**
	 * Encapsulation of getting hold of a class's {@link Object#hashCode hashCode} method.
	 *
	 * @param clazz The class from which to extract the hashCode method.
	 * @return The hashCode method reference
	 * @throws NoSuchMethodException Should indicate an attempt to extract hashCode method from interface.
	 */
	public static Method extractHashCodeMethod(Class<?> clazz) throws NoSuchMethodException {
		return clazz.getMethod( "hashCode", NO_PARAM_SIGNATURE );
	}

	/**
	 * Determine if the given class defines an {@link Object#equals} override.
	 *
	 * @param clazz The class to check
	 * @return True if clazz defines an equals override.
	 */
	public static boolean overridesEquals(Class<?> clazz) {
		if ( clazz.isRecord() || clazz.isEnum() ) {
			return true;
		}
		final Method equals;
		try {
			equals = extractEqualsMethod( clazz );
		}
		catch ( NoSuchMethodException nsme ) {
			return false; //it's an interface so we can't really tell anything
		}
		return !OBJECT_EQUALS.equals( equals );
	}

	/**
	 * Determine if the given class defines a {@link Object#hashCode} override.
	 *
	 * @param clazz The class to check
	 * @return True if clazz defines an hashCode override.
	 */
	public static boolean overridesHashCode(Class<?> clazz) {
		if ( clazz.isRecord() || clazz.isEnum() ) {
			return true;
		}
		final Method hashCode;
		try {
			hashCode = extractHashCodeMethod( clazz );
		}
		catch ( NoSuchMethodException nsme ) {
			return false; //it's an interface so we can't really tell anything
		}
		return !OBJECT_HASHCODE.equals( hashCode );
	}

	/**
	 * Determine if the given class implements the given interface.
	 *
	 * @param clazz The class to check
	 * @param intf The interface to check it against.
	 * @return True if the class does implement the interface, false otherwise.
	 *
	 * @deprecated No longer used
	 */
	@Deprecated(since = "7", forRemoval = true)
	public static boolean implementsInterface(Class<?> clazz, Class<?> intf) {
		assert intf.isInterface() : "Interface to check was not an interface";
		return intf.isAssignableFrom( clazz );
	}

	/**
	 * Perform resolution of a class name.
	 * <p>
	 * Here we first check the context classloader, if one, before delegating to
	 * {@link Class#forName(String, boolean, ClassLoader)} using the caller's classloader
	 *
	 * @param name The class name
	 * @param caller The class from which this call originated (in order to access that class's loader).
	 * @return The class reference.
	 * @throws ClassNotFoundException From {@link Class#forName(String, boolean, ClassLoader)}.
	 */
	public static Class<?> classForName(String name, Class<?> caller) throws ClassNotFoundException {
		try {
			final ClassLoader classLoader = currentThread().getContextClassLoader();
			if ( classLoader != null ) {
				return classLoader.loadClass( name );
			}
		}
		catch ( Throwable ignore ) {
		}
		return Class.forName( name, true, caller.getClassLoader() );
	}

	/**
	 * Perform resolution of a class name.
	 * <p>
	 * Same as {@link #classForName(String, Class)} except that here we delegate to
	 * {@link Class#forName(String)} if the context classloader lookup is unsuccessful.
	 *
	 * @param name The class name
	 * @return The class reference.
	 *
	 * @throws ClassNotFoundException From {@link Class#forName(String)}.
	 *
	 * @deprecated Depending on context, either {@link ClassLoaderService}
	 * or {@link org.hibernate.boot.spi.ClassLoaderAccess} should be preferred
	 */
	@Deprecated
	public static Class<?> classForName(String name) throws ClassNotFoundException {
		try {
			final ClassLoader classLoader = currentThread().getContextClassLoader();
			if ( classLoader != null ) {
				return classLoader.loadClass(name);
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
	 * @param member The member.
	 * @return True if the member is publicly accessible, false otherwise.
	 */
	public static boolean isPublic(Class<?> clazz, Member member) {
		return Modifier.isPublic( member.getModifiers() )
			&& Modifier.isPublic( clazz.getModifiers() );
	}

	/**
	 * Attempt to resolve the specified property type through reflection.
	 *
	 * @param className The name of the class owning the property.
	 * @param name The name of the property.
	 * @param classLoaderService ClassLoader services
	 *
	 * @return The type of the property.
	 *
	 * @throws MappingException Indicates we were unable to locate the property.
	 */
	public static Class<?> reflectedPropertyClass(
			String className,
			String name,
			ClassLoaderService classLoaderService) throws MappingException {
		try {
			final Class<?> clazz = classLoaderService.classForName( className );
			return getter( clazz, name ).getReturnTypeClass();
		}
		catch ( ClassLoadingException e ) {
			throw new MappingException( "class " + className + " not found while looking for property: " + name, e );
		}
	}

	public static java.lang.reflect.Type reflectedPropertyType(
			String className,
			String name,
			ClassLoaderService classLoaderService) throws MappingException {
		try {
			final Class<?> clazz = classLoaderService.classForName( className );
			return getter( clazz, name ).getReturnType();
		}
		catch ( ClassLoadingException e ) {
			throw new MappingException( "class " + className + " not found while looking for property: " + name, e );
		}
	}

	/**
	 * Attempt to resolve the specified property type through reflection.
	 *
	 * @param clazz The class owning the property.
	 * @param name The name of the property.
	 * @return The type of the property.
	 * @throws MappingException Indicates we were unable to locate the property.
	 */
	public static Class<?> reflectedPropertyClass(Class<?> clazz, String name) throws MappingException {
		return getter( clazz, name ).getReturnTypeClass();
	}

	private static Getter getter(Class<?> clazz, String name) throws MappingException {
		return PropertyAccessStrategyGetterImpl.INSTANCE.buildPropertyAccess( clazz, name, true ).getGetter();
	}

	/**
	 * Retrieve the default (no arg) constructor from the given class.
	 *
	 * @param clazz The class for which to retrieve the default ctor.
	 * @return The default constructor.
	 * @throws PropertyNotFoundException Indicates there was not publicly accessible, no-arg constructor (todo : why PropertyNotFoundException???)
	 */
	public static <T> Constructor<T> getDefaultConstructor(Class<T> clazz) throws PropertyNotFoundException {
		if ( isAbstractClass( clazz ) ) {
			return null;
		}

		try {
			final Constructor<T> constructor = clazz.getDeclaredConstructor( NO_PARAM_SIGNATURE );
			ensureAccessibility( constructor );
			return constructor;
		}
		catch ( NoSuchMethodException nme ) {
			throw new PropertyNotFoundException(
					"Object class [" + clazz.getName() + "] must declare a default (no-argument) constructor"
			);
		}
	}

	public static <T> Supplier<T> getDefaultSupplier(Class<T> clazz) {
		if ( isAbstractClass( clazz ) ) {
			throw new IllegalArgumentException( "Abstract class cannot be instantiated: " + clazz.getName() );
		}

		try {
			final Constructor<T> constructor = clazz.getDeclaredConstructor( NO_PARAM_SIGNATURE );
			ensureAccessibility( constructor );
			return () -> {
				try {
					return constructor.newInstance();
				}
				catch ( InstantiationException|IllegalAccessException|InvocationTargetException e ) {
					throw new org.hibernate.InstantiationException( "Constructor threw exception", clazz, e );
				}
			};
		}
		catch ( NoSuchMethodException nme ) {
			return () -> {
				try {
					return clazz.newInstance();
				}
				catch ( InstantiationException|IllegalAccessException e ) {
					throw new org.hibernate.InstantiationException( "Default constructor threw exception", clazz, e );
				}
			};
		}
	}

	/**
	 * Determine if the given class is declared abstract.
	 *
	 * @param clazz The class to check.
	 * @return True if the class is abstract, false otherwise.
	 */
	public static boolean isAbstractClass(Class<?> clazz) {
		final int modifier = clazz.getModifiers();
		return Modifier.isAbstract(modifier) || Modifier.isInterface(modifier);
	}

	/**
	 * Determine is the given class is declared final.
	 *
	 * @param clazz The class to check.
	 * @return True if the class is final, false otherwise.
	 */
	public static boolean isFinalClass(Class<?> clazz) {
		return Modifier.isFinal( clazz.getModifiers() );
	}

	/**
	 * Retrieve a constructor for the given class, with arguments matching
	 * the specified Java {@linkplain Class types}, or return {@code null}
	 * if no such constructor exists.
	 *
	 * @param clazz The class needing instantiation
	 * @param constructorArgs The types representing the required ctor param signature
	 * @return The matching constructor, or {@code null}
	 */
	public static <T> Constructor<T> getConstructorOrNull(
			Class<T> clazz,
			Class<?>... constructorArgs) {
		Constructor<T> constructor = null;
		try {
			constructor = clazz.getDeclaredConstructor( constructorArgs );
			try {
				ensureAccessibility( constructor );
			}
			catch ( SecurityException e ) {
				constructor = null;
			}
		}
		catch ( NoSuchMethodException ignore ) {
		}

		return constructor;
	}

	public static Method getMethod(Class<?> clazz, Method method) {
		try {
			return clazz.getMethod( method.getName(), method.getParameterTypes() );
		}
		catch (Exception e) {
			return null;
		}
	}

	public static Method getMethod(Class<?> clazz, String methodName, Class<?>... paramTypes) {
		try {
			return clazz.getMethod( methodName, paramTypes );
		}
		catch (Exception e) {
			return null;
		}
	}

	public static Field findField(Class<?> containerClass, String propertyName) {
		if ( containerClass == null ) {
			throw new IllegalArgumentException( "Class<?> on which to find field [" + propertyName + "] cannot be null" );
		}
		else if ( containerClass == Object.class ) {
			throw new IllegalArgumentException( "Illegal attempt to locate field [" + propertyName + "] on Object.class" );
		}
		else {
			final Field field = locateField( containerClass, propertyName );
			if ( field == null ) {
				throw new PropertyNotFoundException(
						String.format(
								Locale.ROOT,
								"Could not locate field name [%s] on class [%s]",
								propertyName,
								containerClass.getName()
						)
				);
			}
			ensureAccessibility( field );
			return field;
		}
	}

	public static void ensureAccessibility(AccessibleObject accessibleObject) {
		if ( !accessibleObject.isAccessible() ) {
			accessibleObject.setAccessible( true );
		}
	}

	private static Field locateField(Class<?> clazz, String propertyName) {
		if ( clazz == null || Object.class.equals( clazz ) ) {
			return null;
		}

		try {
			final Field field = clazz.getDeclaredField( propertyName );
			return !isStaticField( field )
					? field
					: locateField( clazz.getSuperclass(), propertyName );
		}
		catch ( NoSuchFieldException nsfe ) {
			return locateField( clazz.getSuperclass(), propertyName );
		}
	}

	public static boolean isStaticField(Field field) {
		return field != null && ( field.getModifiers() & Modifier.STATIC ) == Modifier.STATIC;
	}

	public static Method findGetterMethod(Class<?> containerClass, String propertyName) {
		Class<?> checkClass = containerClass;
		Method getter = null;

		if ( isRecord( containerClass ) ) {
			try {
				getter = containerClass.getMethod( propertyName, NO_PARAM_SIGNATURE );
			}
			catch (NoSuchMethodException e) {
				// Ignore
			}
		}

		// check containerClass, and then its super types (if any)
		while ( getter == null && checkClass != null ) {
			if ( checkClass.equals( Object.class ) ) {
				break;
			}
			else {
				getter = getGetterOrNull( checkClass, propertyName );
				// if no getter found yet, check all implemented interfaces
				if ( getter == null ) {
					getter = getGetterOrNull( checkClass.getInterfaces(), propertyName );
				}
				checkClass = checkClass.getSuperclass();
			}
		}

		if ( getter == null ) {
			throw new PropertyNotFoundException(
					String.format(
							Locale.ROOT,
							"Could not locate getter method for property '%s' of class '%s'",
							propertyName,
							containerClass.getName()
					)
			);
		}

		ensureAccessibility( getter );

		return getter;
	}

	private static Method getGetterOrNull(Class<?>[] interfaces, String propertyName) {
		Method getter = null;
		for ( int i = 0; getter == null && i < interfaces.length; ++i ) {
			final Class<?> anInterface = interfaces[i];
			if ( !shouldSkipInterfaceCheck( anInterface ) ) {
				getter = getGetterOrNull( anInterface, propertyName );
				if ( getter == null ) {
					// if no getter found yet, check all implemented interfaces of interface
					getter = getGetterOrNull( anInterface.getInterfaces(), propertyName );
				}
			}
		}
		return getter;
	}

	/**
	 * Find the method that can be used as the getter for this property.
	 *
	 * @param containerClass The Class which contains the property
	 * @param propertyName The name of the property
	 *
	 * @return The getter method, or {@code null} if there is none.
	 *
	 * @throws MappingException If the {@code containerClass} has both a get- and an is- form.
	 */
	public static Method getGetterOrNull(Class<?> containerClass, String propertyName) {
		if ( isRecord( containerClass ) ) {
			try {
				return containerClass.getMethod( propertyName, NO_PARAM_SIGNATURE );
			}
			catch (NoSuchMethodException e) {
				// Ignore
			}
		}

		for ( Method method : containerClass.getDeclaredMethods() ) {
			if ( method.getParameterCount() == 0 // if the method has parameters, skip it
					&& !Modifier.isStatic( method.getModifiers() )
					&& !method.isBridge()
					&& method.getAnnotation( Transient.class ) == null ) {

				final String methodName = method.getName();

				// try "get"
				if ( methodName.startsWith( "get" ) ) {
					final String stemName = methodName.substring( 3 );
					if ( stemName.equals( propertyName )
							|| decapitalize( stemName ).equals( propertyName ) ) {
						verifyNoIsVariantExists( containerClass, propertyName, method, stemName );
						return method;
					}

				}

				// if not "get", then try "is"
				if ( methodName.startsWith( "is" ) ) {
					final String stemName = methodName.substring( 2 );
					if ( stemName.equals( propertyName )
							|| decapitalize( stemName ).equals( propertyName ) ) {
						// not sure that this can ever really happen given the handling of "get" above.
						// but be safe
						verifyNoGetVariantExists( containerClass, propertyName, method, stemName );
						return method;
					}
				}
			}
		}

		return null;
	}

	public static void verifyNoIsVariantExists(
			Class<?> containerClass,
			String propertyName,
			Method getMethod,
			String stemName) {
		// verify that the Class<?> does not also define a method with the same stem name with 'is'
		try {
			final Method isMethod = containerClass.getDeclaredMethod( "is" + stemName );
			if ( !Modifier.isStatic( isMethod.getModifiers() ) && isMethod.getAnnotation( Transient.class ) == null ) {
				// No such method should throw the caught exception.  So if we get here, there was
				// such a method.
				checkGetAndIsVariants( containerClass, propertyName, getMethod, isMethod );
			}
		}
		catch (NoSuchMethodException ignore) {
		}
	}


	public static void checkGetAndIsVariants(
			Class<?> containerClass,
			String propertyName,
			Method getMethod,
			Method isMethod) {
		// Check the return types.  If they are the same, its ok.  If they are different
		// we are in a situation where we could not reasonably know which to use.
		if ( !isMethod.getReturnType().equals( getMethod.getReturnType() ) ) {
			throw new MappingException(
					String.format(
							Locale.ROOT,
							"Class<?> '%s' declares both 'get' [%s] and 'is' [%s] variants of getter for property '%s'",
							containerClass.getName(),
							getMethod,
							isMethod,
							propertyName
					)
			);
		}
	}

	public static void verifyNoGetVariantExists(
			Class<?> containerClass,
			String propertyName,
			Method isMethod,
			String stemName) {
		// verify that the Class<?> does not also define a method with the same stem name with 'is'
		try {
			final Method getMethod = containerClass.getDeclaredMethod( "get" + stemName );
			// No such method should throw the caught exception.  So if we get here, there was
			// such a method.
			if ( !Modifier.isStatic( getMethod.getModifiers() )
					&& getMethod.getAnnotation( Transient.class ) == null ) {
				checkGetAndIsVariants( containerClass, propertyName, getMethod, isMethod );
			}
		}
		catch (NoSuchMethodException ignore) {
		}
	}

	public static Method getterMethodOrNull(Class<?> containerJavaType, String propertyName) {
		try {
			return findGetterMethod( containerJavaType, propertyName );
		}
		catch (PropertyNotFoundException e) {
			return null;
		}
	}

	public static Method setterMethodOrNull(final Class<?> containerClass, final  String propertyName, final Class<?> propertyType) {

		//Computes the most likely setter name - there might be fallback choices to try, but we try this one first
		//to try not overwhelming the system with swallowed exceptions.
		final String likelyMethodName = likelySetterMethodNameForProperty( propertyName );

		//First let's test the most obvious solution: a public method having exactly the expected name and type;
		//this has the benefit of including parent types and interfaces w/o extensively bothering the reflection api
		//which is very allocation intensive - this is noticeable on bootstrap costs on large models.
		try {
			final Method setter = containerClass.getMethod( likelyMethodName, propertyType );
			ensureAccessibility( setter );
			return setter;
		}
		catch ( NoSuchMethodException e ) {
			//No luck: we'll need to run the more expensive but thorough process
		}

		Class<?> checkClass = containerClass;
		Method setter = null;

		// check containerClass, and then its super types (if any)
		while ( setter == null && checkClass != null ) {
			if ( checkClass.equals( Object.class ) ) {
				break;
			}
			else {
				setter = setterOrNull( checkClass, propertyName, propertyType, likelyMethodName );
				// if no setter found yet, check all implemented interfaces
				if ( setter == null ) {
					setter = setterOrNull( checkClass.getInterfaces(), propertyName, propertyType, likelyMethodName );
				}
				else {
					ensureAccessibility( setter );
				}
				checkClass = checkClass.getSuperclass();
			}
		}
		return setter; // might be null
	}

	public static Method setterMethodOrNullBySetterName(final Class<?> containerClass, final  String setterName, final Class<?> propertyType) {
		Class<?> checkClass = containerClass;
		Method setter = null;

		// check containerClass, and then its super types (if any)
		while ( setter == null && checkClass != null ) {
			if ( checkClass.equals( Object.class ) ) {
				break;
			}
			else {
				setter = setterOrNullBySetterName( checkClass, setterName, propertyType );
				// if no setter found yet, check all implemented interfaces
				if ( setter == null ) {
					setter = setterOrNullBySetterName( checkClass.getInterfaces(), setterName, propertyType );
				}
				else {
					ensureAccessibility( setter );
				}
				checkClass = checkClass.getSuperclass();
			}
		}
		return setter; // might be null
	}

	private static Method setterOrNullBySetterName(Class<?>[] interfaces, String setterName, Class<?> propertyType) {
		Method setter = null;
		for ( int i = 0; setter == null && i < interfaces.length; ++i ) {
			final Class<?> anInterface = interfaces[i];
			if ( !shouldSkipInterfaceCheck( anInterface ) ) {
				setter = setterOrNullBySetterName( anInterface, setterName, propertyType );
				if ( setter == null ) {
					// if no setter found yet, check all implemented interfaces of interface
					setter = setterOrNullBySetterName( anInterface.getInterfaces(), setterName, propertyType );
				}
			}
		}
		return setter;
	}

	private static boolean shouldSkipInterfaceCheck(final Class<?> anInterface) {
		final String interfaceName = anInterface.getName();
		//Skip checking any interface that we've added via bytecode enhancement:
		//there's many of those, and it's pointless to look there.
		if ( interfaceName.startsWith( "org.hibernate.engine." ) ) {
			return true;
		}
		//Also skip jakarta.persistence prefixed interfaces, as otherwise we'll be scanning
		//among mapping annotations as well:
		if ( interfaceName.startsWith( "jakarta.persistence." ) ) {
			return true;
		}
		return false;
	}

	private static Method setterOrNullBySetterName(Class<?> theClass, String setterName, Class<?> propertyType) {
		Method potentialSetter = null;

		for ( Method method : theClass.getDeclaredMethods() ) {
			final String methodName = method.getName();
			if ( method.getParameterCount() == 1 && methodName.equals( setterName ) ) {
				potentialSetter = method;
				if ( propertyType == null || method.getParameterTypes()[0].equals( propertyType ) ) {
					break;
				}
			}
		}

		return potentialSetter;
	}

	public static Method findSetterMethod(final Class<?> containerClass, final String propertyName, final Class<?> propertyType) {
		final Method setter = setterMethodOrNull( containerClass, propertyName, propertyType );
		if ( setter == null ) {
			throw new PropertyNotFoundException(
					String.format(
							Locale.ROOT,
							"Could not locate setter method for property '%s' of class '%s'",
							propertyName,
							containerClass.getName()
					)
			);
		}
		return setter;
	}

	private static Method setterOrNull(Class<?>[] interfaces, String propertyName, Class<?> propertyType, String likelyMethodName) {
		Method setter = null;
		for ( int i = 0; setter == null && i < interfaces.length; ++i ) {
			final Class<?> anInterface = interfaces[i];
			if ( !shouldSkipInterfaceCheck( anInterface ) ) {
				setter = setterOrNull( anInterface, propertyName, propertyType, likelyMethodName );
				if ( setter == null ) {
					// if no setter found yet, check all implemented interfaces of interface
					setter = setterOrNull( anInterface.getInterfaces(), propertyName, propertyType, likelyMethodName );
				}
			}
		}
		return setter;
	}

	private static Method setterOrNull(Class<?> theClass, String propertyName, Class<?> propertyType, String likelyMethodName) {
		try {
			return theClass.getDeclaredMethod( likelyMethodName, propertyType );
		}
		catch ( NoSuchMethodException e ) {
			//Ignore, so we try the old method for best compatibility (even though it's less efficient) next:
		}
		Method potentialSetter = null;
		for ( Method method : theClass.getDeclaredMethods() ) {
			final String methodName = method.getName();
			if ( method.getParameterCount() == 1 && methodName.startsWith( "set" ) ) {
				final String testOldMethod = methodName.substring( 3 );
				final String testStdMethod = decapitalize( testOldMethod );
				if ( testStdMethod.equals( propertyName ) || testOldMethod.equals( propertyName ) ) {
					potentialSetter = method;
					if ( propertyType == null || method.getParameterTypes()[0].equals( propertyType ) ) {
						break;
					}
				}
			}
		}

		return potentialSetter;
	}

	private static String likelySetterMethodNameForProperty(final String propertyName) {
		final char firstCharacter = propertyName.charAt( 0 );
		return Character.isLowerCase( firstCharacter )
				? "set" + Character.toUpperCase( firstCharacter ) + propertyName.substring( 1 )
				: "set" + propertyName;
	}

	/**
	 * Similar to {@link #getterMethodOrNull}, except that here we are just looking for the
	 * corresponding getter for a field (defined as field access) if one exists.
	 * <p>
	 * We do not look at supers, although conceivably the super could declare the method
	 * as an abstract - but again, that is such an edge case...
	 */
	public static Method findGetterMethodForFieldAccess(Field field, String propertyName) {
		for ( Method method : field.getDeclaringClass().getDeclaredMethods() ) {
			if ( method.getParameterCount() == 0 // if the method has parameters, skip it
					&& !Modifier.isStatic( method.getModifiers() )
					&& method.getReturnType().isAssignableFrom( field.getType() ) ) {

				final String methodName = method.getName();

				// try "get"
				if ( methodName.startsWith( "get" ) ) {
					final String stemName = methodName.substring( 3 );
					if ( stemName.equals( propertyName )
							|| decapitalize( stemName ).equals( propertyName ) ) {
						return method;
					}

				}

				// if not "get", then try "is"
				if ( methodName.startsWith( "is" ) ) {
					final String stemName = methodName.substring( 2 );
					if ( stemName.equals( propertyName )
							|| decapitalize( stemName ).equals( propertyName ) ) {
						return method;
					}
				}
			}
		}
		if ( isRecord( field.getDeclaringClass() ) ) {
			try {
				return field.getDeclaringClass().getMethod( field.getName(), NO_PARAM_SIGNATURE );
			}
			catch (NoSuchMethodException e) {
				// Ignore
			}
		}

		return null;
	}

	public static boolean isRecord(Class<?> declaringClass) {
		return RECORD_CLASS != null && RECORD_CLASS.isAssignableFrom( declaringClass );
	}

	public static Class<?>[] getRecordComponentTypes(Class<?> javaType) {
		try {
			final Object[] recordComponents = (Object[]) GET_RECORD_COMPONENTS.invoke( javaType );
			final Class<?>[] componentTypes = new Class[recordComponents.length];
			for (int i = 0; i < recordComponents.length; i++ ) {
				componentTypes[i] = (Class<?>) GET_TYPE.invoke( recordComponents[i] );
			}
			return componentTypes;
		}
		catch (Exception e) {
			throw new IllegalArgumentException(
					"Could not determine the record components for: " + javaType.getName(),
					e
			);
		}
	}

	public static String[] getRecordComponentNames(Class<?> javaType) {
		try {
			final Object[] recordComponents = (Object[]) GET_RECORD_COMPONENTS.invoke( javaType );
			final String[] componentNames = new String[recordComponents.length];
			for (int i = 0; i < recordComponents.length; i++ ) {
				componentNames[i] = (String) GET_NAME.invoke( recordComponents[i] );
			}
			return componentNames;
		}
		catch (Exception e) {
			throw new IllegalArgumentException(
					"Could not determine the record components for: " + javaType.getName(),
					e
			);
		}
	}

	public static <T> Class<T> getClass(java.lang.reflect.Type type) {
		if ( type == null ) {
			return null;
		}
		else if ( type instanceof Class<?> ) {
			return (Class<T>) type;
		}
		else if ( type instanceof ParameterizedType parameterizedType ) {
			return (Class<T>) parameterizedType.getRawType();
		}
		else if ( type instanceof TypeVariable<?> typeVariable ) {
			return getClass( typeVariable.getBounds()[0] );
		}
		else if ( type instanceof WildcardType wildcardType ) {
			return getClass( wildcardType.getUpperBounds()[0] );
		}
		throw new UnsupportedOperationException( "Can't get java type class from type: " + type );
	}

	public static Class<?> getPropertyType(Member member) {
		if (member instanceof Field field) {
			return field.getType();
		}
		else if (member instanceof Method method) {
			return method.getReturnType();
		}
		else {
			throw new AssertionFailure("member should have been a method or field");
		}
	}

	public static boolean isClass(Class<?> resultClass) {
		return !resultClass.isArray()
			&& !resultClass.isPrimitive()
			&& !resultClass.isEnum()
			&& !resultClass.isInterface();
	}

	@SuppressWarnings("unchecked")
	public static <T> Class<T> getClass(T instance) {
		return (Class<T>) instance.getClass();
	}

	@SuppressWarnings("unchecked")
	public static <T extends Enum<T>> Class<T> getClass(Enum<T> value) {
		return (Class<T>) value.getClass();
	}

	@AllowReflection
	public static <T> Class<T[]> arrayClass(Class<T> clazz) {
		final Object instance = Array.newInstance( clazz, 0 );
		@SuppressWarnings("unchecked")
		final T[] array = (T[]) instance;
		return getClass( array );
	}
}
