/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.results.internal.implicit;


import org.hibernate.engine.FetchTiming;
import org.hibernate.metamodel.mapping.internal.EntityCollectionPart;
import org.hibernate.spi.NavigablePath;
import org.hibernate.query.results.FetchBuilder;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;

/**
 * @author Steve Ebersole
 */
public class ImplicitFetchBuilderEntityPart implements ImplicitFetchBuilder {
	private final NavigablePath fetchPath;
	private final EntityCollectionPart fetchable;

	public ImplicitFetchBuilderEntityPart(NavigablePath fetchPath, EntityCollectionPart fetchable) {
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
				FetchTiming.IMMEDIATE,
				true,
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

		final ImplicitFetchBuilderEntityPart that = (ImplicitFetchBuilderEntityPart) o;
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
		return "ImplicitFetchBuilderEntityPart(" + fetchPath + ")";
	}
}
