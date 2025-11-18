/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.loader.ast.internal;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.List;

import org.hibernate.LockOptions;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.build.AllowReflection;
import org.hibernate.loader.ast.spi.MultiIdLoadOptions;
import org.hibernate.loader.ast.spi.SqlArrayMultiKeyLoader;
import org.hibernate.metamodel.mapping.BasicEntityIdentifierMapping;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.SqlTypedMapping;
import org.hibernate.metamodel.mapping.internal.SqlTypedMappingImpl;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.spi.QueryOptionsAdapter;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.exec.internal.JdbcParameterBindingImpl;
import org.hibernate.sql.exec.internal.JdbcParameterBindingsImpl;
import org.hibernate.sql.exec.internal.SqlTypedMappingJdbcParameter;
import org.hibernate.sql.exec.spi.JdbcOperationQuerySelect;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.exec.spi.JdbcParametersList;
import org.hibernate.sql.results.internal.RowTransformerStandardImpl;
import org.hibernate.sql.results.spi.ManagedResultConsumer;

import static java.lang.Boolean.TRUE;
import static org.hibernate.engine.spi.SubselectFetch.createRegistrationHandler;
import static org.hibernate.loader.ast.internal.LoaderHelper.loadByArrayParameter;
import static org.hibernate.loader.ast.internal.LoaderSelectBuilder.createSelectBySingleArrayParameter;
import static org.hibernate.loader.ast.internal.MultiKeyLoadHelper.resolveArrayJdbcMapping;
import static org.hibernate.sql.exec.spi.JdbcParameterBindings.NO_BINDINGS;

/**
 * Implementation of {@link org.hibernate.loader.ast.spi.MultiIdEntityLoader}
 * which uses a single JDBC parameter of SQL array type.
 *
 * @author Steve Ebersole
 */
public class MultiIdEntityLoaderArrayParam<E> extends AbstractMultiIdEntityLoader<E>
		implements SqlArrayMultiKeyLoader {
	private final SqlTypedMapping arraySqlTypedMapping;
	private final JdbcParameter jdbcParameter;
	protected final Object[] idArray;

	@AllowReflection
	public MultiIdEntityLoaderArrayParam(
			EntityMappingType entityDescriptor,
			SessionFactoryImplementor sessionFactory) {
		super( entityDescriptor, sessionFactory );
		final EntityIdentifierMapping identifierMapping = entityDescriptor.getIdentifierMapping();
		final SelectableMapping selectable = identifierMapping.getSelectable( 0 );
		final JdbcMapping jdbcMapping = selectable.getJdbcMapping();
		final Class<?> idClass = identifierMapping.getJavaType().getJavaTypeClass();
		idArray = (Object[]) Array.newInstance( idClass, 0 );
		arraySqlTypedMapping = new SqlTypedMappingImpl(
				selectable.getColumnDefinition(),
				selectable.getLength(),
				selectable.getPrecision(),
				selectable.getScale(),
				selectable.getTemporalPrecision(),
				resolveArrayJdbcMapping(
						jdbcMapping,
						jdbcMapping.getJdbcJavaType().getJavaTypeClass(),
						sessionFactory
				)
		);
		jdbcParameter = new SqlTypedMappingJdbcParameter( arraySqlTypedMapping );
	}

	@Override
	public BasicEntityIdentifierMapping getIdentifierMapping() {
		return (BasicEntityIdentifierMapping) super.getIdentifierMapping();
	}

	@Override
	protected int maxBatchSize(Object[] ids, MultiIdLoadOptions loadOptions) {
		final Integer explicitBatchSize = loadOptions.getBatchSize();
		return explicitBatchSize != null && explicitBatchSize > 0
				? explicitBatchSize
				// disable batching by default
				: ids.length;
	}

	@Override
	protected void loadEntitiesById(
			List<Object> idsInBatch,
			LockOptions lockOptions,
			MultiIdLoadOptions loadOptions,
			SharedSessionContractImplementor session) {
		final SelectStatement sqlAst = createSelectBySingleArrayParameter(
				getLoadable(),
				getIdentifierMapping(),
				session.getLoadQueryInfluencers(),
				lockOptions,
				jdbcParameter,
				getSessionFactory()
		);

		final JdbcParameterBindings bindings = new JdbcParameterBindingsImpl(1);
		bindings.addBinding(
				jdbcParameter,
				new JdbcParameterBindingImpl( arraySqlTypedMapping.getJdbcMapping(), toIdArray( idsInBatch ) )
		);

		final SqlAstTranslator<JdbcOperationQuerySelect> sqlAstTranslator = getSqlAstTranslatorFactory()
				.buildSelectTranslator( getSessionFactory(), sqlAst );
		final JdbcOperationQuerySelect jdbcOperation = sqlAstTranslator.translate( NO_BINDINGS, new QueryOptionsAdapter() {
			@Override
			public LockOptions getLockOptions() {
				return lockOptions;
			}
		} );

		getJdbcSelectExecutor().executeQuery(
				jdbcOperation,
				bindings,
				new ExecutionContextWithSubselectFetchHandler(
						session,
						createRegistrationHandler(
								session.getPersistenceContext().getBatchFetchQueue(),
								sqlAst,
								JdbcParametersList.singleton( jdbcParameter ),
								bindings
						),
						// stateless sessions don't have a read-only mode
						session instanceof SessionImplementor statefulSession
								&& TRUE.equals( loadOptions.getReadOnly( statefulSession ) ),
						lockOptions
				),
				RowTransformerStandardImpl.instance(),
				null,
				idsInBatch.size(),
				ManagedResultConsumer.INSTANCE
		);
	}

	@Override
	protected void loadEntitiesWithUnresolvedIds(
			Object[] unresolvableIds,
			MultiIdLoadOptions loadOptions,
			LockOptions lockOptions,
			List<E> results,
			SharedSessionContractImplementor session) {
		final SelectStatement sqlAst = createSelectBySingleArrayParameter(
				getLoadable(),
				getIdentifierMapping(),
				session.getLoadQueryInfluencers(),
				lockOptions,
				jdbcParameter,
				getSessionFactory()
		);

		final JdbcOperationQuerySelect jdbcSelectOperation =
				getSqlAstTranslatorFactory().buildSelectTranslator( getSessionFactory(), sqlAst )
						.translate( NO_BINDINGS, QueryOptions.NONE );

		final List<E> databaseResults = loadByArrayParameter(
				toIdArray( unresolvableIds ),
				sqlAst,
				jdbcSelectOperation,
				jdbcParameter,
				arraySqlTypedMapping.getJdbcMapping(),
				null,
				null,
				null,
				lockOptions,
				session.isDefaultReadOnly(),
				session
		);
		results.addAll( databaseResults );
	}

	@Override
	protected Object[] toIdArray(List<Object> ids) {
		return ids.toArray( idArray );
	}

	protected Object[] toIdArray(Object[] ids) {
		if ( ids.getClass().equals( idArray.getClass() ) ) {
			return ids;
		}
		else {
			final Object[] typedIdArray = Arrays.copyOf( idArray, ids.length );
			System.arraycopy( ids, 0, typedIdArray, 0, ids.length );
			return typedIdArray;
		}
	}
}
