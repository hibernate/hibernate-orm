/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.Incubating;
import org.hibernate.Internal;
import org.hibernate.MappingException;
import org.hibernate.collection.spi.AbstractPersistentCollection;
import org.hibernate.collection.spi.PersistentArrayHolder;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.CollectionKey;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.MarkerObject;
import org.hibernate.metamodel.CollectionClassification;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.Joinable;


import org.checkerframework.checker.nullness.qual.Nullable;

import static org.hibernate.bytecode.enhance.spi.LazyPropertyInitializer.UNFETCHED_PROPERTY;
import static org.hibernate.internal.CoreMessageLogger.CORE_LOGGER;
import static org.hibernate.internal.util.collections.ArrayHelper.EMPTY_BOOLEAN_ARRAY;
import static org.hibernate.internal.util.collections.ArrayHelper.EMPTY_INT_ARRAY;
import static org.hibernate.internal.util.collections.CollectionHelper.mapOfSize;
import static org.hibernate.pretty.MessageHelper.collectionInfoString;
import static org.hibernate.proxy.HibernateProxy.extractLazyInitializer;

/**
 * A type that handles Hibernate {@code PersistentCollection}s (including arrays).
 *
 * @author Gavin King
 */
public abstract class CollectionType extends AbstractType implements AssociationType {

	@Internal
	public static final Object UNFETCHED_COLLECTION = new MarkerObject( "UNFETCHED COLLECTION" );

	private final String role;
	private final String foreignKeyPropertyName;

	// the need for the persister if very hot in many use cases: cache it in a field
	// TODO initialize it at constructor time
	private volatile CollectionPersister persister;

	public CollectionType(String role, String foreignKeyPropertyName) {
		this.role = role;
		this.foreignKeyPropertyName = foreignKeyPropertyName;
	}

	public abstract CollectionClassification getCollectionClassification();

	public String getRole() {
		return role;
	}

	public Object indexOf(Object collection, Object element) {
		throw new UnsupportedOperationException( "generic collections don't have indexes" );
	}

