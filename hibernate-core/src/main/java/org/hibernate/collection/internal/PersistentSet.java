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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;


/**
 * A persistent wrapper for a <tt>java.util.Set</tt>. The underlying
 * collection is a <tt>HashSet</tt>.
 *
 * @see java.util.HashSet
 * @author Gavin King
 */
public class PersistentSet<E> extends AbstractPersistentCollection<E> implements java.util.Set<E> {
	protected Set set;
	protected transient List tempList;

	protected PersistentSet(
			SharedSessionContractImplementor session,
			PersistentCollectionDescriptor<?,?,E> descriptor) {
		super( session, descriptor );
	}


	/**
	 * Instantiates a non-lazy set (the underlying set is constructed
	 * from the incoming set reference).
	 *
	 * @param session The session to which this set will belong.
	 * @param set The underlying set data.
	 */
	public PersistentSet(
			SharedSessionContractImplementor session,
			PersistentCollectionDescriptor<?,?,E> descriptor,
			java.util.Set set) {
		this( session, descriptor );

		// Sets can be just a view of a part of another collection.
		// do we need to copy it to be sure it won't be changing
		// underneath us?
		// ie. this.set.addAll(set);
		setSet( set );

		setDirectlyAccessible( true );
	}

	private void setSet(Set set) {
		this.set = set;
		setInitialized();
	}

	public PersistentSet(
			SharedSessionContractImplementor session,
			PersistentCollectionDescriptor<?,?,E> descriptor,
			Object key) {
		super( session, descriptor, key );

	}


	@Override
	@SuppressWarnings( {"unchecked"})
	public Serializable getSnapshot(PersistentCollectionDescriptor descriptor) throws HibernateException {
		final HashMap clonedSet = new HashMap( set.size() );
		for ( Object aSet : set ) {
			final Object copied = descriptor.getMutabilityPlan().deepCopy( aSet );
			clonedSet.put( copied, copied );
		}
		return clonedSet;
	}

	@Override
	public Collection getOrphans(Serializable snapshot, String entityName) throws HibernateException {
		final java.util.Map sn = (java.util.Map) snapshot;
		return getOrphans( sn.keySet(), set, entityName, getSession() );
	}

	@Override
	public boolean equalsSnapshot(PersistentCollectionDescriptor<?,?,E> collectionDescriptor) throws HibernateException {
		final java.util.Map sn = (java.util.Map) getSnapshot();
		if ( sn.size()!=set.size() ) {
			return false;
		}
		else {
			for ( Object test : set ) {
				final Object oldValue = sn.get( test );
				if ( oldValue == null || collectionDescriptor.isDirty( oldValue, test, getSession() ) ) {
					return false;
				}
			}
			return true;
		}
	}

	@Override
	public boolean isSnapshotEmpty(Serializable snapshot) {
		return ( (java.util.Map) snapshot ).isEmpty();
	}

