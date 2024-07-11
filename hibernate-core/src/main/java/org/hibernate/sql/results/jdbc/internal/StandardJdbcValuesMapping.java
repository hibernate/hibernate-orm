/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.jdbc.internal;

import java.util.List;
import java.util.function.Supplier;

import org.hibernate.LockMode;
import org.hibernate.engine.spi.SessionFactoryImplementor;
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
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMapping;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMappingResolution;

/**
 * @author Steve Ebersole
 */
public class StandardJdbcValuesMapping implements JdbcValuesMapping {

	private final List<SqlSelection> sqlSelections;
	private final List<DomainResult<?>> domainResults;
	private JdbcValuesMappingResolutionImpl resolution;

	public StandardJdbcValuesMapping(
			List<SqlSelection> sqlSelections,
			List<DomainResult<?>> domainResults) {
		this.sqlSelections = sqlSelections;
		this.domainResults = domainResults;
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
	public JdbcValuesMappingResolution resolveAssemblers(SessionFactoryImplementor sessionFactory) {
		final JdbcValuesMappingResolutionImpl resolution = this.resolution;
		if ( resolution != null ) {
			return resolution;
		}
		final AssemblerCreationStateImpl creationState = new AssemblerCreationStateImpl(
				this,
				sessionFactory
		);

		DomainResultAssembler<?>[] domainResultAssemblers = resolveAssemblers( creationState ).toArray(new DomainResultAssembler[0]);
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
		private final SessionFactoryImplementor sessionFactory;
		//custom Map<NavigablePath, Initializer>
		private final NavigablePathMapToInitializer initializerMap = new NavigablePathMapToInitializer();
		private final InitializersList.Builder initializerListBuilder = new InitializersList.Builder();
		private int initializerId;
		boolean hasCollectionInitializers;
		Boolean dynamicInstantiation;
		Boolean containsMultipleCollectionFetches;

		public AssemblerCreationStateImpl(
				JdbcValuesMapping jdbcValuesMapping,
				SessionFactoryImplementor sessionFactory) {
			this.jdbcValuesMapping = jdbcValuesMapping;
			this.sessionFactory = sessionFactory;
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
					if ( domainResult instanceof FetchParent ) {
						collectionFetchesCount += ( (FetchParent) domainResult ).getCollectionFetchesCount();
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
			return sessionFactory;
		}

	}
}
