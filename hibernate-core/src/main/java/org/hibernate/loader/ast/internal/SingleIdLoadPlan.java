/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.loader.ast.internal;

import java.util.List;

import org.hibernate.LockOptions;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.loader.ast.spi.Loadable;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.query.internal.SimpleQueryOptions;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.spi.QueryOptionsAdapter;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.exec.internal.BaseExecutionContext;
import org.hibernate.sql.exec.internal.CallbackImpl;
import org.hibernate.sql.exec.internal.JdbcParameterBindingsImpl;
import org.hibernate.sql.exec.spi.Callback;
import org.hibernate.sql.exec.spi.JdbcOperationQuerySelect;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.exec.spi.JdbcParametersList;
import org.hibernate.sql.results.internal.RowTransformerStandardImpl;
import org.hibernate.sql.results.spi.ListResultsConsumer;
import org.hibernate.sql.results.spi.RowTransformer;

/**
 * Describes a plan for loading an entity by identifier.
 *
 * @implNote Made up of (1) a SQL AST for the SQL SELECT and (2) the `ModelPart` used as the restriction
 *
 * @author Steve Ebersole
 */
// todo (6.0) : this can generically define a load-by-uk as well.
// only the SQL AST and `restrictivePart` vary and they are passed as constructor args
public class SingleIdLoadPlan<T> implements SingleEntityLoadPlan {
	private final EntityMappingType entityMappingType;
	private final ModelPart restrictivePart;
	private final LockOptions lockOptions;
	private final JdbcOperationQuerySelect jdbcSelect;
	private final JdbcParametersList jdbcParameters;

	public SingleIdLoadPlan(
			EntityMappingType entityMappingType,
			ModelPart restrictivePart,
			SelectStatement sqlAst,
			JdbcParametersList jdbcParameters,
			LockOptions lockOptions,
			SessionFactoryImplementor sessionFactory) {
		this.entityMappingType = entityMappingType;
		this.restrictivePart = restrictivePart;
		this.lockOptions = lockOptions.makeCopy();
		this.jdbcParameters = jdbcParameters;
		final JdbcServices jdbcServices = sessionFactory.getJdbcServices();
		final JdbcEnvironment jdbcEnvironment = jdbcServices.getJdbcEnvironment();
		final SqlAstTranslatorFactory sqlAstTranslatorFactory = jdbcEnvironment.getSqlAstTranslatorFactory();
		this.jdbcSelect = sqlAstTranslatorFactory.buildSelectTranslator( sessionFactory, sqlAst )
				.translate(
						null,
						new QueryOptionsAdapter() {
							@Override
							public LockOptions getLockOptions() {
								return lockOptions;
							}
						}
				);
	}

	protected LockOptions getLockOptions() {
		return lockOptions;
	}

	protected JdbcParametersList getJdbcParameters() {
		return jdbcParameters;
	}

	@Override
	public Loadable getLoadable() {
		return entityMappingType;
	}

	@Override
	public ModelPart getRestrictivePart() {
		return restrictivePart;
	}

	@Override
	public JdbcOperationQuerySelect getJdbcSelect() {
		return jdbcSelect;
	}

	protected RowTransformer<T> getRowTransformer() {
		return RowTransformerStandardImpl.instance();
	}

	public T load(Object restrictedValue, SharedSessionContractImplementor session) {
		return load( restrictedValue, null, null, false, session );
	}

	public T load(Object restrictedValue, Boolean readOnly, SharedSessionContractImplementor session) {
		return load( restrictedValue, null, readOnly, false, session );
	}

	public T load(
			Object restrictedValue,
			Boolean readOnly,
			Boolean singleResultExpected,
			SharedSessionContractImplementor session) {
		return load( restrictedValue, null, readOnly, singleResultExpected, session );
	}

	public T load(
			Object restrictedValue,
			Object entityInstance,
			Boolean readOnly,
			Boolean singleResultExpected,
			SharedSessionContractImplementor session) {
		final int jdbcTypeCount = restrictivePart.getJdbcTypeCount();
		assert jdbcParameters.size() % jdbcTypeCount == 0;

		final JdbcParameterBindings jdbcParameterBindings = new JdbcParameterBindingsImpl( jdbcTypeCount );

		int offset = 0;
		while ( offset < jdbcParameters.size() ) {
			offset += jdbcParameterBindings.registerParametersForEachJdbcValue(
					restrictedValue,
					offset,
					restrictivePart,
					jdbcParameters,
					session
			);
		}
		assert offset == jdbcParameters.size();
		final QueryOptions queryOptions = new SimpleQueryOptions( lockOptions, readOnly );
		final Callback callback = new CallbackImpl();

		final List<T> list = session.getJdbcServices().getJdbcSelectExecutor().list(
				jdbcSelect,
				jdbcParameterBindings,
				new SingleIdExecutionContext(
						session,
						entityInstance,
						restrictedValue,
						entityMappingType.getRootEntityDescriptor(),
						queryOptions,
						callback
				),
				getRowTransformer(),
				null,
				singleResultExpected
						? ListResultsConsumer.UniqueSemantic.ASSERT
						: ListResultsConsumer.UniqueSemantic.FILTER,
				1
		);

		if ( list.isEmpty() ) {
			return null;
		}

		final T entity = list.get( 0 );
		callback.invokeAfterLoadActions( entity, entityMappingType, session );
		return entity;
	}

	private static class SingleIdExecutionContext extends BaseExecutionContext {
		private final Object entityInstance;
		private final Object restrictedValue;
		private final EntityMappingType rootEntityDescriptor;
		private final QueryOptions queryOptions;
		private final Callback callback;

		public SingleIdExecutionContext(
				SharedSessionContractImplementor session,
				Object entityInstance,
				Object restrictedValue,
				EntityMappingType rootEntityDescriptor, QueryOptions queryOptions,
				Callback callback) {
			super( session );
			this.entityInstance = entityInstance;
			this.restrictedValue = restrictedValue;
			this.rootEntityDescriptor = rootEntityDescriptor;
			this.queryOptions = queryOptions;
			this.callback = callback;
		}

		@Override
		public Object getEntityInstance() {
			return entityInstance;
		}

		@Override
		public Object getEntityId() {
			return restrictedValue;
		}

		@Override
		public EntityMappingType getRootEntityDescriptor() {
			return rootEntityDescriptor;
		}

		@Override
		public QueryOptions getQueryOptions() {
			return queryOptions;
		}

		@Override
		public Callback getCallback() {
			return callback;
		}

	}
}
