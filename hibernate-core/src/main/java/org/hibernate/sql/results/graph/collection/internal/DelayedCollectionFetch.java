/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.graph.collection.internal;

import java.util.BitSet;

import org.hibernate.engine.FetchTiming;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.InitializerParent;
import org.hibernate.sql.results.graph.collection.CollectionInitializer;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * @author Steve Ebersole
 */
public class DelayedCollectionFetch extends CollectionFetch {

	private final DomainResult<?> collectionKeyResult;
	private final boolean unfetched;

	public DelayedCollectionFetch(
			NavigablePath fetchedPath,
			PluralAttributeMapping fetchedAttribute,
			FetchParent fetchParent,
			DomainResult<?> collectionKeyResult,
			boolean unfetched) {
		super( fetchedPath, fetchedAttribute, fetchParent );
		this.collectionKeyResult = collectionKeyResult;
		this.unfetched = unfetched;
	}

	@Override
	public DomainResultAssembler<?> createAssembler(
			InitializerParent<?> parent,
			AssemblerCreationState creationState) {
		// lazy attribute
		return unfetched
				? new UnfetchedCollectionAssembler( getFetchedMapping() )
				: super.createAssembler( parent, creationState );
	}

	public CollectionInitializer<?> createInitializer(InitializerParent<?> parent, AssemblerCreationState creationState) {
		return new DelayedCollectionInitializer(
				getNavigablePath(),
				getFetchedMapping(),
				parent,
				collectionKeyResult,
				creationState
		);
	}

	@Override
	public FetchTiming getTiming() {
		return FetchTiming.DELAYED;
	}

	@Override
	public boolean hasTableGroup() {
		return false;
	}

	@Override
	public JavaType<?> getResultJavaType() {
		return getFetchedMapping().getJavaType();
	}

	@Override
	public void collectValueIndexesToCache(BitSet valueIndexes) {
		if ( collectionKeyResult != null ) {
			collectionKeyResult.collectValueIndexesToCache( valueIndexes );
		}
	}
}
