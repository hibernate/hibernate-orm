/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.graph.collection.internal;

import java.util.BitSet;

import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;

import org.hibernate.engine.FetchTiming;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.InitializerParent;
import org.hibernate.sql.results.graph.collection.CollectionInitializer;
import org.hibernate.type.descriptor.java.JavaType;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * @author Andrea Boriero
 */
public class SelectEagerCollectionFetch extends CollectionFetch {
	private final @Nullable DomainResult<?> collectionKeyDomainResult;

	public SelectEagerCollectionFetch(
			NavigablePath fetchedPath,
			PluralAttributeMapping fetchedAttribute,
			DomainResult<?> collectionKeyDomainResult,
			FetchParent fetchParent,
			CacheStoreMode cacheStoreMode,
			CacheRetrieveMode cacheRetrieveMode) {
		super( fetchedPath, fetchedAttribute, fetchParent, cacheStoreMode, cacheRetrieveMode );
		this.collectionKeyDomainResult = collectionKeyDomainResult;
	}

	@Override
	public FetchTiming getTiming() {
		return FetchTiming.DELAYED;
	}

	@Override
	public boolean hasTableGroup() {
		return false;
	}

	public CollectionInitializer<?> createInitializer(InitializerParent<?> parent, AssemblerCreationState creationState) {
		return new SelectEagerCollectionInitializer(
				getNavigablePath(),
				getFetchedMapping(),
				parent,
				collectionKeyDomainResult,
				getCacheStoreMode(),
				getCacheRetrieveMode(),
				creationState
		);
	}

	@Override
	public JavaType<?> getResultJavaType() {
		return getFetchedMapping().getJavaType();
	}

	@Override
	public void collectValueIndexesToCache(BitSet valueIndexes) {
		if ( collectionKeyDomainResult != null ) {
			collectionKeyDomainResult.collectValueIndexesToCache( valueIndexes );
		}
	}
}
