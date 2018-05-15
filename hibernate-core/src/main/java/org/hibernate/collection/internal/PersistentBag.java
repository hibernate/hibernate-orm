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
 * An unordered, unkeyed collection that can contain the same element
 * multiple times. The Java collections API, curiously, has no <tt>Bag</tt>.
 * Most developers seem to use <tt>List</tt>s to represent bag semantics,
 * so Hibernate follows this practice.
 *
 * @author Gavin King
 */
public class PersistentBag<E> extends AbstractPersistentCollection<E> implements List<E> {
	private Object key;
	private List<E> bag;

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
	public PersistentBag(SharedSessionContractImplementor session, PersistentCollectionDescriptor collectionDescriptor) {
		super( session, collectionDescriptor );
	}

	/**
	 * Constructs a PersistentBag
	 *
	 * @param session The session
	 * @param coll The base elements.
	 */
	@SuppressWarnings("unchecked")
	public PersistentBag(
			SharedSessionContractImplementor session,
			PersistentCollectionDescriptor<?,Collection<E>,E> descriptor,
			Collection coll) {
		this( session, descriptor );

		setRawCollection( coll );
	}

	@SuppressWarnings("unchecked")
	private void setRawCollection(Collection coll) {
		if ( coll instanceof List ) {
			bag = (List) coll;
		}
		else {
			bag = new ArrayList();
			bag.addAll( coll );
		}

		setInitialized();
		setDirectlyAccessible( true );
	}

	public PersistentBag(
			SharedSessionContractImplementor session,
			PersistentCollectionDescriptor descriptor,
			Serializable key) {
		this( session, descriptor, (Object) key );
	}

	public PersistentBag(
			SharedSessionContractImplementor session,
			PersistentCollectionDescriptor descriptor,
			Object key) {
		this( session, descriptor );
		this.key = key;
	}

	@Override
	public boolean isWrapper(Object collection) {
		return bag==collection;
	}

	@Override
	public boolean empty() {
		return bag.isEmpty();
	}

	@Override
	public Iterator entries(PersistentCollectionDescriptor descriptor) {
		return bag.iterator();
	}

	@Override
	public Object readFrom(
			ResultSet rs,
			Object owner,
			PersistentCollectionDescriptor collectionDescriptor) throws SQLException {
		throw new NotYetImplementedFor6Exception(  );
		// todo (6.0) : this is done so much differently in this redesign - I think these metods are not going to be needed
//		// note that if we load this collection from a cartesian product
//		// the multiplicity would be broken ... so use an idbag instead
//		final Object element = persister.readElement( rs, owner, descriptor.getSuffixedElementAliases(), getSession() ) ;
//		if ( element != null ) {
//			bag.add( element );
//		}
//		return element;
	}

	@Override
	public void beforeInitialize(
			int anticipatedSize,
			PersistentCollectionDescriptor collectionDescriptor) {
		this.bag = (List) getCollectionDescriptor().instantiateRaw( anticipatedSize );
	}

