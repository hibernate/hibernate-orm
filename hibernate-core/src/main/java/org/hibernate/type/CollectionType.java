/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
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
import org.hibernate.engine.spi.CollectionEntry;
import org.hibernate.engine.spi.CollectionKey;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.MarkerObject;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.CollectionClassification;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.collection.QueryableCollection;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.Joinable;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.proxy.LazyInitializer;
import org.hibernate.sql.results.graph.collection.LoadingCollectionEntry;

import org.jboss.logging.Logger;

import org.checkerframework.checker.nullness.qual.Nullable;

import static org.hibernate.bytecode.enhance.spi.LazyPropertyInitializer.UNFETCHED_PROPERTY;
import static org.hibernate.proxy.HibernateProxy.extractLazyInitializer;

/**
 * A type that handles Hibernate {@code PersistentCollection}s (including arrays).
 *
 * @author Gavin King
 */
public abstract class CollectionType extends AbstractType implements AssociationType {

	private static final CoreMessageLogger LOG = Logger.getMessageLogger(CoreMessageLogger.class, CollectionType.class.getName());

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
		final Iterator<?> elems = getElementsIterator( collection );
		while ( elems.hasNext() ) {
			Object element = elems.next();
			// worrying about proxies is perhaps a little bit of overkill here...
			final LazyInitializer li = extractLazyInitializer( element );
			if ( li != null ) {
				if ( !li.isUninitialized() ) {
					element = li.getImplementation();
				}
			}
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
			|| x instanceof PersistentCollection && isEqual( (PersistentCollection<?>) x, y )
			|| y instanceof PersistentCollection && isEqual( (PersistentCollection<?>) y, x );
	}

