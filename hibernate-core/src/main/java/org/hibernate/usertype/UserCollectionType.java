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
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.collection.CollectionPersister;

/**
 * A custom type for mapping user-written classes that implement <tt>PersistentCollection</tt>
 *
 * @author Gavin King
 * @see org.hibernate.collection.spi.PersistentCollection
 */
public interface UserCollectionType {

	/**
	 * Instantiate an uninitialized instance of the collection wrapper
	 */
	PersistentCollection instantiate(SharedSessionContractImplementor session, CollectionPersister persister)
			throws HibernateException;

	/**
	 * Instantiate an uninitialized instance of the collection wrapper
	 *
	 * @deprecated {@link #instantiate(SharedSessionContractImplementor, CollectionPersister)}
	 *             should be used instead.
	 */
	@Deprecated
	default PersistentCollection instantiate(SessionImplementor session, CollectionPersister persister)
			throws HibernateException {
		return instantiate( (SharedSessionContractImplementor) session, persister );
	}

	/**
	 * Wrap an instance of a collection
	 */
	PersistentCollection wrap(SharedSessionContractImplementor session, Object collection);

	/**
	 * Wrap an instance of a collection
	 *
	 * @deprecated {@link #wrap(SharedSessionContractImplementor, Object)} should be used instead.
	 */
	@Deprecated
	default PersistentCollection wrap(SessionImplementor session, Object collection) {
		return wrap( (SharedSessionContractImplementor) session, collection );
	}

	/**
	 * Return an iterator over the elements of this collection - the passed collection
	 * instance may or may not be a wrapper
	 */
	Iterator getElementsIterator(Object collection);

	/**
	 * Optional operation. Does the collection contain the entity instance?
	 */
	boolean contains(Object collection, Object entity);

	/**
	 * Optional operation. Return the index of the entity in the collection.
	 */
	Object indexOf(Object collection, Object entity);

	/**
	 * Replace the elements of a collection with the elements of another collection
	 */
	Object replaceElements(
			Object original,
			Object target,
			CollectionPersister persister,
			Object owner,
			Map copyCache,
			SharedSessionContractImplementor session) throws HibernateException;

	/**
	 * Replace the elements of a collection with the elements of another collection
	 *
	 * @deprecated {@link #replaceElements(Object, Object, CollectionPersister, Object, Map, SharedSessionContractImplementor)}
	 *             should be used instead.
	 */
	@Deprecated
	default Object replaceElements(
			Object original,
			Object target,
			CollectionPersister persister,
			Object owner,
			Map copyCache,
			SessionImplementor session) throws HibernateException {
		return replaceElements(
				original,
				target,
				persister,
				owner,
				copyCache,
				(SharedSessionContractImplementor) session
		);
	}

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
	Object instantiate(int anticipatedSize);

}
