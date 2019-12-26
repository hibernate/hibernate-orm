/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.collection.internal;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.loader.CollectionAliases;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.type.Type;

/**
 * An unordered, unkeyed collection that can contain the same element
 * multiple times. The Java collections API, curiously, has no <tt>Bag</tt>.
 * Most developers seem to use <tt>List</tt>s to represent bag semantics,
 * so Hibernate follows this practice.
 *
 * @author Gavin King
 */
public class PersistentBag extends AbstractPersistentCollection implements List {

	protected List bag;

	// The Collection provided to a PersistentBag constructor,
	private Collection providedCollection;

	/**
	 * Constructs a PersistentBag.  Needed for SOAP libraries, etc
	 */
	@SuppressWarnings("UnusedDeclaration")
	public PersistentBag() {
	}

	/**
	 * Constructs a PersistentBag
	 *
	 * @param session The session
	 */
	public PersistentBag(SharedSessionContractImplementor session) {
		super( session );
	}

	/**
	 * Constructs a PersistentBag
	 *
	 * @param session The session
	 *
	 * @deprecated {@link #PersistentBag(SharedSessionContractImplementor)} should be used instead.
	 */
	@Deprecated
	public PersistentBag(SessionImplementor session) {
		this( (SharedSessionContractImplementor) session );
	}

	/**
	 * Constructs a PersistentBag
	 *
	 * @param session The session
	 * @param coll The base elements.
	 */
	@SuppressWarnings("unchecked")
	public PersistentBag(SharedSessionContractImplementor session, Collection coll) {
		super( session );
		providedCollection = coll;
		if ( coll instanceof List ) {
			bag = (List) coll;
		}
		else {
			bag = new ArrayList( coll );
		}
		setInitialized();
		setDirectlyAccessible( true );
	}

	/**
	 * Constructs a PersistentBag
	 *
	 * @param session The session
	 * @param coll The base elements.
	 *
	 * @deprecated {@link #PersistentBag(SharedSessionContractImplementor, Collection)}
	 * should be used instead.
	 */
	@Deprecated
	public PersistentBag(SessionImplementor session, Collection coll) {
		this( (SharedSessionContractImplementor) session, coll );
	}

	@Override
	public boolean isWrapper(Object collection) {
		return bag == collection;
	}

	@Override
	public boolean isDirectlyProvidedCollection(Object collection) {
		return isDirectlyAccessible() && providedCollection == collection;
	}

	@Override
	public boolean empty() {
		return bag.isEmpty();
	}

	@Override
	public Iterator entries(CollectionPersister persister) {
		return bag.iterator();
	}

	@Override
	@SuppressWarnings("unchecked")
	public Object readFrom(ResultSet rs, CollectionPersister persister, CollectionAliases descriptor, Object owner)
			throws HibernateException, SQLException {
		// note that if we load this collection from a cartesian product
		// the multiplicity would be broken ... so use an idbag instead
		final Object element = persister.readElement( rs, owner, descriptor.getSuffixedElementAliases(), getSession() );
		if ( element != null ) {
			bag.add( element );
		}
		return element;
	}

