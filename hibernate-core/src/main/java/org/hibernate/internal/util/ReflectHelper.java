/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.internal.util;

import java.beans.Introspector;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Locale;
import java.util.regex.Pattern;
import javax.persistence.Transient;

import org.hibernate.AssertionFailure;
import org.hibernate.MappingException;
import org.hibernate.PropertyNotFoundException;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.property.access.internal.PropertyAccessStrategyMixedImpl;
import org.hibernate.property.access.spi.Getter;
import org.hibernate.type.PrimitiveType;
import org.hibernate.type.Type;

/**
 * Utility class for various reflection operations.
 *
 * @author Gavin King
 * @author Steve Ebersole
 * @author Chris Cranford
 */
@SuppressWarnings("unchecked")
public final class ReflectHelper {

	private static final Pattern JAVA_CONSTANT_PATTERN = Pattern.compile(
			"[a-z\\d]+\\.([A-Z]{1}[a-z\\d]+)+\\$?([A-Z]{1}[a-z\\d]+)*\\.[A-Z_\\$]+", Pattern.UNICODE_CHARACTER_CLASS);

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
	 * Determine if the given class implements the given interface.
	 *
	 * @param clazz The class to check
	 * @param intf The interface to check it against.
	 * @return True if the class does implement the interface, false otherwise.
	 */
	public static boolean implementsInterface(Class clazz, Class intf) {
		assert intf.isInterface() : "Interface to check was not an interface";
		return intf.isAssignableFrom( clazz );
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
			ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
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
	 * <p/>
	 * Same as {@link #classForName(String, Class)} except that here we delegate to
	 * {@link Class#forName(String)} if the context classloader lookup is unsuccessful.
	 *
	 * @param name The class name
	 * @return The class reference.
	 *
	 * @throws ClassNotFoundException From {@link Class#forName(String)}.
	 *
	 * @deprecated Depending on context, either {@link org.hibernate.boot.registry.classloading.spi.ClassLoaderService}
	 * or {@link org.hibernate.boot.spi.ClassLoaderAccess} should be preferred
	 */
	@Deprecated
	public static Class classForName(String name) throws ClassNotFoundException {
		try {
			ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
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
	public static boolean isPublic(Class clazz, Member member) {
		return Modifier.isPublic( member.getModifiers() ) && Modifier.isPublic( clazz.getModifiers() );
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
	public static Class reflectedPropertyClass(
			String className,
			String name,
			ClassLoaderService classLoaderService) throws MappingException {
		try {
			Class clazz = classLoaderService.classForName( className );
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
	public static Class reflectedPropertyClass(Class clazz, String name) throws MappingException {
		return getter( clazz, name ).getReturnType();
	}

	private static Getter getter(Class clazz, String name) throws MappingException {
		return PropertyAccessStrategyMixedImpl.INSTANCE.buildPropertyAccess( clazz, name ).getGetter();
	}

	public static Object getConstantValue(String name, SessionFactoryImplementor factory) {
		boolean conventionalJavaConstants = factory.getSessionFactoryOptions().isConventionalJavaConstants();
		Class clazz;
		try {
			if ( conventionalJavaConstants &&
				!JAVA_CONSTANT_PATTERN.matcher( name ).find() ) {
				return null;
			}
			ClassLoaderService classLoaderService = factory.getServiceRegistry().getService( ClassLoaderService.class );
			clazz = classLoaderService.classForName( StringHelper.qualifier( name ) );
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
			Constructor<T> constructor = clazz.getDeclaredConstructor( NO_PARAM_SIGNATURE );
			ensureAccessibility( constructor );
			return constructor;
		}
		catch ( NoSuchMethodException nme ) {
			throw new PropertyNotFoundException(
					"Object class [" + clazz.getName() + "] must declare a default (no-argument) constructor"
			);
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
	 * @return True if the class is final, false otherwise.
	 */
	public static boolean isFinalClass(Class clazz) {
		return Modifier.isFinal( clazz.getModifiers() );
	}

	/**
	 * Retrieve a constructor for the given class, with arguments matching the specified Hibernate mapping
	 * {@link Type types}.
	 *
	 * @param clazz The class needing instantiation
	 * @param types The types representing the required ctor param signature
	 * @return The matching constructor.
	 * @throws PropertyNotFoundException Indicates we could not locate an appropriate constructor (todo : again with PropertyNotFoundException???)
	 */
	public static Constructor getConstructor(Class clazz, Type[] types) throws PropertyNotFoundException {
		final Constructor[] candidates = clazz.getConstructors();
		Constructor constructor = null;
		int numberOfMatchingConstructors = 0;
		for ( final Constructor candidate : candidates ) {
			final Class[] params = candidate.getParameterTypes();
			if ( params.length == types.length ) {
				boolean found = true;
				for ( int j = 0; j < params.length; j++ ) {
					final boolean ok = types[j] == null || params[j].isAssignableFrom( types[j].getReturnedClass() ) || (
							types[j] instanceof PrimitiveType &&
									params[j] == ( (PrimitiveType) types[j] ).getPrimitiveClass()
					);
					if ( !ok ) {
						found = false;
						break;
					}
				}
				if ( found ) {
					numberOfMatchingConstructors ++;
					ensureAccessibility( candidate );
					constructor = candidate;
				}
			}
		}

		if ( numberOfMatchingConstructors == 1 ) {
			return constructor;
		}
		throw new PropertyNotFoundException( "no appropriate constructor in class: " + clazz.getName() );

	}

	public static <T> Constructor<T> getConstructor(
			Class<T> clazz,
			Class... constructorArgs) {
		Constructor<T> constructor = null;
		try {
			constructor = clazz.getDeclaredConstructor( constructorArgs );
			try {
				ReflectHelper.ensureAccessibility( constructor );
			}
			catch ( SecurityException e ) {
				constructor = null;
			}
		}
		catch ( NoSuchMethodException ignore ) {
		}

		return constructor;
	}

	public static Method getMethod(Class clazz, Method method) {
		try {
			return clazz.getMethod( method.getName(), method.getParameterTypes() );
		}
		catch (Exception e) {
			return null;
		}
	}

	public static Method getMethod(Class clazz, String methodName, Class... paramTypes) {
		try {
			return clazz.getMethod( methodName, paramTypes );
		}
		catch (Exception e) {
			return null;
		}
	}

	public static Field findField(Class containerClass, String propertyName) {
		if ( containerClass == null ) {
			throw new IllegalArgumentException( "Class on which to find field [" + propertyName + "] cannot be null" );
		}
		else if ( containerClass == Object.class ) {
			throw new IllegalArgumentException( "Illegal attempt to locate field [" + propertyName + "] on Object.class" );
		}

		Field field = locateField( containerClass, propertyName );

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

	public static void ensureAccessibility(AccessibleObject accessibleObject) {
		if ( accessibleObject.isAccessible() ) {
			return;
		}

		accessibleObject.setAccessible( true );
	}

	private static Field locateField(Class clazz, String propertyName) {
		if ( clazz == null || Object.class.equals( clazz ) ) {
			return null;
		}

		try {
			Field field = clazz.getDeclaredField( propertyName );
			if ( !isStaticField( field ) ) {
				return field;
			}
			return locateField( clazz.getSuperclass(), propertyName );
		}
		catch ( NoSuchFieldException nsfe ) {
			return locateField( clazz.getSuperclass(), propertyName );
		}
	}

	private static boolean isStaticField(Field field) {
		return field != null && ( field.getModifiers() & Modifier.STATIC ) == Modifier.STATIC;
	}

	public static Method findGetterMethod(Class containerClass, String propertyName) {
		Class checkClass = containerClass;
		Method getter = null;

		// check containerClass, and then its super types (if any)
		while ( getter == null && checkClass != null ) {
			if ( checkClass.equals( Object.class ) ) {
				break;
			}

			getter = getGetterOrNull( checkClass, propertyName );

			// if no getter found yet, check all implemented interfaces
			if ( getter == null ) {
				getter = getGetterOrNull( checkClass.getInterfaces(), propertyName );
			}

			checkClass = checkClass.getSuperclass();
		}


		if ( getter == null ) {
			throw new PropertyNotFoundException(
					String.format(
							Locale.ROOT,
							"Could not locate getter method for property [%s#%s]",
							containerClass.getName(),
							propertyName
					)
			);
		}

		ensureAccessibility( getter );

		return getter;
	}

	private static Method getGetterOrNull(Class[] interfaces, String propertyName) {
		Method getter = null;
		for ( int i = 0; getter == null && i < interfaces.length; ++i ) {
			final Class anInterface = interfaces[i];
			getter = getGetterOrNull( anInterface, propertyName );
			if ( getter == null ) {
				// if no getter found yet, check all implemented interfaces of interface
				getter = getGetterOrNull( anInterface.getInterfaces(), propertyName );
			}
		}
		return getter;
	}

	private static Method getGetterOrNull(Class containerClass, String propertyName) {
		for ( Method method : containerClass.getDeclaredMethods() ) {
			// if the method has parameters, skip it
			if ( method.getParameterCount() != 0 ) {
				continue;
			}

			// if the method is a "bridge", skip it
			if ( method.isBridge() ) {
				continue;
			}

			if ( method.getAnnotation( Transient.class ) != null ) {
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
					verifyNoIsVariantExists( containerClass, propertyName, method, stemName );
					return method;
				}

			}

			// if not "get", then try "is"
			if ( methodName.startsWith( "is" ) ) {
				final String stemName = methodName.substring( 2 );
				String decapitalizedStemName = Introspector.decapitalize( stemName );
				if ( stemName.equals( propertyName ) || decapitalizedStemName.equals( propertyName ) ) {
					verifyNoGetVariantExists( containerClass, propertyName, method, stemName );
					return method;
				}
			}
		}

		return null;
	}

	private static void verifyNoIsVariantExists(
			Class containerClass,
			String propertyName,
			Method getMethod,
			String stemName) {
		// verify that the Class does not also define a method with the same stem name with 'is'
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

	private static void checkGetAndIsVariants(
			Class containerClass,
			String propertyName,
			Method getMethod,
			Method isMethod) {
		// Check the return types.  If they are the same, its ok.  If they are different
		// we are in a situation where we could not reasonably know which to use.
		if ( !isMethod.getReturnType().equals( getMethod.getReturnType() ) ) {
			throw new MappingException(
					String.format(
							Locale.ROOT,
							"In trying to locate getter for property [%s], Class [%s] defined " +
									"both a `get` [%s] and `is` [%s] variant",
							propertyName,
							containerClass.getName(),
							getMethod.toString(),
							isMethod.toString()
					)
			);
		}
	}

	private static void verifyNoGetVariantExists(
			Class containerClass,
			String propertyName,
			Method isMethod,
			String stemName) {
		// verify that the Class does not also define a method with the same stem name with 'is'
		try {
			final Method getMethod = containerClass.getDeclaredMethod( "get" + stemName );
			// No such method should throw the caught exception.  So if we get here, there was
			// such a method.
			if ( !Modifier.isStatic( getMethod.getModifiers() ) && getMethod.getAnnotation( Transient.class ) == null ) {
				checkGetAndIsVariants( containerClass, propertyName, getMethod, isMethod );
			}
		}
		catch (NoSuchMethodException ignore) {
		}
	}

	public static Method getterMethodOrNull(Class containerJavaType, String propertyName) {
		try {
			return findGetterMethod( containerJavaType, propertyName );
		}
		catch (PropertyNotFoundException e) {
			return null;
		}
	}

	public static Method setterMethodOrNull(final Class containerClass, final  String propertyName, final Class propertyType) {
		Class checkClass = containerClass;
		Method setter = null;

		// check containerClass, and then its super types (if any)
		while ( setter == null && checkClass != null ) {
			if ( checkClass.equals( Object.class ) ) {
				break;
			}

			setter = setterOrNull( checkClass, propertyName, propertyType );

			// if no setter found yet, check all implemented interfaces
			if ( setter == null ) {
				setter = setterOrNull( checkClass.getInterfaces(), propertyName, propertyType );
			}
			else {
				ensureAccessibility( setter );
			}

			checkClass = checkClass.getSuperclass();
		}
		return setter; // might be null
	}

	public static Method findSetterMethod(final Class containerClass, final String propertyName, final Class propertyType) {
		final Method setter = setterMethodOrNull( containerClass, propertyName, propertyType );
		if ( setter == null ) {
			throw new PropertyNotFoundException(
					String.format(
							Locale.ROOT,
							"Could not locate setter method for property [%s#%s]",
							containerClass.getName(),
							propertyName
					)
			);
		}
		return setter;
	}

	private static Method setterOrNull(Class[] interfaces, String propertyName, Class propertyType) {
		Method setter = null;
		for ( int i = 0; setter == null && i < interfaces.length; ++i ) {
			final Class anInterface = interfaces[i];
			setter = setterOrNull( anInterface, propertyName, propertyType );
			if ( setter == null ) {
				// if no setter found yet, check all implemented interfaces of interface
				setter = setterOrNull( anInterface.getInterfaces(), propertyName, propertyType );
			}
		}
		return setter;
	}

	private static Method setterOrNull(Class theClass, String propertyName, Class propertyType) {
		Method potentialSetter = null;

		for ( Method method : theClass.getDeclaredMethods() ) {
			final String methodName = method.getName();
			if ( method.getParameterCount() == 1 && methodName.startsWith( "set" ) ) {
				final String testOldMethod = methodName.substring( 3 );
				final String testStdMethod = Introspector.decapitalize( testOldMethod );
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

	/**
	 * Similar to {@link #getterMethodOrNull}, except that here we are just looking for the
	 * corresponding getter for a field (defined as field access) if one exists.
	 *
	 * We do not look at supers, although conceivably the super could declare the method
	 * as an abstract - but again, that is such an edge case...
	 */
	public static Method findGetterMethodForFieldAccess(Field field, String propertyName) {
		for ( Method method : field.getDeclaringClass().getDeclaredMethods() ) {
			// if the method has parameters, skip it
			if ( method.getParameterCount() != 0 ) {
				continue;
			}

			if ( Modifier.isStatic( method.getModifiers() ) ) {
				continue;
			}

			if ( ! method.getReturnType().isAssignableFrom( field.getType() ) ) {
				continue;
			}

			final String methodName = method.getName();

			// try "get"
			if ( methodName.startsWith( "get" ) ) {
				final String stemName = methodName.substring( 3 );
				final String decapitalizedStemName = Introspector.decapitalize( stemName );
				if ( stemName.equals( propertyName ) || decapitalizedStemName.equals( propertyName ) ) {
					return method;
				}

			}

			// if not "get", then try "is"
			if ( methodName.startsWith( "is" ) ) {
				final String stemName = methodName.substring( 2 );
				String decapitalizedStemName = Introspector.decapitalize( stemName );
				if ( stemName.equals( propertyName ) || decapitalizedStemName.equals( propertyName ) ) {
					return method;
				}
			}
		}

		return null;
	}
}
