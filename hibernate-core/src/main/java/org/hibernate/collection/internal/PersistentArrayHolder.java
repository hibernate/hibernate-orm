/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.collection.internal;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import org.hibernate.HibernateException;
import org.hibernate.NotYetImplementedFor6Exception;
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

	private PersistentCollectionDescriptor descriptor;

	private Serializable key;
	protected Object array;

	//just to help out during the load (ugly, i know)
	private transient java.util.List tempList;

	/**
	 * Constructs a PersistentCollection instance for holding an array.
	 *
	 * @param session The session
	 * @param descriptor The descriptor for the array
	 */
	public PersistentArrayHolder(
			SharedSessionContractImplementor session,
			PersistentCollectionDescriptor descriptor) {
		super( session );
		this.descriptor = descriptor;
	}

	/**
	 * Constructs a PersistentCollection instance for holding an array.
	 *
	 * @param session The session
	 * @param array The array (the persistent "collection").
	 */
	public PersistentArrayHolder(
			SharedSessionContractImplementor session,
			PersistentCollectionDescriptor descriptor,
			Object array) {
		this( session, descriptor );
		setArray( (Object[]) array );
	}

	private void setArray(Object[] array) {
		this.array = array;
		setInitialized();
	}

	public PersistentArrayHolder(
			SharedSessionContractImplementor session,
			PersistentCollectionDescriptor descriptor,
			Serializable key) {
		this( session, descriptor );
		this.key = key;
	}


	@Override
	public Serializable getSnapshot(PersistentCollectionDescriptor persister) throws HibernateException {
//		final int length = (array==null) ? tempList.size() : Array.getLength( array );
		final int length = Array.getLength( array );
		final Serializable result = (Serializable) Array.newInstance( persister.getElementDescriptor().getJavaType(), length );
		for ( int i=0; i<length; i++ ) {
//			final Object elt = (array==null) ? tempList.get( i ) : Array.get( array, i );
			final Object elt = Array.get( array, i );
			try {
				Array.set( result, i, persister.getElementDescriptor().getJavaTypeDescriptor().getMutabilityPlan().deepCopy( elt ) );
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
	public boolean equalsSnapshot(PersistentCollectionDescriptor persister) throws HibernateException {
		throw new NotYetImplementedFor6Exception(  );
//		final Type elementType = getElementType( persister );
//		final Serializable snapshot = getSnapshot();
//		final int xlen = Array.getLength( snapshot );
//		if ( xlen!= Array.getLength( array ) ) {
//			return false;
//		}
//		for ( int i=0; i<xlen; i++) {
//			if ( elementType.isDirty( Array.get( snapshot, i ), Array.get( array, i ), getSession() ) ) {
//				return false;
//			}
//		}
//		return true;
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

	@Override
	@SuppressWarnings("unchecked")
	public Object readFrom(ResultSet rs, PersistentCollectionDescriptor persister, Object owner) throws SQLException {
		throw new NotYetImplementedFor6Exception(  );
//		final Object element = persister.readElement( rs, owner, getSession() );
//		final int index = (Integer) persister.readIndex( rs, getSession() );
//		for ( int i = tempList.size(); i<=index; i++) {
//			tempList.add( i, null );
//		}
//		tempList.set( index, element );
//		return element;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Iterator entries(PersistentCollectionDescriptor persister) {
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

		array = Array.newInstance(
				descriptor.getElementDescriptor().getJavaType(),
				tempList.size()
		);
		for ( int i=0; i<tempList.size(); i++ ) {
			Array.set( array, i, tempList.get( i ) );
		}
		tempList = null;
		return true;
	}

	@Override
	public void beforeInitialize(PersistentCollectionDescriptor persister, int anticipatedSize) {
		//if (tempList==null) throw new UnsupportedOperationException("Can't lazily initialize arrays");
	}

	@Override
	public boolean isDirectlyAccessible() {
		return true;
	}

	@Override
	public void initializeFromCache(PersistentCollectionDescriptor persister, Serializable disassembled, Object owner) {
		final Serializable[] cached = (Serializable[]) disassembled;
		array = Array.newInstance( persister.getElementDescriptor().getJavaType(), cached.length );

		for ( int i=0; i<cached.length; i++ ) {
			Array.set( array, i, persister.getElementDescriptor().getJavaTypeDescriptor().getMutabilityPlan().assemble( cached[i] ) );
		}
	}

	@Override
	public Serializable disassemble(PersistentCollectionDescriptor persister) throws HibernateException {
		final int length = Array.getLength( array );
		final Serializable[] result = new Serializable[length];
		for ( int i=0; i<length; i++ ) {
			result[i] = persister.getElementDescriptor().getJavaTypeDescriptor().getMutabilityPlan().disassemble( Array.get( array,i ) );
		}

		return result;
	}

	@Override
	public Object getValue() {
		return array;
	}

	@Override
	public Iterator getDeletes(PersistentCollectionDescriptor persister, boolean indexIsFormula) throws HibernateException {
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
		throw new NotYetImplementedFor6Exception(  );
//		final Serializable sn = getSnapshot();
//		return i < Array.getLength( sn )
//				&& Array.get( sn, i ) != null
//				&& Array.get( array, i ) != null
//				&& elemType.isDirty( Array.get( array, i ), Array.get( sn, i ), getSession() );
	}

	@Override
	public Object getIndex(Object entry, int i, PersistentCollectionDescriptor persister) {
		return i;
	}

	@Override
	public Object getElement(Object entry) {
		return entry;
	}

	@Override
	public Object getSnapshotElement(Object entry, int i) {
		final Serializable sn = getSnapshot();
		return Array.get( sn, i );
	}

	@Override
	public boolean entryExists(Object entry, int i) {
		return entry != null;
	}
}