	@Override
	public void beforeInitialize(CollectionPersister persister, int anticipatedSize) {
		this.bag = (List) persister.getCollectionType().instantiate( anticipatedSize );
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean equalsSnapshot(CollectionPersister persister) throws HibernateException {
		final Type elementType = persister.getElementType();
		final List<Object> sn = (List<Object>) getSnapshot();
		if ( sn.size() != bag.size() ) {
			return false;
		}

		// HHH-11032 - Group objects by Type.getHashCode() to reduce the complexity of the search
		final Map<Integer, List<Object>> hashToInstancesBag = groupByEqualityHash( bag, elementType );
		final Map<Integer, List<Object>> hashToInstancesSn = groupByEqualityHash( sn, elementType );
		if ( hashToInstancesBag.size() != hashToInstancesSn.size() ) {
			return false;
		}

		// First iterate over the hashToInstancesBag entries to see if the number
		// of List values is different for any hash value.
		for ( Map.Entry<Integer, List<Object>> hashToInstancesBagEntry : hashToInstancesBag.entrySet() ) {
			final Integer hash = hashToInstancesBagEntry.getKey();
			final List<Object> instancesBag = hashToInstancesBagEntry.getValue();
			final List<Object> instancesSn = hashToInstancesSn.get( hash );
			if ( instancesSn == null || ( instancesBag.size() != instancesSn.size() ) ) {
				return false;
			}
		}

		// We already know that both hashToInstancesBag and hashToInstancesSn have:
		// 1) the same hash values;
		// 2) the same number of values with the same hash value.

		// Now check if the number of occurrences of each element is the same.
		for ( Map.Entry<Integer, List<Object>> hashToInstancesBagEntry : hashToInstancesBag.entrySet() ) {
			final Integer hash = hashToInstancesBagEntry.getKey();
			final List<Object> instancesBag = hashToInstancesBagEntry.getValue();
			final List<Object> instancesSn = hashToInstancesSn.get( hash );
			for ( Object instance : instancesBag ) {
				if ( !expectOccurrences(
						instance,
						instancesBag,
						elementType,
						countOccurrences( instance, instancesSn, elementType )
				) ) {
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * Groups items in searchedBag according to persistence "equality" as defined in Type.isSame and Type.getHashCode
	 *
	 * @return Map of "equality" hashCode to List of objects
	 */
	private Map<Integer, List<Object>> groupByEqualityHash(List<Object> searchedBag, Type elementType) {
		if ( searchedBag.isEmpty() ) {
			return Collections.emptyMap();
		}
		Map<Integer, List<Object>> map = new HashMap<>();
		for ( Object o : searchedBag ) {
			map.computeIfAbsent( nullableHashCode( o, elementType ), k -> new ArrayList<>() ).add( o );
		}
		return map;
	}

	/**
	 * @param o
	 * @param elementType
	 * @return the default elementType hashcode of the object o, or null if the object is null
	 */
	private Integer nullableHashCode(Object o, Type elementType) {
		if ( o == null ) {
			return null;
		}
		else {
			return elementType.getHashCode( o );
		}
	}

	@Override
	public boolean isSnapshotEmpty(Serializable snapshot) {
		return ( (Collection) snapshot ).isEmpty();
	}

	private int countOccurrences(Object element, List<Object> list, Type elementType) {
		int result = 0;
		for ( Object listElement : list ) {
			if ( elementType.isSame( element, listElement ) ) {
				result++;
			}
		}
		return result;
	}

	private boolean expectOccurrences(Object element, List<Object> list, Type elementType, int expected) {
		int result = 0;
		for ( Object listElement : list ) {
			if ( elementType.isSame( element, listElement ) ) {
				if ( result++ > expected ) {
					return false;
				}
			}
		}
		return result == expected;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Serializable getSnapshot(CollectionPersister persister)
			throws HibernateException {
		final ArrayList clonedList = new ArrayList( bag.size() );
		for ( Object item : bag ) {
			clonedList.add( persister.getElementType().deepCopy( item, persister.getFactory() ) );
		}
		return clonedList;
	}

	@Override
	public Collection getOrphans(Serializable snapshot, String entityName) throws HibernateException {
		final List sn = (List) snapshot;
		return getOrphans( sn, bag, entityName, getSession() );
	}

	@Override
	public Serializable disassemble(CollectionPersister persister)
			throws HibernateException {
		final int length = bag.size();
		final Serializable[] result = new Serializable[length];
		for ( int i = 0; i < length; i++ ) {
			result[i] = persister.getElementType().disassemble( bag.get( i ), getSession(), null );
		}
		return result;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void initializeFromCache(CollectionPersister persister, Serializable disassembled, Object owner)
			throws HibernateException {
		final Serializable[] array = (Serializable[]) disassembled;
		final int size = array.length;
		beforeInitialize( persister, size );
		for ( Serializable item : array ) {
			final Object element = persister.getElementType().assemble( item, getSession(), owner );
			if ( element != null ) {
				bag.add( element );
			}
		}
	}

	@Override
	public boolean needsRecreate(CollectionPersister persister) {
		return !persister.isOneToMany();
	}


	// For a one-to-many, a <bag> is not really a bag;
	// it is *really* a set, since it can't contain the
	// same element twice. It could be considered a bug
	// in the mapping dtd that <bag> allows <one-to-many>.

	// Anyway, here we implement <set> semantics for a
	// <one-to-many> <bag>!

	@Override
	@SuppressWarnings("unchecked")
	public Iterator getDeletes(CollectionPersister persister, boolean indexIsFormula) throws HibernateException {
		final Type elementType = persister.getElementType();
		final ArrayList deletes = new ArrayList();
		final List sn = (List) getSnapshot();
		final Iterator olditer = sn.iterator();
		int i = 0;
		while ( olditer.hasNext() ) {
			final Object old = olditer.next();
			final Iterator newiter = bag.iterator();
			boolean found = false;
			if ( bag.size() > i && elementType.isSame( old, bag.get( i++ ) ) ) {
				//a shortcut if its location didn't change!
				found = true;
			}
			else {
				//search for it
				//note that this code is incorrect for other than one-to-many
				while ( newiter.hasNext() ) {
					if ( elementType.isSame( old, newiter.next() ) ) {
						found = true;
						break;
					}
				}
			}
			if ( !found ) {
				deletes.add( old );
			}
		}
		return deletes.iterator();
	}

	@Override
	public boolean needsInserting(Object entry, int i, Type elemType) throws HibernateException {
		final List sn = (List) getSnapshot();
		if ( sn.size() > i && elemType.isSame( sn.get( i ), entry ) ) {
			//a shortcut if its location didn't change!
			return false;
		}
		else {
			//search for it
			//note that this code is incorrect for other than one-to-many
			for ( Object old : sn ) {
				if ( elemType.isSame( old, entry ) ) {
					return false;
				}
			}
			return true;
		}
	}

	@Override
	public boolean isRowUpdatePossible() {
		return false;
	}

	@Override
	public boolean needsUpdating(Object entry, int i, Type elemType) {
		return false;
	}

	@Override
	public int size() {
		return readSize() ? getCachedSize() : bag.size();
	}

	@Override
	public boolean isEmpty() {
		return readSize() ? getCachedSize() == 0 : bag.isEmpty();
	}

	@Override
	public boolean contains(Object object) {
		final Boolean exists = readElementExistence( object );
		return exists == null ? bag.contains( object ) : exists;
	}

	@Override
	public Iterator iterator() {
		read();
		return new IteratorProxy( bag.iterator() );
	}

	@Override
	public Object[] toArray() {
		read();
		return bag.toArray();
	}

	@Override
	public Object[] toArray(Object[] a) {
		read();
		return bag.toArray( a );
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean add(Object object) {
		if ( !isOperationQueueEnabled() ) {
			write();
			return bag.add( object );
		}
		else {
			queueOperation( new SimpleAdd( object ) );
			return true;
		}
	}

	@Override
	public boolean remove(Object o) {
		initialize( true );
		if ( bag.remove( o ) ) {
			elementRemoved = true;
			dirty();
			return true;
		}
		else {
			return false;
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean containsAll(Collection c) {
		read();
		return bag.containsAll( c );
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean addAll(Collection values) {
		if ( values.size() == 0 ) {
			return false;
		}
		if ( !isOperationQueueEnabled() ) {
			write();
			return bag.addAll( values );
		}
		else {
			for ( Object value : values ) {
				queueOperation( new SimpleAdd( value ) );
			}
			return values.size() > 0;
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean removeAll(Collection c) {
		if ( c.size() > 0 ) {
			initialize( true );
			if ( bag.removeAll( c ) ) {
				elementRemoved = true;
				dirty();
				return true;
			}
			else {
				return false;
			}
		}
		else {
			return false;
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean retainAll(Collection c) {
		initialize( true );
		if ( bag.retainAll( c ) ) {
			dirty();
			return true;
		}
		else {
			return false;
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public void clear() {
		if ( isClearQueueEnabled() ) {
			queueOperation( new Clear() );
		}
		else {
			initialize( true );
			if ( !bag.isEmpty() ) {
				bag.clear();
				dirty();
			}
		}
	}

	@Override
	public Object getIndex(Object entry, int i, CollectionPersister persister) {
		throw new UnsupportedOperationException( "Bags don't have indexes" );
	}

	@Override
	public Object getElement(Object entry) {
		return entry;
	}

	@Override
	public Object getSnapshotElement(Object entry, int i) {
		final List sn = (List) getSnapshot();
		return sn.get( i );
	}

	/**
	 * Count how many times the given object occurs in the elements
	 *
	 * @param o The object to check
	 *
	 * @return The number of occurrences.
	 */
	@SuppressWarnings("UnusedDeclaration")
	public int occurrences(Object o) {
		read();
		final Iterator itr = bag.iterator();
		int result = 0;
		while ( itr.hasNext() ) {
			if ( o.equals( itr.next() ) ) {
				result++;
			}
		}
		return result;
	}

	// List OPERATIONS:

	@Override
	@SuppressWarnings("unchecked")
	public void add(int i, Object o) {
		write();
		bag.add( i, o );
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean addAll(int i, Collection c) {
		if ( c.size() > 0 ) {
			write();
			return bag.addAll( i, c );
		}
		else {
			return false;
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public Object get(int i) {
		read();
		return bag.get( i );
	}

	@Override
	@SuppressWarnings("unchecked")
	public int indexOf(Object o) {
		read();
		return bag.indexOf( o );
	}

	@Override
	@SuppressWarnings("unchecked")
	public int lastIndexOf(Object o) {
		read();
		return bag.lastIndexOf( o );
	}

	@Override
	@SuppressWarnings("unchecked")
	public ListIterator listIterator() {
		read();
		return new ListIteratorProxy( bag.listIterator() );
	}

	@Override
	@SuppressWarnings("unchecked")
	public ListIterator listIterator(int i) {
		read();
		return new ListIteratorProxy( bag.listIterator( i ) );
	}

	@Override
	@SuppressWarnings("unchecked")
	public Object remove(int i) {
		write();
		return bag.remove( i );
	}

	@Override
	@SuppressWarnings("unchecked")
	public Object set(int i, Object o) {
		write();
		return bag.set( i, o );
	}

	@Override
	@SuppressWarnings("unchecked")
	public List subList(int start, int end) {
		read();
		return new ListProxy( bag.subList( start, end ) );
	}

	@Override
	public boolean entryExists(Object entry, int i) {
		return entry != null;
	}

	@Override
	public String toString() {
		read();
		return bag.toString();
	}

	/**
	 * Bag does not respect the collection API and do an
	 * JVM instance comparison to do the equals.
	 * The semantic is broken not to have to initialize a
	 * collection for a simple equals() operation.
	 *
	 * @see java.lang.Object#equals(java.lang.Object)
	 * <p>
	 * {@inheritDoc}
	 */
	@Override
	public boolean equals(Object obj) {
		return super.equals( obj );
	}

	@Override
	public int hashCode() {
		return super.hashCode();
	}

	final class Clear implements DelayedOperation {
		@Override
		public void operate() {
			bag.clear();
		}

		@Override
		public Object getAddedInstance() {
			return null;
		}

		@Override
		public Object getOrphan() {
			throw new UnsupportedOperationException( "queued clear cannot be used with orphan delete" );
		}
	}

	final class SimpleAdd extends AbstractValueDelayedOperation {

		public SimpleAdd(Object addedValue) {
			super( addedValue, null );
		}

		@Override
		@SuppressWarnings("unchecked")
		public void operate() {
			bag.add( getAddedInstance() );
		}
	}
}