	public boolean contains(Object collection, Object childObject, SharedSessionContractImplementor session) {
		// we do not have to worry about queued additions to uninitialized
		// collections, since they can only occur for inverse collections!
		final var elems = getElementsIterator( collection );
		while ( elems.hasNext() ) {
			final Object maybeProxy = elems.next();
			// worrying about proxies is perhaps a little bit of overkill here...
			final var initializer = extractLazyInitializer( maybeProxy );
			final Object element =
					initializer != null && !initializer.isUninitialized()
							? initializer.getImplementation()
							: maybeProxy;
			if ( element == childObject ) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean isCollectionType() {
		return true;
	}

	@Override
	public final boolean isEqual(Object x, Object y) {
		return x == y
			|| x instanceof PersistentCollection<?> xc && isEqual( xc, y )
			|| y instanceof PersistentCollection<?> yc && isEqual( yc, x );
	}

	private boolean isEqual(PersistentCollection<?> collection, Object other) {
		return collection.wasInitialized()
			&& ( collection.isWrapper( other ) || collection.isDirectlyProvidedCollection( other ) );
	}

	@Override
	public int compare(Object x, Object y) {
		return 0; // collections cannot be compared
	}

	@Override
	public int compare(Object x, Object y, SessionFactoryImplementor sessionFactory) {
		return compare( x, y );
	}

	@Override
	public int getHashCode(Object x) {
		throw new UnsupportedOperationException( "cannot doAfterTransactionCompletion lookups on collections" );
	}

	/**
	 * Instantiate an uninitialized collection wrapper or holder. Callers MUST add the holder to the
	 * persistence context!
	 *
	 * @param session The session from which the request is originating.
	 * @param persister The underlying collection persister (metadata)
	 * @param key The owner key.
	 * @return The instantiated collection.
	 */
	public abstract PersistentCollection<?> instantiate(SharedSessionContractImplementor session, CollectionPersister persister, Object key);

	@Override
	public final void nullSafeSet(PreparedStatement st, Object value, int index, boolean[] settable,
			SharedSessionContractImplementor session) throws HibernateException, SQLException {
		//NOOP
	}

	@Override
	public void nullSafeSet(PreparedStatement st, Object value, int index,
			SharedSessionContractImplementor session) throws HibernateException, SQLException {
	}

	@Override
	public int[] getSqlTypeCodes(MappingContext mappingContext) throws MappingException {
		return EMPTY_INT_ARRAY;
	}

	@Override
	public int getColumnSpan(MappingContext session) throws MappingException {
		return 0;
	}

	@Override
	public String toLoggableString(Object value, SessionFactoryImplementor factory) {
		if ( value == null ) {
			return "null";
		}
		else {
			if ( !getReturnedClass().isInstance( value )
					&& !(value instanceof PersistentCollection) ) {
				final var persister = getPersister( factory );
				final Type keyType = persister.getKeyType();
				final Type identifierType = persister.getIdentifierType();
				// its most likely the collection-key
				if ( keyType.getReturnedClass().isInstance( value ) ) {
					return getRole() + "#" + keyType.toLoggableString( value, factory );
				}
				// although it could also be the collection-id
				else if ( identifierType != null
						&& identifierType.getReturnedClass().isInstance( value ) ) {
					return getRole() + "#" + identifierType.toLoggableString( value, factory );
				}
			}
			return renderLoggableString( value, factory );
		}
	}

	protected String renderLoggableString(Object value, SessionFactoryImplementor factory) {
		if ( !Hibernate.isInitialized( value ) ) {
			return "<uninitialized>";
		}
		else {
			final List<String> list = new ArrayList<>();
			final Type elemType = getElementType( factory );
			getElementsIterator( value )
					.forEachRemaining( element -> list.add( elementString( factory, element, elemType ) ) );
			return list.toString();
		}
	}

	private static String elementString(SessionFactoryImplementor factory, Object element, Type elemType) {
		return element == UNFETCHED_PROPERTY || !Hibernate.isInitialized( element )
				? "<uninitialized>"
				: elemType.toLoggableString( element, factory );
	}

	@Override
	public Object deepCopy(Object value, SessionFactoryImplementor factory) {
		return value;
	}

	@Override
	public String getName() {
		return getReturnedClassName() + '(' + getRole() + ')';
	}

	/**
	 * Get an iterator over the element set of the collection, which may not yet be wrapped
	 *
	 * @param collection The collection to be iterated
	 * @param session The session from which the request is originating.
	 * @return The iterator.
	 *
	 * @deprecated use {@link #getElementsIterator(Object)}
	 */
	@Deprecated
	public Iterator<?> getElementsIterator(Object collection, SharedSessionContractImplementor session) {
		return getElementsIterator( collection );
	}

	/**
	 * Get an iterator over the element set of the collection, which may not yet be wrapped
	 *
	 * @param collection The collection to be iterated
	 * @return The element iterator
	 */
	public Iterator<?> getElementsIterator(Object collection) {
		return ( (Collection<?>) collection ).iterator();
	}

	@Override
	public boolean isMutable() {
		return false;
	}

	@Internal @Incubating
	public boolean isInverse(SessionFactoryImplementor factory) {
		return getPersister( factory ).isInverse();
	}

	@Override
	public Serializable disassemble(Object value, SharedSessionContractImplementor session, Object owner)
			throws HibernateException {
		//remember the uk value

		//This solution would allow us to eliminate the owner arg to disassemble(), but
		//what if the collection was null, and then later had elements added? seems unsafe
		//session.getPersistenceContext().getCollectionEntry( (PersistentCollection) value ).getKey();

		final Object key = getKeyOfOwner( owner, session );
		return key == null ? null
				: getPersister( session )
						.getKeyType()
						.disassemble( key, session, owner );
	}

	@Override
	public Serializable disassemble(Object value, SessionFactoryImplementor sessionFactory) throws HibernateException {
		throw new UnsupportedOperationException( "CollectionType not supported as part of cache key!" );
	}

	@Override
	public Object assemble(Serializable cached, SharedSessionContractImplementor session, Object owner)
			throws HibernateException {
		//we must use the "remembered" uk value, since it is
		//not available from the EntityEntry during assembly
		if ( cached == null ) {
			return null;
		}
		else {
			final Object key =
					getPersister( session )
							.getKeyType()
							.assemble( cached, session, owner);
			return resolveKey( key, session, owner );
		}
	}

	/**
	 * Get our underlying collection persister (using the session to access the
	 * factory).
	 *
	 * @param session The session from which the request is originating.
	 * @return The underlying collection persister
	 */
	private CollectionPersister getPersister(SharedSessionContractImplementor session) {
		return getPersister( session.getFactory() );
	}

	private CollectionPersister getPersister(SessionFactoryImplementor factory) {
		CollectionPersister persister = this.persister;
		if ( persister != null ) {
			return persister;
		}
		else {
			synchronized ( this ) {
				persister  = this.persister;
				if ( persister != null ) {
					return persister;
				}
				else {
					persister = factory.getMappingMetamodel().getCollectionDescriptor( role );
					this.persister = persister;
					return persister;
				}
			}
		}
	}

	@Override
	public boolean isDirty(Object old, Object current, SharedSessionContractImplementor session)
			throws HibernateException {
		// collections don't dirty an un-versioned parent entity
		// TODO: I don't really like this implementation; it would be
		//       better if this was handled by searchForDirtyCollections()
		return super.isDirty( old, current, session );
		// return false;

	}

	@Override
	public boolean isDirty(Object old, Object current, boolean[] checkable, SharedSessionContractImplementor session)
			throws HibernateException {
		return isDirty( old, current, session );
	}

	/**
	 * Wrap the naked collection instance in a wrapper, or instantiate a
	 * holder. Callers <b>MUST</b> add the holder to the persistence context!
	 *
	 * @param session The session from which the request is originating.
	 * @param collection The bare collection to be wrapped.
	 * @return The wrapped collection.
	 */
	public abstract PersistentCollection<?> wrap(SharedSessionContractImplementor session, Object collection);

	/**
	 * Note: return true because this type is castable to {@code AssociationType}. Not because
	 * all collections are associations.
	 */
	@Override
	public boolean isAssociationType() {
		return true;
	}

	@Override
	public ForeignKeyDirection getForeignKeyDirection() {
		return ForeignKeyDirection.TO_PARENT;
	}

	/**
	 * Get the key value from the owning entity instance, usually the identifier, but might be some
	 * other unique key, in the case of property-ref
	 *
	 * @param owner The collection owner
	 * @param session The session from which the request is originating.
	 * @return The collection owner's key
	 */
	public @Nullable Object getKeyOfOwner(Object owner, SharedSessionContractImplementor session) {
		final var entityEntry = session.getPersistenceContextInternal().getEntry( owner );
		if ( entityEntry == null ) {
			// This just handles a particular case of component
			// projection, perhaps get rid of it and throw an exception
			return null;
		}
		else if ( foreignKeyPropertyName == null ) {
			return entityEntry.getId();
		}
		else {
			final Object loadedValue = entityEntry.getLoadedValue( foreignKeyPropertyName );
			return loadedValue == null
					? entityEntry.getPersister().getPropertyValue( owner, foreignKeyPropertyName )
					: loadedValue;
		}
	}

	/**
	 * Get the id value from the owning entity key, usually the same as the key, but might be some
	 * other property, in the case of property-ref
	 *
	 * @param key The collection owner key
	 * @param session The session from which the request is originating.
	 * @return The collection owner's id, if it can be obtained from the key;
	 * otherwise, null is returned
	 */
	public Object getIdOfOwnerOrNull(Object key, SharedSessionContractImplementor session) {
		if ( foreignKeyPropertyName == null ) {
			return key;
		}
		else {
			final var ownerPersister = getPersister( session ).getOwnerEntityPersister();
			// TODO: Fix this so it will work for non-POJO entity mode
			final var keyClass = keyClass( session );
			if ( ownerPersister.getMappedClass().isAssignableFrom( keyClass )
					&& keyClass.isInstance( key ) ) {
				// the key is the owning entity itself, so get the ID from the key
				return ownerPersister.getIdentifier( key, session );
			}
			else {
				// TODO: check if key contains the owner ID
				return null;
			}
		}
	}

	private Class<?> keyClass(SharedSessionContractImplementor session) {
		return getPersister( session ).getKeyType().getReturnedClass();
	}

	private Object resolveKey(Object key, SharedSessionContractImplementor session, Object owner) {
		// if (key==null) throw new AssertionFailure("owner identifier unknown when re-assembling
		// collection reference");
		return key == null ? null : // TODO: can this case really occur??
			getCollection( key, session, owner, null );
	}

	public boolean isArrayType() {
		return false;
	}

	@Override
	public boolean useLHSPrimaryKey() {
		return foreignKeyPropertyName == null;
	}

	@Override
	public String getRHSUniqueKeyPropertyName() {
		return null;
	}

	@Override
	public Joinable getAssociatedJoinable(SessionFactoryImplementor factory)
			throws MappingException {
		return (Joinable) factory.getMappingMetamodel().getCollectionDescriptor( role );
	}

	@Override
	public boolean isModified(Object old, Object current, boolean[] checkable, SharedSessionContractImplementor session) {
		return false;
	}

	@Override
	public String getAssociatedEntityName(SessionFactoryImplementor factory)
			throws MappingException {
		final var persister = factory.getMappingMetamodel().getCollectionDescriptor( role );
		if ( persister.getElementType().isEntityType() ) {
			return persister.getElementPersister().getEntityName();
		}
		else {
			throw new MappingException( "Collection is not an association: " + persister.getRole() );
		}
	}

	/**
	 * Replace the elements of a collection with the elements of another collection.
	 *
	 * @param original The 'source' of the replacement elements (where we copy from)
	 * @param target The target of the replacement elements (where we copy to)
	 * @param owner The owner of the collection being merged
	 * @param copyCache The map of elements already replaced.
	 * @param session The session from which the merge event originated.
	 * @return The merged collection.
	 */
	@SuppressWarnings({"rawtypes", "unchecked"})
	public Object replaceElements(
			Object original,
			Object target,
			Object owner,
			Map<Object, Object> copyCache,
			SharedSessionContractImplementor session) {
		final var result = (Collection) target;
		result.clear();

		// copy elements into newly empty target collection
		final Type elemType = getElementType( session.getFactory() );
		for ( Object element : (Collection<?>) original ) {
			result.add( elemType.replace( element, null, session, owner, copyCache ) );
		}

		// if the original is a PersistentCollection, and that original
		// was not flagged as dirty, then reset the target's dirty flag
		// here after the copy operation.
		//
		// One thing to be careful of here is a "bare" original collection
		// in which case we should never ever ever reset the dirty flag
		// on the target because we simply do not know...
		if ( original instanceof PersistentCollection<?> originalPersistentCollection
				&& result instanceof PersistentCollection<?> resultPersistentCollection) {
			preserveSnapshot( originalPersistentCollection, resultPersistentCollection,
					elemType, owner, copyCache, session );
			if ( !originalPersistentCollection.isDirty() ) {
				resultPersistentCollection.clearDirty();
			}
		}

		return result;
	}

	private void preserveSnapshot(
			PersistentCollection<?> original,
			PersistentCollection<?> result,
			Type elemType,
			Object owner,
			Map<Object, Object> copyCache,
			SharedSessionContractImplementor session) {
		final var collectionEntry = session.getPersistenceContextInternal().getCollectionEntry( result );
		if ( collectionEntry != null ) {
			collectionEntry.resetStoredSnapshot( result,
					createSnapshot( original, result, elemType, owner, copyCache, session ) );
		}
	}

	private static Serializable createSnapshot(
			PersistentCollection<?> original,
			PersistentCollection<?> result,
			Type elemType,
			Object owner,
			Map<Object, Object> copyCache,
			SharedSessionContractImplementor session) {
		final Serializable originalSnapshot = original.getStoredSnapshot();
		if ( originalSnapshot instanceof List<?> list ) {
			return createListSnapshot( list, elemType, owner, copyCache, session );
		}
		else if ( originalSnapshot instanceof Map<?,?> map ) {
			return createMapSnapshot( map, result, elemType, owner, copyCache, session );
		}
		else if ( originalSnapshot instanceof Object[] array ) {
			return createArraySnapshot( array, elemType, owner, copyCache, session );
		}
		else {
			// retain the same snapshot
			return result.getStoredSnapshot();
		}
	}

	private static Serializable createArraySnapshot(
			Object[] array,
			Type elemType,
			Object owner,
			Map<Object, Object> copyCache,
			SharedSessionContractImplementor session) {
		for ( int i = 0; i < array.length; i++ ) {
			array[i] = elemType.replace( array[i], null, session, owner, copyCache );
		}
		return array;
	}

	private static <K,V> Serializable createMapSnapshot(
			Map<K, V> map,
			PersistentCollection<?> result,
			Type elemType,
			Object owner,
			Map<Object, Object> copyCache,
			SharedSessionContractImplementor session) {
		final Map<K, V> targetMap;
		final Serializable snapshot;
		if ( map instanceof SortedMap<K,V> sortedMap ) {
			final TreeMap<K, V> treeMap = new TreeMap<>( sortedMap.comparator() );
			targetMap = treeMap;
			snapshot = treeMap;
		}
		else {
			final HashMap<K, V> hashMap = mapOfSize( map.size() );
			targetMap = hashMap;
			snapshot = hashMap;
		}
		final var resultSnapshot = (Map<?,?>) result.getStoredSnapshot();
		for ( var entry : map.entrySet() ) {
			final K key = entry.getKey();
			final V value = entry.getValue();
			final Object resultSnapshotValue = resultSnapshot == null ? null : resultSnapshot.get( key );
			final Object newValue = elemType.replace( value, resultSnapshotValue, session, owner, copyCache );
			//noinspection unchecked
			targetMap.put( key == value ? (K) newValue : key, (V) newValue );
		}
		return snapshot;
	}

	private static ArrayList<Object> createListSnapshot(
			List<?> list,
			Type elemType,
			Object owner,
			Map<Object, Object> copyCache,
			SharedSessionContractImplementor session) {
		final ArrayList<Object> targetList = new ArrayList<>( list.size() );
		for ( Object obj : list ) {
			targetList.add( elemType.replace( obj, null, session, owner, copyCache ) );
		}
		return targetList;
	}

	/**
	 * Instantiate a new "underlying" collection exhibiting the same capacity
	 * characteristics and the passed "original".
	 *
	 * @param original The original collection.
	 * @return The newly instantiated collection.
	 */
	protected Object instantiateResult(Object original) {
		// by default just use an unanticipated capacity since we don't
		// know how to extract the capacity to use from original here...
		return instantiate( -1 );
	}

	/**
	 * Instantiate an empty instance of the "underlying" collection (not a wrapper),
	 * but with the given anticipated size (i.e. accounting for initial capacity
	 * and perhaps load factor).
	 *
	 * @param anticipatedSize The anticipated size of the instantiated collection
	 * after we are done populating it.
	 * @return A newly instantiated collection to be wrapped.
	 */
	public abstract Object instantiate(int anticipatedSize);

	@Override
	public Object replace(
			final Object original,
			final Object target,
			final SharedSessionContractImplementor session,
			final Object owner,
			final Map<Object, Object> copyCache) throws HibernateException {
		if ( original == null ) {
			return replaceNullOriginal( target, session );
		}
		else if ( !Hibernate.isInitialized( original ) ) {
			return replaceUninitializedOriginal( original, target, session, copyCache );
		}
		else {
			return replaceOriginal( original, target, session, owner, copyCache );
		}
	}

	private Object replaceOriginal(
			Object original, Object target,
			SharedSessionContractImplementor session,
			Object owner,
			Map<Object, Object> copyCache) {

		// for arrays, replaceElements() may return a different reference, since
		// the array length might not match
		final Object result =
				replaceElements( original,
						instantiateResultIfNecessary( original, target ),
						owner, copyCache, session );
		if ( original == target ) {
			// get the elements back into the target making sure to handle dirty flag
			final boolean wasClean =
					target instanceof PersistentCollection<?> collection
							&& !collection.isDirty();
			if ( target instanceof PersistentCollection<?> oldCollection
					&& oldCollection.isDirectlyAccessible() ) {
				// When a replacement or merge is requested and the underlying collection is directly accessible,
				// use a new persistent collection, to avoid potential issues like the underlying collection being
				// unmodifiable and hence failing the element replacement
				final var collectionPersister = getPersister( session );
				final Object key = oldCollection.getKey();
				final var newCollection = instantiate( session, collectionPersister, key );
				newCollection.initializeEmptyCollection( collectionPersister );
				newCollection.setSnapshot( key, oldCollection.getRole(), oldCollection.getStoredSnapshot() );
				session.getPersistenceContextInternal()
						.replaceCollection( collectionPersister, oldCollection, newCollection );
				target = newCollection;
			}
			//TODO: this is a little inefficient, don't need to do a whole
			//      deep replaceElements() call
			replaceElements( result, target, owner, copyCache, session );
			if ( wasClean ) {
				((PersistentCollection<?>) target).clearDirty();
			}
			return target;
		}
		else {
			return result;
		}
	}

	private Object instantiateResultIfNecessary(Object original, Object target) {
		// for a null target, or a target which is the same as the original,
		// we need to put the merged elements in a new collection
		return target == null
			|| target == original
			|| target == UNFETCHED_PROPERTY
			|| target instanceof PersistentCollection<?> collection && collection.isWrapper( original )
				? instantiateResult( original )
				: target;
	}

	private Object replaceUninitializedOriginal(
			Object original,
			Object target,
			SharedSessionContractImplementor session,
			Map<Object, Object> copyCache) {
		final var collection = (PersistentCollection<?>) original;
		if ( collection.hasQueuedOperations() ) {
			if ( original == target ) {
				// A managed entity with an uninitialized collection is being merged,
				// We need to replace any detached entities in the queued operations
				// with managed copies.
				final var apc = (AbstractPersistentCollection<?>) original;
				apc.replaceQueuedOperationValues( getPersister( session ), copyCache );
			}
			else {
				// original is a detached copy of the collection;
				// it contains queued operations, which will be ignored
				CORE_LOGGER.ignoreQueuedOperationsOnMerge(
						collectionInfoString( getRole(), collection.getKey() ) );
			}
		}
		return target;
	}

	private static Object replaceNullOriginal(Object target, SharedSessionContractImplementor session) {
		if ( target == null ) {
			return null;
		}
		else if ( target instanceof Collection<?> collection ) {
			collection.clear();
			return collection;
		}
		else if ( target instanceof Map<?,?> map ) {
			map.clear();
			return map;
		}
		else {
			final var persistenceContext = session.getPersistenceContext();
			final var collectionHolder = persistenceContext.getCollectionHolder( target );
			if ( collectionHolder != null ) {
				if ( collectionHolder instanceof PersistentArrayHolder<?> arrayHolder ) {
					persistenceContext.removeCollectionHolder( target );
					arrayHolder.beginRead();
					final var attributeMapping =
							persistenceContext.getCollectionEntry( collectionHolder )
									.getLoadedPersister().getAttributeMapping();
					arrayHolder.injectLoadedState( attributeMapping, null );
					arrayHolder.endRead();
					arrayHolder.dirty();
					persistenceContext.addCollectionHolder( collectionHolder );
					return arrayHolder.getArray();
				}
			}
		}
		return null;
	}

	/**
	 * Get the Hibernate type of the collection elements
	 *
	 * @param factory The session factory.
	 * @return The type of the collection elements
	 * @throws MappingException Indicates the underlying persister could not be located.
	 */
	public final Type getElementType(SessionFactoryImplementor factory) throws MappingException {
		return factory.getMappingMetamodel().getCollectionDescriptor( role ).getElementType();
	}

	@Override
	public String toString() {
		return getClass().getName() + '(' + getRole() + ')';
	}

	/**
	 * instantiate a collection wrapper (called when loading an object)
	 *
	 * @param key The collection owner key
	 * @param session The session from which the request is originating.
	 * @param owner The collection owner
	 * @return The collection
	 */
	public Object getCollection(Object key, SharedSessionContractImplementor session, Object owner, Boolean overridingEager) {
		final var persister = getPersister( session );
		final var persistenceContext = session.getPersistenceContextInternal();
		final var collectionKey = new CollectionKey( persister, key );
		// check if collection is currently being loaded
		final var loadingCollectionEntry =
				persistenceContext.getLoadContexts().findLoadingCollectionEntry( collectionKey );
		PersistentCollection<?> collection =
				loadingCollectionEntry == null ? null
						: loadingCollectionEntry.getCollectionInstance();
		if ( collection == null ) {
			// check if it is already completely loaded, but unowned
			collection = persistenceContext.useUnownedCollection( collectionKey );
			if ( collection == null ) {
				collection = persistenceContext.getCollection( collectionKey );
				if ( collection == null ) {
					// create a new collection wrapper, to be initialized later
					// we have already set the owner so we can just return the value
					return createNewWrapper( key, owner, overridingEager, persister, session ).getValue();
				}
			}
		}
		collection.setOwner( owner );
		return collection.getValue();
	}

	private PersistentCollection<?> createNewWrapper(
			Object key,
			Object owner,
			Boolean overridingEager,
			CollectionPersister persister,
			SharedSessionContractImplementor session) {
		final var persistenceContext = session.getPersistenceContextInternal();
		final var collection = instantiate( session, persister, key );
		collection.setOwner( owner );
		persistenceContext.addUninitializedCollection( persister, collection, key );
		// some collections are not lazy:
		if ( initializeImmediately() ) {
			session.initializeCollection( collection, false );
		}
		else if ( overridingEager != null ? overridingEager : !persister.isLazy() ) {
			persistenceContext.addNonLazyCollection( collection );
		}
		if ( hasHolder() ) {
			persistenceContext.addCollectionHolder( collection );
		}
		if ( CORE_LOGGER.isTraceEnabled() ) {
			CORE_LOGGER.trace( "Created collection wrapper: "
							+ collectionInfoString( persister, collection, key, session ) );
		}
		return collection;
	}

	public boolean hasHolder() {
		return false;
	}

	protected boolean initializeImmediately() {
		return false;
	}

	@Override
	public String getLHSPropertyName() {
		return foreignKeyPropertyName;
	}

	/**
	 * We always need to dirty check the collection because we sometimes
	 * need to increment version number of owner and also because of
	 * how assemble/disassemble is implemented for uks
	 */
	@Override
	public boolean isAlwaysDirtyChecked() {
		return true;
	}

	@Override
	public boolean[] toColumnNullness(Object value, MappingContext mapping) {
		return EMPTY_BOOLEAN_ARRAY;
	}
}
