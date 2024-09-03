/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.collection.spi;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.Incubating;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.type.Type;

/**
 * An unordered, un-keyed collection that can contain the same element
 * multiple times. The Java Collections Framework, curiously, has no
 * {@code Bag} interface. It is, however, common to use {@code List}s
 * to represent a collection with bag semantics, so Hibernate follows
 * this practice.
 *
 * @apiNote Incubating in terms of making this non-internal.
 *          These contracts will be getting cleaned up in following
 *          releases.
 *
 * @author Gavin King
 */
@Incubating
public class PersistentBag<E> extends AbstractPersistentCollection<E> implements List<E> {

	protected List<E> bag;

	/**
	 * The Collection provided to a PersistentBag constructor
	 */
	private Collection<E> providedCollection;

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
	 * @param coll The base elements.
	 */
	public PersistentBag(SharedSessionContractImplementor session, Collection<E> coll) {
		super( session );
		providedCollection = coll;
		if ( coll instanceof List ) {
			bag = (List<E>) coll;
		}
		else {
			bag = new ArrayList<>( coll );
		}
		setInitialized();
		setDirectlyAccessible( true );
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
	public Iterator<E> entries(CollectionPersister persister) {
		return bag.iterator();
	}

	public void injectLoadedState(PluralAttributeMapping attributeMapping, List<?> loadingState) {
		assert bag == null;

		final CollectionPersister collectionDescriptor = attributeMapping.getCollectionDescriptor();
		final CollectionSemantics<?,?> collectionSemantics = collectionDescriptor.getCollectionSemantics();

		final int elementCount = loadingState == null ? 0 : loadingState.size();

		this.bag = (List<E>) collectionSemantics.instantiateRaw( elementCount, collectionDescriptor );

		if ( loadingState != null ) {
			for ( int i = 0; i < elementCount; i++ ) {
				bag.add( (E) loadingState.get( i ) );
			}
		}
	}

	@Override
	public boolean equalsSnapshot(CollectionPersister persister) throws HibernateException {
		final Type elementType = persister.getElementType();
		final List<?> sn = (List<?>) getSnapshot();
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
	private Map<Integer, List<Object>> groupByEqualityHash(List<?> searchedBag, Type elementType) {
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
		return ( (Collection<?>) snapshot ).isEmpty();
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
	public Serializable getSnapshot(CollectionPersister persister)
			throws HibernateException {
		final ArrayList<E> clonedList = new ArrayList<>( bag.size() );
		for ( E item : bag ) {
			clonedList.add( (E) persister.getElementType().deepCopy( item, persister.getFactory() ) );
		}
		return clonedList;
	}

	@Override
	public Collection<E> getOrphans(Serializable snapshot, String entityName) throws HibernateException {
		final List<E> sn = (List<E>) snapshot;
		return getOrphans( sn, bag, entityName, getSession() );
	}

	@Override
	public void initializeEmptyCollection(CollectionPersister persister) {
		assert bag == null;
		//noinspection unchecked
		bag = (List<E>) persister.getCollectionSemantics().instantiateRaw( 0, persister );
		endRead();
	}

	@Override
	public Object disassemble(CollectionPersister persister) {
		final int length = bag.size();
		final Serializable[] result = new Serializable[length];
		for ( int i = 0; i < length; i++ ) {
			result[i] = persister.getElementType().disassemble( bag.get( i ), getSession(), null );
		}
		return result;
	}

	@Override
	public void initializeFromCache(CollectionPersister collectionDescriptor, Object disassembled, Object owner)
			throws HibernateException {
		assert bag == null;

		final Serializable[] array = (Serializable[]) disassembled;
		final int size = array.length;

		this.bag = (List<E>) collectionDescriptor.getCollectionSemantics().instantiateRaw( size, collectionDescriptor );

		for ( Serializable item : array ) {
			final Object element = collectionDescriptor.getElementType().assemble( item, getSession(), owner );
			if ( element != null ) {
				bag.add( (E) element );
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
	public Iterator<?> getDeletes(CollectionPersister persister, boolean indexIsFormula) throws HibernateException {
		final Type elementType = persister.getElementType();
		final ArrayList<Object> deletes = new ArrayList<>();
		final List<?> sn = (List<?>) getSnapshot();
		final Iterator<?> olditer = sn.iterator();
		int i = 0;
		while ( olditer.hasNext() ) {
			final Object old = olditer.next();
			final Iterator<E> newiter = bag.iterator();
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
		final List<?> sn = (List<?>) getSnapshot();
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
	public Iterator<E> iterator() {
		read();
		return new IteratorProxy<>( bag.iterator() );
	}

	@Override
	public Object[] toArray() {
		read();
		return bag.toArray();
	}

	@Override
	public <A> A[] toArray(A[] a) {
		read();
		return bag.toArray( a );
	}

	@Override
	public boolean add(E object) {
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
	public boolean containsAll(Collection<?> c) {
		read();
		return bag.containsAll( c );
	}

	@Override
	public boolean addAll(Collection<? extends E> values) {
		if ( values.isEmpty() ) {
			return false;
		}
		if ( !isOperationQueueEnabled() ) {
			write();
			return bag.addAll( values );
		}
		else {
			for ( E value : values ) {
				queueOperation( new SimpleAdd( value ) );
			}
			return values.size() > 0;
		}
	}

	@Override
	public boolean removeAll(Collection<?> c) {
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
	public boolean retainAll(Collection<?> c) {
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
		throw new UnsupportedOperationException( "Bags don't have indexes : " + persister.getRole() );
	}

	@Override
	public Object getElement(Object entry) {
		return entry;
	}

	@Override
	public Object getSnapshotElement(Object entry, int i) {
		final List<?> sn = (List<?>) getSnapshot();
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
		final Iterator<E> itr = bag.iterator();
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
	public void add(int i, E o) {
		write();
		bag.add( i, o );
	}

	@Override
	public boolean addAll(int i, Collection<? extends E> c) {
		if ( c.size() > 0 ) {
			write();
			return bag.addAll( i, c );
		}
		else {
			return false;
		}
	}

	@Override
	public E get(int i) {
		read();
		return bag.get( i );
	}

	@Override
	public int indexOf(Object o) {
		read();
		return bag.indexOf( o );
	}

	@Override
	public int lastIndexOf(Object o) {
		read();
		return bag.lastIndexOf( o );
	}

	@Override
	public ListIterator<E> listIterator() {
		read();
		return new ListIteratorProxy( bag.listIterator() );
	}

	@Override
	public ListIterator<E> listIterator(int i) {
		read();
		return new ListIteratorProxy( bag.listIterator( i ) );
	}

	@Override
	public E remove(int i) {
		write();
		return bag.remove( i );
	}

	@Override
	public E set(int i, E o) {
		write();
		return bag.set( i, o );
	}

	@Override
	public List<E> subList(int start, int end) {
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
	 * For efficiency, bag does not respect the semantics of
	 * {@link List#equals(Object)} as specified by the supertype
	 * {@link List}. Instead, instance equality is used, to avoid
	 * the need to fetch the elements of the bag.
	 *
	 * @see Object#equals(Object)
	 */
	@Override
	public boolean equals(Object obj) {
		return super.equals( obj );
	}

	@Override
	public int hashCode() {
		return super.hashCode();
	}

	final class Clear implements DelayedOperation<E> {
		@Override
		public void operate() {
			bag.clear();
		}

		@Override
		public E getAddedInstance() {
			return null;
		}

		@Override
		public E getOrphan() {
			throw new UnsupportedOperationException( "queued clear cannot be used with orphan delete" );
		}
	}

	final class SimpleAdd extends AbstractValueDelayedOperation {

		public SimpleAdd(E addedValue) {
			super( addedValue, null );
		}

		@Override
		public void operate() {
			// Delayed operations only work on inverse collections i.e. collections with mappedBy,
			// and these collections don't have duplicates by definition.
			// Since cascading also operates on delayed operation's elements,
			// it can happen that an element is already associated with the collection after cascading,
			// but the queued operations are still executed after the lazy initialization of the collection.
			// To avoid duplicates, we have to check if the bag already contains this element
			if ( !bag.contains( getAddedInstance() ) ) {
				bag.add( getAddedInstance() );
			}
		}
	}

	final class SimpleRemove extends AbstractValueDelayedOperation {

		public SimpleRemove(E orphan) {
			super( null, orphan );
		}

		@Override
		public void operate() {
			bag.remove( getOrphan() );
		}
	}
}
