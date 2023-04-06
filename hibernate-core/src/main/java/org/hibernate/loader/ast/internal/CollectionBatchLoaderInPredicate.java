/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.loader.ast.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.LockOptions;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.CollectionKey;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.SubselectFetch;
import org.hibernate.internal.util.MutableInteger;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.loader.ast.spi.CollectionBatchLoader;
import org.hibernate.loader.ast.spi.SqlArrayMultiLoader;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.exec.internal.JdbcParameterBindingsImpl;
import org.hibernate.sql.exec.spi.JdbcOperationQuerySelect;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.results.internal.RowTransformerStandardImpl;
import org.hibernate.sql.results.spi.ListResultsConsumer;

import static org.hibernate.loader.ast.internal.MultiKeyLoadLogging.MULTI_KEY_LOAD_DEBUG_ENABLED;
import static org.hibernate.loader.ast.internal.MultiKeyLoadLogging.MULTI_KEY_LOAD_LOGGER;

/**
 * CollectionLoader for batch fetching using a SQL IN predicate
 *
 * @author Steve Ebersole
 */
public class CollectionBatchLoaderInPredicate
		extends AbstractCollectionBatchLoader
		implements CollectionBatchLoader, SqlArrayMultiLoader {
	private final int keyColumnCount;
	private final int sqlBatchSize;
	private final List<JdbcParameter> jdbcParameters;
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
		if ( MULTI_KEY_LOAD_DEBUG_ENABLED ) {
			MULTI_KEY_LOAD_LOGGER.debugf(
					"Using IN-predicate batch fetching strategy for collection `%s` : %s (%s)",
					attributeMapping.getNavigableRole().getFullPath(),
					sqlBatchSize,
					domainBatchSize
			);
		}

		this.jdbcParameters = new ArrayList<>();
		this.sqlAst = LoaderSelectBuilder.createSelect(
				attributeMapping,
				null,
				attributeMapping.getKeyDescriptor(),
				null,
				sqlBatchSize,
				influencers,
				LockOptions.NONE,
				jdbcParameters::add,
				sessionFactory
		);
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
		if ( MULTI_KEY_LOAD_DEBUG_ENABLED ) {
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

		initializeKeys( key, keysToInitialize, nonNullCounter.get(), session );

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
			List<T> keysToInitialize,
			int nonNullKeysToInitializeCount,
			SharedSessionContractImplementor session) {
		if ( MULTI_KEY_LOAD_DEBUG_ENABLED ) {
			MULTI_KEY_LOAD_LOGGER.debugf(
					"Collection keys to batch-fetch initialize (`%s#%s`) %s",
					getLoadable().getNavigableRole().getFullPath(),
					key,
					keysToInitialize
			);
		}

		int numberOfKeysLeft = nonNullKeysToInitializeCount;
		int start = 0;
		while ( numberOfKeysLeft > 0 ) {
			if ( MULTI_KEY_LOAD_DEBUG_ENABLED ) {
				MULTI_KEY_LOAD_LOGGER.debugf(
						"Processing batch-fetch chunk (`%s#%s`) %s - %s",
						getLoadable().getNavigableRole().getFullPath(),
						key,
						start,
						start + (sqlBatchSize-1) );
			}
			initializeChunk( keysToInitialize, start, session );

			start += sqlBatchSize;
			numberOfKeysLeft -= sqlBatchSize;
		}

	}
	private <T> void initializeChunk(
			List<T> keysToInitialize,
			int startIndex,
			SharedSessionContractImplementor session) {
		final int parameterCount = sqlBatchSize * keyColumnCount;

		final JdbcParameterBindings jdbcParameterBindings = new JdbcParameterBindingsImpl( parameterCount );
		final MutableInteger nonNullCounter = new MutableInteger();
		int bindCount = 0;
		for ( int i = 0; i < sqlBatchSize; i++ ) {
			final int keyPosition = i + startIndex;
			final Object value;
			if ( keyPosition >= keysToInitialize.size() ) {
				value = null;
			}
			else {
				value = keysToInitialize.get(keyPosition);
			}
			if ( value != null ) {
				nonNullCounter.increment();
			}
			bindCount += jdbcParameterBindings.registerParametersForEachJdbcValue(
					value,
					bindCount,
					getLoadable().getKeyDescriptor(),
					jdbcParameters,
					session
			);
		}
		assert bindCount == jdbcParameters.size();

		if ( nonNullCounter.get() == 0 ) {
			// there are no non-null keys in the chunk
			return;
		}

		final SubselectFetch.RegistrationHandler subSelectFetchableKeysHandler = SubselectFetch.createRegistrationHandler(
				session.getPersistenceContext().getBatchFetchQueue(),
				sqlAst,
				Collections.emptyList(),
				jdbcParameterBindings
		);

		session.getFactory().getJdbcServices().getJdbcSelectExecutor().list(
				jdbcSelect,
				jdbcParameterBindings,
				new ExecutionContextWithSubselectFetchHandler( session, subSelectFetchableKeysHandler ),
				RowTransformerStandardImpl.instance(),
				ListResultsConsumer.UniqueSemantic.FILTER
		);

		for ( int i = 0; i < sqlBatchSize; i++ ) {
			final int keyPosition = i + startIndex;
			if ( keyPosition < keysToInitialize.size() ) {
				final T keyToInitialize = keysToInitialize.get( keyPosition );
				finishInitializingKey( keyToInitialize, session );
			}
		}
	}

}
