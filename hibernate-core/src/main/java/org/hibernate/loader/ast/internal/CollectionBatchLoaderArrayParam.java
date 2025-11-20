/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
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
import org.hibernate.loader.ast.spi.CollectionBatchLoader;
import org.hibernate.loader.ast.spi.SqlArrayMultiKeyLoader;
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.NonAggregatedIdentifierMapping;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.ValuedModelPart;
import org.hibernate.metamodel.mapping.internal.IdClassEmbeddable;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;
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

import static org.hibernate.loader.ast.internal.MultiKeyLoadLogging.MULTI_KEY_LOAD_LOGGER;

/**
 * CollectionBatchLoader using a SQL ARRAY parameter to pass the key values
 *
 * @author Steve Ebersole
 */
public class CollectionBatchLoaderArrayParam
		extends AbstractCollectionBatchLoader
		implements CollectionBatchLoader, SqlArrayMultiKeyLoader {
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

		jdbcSelectOperation = getSessionFactory().getJdbcServices()
				.getJdbcEnvironment()
				.getSqlAstTranslatorFactory()
				.buildSelectTranslator( getSessionFactory(), sqlSelect )
				.translate( JdbcParameterBindings.NO_BINDINGS, QueryOptions.NONE );
	}

	private Class<?> getKeyType(ValuedModelPart keyPart) {
		if ( keyPart instanceof NonAggregatedIdentifierMapping ) {
			final IdClassEmbeddable idClassEmbeddable = ( (NonAggregatedIdentifierMapping) keyPart ).getIdClassEmbeddable();
			if ( idClassEmbeddable != null ) {
				return idClassEmbeddable.getMappedJavaType().getJavaTypeClass();
			}
		}
		return keyPart.getJavaType().getJavaTypeClass();
	}

	@Override
	public PersistentCollection<?> load(Object key, SharedSessionContractImplementor session) {
		if ( MULTI_KEY_LOAD_LOGGER.isDebugEnabled() ) {
			MULTI_KEY_LOAD_LOGGER.debugf( "Batch loading entity `%s#%s`", getLoadable().getNavigableRole().getFullPath(), key );
		}
		final ForeignKeyDescriptor keyDescriptor = getLoadable().getKeyDescriptor();
		if ( keyDescriptor.isEmbedded()
			|| keyDescriptor.getKeyPart().getSingleJdbcMapping().getValueConverter() != null ) {
			assert keyDescriptor.getJdbcTypeCount() == 1;
			return loadWithConversion( key, session, keyDescriptor );
		}
		else {
			final Object[] keysToInitialize = resolveKeysToInitialize( key, session );
			initializeKeys( keysToInitialize, session );

			for ( int i = 0; i < keysToInitialize.length; i++ ) {
				finishInitializingKey( keysToInitialize[i], session );
			}

			final CollectionKey collectionKey = new CollectionKey( getLoadable().getCollectionDescriptor(), key );
			return session.getPersistenceContext().getCollection( collectionKey );
		}
	}

	private PersistentCollection<?> loadWithConversion(
			Object keyBeingLoaded,
			SharedSessionContractImplementor session,
			ForeignKeyDescriptor keyDescriptor) {

		final int length = getDomainBatchSize();
		final Object[] jdbcKeysToInitialize = (Object[]) Array.newInstance(
				jdbcParameter.getExpressionType()
						.getSingleJdbcMapping()
						.getJdbcJavaType()
						.getJavaTypeClass()
						.getComponentType(),
				length
		);
		final Object[] domainKeys = (Object[]) Array.newInstance( keyDomainType, length );
		session.getPersistenceContextInternal().getBatchFetchQueue()
				.collectBatchLoadableCollectionKeys(
						length,
						(index, key) ->
								keyDescriptor.forEachJdbcValue( key, (i, value, jdbcMapping) -> {
									jdbcKeysToInitialize[index] = value;
									domainKeys[index] = key;
								}, session )
						,
						keyBeingLoaded,
						getLoadable()
				);

		initializeKeys( jdbcKeysToInitialize, session );

		for ( Object initializedKey : domainKeys ) {
			finishInitializingKey( initializedKey, session );
		}

		final CollectionKey collectionKey = new CollectionKey(
				getLoadable().getCollectionDescriptor(),
				keyBeingLoaded
		);
		return session.getPersistenceContext().getCollection( collectionKey );
	}

	private Object[] resolveKeysToInitialize(Object keyBeingLoaded, SharedSessionContractImplementor session) {
		final Object[] keysToInitialize = (Object[]) Array.newInstance( keyDomainType, getDomainBatchSize() );
		session.getPersistenceContextInternal().getBatchFetchQueue().collectBatchLoadableCollectionKeys(
				getDomainBatchSize(),
				(index, value) -> keysToInitialize[index] = value,
				keyBeingLoaded,
				getLoadable()
		);
		return keysToInitialize;
	}

	private void initializeKeys(Object[] keysToInitialize, SharedSessionContractImplementor session) {
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

	public void prepare() {
	}
}
