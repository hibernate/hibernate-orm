/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.usertype;
import java.util.Iterator;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.persister.collection.CollectionPersister;

/**
 * A custom type for mapping user-written classes that implement <tt>PersistentCollection</tt>
 * 
 * @see org.hibernate.collection.spi.PersistentCollection
 * @author Gavin King
 */
public interface UserCollectionType {
	
	/**
	 * Instantiate an uninitialized instance of the collection wrapper
	 */
	public PersistentCollection instantiate(SessionImplementor session, CollectionPersister persister) 
	throws HibernateException;
	
	/**
	 * Wrap an instance of a collection
	 */
	public PersistentCollection wrap(SessionImplementor session, Object collection);
	
	/**
	 * Return an iterator over the elements of this collection - the passed collection
	 * instance may or may not be a wrapper
	 */
	public Iterator getElementsIterator(Object collection);

	/**
	 * Optional operation. Does the collection contain the entity instance?
	 */
	public boolean contains(Object collection, Object entity);
	/**
	 * Optional operation. Return the index of the entity in the collection.
	 */
	public Object indexOf(Object collection, Object entity);
	
	/**
	 * Replace the elements of a collection with the elements of another collection
	 */
	public Object replaceElements(
			Object original, 
			Object target, 
			CollectionPersister persister, 
			Object owner, 
			Map copyCache, 
			SessionImplementor session)
			throws HibernateException;

	/**
	 * Instantiate an empty instance of the "underlying" collection (not a wrapper),
	 * but with the given anticipated size (i.e. accounting for initial size
	 * and perhaps load factor).
	 *
	 * @param anticipatedSize The anticipated size of the instaniated collection
	 * after we are done populating it.  Note, may be negative to indicate that
	 * we not yet know anything about the anticipated size (i.e., when initializing
	 * from a result set row by row).
	 */
	public Object instantiate(int anticipatedSize);
	
}
