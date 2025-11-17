/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.collection.spi;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.Incubating;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.type.Type;

import static org.hibernate.generator.EventType.INSERT;

/**
 * An "identifier bag" implements "bag" semantics more efficiently than a
 * regular bag by adding a synthetic identifier column to the table. This
 * identifier is unique over all rows in the table, allowing very efficient
 * updates and deletes. The value of the identifier is never exposed to the
 * application.
 * <p>
 * Identifier bags may not be used for a many-to-one association.
 * Furthermore, there is no reason to use {@code inverse="true"}.
 *
 * @apiNote Incubating in terms of making this non-internal.
 *          These contracts will be getting cleaned up in following
 *          releases.
 *
 * @author Gavin King
 */
@Incubating
public class PersistentIdentifierBag<E> extends AbstractPersistentCollection<E> implements List<E> {
	/**
	 * @deprecated Use {@link #bagAsList()} or {@link #collection} instead.
	 */
	@Deprecated(forRemoval = true, since = "7")
	protected List<E> values;
	protected Map<Integer, Object> identifiers;

	/**
	 * The actual bag.
	 * For backwards compatibility reasons, the {@link #values} field remains as {@link List},
	 * but might be {@code null} when the bag does not implement the {@link List} interface,
	 * whereas this collection field will always contain the actual bag instance.
	 */
	protected Collection<E> collection;

	/**
	 * Constructs a PersistentIdentifierBag.  This form needed for SOAP libraries, etc
	 */
	@SuppressWarnings("unused")
	public PersistentIdentifierBag() {
	}

	/**
	 * Constructs a PersistentIdentifierBag.
	 *
	 * @param session The session
	 */
	public PersistentIdentifierBag(SharedSessionContractImplementor session) {
		super( session );
	}

	/**
	 * Constructs a PersistentIdentifierBag.
	 *
	 * @param session The session
	 * @param coll The base elements
	 */
	public PersistentIdentifierBag(SharedSessionContractImplementor session, Collection<E> coll) {
		super( session );
		setCollection( coll );
		setInitialized();
		setDirectlyAccessible( true );
		identifiers = new HashMap<>();
	}

	private void setCollection(Collection<E> bag) {
		this.collection = bag;
		this.values = bag instanceof List<E> list ? list : null;
	}

	protected List<E> bagAsList() {
		if ( values == null ) {
			throw new IllegalStateException( "Bag is not a list: " + collection.getClass().getName() );
		}
		return values;
	}

	@Override
	public void initializeFromCache(CollectionPersister persister, Object disassembled, Object owner)
			throws HibernateException {
		final Serializable[] array = (Serializable[]) disassembled;
		final int size = array.length;

		assert identifiers == null;
		assert collection == null;

		identifiers = new HashMap<>();
		//noinspection unchecked
		setCollection( (Collection<E>) persister.getCollectionSemantics().instantiateRaw( size, persister ) );

		for ( int i = 0; i < size; i+=2 ) {
			identifiers.put(
				(i/2),
				persister.getIdentifierType().assemble( array[i], getSession(), owner )
			);
			collection.add( (E) persister.getElementType().assemble( array[i+1], getSession(), owner ) );
		}
	}

	@Override
	public Object getIdentifier(Object entry, int i) {
		return identifiers.get( i );
	}

	@Override
	public boolean isWrapper(Object collection) {
		return this.collection == collection;
	}

	@Override
	public boolean add(E o) {
		write();
		collection.add( o );
		return true;
	}

	@Override
	public void clear() {
		initialize( true );
		if ( ! collection.isEmpty() || ! identifiers.isEmpty() ) {
			collection.clear();
			identifiers.clear();
			dirty();
		}
	}

	@Override
	public boolean contains(Object o) {
		read();
		return collection.contains( o );
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		read();
		return collection.containsAll( c );
	}

	@Override
	public boolean isEmpty() {
		return readSize() ? getCachedSize()==0 : collection.isEmpty();
	}

	@Override
	public Iterator<E> iterator() {
		read();
		return new IteratorProxy<>( collection.iterator() );
	}

	@Override
	public boolean remove(Object o) {
		initialize( true );
		final int index = bagAsList().indexOf( o );
		if ( index >= 0 ) {
			beforeRemove( index );
			bagAsList().remove( index );
			elementRemoved = true;
			dirty();
			return true;
		}
		else {
			return false;
		}
	}

