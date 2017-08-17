/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.metamodel.model.domain.spi.NavigableContainer;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.results.spi.Fetch;
import org.hibernate.sql.results.spi.FetchParent;
import org.hibernate.sql.results.spi.FetchParentAccess;
import org.hibernate.sql.results.spi.InitializerCollector;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractFetchParent implements FetchParent {
	private final NavigableContainer fetchContainer;
	private final NavigablePath navigablePath;

	private List<Fetch> fetches;

	// todo (6.0) : some form of "parent key" rather than relying directly on Expressions (NavigableContainerReference)

	public AbstractFetchParent(NavigableContainer fetchContainer, NavigablePath navigablePath) {
		this.fetchContainer = fetchContainer;
		this.navigablePath = navigablePath;
	}

	@Override
	public NavigablePath getNavigablePath() {
		return navigablePath;
	}

	@Override
	public NavigableContainer getFetchContainer() {
		return fetchContainer;
	}

	@Override
	public void addFetch(Fetch fetch) {
		if ( fetches == null ) {
			fetches = new ArrayList<>();
		}

		fetches.add( fetch );
	}

	@Override
	public List<Fetch> getFetches() {
		final List<Fetch> base = fetches == null ? Collections.emptyList() : Collections.unmodifiableList( fetches );
		return new ArrayList<>( base );
	}


	protected void registerFetchInitializers(FetchParentAccess parentAccess, InitializerCollector collector) {
		if ( fetches == null ) {
			return;
		}

		for ( Fetch fetch : fetches ) {
			fetch.registerInitializers( parentAccess, collector );
		}
	}
}
