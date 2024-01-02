/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.generator.values.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.generator.values.GeneratedValueBasicResultBuilder;
import org.hibernate.generator.values.GeneratedValuesMutationDelegate;
import org.hibernate.query.results.DomainResultCreationStateImpl;
import org.hibernate.query.results.JdbcValuesMappingImpl;
import org.hibernate.query.results.ResultBuilder;
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
		final List<DomainResult<?>> domainResults = new ArrayList<>( numberOfResults );

		final DomainResultCreationStateImpl creationState = new DomainResultCreationStateImpl(
				null,
				jdbcResultsMetadata,
				null,
				sqlSelections::add,
				loadQueryInfluencers,
				sessionFactory
		);

		for ( int i = 0; i < numberOfResults; i++ ) {
			final ResultBuilder resultBuilder = resultBuilders.get( i );
			final DomainResult<?> domainResult = resultBuilder.buildResult(
					jdbcResultsMetadata,
					domainResults.size(),
					creationState.getLegacyFetchResolver()::resolve,
					creationState
			);

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
