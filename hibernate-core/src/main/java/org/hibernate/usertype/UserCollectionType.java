/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.usertype;

import java.util.Iterator;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.CollectionClassification;
import org.hibernate.persister.collection.CollectionPersister;

/**
 * A custom type for mapping user-written classes which implement {@link PersistentCollection}.
 *
 * @see PersistentCollection
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public interface UserCollectionType {
	/**
	 * The classification mapped by this custom type
	 */
	CollectionClassification getClassification();

	/**
	 * The Java type that this type maps.
	 */
	Class<?> getCollectionClass();

	/**
	 * Instantiate an uninitialized instance of the collection wrapper
	 */
	PersistentCollection<?> instantiate(SharedSessionContractImplementor session, CollectionPersister persister)
			throws HibernateException;

	/**
	 * Wrap an instance of a collection
	 */
	PersistentCollection<?> wrap(SharedSessionContractImplementor session, Object collection);

	/**
	 * Return an iterator over the elements of this collection - the passed collection
	 * instance may or may not be a wrapper
	 */
	Iterator<?> getElementsIterator(Object collection);

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
	 * Instantiate an empty instance of the "underlying" collection (not a wrapper),
	 * but with the given anticipated size (i.e. accounting for initial size
	 * and perhaps load factor).
	 *
	 * @param anticipatedSize The anticipated size of the instantiated collection
	 * after we are done populating it.  Note, may be negative to indicate that
	 * we not yet know anything about the anticipated size (i.e., when initializing
	 * from a result set row by row).
	 */
	Object instantiate(int anticipatedSize);

}
