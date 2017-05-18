/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.spi;

import java.io.Serializable;

import org.hibernate.MappingException;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionMetadata;

/**
 * @author Steve Ebersole
 */
public interface CollectionType<O,C,E> extends Type<C> {
	PersistentCollectionMetadata<O,C,E> getCollectionPersister();

	@Override
	default Classification getClassification() {
		return Classification.COLLECTION;
	}

	@Override
	default Class<C> getJavaType() {
		return getCollectionPersister().getJavaType();
	}

	@Override
	default String asLoggableText() {
		return getCollectionPersister().asLoggableText();
	}

	@Override
	default JdbcLiteralFormatter getJdbcLiteralFormatter() {
		return null;
	}

	PersistentCollection instantiate(SharedSessionContractImplementor session, PersistentCollectionMetadata persister, Serializable key);

	/**
	 * Instantiate an empty instance of the "underlying" collection (not a wrapper),
	 * but with the given anticipated size (i.e. accounting for initial capacity
	 * and perhaps load factor).
	 *
	 * @param anticipatedSize The anticipated size of the instaniated collection
	 * afterQuery we are done populating it.
	 * @return A newly instantiated collection to be wrapped.
	 */
	Object instantiate(int anticipatedSize);

	/**
	 * Wrap the naked collection instance in a wrapper, or instantiate a
	 * holder. Callers <b>MUST</b> add the holder to the persistence context!
	 *
	 * @param session The session from which the request is originating.
	 * @param collection The bare collection to be wrapped.
	 * @return The wrapped collection.
	 */
	PersistentCollection wrap(SharedSessionContractImplementor session, Object collection);

	/**
	 * Get the Hibernate type of the collection elements
	 *
	 * @return The type of the collection elements
	 * @throws MappingException Indicates the underlying persister could not be located.
	 */
	Type getElementType() throws MappingException;

	Object indexOf(Object collection, Object element);

	boolean contains(Object collection, Object childObject);
}