	@Override
	public boolean equalsSnapshot(PersistentCollectionDescriptor collectionDescriptor) throws HibernateException {
		final List sn = (List) getSnapshot();
		if ( sn.size() != bag.size() ) {
			return false;
		}
		for ( Object elt : bag ) {
			final boolean unequal = countOccurrences( elt, bag ) != countOccurrences( elt, sn );
			if ( unequal ) {
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean isSnapshotEmpty(Serializable snapshot) {
		return ( (Collection) snapshot ).isEmpty();
	}

	@SuppressWarnings("unchecked")
	private int countOccurrences(Object element, List list) {
		final Iterator iter = list.iterator();
		int result = 0;
		while ( iter.hasNext() ) {
			if ( getCollectionDescriptor().getElementDescriptor().getJavaTypeDescriptor().areEqual( (E) element, (E) iter.next() ) ) {
				result++;
			}
		}
		return result;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Serializable getSnapshot(PersistentCollectionDescriptor descriptor)
			throws HibernateException {
		final ArrayList clonedList = new ArrayList( bag.size() );
		for ( Object item : bag ) {
			clonedList.add( getCollectionDescriptor().getElementDescriptor().getJavaTypeDescriptor().getMutabilityPlan().deepCopy( (E) item ) );
		}
		return clonedList;
	}

	@Override
	public Collection getOrphans(Serializable snapshot, String entityName) throws HibernateException {
		final List sn = (List) snapshot;
		return getOrphans( sn, bag, entityName, getSession() );
	}

	@Override
	public Serializable disassemble(PersistentCollectionDescriptor collectionDescriptor)
			throws HibernateException {
		final int length = bag.size();
		final Serializable[] result = new Serializable[length];
		for ( int i=0; i<length; i++ ) {
			result[i] = getCollectionDescriptor().getElementDescriptor().getJavaTypeDescriptor().getMutabilityPlan().disassemble( (E) bag.get( i ) );
		}
		return result;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void initializeFromCache(
			Serializable disassembled,
			Object owner,
			PersistentCollectionDescriptor collectionDescriptor) throws HibernateException {
		throw new NotYetImplementedFor6Exception();
//		final Object[] array = (Object[]) disassembled;
//		final int size = array.length;
//		beforeInitialize( size, collectionDescriptor );
//		for ( Object item : array ) {
//			final E element = getCollectionDescriptor().getElementDescriptor().getJavaTypeDescriptor().getMutabilityPlan().assemble( item );
//			if ( element != null ) {
//				bag.add( element );
//			}
//		}
	}

	@Override
	public boolean needsRecreate(PersistentCollectionDescriptor collectionDescriptor) {
		return !collectionDescriptor.isOneToMany();
	}


	// For a one-to-many, a <bag> is not really a bag;
	// it is *really* a set, since it can't contain the
	// same element twice. It could be considered a bug
	// in the mapping dtd that <bag> allows <one-to-many>.

	// Anyway, here we implement <set> semantics for a
	// <one-to-many> <bag>!

	@Override
	@SuppressWarnings("unchecked")
	public Iterator getDeletes(PersistentCollectionDescriptor descriptor, boolean indexIsFormula) throws HibernateException {
		final ArrayList<E> deletes = new ArrayList<>();

		final List<E> sn = (List<E>) getSnapshot();

		int i=0;
		for ( E old : sn ) {
			boolean found = false;
			if ( bag.size() > i && getCollectionDescriptor().getElementDescriptor().getJavaTypeDescriptor().areEqual( old, bag.get( i++ ) ) ) {
				//a shortcut if its location didn't change!
				found = true;
			}
			else {
				//search for it
				//note that this code is incorrect for other than one-to-many
				for ( E aBag : bag ) {
					if ( getCollectionDescriptor().getElementDescriptor().getJavaTypeDescriptor().areEqual( old, aBag ) ) {
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
	@SuppressWarnings("unchecked")
	public boolean needsInserting(Object entry, int i) throws HibernateException {
		final List<E> sn = (List<E>) getSnapshot();
		if ( sn.size() > i && getCollectionDescriptor().getElementDescriptor().getJavaTypeDescriptor().areEqual( sn.get( i ), (E) entry ) ) {
			//a shortcut if its location didn't change!
			return false;
		}
		else {
			//search for it
			//note that this code is incorrect for other than one-to-many
			for ( E old : sn ) {
				if ( getCollectionDescriptor().getElementDescriptor().getJavaTypeDescriptor().areEqual( old, (E) entry ) ) {
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
	public boolean needsUpdating(Object entry, int i) {
		return false;
	}

	@Override
	public int size() {
		return readSize() ? getCachedSize() : bag.size();
	}

	@Override
	public boolean isEmpty() {
		return readSize() ? getCachedSize()==0 : bag.isEmpty();
	}

	@Override
	public boolean contains(Object object) {
		final Boolean exists = readElementExistence( object );
		return exists == null ? bag.contains( object ) : exists;
	}

	@Override
	public Iterator<E> iterator() {
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
	@SuppressWarnings("unchecked")
	public boolean containsAll(Collection c) {
		read();
		return bag.containsAll( c );
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean addAll(Collection values) {
		if ( values.size()==0 ) {
			return false;
		}
		if ( !isOperationQueueEnabled() ) {
			write();
			return bag.addAll( values );
		}
		else {
			for ( Object value : values ) {
				queueOperation( new SimpleAdd( (E) value ) );
			}
			return values.size()>0;
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean removeAll(Collection c) {
		if ( c.size()>0 ) {
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
			if ( ! bag.isEmpty() ) {
				bag.clear();
				dirty();
			}
		}
	}

	@Override
	public Object getIndex(
			Object entry,
			int assumedIndex,
			PersistentCollectionDescriptor collectionDescriptor) {
		throw new UnsupportedOperationException("Bags don't have indexes");
	}

	@Override
	@SuppressWarnings("unchecked")
	public E getElement(
			Object entry,
			PersistentCollectionDescriptor collectionDescriptor) {
		return (E) entry;
	}

	@Override
	@SuppressWarnings("unchecked")
	public E getSnapshotElement(Object entry, int index) {
		final List sn = (List) getSnapshot();
		return (E) sn.get( index );
	}

	/**
	 * Count how many times the given object occurs in the elements
	 *
	 * @param o The object to check
	 *
	 * @return The number of occurences.
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
	public void add(int i, E o) {
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
	public E get(int i) {
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
	public ListIterator<E> listIterator() {
		read();
		return new ListIteratorProxy( bag.listIterator() );
	}

	@Override
	@SuppressWarnings("unchecked")
	public ListIterator<E> listIterator(int i) {
		read();
		return new ListIteratorProxy( bag.listIterator( i ) );
	}

	@Override
	@SuppressWarnings("unchecked")
	public E remove(int i) {
		write();
		return bag.remove( i );
	}

	@Override
	@SuppressWarnings("unchecked")
	public E set(int i, E o) {
		write();
		return bag.set( i, o );
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<E> subList(int start, int end) {
		read();
		return new ListProxy( bag.subList( start, end ) );
	}

	@Override
	public boolean entryExists(Object entry, int i) {
		return entry!=null;
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
	 * @see java.lang.Object#equals(java.lang.Object)
	 *
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

	public void load(E element) {
		assert isInitializing();
		bag.add( element );
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
			throw new UnsupportedOperationException("queued clear cannot be used with orphan delete");
		}
	}

	final class SimpleAdd extends AbstractValueDelayedOperation {

		public SimpleAdd(E addedValue) {
			super( addedValue, null );
		}

		@Override
		@SuppressWarnings("unchecked")
		public void operate() {
			bag.add( getAddedInstance() );
		}
	}

}
