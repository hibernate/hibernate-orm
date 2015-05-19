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
import java.util.ListIterator;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.loader.CollectionAliases;
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
	protected List<Object> values;
	protected Map<Integer, Object> identifiers;

	/**
	 * Constructs a PersistentIdentifierBag.  This form needed for SOAP libraries, etc
	 */
	@SuppressWarnings("UnusedDeclaration")
	public PersistentIdentifierBag() {
	}

	/**
	 * Constructs a PersistentIdentifierBag.
	 *
	 * @param session The session
	 */
	public PersistentIdentifierBag(SessionImplementor session) {
		super( session );
	}

	/**
	 * Constructs a PersistentIdentifierBag.
	 *
	 * @param session The session
	 * @param coll The base elements
	 */
	@SuppressWarnings("unchecked")
	public PersistentIdentifierBag(SessionImplementor session, Collection coll) {
		super( session );
		if (coll instanceof List) {
			values = (List<Object>) coll;
		}
		else {
			values = new ArrayList<Object>();
			for ( Object element : coll ) {
				values.add( element );
			}
		}
		setInitialized();
		setDirectlyAccessible( true );
		identifiers = new HashMap<Integer, Object>();
	}

	@Override
	public void initializeFromCache(CollectionPersister persister, Serializable disassembled, Object owner)
			throws HibernateException {
		final Serializable[] array = (Serializable[]) disassembled;
		final int size = array.length;
		beforeInitialize( persister, size );
		for ( int i = 0; i < size; i+=2 ) {
			identifiers.put(
				(i/2),
				persister.getIdentifierType().assemble( array[i], getSession(), owner )
			);
			values.add( persister.getElementType().assemble( array[i+1], getSession(), owner ) );
		}
	}

	@Override
	public Object getIdentifier(Object entry, int i) {
		return identifiers.get( i );
	}

	@Override
	public boolean isWrapper(Object collection) {
		return values==collection;
	}

	@Override
	public boolean add(Object o) {
		write();
		values.add( o );
		return true;
	}

	@Override
	public void clear() {
		initialize( true );
		if ( ! values.isEmpty() || ! identifiers.isEmpty() ) {
			values.clear();
			identifiers.clear();
			dirty();
		}
	}

	@Override
	public boolean contains(Object o) {
		read();
		return values.contains( o );
	}

	@Override
	public boolean containsAll(Collection c) {
		read();
		return values.containsAll( c );
	}

	@Override
	public boolean isEmpty() {
		return readSize() ? getCachedSize()==0 : values.isEmpty();
	}

	@Override
	public Iterator iterator() {
		read();
		return new IteratorProxy( values.iterator() );
	}

	@Override
	public boolean remove(Object o) {
		initialize( true );
		final int index = values.indexOf( o );
		if ( index >= 0 ) {
			beforeRemove( index );
			values.remove( index );
			dirty();
			return true;
		}
		else {
			return false;
		}
	}

	@Override
	public boolean removeAll(Collection c) {
		if ( c.size() > 0 ) {
			boolean result = false;
			for ( Object element : c ) {
				if ( remove( element ) ) {
					result = true;
				}
			}
			return result;
		}
		else {
			return false;
		}
	}

	@Override
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

	@Override
	public int size() {
		return readSize() ? getCachedSize() : values.size();
	}

	@Override
	public Object[] toArray() {
		read();
		return values.toArray();
	}

	@Override
	public Object[] toArray(Object[] a) {
		read();
		return values.toArray( a );
	}

	@Override
	public void beforeInitialize(CollectionPersister persister, int anticipatedSize) {
		identifiers = anticipatedSize <= 0
				? new HashMap<Integer, Object>()
				: new HashMap<Integer, Object>( anticipatedSize + 1 + (int)( anticipatedSize * .75f ), .75f );
		values = anticipatedSize <= 0
				? new ArrayList<Object>()
				: new ArrayList<Object>( anticipatedSize );
	}

	@Override
	public Serializable disassemble(CollectionPersister persister)
			throws HibernateException {
		final Serializable[] result = new Serializable[ values.size() * 2 ];
		int i = 0;
		for ( int j=0; j< values.size(); j++ ) {
			final Object value = values.get( j );
			result[i++] = persister.getIdentifierType().disassemble( identifiers.get( j ), getSession(), null );
			result[i++] = persister.getElementType().disassemble( value, getSession(), null );
		}
		return result;
	}

	@Override
	public boolean empty() {
		return values.isEmpty();
	}

	@Override
	public Iterator entries(CollectionPersister persister) {
		return values.iterator();
	}

	@Override
	public boolean entryExists(Object entry, int i) {
		return entry!=null;
	}

	@Override
	public boolean equalsSnapshot(CollectionPersister persister) throws HibernateException {
		final Type elementType = persister.getElementType();
		final Map snap = (Map) getSnapshot();
		if ( snap.size()!= values.size() ) {
			return false;
		}
		for ( int i=0; i<values.size(); i++ ) {
			final Object value = values.get( i );
			final Object id = identifiers.get( i );
			if ( id == null ) {
				return false;
			}
			final Object old = snap.get( id );
			if ( elementType.isDirty( old, value, getSession() ) ) {
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean isSnapshotEmpty(Serializable snapshot) {
		return ( (Map) snapshot ).isEmpty();
	}

	@Override
	@SuppressWarnings("unchecked")
	public Iterator getDeletes(CollectionPersister persister, boolean indexIsFormula) throws HibernateException {
		final Map snap = (Map) getSnapshot();
		final List deletes = new ArrayList( snap.keySet() );
		for ( int i=0; i<values.size(); i++ ) {
			if ( values.get( i ) != null ) {
				deletes.remove( identifiers.get( i ) );
			}
		}
		return deletes.iterator();
	}

	@Override
	public Object getIndex(Object entry, int i, CollectionPersister persister) {
		throw new UnsupportedOperationException("Bags don't have indexes");
	}

	@Override
	public Object getElement(Object entry) {
		return entry;
	}

	@Override
	public Object getSnapshotElement(Object entry, int i) {
		final Map snap = (Map) getSnapshot();
		final Object id = identifiers.get( i );
		return snap.get( id );
	}

	@Override
	public boolean needsInserting(Object entry, int i, Type elemType)
			throws HibernateException {
		final Map snap = (Map) getSnapshot();
		final Object id = identifiers.get( i );
		return entry != null
				&& ( id==null || snap.get( id )==null );
	}

	@Override
	public boolean needsUpdating(Object entry, int i, Type elemType) throws HibernateException {
		if ( entry == null ) {
			return false;
		}

		final Map snap = (Map) getSnapshot();
		final Object id = identifiers.get( i );
		if ( id == null ) {
			return false;
		}

		final Object old = snap.get( id );
		return old != null && elemType.isDirty( old, entry, getSession() );
	}

	@Override
	public Object readFrom(
			ResultSet rs,
			CollectionPersister persister,
			CollectionAliases descriptor,
			Object owner) throws HibernateException, SQLException {
		final Object element = persister.readElement( rs, owner, descriptor.getSuffixedElementAliases(), getSession() );
		final Object old = identifiers.put(
			values.size(),
			persister.readIdentifier( rs, descriptor.getSuffixedIdentifierAlias(), getSession() )
		);

		if ( old == null ) {
			//maintain correct duplication if loaded in a cartesian product
			values.add( element );
		}
		return element;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Serializable getSnapshot(CollectionPersister persister) throws HibernateException {
		final HashMap map = new HashMap( values.size() );
		final Iterator iter = values.iterator();
		int i=0;
		while ( iter.hasNext() ) {
			final Object value = iter.next();
			map.put(
					identifiers.get( i++ ),
					persister.getElementType().deepCopy( value, persister.getFactory() )
			);
		}
		return map;
	}

	@Override
	public Collection getOrphans(Serializable snapshot, String entityName) throws HibernateException {
		final Map sn = (Map) snapshot;
		return getOrphans( sn.values(), values, entityName, getSession() );
	}

	@Override
	public void preInsert(CollectionPersister persister) throws HibernateException {
		final Iterator itr = values.iterator();
		int i = 0;
		while ( itr.hasNext() ) {
			final Object entry = itr.next();
			final Integer loc = i++;
			if ( !identifiers.containsKey( loc ) ) {
				//TODO: native ids
				final Serializable id = persister.getIdentifierGenerator().generate( getSession(), entry );
				identifiers.put( loc, id );
			}
		}
	}

	@Override
	public void add(int index, Object element) {
		write();
		beforeAdd( index );
		values.add( index, element );
	}

	@Override
	public boolean addAll(int index, Collection c) {
		if ( c.size() > 0 ) {
			for ( Object element : c ) {
				add( index++, element );
			}
			return true;
		}
		else {
			return false;
		}
	}

	@Override
	public Object get(int index) {
		read();
		return values.get( index );
	}

	@Override
	public int indexOf(Object o) {
		read();
		return values.indexOf( o );
	}

	@Override
	public int lastIndexOf(Object o) {
		read();
		return values.lastIndexOf( o );
	}

	@Override
	public ListIterator listIterator() {
		read();
		return new ListIteratorProxy( values.listIterator() );
	}

	@Override
	public ListIterator listIterator(int index) {
		read();
		return new ListIteratorProxy( values.listIterator( index ) );
	}

	private void beforeRemove(int index) {
		final Object removedId = identifiers.get( index );
		final int last = values.size()-1;
		for ( int i=index; i<last; i++ ) {
			final Object id = identifiers.get( i+1 );
			if ( id == null ) {
				identifiers.remove( i );
			}
			else {
				identifiers.put( i, id );
			}
		}
		identifiers.put( last, removedId );
	}

	private void beforeAdd(int index) {
		for ( int i=index; i<values.size(); i++ ) {
			identifiers.put( i+1, identifiers.get( i ) );
		}
		identifiers.remove( index );
	}

	@Override
	public Object remove(int index) {
		write();
		beforeRemove( index );
		return values.remove( index );
	}

	@Override
	public Object set(int index, Object element) {
		write();
		return values.set( index, element );
	}

	@Override
	public List subList(int fromIndex, int toIndex) {
		read();
		return new ListProxy( values.subList( fromIndex, toIndex ) );
	}

	@Override
	public boolean addAll(Collection c) {
		if ( c.size()> 0 ) {
			write();
			return values.addAll( c );
		}
		else {
			return false;
		}
	}

	@Override
	public void afterRowInsert(
			CollectionPersister persister,
			Object entry,
			int i) throws HibernateException {
		//TODO: if we are using identity columns, fetch the identifier
	}

}
