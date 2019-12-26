/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.bytecode.enhance.spi.LazyPropertyInitializer;
import org.hibernate.collection.internal.AbstractPersistentCollection;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.jdbc.Size;
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
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.collection.QueryableCollection;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.Joinable;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;

import org.jboss.logging.Logger;

/**
 * A type that handles Hibernate <tt>PersistentCollection</tt>s (including arrays).
 *
 * @author Gavin King
 */
public abstract class CollectionType extends AbstractType implements AssociationType {

	private static final CoreMessageLogger LOG = Logger.getMessageLogger(CoreMessageLogger.class, CollectionType.class.getName());

	private static final Object NOT_NULL_COLLECTION = new MarkerObject( "NOT NULL COLLECTION" );
	public static final Object UNFETCHED_COLLECTION = new MarkerObject( "UNFETCHED COLLECTION" );

	private final String role;
	private final String foreignKeyPropertyName;

	// the need for the persister if very hot in many use cases: cache it in a field
	// TODO initialize it at constructor time
	private volatile CollectionPersister persister;

	/**
	 * @deprecated Use the other constructor
	 */
	@Deprecated
	public CollectionType(TypeFactory.TypeScope typeScope, String role, String foreignKeyPropertyName) {
		this( role, foreignKeyPropertyName );
	}

	public CollectionType(String role, String foreignKeyPropertyName) {
		this.role = role;
		this.foreignKeyPropertyName = foreignKeyPropertyName;
	}

	public String getRole() {
		return role;
	}

	public Object indexOf(Object collection, Object element) {
		throw new UnsupportedOperationException( "generic collections don't have indexes" );
	}

