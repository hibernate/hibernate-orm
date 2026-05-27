/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.graph.collection.internal;

import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;

import org.hibernate.LockMode;
import org.hibernate.collection.spi.CollectionInitializerProducer;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.InitializerParent;
import org.hibernate.sql.results.graph.collection.CollectionInitializer;

/**
 * @author Steve Ebersole
 */
public class SetInitializerProducer implements CollectionInitializerProducer {
	private final PluralAttributeMapping setDescriptor;
	private final Fetch elementFetch;

	public SetInitializerProducer(
			PluralAttributeMapping setDescriptor,
			Fetch elementFetch) {
		this.setDescriptor = setDescriptor;
		this.elementFetch = elementFetch;
	}

	@Override
	public CollectionInitializer<?> produceInitializer(
			NavigablePath navigablePath,
			PluralAttributeMapping attribute,
			InitializerParent<?> parent,
			LockMode lockMode,
			DomainResult<?> collectionKeyResult,
			DomainResult<?> collectionValueKeyResult,
			boolean isResultInitializer,
			CacheStoreMode cacheStoreMode,
			CacheRetrieveMode cacheRetrieveMode,
			Integer batchSize,
			AssemblerCreationState creationState) {
		return new SetInitializer(
				navigablePath,
				setDescriptor,
				parent,
				lockMode,
				collectionKeyResult,
				collectionValueKeyResult,
				isResultInitializer,
				cacheStoreMode,
				cacheRetrieveMode,
				batchSize,
				creationState,
				elementFetch
		);
	}
}
