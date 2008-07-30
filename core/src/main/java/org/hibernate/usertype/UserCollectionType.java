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
package org.hibernate.usertype;

import java.util.Iterator;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.collection.PersistentCollection;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.persister.collection.CollectionPersister;

/**
 * A custom type for mapping user-written classes that implement <tt>PersistentCollection</tt>
 * 
 * @see org.hibernate.collection.PersistentCollection
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
