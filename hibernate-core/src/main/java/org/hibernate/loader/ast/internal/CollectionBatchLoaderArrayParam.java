/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.loader.ast.internal;


import org.hibernate.LockOptions;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.CollectionKey;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.SubselectFetch;
import org.hibernate.internal.build.AllowReflection;
import org.hibernate.loader.ast.spi.CollectionBatchLoader;
import org.hibernate.loader.ast.spi.SqlArrayMultiKeyLoader;
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.exec.internal.JdbcParameterBindingImpl;
import org.hibernate.sql.exec.internal.JdbcParameterBindingsImpl;
import org.hibernate.sql.exec.internal.JdbcParameterImpl;
import org.hibernate.sql.exec.internal.JdbcOperationQuerySelect;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.exec.spi.JdbcParametersList;
import org.hibernate.sql.results.internal.RowTransformerStandardImpl;
import org.hibernate.sql.results.spi.ListResultsConsumer;

import static java.lang.reflect.Array.newInstance;
import static org.hibernate.loader.ast.internal.MultiKeyLoadHelper.hasSingleId;
import static org.hibernate.loader.ast.internal.MultiKeyLoadHelper.trimIdBatch;
import static org.hibernate.loader.ast.internal.MultiKeyLoadLogging.MULTI_KEY_LOAD_LOGGER;
import static org.hibernate.pretty.MessageHelper.collectionInfoString;

/**
 * {@link CollectionBatchLoader} using a SQL {@code ARRAY} parameter to pass the key values.
 *
 * @author Steve Ebersole
 */
