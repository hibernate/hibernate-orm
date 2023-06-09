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
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.internal.SimpleForeignKeyDescriptor;
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

import static org.hibernate.loader.ast.internal.MultiKeyLoadLogging.MULTI_KEY_LOAD_DEBUG_ENABLED;
import static org.hibernate.loader.ast.internal.MultiKeyLoadLogging.MULTI_KEY_LOAD_LOGGER;

/**
 * CollectionBatchLoader using a SQL ARRAY parameter to pass the key values
 *
 * @author Steve Ebersole
 */
public class CollectionBatchLoaderArrayParam
		extends AbstractCollectionBatchLoader
		implements CollectionBatchLoader, SqlArrayMultiKeyLoader {
	private final  Class<?> arrayElementType;
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

		if ( MULTI_KEY_LOAD_DEBUG_ENABLED ) {
			MULTI_KEY_LOAD_LOGGER.debugf(
					"Using ARRAY batch fetching strategy for collection `%s` : %s",
					attributeMapping.getNavigableRole().getFullPath(),
					domainBatchSize
			);
		}

		final ForeignKeyDescriptor keyDescriptor = getLoadable().getKeyDescriptor();

		arrayElementType = keyDescriptor.getJavaType().getJavaTypeClass();
		Class<?> arrayClass = Array.newInstance( arrayElementType, 0 ).getClass();

		final BasicType<?> arrayBasicType = getSessionFactory().getTypeConfiguration()
				.getBasicTypeRegistry()
				.getRegisteredType( arrayClass );
		arrayJdbcMapping = MultiKeyLoadHelper.resolveArrayJdbcMapping(
				arrayBasicType,
				keyDescriptor.getSingleJdbcMapping(),
				arrayClass,
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

	@Override
	public PersistentCollection<?> load(Object key, SharedSessionContractImplementor session) {
		if ( MULTI_KEY_LOAD_DEBUG_ENABLED ) {
			MULTI_KEY_LOAD_LOGGER.debugf( "Batch loading entity `%s#%s`", getLoadable().getNavigableRole().getFullPath(), key );
		}
		final ForeignKeyDescriptor keyDescriptor = getLoadable().getKeyDescriptor();
		if ( keyDescriptor.isEmbedded() ) {
			assert keyDescriptor.getJdbcTypeCount() == 1;
			return loadEmbeddable( key, session, keyDescriptor );
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

	private PersistentCollection<?> loadEmbeddable(
			Object keyBeingLoaded,
			SharedSessionContractImplementor session,
			ForeignKeyDescriptor keyDescriptor) {

		final int length = getDomainBatchSize();
		final Object[] keysToInitialize = (Object[]) Array.newInstance(
				keyDescriptor.getSingleJdbcMapping().getJdbcJavaType().getJavaTypeClass(),
				length
		);
		final Object[] embeddedKeys = (Object[]) Array.newInstance(
				arrayElementType,
				length
		);
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

		initializeKeys( keysToInitialize, session );

		for ( Object initializedKey : embeddedKeys ) {
				finishInitializingKey( initializedKey, session );
		}

		final CollectionKey collectionKey = new CollectionKey(
				getLoadable().getCollectionDescriptor(),
				keysToInitialize
		);
		return session.getPersistenceContext().getCollection( collectionKey );
	}

	private Object[] resolveKeysToInitialize(Object keyBeingLoaded, SharedSessionContractImplementor session) {
		final Object[] keysToInitialize = (Object[]) Array.newInstance( arrayElementType, getDomainBatchSize() );
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
