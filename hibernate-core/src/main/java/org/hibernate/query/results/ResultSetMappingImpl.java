/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.results;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.hibernate.Incubating;
import org.hibernate.Internal;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.named.NamedResultSetMappingMemento;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.basic.BasicResult;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMapping;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Steve Ebersole
 */
@Incubating
@Internal
public class ResultSetMappingImpl implements ResultSetMapping {
	private List<ResultBuilder> resultBuilders;
	private Map<String, Map<String,LegacyFetchBuilder>> legacyFetchBuilders;

	@Override
	public int getNumberOfResultBuilders() {
		return resultBuilders == null ? 0 : resultBuilders.size();
	}

	@Override
	public void visitResultBuilders(BiConsumer<Integer, ResultBuilder> resultBuilderConsumer) {
		if ( resultBuilders == null ) {
			return;
		}

		for ( int i = 0; i < resultBuilders.size(); i++ ) {
			resultBuilderConsumer.accept( i, resultBuilders.get( i ) );
		}
	}

	@Override
	public void addResultBuilder(ResultBuilder resultBuilder) {
		if ( resultBuilders == null ) {
			resultBuilders = new ArrayList<>();
		}
		resultBuilders.add( resultBuilder );
	}

	@Override
	public void addLegacyFetchBuilder(LegacyFetchBuilder fetchBuilder) {
		final Map<String, LegacyFetchBuilder> existingFetchBuildersByOwner;

		if ( legacyFetchBuilders == null ) {
			legacyFetchBuilders = new HashMap<>();
			existingFetchBuildersByOwner = null;
		}
		else {
			existingFetchBuildersByOwner = legacyFetchBuilders.get( fetchBuilder.getOwnerAlias() );
		}

		final Map<String, LegacyFetchBuilder> fetchBuildersByOwner;
		if ( existingFetchBuildersByOwner == null ) {
			fetchBuildersByOwner = new HashMap<>();
			legacyFetchBuilders.put( fetchBuilder.getOwnerAlias(), fetchBuildersByOwner );
		}
		else {
			fetchBuildersByOwner = existingFetchBuildersByOwner;
		}

		final LegacyFetchBuilder previousBuilder = fetchBuildersByOwner.put( fetchBuilder.getFetchedAttributeName(), fetchBuilder );
		if ( previousBuilder != null ) {
			// todo (6.0) : error?  log?  nothing?
		}
	}

	@Override
	public JdbcValuesMapping resolve(
			JdbcValuesMetadata jdbcResultsMetadata,
			SessionFactoryImplementor sessionFactory) {
		final List<SqlSelection> sqlSelections = new ArrayList<>( jdbcResultsMetadata.getColumnCount() );

		final int numberOfResults;

		if ( resultBuilders == null ) {
			numberOfResults = jdbcResultsMetadata.getColumnCount();
		}
		else {
			numberOfResults = resultBuilders.size();
		}

		final List<DomainResult<?>> domainResults = new ArrayList<>( numberOfResults );

		for ( int i = 0; i < numberOfResults; i++ ) {
			final ResultBuilder resultBuilder = resultBuilders != null
					? resultBuilders.get( i )
					: null;

			final DomainResult<?> domainResult;
			if ( resultBuilder == null ) {
				domainResult = makeImplicitDomainResult(
						i,
						sqlSelections::add,
						jdbcResultsMetadata,
						sessionFactory
				);
			}
			else {
				domainResult = resultBuilder.buildReturn(
						jdbcResultsMetadata,
						(ownerAlias, fetchName) -> {
							if ( legacyFetchBuilders == null ) {
								return null;
							}

							final Map<String, LegacyFetchBuilder> fetchBuildersForOwner = legacyFetchBuilders.get(
									ownerAlias );
							if ( fetchBuildersForOwner == null ) {
								return null;
							}

							return fetchBuildersForOwner.get( fetchName );
						},
						sqlSelections::add,
						sessionFactory
				);
			}
			domainResults.add( domainResult );
		}

		return new JdbcValuesMappingImpl( sqlSelections, domainResults );
	}

	private DomainResult<?> makeImplicitDomainResult(
			int valuesArrayPosition,
			Consumer<SqlSelection> sqlSelectionConsumer,
			JdbcValuesMetadata jdbcResultsMetadata,
			SessionFactoryImplementor sessionFactory) {
		final int jdbcPosition = valuesArrayPosition + 1;
		final SqlTypeDescriptor sqlTypeDescriptor = jdbcResultsMetadata.resolveSqlTypeDescriptor( jdbcPosition );

		final TypeConfiguration typeConfiguration = sessionFactory.getTypeConfiguration();

		final JavaTypeDescriptor<?> javaTypeDescriptor = sqlTypeDescriptor.getJdbcRecommendedJavaTypeMapping( typeConfiguration );

		final BasicType<?> jdbcMapping = typeConfiguration.getBasicTypeRegistry().resolve(
				javaTypeDescriptor,
				sqlTypeDescriptor
		);

		final String name = jdbcResultsMetadata.resolveColumnName( jdbcPosition );

		final SqlSelectionImpl sqlSelection = new SqlSelectionImpl( valuesArrayPosition, jdbcMapping );
		sqlSelectionConsumer.accept( sqlSelection );

		return new BasicResult( valuesArrayPosition, name, jdbcMapping.getJavaTypeDescriptor() );
	}

	@Override
	public NamedResultSetMappingMemento toMemento(String name) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}
}
