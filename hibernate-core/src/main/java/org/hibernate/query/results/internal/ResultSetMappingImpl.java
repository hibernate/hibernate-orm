/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.results.internal;

import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.loader.NonUniqueDiscoveredSqlAliasException;
import org.hibernate.metamodel.mapping.BasicValuedMapping;
import org.hibernate.query.named.NamedResultSetMappingMemento;
import org.hibernate.query.results.spi.LegacyFetchBuilder;
import org.hibernate.query.results.spi.ResultBuilder;
import org.hibernate.query.results.spi.ResultSetMapping;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.Fetchable;
import org.hibernate.sql.results.graph.basic.BasicResult;
import org.hibernate.sql.results.graph.entity.EntityResult;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMapping;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;
import org.hibernate.type.BasicType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static java.util.Collections.addAll;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static org.hibernate.internal.util.StringHelper.isEmpty;

/**
 * ResultSetMapping implementation used while building
 * {@linkplain ResultSetMapping} references.
 *
 * @author Steve Ebersole
 */
public class ResultSetMappingImpl implements ResultSetMapping {
	private final String mappingIdentifier;
	private final boolean isDynamic;
	private List<ResultBuilder> resultBuilders;
	private Map<String, Map<Fetchable, LegacyFetchBuilder>> legacyFetchBuilders;

	public ResultSetMappingImpl(String mappingIdentifier) {
		this( mappingIdentifier, false );
	}

	public ResultSetMappingImpl(String mappingIdentifier, boolean isDynamic) {
		this.mappingIdentifier = mappingIdentifier;
		this.isDynamic = isDynamic;
	}

	private ResultSetMappingImpl(ResultSetMappingImpl original) {
		this.mappingIdentifier = original.mappingIdentifier;
		this.isDynamic = original.isDynamic;
		if ( !original.isDynamic || original.resultBuilders == null ) {
			this.resultBuilders = null;
		}
		else {
			final List<ResultBuilder> resultBuilders = new ArrayList<>( original.resultBuilders.size() );
			for ( var resultBuilder : original.resultBuilders ) {
				resultBuilders.add( resultBuilder.cacheKeyInstance() );
			}
			this.resultBuilders = resultBuilders;
		}
		if ( !original.isDynamic || original.legacyFetchBuilders == null ) {
			this.legacyFetchBuilders = null;
		}
		else {
			final Map<String, Map<Fetchable, LegacyFetchBuilder>> builders =
					new HashMap<>( original.legacyFetchBuilders.size() );
			for ( var entry : original.legacyFetchBuilders.entrySet() ) {
				final Map<Fetchable, LegacyFetchBuilder> newValue =
						new HashMap<>( entry.getValue().size() );
				for ( var builderEntry : entry.getValue().entrySet() ) {
					newValue.put( builderEntry.getKey(),
							builderEntry.getValue().cacheKeyInstance() );
				}
				builders.put( entry.getKey(), newValue );
			}
			this.legacyFetchBuilders = builders;
		}
	}

	@Override
	public String getMappingIdentifier(){
		return mappingIdentifier;
	}

	@Override
	public boolean isDynamic() {
		return isDynamic;
	}

	@Override
	public int getNumberOfResultBuilders() {
		return resultBuilders == null ? 0 : resultBuilders.size();
	}

	public List<ResultBuilder> getResultBuilders() {
		return resultBuilders == null
				? emptyList()
				: unmodifiableList( resultBuilders );
	}

	@Override
	public void visitResultBuilders(BiConsumer<Integer, ResultBuilder> resultBuilderConsumer) {
		if ( resultBuilders != null ) {
			for ( int i = 0; i < resultBuilders.size(); i++ ) {
				resultBuilderConsumer.accept( i, resultBuilders.get( i ) );
			}
		}
	}

