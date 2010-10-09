/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 *
 */
package org.hibernate.collection;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.hibernate.EntityMode;
import org.hibernate.HibernateException;
import org.hibernate.loader.CollectionAliases;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.type.Type;

/**
 * A persistent wrapper for an array. Lazy initialization
 * is NOT supported. Use of Hibernate arrays is not really
 * recommended.
 *
 * @author Gavin King
 */
public class PersistentArrayHolder extends AbstractPersistentCollection {
	protected Object array;

	private static final Logger log = LoggerFactory.getLogger(PersistentArrayHolder.class);

	//just to help out during the load (ugly, i know)
	private transient Class elementClass;
	private transient java.util.List tempList;

	public PersistentArrayHolder(SessionImplementor session, Object array) {
		super(session);
		this.array = array;
		setInitialized();
	}

	public Serializable getSnapshot(CollectionPersister persister) throws HibernateException {
		EntityMode entityMode = getSession().getEntityMode();
		int length = /*(array==null) ? tempList.size() :*/ Array.getLength(array);
		Serializable result = (Serializable) Array.newInstance( persister.getElementClass(), length );
		for ( int i=0; i<length; i++ ) {
			Object elt = /*(array==null) ? tempList.get(i) :*/ Array.get(array, i);
			try {
				Array.set( result, i, persister.getElementType().deepCopy(elt, entityMode, persister.getFactory()) );
			}
			catch (IllegalArgumentException iae) {
				log.error("Array element type error", iae);
				throw new HibernateException( "Array element type error", iae );
			}
		}
		return result;
	}

	public boolean isSnapshotEmpty(Serializable snapshot) {
		return Array.getLength( snapshot ) == 0;
	}

	public Collection getOrphans(Serializable snapshot, String entityName) throws HibernateException {
		Object[] sn = (Object[]) snapshot;
		Object[] arr = (Object[]) array;
		ArrayList result = new ArrayList();
		for (int i=0; i<sn.length; i++) result.add( sn[i] );
		for (int i=0; i<sn.length; i++) identityRemove( result, arr[i], entityName, getSession() );
		return result;
	}

	public PersistentArrayHolder(SessionImplementor session, CollectionPersister persister) throws HibernateException {
		super(session);
		elementClass = persister.getElementClass();
	}

	public Object getArray() {
		return array;
	}

	public boolean isWrapper(Object collection) {
		return array==collection;
	}

	public boolean equalsSnapshot(CollectionPersister persister) throws HibernateException {
		Type elementType = persister.getElementType();
		Serializable snapshot = getSnapshot();
		int xlen = Array.getLength(snapshot);
		if ( xlen!= Array.getLength(array) ) return false;
		for ( int i=0; i<xlen; i++) {
			if ( elementType.isDirty( Array.get(snapshot, i), Array.get(array, i), getSession() ) ) return false;
		}
		return true;
	}

	public Iterator elements() {
		//if (array==null) return tempList.iterator();
		int length = Array.getLength(array);
		java.util.List list = new ArrayList(length);
		for (int i=0; i<length; i++) {
			list.add( Array.get(array, i) );
		}
		return list.iterator();
	}
	public boolean empty() {
		return false;
	}

	public Object readFrom(ResultSet rs, CollectionPersister persister, CollectionAliases descriptor, Object owner)
	throws HibernateException, SQLException {

		Object element = persister.readElement( rs, owner, descriptor.getSuffixedElementAliases(), getSession() );
		int index = ( (Integer) persister.readIndex( rs, descriptor.getSuffixedIndexAliases(), getSession() ) ).intValue();
		for ( int i = tempList.size(); i<=index; i++) {
			tempList.add(i, null);
		}
		tempList.set(index, element);
		return element;
	}

	public Iterator entries(CollectionPersister persister) {
		return elements();
	}

	public void beginRead() {
		super.beginRead();
		tempList = new ArrayList();
	}
	public boolean endRead() {
		setInitialized();
		array = Array.newInstance( elementClass, tempList.size() );
		for ( int i=0; i<tempList.size(); i++) {
			Array.set(array, i, tempList.get(i) );
		}
		tempList=null;
		return true;
	}

	public void beforeInitialize(CollectionPersister persister, int anticipatedSize) {
		//if (tempList==null) throw new UnsupportedOperationException("Can't lazily initialize arrays");
	}

	public boolean isDirectlyAccessible() {
		return true;
	}

	public void initializeFromCache(CollectionPersister persister, Serializable disassembled, Object owner)
	throws HibernateException {
		Serializable[] cached = (Serializable[]) disassembled;

		array = Array.newInstance( persister.getElementClass(), cached.length );

		for ( int i=0; i<cached.length; i++ ) {
			Array.set( array, i, persister.getElementType().assemble( cached[i], getSession(), owner ) );
		}
	}

	public Serializable disassemble(CollectionPersister persister) throws HibernateException {
		int length = Array.getLength(array);
		Serializable[] result = new Serializable[length];
		for ( int i=0; i<length; i++ ) {
			result[i] = persister.getElementType().disassemble( Array.get(array,i), getSession(), null );
		}

		/*int length = tempList.size();
		Serializable[] result = new Serializable[length];
		for ( int i=0; i<length; i++ ) {
			result[i] = persister.getElementType().disassemble( tempList.get(i), session );
		}*/

		return result;

	}

	public Object getValue() {
		return array;
	}

	public Iterator getDeletes(CollectionPersister persister, boolean indexIsFormula) throws HibernateException {
		java.util.List deletes = new ArrayList();
		Serializable sn = getSnapshot();
		int snSize = Array.getLength(sn);
		int arraySize = Array.getLength(array);
		int end;
		if ( snSize > arraySize ) {
			for ( int i=arraySize; i<snSize; i++ ) deletes.add( new Integer(i) );
			end = arraySize;
		}
		else {
			end = snSize;
		}
		for ( int i=0; i<end; i++ ) {
			if ( Array.get(array, i)==null && Array.get(sn, i)!=null ) deletes.add( new Integer(i) );
		}
		return deletes.iterator();
	}

	public boolean needsInserting(Object entry, int i, Type elemType) throws HibernateException {
		Serializable sn = getSnapshot();
		return Array.get(array, i)!=null && ( i >= Array.getLength(sn) || Array.get(sn, i)==null );
	}

	public boolean needsUpdating(Object entry, int i, Type elemType) throws HibernateException {
		Serializable sn = getSnapshot();
		return i<Array.getLength(sn) &&
				Array.get(sn, i)!=null &&
				Array.get(array, i)!=null &&
				elemType.isDirty( Array.get(array, i), Array.get(sn, i), getSession() );
	}

	public Object getIndex(Object entry, int i, CollectionPersister persister) {
		return new Integer(i);
	}

	public Object getElement(Object entry) {
		return entry;
	}

	public Object getSnapshotElement(Object entry, int i) {
		Serializable sn = getSnapshot();
		return Array.get(sn, i);
	}

	public boolean entryExists(Object entry, int i) {
		return entry!=null;
	}

}
