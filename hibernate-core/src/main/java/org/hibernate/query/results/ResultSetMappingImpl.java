/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.results;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.hibernate.Incubating;
import org.hibernate.Internal;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.loader.NonUniqueDiscoveredSqlAliasException;
import org.hibernate.metamodel.mapping.BasicValuedMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.named.NamedResultSetMappingMemento;
import org.hibernate.query.results.dynamic.DynamicFetchBuilderLegacy;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.basic.BasicResult;
import org.hibernate.sql.results.graph.entity.EntityResult;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMapping;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMappingProducer;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;
import org.hibernate.type.BasicType;

/**
 * @author Steve Ebersole
 */
@Incubating
@Internal
public class ResultSetMappingImpl implements ResultSetMapping {
	private final String mappingIdentifier;
	private final boolean isDynamic;
	private List<ResultBuilder> resultBuilders;
	private Map<String, Map<String, DynamicFetchBuilderLegacy>> legacyFetchBuilders;

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
			for ( ResultBuilder resultBuilder : original.resultBuilders ) {
				resultBuilders.add( resultBuilder.cacheKeyInstance() );
			}
			this.resultBuilders = resultBuilders;
		}
		if ( !original.isDynamic || original.legacyFetchBuilders == null ) {
			this.legacyFetchBuilders = null;
		}
		else {
			final Map<String, Map<String, DynamicFetchBuilderLegacy>> legacyFetchBuilders = new HashMap<>( original.legacyFetchBuilders.size() );
			for ( Map.Entry<String, Map<String, DynamicFetchBuilderLegacy>> entry : original.legacyFetchBuilders.entrySet() ) {
				final Map<String, DynamicFetchBuilderLegacy> newValue = new HashMap<>( entry.getValue().size() );
				for ( Map.Entry<String, DynamicFetchBuilderLegacy> builderEntry : entry.getValue().entrySet() ) {
					newValue.put( builderEntry.getKey(), builderEntry.getValue().cacheKeyInstance() );
				}
				legacyFetchBuilders.put( entry.getKey(), newValue );
			}
			this.legacyFetchBuilders = legacyFetchBuilders;
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
		if ( resultBuilders == null ) {
			return Collections.emptyList();
		}
		return Collections.unmodifiableList( resultBuilders );
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
	public void visitLegacyFetchBuilders(Consumer<DynamicFetchBuilderLegacy> resultBuilderConsumer) {
		if ( legacyFetchBuilders == null ) {
			return;
		}

		for ( Map.Entry<String, Map<String, DynamicFetchBuilderLegacy>> entry : legacyFetchBuilders.entrySet() ) {
			for ( DynamicFetchBuilderLegacy fetchBuilder : entry.getValue().values() ) {
				resultBuilderConsumer.accept( fetchBuilder );
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
	public void removeResultBuilder(ResultBuilder resultBuilder) {
		resultBuilders.remove( resultBuilder );
	}

	@Override
	public void addLegacyFetchBuilder(DynamicFetchBuilderLegacy fetchBuilder) {
		final Map<String, DynamicFetchBuilderLegacy> existingFetchBuildersByOwner;

		if ( legacyFetchBuilders == null ) {
			legacyFetchBuilders = new HashMap<>();
			existingFetchBuildersByOwner = null;
		}
		else {
			existingFetchBuildersByOwner = legacyFetchBuilders.get( fetchBuilder.getOwnerAlias() );
		}

		final Map<String, DynamicFetchBuilderLegacy> fetchBuildersByOwner;
		if ( existingFetchBuildersByOwner == null ) {
			fetchBuildersByOwner = new HashMap<>();
			legacyFetchBuilders.put( fetchBuilder.getOwnerAlias(), fetchBuildersByOwner );
		}
		else {
			fetchBuildersByOwner = existingFetchBuildersByOwner;
		}

		final DynamicFetchBuilderLegacy previousBuilder = fetchBuildersByOwner.put( fetchBuilder.getFetchableName(), fetchBuilder );
		if ( previousBuilder != null ) {
			// todo (6.0) : error?  log?  nothing?
		}
	}

	@Override
	public void addAffectedTableNames(Set<String> affectedTableNames, SessionFactoryImplementor sessionFactory) {
		if ( StringHelper.isEmpty( mappingIdentifier ) ) {
			return;
		}

		final EntityPersister entityDescriptor = sessionFactory.getRuntimeMetamodels()
				.getMappingMetamodel()
				.findEntityDescriptor( mappingIdentifier );
		if ( entityDescriptor == null ) {
			return;
		}

		Collections.addAll( affectedTableNames, (String[]) entityDescriptor.getQuerySpaces() );
	}

	@Override
	public JdbcValuesMapping resolve(
			JdbcValuesMetadata jdbcResultsMetadata,
			LoadQueryInfluencers loadQueryInfluencers,
			SessionFactoryImplementor sessionFactory) {

		final int numberOfResults;
		final int rowSize = jdbcResultsMetadata.getColumnCount();

		if ( resultBuilders == null ) {
			numberOfResults = rowSize;
		}
		else {
			numberOfResults = resultBuilders.size();
		}

		final List<SqlSelection> sqlSelections = new ArrayList<>( rowSize );
		final List<DomainResult<?>> domainResults = new ArrayList<>( numberOfResults );

		final DomainResultCreationStateImpl creationState = new DomainResultCreationStateImpl(
				mappingIdentifier,
				jdbcResultsMetadata,
				legacyFetchBuilders,
				sqlSelections::add,
				loadQueryInfluencers,
				sessionFactory
		);

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
				domainResult = resultBuilder.buildResult(
						jdbcResultsMetadata,
						domainResults.size(),
						creationState.getLegacyFetchResolver()::resolve,
						creationState
				);
			}

			if ( domainResult.containsAnyNonScalarResults() ) {
				creationState.disallowPositionalSelections();
			}

			domainResults.add( domainResult );
		}
		// We only need this check when we actually have result builders
		// As people should be able to just run native queries and work with tuples
		if ( resultBuilders != null ) {
			final Set<String> knownDuplicateAliases = new TreeSet<>( String.CASE_INSENSITIVE_ORDER );
			if ( resultBuilders.size() == 1 && domainResults.size()  == 1 && domainResults.get( 0 ) instanceof EntityResult entityResult ) {
				// Special case for result set mappings that just fetch a single polymorphic entity
				final EntityPersister persister = entityResult.getReferencedMappingContainer().getEntityPersister();
				final boolean polymorphic = persister.getEntityMetamodel().isPolymorphic();
				// We only need to check for duplicate aliases if we have join fetches,
				// otherwise we assume that even if there are duplicate aliases, the values are equivalent.
				// If we don't do that, there is no way to fetch joined inheritance entities
				if ( polymorphic && ( legacyFetchBuilders == null || legacyFetchBuilders.isEmpty() )
						&& !entityResult.hasJoinFetches() ) {
					final Set<String> aliases = new TreeSet<>( String.CASE_INSENSITIVE_ORDER );
					for ( String[] columns : persister.getConstraintOrderedTableKeyColumnClosure() ) {
						addColumns( aliases, knownDuplicateAliases, columns );
					}
					addColumn( aliases, knownDuplicateAliases, persister.getDiscriminatorColumnName() );
					addColumn( aliases, knownDuplicateAliases, persister.getVersionColumnName() );
					for (int i = 0; i < persister.countSubclassProperties(); i++ ) {
						addColumns(
								aliases,
								knownDuplicateAliases,
								persister.getSubclassPropertyColumnNames( i )
						);
					}
				}
			}
			final String[] aliases = new String[rowSize];
			final Map<String, Boolean> aliasHasDuplicates = new HashMap<>( rowSize );
			for ( int i = 0; i < rowSize; i++ ) {
				aliasHasDuplicates.compute(
						aliases[i] = jdbcResultsMetadata.resolveColumnName( i + 1 ),
						(k, v) -> v == null ? Boolean.FALSE : Boolean.TRUE
				);
			}
			// Only check for duplicates for the selections that we actually use
			for ( SqlSelection sqlSelection : sqlSelections ) {
				final String alias = aliases[sqlSelection.getValuesArrayPosition()];
				if ( !knownDuplicateAliases.contains( alias ) && aliasHasDuplicates.get( alias ) == Boolean.TRUE ) {
					throw new NonUniqueDiscoveredSqlAliasException(
							"Encountered a duplicated sql alias [" + alias + "] during auto-discovery of a native-sql query"
					);
				}
			}
		}

		return new JdbcValuesMappingImpl(
				sqlSelections,
				domainResults,
				rowSize,
				creationState.getRegisteredLockModes()
		);
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
		final BasicType<?> jdbcMapping = jdbcResultsMetadata.resolveType( jdbcPosition, null, sessionFactory );

		final String name = jdbcResultsMetadata.resolveColumnName( jdbcPosition );

		final ResultSetMappingSqlSelection sqlSelection = new ResultSetMappingSqlSelection( valuesArrayPosition, (BasicValuedMapping) jdbcMapping );
		sqlSelectionConsumer.accept( sqlSelection );

		return new BasicResult(
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
	public JdbcValuesMappingProducer cacheKeyInstance() {
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
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		final ResultSetMappingImpl that = (ResultSetMappingImpl) o;
		if ( isDynamic ) {
			return that.isDynamic
					&& Objects.equals( mappingIdentifier, that.mappingIdentifier )
					&& Objects.equals( resultBuilders, that.resultBuilders )
					&& Objects.equals( legacyFetchBuilders, that.legacyFetchBuilders );
		}
		else {
			return !that.isDynamic && mappingIdentifier != null && mappingIdentifier.equals( that.mappingIdentifier );
		}
	}
}
