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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.hibernate.EntityMode;
import org.hibernate.HibernateException;
import org.hibernate.loader.CollectionAliases;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.type.Type;

/**
 * An <tt>IdentifierBag</tt> implements "bag" semantics more efficiently than
 * a regular <tt>Bag</tt> by adding a synthetic identifier column to the
 * table. This identifier is unique for all rows in the table, allowing very
 * efficient updates and deletes. The value of the identifier is never exposed
 * to the application.<br>
 * <br>
 * <tt>IdentifierBag</tt>s may not be used for a many-to-one association.
 * Furthermore, there is no reason to use <tt>inverse="true"</tt>.
 *
 * @author Gavin King
 */
public class PersistentIdentifierBag extends AbstractPersistentCollection implements List {

	protected List values; //element
	protected Map identifiers; //index -> id

	public PersistentIdentifierBag(SessionImplementor session) {
		super(session);
	}

	public PersistentIdentifierBag() {} //needed for SOAP libraries, etc

	public PersistentIdentifierBag(SessionImplementor session, Collection coll) {
		super(session);
		if (coll instanceof List) {
			values = (List) coll;
		}
		else {
			values = new ArrayList();
			Iterator iter = coll.iterator();
			while ( iter.hasNext() ) {
				values.add( iter.next() );
			}
		}
		setInitialized();
		setDirectlyAccessible(true);
		identifiers = new HashMap();
	}

	public void initializeFromCache(CollectionPersister persister, Serializable disassembled, Object owner)
	throws HibernateException {
		Serializable[] array = (Serializable[]) disassembled;
		int size = array.length;
		beforeInitialize( persister, size );
		for ( int i = 0; i < size; i+=2 ) {
			identifiers.put(
				new Integer(i/2),
				persister.getIdentifierType().assemble( array[i], getSession(), owner )
			);
			values.add( persister.getElementType().assemble( array[i+1], getSession(), owner ) );
		}
	}

	public Object getIdentifier(Object entry, int i) {
		return identifiers.get( new Integer(i) );
	}

	public boolean isWrapper(Object collection) {
		return values==collection;
	}

	public boolean add(Object o) {
		write();
		values.add(o);
		return true;
	}

	public void clear() {
		initialize( true );
		if ( ! values.isEmpty() || ! identifiers.isEmpty() ) {
			values.clear();
			identifiers.clear();
			dirty();
		}
	}

	public boolean contains(Object o) {
		read();
		return values.contains(o);
	}

	public boolean containsAll(Collection c) {
		read();
		return values.containsAll(c);
	}

	public boolean isEmpty() {
		return readSize() ? getCachedSize()==0 : values.isEmpty();
	}

	public Iterator iterator() {
		read();
		return new IteratorProxy( values.iterator() );
	}

	public boolean remove(Object o) {
		initialize( true );
		int index = values.indexOf(o);
		if (index>=0) {
			beforeRemove(index);
			values.remove(index);
			dirty();
			return true;
		}
		else {
			return false;
		}
	}

	public boolean removeAll(Collection c) {
		if ( c.size() > 0 ) {
			boolean result = false;
			Iterator iter = c.iterator();
			while ( iter.hasNext() ) {
				if ( remove( iter.next() ) ) result=true;
			}
			return result;
		}
		else {
			return false;
		}
	}

	public boolean retainAll(Collection c) {
		initialize( true );
		if ( values.retainAll( c ) ) {
			dirty();
			return true;
		}
		else {
			return false;
		}
	}

	public int size() {
		return readSize() ? getCachedSize() : values.size();
	}

	public Object[] toArray() {
		read();
		return values.toArray();
	}

	public Object[] toArray(Object[] a) {
		read();
		return values.toArray(a);
	}

	public void beforeInitialize(CollectionPersister persister, int anticipatedSize) {
		identifiers = anticipatedSize <= 0 ? new HashMap() : new HashMap( anticipatedSize + 1 + (int)( anticipatedSize * .75f ), .75f );
		values = anticipatedSize <= 0 ? new ArrayList() : new ArrayList( anticipatedSize );
	}

	public Serializable disassemble(CollectionPersister persister)
			throws HibernateException {
		Serializable[] result = new Serializable[ values.size() * 2 ];
		int i=0;
		for (int j=0; j< values.size(); j++) {
			Object value = values.get(j);
			result[i++] = persister.getIdentifierType().disassemble( identifiers.get( new Integer(j) ), getSession(), null );
			result[i++] = persister.getElementType().disassemble( value, getSession(), null );
		}
		return result;
	}

	public boolean empty() {
		return values.isEmpty();
	}

	public Iterator entries(CollectionPersister persister) {
		return values.iterator();
	}

	public boolean entryExists(Object entry, int i) {
		return entry!=null;
	}

	public boolean equalsSnapshot(CollectionPersister persister) throws HibernateException {
		Type elementType = persister.getElementType();
		Map snap = (Map) getSnapshot();
		if ( snap.size()!= values.size() ) return false;
		for ( int i=0; i<values.size(); i++ ) {
			Object value = values.get(i);
			Object id = identifiers.get( new Integer(i) );
			if (id==null) return false;
			Object old = snap.get(id);
			if ( elementType.isDirty( old, value, getSession() ) ) return false;
		}
		return true;
	}

	public boolean isSnapshotEmpty(Serializable snapshot) {
		return ( (Map) snapshot ).isEmpty();
	}

