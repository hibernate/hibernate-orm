/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.loader.ast.internal;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.LockOptions;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.BatchFetchQueue;
import org.hibernate.engine.spi.CollectionKey;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.SubselectFetch;
import org.hibernate.internal.util.MutableInteger;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.loader.ast.spi.CollectionBatchLoader;
import org.hibernate.loader.ast.spi.SqlArrayMultiKeyLoader;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.exec.spi.JdbcOperationQuerySelect;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.exec.spi.JdbcParametersList;

import static org.hibernate.loader.ast.internal.MultiKeyLoadLogging.MULTI_KEY_LOAD_LOGGER;

/**
 * CollectionLoader for batch fetching using a SQL IN predicate
 *
 * @author Steve Ebersole
 */
public class CollectionBatchLoaderInPredicate
		extends AbstractCollectionBatchLoader
		implements CollectionBatchLoader, SqlArrayMultiKeyLoader {
	private final int keyColumnCount;
	private final int sqlBatchSize;
	private final JdbcParametersList jdbcParameters;
	private final SelectStatement sqlAst;
	private final JdbcOperationQuerySelect jdbcSelect;

	private CollectionLoaderSingleKey singleKeyLoader;

	public CollectionBatchLoaderInPredicate(
			int domainBatchSize,
			LoadQueryInfluencers influencers,
			PluralAttributeMapping attributeMapping,
			SessionFactoryImplementor sessionFactory) {
		super( domainBatchSize, influencers, attributeMapping, sessionFactory );

		this.keyColumnCount = attributeMapping.getKeyDescriptor().getJdbcTypeCount();
		this.sqlBatchSize = sessionFactory.getJdbcServices()
				.getDialect()
				.getBatchLoadSizingStrategy()
				.determineOptimalBatchLoadSize( keyColumnCount, domainBatchSize, false );
		if ( MULTI_KEY_LOAD_LOGGER.isDebugEnabled() ) {
			MULTI_KEY_LOAD_LOGGER.debugf(
					"Using IN-predicate batch fetching strategy for collection `%s` : %s (%s)",
					attributeMapping.getNavigableRole().getFullPath(),
					sqlBatchSize,
					domainBatchSize
			);
		}

		final JdbcParametersList.Builder jdbcParametersBuilder = JdbcParametersList.newBuilder();
		this.sqlAst = LoaderSelectBuilder.createSelect(
				attributeMapping,
				null,
				attributeMapping.getKeyDescriptor(),
				null,
				sqlBatchSize,
				influencers,
				LockOptions.NONE,
				jdbcParametersBuilder::add,
				sessionFactory
		);
		this.jdbcParameters = jdbcParametersBuilder.build();
		assert this.jdbcParameters.size() == this.sqlBatchSize * this.keyColumnCount;

		this.jdbcSelect = sessionFactory.getJdbcServices()
				.getJdbcEnvironment()
				.getSqlAstTranslatorFactory()
				.buildSelectTranslator( sessionFactory, sqlAst )
				.translate( JdbcParameterBindings.NO_BINDINGS, QueryOptions.NONE );
	}

	@Override
	public PersistentCollection<?> load(
			Object key,
			SharedSessionContractImplementor session) {
		if ( MULTI_KEY_LOAD_LOGGER.isDebugEnabled() ) {
			MULTI_KEY_LOAD_LOGGER.debugf( "Loading collection `%s#%s` by batch-fetch", getLoadable().getNavigableRole().getFullPath(), key );
		}

		final MutableInteger nonNullCounter = new MutableInteger();
		final ArrayList<Object> keysToInitialize = CollectionHelper.arrayList( getDomainBatchSize() );
		session.getPersistenceContextInternal().getBatchFetchQueue().collectBatchLoadableCollectionKeys(
				getDomainBatchSize(),
				(index, batchableKey) -> {
					keysToInitialize.add( batchableKey );
					if ( batchableKey != null ) {
						nonNullCounter.increment();
					}
				},
				key,
				getLoadable().asPluralAttributeMapping()
		);

		if ( nonNullCounter.get() <= 0 ) {
			throw new IllegalStateException( "Number of non-null collection keys to batch fetch should never be 0" );
		}

		if ( nonNullCounter.get() == 1 ) {
			prepareSingleKeyLoaderIfNeeded();
			return singleKeyLoader.load( key, session );
		}

		initializeKeys( key, keysToInitialize.toArray( keysToInitialize.toArray( new Object[0] ) ), nonNullCounter.get(), session );

		final CollectionKey collectionKey = new CollectionKey( getLoadable().getCollectionDescriptor(), key );
		return session.getPersistenceContext().getCollection( collectionKey );
	}

	private void prepareSingleKeyLoaderIfNeeded() {
		if ( singleKeyLoader == null ) {
			singleKeyLoader = new CollectionLoaderSingleKey( getLoadable(), getInfluencers(), getSessionFactory() );
		}
	}

	private <T> void initializeKeys(
			T key,
			T[] keysToInitialize,
			int nonNullKeysToInitializeCount,
			SharedSessionContractImplementor session) {
		final boolean loggerDebugEnabled = MULTI_KEY_LOAD_LOGGER.isDebugEnabled();
		if ( loggerDebugEnabled ) {
			MULTI_KEY_LOAD_LOGGER.debugf(
					"Collection keys to batch-fetch initialize (`%s#%s`) %s",
					getLoadable().getNavigableRole().getFullPath(),
					key,
					keysToInitialize
			);
		}

		final MultiKeyLoadChunker<T> chunker = new MultiKeyLoadChunker<>(
				sqlBatchSize,
				keyColumnCount,
				getLoadable().getKeyDescriptor(),
				jdbcParameters,
				sqlAst,
				jdbcSelect
		);

		final BatchFetchQueue batchFetchQueue = session.getPersistenceContextInternal().getBatchFetchQueue();

		chunker.processChunks(
				keysToInitialize,
				nonNullKeysToInitializeCount,
				(jdbcParameterBindings, session1) -> {
					// Create a RegistrationHandler for handling any subselect fetches we encounter handling this chunk
					final SubselectFetch.RegistrationHandler registrationHandler = SubselectFetch.createRegistrationHandler(
							batchFetchQueue,
							sqlAst,
							jdbcParameters,
							jdbcParameterBindings
					);
					return new ExecutionContextWithSubselectFetchHandler( session, registrationHandler );
				},
				(key1, relativePosition, absolutePosition) -> {
				},
				(startIndex) -> {
					if ( loggerDebugEnabled ) {
						MULTI_KEY_LOAD_LOGGER.debugf(
								"Processing collection batch-fetch chunk (`%s#%s`) %s - %s",
								getLoadable().getNavigableRole().getFullPath(),
								key,
								startIndex,
								startIndex + (sqlBatchSize-1)
						);
					}
				},
				(startIndex, nonNullElementCount) -> {
					if ( loggerDebugEnabled ) {
						MULTI_KEY_LOAD_LOGGER.debugf(
								"Finishing collection batch-fetch chunk (`%s#%s`) %s - %s (%s)",
								getLoadable().getNavigableRole().getFullPath(),
								key,
								startIndex,
								startIndex + (sqlBatchSize-1),
								nonNullElementCount
						);
					}
					for ( int i = 0; i < nonNullElementCount; i++ ) {
						final int keyPosition = i + startIndex;
						if ( keyPosition < keysToInitialize.length ) {
							final T keyToInitialize = keysToInitialize[keyPosition];
							finishInitializingKey( keyToInitialize, session );
						}
					}
				},
				session
		);
	}

}
