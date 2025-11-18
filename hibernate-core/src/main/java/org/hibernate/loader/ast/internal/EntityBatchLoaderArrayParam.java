/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.loader.ast.internal;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Locale;

import org.hibernate.LockOptions;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.loader.ast.spi.SqlArrayMultiKeyLoader;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.SqlTypedMapping;
import org.hibernate.metamodel.mapping.internal.SqlTypedMappingImpl;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.exec.internal.JdbcParameterImpl;
import org.hibernate.sql.exec.internal.SqlTypedMappingJdbcParameter;
import org.hibernate.sql.exec.spi.JdbcOperationQuerySelect;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;

import static org.hibernate.engine.internal.BatchFetchQueueHelper.removeBatchLoadableEntityKey;
import static org.hibernate.loader.ast.internal.MultiKeyLoadHelper.trimIdBatch;
import static org.hibernate.loader.ast.internal.MultiKeyLoadLogging.MULTI_KEY_LOAD_LOGGER;

/**
 * {@link SingleIdEntityLoaderSupport} implementation based on using a single
 * {@linkplain org.hibernate.type.SqlTypes#ARRAY array} parameter to pass the
 * entire batch of ids.
 *
 * @author Steve Ebersole
 */
public class EntityBatchLoaderArrayParam<T>
		extends AbstractEntityBatchLoader<T>
		implements SqlArrayMultiKeyLoader {
	private final int domainBatchSize;

	private final LoadQueryInfluencers loadQueryInfluencers;
	private final EntityIdentifierMapping identifierMapping;
	private final SqlTypedMapping arraySqlTypedMapping;
	private final JdbcParameter jdbcParameter;
	private final SelectStatement sqlAst;
	private final JdbcOperationQuerySelect jdbcSelectOperation;


	/**
	 * Instantiates the loader
	 *
	 * @param domainBatchSize The number of domain model parts (up to)
	 *
	 * @implNote We delay initializing the internal SQL AST state until first use.  Creating
	 * the SQL AST internally relies on the entity's {@link EntityIdentifierMapping}. However, we
	 * do create the static batch-loader for the entity in the persister constructor and
	 * {@link EntityIdentifierMapping} is not available at that time.  On first use, we know we
	 * have it available
	 */
	public EntityBatchLoaderArrayParam(
			int domainBatchSize,
			EntityMappingType entityDescriptor,
			LoadQueryInfluencers loadQueryInfluencers) {
		super( entityDescriptor, loadQueryInfluencers );
		this.loadQueryInfluencers = loadQueryInfluencers;
		this.domainBatchSize = domainBatchSize;

		if ( MULTI_KEY_LOAD_LOGGER.isDebugEnabled() ) {
			MULTI_KEY_LOAD_LOGGER.debugf(
					"Batch fetching enabled for `%s` (entity) using ARRAY strategy : %s",
					entityDescriptor.getEntityName(),
					domainBatchSize
			);
		}

		identifierMapping = getLoadable().getIdentifierMapping();
		final SelectableMapping selectable = identifierMapping.getSelectable( 0 );
		final JdbcMapping jdbcMapping = selectable.getJdbcMapping();
		final Class<?> jdbcArrayClass = Array.newInstance( jdbcMapping.getJdbcJavaType().getJavaTypeClass(), 0 )
				.getClass();
		arraySqlTypedMapping = new SqlTypedMappingImpl(
				selectable.getColumnDefinition(),
				selectable.getLength(),
				selectable.getPrecision(),
				selectable.getScale(),
				selectable.getTemporalPrecision(),
				MultiKeyLoadHelper.resolveArrayJdbcMapping(
					sessionFactory.getTypeConfiguration().getBasicTypeRegistry().getRegisteredType( jdbcArrayClass ),
					jdbcMapping,
					jdbcArrayClass,
					sessionFactory
				)
		);

		jdbcParameter = new SqlTypedMappingJdbcParameter( arraySqlTypedMapping );
		sqlAst = LoaderSelectBuilder.createSelectBySingleArrayParameter(
				getLoadable(),
				identifierMapping,
				loadQueryInfluencers,
				LockOptions.NONE,
				jdbcParameter,
				sessionFactory
		);

		jdbcSelectOperation = sessionFactory.getJdbcServices()
				.getJdbcEnvironment()
				.getSqlAstTranslatorFactory()
				.buildSelectTranslator( sessionFactory, sqlAst )
				.translate( JdbcParameterBindings.NO_BINDINGS, QueryOptions.NONE );
	}

	@Override
	public int getDomainBatchSize() {
		return domainBatchSize;
	}

	protected Object[] resolveIdsToInitialize(Object pkValue, SharedSessionContractImplementor session) {
		//TODO: should this really be different to EntityBatchLoaderInPredicate impl?
		final Class<?> idType = identifierMapping.getJavaType().getJavaTypeClass();
		final Object[] idsToLoad = (Object[]) Array.newInstance( idType, domainBatchSize );
		session.getPersistenceContextInternal().getBatchFetchQueue()
				.collectBatchLoadableEntityIds(
						domainBatchSize,
						(index, value) -> idsToLoad[index] = value,
						pkValue,
						getLoadable()
				);
		return trimIdBatch( domainBatchSize, idsToLoad );
	}

	@Override
	protected void initializeEntities(
			Object[] idsToInitialize,
			Object id,
			Object entityInstance,
			LockOptions lockOptions,
			Boolean readOnly,
			SharedSessionContractImplementor session) {
		if ( MULTI_KEY_LOAD_LOGGER.isDebugEnabled() ) {
			MULTI_KEY_LOAD_LOGGER.debugf( "Ids to batch-fetch initialize (`%s#%s`) %s",
					getLoadable().getEntityName(), id, Arrays.toString(idsToInitialize) );
		}

		for ( Object initializedId : idsToInitialize ) {
			if ( initializedId != null ) {
				// found or not, remove the key from the batch-fetch queue
				removeBatchLoadableEntityKey( initializedId, getLoadable(), session );
			}
		}

		LoaderHelper.loadByArrayParameter(
				idsToInitialize,
				sqlAst,
				jdbcSelectOperation,
				jdbcParameter,
				arraySqlTypedMapping.getJdbcMapping(),
				id,
				entityInstance,
				getLoadable().getRootEntityDescriptor(),
				lockOptions,
				readOnly,
				session
		);
	}

	@Override
	public T load(Object pkValue, LockOptions lockOptions, Boolean readOnly, SharedSessionContractImplementor session) {
		return load( pkValue, null, lockOptions, readOnly, session );
	}

	@Override
	public String toString() {
		return String.format(
				Locale.ROOT,
				"EntityBatchLoaderArrayParam(%s [%s])",
				getLoadable().getEntityName(),
				domainBatchSize
		);
	}
}