	private boolean isEqual(PersistentCollection<?> x, Object y) {
		return x.wasInitialized()
			&& ( x.isWrapper( y ) || x.isDirectlyProvidedCollection( y ) );
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
	public int[] getSqlTypeCodes(Mapping session) throws MappingException {
		return ArrayHelper.EMPTY_INT_ARRAY;
	}

	@Override
	public int getColumnSpan(Mapping session) throws MappingException {
		return 0;
	}

	@Override
	public String toLoggableString(Object value, SessionFactoryImplementor factory) {
		if ( value == null ) {
			return "null";
		}

		if ( !getReturnedClass().isInstance( value ) && !(value instanceof PersistentCollection) ) {
			// its most likely the collection-key
			final CollectionPersister persister = getPersister( factory );
			final Type keyType = persister.getKeyType();
			if ( keyType.getReturnedClass().isInstance( value ) ) {
				return getRole() + "#" + keyType.toLoggableString( value, factory );
			}
			else {
				// although it could also be the collection-id
				final Type identifierType = persister.getIdentifierType();
				if ( identifierType != null
						&& identifierType.getReturnedClass().isInstance( value ) ) {
					return getRole() + "#" + identifierType.toLoggableString( value, factory );
				}
			}
		}
		return renderLoggableString( value, factory );
	}

	protected String renderLoggableString(Object value, SessionFactoryImplementor factory) {
		if ( !Hibernate.isInitialized( value ) ) {
			return "<uninitialized>";
		}

		final List<String> list = new ArrayList<>();
		final Type elemType = getElementType( factory );
		final Iterator<?> itr = getElementsIterator( value );
		while ( itr.hasNext() ) {
			final Object element = itr.next();
			final String string =
					element == UNFETCHED_PROPERTY || !Hibernate.isInitialized(element)
							? "<uninitialized>"
							: elemType.toLoggableString( element, factory );
			list.add( string );
		}
		return list.toString();
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
		CollectionPersister p = this.persister;
		return p != null ? p : getPersister( session.getFactory() );
	}

	private CollectionPersister getPersister(SessionFactoryImplementor factory) {
		CollectionPersister p = this.persister;
		if ( p != null ) {
			return p;
		}
		else {
			synchronized ( this ) {
				p  = this.persister;
				if ( p != null ) {
					return p;
				}
				else {
					p = factory.getRuntimeMetamodels().getMappingMetamodel().getCollectionDescriptor( role );
					this.persister = p;
					return p;
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
		final EntityEntry entityEntry = session.getPersistenceContextInternal().getEntry( owner );
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
			final CollectionPersister persister = getPersister( session );
			final EntityPersister ownerPersister = persister.getOwnerEntityPersister();
			// TODO: Fix this so it will work for non-POJO entity mode
			final Class<?> keyClass = keyClass( session );
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
		return (Joinable) factory.getRuntimeMetamodels().getMappingMetamodel().getCollectionDescriptor( role );
	}

	@Override
	public boolean isModified(Object old, Object current, boolean[] checkable, SharedSessionContractImplementor session) {
		return false;
	}

	@Override
	public String getAssociatedEntityName(SessionFactoryImplementor factory)
			throws MappingException {
		try {

			QueryableCollection collectionPersister = (QueryableCollection) factory.getRuntimeMetamodels().getMappingMetamodel().getCollectionDescriptor( role );

			if ( !( collectionPersister.getElementType() instanceof EntityType ) ) {
				throw new MappingException(
						"collection was not an association: " +
						collectionPersister.getRole()
				);
			}

			return collectionPersister.getElementPersister().getEntityName();

		}
		catch (ClassCastException cce) {
			throw new MappingException( "collection role is not queryable " + role );
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
		Collection result = (Collection) target;
		result.clear();

		// copy elements into newly empty target collection
		Type elemType = getElementType( session.getFactory() );
		for ( Object o : (Collection<?>) original ) {
			result.add( elemType.replace(o, null, session, owner, copyCache) );
		}

		// if the original is a PersistentCollection, and that original
		// was not flagged as dirty, then reset the target's dirty flag
		// here after the copy operation.
		// </p>
		// One thing to be careful of here is a "bare" original collection
		// in which case we should never ever ever reset the dirty flag
		// on the target because we simply do not know...
		if ( original instanceof PersistentCollection && result instanceof PersistentCollection ) {
			final PersistentCollection<?> originalPersistentCollection = (PersistentCollection<?>) original;
			final PersistentCollection<?> resultPersistentCollection = (PersistentCollection<?>) result;

			preserveSnapshot( originalPersistentCollection, resultPersistentCollection, elemType, owner, copyCache, session );

			if ( ! originalPersistentCollection.isDirty() ) {
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
		Serializable originalSnapshot = original.getStoredSnapshot();
		Serializable resultSnapshot = result.getStoredSnapshot();
		Serializable targetSnapshot;

		if ( originalSnapshot instanceof List ) {
			ArrayList<Object> targetList = new ArrayList<>( ( (List<?>) originalSnapshot ).size() );
			targetSnapshot = targetList;
			for ( Object obj : (List<?>) originalSnapshot ) {
				targetList.add( elemType.replace( obj, null, session, owner, copyCache ) );
			}

		}
		else if ( originalSnapshot instanceof Map ) {
			Map<Object,Object> targetMap;
			if ( originalSnapshot instanceof SortedMap ) {
				@SuppressWarnings({"unchecked", "rawtypes"})
				Comparator<Object> comparator = ((SortedMap) originalSnapshot).comparator();
				targetMap = new TreeMap<>( comparator );
			}
			else {
				targetMap = new HashMap<>(
						CollectionHelper.determineProperSizing( ( (Map<?,?>) originalSnapshot ).size() ),
						CollectionHelper.LOAD_FACTOR
				);
			}
			targetSnapshot = (Serializable) targetMap;

			for ( Map.Entry<?,?> entry : ( (Map<?, ?>) originalSnapshot ).entrySet() ) {
				Object key = entry.getKey();
				Object value = entry.getValue();
				Object resultSnapshotValue = ( resultSnapshot == null )
						? null
						: ( (Map<?,?>) resultSnapshot ).get( key );

				Object newValue = elemType.replace( value, resultSnapshotValue, session, owner, copyCache );

				if ( key == value ) {
					targetMap.put( newValue, newValue );

				}
				else {
					targetMap.put( key, newValue );
				}

			}

		}
		else if ( originalSnapshot instanceof Object[] ) {
			Object[] arr = (Object[]) originalSnapshot;
			for ( int i = 0; i < arr.length; i++ ) {
				arr[i] = elemType.replace( arr[i], null, session, owner, copyCache );
			}
			targetSnapshot = originalSnapshot;

		}
		else {
			// retain the same snapshot
			targetSnapshot = resultSnapshot;

		}

		CollectionEntry ce = session.getPersistenceContextInternal().getCollectionEntry( result );
		if ( ce != null ) {
			ce.resetStoredSnapshot( result, targetSnapshot );
		}

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
			if ( target == null ) {
				return null;
			}
			if ( target instanceof Collection<?> ) {
				( (Collection<?>) target ).clear();
				return target;
			}
			else if ( target instanceof Map<?, ?> ) {
				( (Map<?, ?>) target ).clear();
				return target;
			}
			else {
				final PersistenceContext persistenceContext = session.getPersistenceContext();
				final PersistentCollection<?> collectionHolder = persistenceContext
						.getCollectionHolder( target );
				if ( collectionHolder != null ) {
					if ( collectionHolder instanceof PersistentArrayHolder<?> ) {
						PersistentArrayHolder<?> persistentArrayHolder = (PersistentArrayHolder<?>) collectionHolder;
						persistenceContext.removeCollectionHolder( target );
						persistentArrayHolder.beginRead();
						persistentArrayHolder.injectLoadedState(
								persistenceContext.getCollectionEntry( collectionHolder )
										.getLoadedPersister()
										.getAttributeMapping(), null
						);
						persistentArrayHolder.endRead();
						persistentArrayHolder.dirty();
						persistenceContext.addCollectionHolder( collectionHolder );
						return persistentArrayHolder.getArray();
					}
				}
			}

			return null;
		}
		if ( !Hibernate.isInitialized( original ) ) {
			if ( ( (PersistentCollection<?>) original ).hasQueuedOperations() ) {
				if ( original == target ) {
					// A managed entity with an uninitialized collection is being merged,
					// We need to replace any detached entities in the queued operations
					// with managed copies.
					final AbstractPersistentCollection<?> pc = (AbstractPersistentCollection<?>) original;
					pc.replaceQueuedOperationValues( getPersister( session ), copyCache );
				}
				else {
					// original is a detached copy of the collection;
					// it contains queued operations, which will be ignored
					LOG.ignoreQueuedOperationsOnMerge(
							MessageHelper.collectionInfoString(
									getRole(),
									( (PersistentCollection<?>) original ).getKey()
							)
					);
				}
			}
			return target;
		}

		// for a null target, or a target which is the same as the original, we
		// need to put the merged elements in a new collection
		Object result = ( target == null ||
				target == original ||
				target == UNFETCHED_PROPERTY ||
				target instanceof PersistentCollection
						&& ( (PersistentCollection<?>) target ).isWrapper( original ) )
								? instantiateResult( original ) : target;

		//for arrays, replaceElements() may return a different reference, since
		//the array length might not match
		result = replaceElements( original, result, owner, copyCache, session );

		if ( original == target ) {
			// get the elements back into the target making sure to handle dirty flag
			boolean wasClean = target instanceof PersistentCollection
					&& !( (PersistentCollection<?>) target ).isDirty();
			//TODO: this is a little inefficient, don't need to do a whole
			//      deep replaceElements() call
			replaceElements( result, target, owner, copyCache, session );
			if ( wasClean ) {
				( (PersistentCollection<?>) target ).clearDirty();
			}
			result = target;
		}

		return result;
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

		final CollectionPersister persister = getPersister( session );
		final PersistenceContext persistenceContext = session.getPersistenceContextInternal();

		final CollectionKey collectionKey = new CollectionKey( persister, key );
		PersistentCollection<?> collection = null;

		// check if collection is currently being loaded
		final LoadingCollectionEntry loadingCollectionEntry = persistenceContext.getLoadContexts().findLoadingCollectionEntry( collectionKey );
		if ( loadingCollectionEntry != null ) {
			collection = loadingCollectionEntry.getCollectionInstance();
		}

		if ( collection == null ) {

			// check if it is already completely loaded, but unowned
			collection = persistenceContext.useUnownedCollection( collectionKey );

			if ( collection == null ) {

				collection = persistenceContext.getCollection( collectionKey );

				if ( collection == null ) {
					// create a new collection wrapper, to be initialized later
					collection = instantiate( session, persister, key );

					collection.setOwner( owner );

					persistenceContext.addUninitializedCollection( persister, collection, key );

					// some collections are not lazy:
					boolean eager = overridingEager != null ? overridingEager : !persister.isLazy();
					if ( initializeImmediately() ) {
						session.initializeCollection( collection, false );
					}
					else if ( eager ) {
						persistenceContext.addNonLazyCollection( collection );
					}

					if ( hasHolder() ) {
						persistenceContext.addCollectionHolder( collection );
					}

					if ( LOG.isTraceEnabled() ) {
						LOG.tracef( "Created collection wrapper: %s",
									MessageHelper.collectionInfoString( persister, collection,
																		key, session ) );
					}
					// we have already set the owner so we can just return the value
					return collection.getValue();
				}
			}
		}

		collection.setOwner( owner );

		return collection.getValue();
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
	public boolean[] toColumnNullness(Object value, Mapping mapping) {
		return ArrayHelper.EMPTY_BOOLEAN_ARRAY;
	}
}
