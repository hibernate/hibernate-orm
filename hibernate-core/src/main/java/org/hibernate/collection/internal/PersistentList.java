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
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.hibernate.HibernateException;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;

/**
 * A persistent wrapper for a <tt>java.util.List</tt>. Underlying
 * collection is an <tt>ArrayList</tt>.
 *
 * @see java.util.ArrayList
 * @author Gavin King
 */
public class PersistentList<E> extends AbstractPersistentCollection<E> implements List<E> {
	private List<E> list;

	/**
	 * Constructs a PersistentList.
	 *
	 * @param session The session
	 * @param list The raw list
	 */
	public PersistentList(
			SharedSessionContractImplementor session,
			PersistentCollectionDescriptor<?,List<E>,E> descriptor,
			List<E> list) {
		super( session, descriptor );
		setList( list );
		setDirectlyAccessible( true );
	}

	private void setList(List<E> list) {
		this.list = list;
		setInitialized();
	}

	public PersistentList(
			SharedSessionContractImplementor session,
			PersistentCollectionDescriptor<?,List<E>,E> collectionDescriptor,
			Object key) {
		super( session, collectionDescriptor, key );
	}

	protected List list() {
		return list;
	}





	@Override
	@SuppressWarnings( {"unchecked"})
	public Serializable getSnapshot(PersistentCollectionDescriptor descriptor) throws HibernateException {
		final ArrayList clonedList = new ArrayList( list.size() );
		for ( Object element : list ) {
			final Object deepCopy = descriptor.getElementDescriptor().getJavaTypeDescriptor().getMutabilityPlan().deepCopy( element );
			clonedList.add( deepCopy );
		}
		return clonedList;
	}

	@Override
	public Collection getOrphans(Serializable snapshot, String entityName) throws HibernateException {
		final List sn = (List) snapshot;
		return getOrphans( sn, list, entityName, getSession() );
	}

