/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.jdbc.internal;

import java.util.BitSet;
import java.util.List;
import java.util.function.Supplier;

import org.hibernate.LockMode;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.SqlAstCreationContext;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.results.ResultsLogger;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.Initializer;
import org.hibernate.sql.results.graph.InitializerParent;
import org.hibernate.sql.results.graph.InitializerProducer;
import org.hibernate.sql.results.graph.collection.internal.AbstractImmediateCollectionInitializer;
import org.hibernate.sql.results.graph.instantiation.DynamicInstantiationResult;
import org.hibernate.sql.results.internal.InitializersList;
import org.hibernate.sql.results.internal.NavigablePathMapToInitializer;
import org.hibernate.sql.results.internal.SqlSelectionImpl;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMapping;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMappingResolution;

/**
 * @author Steve Ebersole
 */
public class StandardJdbcValuesMapping implements JdbcValuesMapping {

	private final List<SqlSelection> sqlSelections;
	private final List<DomainResult<?>> domainResults;
	private final boolean needsResolve;
	private final int[] valueIndexesToCacheIndexes;
	// Is only meaningful if valueIndexesToCacheIndexes is not null
	// Contains the size of the row to cache, or if the value is negative,
	// represents the inverted index of the single value to cache
	private final int rowToCacheSize;
	private JdbcValuesMappingResolutionImpl resolution;

	public StandardJdbcValuesMapping(
			List<SqlSelection> sqlSelections,
			List<DomainResult<?>> domainResults) {
		this.sqlSelections = sqlSelections;
		this.domainResults = domainResults;

		final int rowSize = sqlSelections.size();
		final BitSet valueIndexesToCache = new BitSet( rowSize );
		for ( DomainResult<?> domainResult : domainResults ) {
			domainResult.collectValueIndexesToCache( valueIndexesToCache );
		}
		final int[] valueIndexesToCacheIndexes = new int[rowSize];
		int cacheIndex = 0;
		boolean needsResolve = false;
		for ( int i = 0; i < valueIndexesToCacheIndexes.length; i++ ) {
			final SqlSelection sqlSelection = sqlSelections.get( i );
			needsResolve = needsResolve
					|| sqlSelection instanceof SqlSelectionImpl selection && selection.needsResolve();
			if ( valueIndexesToCache.get( i ) ) {
				valueIndexesToCacheIndexes[i] = cacheIndex++;
			}
			else {
				valueIndexesToCacheIndexes[i] = -1;
			}
		}

		this.needsResolve = needsResolve;
		this.valueIndexesToCacheIndexes = cacheIndex == 0 ? ArrayHelper.EMPTY_INT_ARRAY : valueIndexesToCacheIndexes;
		this.rowToCacheSize = cacheIndex;
	}

	@Override
	public List<SqlSelection> getSqlSelections() {
		return sqlSelections;
	}

	@Override
	public List<DomainResult<?>> getDomainResults() {
		return domainResults;
	}

	@Override
	public int getRowSize() {
		return sqlSelections.size();
	}

	@Override
	public int[] getValueIndexesToCacheIndexes() {
		return valueIndexesToCacheIndexes;
	}

	@Override
	public int getRowToCacheSize() {
		return rowToCacheSize;
	}

	public boolean needsResolve() {
		return needsResolve;
	}

	@Override
	public JdbcValuesMappingResolution resolveAssemblers(SessionFactoryImplementor sessionFactory) {
		final JdbcValuesMappingResolutionImpl resolution = this.resolution;
		if ( resolution != null ) {
			return resolution;
		}
		final AssemblerCreationStateImpl creationState = new AssemblerCreationStateImpl(
				this,
				sessionFactory.getSqlTranslationEngine()
		);

		DomainResultAssembler<?>[] domainResultAssemblers =
				resolveAssemblers( creationState ).toArray(new DomainResultAssembler[0]);
		creationState.initializerMap.logInitializers();
		return this.resolution = new JdbcValuesMappingResolutionImpl(
				domainResultAssemblers,
				creationState.hasCollectionInitializers,
				creationState.initializerListBuilder.build()
		);
	}

