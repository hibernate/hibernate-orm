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
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.hibernate.HibernateException;
import org.hibernate.Incubating;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.type.Type;

/**
 * A persistent wrapper for a {@link java.util.List}. Underlying
 * collection is an {@code ArrayList}.
 *
 * @apiNote Incubating in terms of making this non-internal.
 *          These contracts will be getting cleaned up in following
 *          releases.
 *
 * @author Gavin King
 */
@Incubating
public class PersistentList<E> extends AbstractPersistentCollection<E> implements List<E> {
	protected List<E> list;

	/**
	 * Constructs a PersistentList.  This form needed for SOAP libraries, etc
	 */
	public PersistentList() {
	}

	/**
	 * Constructs a PersistentList.
	 *
	 * @param session The session
	 */
	public PersistentList(SharedSessionContractImplementor session) {
		super( session );
	}

	/**
	 * Constructs a PersistentList.
	 *
	 * @param session The session
	 * @param list The raw list
	 */
	public PersistentList(SharedSessionContractImplementor session, List<E> list) {
		super( session );
		this.list = list;
		setInitialized();
		setDirectlyAccessible( true );
	}

	protected List<E> getRawList() {
		return list;
	}

	@Override
	public Serializable getSnapshot(CollectionPersister persister) throws HibernateException {
		final ArrayList<Object> clonedList = new ArrayList<>( list.size() );
		for ( Object element : list ) {
			final Object deepCopy = persister.getElementType().deepCopy( element, persister.getFactory() );
			clonedList.add( deepCopy );
		}
		return clonedList;
	}

	@Override
	public Collection<E> getOrphans(Serializable snapshot, String entityName) throws HibernateException {
		return getOrphans( (List<E>) snapshot, list, entityName, getSession() );
	}

	@Override
	@SuppressWarnings("unchecked")
	public void initializeEmptyCollection(CollectionPersister persister) {
		assert list == null;
		//noinspection unchecked
		list = (List<E>) persister.getCollectionSemantics().instantiateRaw( 0, persister );
		endRead();
	}