	@Override
	public boolean equalsSnapshot(PersistentCollectionDescriptor collectionDescriptor) throws HibernateException {
		final List sn = (List) getSnapshot();
		if ( sn.size()!=this.list.size() ) {
			return false;
		}
		final Iterator itr = list.iterator();
		final Iterator snapshotItr = sn.iterator();
		while ( itr.hasNext() ) {
			if ( collectionDescriptor.isDirty( itr.next(), snapshotItr.next(), getSession() ) ) {
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean isSnapshotEmpty(Serializable snapshot) {
		return ( (Collection) snapshot ).isEmpty();
	}

	@Override
	public void beforeInitialize(
			int anticipatedSize,
			PersistentCollectionDescriptor collectionDescriptor) {
		this.list = (List) getCollectionDescriptor().instantiateRaw( anticipatedSize );
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
		return new IteratorProxy( list.iterator() );
	}

	@Override
	public Object[] toArray() {
		read();
		return list.toArray();
	}

	@Override
	public Object[] toArray(Object[] array) {
		read();
		return list.toArray( array );
	}

	@Override
	@SuppressWarnings("unchecked")
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
			queueOperation( new SimpleRemove( value ) );
			return true;
		}
		else {
			return false;
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean containsAll(Collection coll) {
		read();
		return list.containsAll( coll );
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean addAll(Collection<? extends E> values) {
		if ( values.size()==0 ) {
			return false;
		}
		if ( !isOperationQueueEnabled() ) {
			write();
			return list.addAll( values );
		}
		else {
			values.forEach( e -> queueOperation( new SimpleAdd( e ) ) );
			return values.size()>0;
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean addAll(int index, Collection<? extends E> coll) {
		if ( coll.size()>0 ) {
			write();
			return list.addAll( index,  coll );
		}
		else {
			return false;
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean removeAll(Collection coll) {
		if ( coll.size()>0 ) {
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
	@SuppressWarnings("unchecked")
	public boolean retainAll(Collection coll) {
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
	@SuppressWarnings("unchecked")
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
	@SuppressWarnings("unchecked")
	public E get(int index) {
		if ( index < 0 ) {
			throw new ArrayIndexOutOfBoundsException( "negative index" );
		}
		final E result = readElementByIndex( index );
		return result == UNKNOWN ? list.get( index ) : result;
	}

	@Override
	@SuppressWarnings("unchecked")
	public E set(int index, E value) {
		if (index<0) {
			throw new ArrayIndexOutOfBoundsException("negative index");
		}

		final E old = isPutQueueEnabled() ? readElementByIndex( index ) : (E) UNKNOWN;

		if ( old==UNKNOWN ) {
			write();
			return list.set( index, value );
		}
		else {
			queueOperation( new Set( index, value, old ) );
			return old;
		}
	}

	@Override
	@SuppressWarnings("unchecked")
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
			queueOperation( new Remove( index, old ) );
			return (E) old;
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public void add(int index, E value) {
		if ( index < 0 ) {
			throw new ArrayIndexOutOfBoundsException( "negative index" );
		}
		write();
		list.add( index, value );
	}

	@Override
	@SuppressWarnings("unchecked")
	public int indexOf(Object value) {
		read();
		return list.indexOf( value );
	}

	@Override
	@SuppressWarnings("unchecked")
	public int lastIndexOf(Object value) {
		read();
		return list.lastIndexOf( value );
	}

	@Override
	@SuppressWarnings("unchecked")
	public ListIterator listIterator() {
		read();
		return new ListIteratorProxy( list.listIterator() );
	}

	@Override
	@SuppressWarnings("unchecked")
	public ListIterator listIterator(int index) {
		read();
		return new ListIteratorProxy( list.listIterator( index ) );
	}

	@Override
	@SuppressWarnings("unchecked")
	public java.util.List subList(int from, int to) {
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
	@SuppressWarnings("unchecked")
	public Object readFrom(
			ResultSet rs,
			Object owner,
			PersistentCollectionDescriptor collectionDescriptor) throws SQLException {
		throw new NotYetImplementedFor6Exception(  );
//		final Object element = persister.readElement( rs, owner, descriptor.getSuffixedElementAliases(), getSession() ) ;
//		final int index = (Integer) persister.readIndex( rs, descriptor.getSuffixedIndexAliases(), getSession() );
//
//		//pad with nulls from the current last element up to the new index
//		for ( int i = list.size(); i<=index; i++) {
//			list.add( i, null );
//		}
//
//		list.set( index, element );
//		return element;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Iterator<E> entries(PersistentCollectionDescriptor descriptor) {
		return list.iterator();
	}

	@Override
	@SuppressWarnings("unchecked")
	public void initializeFromCache(
			Serializable disassembled,
			Object owner,
			PersistentCollectionDescriptor<?,?,E> collectionDescriptor)
			throws HibernateException {
		final Serializable[] array = (Serializable[]) disassembled;
		final int size = array.length;
		beforeInitialize( size, collectionDescriptor );
		for ( Serializable arrayElement : array ) {
			list.add( getCollectionDescriptor().getElementDescriptor().getJavaTypeDescriptor().getMutabilityPlan().assemble( arrayElement ) );
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public Serializable disassemble(PersistentCollectionDescriptor collectionDescriptor) throws HibernateException {
		final int length = list.size();
		final Serializable[] result = new Serializable[length];
		for ( int i=0; i<length; i++ ) {
			result[i] = getCollectionDescriptor().getElementDescriptor().getJavaTypeDescriptor().getMutabilityPlan().disassemble( list.get( i ) );
		}
		return result;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Iterator getDeletes(PersistentCollectionDescriptor descriptor, boolean indexIsFormula) throws HibernateException {
		final List deletes = new ArrayList();
		final List sn = (List) getSnapshot();
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
	public boolean needsInserting(Object entry, int i) throws HibernateException {
		final List sn = (List) getSnapshot();
		return list.get( i ) != null && ( i >= sn.size() || sn.get( i ) == null );
	}

	@Override
	public boolean needsUpdating(Object entry, int i) throws HibernateException {
		final List sn = (List) getSnapshot();
		return i < sn.size()
				&& sn.get( i ) != null
				&& list.get( i ) != null
				&& getCollectionDescriptor().isDirty( list.get( i ), sn.get( i ), getSession() );
	}

	@Override
	public Object getIndex(
			Object entry,
			int assumedIndex,
			PersistentCollectionDescriptor collectionDescriptor) {
		return assumedIndex;
	}

	@Override
	public Object getElement(
			Object entry,
			PersistentCollectionDescriptor collectionDescriptor) {
		return entry;
	}

	@Override
	public E getSnapshotElement(Object entry, int index) {
		final List<E> sn = (List<E>) getSnapshot();
		return sn.get( index );
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

	public void load(int index, E element) {
		assert isInitializing();
		// todo (6.0) : we need to account for base - but it is not exposed from collection descriptor nor attribute
		list.add( index, element );
	}

	final class Clear implements DelayedOperation {
		@Override
		public void operate() {
			list.clear();
		}

		@Override
		public E getAddedInstance() {
			return null;
		}

		@Override
		public Object getOrphan() {
			throw new UnsupportedOperationException( "queued clear cannot be used with orphan delete" );
		}
	}

	final class SimpleAdd extends AbstractValueDelayedOperation {

		public SimpleAdd(E addedValue) {
			super( addedValue, null );
		}

		@Override
		@SuppressWarnings("unchecked")
		public void operate() {
			list.add( getAddedInstance() );
		}
	}

	abstract class AbstractListValueDelayedOperation extends AbstractValueDelayedOperation {
		private int index;

		AbstractListValueDelayedOperation(Integer index, E addedValue, Object orphan) {
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
		@SuppressWarnings("unchecked")
		public void operate() {
			list.add( getIndex(), getAddedInstance() );
		}
	}

	final class Set extends AbstractListValueDelayedOperation {

		public Set(int index, E addedValue, Object orphan) {
			super( index, addedValue, orphan );
		}

		@Override
		@SuppressWarnings("unchecked")
		public void operate() {
			list.set( getIndex(), getAddedInstance() );
		}
	}

	final class Remove extends AbstractListValueDelayedOperation {

		public Remove(int index, Object orphan) {
			super( index, null, orphan );
		}

		@Override
		@SuppressWarnings("unchecked")
		public void operate() {
			list.remove( getIndex() );
		}
	}

	final class SimpleRemove extends AbstractValueDelayedOperation {

		public SimpleRemove(Object orphan) {
			super( null, orphan );
		}

		@Override
		@SuppressWarnings("unchecked")
		public void operate() {
			list.remove( getOrphan() );
		}
	}
}
