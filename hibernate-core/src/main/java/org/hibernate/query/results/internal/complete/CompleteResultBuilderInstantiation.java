/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.results.internal.complete;

import org.hibernate.query.results.ResultBuilder;
import org.hibernate.query.results.ResultBuilderInstantiationValued;
import org.hibernate.query.sqm.DynamicInstantiationNature;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.instantiation.internal.ArgumentDomainResult;
import org.hibernate.sql.results.graph.instantiation.internal.DynamicInstantiationResultImpl;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;
import org.hibernate.type.descriptor.java.JavaType;

import java.util.ArrayList;
import java.util.List;

/**
 * ResultBuilder for dynamic instantiation results ({@link jakarta.persistence.ConstructorResult}
 *
 * @author Steve Ebersole
 */
public class CompleteResultBuilderInstantiation
		implements CompleteResultBuilder, ResultBuilderInstantiationValued {

	private final JavaType<?> javaType;
	private final List<ResultBuilder> argumentResultBuilders;

	public CompleteResultBuilderInstantiation(
			JavaType<?> javaType,
			List<ResultBuilder> argumentResultBuilders) {
		this.javaType = javaType;
		this.argumentResultBuilders = argumentResultBuilders;
	}

	@Override
	public Class<?> getJavaType() {
		return javaType.getJavaTypeClass();
	}

	@Override
	public ResultBuilder cacheKeyInstance() {
		return this;
	}

	@Override
	public DomainResult<?> buildResult(
			JdbcValuesMetadata jdbcResultsMetadata,
			int resultPosition,
			DomainResultCreationState domainResultCreationState) {
		return new DynamicInstantiationResultImpl<>(
				null,
				DynamicInstantiationNature.CLASS,
				javaType,
				argumentDomainResults( jdbcResultsMetadata, domainResultCreationState )
		);
	}

	private List<ArgumentDomainResult<?>> argumentDomainResults(
			JdbcValuesMetadata jdbcResultsMetadata, DomainResultCreationState domainResultCreationState) {
		final List<ArgumentDomainResult<?>> argumentDomainResults = new ArrayList<>( argumentResultBuilders.size() );
		for ( int i = 0; i < argumentResultBuilders.size(); i++ ) {
			argumentDomainResults.add( new ArgumentDomainResult<>(
					argumentResultBuilders.get( i )
							.buildResult( jdbcResultsMetadata, i, domainResultCreationState )
			) );
		}
		return argumentDomainResults;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		final CompleteResultBuilderInstantiation that = (CompleteResultBuilderInstantiation) o;
		return javaType.equals( that.javaType )
			&& argumentResultBuilders.equals( that.argumentResultBuilders );
	}

	@Override
	public int hashCode() {
		int result = javaType.hashCode();
		result = 31 * result + argumentResultBuilders.hashCode();
		return result;
	}
}
