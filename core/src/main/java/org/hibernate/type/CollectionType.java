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
 *
 */
package org.hibernate.type;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.dom4j.Element;
import org.dom4j.Node;

import org.hibernate.EntityMode;
import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.collection.PersistentCollection;
import org.hibernate.engine.CollectionKey;
import org.hibernate.engine.EntityEntry;
import org.hibernate.engine.Mapping;
import org.hibernate.engine.PersistenceContext;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.collection.QueryableCollection;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.Joinable;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;
import org.hibernate.util.ArrayHelper;
import org.hibernate.util.MarkerObject;

/**
 * A type that handles Hibernate <tt>PersistentCollection</tt>s (including arrays).
 * 
 * @author Gavin King
 */
public abstract class CollectionType extends AbstractType implements AssociationType {

	private static final Object NOT_NULL_COLLECTION = new MarkerObject( "NOT NULL COLLECTION" );
	public static final Object UNFETCHED_COLLECTION = new MarkerObject( "UNFETCHED COLLECTION" );

	private final String role;
	private final String foreignKeyPropertyName;
	private final boolean isEmbeddedInXML;

	public CollectionType(String role, String foreignKeyPropertyName, boolean isEmbeddedInXML) {
		this.role = role;
		this.foreignKeyPropertyName = foreignKeyPropertyName;
		this.isEmbeddedInXML = isEmbeddedInXML;
	}
	
	public boolean isEmbeddedInXML() {
		return isEmbeddedInXML;
	}

	public String getRole() {
		return role;
	}

	public Object indexOf(Object collection, Object element) {
		throw new UnsupportedOperationException( "generic collections don't have indexes" );
	}

	public boolean contains(Object collection, Object childObject, SessionImplementor session) {
		// we do not have to worry about queued additions to uninitialized
		// collections, since they can only occur for inverse collections!
		Iterator elems = getElementsIterator( collection, session );
		while ( elems.hasNext() ) {
			Object element = elems.next();
			// worrying about proxies is perhaps a little bit of overkill here...
			if ( element instanceof HibernateProxy ) {
				LazyInitializer li = ( (HibernateProxy) element ).getHibernateLazyInitializer();
				if ( !li.isUninitialized() ) element = li.getImplementation();
			}
			if ( element == childObject ) return true;
		}
		return false;
	}

	public boolean isCollectionType() {
		return true;
	}

	public final boolean isEqual(Object x, Object y, EntityMode entityMode) {
		return x == y
			|| ( x instanceof PersistentCollection && ( (PersistentCollection) x ).isWrapper( y ) )
			|| ( y instanceof PersistentCollection && ( (PersistentCollection) y ).isWrapper( x ) );
	}

	public int compare(Object x, Object y, EntityMode entityMode) {
		return 0; // collections cannot be compared
	}

	public int getHashCode(Object x, EntityMode entityMode) {
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
	public abstract PersistentCollection instantiate(SessionImplementor session, CollectionPersister persister, Serializable key);

	public Object nullSafeGet(ResultSet rs, String name, SessionImplementor session, Object owner) throws SQLException {
		return nullSafeGet( rs, new String[] { name }, session, owner );
	}

	public Object nullSafeGet(ResultSet rs, String[] name, SessionImplementor session, Object owner)
			throws HibernateException, SQLException {
		return resolve( null, session, owner );
	}

	public final void nullSafeSet(PreparedStatement st, Object value, int index, boolean[] settable,
			SessionImplementor session) throws HibernateException, SQLException {
		//NOOP
	}

	public void nullSafeSet(PreparedStatement st, Object value, int index,
			SessionImplementor session) throws HibernateException, SQLException {
	}

	public int[] sqlTypes(Mapping session) throws MappingException {
		return ArrayHelper.EMPTY_INT_ARRAY;
	}

	public int getColumnSpan(Mapping session) throws MappingException {
		return 0;
	}

	public String toLoggableString(Object value, SessionFactoryImplementor factory)
			throws HibernateException {
		if ( value == null ) {
			return "null";
		}
		else if ( !Hibernate.isInitialized( value ) ) {
			return "<uninitialized>";
		}
		else {
			return renderLoggableString( value, factory );
		}
	}

	protected String renderLoggableString(Object value, SessionFactoryImplementor factory)
			throws HibernateException {
		if ( Element.class.isInstance( value ) ) {
			// for DOM4J "collections" only
			// TODO: it would be better if this was done at the higher level by Printer
			return ( ( Element ) value ).asXML();
		}
		else {
			List list = new ArrayList();
			Type elemType = getElementType( factory );
			Iterator iter = getElementsIterator( value );
			while ( iter.hasNext() ) {
				list.add( elemType.toLoggableString( iter.next(), factory ) );
			}
			return list.toString();
		}
	}

	public Object deepCopy(Object value, EntityMode entityMode, SessionFactoryImplementor factory)
			throws HibernateException {
		return value;
	}

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
	public Iterator getElementsIterator(Object collection, SessionImplementor session) {
		if ( session.getEntityMode()==EntityMode.DOM4J ) {
			final SessionFactoryImplementor factory = session.getFactory();
			final CollectionPersister persister = factory.getCollectionPersister( getRole() );
			final Type elementType = persister.getElementType();
			
			List elements = ( (Element) collection ).elements( persister.getElementNodeName() );
			ArrayList results = new ArrayList();
			for ( int i=0; i<elements.size(); i++ ) {
				Element value = (Element) elements.get(i);
				results.add( elementType.fromXMLNode( value, factory ) );
			}
			return results.iterator();
		}
		else {
			return getElementsIterator(collection);
		}
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

	public boolean isMutable() {
		return false;
	}

	public Serializable disassemble(Object value, SessionImplementor session, Object owner)
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

	public Object assemble(Serializable cached, SessionImplementor session, Object owner)
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
			return resolveKey( key, session, owner );
		}
	}

