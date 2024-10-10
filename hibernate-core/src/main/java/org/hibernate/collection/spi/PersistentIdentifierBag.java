/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
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
	protected List<E> values;
	protected Map<Integer, Object> identifiers;

	/**
	 * The Collection provided to a PersistentIdentifierBag constructor
	 */
	private Collection<E> providedValues;

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
		providedValues = coll;
		if (coll instanceof List) {
			values = (List<E>) coll;
		}
		else {
			values = new ArrayList<>( coll );
		}
		setInitialized();
		setDirectlyAccessible( true );
		identifiers = new HashMap<>();
	}

	@Override
	public void initializeFromCache(CollectionPersister persister, Object disassembled, Object owner)
			throws HibernateException {
		final Serializable[] array = (Serializable[]) disassembled;
		final int size = array.length;

		assert identifiers == null;
		assert values == null;

		identifiers = new HashMap<>();
		values = size <= 0
				? new ArrayList<>()
				: new ArrayList<>( size );

		for ( int i = 0; i < size; i+=2 ) {
			identifiers.put(
				(i/2),
				persister.getIdentifierType().assemble( array[i], getSession(), owner )
			);
			values.add( (E) persister.getElementType().assemble( array[i+1], getSession(), owner ) );
		}
	}

	@Override
	public Object getIdentifier(Object entry, int i) {
		return identifiers.get( i );
	}

	@Override
	public boolean isWrapper(Object collection) {
		return values == collection;
	}

	@Override
	public boolean isDirectlyProvidedCollection(Object collection) {
		return isDirectlyAccessible() && providedValues == collection;
	}

	@Override
	public boolean add(E o) {
		write();
		values.add( o );
		return true;
	}

	@Override
	public void clear() {
		initialize( true );
		if ( ! values.isEmpty() || ! identifiers.isEmpty() ) {
			values.clear();
			identifiers.clear();
			dirty();
		}
	}

	@Override
	public boolean contains(Object o) {
		read();
		return values.contains( o );
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		read();
		return values.containsAll( c );
	}

	@Override
	public boolean isEmpty() {
		return readSize() ? getCachedSize()==0 : values.isEmpty();
	}

	@Override
	public Iterator<E> iterator() {
		read();
		return new IteratorProxy<>( values.iterator() );
	}

	@Override
	public boolean remove(Object o) {
		initialize( true );
		final int index = values.indexOf( o );
		if ( index >= 0 ) {
			beforeRemove( index );
			values.remove( index );
			elementRemoved = true;
			dirty();
			return true;
		}
		else {
			return false;
		}
	}

	@Override
	public void queueRemoveOperation(Object o) {
		if ( !isInitialized() ) {
			final DelayedOperation operation = new DelayedOperation() {
				@Override
				public void operate() {
					remove( o );
				}

				@Override
				public Object getAddedInstance() {
					return null;
				}

				@Override
				public Object getOrphan() {
					return null;
				}
			};
			queueOperation( operation );
		}
		else {
			remove( o );
		}
	}

	@Override
	public void queueAddOperation(E o){
		if ( !isInitialized() ) {
			final DelayedOperation operation = new DelayedOperation() {
				@Override
				public void operate() {
					add( o );
				}

				@Override
				public Object getAddedInstance() {
					return null;
				}

				@Override
				public Object getOrphan() {
					return null;
				}
			};
			queueOperation( operation );
		}
		else {
			add( o );
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
		if ( values.retainAll( c ) ) {
			dirty();
			return true;
		}
		else {
			return false;
		}
	}

	@Override
	public int size() {
		return readSize() ? getCachedSize() : values.size();
	}

	@Override
	public Object[] toArray() {
		read();
		return values.toArray();
	}

	@Override
	public <A> A[] toArray(A[] a) {
		read();
		return values.toArray( a );
	}

	@Override
	public Object disassemble(CollectionPersister persister) {
		final Serializable[] result = new Serializable[ values.size() * 2 ];
		int i = 0;
		for ( int j=0; j< values.size(); j++ ) {
			final Object value = values.get( j );
			result[i++] = persister.getIdentifierType().disassemble( identifiers.get( j ), getSession(), null );
			result[i++] = persister.getElementType().disassemble( value, getSession(), null );
		}
		return result;
	}

	@Override
	public boolean empty() {
		return values.isEmpty();
	}

	@Override
	public Iterator<E> entries(CollectionPersister persister) {
		return values.iterator();
	}

	@Override
	public boolean entryExists(Object entry, int i) {
		return entry!=null;
	}

	@Override
	public boolean equalsSnapshot(CollectionPersister persister) throws HibernateException {
		final Type elementType = persister.getElementType();
		final Map<?,?> snap = (Map<?,?>) getSnapshot();
		if ( snap.size()!= values.size() ) {
			return false;
		}
		for ( int i=0; i<values.size(); i++ ) {
			final Object value = values.get( i );
			final Object id = identifiers.get( i );
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
		for ( int i=0; i<values.size(); i++ ) {
			if ( values.get( i ) != null ) {
				deletes.remove( identifiers.get( i ) );
			}
		}
		return deletes.iterator();
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
		final HashMap<Object,E> map = CollectionHelper.mapOfSize( values.size() );
		final Iterator<E> iter = values.iterator();
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
		return getOrphans( sn.values(), values, entityName, getSession() );
	}

	@Override
	public void initializeEmptyCollection(CollectionPersister persister) {
		assert identifiers == null;
		identifiers = new HashMap<>();
		values = new ArrayList<>();
		endRead();
	}

	@Override
	public void preInsert(CollectionPersister persister) throws HibernateException {
		final Iterator<E> itr = values.iterator();
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
		values.add( index, element );
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
		return values.get( index );
	}

	@Override
	public int indexOf(Object o) {
		read();
		return values.indexOf( o );
	}

	@Override
	public int lastIndexOf(Object o) {
		read();
		return values.lastIndexOf( o );
	}

	@Override
	public ListIterator<E> listIterator() {
		read();
		return new ListIteratorProxy( values.listIterator() );
	}

	@Override
	public ListIterator<E> listIterator(int index) {
		read();
		return new ListIteratorProxy( values.listIterator( index ) );
	}

	private void beforeRemove(int index) {
		final Object removedId = identifiers.get( index );
		final int last = values.size()-1;
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
		for ( int i=index; i<values.size(); i++ ) {
			identifiers.put( i+1, identifiers.get( i ) );
		}
		identifiers.remove( index );
	}

	@Override
	public E remove(int index) {
		write();
		beforeRemove( index );
		return values.remove( index );
	}

	@Override
	public E set(int index, E element) {
		write();
		return values.set( index, element );
	}

	@Override
	public List<E> subList(int fromIndex, int toIndex) {
		read();
		return new ListProxy( values.subList( fromIndex, toIndex ) );
	}

	@Override
	public boolean addAll(Collection<? extends E> c) {
		if ( c.size()> 0 ) {
			write();
			return values.addAll( c );
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
		assert values == null;

		identifiers = new HashMap<>();
		values = new ArrayList<>( loadingState.size() );

		for ( int i = 0; i < loadingState.size(); i++ ) {
			final Object[] row = (Object[]) loadingState.get( i );
			final Object identifier = row[0];
			final E element = (E) row[1];

			Object old = identifiers.put( values.size(), identifier );
			if ( old == null ) {
				//maintain correct duplication if loaded in a cartesian product
				values.add( element );
			}
		}
	}
}
