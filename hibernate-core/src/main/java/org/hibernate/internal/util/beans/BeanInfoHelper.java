/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal.util.beans;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.hibernate.internal.util.StringHelper.decapitalize;

/**
 * Utility for helping deal with {@link BeanInfo}.
 * <p>
 * This is a simplified reimplementation to avoid dependency on the {@code java.desktop} module.
 * Unlike the JDK's {@code java.beans.Introspector}, this implementation:
 * <ul>
 *   <li>Does not validate type compatibility between getters and setters</li>
 *   <li>Does not introspect methods from implemented interfaces (only class hierarchy)</li>
 *   <li>Takes the first matching setter when overloads exist</li>
 * </ul>
 * These simplifications are acceptable for Hibernate's use case of property-name-based injection.
 * <p>
 * Caching is implemented using {@link ClassValue}, which is the JVM's built-in mechanism for
 * associating computed values with classes. Unlike the JDK's {@code Introspector} cache, this
 * approach is inherently GC-friendly: when a class is unloaded, the associated {@link BeanInfo}
 * is automatically released, preventing classloader leaks without requiring explicit cache flushing.
 *
 * @author Steve Ebersole
 */
public class BeanInfoHelper {

	/**
	 * Cache of BeanInfo instances, keyed by class.
	 * Uses ClassValue which automatically handles class unloading - when a class is GC'd,
	 * its associated BeanInfo is also released, preventing classloader leaks.
	 * This cache is for the common case where stopClass is null (introspect up to Object).
	 */
	private static final ClassValue<BeanInfo> BEAN_INFO_CACHE = new ClassValue<>() {
		@Override
		protected BeanInfo computeValue(final Class<?> type) {
			return computeBeanInfo( type, null );
		}
	};

	public interface BeanInfoDelegate {
		void processBeanInfo(BeanInfo beanInfo) throws Exception;
	}

	public interface ReturningBeanInfoDelegate<T> {
		T processBeanInfo(BeanInfo beanInfo) throws Exception;
	}

	private final Class<?> beanClass;
	private final Class<?> stopClass;

	public BeanInfoHelper(Class<?> beanClass) {
		this( beanClass, Object.class );
	}

	public BeanInfoHelper(Class<?> beanClass, Class<?> stopClass) {
		this.beanClass = beanClass;
		this.stopClass = stopClass;
	}

	public void applyToBeanInfo(Object bean, BeanInfoDelegate delegate) {
		if ( ! beanClass.isInstance( bean ) ) {
			throw new BeanIntrospectionException( "Bean [" + bean + "] was not of declared bean type [" + beanClass.getName() + "]" );
		}

		visitBeanInfo( beanClass, stopClass, delegate );
	}

	public static void visitBeanInfo(Class<?> beanClass, BeanInfoDelegate delegate) {
		visitBeanInfo( beanClass, Object.class, delegate );
	}

	public static void visitBeanInfo(Class<?> beanClass, Class<?> stopClass, BeanInfoDelegate delegate) {
		try {
			final BeanInfo info = getBeanInfo( beanClass, stopClass );
			try {
				delegate.processBeanInfo( info );
			}
			catch ( RuntimeException e ) {
				throw e;
			}
			catch ( InvocationTargetException e ) {
				throw new BeanIntrospectionException( "Error delegating bean info use", e.getTargetException() );
			}
			catch ( Exception e ) {
				throw new BeanIntrospectionException( "Error delegating bean info use", e );
			}
		}
		catch ( BeanIntrospectionException e ) {
			throw e;
		}
		catch ( Exception e ) {
			throw new BeanIntrospectionException( "Unable to determine bean info from class [" + beanClass.getName() + "]", e );
		}
	}

	public static <T> T visitBeanInfo(Class<?> beanClass, ReturningBeanInfoDelegate<T> delegate) {
		return visitBeanInfo( beanClass, null, delegate );
	}

	public static <T> T visitBeanInfo(Class<?> beanClass, Class<?> stopClass, ReturningBeanInfoDelegate<T> delegate) {
		try {
			final BeanInfo info = getBeanInfo( beanClass, stopClass );
			try {
				return delegate.processBeanInfo( info );
			}
			catch ( RuntimeException e ) {
				throw e;
			}
			catch ( InvocationTargetException e ) {
				throw new BeanIntrospectionException( "Error delegating bean info use", e.getTargetException() );
			}
			catch ( Exception e ) {
				throw new BeanIntrospectionException( "Error delegating bean info use", e );
			}
		}
		catch ( BeanIntrospectionException e ) {
			throw e;
		}
		catch ( Exception e ) {
			throw new BeanIntrospectionException( "Unable to determine bean info from class [" + beanClass.getName() + "]", e );
		}
	}

