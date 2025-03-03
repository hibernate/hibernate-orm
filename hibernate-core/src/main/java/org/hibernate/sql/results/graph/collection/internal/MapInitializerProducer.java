/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.graph.collection.internal;

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
public class MapInitializerProducer implements CollectionInitializerProducer {
	private final PluralAttributeMapping mapDescriptor;
	private final Fetch mapKeyFetch;
	private final Fetch mapValueFetch;

	public MapInitializerProducer(
			PluralAttributeMapping mapDescriptor,
			Fetch mapKeyFetch,
			Fetch mapValueFetch) {
		this.mapDescriptor = mapDescriptor;
		this.mapKeyFetch = mapKeyFetch;
		this.mapValueFetch = mapValueFetch;
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
			AssemblerCreationState creationState) {
		return new MapInitializer(
				navigablePath,
				mapDescriptor,
				parent,
				lockMode,
				collectionKeyResult,
				collectionValueKeyResult,
				isResultInitializer,
				creationState,
				mapKeyFetch,
				mapValueFetch
		);
	}
}
