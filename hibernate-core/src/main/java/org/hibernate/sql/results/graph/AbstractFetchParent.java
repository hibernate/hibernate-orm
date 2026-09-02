/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.graph;

import java.util.BitSet;

import org.hibernate.metamodel.mapping.EntityVersionMapping;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.results.graph.internal.ImmutableFetchList;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * @author Steve Ebersole
 */
@org.hibernate.SPI({ org.hibernate.SPI.Role.USE, org.hibernate.SPI.Role.IMPLEMENT })
public abstract class AbstractFetchParent implements FetchParent {
	private final NavigablePath navigablePath;

	private FetchList fetches = ImmutableFetchList.EMPTY;
	private boolean hasJoinFetches;
	private boolean containsCollectionFetches;

	@org.hibernate.SPI(org.hibernate.SPI.Role.IMPLEMENT)
	public AbstractFetchParent(NavigablePath navigablePath) {
		this.navigablePath = navigablePath;
	}

	/*
	 * Used by Hibernate Reactive
	 */
	@org.hibernate.SPI(org.hibernate.SPI.Role.IMPLEMENT)
	public AbstractFetchParent(AbstractFetchParent original) {
		navigablePath = original.navigablePath;
		fetches = original.fetches;
		hasJoinFetches = original.hasJoinFetches;
		containsCollectionFetches = original.containsCollectionFetches;
	}

	public void afterInitialize(FetchParent fetchParent, DomainResultCreationState creationState) {
		assert fetches == ImmutableFetchList.EMPTY;
		resetFetches( creationState.visitFetches( fetchParent ) );
	}

	protected void resetFetches(FetchList newFetches) {
		this.fetches = newFetches;
		this.hasJoinFetches = newFetches.hasJoinFetches();
		this.containsCollectionFetches = newFetches.containsCollectionFetches();
	}

	public abstract FetchableContainer getFetchContainer();

	@Override
	public NavigablePath getNavigablePath() {
		return navigablePath;
	}

	@Override
	public JavaType<?> getResultJavaType() {
		return getFetchContainer().getJavaType();
	}

	@Override
	public FetchableContainer getReferencedMappingContainer() {
		return getFetchContainer();
	}

	@Override
	public FetchList getFetches() {
		return fetches;
	}

	@Override
	public Fetch findFetch(final Fetchable fetchable) {
		if ( fetchable instanceof EntityVersionMapping entityVersionMapping ) {
			return fetches.get( entityVersionMapping.getVersionAttribute() );
		}
		return fetches.get( fetchable );
	}

	@Override
	public boolean hasJoinFetches() {
		return hasJoinFetches;
	}

	@Override
	public boolean containsCollectionFetches() {
		return containsCollectionFetches;
	}

	public void collectValueIndexesToCache(BitSet valueIndexes) {
		FetchParent.super.collectValueIndexesToCache( valueIndexes );
	}
}
