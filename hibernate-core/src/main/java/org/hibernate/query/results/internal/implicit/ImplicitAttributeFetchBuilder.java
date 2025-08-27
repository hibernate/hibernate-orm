/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.results.internal.implicit;

import org.hibernate.engine.FetchTiming;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.query.results.FetchBuilder;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;

/**
 * FetchBuilder used when an explicit mapping was not given
 *
 * @author Steve Ebersole
 */
public class ImplicitAttributeFetchBuilder implements FetchBuilder, ImplicitFetchBuilder {
	private final NavigablePath navigablePath;
	private final AttributeMapping attributeMapping;

	public ImplicitAttributeFetchBuilder(NavigablePath navigablePath, AttributeMapping attributeMapping) {
		this.navigablePath = navigablePath;
		this.attributeMapping = attributeMapping;
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
			DomainResultCreationState domainResultCreationState) {
		assert fetchPath.equals( navigablePath );

		return parent.generateFetchableFetch(
				attributeMapping,
				fetchPath,
				FetchTiming.IMMEDIATE,
				true,
				null,
				domainResultCreationState
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

		final ImplicitAttributeFetchBuilder that = (ImplicitAttributeFetchBuilder) o;
		return navigablePath.equals( that.navigablePath )
			&& attributeMapping.equals( that.attributeMapping );
	}

	@Override
	public int hashCode() {
		int result = navigablePath.hashCode();
		result = 31 * result + attributeMapping.hashCode();
		return result;
	}
}