	public Iterator getDeletes(CollectionPersister persister, boolean indexIsFormula) throws HibernateException {
		Map snap = (Map) getSnapshot();
		List deletes = new ArrayList( snap.keySet() );
		for ( int i=0; i<values.size(); i++ ) {
			if ( values.get(i)!=null ) deletes.remove( identifiers.get( new Integer(i) ) );
		}
		return deletes.iterator();
	}

	public Object getIndex(Object entry, int i, CollectionPersister persister) {
		throw new UnsupportedOperationException("Bags don't have indexes");
	}

	public Object getElement(Object entry) {
		return entry;
	}

	public Object getSnapshotElement(Object entry, int i) {
		Map snap = (Map) getSnapshot();
		Object id = identifiers.get( new Integer(i) );
		return snap.get(id);
	}

	public boolean needsInserting(Object entry, int i, Type elemType)
		throws HibernateException {

		Map snap = (Map) getSnapshot();
		Object id = identifiers.get( new Integer(i) );
		return entry!=null && ( id==null || snap.get(id)==null );
	}

	public boolean needsUpdating(Object entry, int i, Type elemType) throws HibernateException {

		if (entry==null) return false;
		Map snap = (Map) getSnapshot();
		Object id = identifiers.get( new Integer(i) );
		if (id==null) return false;
		Object old = snap.get(id);
		return old!=null && elemType.isDirty( old, entry, getSession() );
	}


	public Object readFrom(
		ResultSet rs,
		CollectionPersister persister,
		CollectionAliases descriptor,
		Object owner)
		throws HibernateException, SQLException {

		Object element = persister.readElement( rs, owner, descriptor.getSuffixedElementAliases(), getSession() );
		Object old = identifiers.put(
			new Integer( values.size() ),
			persister.readIdentifier( rs, descriptor.getSuffixedIdentifierAlias(), getSession() )
		);
		if ( old==null ) values.add(element); //maintain correct duplication if loaded in a cartesian product
		return element;
	}

	public Serializable getSnapshot(CollectionPersister persister)
		throws HibernateException {

		EntityMode entityMode = getSession().getEntityMode();

		HashMap map = new HashMap( values.size() );
		Iterator iter = values.iterator();
		int i=0;
		while ( iter.hasNext() ) {
			Object value = iter.next();
			map.put(
				identifiers.get( new Integer(i++) ),
				persister.getElementType().deepCopy(value, entityMode, persister.getFactory())
			);
		}
		return map;
	}

	public Collection getOrphans(Serializable snapshot, String entityName) throws HibernateException {
		Map sn = (Map) snapshot;
		return getOrphans( sn.values(), values, entityName, getSession() );
	}

	public void preInsert(CollectionPersister persister) throws HibernateException {
		Iterator iter = values.iterator();
		int i=0;
		while ( iter.hasNext() ) {
			Object entry = iter.next();
			Integer loc = new Integer(i++);
			if ( !identifiers.containsKey(loc) ) { //TODO: native ids
				Serializable id = persister.getIdentifierGenerator().generate( getSession(), entry );
				identifiers.put(loc, id);
			}
		}
	}

	public void add(int index, Object element) {
		write();
		beforeAdd(index);
		values.add(index, element);
	}

	public boolean addAll(int index, Collection c) {
		if ( c.size() > 0 ) {
			Iterator iter = c.iterator();
			while ( iter.hasNext() ) {
				add( index++, iter.next() );
			}
			return true;
		}
		else {
			return false;
		}
	}

	public Object get(int index) {
		read();
		return values.get(index);
	}

	public int indexOf(Object o) {
		read();
		return values.indexOf(o);
	}

	public int lastIndexOf(Object o) {
		read();
		return values.lastIndexOf(o);
	}

	public ListIterator listIterator() {
		read();
		return new ListIteratorProxy( values.listIterator() );
	}

	public ListIterator listIterator(int index) {
		read();
		return new ListIteratorProxy( values.listIterator(index) );
	}

	private void beforeRemove(int index) {
		Object removedId = identifiers.get( new Integer(index) );
		int last = values.size()-1;
		for ( int i=index; i<last; i++ ) {
			Object id = identifiers.get( new Integer(i+1) );
	        if ( id==null ) {
				identifiers.remove( new Integer(i) );
	        }
	        else {
				identifiers.put( new Integer(i), id );
	        }
		}
		identifiers.put( new Integer(last), removedId );
	}

	private void beforeAdd(int index) {
		for ( int i=index; i<values.size(); i++ ) {
			identifiers.put( new Integer(i+1), identifiers.get( new Integer(i) ) );
		}
		identifiers.remove( new Integer(index) );
	}

	public Object remove(int index) {
		write();
		beforeRemove(index);
		return values.remove(index);
	}

	public Object set(int index, Object element) {
		write();
		return values.set(index, element);
	}

	public List subList(int fromIndex, int toIndex) {
		read();
		return new ListProxy( values.subList(fromIndex, toIndex) );
	}

	public boolean addAll(Collection c) {
		if ( c.size()> 0 ) {
			write();
			return values.addAll(c);
		}
		else {
			return false;
		}
	}

	public void afterRowInsert(
		CollectionPersister persister,
		Object entry,
		int i)
		throws HibernateException {
		//TODO: if we are using identity columns, fetch the identifier
	}

}