	public boolean contains(Object collection, Object childObject, SharedSessionContractImplementor session) {
		// we do not have to worry about queued additions to uninitialized
		// collections, since they can only occur for inverse collections!
		Iterator elems = getElementsIterator( collection, session );
		while ( elems.hasNext() ) {
			Object element = elems.next();
			// worrying about proxies is perhaps a little bit of overkill here...
			if ( element instanceof HibernateProxy ) {
				LazyInitializer li = ( (HibernateProxy) element ).getHibernateLazyInitializer();
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
			|| ( x instanceof PersistentCollection && isEqual( (PersistentCollection) x, y ) )
			|| ( y instanceof PersistentCollection && isEqual( (PersistentCollection) y, x ) );
	}

	private boolean isEqual(PersistentCollection x, Object y) {
		return x.wasInitialized() && ( x.isWrapper( y ) || x.isDirectlyProvidedCollection( y ) );
	}

	@Override
	public int compare(Object x, Object y) {
		return 0; // collections cannot be compared
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
	public abstract PersistentCollection instantiate(SharedSessionContractImplementor session, CollectionPersister persister, Serializable key);

	@Override
	public Object nullSafeGet(ResultSet rs, String name, SharedSessionContractImplementor session, Object owner) throws SQLException {
		return nullSafeGet( rs, new String[] { name }, session, owner );
	}

	@Override
	public Object nullSafeGet(ResultSet rs, String[] name, SharedSessionContractImplementor session, Object owner)
			throws HibernateException, SQLException {
		return resolve( null, session, owner );
	}

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
	public int[] sqlTypes(Mapping session) throws MappingException {
		return ArrayHelper.EMPTY_INT_ARRAY;
	}

	@Override
	public Size[] dictatedSizes(Mapping mapping) throws MappingException {
		return new Size[] { LEGACY_DICTATED_SIZE };
	}

	@Override
	public Size[] defaultSizes(Mapping mapping) throws MappingException {
		return new Size[] { LEGACY_DEFAULT_SIZE };
	}

	@Override
	public int getColumnSpan(Mapping session) throws MappingException {
		return 0;
	}

	@Override
	public String toLoggableString(Object value, SessionFactoryImplementor factory)
			throws HibernateException {
		if ( value == null ) {
			return "null";
		}

		if ( !getReturnedClass().isInstance( value ) && !PersistentCollection.class.isInstance( value ) ) {
			// its most likely the collection-key
			final CollectionPersister persister = getPersister( factory );
			if ( persister.getKeyType().getReturnedClass().isInstance( value ) ) {
				return getRole() + "#" + getPersister( factory ).getKeyType().toLoggableString( value, factory );
			}
			else {
				// although it could also be the collection-id
				if ( persister.getIdentifierType() != null
						&& persister.getIdentifierType().getReturnedClass().isInstance( value ) ) {
					return getRole() + "#" + getPersister( factory ).getIdentifierType().toLoggableString( value, factory );
				}
			}
		}
		return renderLoggableString( value, factory );
	}

	protected String renderLoggableString(Object value, SessionFactoryImplementor factory) throws HibernateException {
		if ( !Hibernate.isInitialized( value ) ) {
			return "<uninitialized>";
		}

		final List<String> list = new ArrayList<>();
		Type elemType = getElementType( factory );
		Iterator itr = getElementsIterator( value );
		while ( itr.hasNext() ) {
			Object element = itr.next();
			if ( element == LazyPropertyInitializer.UNFETCHED_PROPERTY || !Hibernate.isInitialized( element ) ) {
				list.add( "<uninitialized>" );
			}
			else {
				list.add( elemType.toLoggableString( element, factory ) );
			}
		}
		return list.toString();
	}

	@Override
	public Object deepCopy(Object value, SessionFactoryImplementor factory)
			throws HibernateException {
		return value;
	}

	@Override
	public String getName() {
		return getReturnedClass().getName() + '(' + getRole() + ')';
	}

	/**
	 * Get an iterator over the element set of the collection, which may not yet be wrapped
	 *
	 * @param collection The collection to be iterated
	 * @param session The session from which the request is originating.
	 * @return The iterator.
	 */
	public Iterator getElementsIterator(Object collection, SharedSessionContractImplementor session) {
		return getElementsIterator( collection );
	}

	/**
	 * Get an iterator over the element set of the collection in POJO mode
	 *
	 * @param collection The collection to be iterated
	 * @return The iterator.
	 */
	protected Iterator getElementsIterator(Object collection) {
		return ( (Collection) collection ).iterator();
	}

	@Override
	public boolean isMutable() {
		return false;
	}

	@Override
	public Serializable disassemble(Object value, SharedSessionContractImplementor session, Object owner)
			throws HibernateException {
		//remember the uk value

		//This solution would allow us to eliminate the owner arg to disassemble(), but
		//what if the collection was null, and then later had elements added? seems unsafe
		//session.getPersistenceContext().getCollectionEntry( (PersistentCollection) value ).getKey();

		final Serializable key = getKeyOfOwner(owner, session);
		if (key==null) {
			return null;
		}
		else {
			return getPersister(session)
					.getKeyType()
					.disassemble( key, session, owner );
		}
	}

	@Override
	public Object assemble(Serializable cached, SharedSessionContractImplementor session, Object owner)
			throws HibernateException {
		//we must use the "remembered" uk value, since it is
		//not available from the EntityEntry during assembly
		if (cached==null) {
			return null;
		}
		else {
			final Serializable key = (Serializable) getPersister(session)
					.getKeyType()
					.assemble( cached, session, owner);
			return resolveKey( key, session, owner, null );
		}
	}

	/**
	 * Is the owning entity versioned?
	 *
	 * @param session The session from which the request is originating.
	 * @return True if the collection owner is versioned; false otherwise.
	 * @throws org.hibernate.MappingException Indicates our persister could not be located.
	 */
	private boolean isOwnerVersioned(SharedSessionContractImplementor session) throws MappingException {
		return getPersister( session ).getOwnerEntityPersister().isVersioned();
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
		if ( p != null ) {
			return p;
		}
		else {
			return getPersister( session.getFactory() );
		}
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
					p = factory.getMetamodel().collectionPersister( role );
					this.persister = p;
					return p;
				}
			}
		}
	}

	@Override
	public boolean isDirty(Object old, Object current, SharedSessionContractImplementor session)
			throws HibernateException {

		// collections don't dirty an unversioned parent entity

		// TODO: I don't really like this implementation; it would be better if
		// this was handled by searchForDirtyCollections()
		return super.isDirty( old, current, session );
		// return false;

	}

	@Override
	public boolean isDirty(Object old, Object current, boolean[] checkable, SharedSessionContractImplementor session)
			throws HibernateException {
		return isDirty(old, current, session);
	}

	/**
	 * Wrap the naked collection instance in a wrapper, or instantiate a
	 * holder. Callers <b>MUST</b> add the holder to the persistence context!
	 *
	 * @param session The session from which the request is originating.
	 * @param collection The bare collection to be wrapped.
	 * @return The wrapped collection.
	 */
	public abstract PersistentCollection wrap(SharedSessionContractImplementor session, Object collection);