	@Override
	public boolean equalsSnapshot(CollectionPersister persister) throws HibernateException {
		final Type elementType = persister.getElementType();
		final List<?> sn = (List<?>) getSnapshot();
		if ( sn.size() != this.list.size() ) {
			return false;
		}
		final Iterator<?> itr = list.iterator();
		final Iterator<?> snapshotItr = sn.iterator();
		while ( itr.hasNext() ) {
			if ( elementType.isDirty( itr.next(), snapshotItr.next(), getSession() ) ) {
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean isSnapshotEmpty(Serializable snapshot) {
		return ( (Collection<?>) snapshot ).isEmpty();
	}

	@Override
	@SuppressWarnings("unchecked")
	public void initializeFromCache(CollectionPersister persister, Object disassembled, Object owner)
			throws HibernateException {
		final Serializable[] array = (Serializable[]) disassembled;
		final int size = array.length;

		assert list == null;
		list = (List<E>) persister.getCollectionSemantics().instantiateRaw( size, persister );

		for ( Serializable arrayElement : array ) {
			list.add( (E) persister.getElementType().assemble( arrayElement, getSession(), owner ) );
		}
	}

	@Override
	public void injectLoadedState(PluralAttributeMapping attributeMapping, List<?> loadingStateList) {
		assert isInitializing();
		assert list == null;

		final CollectionPersister collectionDescriptor = attributeMapping.getCollectionDescriptor();

		this.list = (List<E>) collectionDescriptor.getCollectionSemantics().instantiateRaw(
				loadingStateList.size(),
				collectionDescriptor
		);

		list.addAll( (List<E>) loadingStateList );
	}

	@Override
	public boolean isWrapper(Object collection) {
		return list==collection;
	}

	@Override
	public int size() {
		return readSize() ? getCachedSize() : list.size();
	}

	@Override
	public boolean isEmpty() {
		return readSize() ? getCachedSize()==0 : list.isEmpty();
	}

	@Override
	public boolean contains(Object object) {
		final Boolean exists = readElementExistence( object );
		return exists == null
				? list.contains( object )
				: exists;
	}

	@Override
	public Iterator<E> iterator() {
		read();
		return new IteratorProxy<>( list.iterator() );
	}

	@Override
	public Object[] toArray() {
		read();
		return list.toArray();
	}

	@Override
	public <A> A[] toArray(A[] array) {
		read();
		return list.toArray( array );
	}

	@Override
	public boolean add(E object) {
		if ( !isOperationQueueEnabled() ) {
			write();
			return list.add( object );
		}
		else {
			queueOperation( new SimpleAdd( object ) );
			return true;
		}
	}

	@Override
	public boolean remove(Object value) {
		final Boolean exists = isPutQueueEnabled() ? readElementExistence( value ) : null;
		if ( exists == null ) {
			initialize( true );
			if ( list.remove( value ) ) {
				elementRemoved = true;
				dirty();
				return true;
			}
			else {
				return false;
			}
		}
		else if ( exists ) {
			elementRemoved = true;
			queueOperation( new SimpleRemove( (E) value ) );
			return true;
		}
		else {
			return false;
		}
	}

	@Override
	public boolean containsAll(Collection<?> coll) {
		read();
		return list.containsAll( coll );
	}

	@Override
	public boolean addAll(Collection<? extends E> values) {
		if ( values.isEmpty() ) {
			return false;
		}
		if ( !isOperationQueueEnabled() ) {
			write();
			return list.addAll( values );
		}
		else {
			for ( E value : values ) {
				queueOperation( new SimpleAdd( value ) );
			}
			return !values.isEmpty();
		}
	}

	@Override
	public boolean addAll(int index, Collection<? extends E> coll) {
		if ( coll.size() > 0 ) {
			write();
			return list.addAll( index, coll );
		}
		else {
			return false;
		}
	}

	@Override
	public boolean removeAll(Collection<?> coll) {
		if ( coll.size() > 0 ) {
			initialize( true );
			if ( list.removeAll( coll ) ) {
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
	public boolean retainAll(Collection<?> coll) {
		initialize( true );
		if ( list.retainAll( coll ) ) {
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
			if ( ! list.isEmpty() ) {
				list.clear();
				dirty();
			}
		}
	}

	@Override
	public E get(int index) {
		if ( index < 0 ) {
			throw new ArrayIndexOutOfBoundsException( "negative index" );
		}
		final Object result = readElementByIndex( index );
		return result == UNKNOWN ? list.get( index ) : (E) result;
	}

	@Override
	public E set(int index, E value) {
		if (index<0) {
			throw new ArrayIndexOutOfBoundsException("negative index");
		}

		final Object old = isPutQueueEnabled() ? readElementByIndex( index ) : UNKNOWN;

		if ( old==UNKNOWN ) {
			write();
			return list.set( index, value );
		}
		else {
			queueOperation( new Set( index, value, (E) old ) );
			return (E) old;
		}
	}

	@Override
	public E remove(int index) {
		if ( index < 0 ) {
			throw new ArrayIndexOutOfBoundsException( "negative index" );
		}
		final Object old = isPutQueueEnabled() ? readElementByIndex( index ) : UNKNOWN;
		elementRemoved = true;
		if ( old == UNKNOWN ) {
			write();
			dirty();
			return list.remove( index );
		}
		else {
			queueOperation( new Remove( index, (E) old ) );
			return (E) old;
		}
	}

	@Override
	public void add(int index, E value) {
		if ( index < 0 ) {
			throw new ArrayIndexOutOfBoundsException( "negative index" );
		}
		write();
		list.add( index, value );
	}

	@Override
	public int indexOf(Object value) {
		read();
		return list.indexOf( value );
	}

	@Override
	public int lastIndexOf(Object value) {
		read();
		return list.lastIndexOf( value );
	}

	@Override
	public ListIterator<E> listIterator() {
		read();
		return new ListIteratorProxy( list.listIterator() );
	}

	@Override
	public ListIterator<E> listIterator(int index) {
		read();
		return new ListIteratorProxy( list.listIterator( index ) );
	}

	@Override
	public List<E> subList(int from, int to) {
		read();
		return new ListProxy( list.subList( from, to ) );
	}

	@Override
	public boolean empty() {
		return list.isEmpty();
	}

	@Override
	public String toString() {
		read();
		return list.toString();
	}

	@Override
	public Iterator<E> entries(CollectionPersister persister) {
		return list.iterator();
	}

	@Override
	public Object disassemble(CollectionPersister persister) throws HibernateException {
		final int length = list.size();
		final Serializable[] result = new Serializable[length];
		for ( int i=0; i<length; i++ ) {
			result[i] = persister.getElementType().disassemble( list.get( i ), getSession(), null );
		}
		return result;
	}

	@Override
	public Iterator<?> getDeletes(CollectionPersister persister, boolean indexIsFormula) throws HibernateException {
		final List<Object> deletes = new ArrayList<>();
		final List<?> sn = (List<?>) getSnapshot();
		int end;
		if ( sn.size() > list.size() ) {
			for ( int i=list.size(); i<sn.size(); i++ ) {
				deletes.add( indexIsFormula ? sn.get( i ) : i );
			}
			end = list.size();
		}
		else {
			end = sn.size();
		}
		for ( int i=0; i<end; i++ ) {
			final Object item = list.get( i );
			final Object snapshotItem = sn.get( i );
			if ( item == null && snapshotItem != null ) {
				deletes.add( indexIsFormula ? snapshotItem : i );
			}
		}
		return deletes.iterator();
	}

	@Override
	public boolean needsInserting(Object entry, int i, Type elemType) throws HibernateException {
		final List<?> sn = (List<?>) getSnapshot();
		return list.get( i ) != null && ( i >= sn.size() || sn.get( i ) == null );
	}

	@Override
	public boolean needsUpdating(Object entry, int i, Type elemType) throws HibernateException {
		final List<?> sn = (List<?>) getSnapshot();
		return i < sn.size()
				&& sn.get( i ) != null
				&& list.get( i ) != null
				&& elemType.isDirty( list.get( i ), sn.get( i ), getSession() );
	}

	@Override
	public Object getIndex(Object entry, int i, CollectionPersister persister) {
		return i;
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

	@Override
	@SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
	public boolean equals(Object other) {
		read();
		return list.equals( other );
	}

	@Override
	public int hashCode() {
		read();
		return list.hashCode();
	}

	@Override
	public boolean entryExists(Object entry, int i) {
		return entry!=null;
	}

	final class Clear implements DelayedOperation<E> {
		@Override
		public void operate() {
			list.clear();
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

	protected final class SimpleAdd extends AbstractValueDelayedOperation {

		public SimpleAdd(E addedValue) {
			super( addedValue, null );
		}

		@Override
		public void operate() {
			list.add( getAddedInstance() );
		}
	}

	abstract class AbstractListValueDelayedOperation extends AbstractValueDelayedOperation {
		private final int index;

		AbstractListValueDelayedOperation(Integer index, E addedValue, E orphan) {
			super( addedValue, orphan );
			this.index = index;
		}

		protected final int getIndex() {
			return index;
		}
	}

	final class Add extends AbstractListValueDelayedOperation {

		public Add(int index, E addedValue) {
			super( index, addedValue, null );
		}

		@Override
		public void operate() {
			list.add( getIndex(), getAddedInstance() );
		}
	}

	final class Set extends AbstractListValueDelayedOperation {

		public Set(int index, E addedValue, E orphan) {
			super( index, addedValue, orphan );
		}

		@Override
		public void operate() {
			list.set( getIndex(), getAddedInstance() );
		}
	}

	final class Remove extends AbstractListValueDelayedOperation {

		public Remove(int index, E orphan) {
			super( index, null, orphan );
		}

		@Override
		public void operate() {
			list.remove( getIndex() );
		}
	}

	final class SimpleRemove extends AbstractValueDelayedOperation {

		public SimpleRemove(E orphan) {
			super( null, orphan );
		}

		@Override
		public void operate() {
			list.remove( getOrphan() );
		}
	}
}
