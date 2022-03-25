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
import org.hibernate.spi.NavigablePath;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractFetchParent implements FetchParent {
	private final FetchableContainer fetchContainer;
	private final NavigablePath navigablePath;

	protected List<Fetch> fetches;

	public AbstractFetchParent(FetchableContainer fetchContainer, NavigablePath navigablePath) {
		this.fetchContainer = fetchContainer;
		this.navigablePath = navigablePath;
	}

	public void afterInitialize(FetchParent fetchParent, DomainResultCreationState creationState) {
		assert fetches == null;
		this.fetches = creationState.visitFetches( fetchParent );
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
		return fetches == null ? Collections.emptyList() : Collections.unmodifiableList( fetches );
	}

	@Override
	public Fetch findFetch(Fetchable fetchable) {
		if ( fetches == null ) {
			return null;
		}

		for ( int i = 0; i < fetches.size(); i++ ) {
			final Fetch fetch = fetches.get( i );
			final Fetchable fetchedMapping = fetch.getFetchedMapping();
			if ( fetchedMapping == fetchable
					// the fetched mapping for the version is a Basic attribute, so check the role
					|| ( fetchable instanceof EntityVersionMapping && fetchable.getNavigableRole().equals( fetchedMapping.getNavigableRole() ) ) ) {
				return fetch;
			}
		}

		return null;
	}
}
