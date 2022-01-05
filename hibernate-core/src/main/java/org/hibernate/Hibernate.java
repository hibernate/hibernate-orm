/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Supplier;

import org.hibernate.bytecode.enhance.spi.interceptor.BytecodeLazyAttributeInterceptor;
import org.hibernate.bytecode.enhance.spi.interceptor.EnhancementAsProxyLazinessInterceptor;
import org.hibernate.collection.spi.PersistentBag;
import org.hibernate.collection.spi.PersistentList;
import org.hibernate.collection.spi.PersistentMap;
import org.hibernate.collection.spi.PersistentSet;
import org.hibernate.collection.spi.PersistentSortedMap;
import org.hibernate.collection.spi.PersistentSortedSet;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.HibernateIterator;
import org.hibernate.engine.jdbc.LobCreator;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.PersistentAttributeInterceptable;
import org.hibernate.engine.spi.PersistentAttributeInterceptor;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;

/**
 * <ul>
 * <li>Provides access to the full range of Hibernate built-in types. {@code Type}
 * instances may be used to bind values to query parameters.
 * <li>A factory for new {@link java.sql.Blob}s and {@link java.sql.Clob}s.
 * <li>Defines static methods for manipulation of proxies.
 * </ul>
 *
 * @author Gavin King
 * @see java.sql.Clob
 * @see java.sql.Blob
 * @see org.hibernate.type.Type
 */

public final class Hibernate {
	/**
	 * Cannot be instantiated.
	 */
	private Hibernate() {
		throw new UnsupportedOperationException();
	}


	/**
	 * Force initialization of a proxy or persistent collection.
	 * <p/>
	 * Note: This only ensures initialization of a proxy object or collection;
	 * it is not guaranteed that the elements INSIDE the collection will be initialized/materialized.
	 *
	 * @param proxy a persistable object, proxy, persistent collection or {@code null}
	 * @throws HibernateException if we can't initialize the proxy at this time, eg. the {@code Session} was closed
	 */
	public static void initialize(Object proxy) throws HibernateException {
		if ( proxy == null ) {
			return;
		}

		if ( proxy instanceof HibernateProxy ) {
			( (HibernateProxy) proxy ).getHibernateLazyInitializer().initialize();
		}
		else if ( proxy instanceof PersistentCollection ) {
			( (PersistentCollection) proxy ).forceInitialization();
		}
		else if ( proxy instanceof PersistentAttributeInterceptable ) {
			final PersistentAttributeInterceptable interceptable = (PersistentAttributeInterceptable) proxy;
			final PersistentAttributeInterceptor interceptor = interceptable.$$_hibernate_getInterceptor();
			if ( interceptor instanceof EnhancementAsProxyLazinessInterceptor ) {
				( (EnhancementAsProxyLazinessInterceptor) interceptor ).forceInitialize( proxy, null );
			}
		}
	}

	/**
	 * Check if the proxy or persistent collection is initialized.
	 *
	 * @param proxy a persistable object, proxy, persistent collection or {@code null}
	 * @return true if the argument is already initialized, or is not a proxy or collection
	 */
	@SuppressWarnings("SimplifiableIfStatement")
	public static boolean isInitialized(Object proxy) {
		if ( proxy instanceof HibernateProxy ) {
			return !( (HibernateProxy) proxy ).getHibernateLazyInitializer().isUninitialized();
		}
		else if ( proxy instanceof PersistentAttributeInterceptable ) {
			final PersistentAttributeInterceptor interceptor = ( (PersistentAttributeInterceptable) proxy ).$$_hibernate_getInterceptor();
			if ( interceptor instanceof EnhancementAsProxyLazinessInterceptor ) {
				return false;
			}
			return true;
		}
		else if ( proxy instanceof PersistentCollection ) {
			return ( (PersistentCollection) proxy ).wasInitialized();
		}
		else {
			return true;
		}
	}

	/**
	 * Get the true, underlying class of a proxied persistent class. This operation
	 * will initialize a proxy by side-effect.
	 *
	 * @param proxy a persistable object or proxy
	 * @return the true class of the instance
	 * @throws HibernateException
	 */
	public static Class getClass(Object proxy) {
		if ( proxy instanceof HibernateProxy ) {
			return ( (HibernateProxy) proxy ).getHibernateLazyInitializer()
					.getImplementation()
					.getClass();
		}
		else {
			return proxy.getClass();
		}
	}

