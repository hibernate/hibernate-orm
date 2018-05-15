/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.collection.spi;

import java.util.Iterator;
import java.util.function.Consumer;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;

/**
 * Would be nice to expose this from {@link org.hibernate.type.descriptor.java.internal.CollectionJavaDescriptor} -
 * then plugging in a custom collection type is as simple as plugging in its CollectionJavaDescriptor
 *
 * @author Steve Ebersole
 */
public interface CollectionSemantics<C> {
	/**
	 * Get the classification of collections described by this semantic
	 */
	CollectionClassification getCollectionClassification();

	<E> C instantiateRaw(
			int anticipatedSize,
			PersistentCollectionDescriptor<?,C,E> collectionDescriptor);

	<E> PersistentCollection<E> instantiateWrapper(
			Object key,
			PersistentCollectionDescriptor<?,C,E> collectionDescriptor,
			SharedSessionContractImplementor session);

	<E> PersistentCollection<E> wrap(
			C rawCollection,
			PersistentCollectionDescriptor<?,C,E> collectionDescriptor,
			SharedSessionContractImplementor session);

	<E> Iterator<E> getElementIterator(C rawCollection);

	void visitElements(C rawCollection, Consumer action);
}
