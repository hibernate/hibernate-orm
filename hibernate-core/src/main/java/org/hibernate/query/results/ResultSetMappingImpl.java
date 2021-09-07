/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.results;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.hibernate.Incubating;
import org.hibernate.Internal;
import org.hibernate.LockMode;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.loader.NonUniqueDiscoveredSqlAliasException;
import org.hibernate.metamodel.mapping.BasicValuedMapping;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.named.NamedResultSetMappingMemento;
import org.hibernate.query.results.dynamic.DynamicFetchBuilderLegacy;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.basic.BasicFetch;
import org.hibernate.sql.results.graph.basic.BasicResult;
import org.hibernate.sql.results.graph.embeddable.EmbeddableResultGraphNode;
import org.hibernate.sql.results.graph.entity.EntityResult;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMapping;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;
import org.hibernate.type.BasicType;

/**
 * @author Steve Ebersole
 */
@Incubating
@Internal
public class ResultSetMappingImpl implements ResultSetMapping {
	private final String mappingIdentifier;

	private List<ResultBuilder> resultBuilders;
	private Map<String, Map<String, DynamicFetchBuilderLegacy>> legacyFetchBuilders;

	public ResultSetMappingImpl(String mappingIdentifier) {
		this.mappingIdentifier = mappingIdentifier;
	}

	@Override
	public int getNumberOfResultBuilders() {
		return resultBuilders == null ? 0 : resultBuilders.size();
	}

	public List<ResultBuilder> getResultBuilders() {
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

	public String getMappingIdentifier(){
		return mappingIdentifier;
	}

	@Override
	public void addAffectedTableNames(Set<String> affectedTableNames, SessionFactoryImplementor sessionFactory) {
		if ( StringHelper.isEmpty( mappingIdentifier ) ) {
			return;
		}
		EntityPersister entityDescriptor = sessionFactory.getMetamodel().findEntityDescriptor( mappingIdentifier );
		if ( entityDescriptor == null ) {
			return;
		}

		Collections.addAll( affectedTableNames, (String[]) entityDescriptor.getQuerySpaces());
	}

	@Override
	public JdbcValuesMapping resolve(
			JdbcValuesMetadata jdbcResultsMetadata,
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
			if ( resultBuilders.size() == 1 && domainResults.size()  == 1 && domainResults.get( 0 ) instanceof EntityResult ) {
				// Special case for result set mappings that just fetch a single polymorphic entity
				final EntityResult entityResult = (EntityResult) domainResults.get( 0 );
				final boolean polymorphic = entityResult.getReferencedMappingContainer()
						.getEntityPersister()
						.getEntityMetamodel()
						.isPolymorphic();
				// We only need to check for duplicate aliases if we have join fetches,
				// otherwise we assume that even if there are duplicate aliases, the values are equivalent.
				// If we don't do that, there is no way to fetch joined inheritance entities
				if ( polymorphic && ( legacyFetchBuilders == null || legacyFetchBuilders.isEmpty() )
						&& !hasJoinFetches( entityResult.getFetches() ) ) {
					final Set<String> aliases = new TreeSet<>( String.CASE_INSENSITIVE_ORDER );
					final AbstractEntityPersister entityPersister = (AbstractEntityPersister) entityResult.getReferencedMappingContainer()
							.getEntityPersister();
					for ( String[] columns : entityPersister.getContraintOrderedTableKeyColumnClosure() ) {
						addColumns( aliases, knownDuplicateAliases, columns );
					}
					addColumn( aliases, knownDuplicateAliases, entityPersister.getDiscriminatorColumnName() );
					addColumn( aliases, knownDuplicateAliases, entityPersister.getVersionColumnName() );
					for ( int i = 0; i < entityPersister.countSubclassProperties(); i++ ) {
						addColumns(
								aliases,
								knownDuplicateAliases,
								entityPersister.getSubclassPropertyColumnNames( i )
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
		final Map<String, LockMode> registeredLockModes = creationState.getRegisteredLockModes();
		return new JdbcValuesMappingImpl( sqlSelections, domainResults, rowSize, registeredLockModes );
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

	private static boolean hasJoinFetches(List<Fetch> fetches) {
		for ( int i = 0; i < fetches.size(); i++ ) {
			final Fetch fetch = fetches.get( i );
			if ( fetch instanceof BasicFetch<?> || fetch.getTiming() == FetchTiming.DELAYED ) {
				// That's fine
			}
			else if ( fetch instanceof EmbeddableResultGraphNode ) {
				// Check all these fetches as well
				if ( hasJoinFetches( ( (EmbeddableResultGraphNode) fetch ).getFetches() ) ) {
					return true;
				}
			}
			else {
				return true;
			}
		}
		return false;
	}

	private DomainResult<?> makeImplicitDomainResult(
			int valuesArrayPosition,
			Consumer<SqlSelection> sqlSelectionConsumer,
			JdbcValuesMetadata jdbcResultsMetadata,
			SessionFactoryImplementor sessionFactory) {
		final int jdbcPosition = valuesArrayPosition + 1;
		final BasicType<?> jdbcMapping = jdbcResultsMetadata.resolveType( jdbcPosition, null );

		final String name = jdbcResultsMetadata.resolveColumnName( jdbcPosition );

		final SqlSelectionImpl sqlSelection = new SqlSelectionImpl( valuesArrayPosition, (BasicValuedMapping) jdbcMapping );
		sqlSelectionConsumer.accept( sqlSelection );

		return new BasicResult( valuesArrayPosition, name, jdbcMapping.getJavaTypeDescriptor() );
	}

	@Override
	public NamedResultSetMappingMemento toMemento(String name) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}
}
