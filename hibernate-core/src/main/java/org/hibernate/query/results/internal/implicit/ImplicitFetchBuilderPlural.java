/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.results.internal.implicit;


import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.spi.NavigablePath;
import org.hibernate.query.results.FetchBuilder;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;


/**
 * @author Christian Beikov
 */
public class ImplicitFetchBuilderPlural implements ImplicitFetchBuilder {
	private final NavigablePath fetchPath;
	private final PluralAttributeMapping fetchable;

	public ImplicitFetchBuilderPlural(
			NavigablePath fetchPath,
			PluralAttributeMapping fetchable,
			DomainResultCreationState creationState) {
		this.fetchPath = fetchPath;
		this.fetchable = fetchable;
	}

	@Override
	public FetchBuilder cacheKeyInstance() {
		return this;
	}

	@Override
	public Fetch buildFetch(
			FetchParent parent,
			NavigablePath fetchPath,
			JdbcValuesMetadata jdbcResultsMetadata,
			DomainResultCreationState creationState) {
		return parent.generateFetchableFetch(
				fetchable,
				fetchPath,
				fetchable.getMappedFetchOptions().getTiming(),
				false,
				null,
				creationState
		);
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		final ImplicitFetchBuilderPlural that = (ImplicitFetchBuilderPlural) o;
		return fetchPath.equals( that.fetchPath )
			&& fetchable.equals( that.fetchable );
	}

	@Override
	public int hashCode() {
		int result = fetchPath.hashCode();
		result = 31 * result + fetchable.hashCode();
		return result;
	}

	@Override
	public String toString() {
		return "ImplicitFetchBuilderPlural(" + fetchPath + ")";
	}
}