	/**
	 * Is the owning entity versioned?
	 *
	 * @param session The session from which the request is originating.
	 * @return True if the collection owner is versioned; false otherwise.
	 * @throws org.hibernate.MappingException Indicates our persister could not be located.
	 */
	private boolean isOwnerVersioned(SessionImplementor session) throws MappingException {
		return getPersister( session ).getOwnerEntityPersister().isVersioned();
	}

	/**
	 * Get our underlying collection persister (using the session to access the
	 * factory).
	 *
	 * @param session The session from which the request is originating.
	 * @return The underlying collection persister
	 */
	private CollectionPersister getPersister(SessionImplementor session) {
		return session.getFactory().getCollectionPersister( role );
	}

	public boolean isDirty(Object old, Object current, SessionImplementor session)
			throws HibernateException {

		// collections don't dirty an unversioned parent entity

		// TODO: I don't really like this implementation; it would be better if
		// this was handled by searchForDirtyCollections()
		return isOwnerVersioned( session ) && super.isDirty( old, current, session );
		// return false;

	}

	public boolean isDirty(Object old, Object current, boolean[] checkable, SessionImplementor session)
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
	public abstract PersistentCollection wrap(SessionImplementor session, Object collection);

	/**
	 * Note: return true because this type is castable to <tt>AssociationType</tt>. Not because
	 * all collections are associations.
	 */
	public boolean isAssociationType() {
		return true;
	}

	public ForeignKeyDirection getForeignKeyDirection() {
		return ForeignKeyDirection.FOREIGN_KEY_TO_PARENT;
	}

	/**
	 * Get the key value from the owning entity instance, usually the identifier, but might be some
	 * other unique key, in the case of property-ref
	 *
	 * @param owner The collection owner
	 * @param session The session from which the request is originating.
	 * @return The collection owner's key
	 */
	public Serializable getKeyOfOwner(Object owner, SessionImplementor session) {
		
		EntityEntry entityEntry = session.getPersistenceContext().getEntry( owner );
		if ( entityEntry == null ) return null; // This just handles a particular case of component
									  // projection, perhaps get rid of it and throw an exception
		
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
				id = entityEntry.getPersister().getPropertyValue( owner, foreignKeyPropertyName, session.getEntityMode() );
			}