	@Override
	public void visitLegacyFetchBuilders(Consumer<LegacyFetchBuilder> resultBuilderConsumer) {
		if ( legacyFetchBuilders != null ) {
			for ( var entry : legacyFetchBuilders.entrySet() ) {
				for ( LegacyFetchBuilder fetchBuilder : entry.getValue().values() ) {
					resultBuilderConsumer.accept( fetchBuilder );
				}
			}
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
		final Map<Fetchable, LegacyFetchBuilder> existingFetchBuildersByOwner;

		if ( legacyFetchBuilders == null ) {
			legacyFetchBuilders = new HashMap<>();
			existingFetchBuildersByOwner = null;
		}
		else {
			existingFetchBuildersByOwner = legacyFetchBuilders.get( fetchBuilder.getOwnerAlias() );
		}

		final Map<Fetchable, LegacyFetchBuilder> fetchBuildersByOwner;
		if ( existingFetchBuildersByOwner == null ) {
			fetchBuildersByOwner = new HashMap<>();
			legacyFetchBuilders.put( fetchBuilder.getOwnerAlias(), fetchBuildersByOwner );
		}
		else {
			fetchBuildersByOwner = existingFetchBuildersByOwner;
		}

		fetchBuildersByOwner.put( fetchBuilder.getFetchable(), fetchBuilder );
	}

	@Override
	public void addAffectedTableNames(Set<String> affectedTableNames, SessionFactoryImplementor sessionFactory) {
		if ( !isEmpty( mappingIdentifier ) ) {
			final var entityDescriptor =
					sessionFactory.getMappingMetamodel()
							.findEntityDescriptor( mappingIdentifier );
			if ( entityDescriptor != null ) {
				addAll( affectedTableNames, (String[]) entityDescriptor.getQuerySpaces() );
			}
		}
	}

	@Override
	public JdbcValuesMapping resolve(
			JdbcValuesMetadata jdbcResultsMetadata,
			LoadQueryInfluencers loadQueryInfluencers,
			SessionFactoryImplementor sessionFactory) {

		final int rowSize = jdbcResultsMetadata.getColumnCount();
		final int numberOfResults = resultBuilders == null ? rowSize : resultBuilders.size();

		final List<SqlSelection> sqlSelections = new ArrayList<>( rowSize );

		final var creationState = new DomainResultCreationStateImpl(
				mappingIdentifier,
				jdbcResultsMetadata,
				legacyFetchBuilders,
				sqlSelections::add,
				loadQueryInfluencers,
				true,
				sessionFactory
		);

		final var domainResults =
				collectDomainResults( jdbcResultsMetadata, sessionFactory,
						numberOfResults, sqlSelections, creationState );

		// We only need this check when we actually have result builders
		// As people should be able to just run native queries and work with tuples
		if ( resultBuilders != null ) {
			checkDuplicateAliases( jdbcResultsMetadata, domainResults, rowSize, sqlSelections );
		}

		return new JdbcValuesMappingImpl(
				sqlSelections,
				domainResults,
				rowSize,
				creationState.getRegisteredLockModes()
		);
	}

	private void checkDuplicateAliases(
			JdbcValuesMetadata jdbcResultsMetadata,
			List<DomainResult<?>> domainResults,
			int rowSize,
			List<SqlSelection> sqlSelections) {
		final Set<String> knownDuplicateAliases = new TreeSet<>( String.CASE_INSENSITIVE_ORDER );
		if ( resultBuilders.size() == 1 && domainResults.size() == 1
			&& domainResults.get( 0 ) instanceof EntityResult<?> entityResult ) {
			// Special case for result set mappings that just fetch a single polymorphic entity
			final var persister = entityResult.getReferencedMappingContainer().getEntityPersister();
			final boolean polymorphic = persister.isPolymorphic();
			// We only need to check for duplicate aliases if we have join fetches,
			// otherwise we assume that even if there are duplicate aliases, the values are equivalent.
			// If we don't do that, there is no way to fetch joined inheritance entities
			if ( polymorphic
					&& ( legacyFetchBuilders == null || legacyFetchBuilders.isEmpty() )
					&& !entityResult.hasJoinFetches() ) {
				final Set<String> aliases = new TreeSet<>( String.CASE_INSENSITIVE_ORDER );
				for ( var columns : persister.getConstraintOrderedTableKeyColumnClosure() ) {
					addColumns( aliases, knownDuplicateAliases, columns );
				}
				addColumn( aliases, knownDuplicateAliases, persister.getDiscriminatorColumnName() );
				addColumn( aliases, knownDuplicateAliases, persister.getVersionColumnName() );
				for (int i = 0; i < persister.countSubclassProperties(); i++ ) {
					addColumns( aliases, knownDuplicateAliases,
							persister.getSubclassPropertyColumnNames( i ) );
				}
			}
		}
		final var aliases = new String[rowSize];
		final Map<String, Boolean> aliasHasDuplicates = new HashMap<>( rowSize );
		for ( int i = 0; i < rowSize; i++ ) {
			aliasHasDuplicates.compute(
					aliases[i] = jdbcResultsMetadata.resolveColumnName( i + 1 ),
					(k, v) -> v == null ? Boolean.FALSE : Boolean.TRUE
			);
		}
		// Only check for duplicates for the selections that we actually use
		for ( var sqlSelection : sqlSelections ) {
			final String alias = aliases[sqlSelection.getValuesArrayPosition()];
			if ( !knownDuplicateAliases.contains( alias )
					&& aliasHasDuplicates.get( alias ) == Boolean.TRUE ) {
				throw new NonUniqueDiscoveredSqlAliasException(
						"Encountered a duplicated sql alias [" + alias + "] during auto-discovery of a native-sql query"
				);
			}
		}
	}

	private List<DomainResult<?>> collectDomainResults(
			JdbcValuesMetadata jdbcResultsMetadata,
			SessionFactoryImplementor sessionFactory,
			int numberOfResults,
			List<SqlSelection> sqlSelections,
			DomainResultCreationStateImpl creationState) {
		final List<DomainResult<?>> domainResults = new ArrayList<>( numberOfResults );
		for ( int i = 0; i < numberOfResults; i++ ) {
			final var domainResult =
					buildDomainResult(
							jdbcResultsMetadata,
							sessionFactory,
							resultBuilders == null
									? null
									: resultBuilders.get( i ),
							i,
							sqlSelections,
							domainResults,
							creationState
					);
			if ( domainResult.containsAnyNonScalarResults() ) {
				creationState.disallowPositionalSelections();
			}
			domainResults.add( domainResult );
		}
		return domainResults;
	}

	private DomainResult<?> buildDomainResult(
			JdbcValuesMetadata jdbcResultsMetadata,
			SessionFactoryImplementor sessionFactory,
			ResultBuilder resultBuilder,
			int i,
			List<SqlSelection> sqlSelections,
			List<DomainResult<?>> domainResults,
			DomainResultCreationStateImpl creationState) {
		if ( resultBuilder == null ) {
			return makeImplicitDomainResult(
					i,
					sqlSelections::add,
					jdbcResultsMetadata,
					sessionFactory
			);
		}
		else {
			return resultBuilder.buildResult(
					jdbcResultsMetadata,
					domainResults.size(),
					creationState
			);
		}
	}

	private static void addColumns(Set<String> aliases, Set<String> knownDuplicateAliases, String[] columns) {
		for ( int i = 0; i < columns.length; i++ ) {
			addColumn( aliases, knownDuplicateAliases, columns[i] );
		}
	}

	private static void addColumn(Set<String> aliases, Set<String> knownDuplicateAliases, String column) {
		if ( column != null && !aliases.add( column ) ) {
			knownDuplicateAliases.add( column );
		}
	}

	private DomainResult<?> makeImplicitDomainResult(
			int valuesArrayPosition,
			Consumer<SqlSelection> sqlSelectionConsumer,
			JdbcValuesMetadata jdbcResultsMetadata,
			SessionFactoryImplementor sessionFactory) {
		final int jdbcPosition = valuesArrayPosition + 1;
		final BasicType<?> jdbcMapping =
				jdbcResultsMetadata.resolveType( jdbcPosition, null,
						sessionFactory.getTypeConfiguration() );

		final String name = jdbcResultsMetadata.resolveColumnName( jdbcPosition );

		final var sqlSelection =
				new ResultSetMappingSqlSelection( valuesArrayPosition,
						(BasicValuedMapping) jdbcMapping );
		sqlSelectionConsumer.accept( sqlSelection );

		return new BasicResult<>(
				valuesArrayPosition,
				name,
				jdbcMapping,
				null,
				false,
				false
		);
	}

	@Override
	public NamedResultSetMappingMemento toMemento(String name) {
		throw new UnsupportedOperationException();
	}

	@Override
	public ResultSetMapping cacheKeyInstance() {
		return new ResultSetMappingImpl( this );
	}

	@Override
	public int hashCode() {
		if ( isDynamic ) {
			int result = mappingIdentifier != null ? mappingIdentifier.hashCode() : 0;
			result = 31 * result + ( resultBuilders != null ? resultBuilders.hashCode() : 0 );
			result = 31 * result + ( legacyFetchBuilders != null ? legacyFetchBuilders.hashCode() : 0 );
			return result;
		}
		else {
			return mappingIdentifier.hashCode();
		}
	}

	@Override
	public boolean equals(Object object) {
		if ( this == object ) {
			return true;
		}
		else if ( !( object instanceof ResultSetMappingImpl that ) ) {
			return false;
		}
		else if ( isDynamic ) {
			return that.isDynamic
				&& Objects.equals( this.mappingIdentifier, that.mappingIdentifier )
				&& Objects.equals( this.resultBuilders, that.resultBuilders )
				&& Objects.equals( this.legacyFetchBuilders, that.legacyFetchBuilders );
		}
		else {
			return !that.isDynamic
				&& mappingIdentifier != null
				&& mappingIdentifier.equals( that.mappingIdentifier );
		}
	}
}
