/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.collection.internal;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;

import org.jboss.logging.Logger;

/**
 * A persistent wrapper for an array. Lazy initialization
 * is NOT supported. Use of Hibernate arrays is not really
 * recommended.
 *
 * @author Gavin King
 */
public class PersistentArrayHolder extends AbstractPersistentCollection {
	private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			CoreMessageLogger.class,
			PersistentArrayHolder.class.getName()
	);

	protected Object array;

	//just to help out during the load (ugly, i know)
	private transient java.util.List tempList;

	/**
	 * Constructs a PersistentCollection instance for holding an array.
	 */
	public PersistentArrayHolder(
			SharedSessionContractImplementor session,
			PersistentCollectionDescriptor collectionDescriptor) {
		super( session, collectionDescriptor );
	}

	/**
	 * Constructs a PersistentCollection instance for holding an array.
	 *
	 * @param session The session
	 * @param array The array (the persistent "collection").
	 */
	public PersistentArrayHolder(
			SharedSessionContractImplementor session,
			PersistentCollectionDescriptor  descriptor,
			Object array) {
		this( session, descriptor );
		setArray( array );
	}

	private void setArray(Object array) {
		this.array = array;
		setInitialized();
	}

	public PersistentArrayHolder(
			Object key,
			SharedSessionContractImplementor session,
			PersistentCollectionDescriptor descriptor) {
		super( session, descriptor, key );
	}

	@Override
	public Serializable getSnapshot(PersistentCollectionDescriptor descriptor) {
		final int length = Array.getLength( array );
		final Serializable result = (Serializable) Array.newInstance( descriptor.getElementDescriptor().getJavaType(), length );
		for ( int i=0; i<length; i++ ) {
			final Object elt = Array.get( array, i );
			try {
				Array.set( result, i, descriptor.getElementDescriptor().getJavaTypeDescriptor().getMutabilityPlan().deepCopy( elt ) );
			}
			catch (IllegalArgumentException iae) {
				LOG.invalidArrayElementType( iae.getMessage() );
				throw new HibernateException( "Array element type error", iae );
			}
		}
		return result;
	}

	@Override
	public boolean isSnapshotEmpty(Serializable snapshot) {
		return Array.getLength( snapshot ) == 0;
	}

	@Override
	public Collection getOrphans(Serializable snapshot, String entityName) throws HibernateException {
		final Object[] sn = (Object[]) snapshot;
		final Object[] arr = (Object[]) array;
		final ArrayList result = new ArrayList();
		Collections.addAll( result, sn );
		for ( int i=0; i<sn.length; i++ ) {
			identityRemove( result, arr[i], entityName, getSession() );
		}
		return result;
	}

	@SuppressWarnings("UnusedDeclaration")
	public Object getArray() {
		return array;
	}

	@Override
	public boolean isWrapper(Object collection) {
		return array==collection;
	}

	@Override
	public boolean equalsSnapshot(PersistentCollectionDescriptor collectionDescriptor) throws HibernateException {
		final Serializable snapshot = getSnapshot();
		final int xlen = Array.getLength( snapshot );
		if ( xlen!= Array.getLength( array ) ) {
			return false;
		}
		for ( int i=0; i<xlen; i++) {
			if ( collectionDescriptor.isDirty( Array.get( snapshot, i ), Array.get( array, i ), getSession() ) ) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Get an iterator over the array elements
	 *
	 * @return The iterator
	 */
	@SuppressWarnings("unchecked")
	public Iterator elements() {
		final int length = Array.getLength( array );
		final java.util.List list = new ArrayList( length );
		for ( int i=0; i<length; i++ ) {
			list.add( Array.get( array, i ) );
		}
		return list.iterator();
	}

	@Override
	public boolean empty() {
		return false;
	}

	@SuppressWarnings("unchecked")
	public void load(int index, Object element) {
		assert isInitializing();

		for ( int i = tempList.size(); i <= index; ++i ) {
			tempList.add( i, null );
		}

		tempList.set( index, element );
	}

	@Override
	@SuppressWarnings("unchecked")
	public Iterator entries(PersistentCollectionDescriptor descriptor) {
		return elements();
	}

	@Override
	public void beginRead() {
		super.beginRead();
		tempList = new ArrayList();
	}

	@Override
	public boolean endRead() {
		setInitialized();

		createArray();

		tempList = null;
		return true;
	}

	private void createArray() {
		array = Array.newInstance(
				getCollectionDescriptor().getElementClass(),
				tempList.size()
		);
		for ( int i = 0; i < tempList.size(); i++ ) {
			Array.set( array, i, tempList.get( i ) );
		}
	}

	@Override
	public void beforeInitialize(
			int anticipatedSize,
			PersistentCollectionDescriptor collectionDescriptor) {
		//if (tempList==null) throw new UnsupportedOperationException("Can't lazily initialize arrays");
	}

	@Override
	public boolean isDirectlyAccessible() {
		return true;
	}

	@Override
	public void initializeFromCache(
			Serializable disassembled,
			Object owner,
			PersistentCollectionDescriptor collectionDescriptor) {
		final Serializable[] cached = (Serializable[]) disassembled;
		array = Array.newInstance( getCollectionDescriptor().getElementDescriptor().getJavaType(), cached.length );

		for ( int i=0; i<cached.length; i++ ) {
			Array.set( array, i, getCollectionDescriptor().getElementDescriptor().getJavaTypeDescriptor().getMutabilityPlan().assemble( cached[i] ) );
		}
	}

	@Override
	public Serializable disassemble(PersistentCollectionDescriptor collectionDescriptor) throws HibernateException {
		final int length = Array.getLength( array );
		final Serializable[] result = new Serializable[length];
		for ( int i=0; i<length; i++ ) {
			result[i] = collectionDescriptor.getElementDescriptor()
					.getJavaTypeDescriptor()
					.getMutabilityPlan()
					.disassemble( Array.get( array,i ) );
		}

		return result;
	}

	@Override
	public Object getValue() {
		if ( tempList != null ) {
			createArray();
		}
		return array;
	}

	@Override
	public Iterator getDeletes(PersistentCollectionDescriptor descriptor, boolean indexIsFormula) throws HibernateException {
		final java.util.List<Integer> deletes = new ArrayList<>();
		final Serializable sn = getSnapshot();
		final int snSize = Array.getLength( sn );
		final int arraySize = Array.getLength( array );
		int end;
		if ( snSize > arraySize ) {
			for ( int i=arraySize; i<snSize; i++ ) {
				deletes.add( i );
			}
			end = arraySize;
		}
		else {
			end = snSize;
		}
		for ( int i=0; i<end; i++ ) {
			if ( Array.get( array, i ) == null && Array.get( sn, i ) != null ) {
				deletes.add( i );
			}
		}
		return deletes.iterator();
	}

	@Override
	public boolean needsInserting(Object entry, int i) throws HibernateException {
		final Serializable sn = getSnapshot();
		return Array.get( array, i ) != null && ( i >= Array.getLength( sn ) || Array.get( sn, i ) == null );
	}

	@Override
	public boolean needsUpdating(Object entry, int i) throws HibernateException {
		final Serializable sn = getSnapshot();
		return i < Array.getLength( sn )
				&& Array.get( sn, i ) != null
				&& Array.get( array, i ) != null
				&& getCollectionDescriptor().isDirty( Array.get( array, i ), Array.get( sn, i ), getSession() );
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
	public Object getSnapshotElement(Object entry, int index) {
		final Serializable sn = getSnapshot();
		return Array.get( sn, index );
	}

	@Override
	public boolean entryExists(Object entry, int i) {
		return entry != null;
	}
}
