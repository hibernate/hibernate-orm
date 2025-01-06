/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.loader.ast.internal;

import java.lang.reflect.Array;

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
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.exec.internal.JdbcParameterBindingImpl;
import org.hibernate.sql.exec.internal.JdbcParameterBindingsImpl;
import org.hibernate.sql.exec.internal.JdbcParameterImpl;
import org.hibernate.sql.exec.spi.JdbcOperationQuerySelect;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.exec.spi.JdbcParametersList;
import org.hibernate.sql.results.internal.RowTransformerStandardImpl;
import org.hibernate.sql.results.spi.ListResultsConsumer;
import org.hibernate.type.BasicType;

import static org.hibernate.loader.ast.internal.MultiKeyLoadHelper.hasSingleId;
import static org.hibernate.loader.ast.internal.MultiKeyLoadHelper.trimIdBatch;
import static org.hibernate.loader.ast.internal.MultiKeyLoadLogging.MULTI_KEY_LOAD_LOGGER;

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

	@AllowReflection
	public CollectionBatchLoaderArrayParam(
			int domainBatchSize,
			LoadQueryInfluencers loadQueryInfluencers,
			PluralAttributeMapping attributeMapping,
			SessionFactoryImplementor sessionFactory) {
		super( domainBatchSize, loadQueryInfluencers, attributeMapping, sessionFactory );

		if ( MULTI_KEY_LOAD_LOGGER.isDebugEnabled() ) {
			MULTI_KEY_LOAD_LOGGER.debugf(
					"Using ARRAY batch fetching strategy for collection `%s` : %s",
					attributeMapping.getNavigableRole().getFullPath(),
					domainBatchSize
			);
		}

		final ForeignKeyDescriptor keyDescriptor = getLoadable().getKeyDescriptor();
		final JdbcMapping jdbcMapping = keyDescriptor.getSingleJdbcMapping();
		final Class<?> jdbcArrayClass = Array.newInstance( jdbcMapping.getJdbcJavaType().getJavaTypeClass(), 0 )
				.getClass();
		keyDomainType = getKeyType( keyDescriptor.getKeyPart() );

		final BasicType<?> arrayBasicType = getSessionFactory().getTypeConfiguration()
				.getBasicTypeRegistry()
				.getRegisteredType( jdbcArrayClass );
		arrayJdbcMapping = MultiKeyLoadHelper.resolveArrayJdbcMapping(
				arrayBasicType,
				jdbcMapping,
				jdbcArrayClass,
				getSessionFactory()
		);

		jdbcParameter = new JdbcParameterImpl( arrayJdbcMapping );
		sqlSelect = LoaderSelectBuilder.createSelectBySingleArrayParameter(
				getLoadable(),
				keyDescriptor.getKeyPart(),
				getInfluencers(),
				LockOptions.NONE,
				jdbcParameter,
				getSessionFactory()
		);

		final QuerySpec querySpec = sqlSelect.getQueryPart().getFirstQuerySpec();
		final TableGroup tableGroup = querySpec.getFromClause().getRoots().get( 0 );
		attributeMapping.applySoftDeleteRestrictions( tableGroup, querySpec::applyPredicate );

		jdbcSelectOperation = getSessionFactory().getJdbcServices()
				.getJdbcEnvironment()
				.getSqlAstTranslatorFactory()
				.buildSelectTranslator( getSessionFactory(), sqlSelect )
				.translate( JdbcParameterBindings.NO_BINDINGS, QueryOptions.NONE );
	}
	@Override
	public PersistentCollection<?> load(Object keyBeingLoaded, SharedSessionContractImplementor session) {
		final ForeignKeyDescriptor keyDescriptor = getLoadable().getKeyDescriptor();
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
		if ( MULTI_KEY_LOAD_LOGGER.isDebugEnabled() ) {
			MULTI_KEY_LOAD_LOGGER.debugf(
					"Batch fetching collection: %s.%s",
					getLoadable().getNavigableRole().getFullPath(), keyBeingLoaded
			);
		}

		final int length = getDomainBatchSize();
		final Object[] keysToInitialize = (Object[]) Array.newInstance(
				jdbcParameter.getExpressionType()
						.getSingleJdbcMapping()
						.getJdbcJavaType()
						.getJavaTypeClass()
						.getComponentType(),
				length
		);
		final Object[] embeddedKeys = (Object[]) Array.newInstance( keyDomainType, length );
		session.getPersistenceContextInternal().getBatchFetchQueue()
				.collectBatchLoadableCollectionKeys(
						length,
						(index, key) ->
								keyDescriptor.forEachJdbcValue( key, (i, value, jdbcMapping) -> {
									keysToInitialize[index] = value;
									embeddedKeys[index] = key;
								}, session )
						,
						keyBeingLoaded,
						getLoadable()
				);
		// now trim down the array to the number of keys we found
		final Object[] keys = trimIdBatch( length, keysToInitialize );

		if ( hasSingleId( keys ) ) {
			return singleKeyLoader.load( keyBeingLoaded, session );
		}

		initializeKeys( keyBeingLoaded, keys, session );

		for ( Object initializedKey : embeddedKeys ) {
			if ( initializedKey != null ) {
				finishInitializingKey( initializedKey, session );
			}
		}
		final CollectionKey collectionKey = new CollectionKey(
				getLoadable().getCollectionDescriptor(),
				keyBeingLoaded
		);
		return session.getPersistenceContext().getCollection( collectionKey );
	}

	@Override
	void initializeKeys(Object key, Object[] keysToInitialize, SharedSessionContractImplementor session) {
		if ( MULTI_KEY_LOAD_LOGGER.isDebugEnabled() ) {
			MULTI_KEY_LOAD_LOGGER.debugf(
					"Collection keys to batch-fetch initialize (`%s#%s`) %s",
					getLoadable().getNavigableRole().getFullPath(),
					key,
					keysToInitialize
			);
		}

		assert jdbcSelectOperation != null;
		assert jdbcParameter != null;

		final JdbcParameterBindings jdbcParameterBindings = new JdbcParameterBindingsImpl(1);
		jdbcParameterBindings.addBinding(
				jdbcParameter,
				new JdbcParameterBindingImpl( arrayJdbcMapping, keysToInitialize )
		);

		final SubselectFetch.RegistrationHandler subSelectFetchableKeysHandler = SubselectFetch.createRegistrationHandler(
				session.getPersistenceContext().getBatchFetchQueue(),
				sqlSelect,
				JdbcParametersList.singleton( jdbcParameter ),
				jdbcParameterBindings
		);

		session.getJdbcServices().getJdbcSelectExecutor().list(
				jdbcSelectOperation,
				jdbcParameterBindings,
				new ExecutionContextWithSubselectFetchHandler(
						session,
						subSelectFetchableKeysHandler
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
		final ForeignKeyDescriptor keyDescriptor = getLoadable().getKeyDescriptor();
		if( keyDescriptor.isEmbedded()){
			assert keyDescriptor.getJdbcTypeCount() == 1;
			final int length = getDomainBatchSize();
			final Object[] keysToInitialize = (Object[]) Array.newInstance( keyDescriptor.getSingleJdbcMapping().getJdbcJavaType().getJavaTypeClass(), length );
			session.getPersistenceContextInternal().getBatchFetchQueue()
					.collectBatchLoadableCollectionKeys(
							length,
							(index, key) ->
								keyDescriptor.forEachJdbcValue( key, (i, value, jdbcMapping) -> {
									keysToInitialize[index] = value;
								}, session )
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