	@Override
	public boolean removeAll(Collection c) {
		if ( c.size() > 0 ) {
			boolean result = false;
			for ( Object element : c ) {
				if ( remove( element ) ) {
					result = true;
				}
			}
			return result;
		}
		else {
			return false;
		}
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		initialize( true );
		if ( collection.retainAll( c ) ) {
			dirty();
			return true;
		}
		else {
			return false;
		}
	}

	@Override
	public int size() {
		return readSize() ? getCachedSize() : collection.size();
	}

	@Override
	public Object[] toArray() {
		read();
		return collection.toArray();
	}

	@Override
	public <A> A[] toArray(A[] a) {
		read();
		return collection.toArray( a );
	}

	@Override
	public Object disassemble(CollectionPersister persister) {
		final Serializable[] result = new Serializable[ collection.size() * 2 ];
		int i = 0;
		int j = 0;
		for ( E value : collection ) {
			result[i++] = persister.getIdentifierType().disassemble( identifiers.get( j ), getSession(), null );
			result[i++] = persister.getElementType().disassemble( value, getSession(), null );
			j++;
		}
		return result;
	}

	@Override
	public boolean empty() {
		return collection.isEmpty();
	}

	@Override
	public Iterator<E> entries(CollectionPersister persister) {
		return collection.iterator();
	}

	@Override
	public boolean entryExists(Object entry, int i) {
		return entry!=null;
	}