	/**
	 * Note: return true because this type is castable to <tt>AssociationType</tt>. Not because
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
	public Serializable getKeyOfOwner(Object owner, SharedSessionContractImplementor session) {
		final PersistenceContext pc = session.getPersistenceContextInternal();

		EntityEntry entityEntry = pc.getEntry( owner );
		if ( entityEntry == null ) {
			// This just handles a particular case of component
			// projection, perhaps get rid of it and throw an exception
			return null;
		}

		if ( foreignKeyPropertyName == null ) {
			return entityEntry.getId();
		}
		else {
			// TODO: at the point where we are resolving collection references, we don't
			// know if the uk value has been resolved (depends if it was earlier or
			// later in the mapping document) - now, we could try and use e.getStatus()
			// to decide to semiResolve(), trouble is that initializeEntity() reuses
			// the same array for resolved and hydrated values
			Object id;
			if ( entityEntry.getLoadedState() != null ) {
				id = entityEntry.getLoadedValue( foreignKeyPropertyName );
			}
			else {
				id = entityEntry.getPersister().getPropertyValue( owner, foreignKeyPropertyName );
			}

			// NOTE VERY HACKISH WORKAROUND!!
			// TODO: Fix this so it will work for non-POJO entity mode
			Type keyType = getPersister( session ).getKeyType();
			Class returnedClass = keyType.getReturnedClass();

			if ( !returnedClass.isInstance( id ) ) {
				id = keyType.semiResolve(
						entityEntry.getLoadedValue( foreignKeyPropertyName ),
						session,
						owner
				);
			}

			return (Serializable) id;
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
	public Serializable getIdOfOwnerOrNull(Serializable key, SharedSessionContractImplementor session) {
		Serializable ownerId = null;
		if ( foreignKeyPropertyName == null ) {
			ownerId = key;
		}
		else {
			final CollectionPersister persister = getPersister( session );
			Type keyType = persister.getKeyType();
			EntityPersister ownerPersister = persister.getOwnerEntityPersister();
			// TODO: Fix this so it will work for non-POJO entity mode
			Class ownerMappedClass = ownerPersister.getMappedClass();
			if ( ownerMappedClass.isAssignableFrom( keyType.getReturnedClass() ) &&
					keyType.getReturnedClass().isInstance( key ) ) {
				// the key is the owning entity itself, so get the ID from the key
				ownerId = ownerPersister.getIdentifier( key, session );
			}
			else {
				// TODO: check if key contains the owner ID
			}
		}
		return ownerId;
	}

	@Override
	public Object hydrate(ResultSet rs, String[] name, SharedSessionContractImplementor session, Object owner) {
		// can't just return null here, since that would
		// cause an owning component to become null
		return NOT_NULL_COLLECTION;
	}

	@Override
	public Object resolve(Object value, SharedSessionContractImplementor session, Object owner)
			throws HibernateException {

		return resolve( value, session, owner, null );
	}

	@Override
	public Object resolve(Object value, SharedSessionContractImplementor session, Object owner, Boolean overridingEager) throws HibernateException {
		return resolveKey( getKeyOfOwner( owner, session ), session, owner, overridingEager );
	}

	private Object resolveKey(Serializable key, SharedSessionContractImplementor session, Object owner, Boolean overridingEager) {
		// if (key==null) throw new AssertionFailure("owner identifier unknown when re-assembling
		// collection reference");
		return key == null ? null : // TODO: can this case really occur??
			getCollection( key, session, owner, overridingEager );
	}

	@Override
	public Object semiResolve(Object value, SharedSessionContractImplementor session, Object owner)
			throws HibernateException {
		throw new UnsupportedOperationException(
			"collection mappings may not form part of a property-ref" );
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
		return (Joinable) factory.getCollectionPersister( role );
	}

	@Override
	public boolean isModified(Object old, Object current, boolean[] checkable, SharedSessionContractImplementor session) throws HibernateException {
		return false;
	}

	@Override
	public String getAssociatedEntityName(SessionFactoryImplementor factory)
			throws MappingException {
		try {

			QueryableCollection collectionPersister = (QueryableCollection) factory
					.getCollectionPersister( role );

			if ( !collectionPersister.getElementType().isEntityType() ) {
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
	public Object replaceElements(
			Object original,
			Object target,
			Object owner,
			Map copyCache,
			SharedSessionContractImplementor session) {
		// TODO: does not work for EntityMode.DOM4J yet!
		java.util.Collection result = ( java.util.Collection ) target;
		result.clear();

		// copy elements into newly empty target collection
		Type elemType = getElementType( session.getFactory() );
		Iterator iter = ( (java.util.Collection) original ).iterator();
		while ( iter.hasNext() ) {
			result.add( elemType.replace( iter.next(), null, session, owner, copyCache ) );
		}

		// if the original is a PersistentCollection, and that original
		// was not flagged as dirty, then reset the target's dirty flag
		// here after the copy operation.
		// </p>
		// One thing to be careful of here is a "bare" original collection
		// in which case we should never ever ever reset the dirty flag
		// on the target because we simply do not know...
		if ( original instanceof PersistentCollection ) {
			if ( result instanceof PersistentCollection ) {
				final PersistentCollection originalPersistentCollection = (PersistentCollection) original;
				final PersistentCollection resultPersistentCollection = (PersistentCollection) result;

				preserveSnapshot( originalPersistentCollection, resultPersistentCollection, elemType, owner, copyCache, session );

				if ( ! originalPersistentCollection.isDirty() ) {
					resultPersistentCollection.clearDirty();
				}
			}
		}

		return result;
	}

	private void preserveSnapshot(
			PersistentCollection original,
			PersistentCollection result,
			Type elemType,
			Object owner,
			Map copyCache,
			SharedSessionContractImplementor session) {
		Serializable originalSnapshot = original.getStoredSnapshot();
		Serializable resultSnapshot = result.getStoredSnapshot();
		Serializable targetSnapshot;

		if ( originalSnapshot instanceof List ) {
			targetSnapshot = new ArrayList(
					( (List) originalSnapshot ).size() );
			for ( Object obj : (List) originalSnapshot ) {
				( (List) targetSnapshot ).add( elemType.replace( obj, null, session, owner, copyCache ) );
			}

		}
		else if ( originalSnapshot instanceof Map ) {
			if ( originalSnapshot instanceof SortedMap ) {
				targetSnapshot = new TreeMap( ( (SortedMap) originalSnapshot ).comparator() );
			}
			else {
				targetSnapshot = new HashMap(
						CollectionHelper.determineProperSizing( ( (Map) originalSnapshot ).size() ),
						CollectionHelper.LOAD_FACTOR
				);
			}

			for ( Map.Entry<Object, Object> entry : ( (Map<Object, Object>) originalSnapshot ).entrySet() ) {
				Object key = entry.getKey();
				Object value = entry.getValue();
				Object resultSnapshotValue = ( resultSnapshot == null )
						? null
						: ( (Map<Object, Object>) resultSnapshot ).get( key );

				Object newValue = elemType.replace( value, resultSnapshotValue, session, owner, copyCache );

				if ( key == value ) {
					( (Map) targetSnapshot ).put( newValue, newValue );

				}
				else {
					( (Map) targetSnapshot ).put( key, newValue );
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
			final Map copyCache) throws HibernateException {
		if ( original == null ) {
			return null;
		}
		if ( !Hibernate.isInitialized( original ) ) {
			if ( ( (PersistentCollection) original ).hasQueuedOperations() ) {
				if ( original == target ) {
					// A managed entity with an uninitialized collection is being merged,
					// We need to replace any detached entities in the queued operations
					// with managed copies.
					final AbstractPersistentCollection pc = (AbstractPersistentCollection) original;
					pc.replaceQueuedOperationValues( getPersister( session ), copyCache );
				}
				else {
					// original is a detached copy of the collection;
					// it contains queued operations, which will be ignored
					LOG.ignoreQueuedOperationsOnMerge(
							MessageHelper.collectionInfoString(
									getRole(),
									( (PersistentCollection) original ).getKey()
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
				target == LazyPropertyInitializer.UNFETCHED_PROPERTY ) ?
				instantiateResult( original ) : target;

		//for arrays, replaceElements() may return a different reference, since
		//the array length might not match
		result = replaceElements( original, result, owner, copyCache, session );

		if ( original == target ) {
			// get the elements back into the target making sure to handle dirty flag
			boolean wasClean = PersistentCollection.class.isInstance( target ) && !( ( PersistentCollection ) target ).isDirty();
			//TODO: this is a little inefficient, don't need to do a whole
			//      deep replaceElements() call
			replaceElements( result, target, owner, copyCache, session );
			if ( wasClean ) {
				( ( PersistentCollection ) target ).clearDirty();
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
		return factory.getCollectionPersister( getRole() ).getElementType();
	}

	@Override
	public String toString() {
		return getClass().getName() + '(' + getRole() + ')';
	}

	@Override
	public String getOnCondition(String alias, SessionFactoryImplementor factory, Map enabledFilters)
			throws MappingException {
		return getAssociatedJoinable( factory ).filterFragment( alias, enabledFilters );
	}

	@Override
	public String getOnCondition(
			String alias,
			SessionFactoryImplementor factory,
			Map enabledFilters,
			Set<String> treatAsDeclarations) {
		return getAssociatedJoinable( factory ).filterFragment( alias, enabledFilters, treatAsDeclarations );
	}

	/**
	 * instantiate a collection wrapper (called when loading an object)
	 *
	 * @param key The collection owner key
	 * @param session The session from which the request is originating.
	 * @param owner The collection owner
	 * @return The collection
	 */
	public Object getCollection(Serializable key, SharedSessionContractImplementor session, Object owner, Boolean overridingEager) {

		final CollectionPersister persister = getPersister( session );
		final PersistenceContext persistenceContext = session.getPersistenceContextInternal();

		final CollectionKey collectionKey = new CollectionKey( persister, key );
		// check if collection is currently being loaded
		PersistentCollection collection = persistenceContext.getLoadContexts()
				.locateLoadingCollection( persister, collectionKey );

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
