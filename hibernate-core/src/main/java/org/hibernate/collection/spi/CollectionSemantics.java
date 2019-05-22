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
import org.hibernate.metamodel.CollectionClassification;
import org.hibernate.persister.collection.CollectionPersister;

/**
 * Describes the semantics of a persistent collection such that Hibernate
 * understands how to use it - create one, handle elements, etc.
 *
 * @apiNote The described collection need not be part of the "Java Collection Framework"
 *
 * @author Steve Ebersole
 * @author Gavin King
 */
public interface CollectionSemantics<C> {
	/**
	 * Get the classification of collections described by this semantic
	 */
	CollectionClassification getCollectionClassification();

	<E> C instantiateRaw(
			int anticipatedSize,
			CollectionPersister collectionDescriptor);

	<E> PersistentCollection instantiateWrapper(
			Object key,
			CollectionPersister collectionDescriptor,
			SharedSessionContractImplementor session);

	<E> PersistentCollection wrap(
			Object rawCollection,
			CollectionPersister collectionDescriptor,
			SharedSessionContractImplementor session);

	<E> Iterator<E> getElementIterator(C rawCollection);

	void visitElements(C rawCollection, Consumer action);
}
