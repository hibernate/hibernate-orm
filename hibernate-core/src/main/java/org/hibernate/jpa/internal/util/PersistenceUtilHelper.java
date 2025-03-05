/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.jpa.internal.util;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.persistence.spi.LoadState;

import org.hibernate.HibernateException;
import org.hibernate.bytecode.enhance.spi.interceptor.BytecodeLazyAttributeInterceptor;
import org.hibernate.bytecode.enhance.spi.interceptor.LazyAttributeLoadingInterceptor;
import org.hibernate.collection.spi.LazyInitializable;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.PersistentAttributeInterceptable;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;

import static jakarta.persistence.spi.LoadState.LOADED;
import static jakarta.persistence.spi.LoadState.NOT_LOADED;
import static jakarta.persistence.spi.LoadState.UNKNOWN;
import static org.hibernate.engine.internal.ManagedTypeHelper.asPersistentAttributeInterceptable;
import static org.hibernate.engine.internal.ManagedTypeHelper.isPersistentAttributeInterceptable;
import static org.hibernate.proxy.HibernateProxy.extractLazyInitializer;

/**
 * Central delegate for handling calls from:<ul>
 *     <li>{@link jakarta.persistence.PersistenceUtil#isLoaded(Object)}</li>
 *     <li>{@link jakarta.persistence.PersistenceUtil#isLoaded(Object, String)}</li>
 *     <li>{@link jakarta.persistence.spi.ProviderUtil#isLoaded(Object)}</li>
 *     <li>{@link jakarta.persistence.spi.ProviderUtil#isLoadedWithReference(Object, String)}</li>
 *     <li>{@link jakarta.persistence.spi.ProviderUtil#isLoadedWithoutReference(Object, String)}li>
 * </ul>
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 * @author Steve Ebersole
 */
public final class PersistenceUtilHelper {
	private PersistenceUtilHelper() {
	}

	/**
	 * Determine if the given object reference represents loaded state.  The reference may be to an entity or a
	 * persistent collection.
	 * <p>
	 * Return is defined as follows:<ol>
	 *     <li>
	 *         If the reference is a {@link HibernateProxy}, we return {@link LoadState#LOADED} if
	 *         {@link LazyInitializer#isUninitialized()} returns {@code false}; else we return
	 *         {@link LoadState#NOT_LOADED}
	 *     </li>
	 *     <li>
	 *         If the reference is an enhanced (by Hibernate) entity, we return {@link LoadState#LOADED} if
	 *         {@link LazyAttributeLoadingInterceptor#hasAnyUninitializedAttributes()} returns {@code false};
	 *         otherwise we return {@link LoadState#NOT_LOADED}
	 *     </li>
	 *     <li>
	 *         If the reference is a {@link PersistentCollection}, we return {@link LoadState#LOADED} if
	 *         {@link PersistentCollection#wasInitialized()} returns {@code true}; else
	 *         we return {@link LoadState#NOT_LOADED}
	 *     </li>
	 *     <li>
	 *         In all other cases we return {@link LoadState#UNKNOWN}
	 *     </li>
	 * </ol>
	 *
	 *
	 * @param reference The object reference to check.
	 *
	 * @return The appropriate LoadState (see above)
	 */
	public static LoadState getLoadState(Object reference) {
		final LazyInitializer lazyInitializer = extractLazyInitializer( reference );
		if ( lazyInitializer != null ) {
			return !lazyInitializer.isUninitialized() ? LOADED : NOT_LOADED;
		}
		else if ( isPersistentAttributeInterceptable( reference ) ) {
			return isInitialized( asPersistentAttributeInterceptable( reference ) ) ? LOADED : NOT_LOADED;
		}
		else if ( reference instanceof LazyInitializable lazyInitializable) {
			return lazyInitializable.wasInitialized() ? LOADED : NOT_LOADED;
		}
		else {
			return UNKNOWN;
		}
	}

	private static boolean isInitialized(PersistentAttributeInterceptable interceptable) {
		final BytecodeLazyAttributeInterceptor interceptor = extractInterceptor( interceptable );
		return interceptor == null || !interceptor.hasAnyUninitializedAttributes();
	}

	private static BytecodeLazyAttributeInterceptor extractInterceptor(PersistentAttributeInterceptable interceptable) {
		return (BytecodeLazyAttributeInterceptor) interceptable.$$_hibernate_getInterceptor();
	}

