/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.generator.values.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.generator.values.GeneratedValueBasicResultBuilder;
import org.hibernate.generator.values.GeneratedValuesMutationDelegate;
import org.hibernate.query.results.internal.DomainResultCreationStateImpl;
import org.hibernate.query.results.internal.JdbcValuesMappingImpl;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMapping;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMappingProducer;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;

/**
 * Simple implementation of {@link JdbcValuesMappingProducer} used when reading
 * generated values from a mutation statement.
 *
 * @author Marco Belladelli
 * @see GeneratedValuesMutationDelegate
 * @see GeneratedValuesHelper#getGeneratedValues
 */
public class GeneratedValuesMappingProducer implements JdbcValuesMappingProducer {
	private final List<GeneratedValueBasicResultBuilder> resultBuilders = new ArrayList<>();

	@Override
	public JdbcValuesMapping resolve(
			JdbcValuesMetadata jdbcResultsMetadata,
			LoadQueryInfluencers loadQueryInfluencers,
			SessionFactoryImplementor sessionFactory) {
		final int numberOfResults = resultBuilders.size();
		final int rowSize = jdbcResultsMetadata.getColumnCount();

		final List<SqlSelection> sqlSelections = new ArrayList<>( rowSize );

		final var creationState = new DomainResultCreationStateImpl(
				null,
				jdbcResultsMetadata,
				null,
				sqlSelections::add,
				loadQueryInfluencers,
				false,
				sessionFactory
		);

		final List<DomainResult<?>> domainResults = new ArrayList<>( numberOfResults );
		for ( int i = 0; i < numberOfResults; i++ ) {
			final var domainResult =
					resultBuilders.get( i )
							.buildResult( jdbcResultsMetadata, i, creationState );
			if ( domainResult.containsAnyNonScalarResults() ) {
				creationState.disallowPositionalSelections();
			}
			domainResults.add( domainResult );
		}

		return new JdbcValuesMappingImpl(
				sqlSelections,
				domainResults,
				rowSize,
				creationState.getRegisteredLockModes()
		);
	}

	public void addResultBuilder(GeneratedValueBasicResultBuilder resultBuilder) {
		resultBuilders.add( resultBuilder );
	}

	public List<GeneratedValueBasicResultBuilder> getResultBuilders() {
		return resultBuilders;
	}

	@Override
	public void addAffectedTableNames(Set<String> affectedTableNames, SessionFactoryImplementor sessionFactory) {
		// nothing to do
	}
}