	@Override
	public boolean equalsSnapshot(CollectionPersister persister) throws HibernateException {
		final Type elementType = persister.getElementType();
		final Map<?,?> snap = (Map<?,?>) getSnapshot();
		if ( snap.size()!= collection.size() ) {
			return false;
		}
		int i = 0;
		for ( E value : collection ) {
			final Object id = identifiers.get( i++ );
			if ( id == null ) {
				return false;
			}
			final Object old = snap.get( id );
			if ( elementType.isDirty( old, value, getSession() ) ) {
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean isSnapshotEmpty(Serializable snapshot) {
		return ( (Map<?,?>) snapshot ).isEmpty();
	}

	@Override
	public Iterator<?> getDeletes(CollectionPersister persister, boolean indexIsFormula) throws HibernateException {
		final Map<?,?> snap = (Map<?,?>) getSnapshot();
		final List<Object> deletes = new ArrayList<>( snap.keySet() );
		int i = 0;
		for ( E value : collection ) {
			if ( value != null ) {
				deletes.remove( identifiers.get( i ) );
			}
			i++;
		}
		return deletes.iterator();
	}

	@Override
	public boolean hasDeletes(CollectionPersister persister) {
		final Map<?,?> snap = (Map<?,?>) getSnapshot();
		int deletes = snap.size();
		for ( E value : collection ) {
			if ( value != null ) {
				deletes --;
			}
		}
		return deletes > 0;
	}

	@Override
	public Object getIndex(Object entry, int i, CollectionPersister persister) {
		throw new UnsupportedOperationException("Bags don't have indexes");
	}

	@Override
	public Object getElement(Object entry) {
		return entry;
	}

	@Override
	public Object getSnapshotElement(Object entry, int i) {
		final Map<?,?> snap = (Map<?,?>) getSnapshot();
		final Object id = identifiers.get( i );
		return snap.get( id );
	}

	@Override
	public boolean needsInserting(Object entry, int i, Type elemType)
			throws HibernateException {
		final Map<?,?> snap = (Map<?,?>) getSnapshot();
		final Object id = identifiers.get( i );
		return entry != null
				&& ( id==null || snap.get( id )==null );
	}

	@Override
	public boolean needsUpdating(Object entry, int i, Type elemType) throws HibernateException {
		if ( entry == null ) {
			return false;
		}

		final Map<?,?> snap = (Map<?,?>) getSnapshot();
		final Object id = identifiers.get( i );
		if ( id == null ) {
			return false;
		}

		final Object old = snap.get( id );
		return old != null && elemType.isDirty( old, entry, getSession() );
	}

	@Override
	public Serializable getSnapshot(CollectionPersister persister) throws HibernateException {
		final HashMap<Object,E> map = CollectionHelper.mapOfSize( collection.size() );
		final Iterator<E> iter = collection.iterator();
		int i=0;
		while ( iter.hasNext() ) {
			final Object value = iter.next();
			map.put(
					identifiers.get( i++ ),
					(E) persister.getElementType().deepCopy( value, persister.getFactory() )
			);
		}
		return map;
	}

	@Override
	public Collection<E> getOrphans(Serializable snapshot, String entityName) throws HibernateException {
		final Map<Object,E> sn = (Map<Object,E>) snapshot;
		return getOrphans( sn.values(), collection, entityName, getSession() );
	}

	@Override
	public void initializeEmptyCollection(CollectionPersister persister) {
		assert identifiers == null;
		identifiers = new HashMap<>();
		//noinspection unchecked
		setCollection( (Collection<E>) persister.getCollectionSemantics().instantiateRaw( 0, persister ) );
		endRead();
	}

	@Override
	public void preInsert(CollectionPersister persister) throws HibernateException {
		final Iterator<E> itr = collection.iterator();
		int i = 0;
		while ( itr.hasNext() ) {
			final E entry = itr.next();
			final Integer loc = i++;
			if ( !identifiers.containsKey( loc ) ) {
				//TODO: native ids
				final Object id = persister.getGenerator().generate( getSession(), entry, null, INSERT );
				identifiers.put( loc, id );
			}
		}
	}

	@Override
	public void add(int index, E element) {
		write();
		beforeAdd( index );
		bagAsList().add( index, element );
	}

	@Override
	public boolean addAll(int index, Collection<? extends E> c) {
		if ( c.size() > 0 ) {
			for ( E element : c ) {
				add( index++, element );
			}
			return true;
		}
		else {
			return false;
		}
	}

	@Override
	public E get(int index) {
		read();
		return bagAsList().get( index );
	}

	@Override
	public int indexOf(Object o) {
		read();
		return bagAsList().indexOf( o );
	}

	@Override
	public int lastIndexOf(Object o) {
		read();
		return bagAsList().lastIndexOf( o );
	}

	@Override
	public ListIterator<E> listIterator() {
		read();
		return new ListIteratorProxy( bagAsList().listIterator() );
	}

	@Override
	public ListIterator<E> listIterator(int index) {
		read();
		return new ListIteratorProxy( bagAsList().listIterator( index ) );
	}

	private void beforeRemove(int index) {
		final Object removedId = identifiers.get( index );
		final int last = collection.size()-1;
		for ( int i=index; i<last; i++ ) {
			final Object id = identifiers.get( i+1 );
			if ( id == null ) {
				identifiers.remove( i );
			}
			else {
				identifiers.put( i, id );
			}
		}
		identifiers.put( last, removedId );
	}

	private void beforeAdd(int index) {
		for ( int i=index; i<collection.size(); i++ ) {
			identifiers.put( i+1, identifiers.get( i ) );
		}
		identifiers.remove( index );
	}

	@Override
	public E remove(int index) {
		write();
		beforeRemove( index );
		return bagAsList().remove( index );
	}

	@Override
	public E set(int index, E element) {
		write();
		return bagAsList().set( index, element );
	}

	@Override
	public List<E> subList(int fromIndex, int toIndex) {
		read();
		return new ListProxy( bagAsList().subList( fromIndex, toIndex ) );
	}

	@Override
	public boolean addAll(Collection<? extends E> c) {
		if ( c.size()> 0 ) {
			write();
			return collection.addAll( c );
		}
		else {
			return false;
		}
	}

	@Override
	public void afterRowInsert(
			CollectionPersister persister,
			Object entry,
			int i) throws HibernateException {
		//TODO: if we are using identity columns, fetch the identifier
	}

	public void injectLoadedState(PluralAttributeMapping attributeMapping, List<?> loadingState) {
		assert identifiers == null;
		assert collection == null;

		final CollectionPersister collectionDescriptor = attributeMapping.getCollectionDescriptor();
		final CollectionSemantics<?,?> collectionSemantics = collectionDescriptor.getCollectionSemantics();

		final int elementCount = loadingState == null ? 0 : loadingState.size();

		identifiers = new HashMap<>();
		//noinspection unchecked
		setCollection( (Collection<E>) collectionSemantics.instantiateRaw( elementCount, collectionDescriptor ) );

		for ( int i = 0; i < loadingState.size(); i++ ) {
			final Object[] row = (Object[]) loadingState.get( i );
			final Object identifier = row[0];
			final E element = (E) row[1];

			Object old = identifiers.put( collection.size(), identifier );
			if ( old == null ) {
				//maintain correct duplication if loaded in a cartesian product
				collection.add( element );
			}
		}
	}
}