	/**
	 * Is the given attribute (by name) loaded?  This form must take care to not access the attribute (trigger
	 * initialization).
	 *
	 * @param entity The entity
	 * @param attributeName The name of the attribute to check
	 * @param cache The cache we maintain of attribute resolutions
	 *
	 * @return The LoadState
	 */
	public static LoadState isLoadedWithoutReference(Object entity, String attributeName, MetadataCache cache) {
		final boolean sureFromUs;
		final LazyInitializer lazyInitializer = extractLazyInitializer( entity );
		if ( lazyInitializer != null ) {
			if ( lazyInitializer.isUninitialized() ) {
				// we have an uninitialized proxy, the attribute cannot be loaded
				return NOT_LOADED;
			}
			else {
				// swap the proxy with target (for proper class name resolution)
				entity = lazyInitializer.getImplementation();
			}
			sureFromUs = true;
		}
		else {
			sureFromUs = false;
		}

		// we are instrumenting, but we can't assume we are the only ones
		if ( isPersistentAttributeInterceptable( entity ) ) {
			final BytecodeLazyAttributeInterceptor interceptor =
					extractInterceptor( asPersistentAttributeInterceptable( entity ) );
			final boolean isInitialized = interceptor == null || interceptor.isAttributeLoaded( attributeName );
			return getLoadState( entity, attributeName, cache, isInitialized, interceptor, sureFromUs );
		}
		else {
			return UNKNOWN;
		}
	}

	private static LoadState getLoadState(
			Object entity, String attributeName,
			MetadataCache cache,
			boolean isInitialized,
			BytecodeLazyAttributeInterceptor interceptor,
			boolean sureFromUs) {
		if ( isInitialized && interceptor != null) {
			// attributeName is loaded according to bytecode enhancement, but is it loaded as far as association?
			// it's ours, we can read
			return getLoadState( entity, attributeName, cache );
		}
		else if ( interceptor != null ) {
			return NOT_LOADED;
		}
		else if ( sureFromUs ) {
			// property is loaded according to bytecode enhancement, but is it loaded as far as association?
			// it's ours, we can read
			return getLoadState( entity, attributeName, cache );
		}
		else {
			return UNKNOWN;
		}
	}

	private static LoadState getLoadState(Object entity, String attributeName, MetadataCache cache) {
		try {
			final LoadState state = getLoadState( getAttributeValue( entity, attributeName, cache ) );
			// it's ours so we know it's loaded
			return state == UNKNOWN ? LOADED : state;
		}
		catch (AttributeExtractionException ignore) {
			return UNKNOWN;
		}
	}

	/**
	 * Is the given attribute (by name) loaded?  This form must take care to not access the attribute (trigger
	 * initialization).
	 *
	 * @param entity The entity
	 * @param attributeName The name of the attribute to check
	 * @param cache The cache we maintain of attribute resolutions
	 *
	 * @return The LoadState
	 */
	public static LoadState isLoadedWithReference(Object entity, String attributeName, MetadataCache cache) {
		final LazyInitializer lazyInitializer = extractLazyInitializer( entity );
		if ( lazyInitializer != null ) {
			if ( lazyInitializer.isUninitialized() ) {
				// we have an uninitialized proxy, the attribute cannot be loaded
				return NOT_LOADED;
			}
			else {
				// swap the proxy with target (for proper class name resolution)
				entity = lazyInitializer.getImplementation();
			}
		}

		try {
			return getLoadState( getAttributeValue( entity, attributeName, cache ) );
		}
		catch (AttributeExtractionException ignore) {
			return UNKNOWN;
		}
	}

	private static Object getAttributeValue(Object entity, String attributeName, MetadataCache cache) {
		return cache.getClassMetadata( entity.getClass() )
				.getAttributeAccess( attributeName )
				.extractValue( entity );
	}

	public static class AttributeExtractionException extends HibernateException {
		public AttributeExtractionException(String message) {
			super( message );
		}

		public AttributeExtractionException(String message, Throwable cause) {
			super( message, cause );
		}
	}

	public interface AttributeAccess {
		Object extractValue(Object owner) throws AttributeExtractionException;
	}

	public static class FieldAttributeAccess implements AttributeAccess {
		private final String name;
		private final Field field;

		public FieldAttributeAccess(Field field) {
			this.name = field.getName();
			try {
				ReflectHelper.ensureAccessibility( field );
			}
			catch (Exception e) {
				this.field = null;
				return;
			}
			this.field = field;
		}

		@Override
		public Object extractValue(Object owner) {
			if ( field == null ) {
				throw new AttributeExtractionException( "Attribute (field) " + name + " is not accessible" );
			}

			try {
				return field.get( owner );
			}
			catch ( IllegalAccessException e ) {
				throw new AttributeExtractionException(
						"Unable to access attribute (field): " + field.getDeclaringClass().getName() + "#" + name,
						e
				);
			}
		}
	}

	public static class MethodAttributeAccess implements AttributeAccess {
		private final String name;
		private final Method method;

		public MethodAttributeAccess(String attributeName, Method method) {
			this.name = attributeName;
			try {
				ReflectHelper.ensureAccessibility( method );
			}
			catch (Exception e) {
				this.method = null;
				return;
			}
			this.method = method;
		}

