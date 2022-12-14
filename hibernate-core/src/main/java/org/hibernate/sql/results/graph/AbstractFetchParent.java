/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph;

import java.util.Collections;
import java.util.List;

import org.hibernate.metamodel.mapping.EntityVersionMapping;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.spi.NavigablePath;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractFetchParent implements FetchParent {
	private final FetchableContainer fetchContainer;
	private final NavigablePath navigablePath;

	private List<Fetch> fetches;

	public AbstractFetchParent(FetchableContainer fetchContainer, NavigablePath navigablePath) {
		this.fetchContainer = fetchContainer;
		this.navigablePath = navigablePath;
	}

	public void afterInitialize(FetchParent fetchParent, DomainResultCreationState creationState) {
		assert fetches == null;
		resetFetches( creationState.visitFetches( fetchParent ) );
	}

	protected void resetFetches(final List<Fetch> newFetches) {
		this.fetches = Collections.unmodifiableList( newFetches );
	}

	public FetchableContainer getFetchContainer() {
		return fetchContainer;
	}

	@Override
	public NavigablePath getNavigablePath() {
		return navigablePath;
	}

	@Override
	public JavaType<?> getResultJavaType() {
		return fetchContainer.getJavaType();
	}

	@Override
	public FetchableContainer getReferencedMappingContainer() {
		return fetchContainer;
	}

	@Override
	public List<Fetch> getFetches() {
		return fetches == null ? Collections.emptyList() : fetches;
	}

	@Override
	public Fetch findFetch(final Fetchable fetchable) {
		if ( fetches == null ) {
			return null;
		}

		//Iterate twice so we can perform the cheapest checks on each item first:
		for ( int i = 0; i < fetches.size(); i++ ) {
			final Fetch fetch = fetches.get( i );
			if ( fetchable == fetch.getFetchedMapping() ) {
				return fetch;
			}
		}

		if ( fetchable instanceof EntityVersionMapping ) {
			//Second iteration performs the slightly more expensive checks, necessary for EntityVersionMapping:
			final NavigableRole navigableRole = fetchable.getNavigableRole();
			for ( int i = 0; i < fetches.size(); i++ ) {
				final Fetch fetch = fetches.get( i );
				if ( fetch.getFetchedMapping().getNavigableRole().equals( navigableRole ) ) {
					return fetch;
				}
			}
		}

		return null;
	}
}