public class CollectionBatchLoaderArrayParam
		extends AbstractCollectionBatchLoader
		implements SqlArrayMultiKeyLoader {
	private final Class<?> keyDomainType;
	private final JdbcMapping arrayJdbcMapping;
	private final JdbcParameter jdbcParameter;
	private final SelectStatement sqlSelect;
	private final JdbcOperationQuerySelect jdbcSelectOperation;

	public CollectionBatchLoaderArrayParam(
			int domainBatchSize,
			LoadQueryInfluencers loadQueryInfluencers,
			PluralAttributeMapping attributeMapping,
			SessionFactoryImplementor sessionFactory) {
		super( domainBatchSize, loadQueryInfluencers, attributeMapping, sessionFactory );

		if ( MULTI_KEY_LOAD_LOGGER.isTraceEnabled() ) {
			MULTI_KEY_LOAD_LOGGER.enabledCollectionArray(
					attributeMapping.getNavigableRole().getFullPath(),
					domainBatchSize
			);
		}

		final var keyDescriptor = getLoadable().getKeyDescriptor();
		final var jdbcMapping = keyDescriptor.getSingleJdbcMapping();
		final var jdbcJavaTypeClass = jdbcMapping.getJdbcJavaType().getJavaTypeClass();
		keyDomainType = getKeyType( keyDescriptor.getKeyPart() );

		arrayJdbcMapping = MultiKeyLoadHelper.resolveArrayJdbcMapping(
				jdbcMapping,
				jdbcJavaTypeClass,
				getSessionFactory()
		);

		jdbcParameter = new JdbcParameterImpl( arrayJdbcMapping );
		sqlSelect = LoaderSelectBuilder.createSelectBySingleArrayParameter(
				getLoadable(),
				keyDescriptor.getKeyPart(),
				getInfluencers(),
				new LockOptions(),
				jdbcParameter,
				getSessionFactory()
		);

		final var querySpec = sqlSelect.getQueryPart().getFirstQuerySpec();
		final var tableGroup = querySpec.getFromClause().getRoots().get( 0 );
		attributeMapping.applySoftDeleteRestrictions( tableGroup, querySpec::applyPredicate );

		jdbcSelectOperation = getSessionFactory().getJdbcServices()
				.getJdbcEnvironment()
				.getSqlAstTranslatorFactory()
				.buildSelectTranslator( getSessionFactory(), sqlSelect )
				.translate( JdbcParameterBindings.NO_BINDINGS, QueryOptions.NONE );
	}
	@Override
	public PersistentCollection<?> load(Object keyBeingLoaded, SharedSessionContractImplementor session) {
		final var keyDescriptor = getLoadable().getKeyDescriptor();
		if ( keyDescriptor.isEmbedded() ) {
			assert keyDescriptor.getJdbcTypeCount() == 1;
			return loadEmbeddable( keyBeingLoaded, session, keyDescriptor );
		}
		else {
			return super.load( keyBeingLoaded, session );
		}
	}

	@AllowReflection
	private PersistentCollection<?> loadEmbeddable(
			Object keyBeingLoaded,
			SharedSessionContractImplementor session,
			ForeignKeyDescriptor keyDescriptor) {
		if ( MULTI_KEY_LOAD_LOGGER.isTraceEnabled() ) {
			MULTI_KEY_LOAD_LOGGER.batchFetchingCollection(
					collectionInfoString( getLoadable(), keyBeingLoaded ) );
		}

		final int length = getDomainBatchSize();
		final Object[] keysToInitialize = (Object[]) newInstance(
				jdbcParameter.getExpressionType()
						.getSingleJdbcMapping()
						.getJdbcJavaType()
						.getJavaTypeClass()
						.getComponentType(),
				length
		);
		final Object[] embeddedKeys = new Object[length];
		session.getPersistenceContextInternal().getBatchFetchQueue()
				.collectBatchLoadableCollectionKeys(
						length,
						(index, key) ->
								keyDescriptor.forEachJdbcValue(
										key,
										(i, value, jdbcMapping) -> {
											keysToInitialize[index] = value;
											embeddedKeys[index] = key;
										},
										session
								)
						,
						keyBeingLoaded,
						getLoadable()
				);
		// now trim down the array to the number of keys we found
		final var keys = trimIdBatch( length, keysToInitialize );

		if ( hasSingleId( keys ) ) {
			return singleKeyLoader.load( keyBeingLoaded, session );
		}

		initializeKeys( keyBeingLoaded, keys, session );

		for ( Object initializedKey : embeddedKeys ) {
			if ( initializedKey != null ) {
				finishInitializingKey( initializedKey, session );
			}
		}
		final var collectionKey = new CollectionKey(
				getLoadable().getCollectionDescriptor(),
				keyBeingLoaded
		);
		return session.getPersistenceContext().getCollection( collectionKey );
	}

	@Override
	void initializeKeys(Object key, Object[] keysToInitialize, SharedSessionContractImplementor session) {
		if ( MULTI_KEY_LOAD_LOGGER.isTraceEnabled() ) {
			MULTI_KEY_LOAD_LOGGER.collectionKeysToInitialize(
					collectionInfoString( getLoadable(), key ), keysToInitialize );
		}

		assert jdbcSelectOperation != null;
		assert jdbcParameter != null;

		final var jdbcParameterBindings = new JdbcParameterBindingsImpl(1);
		jdbcParameterBindings.addBinding(
				jdbcParameter,
				new JdbcParameterBindingImpl( arrayJdbcMapping, keysToInitialize )
		);

		session.getJdbcServices().getJdbcSelectExecutor().list(
				jdbcSelectOperation,
				jdbcParameterBindings,
				new ExecutionContextWithSubselectFetchHandler(
						session,
						SubselectFetch.createRegistrationHandler(
								session.getPersistenceContext().getBatchFetchQueue(),
								sqlSelect,
								JdbcParametersList.singleton( jdbcParameter ),
								jdbcParameterBindings
						)
				),
				RowTransformerStandardImpl.instance(),
				ListResultsConsumer.UniqueSemantic.FILTER
		);
	}

	@Override
	void finishInitializingKeys(Object[] keys, SharedSessionContractImplementor session) {
		for ( Object initializedKey : keys ) {
			finishInitializingKey( initializedKey, session );
		}
	}

	@Override
	@AllowReflection
	Object[] resolveKeysToInitialize(Object keyBeingLoaded, SharedSessionContractImplementor session) {
		final var keyDescriptor = getLoadable().getKeyDescriptor();
		if( keyDescriptor.isEmbedded()){
			assert keyDescriptor.getJdbcTypeCount() == 1;
			final int length = getDomainBatchSize();
			final var keysToInitialize = new Object[length];
			session.getPersistenceContextInternal().getBatchFetchQueue()
					.collectBatchLoadableCollectionKeys(
							length,
							(index, key) ->
									keyDescriptor.forEachJdbcValue(
											key,
											(i, value, jdbcMapping) -> {
												keysToInitialize[index] = value;
											},
											session
									)
							,
							keyBeingLoaded,
							getLoadable()
					);
			// now trim down the array to the number of keys we found
			return trimIdBatch( length, keysToInitialize );
		}
		return super.resolveKeysToInitialize( keyBeingLoaded, session );
	}
}
