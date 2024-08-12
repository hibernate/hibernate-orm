/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
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
public abstract class AbstractFetchParent implements FetchParent {
	private final NavigablePath navigablePath;

	private ImmutableFetchList fetches = ImmutableFetchList.EMPTY;
	private boolean hasJoinFetches;
	private boolean containsCollectionFetches;

	public AbstractFetchParent(NavigablePath navigablePath) {
		this.navigablePath = navigablePath;
	}

	/*
	 * Used by Hibernate Reactive
	 */
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

	protected void resetFetches(ImmutableFetchList newFetches) {
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
	public ImmutableFetchList getFetches() {
		return fetches;
	}

	@Override
	public Fetch findFetch(final Fetchable fetchable) {
		if ( fetchable instanceof EntityVersionMapping ) {
			return fetches.get( ( (EntityVersionMapping) fetchable ).getVersionAttribute() );
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
