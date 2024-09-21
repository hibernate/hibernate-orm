/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.results.complete;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

import org.hibernate.query.sqm.DynamicInstantiationNature;
import org.hibernate.query.results.ResultBuilderInstantiationValued;
import org.hibernate.query.results.dynamic.DynamicFetchBuilderLegacy;
import org.hibernate.query.results.ResultBuilder;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.instantiation.internal.ArgumentDomainResult;
import org.hibernate.sql.results.graph.instantiation.internal.DynamicInstantiationResultImpl;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;
import org.hibernate.type.descriptor.java.JavaType;

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
			BiFunction<String, String, DynamicFetchBuilderLegacy> legacyFetchResolver,
			DomainResultCreationState domainResultCreationState) {
		final List<ArgumentDomainResult<?>> argumentDomainResults = new ArrayList<>( argumentResultBuilders.size() );

		for ( int i = 0; i < argumentResultBuilders.size(); i++ ) {
			final ResultBuilder argumentResultBuilder = argumentResultBuilders.get( i );

			@SuppressWarnings({"unchecked", "rawtypes"}) final ArgumentDomainResult<?> argumentDomainResult = new ArgumentDomainResult(
					argumentResultBuilder.buildResult(
							jdbcResultsMetadata,
							i,
							legacyFetchResolver,
							domainResultCreationState
					)
			);

			argumentDomainResults.add( argumentDomainResult );
		}

		return new DynamicInstantiationResultImpl<>(
				null,
				DynamicInstantiationNature.CLASS,
				javaType,
				argumentDomainResults
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