	private List<DomainResultAssembler<?>> resolveAssemblers(AssemblerCreationState creationState) {
		final List<DomainResultAssembler<?>> assemblers = CollectionHelper.arrayList( domainResults.size() );

		//noinspection ForLoopReplaceableByForEach
		for ( int i = 0; i < domainResults.size(); i++ ) {
			final DomainResultAssembler<?> resultAssembler = domainResults.get( i )
					.createResultAssembler( null, creationState );

			assemblers.add( resultAssembler );
		}

		return assemblers;
	}

	@Override
	public LockMode determineDefaultLockMode(String alias, LockMode defaultLockMode) {
		return defaultLockMode;
	}

	private static class AssemblerCreationStateImpl implements AssemblerCreationState {
		private final JdbcValuesMapping jdbcValuesMapping;
		private final SqlAstCreationContext sqlAstCreationContexty;
		//custom Map<NavigablePath, Initializer>
		private final NavigablePathMapToInitializer initializerMap = new NavigablePathMapToInitializer();
		private final InitializersList.Builder initializerListBuilder = new InitializersList.Builder();
		private int initializerId;
		boolean hasCollectionInitializers;
		Boolean dynamicInstantiation;
		Boolean containsMultipleCollectionFetches;

		public AssemblerCreationStateImpl(
				JdbcValuesMapping jdbcValuesMapping,
				SqlAstCreationContext sqlAstCreationContexty) {
			this.jdbcValuesMapping = jdbcValuesMapping;
			this.sqlAstCreationContexty = sqlAstCreationContexty;
		}

		@Override
		public boolean isDynamicInstantiation() {
			if ( dynamicInstantiation == null ) {
				dynamicInstantiation = jdbcValuesMapping.getDomainResults()
						.stream()
						.anyMatch( domainResult -> domainResult instanceof DynamicInstantiationResult );
			}
			return dynamicInstantiation;
		}

		@Override
		public boolean containsMultipleCollectionFetches() {
			if ( containsMultipleCollectionFetches == null ) {
				int collectionFetchesCount = 0;
				for ( DomainResult<?> domainResult : jdbcValuesMapping.getDomainResults() ) {
					if ( domainResult instanceof FetchParent fetchParent ) {
						collectionFetchesCount += fetchParent.getCollectionFetchesCount();
					}
				}
				containsMultipleCollectionFetches = collectionFetchesCount > 1;
			}
			return containsMultipleCollectionFetches;
		}

		@Override
		public int acquireInitializerId() {
			return initializerId++;
		}

		@Override
		public Initializer<?> resolveInitializer(
				NavigablePath navigablePath,
				ModelPart fetchedModelPart,
				Supplier<Initializer<?>> producer) {
			return resolveInitializer(
					navigablePath,
					fetchedModelPart,
					null,
					null,
					(resultGraphNode, parent, creationState) -> producer.get()
			);
		}

		@Override
		public <P extends FetchParent> Initializer<?> resolveInitializer(
				P resultGraphNode,
				InitializerParent<?> parent,
				InitializerProducer<P> producer) {
			return resolveInitializer(
					resultGraphNode.getNavigablePath(),
					resultGraphNode.getReferencedModePart(),
					resultGraphNode,
					parent,
					producer
			);
		}

		public <T extends FetchParent> Initializer<?> resolveInitializer(
				NavigablePath navigablePath,
				ModelPart fetchedModelPart,
				T resultGraphNode,
				InitializerParent<?> parent,
				InitializerProducer<T> producer) {
			final Initializer<?> existing = initializerMap.get( navigablePath );
			if ( existing != null ) {
				if ( fetchedModelPart.getNavigableRole().equals(
						existing.getInitializedPart().getNavigableRole() ) ) {
					ResultsLogger.RESULTS_MESSAGE_LOGGER.tracef(
							"Returning previously-registered initializer : %s",
							existing
					);
					return existing;
				}
			}

			final Initializer<?> initializer = producer.createInitializer( resultGraphNode, parent, this );
			ResultsLogger.RESULTS_MESSAGE_LOGGER.tracef(
					"Registering initializer : %s",
					initializer
			);

			if ( initializer instanceof AbstractImmediateCollectionInitializer ) {
				hasCollectionInitializers = true;
			}
			initializerMap.put( navigablePath, initializer );
			initializerListBuilder.addInitializer( initializer );

			return initializer;
		}

		@Override
		public SqlAstCreationContext getSqlAstCreationContext() {
			return sqlAstCreationContexty;
		}

	}
}
