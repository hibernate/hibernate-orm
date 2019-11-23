/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal.domain.collection;

import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.results.spi.Fetch;
import org.hibernate.sql.results.spi.FetchParent;

/**
 * @author Steve Ebersole
 */
public abstract class CollectionFetch implements Fetch {
	private final NavigablePath fetchedPath;
	private final PluralAttributeMapping fetchedAttribute;

	private final boolean nullable;

	private final FetchParent fetchParent;

	public CollectionFetch(
			NavigablePath fetchedPath,
			PluralAttributeMapping fetchedAttribute,
			boolean nullable,
			FetchParent fetchParent) {
		this.fetchedPath = fetchedPath;
		this.fetchedAttribute = fetchedAttribute;
		this.fetchParent = fetchParent;
		this.nullable = nullable;
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
	public NavigablePath getNavigablePath() {
		return fetchedPath;
	}

	@Override
	public boolean isNullable() {
		return nullable;
	}
}