	/**
	 * Obtain a lob creator for the given session.
	 *
	 * @param session The session for which to obtain a lob creator
	 *
	 * @return The log creator reference
	 */
	public static LobCreator getLobCreator(Session session) {
		return getLobCreator( (SessionImplementor) session );
	}

	/**
	 * Obtain a lob creator for the given session.
	 *
	 * @param session The session for which to obtain a lob creator
	 *
	 * @return The log creator reference
	 */
	public static LobCreator getLobCreator(SharedSessionContractImplementor session) {
		return session.getFactory()
				.getServiceRegistry()
				.getService( JdbcServices.class )
				.getLobCreator( session );
	}

	/**
	 * Obtain a lob creator for the given session.
	 *
	 * @param session The session for which to obtain a lob creator
	 *
	 * @return The log creator reference
	 */
	public static LobCreator getLobCreator(SessionImplementor session) {
		return session.getFactory()
				.getServiceRegistry()
				.getService( JdbcServices.class )
				.getLobCreator( session );
	}

	/**
	 * Close an {@link Iterator} instances obtained from {@link org.hibernate.Query#iterate()} immediately
	 * instead of waiting until the session is closed or disconnected.
	 *
	 * @param iterator an Iterator created by iterate()
	 *
	 * @throws HibernateException Indicates a problem closing the Hibernate iterator.
	 * @throws IllegalArgumentException If the Iterator is not a "Hibernate Iterator".
	 *
	 * @see Query#iterate()
	 */
	public static void close(Iterator iterator) throws HibernateException {
		if ( iterator instanceof HibernateIterator ) {
			( (HibernateIterator) iterator ).close();
		}
		else {
			throw new IllegalArgumentException( "not a Hibernate iterator" );
		}
	}

	/**
	 * Check if the property is initialized. If the named property does not exist
	 * or is not persistent, this method always returns {@code true}.
	 *
	 * @param proxy The potential proxy
	 * @param propertyName the name of a persistent attribute of the object
	 * @return true if the named property of the object is not listed as uninitialized; false otherwise
	 */
	public static boolean isPropertyInitialized(Object proxy, String propertyName) {
		final Object entity;
		if ( proxy instanceof HibernateProxy ) {
			final LazyInitializer li = ( (HibernateProxy) proxy ).getHibernateLazyInitializer();
			if ( li.isUninitialized() ) {
				return false;
			}
			else {
				entity = li.getImplementation();
			}
		}
		else {
			entity = proxy;
		}

		if ( entity instanceof PersistentAttributeInterceptable ) {
			PersistentAttributeInterceptor interceptor = ( (PersistentAttributeInterceptable) entity ).$$_hibernate_getInterceptor();
			if ( interceptor instanceof BytecodeLazyAttributeInterceptor ) {
				return ( (BytecodeLazyAttributeInterceptor) interceptor ).isAttributeLoaded( propertyName );
			}
		}

		return true;
	}

    /**
     * Unproxies a {@link HibernateProxy}. If the proxy is uninitialized, it automatically triggers an initialization.
     * In case the supplied object is null or not a proxy, the object will be returned as-is.
     *
     * @param proxy the {@link HibernateProxy} to be unproxied
     * @return the proxy's underlying implementation object, or the supplied object otherwise
     */
	public static Object unproxy(Object proxy) {
		if ( proxy instanceof HibernateProxy ) {
			HibernateProxy hibernateProxy = (HibernateProxy) proxy;
			LazyInitializer initializer = hibernateProxy.getHibernateLazyInitializer();
			return initializer.getImplementation();
		}
		else {
			return proxy;
		}
	}

	/**
	 * Unproxies a {@link HibernateProxy}. If the proxy is uninitialized, it automatically triggers an initialization.
	 * In case the supplied object is null or not a proxy, the object will be returned as-is.
	 *
	 * @param proxy the {@link HibernateProxy} to be unproxied
	 * @param entityClass the entity type
	 * @return the proxy's underlying implementation object, or the supplied object otherwise
	 */
	public static <T> T unproxy(T proxy, Class<T> entityClass) {
		return entityClass.cast( unproxy( proxy ) );
	}

