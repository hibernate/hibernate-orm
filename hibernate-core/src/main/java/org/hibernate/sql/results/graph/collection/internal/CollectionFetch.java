/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.collection.internal;

import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.results.ResultsHelper;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParent;

/**
 * @author Steve Ebersole
 */
public abstract class CollectionFetch implements Fetch {
	private final NavigablePath fetchedPath;
	private final PluralAttributeMapping fetchedAttribute;

	private final FetchParent fetchParent;

	public CollectionFetch(
			NavigablePath fetchedPath,
			PluralAttributeMapping fetchedAttribute,
			FetchParent fetchParent) {
		this.fetchedPath = fetchedPath;
		this.fetchedAttribute = fetchedAttribute;
		this.fetchParent = fetchParent;
	}

	@Override
	public FetchParent getFetchParent() {
		return fetchParent;
	}

	@Override
	public PluralAttributeMapping getFetchedMapping() {
		return fetchedAttribute;
	}

	@Override
	public DomainResult<?> asResult(DomainResultCreationState creationState) {
		return fetchedAttribute.createDomainResult(
				fetchedPath,
				ResultsHelper.impl( creationState )
						.getFromClauseAccess()
						.getTableGroup( fetchParent.getNavigablePath() ),
				null,
				creationState
		);
	}

	@Override
	public NavigablePath getNavigablePath() {
		return fetchedPath;
	}
}
