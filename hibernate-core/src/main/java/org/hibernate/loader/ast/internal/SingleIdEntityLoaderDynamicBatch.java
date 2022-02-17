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
import org.hibernate.engine.internal.BatchFetchQueueHelper;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.SubselectFetch;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.spi.QueryOptionsAdapter;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.exec.internal.JdbcParameterBindingsImpl;
import org.hibernate.sql.exec.internal.JdbcSelectExecutorStandardImpl;
import org.hibernate.sql.exec.spi.Callback;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.exec.spi.JdbcSelect;
import org.hibernate.sql.results.graph.entity.LoadingEntityEntry;
import org.hibernate.sql.results.internal.RowTransformerPassThruImpl;
import org.hibernate.sql.results.spi.ListResultsConsumer;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class SingleIdEntityLoaderDynamicBatch<T> extends SingleIdEntityLoaderSupport<T> {
	private static final Logger log = Logger.getLogger( SingleIdEntityLoaderDynamicBatch.class );

	private final int maxBatchSize;

	private SingleIdEntityLoaderStandardImpl<T> singleIdLoader;

	public SingleIdEntityLoaderDynamicBatch(
			EntityMappingType entityDescriptor,
			int maxBatchSize,
			SessionFactoryImplementor sessionFactory) {
		super( entityDescriptor, sessionFactory );
		this.maxBatchSize = maxBatchSize;
	}

	@Override
	public T load(Object pkValue, LockOptions lockOptions, Boolean readOnly, SharedSessionContractImplementor session) {
		return load( pkValue, null, lockOptions, readOnly, session );
	}

	@Override
	public T load(
			Object pkValue,
			Object entityInstance,
			LockOptions lockOptions,
			Boolean readOnly,
			SharedSessionContractImplementor session) {
		final Object[] batchIds = session.getPersistenceContextInternal()
				.getBatchFetchQueue()
				.getBatchLoadableEntityIds( getLoadable(), pkValue, maxBatchSize );

		final int numberOfIds = ArrayHelper.countNonNull( batchIds );
		if ( numberOfIds <= 1 ) {
			initializeSingleIdLoaderIfNeeded( session );

			final T result = singleIdLoader.load( pkValue, entityInstance, lockOptions, readOnly, session );
			if ( result == null ) {
				// There was no entity with the specified ID. Make sure the EntityKey does not remain
				// in the batch to avoid including it in future batches that get executed.
				BatchFetchQueueHelper.removeBatchLoadableEntityKey( pkValue, getLoadable(), session );
			}

			return result;
		}

		final Object[] idsToLoad = new Object[numberOfIds];
		System.arraycopy( batchIds, 0, idsToLoad, 0, numberOfIds );

		if ( log.isDebugEnabled() ) {
			log.debugf( "Batch loading entity [%s] : %s", getLoadable().getEntityName(), idsToLoad );
		}

		final List<JdbcParameter> jdbcParameters = new ArrayList<>();

		final SelectStatement sqlAst = LoaderSelectBuilder.createSelect(
				getLoadable(),
				// null here means to select everything
				null,
				getLoadable().getIdentifierMapping(),
				null,
				numberOfIds,
				session.getLoadQueryInfluencers(),
				lockOptions,
				jdbcParameters::add,
				session.getFactory()
		);

		final SessionFactoryImplementor sessionFactory = session.getFactory();
		final JdbcServices jdbcServices = sessionFactory.getJdbcServices();
		final JdbcEnvironment jdbcEnvironment = jdbcServices.getJdbcEnvironment();
		final SqlAstTranslatorFactory sqlAstTranslatorFactory = jdbcEnvironment.getSqlAstTranslatorFactory();

		final JdbcParameterBindings jdbcParameterBindings = new JdbcParameterBindingsImpl(
				getLoadable().getIdentifierMapping().getJdbcTypeCount()
		);

		int offset = 0;
		for ( int i = 0; i < numberOfIds; i++ ) {
			offset += jdbcParameterBindings.registerParametersForEachJdbcValue(
					idsToLoad[i],
					Clause.WHERE,
					offset,
					getLoadable().getIdentifierMapping(),
					jdbcParameters,
					session
			);
		}
		assert offset == jdbcParameters.size();

		final JdbcSelect jdbcSelect = sqlAstTranslatorFactory
				.buildSelectTranslator( sessionFactory, sqlAst )
				.translate( jdbcParameterBindings, QueryOptions.NONE );

		final SubselectFetch.RegistrationHandler subSelectFetchableKeysHandler = SubselectFetch.createRegistrationHandler(
				session.getPersistenceContext().getBatchFetchQueue(),
				sqlAst,
				jdbcParameters,
				jdbcParameterBindings
		);

		JdbcSelectExecutorStandardImpl.INSTANCE.list(
				jdbcSelect,
				jdbcParameterBindings,
				getExecutionContext(
						pkValue,
						entityInstance,
						readOnly,
						lockOptions,
						session,
						subSelectFetchableKeysHandler
				),
				RowTransformerPassThruImpl.instance(),
				ListResultsConsumer.UniqueSemantic.FILTER
		);

		//noinspection ForLoopReplaceableByForEach
		for ( int i = 0; i < idsToLoad.length; i++ ) {
			final Object id = idsToLoad[i];
			// found or not, remove the key from the batch-fetch queye
			BatchFetchQueueHelper.removeBatchLoadableEntityKey( id, getLoadable(), session );
		}

		final EntityKey entityKey = session.generateEntityKey( pkValue, getLoadable().getEntityPersister() );
		//noinspection unchecked
		return (T) session.getPersistenceContext().getEntity( entityKey );

	}

	private ExecutionContext getExecutionContext(
			Object entityId,
			Object entityInstance,
			Boolean readOnly,
			LockOptions lockOptions,
			SharedSessionContractImplementor session,
			SubselectFetch.RegistrationHandler subSelectFetchableKeysHandler) {
		return new ExecutionContext() {
			@Override
			public SharedSessionContractImplementor getSession() {
				return session;
			}

			@Override
			public Object getEntityInstance() {
				return entityInstance;
			}

			@Override
			public Object getEntityId() {
				return entityId;
			}

			@Override
			public QueryOptions getQueryOptions() {
				return new QueryOptionsAdapter() {
					@Override
					public Boolean isReadOnly() {
						return readOnly;
					}

					@Override
					public LockOptions getLockOptions() {
						return lockOptions;
					}
				};
			}

			@Override
			public String getQueryIdentifier(String sql) {
				return sql;
			}

			@Override
			public void registerLoadingEntityEntry(EntityKey entityKey, LoadingEntityEntry entry) {
				subSelectFetchableKeysHandler.addKey( entityKey, entry );
			}

			@Override
			public QueryParameterBindings getQueryParameterBindings() {
				return QueryParameterBindings.NO_PARAM_BINDINGS;
			}

			@Override
			public Callback getCallback() {
				return null;
			}

		};
	}

	private void initializeSingleIdLoaderIfNeeded(SharedSessionContractImplementor session) {
		if ( singleIdLoader == null ) {
			singleIdLoader = new SingleIdEntityLoaderStandardImpl<>( getLoadable(), session.getFactory() );
			singleIdLoader.prepare();
		}
	}
}
