/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.collection.spi;

import java.util.Iterator;
import java.util.function.Consumer;

import org.hibernate.Incubating;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.CollectionClassification;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParent;

/**
 * Each instance of this interface describes the semantics of some sort of
 * persistent collection so that Hibernate understands how to manage the
 * lifecycle of instances of that sort of collection.
 * <p>
 * A collection type with semantics described by a {@code CollectionSemantics}
 * object need not be part of the Java Collections Framework.
 *
 * @param <E> the collection element or map key type
 * @param <CE> the type of the collection
 *
 * @author Steve Ebersole
 * @author Gavin King
 *
 * @since 6.0
 */
@Incubating
public interface CollectionSemantics<CE, E> {
	/**
	 * The classification handled by this semantic
	 */
	CollectionClassification getCollectionClassification();

	/**
	 * The collection's Java type
	 */
	Class<?> getCollectionJavaType();

	/**
	 * Create a raw (unwrapped) version of the collection
	 */
	CE instantiateRaw(
			int anticipatedSize,
			CollectionPersister collectionDescriptor);

	/**
	 * Create a wrapper for the collection
	 */
	PersistentCollection<E> instantiateWrapper(
			Object key,
			CollectionPersister collectionDescriptor,
			SharedSessionContractImplementor session);

	/**
	 * Wrap a raw collection in wrapper
	 */
	PersistentCollection<E> wrap(
			CE rawCollection,
			CollectionPersister collectionDescriptor,
			SharedSessionContractImplementor session);

	/**
	 * Obtain an iterator over the collection elements
	 */
	Iterator<E> getElementIterator(CE rawCollection);

	/**
	 * Visit the elements of the collection
	 */
	void visitElements(CE rawCollection, Consumer<? super E> action);

	/**
	 * Create a producer for {@link org.hibernate.sql.results.graph.collection.CollectionInitializer}
	 * instances for the given collection semantics
	 *
	 * @see InitializerProducerBuilder
	 */
	default CollectionInitializerProducer createInitializerProducer(
			NavigablePath navigablePath,
			PluralAttributeMapping attributeMapping,
			FetchParent fetchParent,
			boolean selected,
			String resultVariable,
			DomainResultCreationState creationState) {
		return createInitializerProducer(
				navigablePath, attributeMapping, fetchParent, selected, resultVariable, null, null, creationState
		);
	}

	/**
	 * Create a producer for {@link org.hibernate.sql.results.graph.collection.CollectionInitializer}
	 * instances for the given collection semantics
	 *
	 * @see InitializerProducerBuilder
	 */
	default CollectionInitializerProducer createInitializerProducer(
			NavigablePath navigablePath,
			PluralAttributeMapping attributeMapping,
			FetchParent fetchParent,
			boolean selected,
			String resultVariable,
			Fetch indexFetch,
			Fetch elementFetch,
			DomainResultCreationState creationState) {
		return InitializerProducerBuilder.createInitializerProducer(
				navigablePath,
				attributeMapping,
				getCollectionClassification(),
				fetchParent,
				selected,
				indexFetch,
				elementFetch,
				creationState
		);
	}
}