			// NOTE VERY HACKISH WORKAROUND!!
			// TODO: Fix this so it will work for non-POJO entity mode
			Type keyType = getPersister( session ).getKeyType();
			if ( !keyType.getReturnedClass().isInstance( id ) ) {
				id = (Serializable) keyType.semiResolve(
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
	public Serializable getIdOfOwnerOrNull(Serializable key, SessionImplementor session) {
		Serializable ownerId = null;
		if ( foreignKeyPropertyName == null ) {
			ownerId = key;
		}
		else {
			Type keyType = getPersister( session ).getKeyType();
			EntityPersister ownerPersister = getPersister( session ).getOwnerEntityPersister();
			// TODO: Fix this so it will work for non-POJO entity mode
			Class ownerMappedClass = ownerPersister.getMappedClass( session.getEntityMode() );
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

	public Object hydrate(ResultSet rs, String[] name, SessionImplementor session, Object owner) {
		// can't just return null here, since that would
		// cause an owning component to become null
		return NOT_NULL_COLLECTION;
	}

	public Object resolve(Object value, SessionImplementor session, Object owner)
			throws HibernateException {
		
		return resolveKey( getKeyOfOwner( owner, session ), session, owner );
	}
	
	private Object resolveKey(Serializable key, SessionImplementor session, Object owner) {
		// if (key==null) throw new AssertionFailure("owner identifier unknown when re-assembling
		// collection reference");
		return key == null ? null : // TODO: can this case really occur??
			getCollection( key, session, owner );
	}

	public Object semiResolve(Object value, SessionImplementor session, Object owner)
			throws HibernateException {
		throw new UnsupportedOperationException(
			"collection mappings may not form part of a property-ref" );
	}

	public boolean isArrayType() {
		return false;
	}

	public boolean useLHSPrimaryKey() {
		return foreignKeyPropertyName == null;
	}

	public String getRHSUniqueKeyPropertyName() {
		return null;
	}

	public Joinable getAssociatedJoinable(SessionFactoryImplementor factory)
			throws MappingException {
		return (Joinable) factory.getCollectionPersister( role );
	}

	public boolean isModified(Object old, Object current, boolean[] checkable, SessionImplementor session) throws HibernateException {
		return false;
	}

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
			SessionImplementor session) {
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
				if ( ! ( ( PersistentCollection ) original ).isDirty() ) {
					( ( PersistentCollection ) result ).clearDirty();
				}
			}
		}

		return result;
	}

	/**
	 * Instantiate a new "underlying" collection exhibiting the same capacity
	 * charactersitcs and the passed "original".
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
	 * @param anticipatedSize The anticipated size of the instaniated collection
	 * after we are done populating it.
	 * @return A newly instantiated collection to be wrapped.
	 */
	public abstract Object instantiate(int anticipatedSize);

	/**
	 * {@inheritDoc}
	 */
	public Object replace(
			final Object original,
			final Object target,
			final SessionImplementor session,
			final Object owner,
			final Map copyCache) throws HibernateException {
		if ( original == null ) {
			return null;
		}
		if ( !Hibernate.isInitialized( original ) ) {
			return target;
		}

		// for a null target, or a target which is the same as the original, we
		// need to put the merged elements in a new collection
		Object result = target == null || target == original ? instantiateResult( original ) : target;
		
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

	public String toString() {
		return getClass().getName() + '(' + getRole() + ')';
	}

	public String getOnCondition(String alias, SessionFactoryImplementor factory, Map enabledFilters)
			throws MappingException {
		return getAssociatedJoinable( factory ).filterFragment( alias, enabledFilters );
	}

	/**
	 * instantiate a collection wrapper (called when loading an object)
	 *
	 * @param key The collection owner key
	 * @param session The session from which the request is originating.
	 * @param owner The collection owner
	 * @return The collection
	 */
	public Object getCollection(Serializable key, SessionImplementor session, Object owner) {

		CollectionPersister persister = getPersister( session );
		final PersistenceContext persistenceContext = session.getPersistenceContext();
		final EntityMode entityMode = session.getEntityMode();

		if (entityMode==EntityMode.DOM4J && !isEmbeddedInXML) {
			return UNFETCHED_COLLECTION;
		}
		
		// check if collection is currently being loaded
		PersistentCollection collection = persistenceContext.getLoadContexts().locateLoadingCollection( persister, key );
		
		if ( collection == null ) {
			
			// check if it is already completely loaded, but unowned
			collection = persistenceContext.useUnownedCollection( new CollectionKey(persister, key, entityMode) );
			
			if ( collection == null ) {
				// create a new collection wrapper, to be initialized later
				collection = instantiate( session, persister, key );
				collection.setOwner(owner);
	
				persistenceContext.addUninitializedCollection( persister, collection, key );
	
				// some collections are not lazy:
				if ( initializeImmediately( entityMode ) ) {
					session.initializeCollection( collection, false );
				}
				else if ( !persister.isLazy() ) {
					persistenceContext.addNonLazyCollection( collection );
				}
	
				if ( hasHolder( entityMode ) ) {
					session.getPersistenceContext().addCollectionHolder( collection );
				}
				
			}
			
		}
		
		collection.setOwner(owner);

		return collection.getValue();
	}

	public boolean hasHolder(EntityMode entityMode) {
		return entityMode == EntityMode.DOM4J;
	}

	protected boolean initializeImmediately(EntityMode entityMode) {
		return entityMode == EntityMode.DOM4J;
	}

	public String getLHSPropertyName() {
		return foreignKeyPropertyName;
	}

	public boolean isXMLElement() {
		return true;
	}

	public Object fromXMLNode(Node xml, Mapping factory) throws HibernateException {
		return xml;
	}

	public void setToXMLNode(Node node, Object value, SessionFactoryImplementor factory) 
	throws HibernateException {
		if ( !isEmbeddedInXML ) {
			node.detach();
		}
		else {
			replaceNode( node, (Element) value );
		}
	}
	
	/**
	 * We always need to dirty check the collection because we sometimes 
	 * need to incremement version number of owner and also because of 
	 * how assemble/disassemble is implemented for uks
	 */
	public boolean isAlwaysDirtyChecked() {
		return true; 
	}

	public boolean[] toColumnNullness(Object value, Mapping mapping) {
		return ArrayHelper.EMPTY_BOOLEAN_ARRAY;
	}
}
