/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.internal.util;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import javax.persistence.spi.LoadState;

import org.hibernate.HibernateException;
import org.hibernate.bytecode.enhance.spi.interceptor.AbstractLazyLoadInterceptor;
import org.hibernate.bytecode.enhance.spi.interceptor.BytecodeLazyAttributeInterceptor;
import org.hibernate.bytecode.enhance.spi.interceptor.EnhancementAsProxyLazinessInterceptor;
import org.hibernate.bytecode.enhance.spi.interceptor.LazyAttributeLoadingInterceptor;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.PersistentAttributeInterceptable;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;

/**
 * Central delegate for handling calls from:<ul>
 *     <li>{@link javax.persistence.PersistenceUtil#isLoaded(Object)}</li>
 *     <li>{@link javax.persistence.PersistenceUtil#isLoaded(Object, String)}</li>
 *     <li>{@link javax.persistence.spi.ProviderUtil#isLoaded(Object)}</li>
 *     <li>{@link javax.persistence.spi.ProviderUtil#isLoadedWithReference(Object, String)}</li>
 *     <li>{@link javax.persistence.spi.ProviderUtil#isLoadedWithoutReference(Object, String)}li>
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
	 * <p/>
	 * Return is defined as follows:<ol>
	 *     <li>
	 *         If the reference is a {@link HibernateProxy}, we return {@link LoadState#LOADED} if
	 *         {@link org.hibernate.proxy.LazyInitializer#isUninitialized()} returns {@code false}; else we return
	 *         {@link LoadState#NOT_LOADED}
	 *     </li>
	 *     <li>
	 *         If the reference is an enhanced (by Hibernate) entity, we return {@link LoadState#LOADED} if
	 *         {@link LazyAttributeLoadingInterceptor#hasAnyUninitializedAttributes()} returns {@code false};
	 *         otherwise we return {@link LoadState#NOT_LOADED}
	 *     </li>
	 *     <li>
	 *         If the reference is a {@link PersistentCollection}, we return {@link LoadState#LOADED} if
	 *         {@link org.hibernate.collection.spi.PersistentCollection#wasInitialized()} returns {@code true}; else
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
	public static LoadState isLoaded(Object reference) {
		if ( reference instanceof HibernateProxy ) {
			final boolean isInitialized = !( (HibernateProxy) reference ).getHibernateLazyInitializer().isUninitialized();
			return isInitialized ? LoadState.LOADED : LoadState.NOT_LOADED;
		}
		else if ( reference instanceof PersistentAttributeInterceptable ) {
			boolean isInitialized = isInitialized( (PersistentAttributeInterceptable) reference );
			return isInitialized ? LoadState.LOADED : LoadState.NOT_LOADED;
		}
		else if ( reference instanceof PersistentCollection ) {
			final boolean isInitialized = ( (PersistentCollection) reference ).wasInitialized();
			return isInitialized ? LoadState.LOADED : LoadState.NOT_LOADED;
		}
		else {
			return LoadState.UNKNOWN;
		}
	}

	@SuppressWarnings("SimplifiableIfStatement")
	private static boolean isInitialized(PersistentAttributeInterceptable interceptable) {
		final BytecodeLazyAttributeInterceptor interceptor = extractInterceptor( interceptable );
		return interceptable == null || interceptor == null || !interceptor.hasAnyUninitializedAttributes();
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
		boolean sureFromUs = false;
		if ( entity instanceof HibernateProxy ) {
			LazyInitializer li = ( (HibernateProxy) entity ).getHibernateLazyInitializer();
			if ( li.isUninitialized() ) {
				// we have an uninitialized proxy, the attribute cannot be loaded
				return LoadState.NOT_LOADED;
			}
			else {
				// swap the proxy with target (for proper class name resolution)
				entity = li.getImplementation();
			}
			sureFromUs = true;
		}

		// we are instrumenting but we can't assume we are the only ones
		if ( entity instanceof PersistentAttributeInterceptable ) {
			final BytecodeLazyAttributeInterceptor interceptor = extractInterceptor( (PersistentAttributeInterceptable) entity );
			final boolean isInitialized = interceptor == null || interceptor.isAttributeLoaded( attributeName );
			LoadState state;
			if (isInitialized && interceptor != null) {
				// attributeName is loaded according to bytecode enhancement, but is it loaded as far as association?
				// it's ours, we can read
				try {
					final Class entityClass = entity.getClass();
					final Object attributeValue = cache.getClassMetadata( entityClass )
							.getAttributeAccess( attributeName )
							.extractValue( entity );
					state = isLoaded( attributeValue );

					// it's ours so we know it's loaded
					if ( state == LoadState.UNKNOWN ) {
						state = LoadState.LOADED;
					}
				}
				catch (AttributeExtractionException ignore) {
					state = LoadState.UNKNOWN;
				}
			}
			else if ( interceptor != null ) {
				state = LoadState.NOT_LOADED;
			}
			else if ( sureFromUs ) {
				// property is loaded according to bytecode enhancement, but is it loaded as far as association?
				// it's ours, we can read
				try {
					final Class entityClass = entity.getClass();
					final Object attributeValue = cache.getClassMetadata( entityClass )
							.getAttributeAccess( attributeName )
							.extractValue( entity );
					state = isLoaded( attributeValue );

					// it's ours so we know it's loaded
					if ( state == LoadState.UNKNOWN ) {
						state = LoadState.LOADED;
					}
				}
				catch (AttributeExtractionException ignore) {
					state = LoadState.UNKNOWN;
				}
			}
			else {
				state = LoadState.UNKNOWN;
			}

			return state;
		}
		else {
			return LoadState.UNKNOWN;
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
		if ( entity instanceof HibernateProxy ) {
			final LazyInitializer li = ( (HibernateProxy) entity ).getHibernateLazyInitializer();
			if ( li.isUninitialized() ) {
				// we have an uninitialized proxy, the attribute cannot be loaded
				return LoadState.NOT_LOADED;
			}
			else {
				// swap the proxy with target (for proper class name resolution)
				entity = li.getImplementation();
			}
		}

		try {
			final Class entityClass = entity.getClass();
			final Object attributeValue = cache.getClassMetadata( entityClass )
					.getAttributeAccess( attributeName )
					.extractValue( entity );
			return isLoaded( attributeValue );
		}
		catch (AttributeExtractionException ignore) {
			return LoadState.UNKNOWN;
		}
	}


	public static class AttributeExtractionException extends HibernateException {
		public AttributeExtractionException(String message) {
			super( message );
		}

		public AttributeExtractionException(String message, Throwable cause) {
			super( message, cause );
		}
	}

	public static interface AttributeAccess {
		public Object extractValue(Object owner) throws AttributeExtractionException;
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
		private final Class clazz;
		private final String attributeName;

		public NoSuchAttributeAccess(Class clazz, String attributeName) {
			this.clazz = clazz;
			this.attributeName = attributeName;
		}

		@Override
		public Object extractValue(Object owner) throws AttributeExtractionException {
			throw new AttributeExtractionException( "No such attribute : " + clazz.getName() + "#" + attributeName );
		}
	}

	public static class ClassMetadataCache {
		private final Class specifiedClass;
		private List<Class<?>> classHierarchy;
		private Map<String, AttributeAccess> attributeAccessMap = new HashMap<String, AttributeAccess>();

		public ClassMetadataCache(Class<?> clazz) {
			this.specifiedClass = clazz;
			this.classHierarchy = findClassHierarchy( clazz );
		}

		private static List<Class<?>> findClassHierarchy(Class<?> clazz) {
			List<Class<?>> classes = new ArrayList<Class<?>>();
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
			final PrivilegedAction<AttributeAccess> action = new PrivilegedAction<AttributeAccess>() {
				@Override
				public AttributeAccess run() {
					for ( Class clazz : classHierarchy ) {
						try {
							final Field field = clazz.getDeclaredField( attributeName );
							if ( field != null ) {
								return new FieldAttributeAccess( field );
							}
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
			};
			return System.getSecurityManager() != null ? AccessController.doPrivileged( action ) : action.run();
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
			char[] string = attributeName.toCharArray();
			string[0] = Character.toUpperCase( string[0] );
			String casedAttributeName = new String( string );
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
	 * Cache hierarchy and member resolution in a weak hash map
	 */
	//TODO not really thread-safe
	public static class MetadataCache implements Serializable {
		private transient Map<Class<?>, ClassMetadataCache> classCache = new WeakHashMap<Class<?>, ClassMetadataCache>();


		private void readObject(java.io.ObjectInputStream stream) {
			classCache = new WeakHashMap<Class<?>, ClassMetadataCache>();
		}

		ClassMetadataCache getClassMetadata(Class<?> clazz) {
			ClassMetadataCache classMetadataCache = classCache.get( clazz );
			if ( classMetadataCache == null ) {
				classMetadataCache = new ClassMetadataCache( clazz );
				classCache.put( clazz, classMetadataCache );
			}
			return classMetadataCache;
		}
	}

}
