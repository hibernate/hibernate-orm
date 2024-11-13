/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.collection.spi;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.Incubating;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.type.Type;

/**
 * A dummy collection wrapper for an array. Lazy initialization is
 * <em>not</em> supported. The use of arrays to represent persistent
 * collections in Hibernate is discouraged.
 *
 * @apiNote Incubating in terms of making this non-internal.
 *          These contracts will be getting cleaned up in following
 *          releases.
 *
 * @author Gavin King
 */
@Incubating
public class PersistentArrayHolder<E> extends AbstractPersistentCollection<E> {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( PersistentArrayHolder.class );

	protected Object array;

	//just to help out during the load (ugly, i know)
	private transient Class<?> elementClass;

	/**
	 * Constructs a PersistentCollection instance for holding an array.
	 *
	 * @param session The session
	 * @param array The array (the persistent "collection").
	 */
	public PersistentArrayHolder(SharedSessionContractImplementor session, Object array) {
		super( session );
		this.array = array;
		setInitialized();
	}

	/**
	 * Constructs a PersistentCollection instance for holding an array.
	 *
	 * @param session The session
	 * @param persister The persister for the array
	 */
	public PersistentArrayHolder(SharedSessionContractImplementor session, CollectionPersister persister) {
		super( session );
		elementClass = persister.getElementClass();
	}

	@Override
	public Serializable getSnapshot(CollectionPersister persister) throws HibernateException {
//		final int length = (array==null) ? tempList.size() : Array.getLength( array );
		final int length = Array.getLength( array );
		final Serializable result = (Serializable) Array.newInstance( persister.getElementClass(), length );
		for ( int i=0; i<length; i++ ) {
//			final Object elt = (array==null) ? tempList.get( i ) : Array.get( array, i );
			final Object elt = Array.get( array, i );
			try {
				Array.set( result, i, persister.getElementType().deepCopy( elt, persister.getFactory() ) );
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
		if ( arr.length == 0 ) {
			return Arrays.asList( sn );
		}
		final ArrayList result = new ArrayList();
		Collections.addAll( result, sn );
		for ( int i=0; i<sn.length; i++ ) {
			identityRemove( result, arr[i], entityName, getSession() );
		}
		return result;
	}

	@Override
	public void initializeEmptyCollection(CollectionPersister persister) {
		assert array == null;
		array = Array.newInstance( persister.getElementClass(), 0 );
		persister.getAttributeMapping().getPropertyAccess().getSetter().set( getOwner(), array );
		endRead();
	}

	@Override
	public void injectLoadedState(PluralAttributeMapping attributeMapping, List loadingState) {
		assert isInitializing();
		if ( loadingState == null ) {
			array = Array.newInstance( elementClass, 0 );
		}
		else {
			array = Array.newInstance( elementClass, loadingState.size() );
			for ( int i = 0; i < loadingState.size(); i++ ) {
				Array.set( array, i, loadingState.get( i ) );
			}
		}
		attributeMapping.getPropertyAccess().getSetter().set( getOwner(), array );
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
	public boolean equalsSnapshot(CollectionPersister persister) throws HibernateException {
		final Type elementType = persister.getElementType();
		final Serializable snapshot = getSnapshot();
		final int xlen = Array.getLength( snapshot );
		if ( xlen!= Array.getLength( array ) ) {
			return false;
		}
		for ( int i=0; i<xlen; i++) {
			if ( elementType.isDirty( Array.get( snapshot, i ), Array.get( array, i ), getSession() ) ) {
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
		final List list = new ArrayList( length );
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
	public Iterator entries(CollectionPersister persister) {
		return elements();
	}

	@Override
	public boolean endRead() {
		setInitialized();
		return true;
	}

	@Override
	public boolean isDirectlyAccessible() {
		return true;
	}

	@Override
	public void initializeFromCache(CollectionPersister persister, Object disassembled, Object owner)
			throws HibernateException {
		final Serializable[] cached = (Serializable[]) disassembled;
		array = Array.newInstance( persister.getElementClass(), cached.length );

		for ( int i=0; i<cached.length; i++ ) {
			Array.set( array, i, persister.getElementType().assemble( cached[i], getSession(), owner ) );
		}
	}

	@Override
	public Object disassemble(CollectionPersister persister) throws HibernateException {
		final int length = Array.getLength( array );
		final Serializable[] result = new Serializable[length];
		for ( int i=0; i<length; i++ ) {
			result[i] = persister.getElementType().disassemble( Array.get( array,i ), getSession(), null );
		}

		return result;
	}

	@Override
	public Object getValue() {
		return array;
	}

	@Override
	public Iterator getDeletes(CollectionPersister persister, boolean indexIsFormula) throws HibernateException {
		final List<Integer> deletes = new ArrayList<>();
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
	public boolean needsInserting(Object entry, int i, Type elemType) throws HibernateException {
		final Serializable sn = getSnapshot();
		return Array.get( array, i ) != null && ( i >= Array.getLength( sn ) || Array.get( sn, i ) == null );
	}

	@Override
	public boolean needsUpdating(Object entry, int i, Type elemType) throws HibernateException {
		final Serializable sn = getSnapshot();
		return i < Array.getLength( sn )
				&& Array.get( sn, i ) != null
				&& Array.get( array, i ) != null
				&& elemType.isDirty( Array.get( array, i ), Array.get( sn, i ), getSession() );
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
		final Serializable sn = getSnapshot();
		return Array.get( sn, i );
	}

	@Override
	public boolean entryExists(Object entry, int i) {
		return entry != null;
	}
}