		@Override
		public Object extractValue(Object owner) {
			if ( method == null ) {
				throw new AttributeExtractionException( "Attribute (method) " + name + " is not accessible" );
			}

			try {
				return method.invoke( owner );
			}
			catch ( IllegalAccessException e ) {
				throw new AttributeExtractionException(
						"Unable to access attribute (method): " + method.getDeclaringClass().getName() + "#" + name,
						e
				);
			}
			catch ( InvocationTargetException e ) {
				throw new AttributeExtractionException(
						"Unable to access attribute (method): " + method.getDeclaringClass().getName() + "#" + name,
						e.getCause()
				);
			}
		}
	}

	private static class NoSuchAttributeAccess implements AttributeAccess {
		private final Class<?> clazz;
		private final String attributeName;

		public NoSuchAttributeAccess(Class<?> clazz, String attributeName) {
			this.clazz = clazz;
			this.attributeName = attributeName;
		}

		@Override
		public Object extractValue(Object owner) throws AttributeExtractionException {
			throw new AttributeExtractionException( "No such attribute : " + clazz.getName() + "#" + attributeName );
		}
	}

	public static class ClassMetadataCache {
		private final Class<?> specifiedClass;
		private final List<Class<?>> classHierarchy;
		private final Map<String, AttributeAccess> attributeAccessMap = new HashMap<>();

		public ClassMetadataCache(Class<?> clazz) {
			this.specifiedClass = clazz;
			this.classHierarchy = findClassHierarchy( clazz );
		}

		private static List<Class<?>> findClassHierarchy(Class<?> clazz) {
			final List<Class<?>> classes = new ArrayList<>();
			Class<?> current = clazz;
			do {
				classes.add( current );
				current = current.getSuperclass();
			} while ( current != null );

			return classes;
		}

		public AttributeAccess getAttributeAccess(String attributeName) {
			AttributeAccess attributeAccess = attributeAccessMap.get( attributeName );
			if ( attributeAccess == null ) {
				attributeAccess = buildAttributeAccess( attributeName );
				attributeAccessMap.put( attributeName, attributeAccess );
			}
			return attributeAccess;
		}

		private AttributeAccess buildAttributeAccess(final String attributeName) {
			for ( Class<?> clazz : classHierarchy ) {
				try {
					return new FieldAttributeAccess( clazz.getDeclaredField( attributeName ) );
				}
				catch ( NoSuchFieldException e ) {
					final Method method = getMethod( clazz, attributeName );
					if ( method != null ) {
						return new MethodAttributeAccess( attributeName, method );
					}
				}
			}
			//we could not find any match
			return new NoSuchAttributeAccess( specifiedClass, attributeName );
		}
	}

	/**
	 * Returns the method with the specified name or <code>null</code> if it does not exist.
	 *
	 * @param clazz The class to check.
	 * @param attributeName The attribute name.
	 *
	 * @return Returns the method with the specified name or <code>null</code> if it does not exist.
	 */
	private static Method getMethod(Class<?> clazz, String attributeName) {
		try {
			final char[] string = attributeName.toCharArray();
			string[0] = Character.toUpperCase( string[0] );
			final String casedAttributeName = new String( string );
			try {
				return clazz.getDeclaredMethod( "get" + casedAttributeName );
			}
			catch ( NoSuchMethodException e ) {
				return clazz.getDeclaredMethod( "is" + casedAttributeName );
			}
		}
		catch ( NoSuchMethodException e ) {
			return null;
		}
	}

	/**
	 * Cache hierarchy and member resolution, taking care to not leak
	 * references to Class instances.
	 */
	public static final class MetadataCache implements Serializable {

		private final ClassValue<ClassMetadataCache> metadataCacheClassValue;

		public MetadataCache() {
			this( new MetadataClassValue() );
		}

		//To help with serialization: no need to serialize the actual metadataCacheClassValue field
		private MetadataCache(ClassValue<ClassMetadataCache> metadataCacheClassValue) {
			this.metadataCacheClassValue = metadataCacheClassValue;
		}

		Object writeReplace() throws ObjectStreamException {
			//Writing a different instance which doesn't include the cache
			return new MetadataCache(null);
		}

		private Object readResolve() throws ObjectStreamException {
			//Ensure we do instantiate a new cache instance on deserialization
			return new MetadataCache();
		}

		ClassMetadataCache getClassMetadata(final Class<?> clazz) {
			return metadataCacheClassValue.get( clazz );
		}

	}

	private static final class MetadataClassValue extends ClassValue<ClassMetadataCache> {
		@Override
		protected ClassMetadataCache computeValue(final Class type) {
			return new ClassMetadataCache( type );
		}
	}

}
