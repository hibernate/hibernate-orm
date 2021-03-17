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
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.loader.CollectionAliases;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.type.Type;

/**
 * A persistent wrapper for a <tt>java.util.List</tt>. Underlying
 * collection is an <tt>ArrayList</tt>.
 *
 * @see java.util.ArrayList
 * @author Gavin King
 */
public class PersistentList extends AbstractPersistentCollection implements List {
	protected List list;

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
	 * @deprecated {@link #PersistentList(SharedSessionContractImplementor)} should be used instead.
	 */
	@Deprecated
	public PersistentList(SessionImplementor session) {
		this( (SharedSessionContractImplementor) session );
	}

	/**
	 * Constructs a PersistentList.
	 *
	 * @param session The session
	 * @param list The raw list
	 */
	public PersistentList(SharedSessionContractImplementor session, List list) {
		super( session );
		this.list = list;
		setInitialized();
		setDirectlyAccessible( true );
	}

	/**
	 * Constructs a PersistentList.
	 *
	 * @param session The session
	 * @param list The raw list
	 * @deprecated {@link #PersistentList(SharedSessionContractImplementor, List)} should be used instead.
	 */
	@Deprecated
	public PersistentList(SessionImplementor session, List list) {
		this( (SharedSessionContractImplementor) session, list );
	}

	@Override
	@SuppressWarnings( {"unchecked"})
	public Serializable getSnapshot(CollectionPersister persister) throws HibernateException {
		final ArrayList clonedList = new ArrayList( list.size() );
		for ( Object element : list ) {
			final Object deepCopy = persister.getElementType().deepCopy( element, persister.getFactory() );
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
	public boolean equalsSnapshot(CollectionPersister persister) throws HibernateException {
		final Type elementType = persister.getElementType();
		final List sn = (List) getSnapshot();
		if ( sn.size() != this.list.size() ) {
			return false;
		}
		final Iterator itr = list.iterator();
		final Iterator snapshotItr = sn.iterator();
		while ( itr.hasNext() ) {
			if ( elementType.isDirty( itr.next(), snapshotItr.next(), getSession() ) ) {
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
	public void beforeInitialize(CollectionPersister persister, int anticipatedSize) {
		this.list = (List) persister.getCollectionType().instantiate( anticipatedSize );
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
	public Iterator iterator() {
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
	public boolean add(Object object) {
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
	public boolean addAll(Collection values) {
		if ( values.size() == 0 ) {
			return false;
		}
		if ( !isOperationQueueEnabled() ) {
			write();
			return list.addAll( values );
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
	public boolean addAll(int index, Collection coll) {
		if ( coll.size() > 0 ) {
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
	public Object get(int index) {
		if ( index < 0 ) {
			throw new ArrayIndexOutOfBoundsException( "negative index" );
		}
		final Object result = readElementByIndex( index );
		return result == UNKNOWN ? list.get( index ) : result;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Object set(int index, Object value) {
		if (index<0) {
			throw new ArrayIndexOutOfBoundsException("negative index");
		}

		final Object old = isPutQueueEnabled() ? readElementByIndex( index ) : UNKNOWN;

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
	public Object remove(int index) {
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
			return old;
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public void add(int index, Object value) {
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
	public Object readFrom(ResultSet rs, CollectionPersister persister, CollectionAliases descriptor, Object owner)
			throws HibernateException, SQLException {
		final Object element = persister.readElement( rs, owner, descriptor.getSuffixedElementAliases(), getSession() ) ;
		final int index = (Integer) persister.readIndex( rs, descriptor.getSuffixedIndexAliases(), getSession() );

		//pad with nulls from the current last element up to the new index
		for ( int i = list.size(); i<=index; i++) {
			list.add( i, null );
		}

		list.set( index, element );
		return element;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Iterator entries(CollectionPersister persister) {
		return list.iterator();
	}

	@Override
	@SuppressWarnings("unchecked")
	public void initializeFromCache(CollectionPersister persister, Serializable disassembled, Object owner)
			throws HibernateException {
		final Serializable[] array = (Serializable[]) disassembled;
		final int size = array.length;
		beforeInitialize( persister, size );
		for ( Serializable arrayElement : array ) {
			list.add( persister.getElementType().assemble( arrayElement, getSession(), owner ) );
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public Serializable disassemble(CollectionPersister persister) throws HibernateException {
		final int length = list.size();
		final Serializable[] result = new Serializable[length];
		for ( int i=0; i<length; i++ ) {
			result[i] = persister.getElementType().disassemble( list.get( i ), getSession(), null );
		}
		return result;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Iterator getDeletes(CollectionPersister persister, boolean indexIsFormula) throws HibernateException {
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
	public boolean needsInserting(Object entry, int i, Type elemType) throws HibernateException {
		final List sn = (List) getSnapshot();
		return list.get( i ) != null && ( i >= sn.size() || sn.get( i ) == null );
	}

	@Override
	public boolean needsUpdating(Object entry, int i, Type elemType) throws HibernateException {
		final List sn = (List) getSnapshot();
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
		final List sn = (List) getSnapshot();
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

	final class Clear implements DelayedOperation {
		@Override
		public void operate() {
			list.clear();
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
			list.add( getAddedInstance() );
		}
	}

	abstract class AbstractListValueDelayedOperation extends AbstractValueDelayedOperation {
		private int index;

		AbstractListValueDelayedOperation(Integer index, Object addedValue, Object orphan) {
			super( addedValue, orphan );
			this.index = index;
		}

		protected final int getIndex() {
			return index;
		}
	}

	final class Add extends AbstractListValueDelayedOperation {

		public Add(int index, Object addedValue) {
			super( index, addedValue, null );
		}

		@Override
		@SuppressWarnings("unchecked")
		public void operate() {
			list.add( getIndex(), getAddedInstance() );
		}
	}

	final class Set extends AbstractListValueDelayedOperation {

		public Set(int index, Object addedValue, Object orphan) {
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