	/**
	 * Obtain a detached, uninitialized reference (a proxy) for a persistent entity with the given identifier.
	 *
	 * The returned proxy is not associated with any session, and cannot be initialized by calling
	 * {@link #initialize(Object)}. It can be used to represent a reference to the entity when working with
	 * a detached object graph.
	 *
	 * @param sessionFactory the session factory with which the entity is associated
	 * @param entityClass the entity class
	 * @param id the id of the persistent entity instance
	 *
	 * @return a detached uninitialized proxy
	 */
	public static <E> E createDetachedProxy(SessionFactory sessionFactory, Class<E> entityClass, Object id) {
		EntityPersister persister =
				sessionFactory.unwrap(SessionFactoryImplementor.class).getMetamodel()
						.findEntityDescriptor(entityClass);
		if (persister==null) {
			throw new UnknownEntityTypeException("unknown entity type");
		}
		return (E) persister.createProxy(id, null);
	}

	/**
	 * Operations for obtaining references to persistent collections of a certain type.
	 *
	 * @param <C> the type of collection, for example, {@code List&lt;User&gt;}
	 */
	public static final class CollectionInterface<C> {
		private final Supplier<C> detached;
		private final Supplier<C> created;

		private CollectionInterface(Supplier<C> detached, Supplier<C> created) {
			this.detached = detached;
			this.created = created;
		}

		/**
		 * Obtain a detached, uninitialized persistent collection of the type represented by this object.
		 *
		 * The returned wrapper object is not associated with any session, and cannot be initialized by calling
		 * {@link #initialize(Object)}. It can be used to represent an uninitialized collection when working with
		 * a detached object graph.
		 *
		 * @return an uninitialized persistent collection
		 */
		public C createDetachedInstance() {
			return detached.get();
		}

		/**
		 * Instantiate an empty collection of the type represented by this object.
		 *
		 * @return a newly-instantiated empty collection
		 */
		public C createNewInstance() {
			return created.get();
		}
	}

	/**
	 * Obtain an instance of {@link CollectionInterface} representing persistent bags
	 * of a given element type.
	 *
	 * @param <U> the element type
	 */
	public static <U> CollectionInterface<Collection<U>> bag() {
		return new CollectionInterface<>(PersistentBag::new, ArrayList::new);
	}

	/**
	 * Obtain an instance of {@link CollectionInterface} representing persistent sets
	 * of a given element type.
	 *
	 * @param <U> the element type
	 */
	public static <U> CollectionInterface<Set<U>> set() {
		return new CollectionInterface<>(PersistentSet::new, HashSet::new);
	}

	/**
	 * Obtain an instance of {@link CollectionInterface} representing persistent lists
	 * of a given element type.
	 *
	 * @param <U> the element type
	 */
	public static <U> CollectionInterface<List<U>> list() {
		return new CollectionInterface<>(PersistentList::new, ArrayList::new);
	}

	/**
	 * Obtain an instance of {@link CollectionInterface} representing persistent maps
	 * of a given key and value types.
	 *
	 * @param <U> the key type
	 * @param <V> the value type
	 */
	public static <U,V> CollectionInterface<Map<U,V>> map() {
		return new CollectionInterface<>(PersistentMap::new, HashMap::new);
	}

	/**
	 * Obtain an instance of {@link CollectionInterface} representing sorted persistent sets
	 * of a given element type.
	 *
	 * @param <U> the element type
	 */
	public static <U> CollectionInterface<SortedSet<U>> sortedSet() {
		return new CollectionInterface<>(PersistentSortedSet::new, TreeSet::new);
	}

	/**
	 * Obtain an instance of {@link CollectionInterface} representing sorted persistent maps
	 * of a given key and value types.
	 *
	 * @param <U> the key type
	 * @param <V> the value type
	 *
	 */
	public static <U,V> CollectionInterface<Map<U,V>> sortedMap() {
		return new CollectionInterface<>(PersistentSortedMap::new, TreeMap::new);
	}

	/**
	 * Obtain an instance of {@link CollectionInterface} representing persistent collections
	 * of the given type.
	 *
	 * @param collectionClass the Java class object representing the collection type
	 */
	@SuppressWarnings("unchecked")
	public static <C> CollectionInterface<C> collection(Class<C> collectionClass) {
		if (collectionClass == List.class) {
			return (CollectionInterface<C>) list();
		}
		else if (collectionClass == Set.class) {
			return (CollectionInterface<C>) set();
		}
		else if (collectionClass == Map.class) {
			return (CollectionInterface<C>) map();
		}
		if (collectionClass == SortedMap.class) {
			return (CollectionInterface<C>) sortedMap();
		}
		else if (collectionClass == SortedSet.class) {
			return (CollectionInterface<C>) sortedSet();
		}
		else if (collectionClass == Collection.class) {
			return (CollectionInterface<C>) bag();
		}
		else {
			throw new IllegalArgumentException("illegal collection interface type");
		}
	}
}