	/**
	 * Introspect a JavaBean and return a BeanInfo object describing its properties.
	 * This method walks up the class hierarchy to collect all properties.
	 * <p>
	 * Results are cached using {@link ClassValue} for the common case where stopClass is null.
	 * The cache is GC-friendly and does not cause classloader leaks.
	 *
	 * @param beanClass The class to introspect
	 * @param stopClass The base class at which to stop the analysis. Any methods
	 *                  declared in the stopClass or its superclasses will be ignored.
	 *                  May be null.
	 * @return A BeanInfo object describing the target bean
	 */
	public static BeanInfo getBeanInfo(final Class<?> beanClass, final Class<?> stopClass) {
		// Use cache for the common case (stopClass == null means introspect up to Object)
		if ( stopClass == null ) {
			return BEAN_INFO_CACHE.get( beanClass );
		}
		// Non-null stopClass is rare; compute without caching to keep cache simple
		return computeBeanInfo( beanClass, stopClass );
	}

	private static BeanInfo computeBeanInfo(final Class<?> beanClass, final Class<?> stopClass) {
		// LinkedHashMap for reproducible ordering (important for build-time code)
		final Map<String, PropertyDescriptor> properties = new LinkedHashMap<>();

		// Walk from subclass to superclass; subclass properties take precedence
		Class<?> currentClass = beanClass;
		while ( currentClass != null && currentClass != stopClass && currentClass != Object.class ) {
			introspectClass( currentClass, properties );
			currentClass = currentClass.getSuperclass();
		}

		final PropertyDescriptor[] descriptors = properties.values().toArray( new PropertyDescriptor[0] );
		return new SimpleBeanInfo( descriptors );
	}

	/**
	 * Simple implementation of {@link BeanInfo} that holds a fixed array of property descriptors.
	 */
	private static class SimpleBeanInfo implements BeanInfo {
		private final PropertyDescriptor[] propertyDescriptors;

		SimpleBeanInfo(PropertyDescriptor[] propertyDescriptors) {
			this.propertyDescriptors = propertyDescriptors;
		}

		@Override
		public PropertyDescriptor[] getPropertyDescriptors() {
			return propertyDescriptors;
		}
	}

	private static void introspectClass(final Class<?> clazz, final Map<String, PropertyDescriptor> properties) {
		for ( final Method method : clazz.getDeclaredMethods() ) {
			final int modifiers = method.getModifiers();
			// Skip static, non-public, bridge, and synthetic methods
			if ( Modifier.isStatic( modifiers )
					|| !Modifier.isPublic( modifiers )
					|| method.isBridge()
					|| method.isSynthetic() ) {
				continue;
			}

			final String methodName = method.getName();
			final int paramCount = method.getParameterCount();
			final Class<?> returnType = method.getReturnType();

			// Check for getter: getXxx() or isXxx()
			if ( paramCount == 0 && returnType != void.class ) {
				String propertyName = null;
				if ( methodName.startsWith( "get" ) && methodName.length() > 3 ) {
					propertyName = decapitalize( methodName.substring( 3 ) );
				}
				else if ( methodName.startsWith( "is" ) && methodName.length() > 2
						&& ( returnType == boolean.class || returnType == Boolean.class ) ) {
					propertyName = decapitalize( methodName.substring( 2 ) );
				}

				if ( propertyName != null && !propertyName.isEmpty() ) {
					final PropertyDescriptor existing = properties.get( propertyName );
					if ( existing == null ) {
						properties.put( propertyName, new PropertyDescriptor( propertyName, method, null ) );
					}
					else if ( existing.getReadMethod() == null ) {
						// Merge: we had a setter, now add the getter
						properties.put( propertyName,
								new PropertyDescriptor( propertyName, method, existing.getWriteMethod() ) );
					}
					// else: subclass already defined a getter, keep it (subclass precedence)
				}
			}

			// Check for setter: setXxx(value)
			// Note: if overloaded setters exist, we take the first one found.
			// This is acceptable for name-based property matching.
			if ( paramCount == 1 && methodName.startsWith( "set" ) && methodName.length() > 3 ) {
				final String propertyName = decapitalize( methodName.substring( 3 ) );
				if ( !propertyName.isEmpty() ) {
					final PropertyDescriptor existing = properties.get( propertyName );
					if ( existing == null ) {
						properties.put( propertyName, new PropertyDescriptor( propertyName, null, method ) );
					}
					else if ( existing.getWriteMethod() == null ) {
						// Merge: we had a getter, now add the setter
						properties.put( propertyName,
								new PropertyDescriptor( propertyName, existing.getReadMethod(), method ) );
					}
					// else: subclass already defined a setter, keep it (subclass precedence)
				}
			}
		}
	}
}