	@Override
	public void beforeInitialize(
			int anticipatedSize,
			PersistentCollectionDescriptor collectionDescriptor) {
		this.set = (Set) getCollectionDescriptor().instantiateRaw( anticipatedSize );
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
			final Object assembledArrayElement = getCollectionDescriptor().getElementDescriptor()
					.getJavaTypeDescriptor()
					.getMutabilityPlan()
					.assemble( arrayElement );
			if ( assembledArrayElement != null ) {
				set.add( assembledArrayElement );
			}
		}
	}

	@Override
	public boolean empty() {
		return set.isEmpty();
	}

	@Override
	@SuppressWarnings("unchecked")
	public int size() {
		return readSize() ? getCachedSize() : set.size();
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean isEmpty() {
		return readSize() ? getCachedSize()==0 : set.isEmpty();
	}

	@Override
	public boolean contains(Object object) {
		final Boolean exists = readElementExistence( object );
		return exists == null
				? set.contains( object )
				: exists;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Iterator iterator() {
		read();
		return new IteratorProxy( set.iterator() );
	}

	@Override
	@SuppressWarnings("unchecked")
	public Object[] toArray() {
		read();
		return set.toArray();
	}

	@Override
	@SuppressWarnings("unchecked")
	public Object[] toArray(Object[] array) {
		read();
		return set.toArray( array );
	}

	@Override
	public boolean add(E value) {
		final Boolean exists = isOperationQueueEnabled() ? readElementExistence( value ) : null;
		if ( exists == null ) {
			initialize( true );
			if ( set.add( value ) ) {
				dirty();
				return true;
			}
			else {
				return false;
			}
		}
		else if ( exists ) {
			return false;
		}
		else {
			queueOperation( new SimpleAdd( value ) );
			return true;
		}
	}

	@Override
	public boolean remove(Object value) {
		final Boolean exists = isPutQueueEnabled() ? readElementExistence( value ) : null;
		if ( exists == null ) {
			initialize( true );
			if ( set.remove( value ) ) {
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
		return set.containsAll( coll );
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean addAll(Collection coll) {
		if ( coll.size() > 0 ) {
			initialize( true );
			if ( set.addAll( coll ) ) {
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
		if ( set.retainAll( coll ) ) {
			dirty();
			return true;
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
			if ( set.removeAll( coll ) ) {
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
	public void clear() {
		if ( isClearQueueEnabled() ) {
			queueOperation( new Clear() );
		}
		else {
			initialize( true );
			if ( !set.isEmpty() ) {
				set.clear();
				dirty();
			}
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public String toString() {
		read();
		return set.toString();
	}

	@Override
	@SuppressWarnings("unchecked")
	public Object readFrom(
			ResultSet rs,
			Object owner, PersistentCollectionDescriptor collectionDescriptor) throws SQLException {
		throw new NotYetImplementedFor6Exception(  );
//		final Object element = persister.readElement( rs, owner, descriptor.getSuffixedElementAliases(), getSession() );
//		if ( element != null ) {
//			tempList.add( element );
//		}
//		return element;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void beginRead() {
		super.beginRead();
		tempList = new ArrayList();
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean endRead() {
		set.addAll( tempList );
		tempList = null;
		// ensure that operationQueue is considered
		return super.endRead();
	}

	@Override
	@SuppressWarnings("unchecked")
	public Iterator entries(PersistentCollectionDescriptor descriptor) {
		return set.iterator();
	}

	@Override
	@SuppressWarnings("unchecked")
	public Serializable disassemble(PersistentCollectionDescriptor collectionDescriptor) throws HibernateException {
		final Serializable[] result = new Serializable[ set.size() ];
		final Iterator itr = set.iterator();
		int i=0;
		while ( itr.hasNext() ) {
			result[i++] = collectionDescriptor.getElementDescriptor().getJavaTypeDescriptor().getMutabilityPlan().disassemble( itr.next() );
		}
		return result;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Iterator getDeletes(PersistentCollectionDescriptor descriptor, boolean indexIsFormula) throws HibernateException {
		final java.util.Map sn = (java.util.Map) getSnapshot();
		final ArrayList deletes = new ArrayList( sn.size() );

		Iterator itr = sn.keySet().iterator();
		while ( itr.hasNext() ) {
			final Object test = itr.next();
			if ( !set.contains( test ) ) {
				// the element has been removed from the set
				deletes.add( test );
			}
		}

		itr = set.iterator();
		while ( itr.hasNext() ) {
			final Object test = itr.next();
			final Object oldValue = sn.get( test );
			if ( oldValue!=null && descriptor.isDirty( test, oldValue, getSession() ) ) {
				// the element has changed
				deletes.add( oldValue );
			}
		}

		return deletes.iterator();
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean needsInserting(Object entry, int i) throws HibernateException {
		final Object oldValue = ( (java.util.Map) getSnapshot() ).get( entry );
		// note that it might be better to iterate the snapshot but this is safe,
		// assuming the user implements equals() properly, as required by the Set
		// contract!
		return ( oldValue == null && entry != null ) || getCollectionDescriptor().isDirty( oldValue, entry, getSession() );
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean needsUpdating(Object entry, int i) {
		return false;
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean isRowUpdatePossible() {
		return false;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Object getIndex(
			Object entry,
			int assumedIndex,
			PersistentCollectionDescriptor collectionDescriptor) {
		throw new UnsupportedOperationException("Sets don't have indexes");
	}

	@Override
	@SuppressWarnings("unchecked")
	public Object getElement(
			Object entry,
			PersistentCollectionDescriptor collectionDescriptor) {
		return entry;
	}

	@Override
	@SuppressWarnings("unchecked")
	public E getSnapshotElement(Object entry, int index) {
		throw new UnsupportedOperationException("Sets don't support updating by element");
	}

	@Override
	@SuppressWarnings({"unchecked", "EqualsWhichDoesntCheckParameterClass"})
	public boolean equals(Object other) {
		read();
		return set.equals( other );
	}

	@Override
	@SuppressWarnings("unchecked")
	public int hashCode() {
		read();
		return set.hashCode();
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean entryExists(Object key, int i) {
		return key != null;
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean isWrapper(Object collection) {
		return set==collection;
	}

	@SuppressWarnings("unchecked")
	public void load(E element) {
		assert isInitializing();
		tempList.add( element );
	}

	final class Clear implements DelayedOperation {
		@Override
		public void operate() {
			set.clear();
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
			set.add( getAddedInstance() );
		}
	}

	final class SimpleRemove extends AbstractValueDelayedOperation {

		public SimpleRemove(Object orphan) {
			super( null, orphan );
		}

		@Override
		@SuppressWarnings("unchecked")
		public void operate() {
			set.remove( getOrphan() );
		}
	}
}
