/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.loader.ast.internal;

import jakarta.annotation.Nullable;

import org.hibernate.dialect.sql.ast.spi.SqlAstTranslationRequest;

import jakarta.annotation.Nonnull;
import org.hibernate.LockOptions;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.loader.ast.spi.Loadable;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.query.internal.SimpleQueryOptions;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.spi.QueryOptionsAdapter;
import org.hibernate.sql.ast.spi.query.select.SelectStatement;
import org.hibernate.sql.exec.internal.BaseExecutionContext;
import org.hibernate.sql.exec.internal.CallbackImpl;
import org.hibernate.sql.exec.internal.JdbcParameterBindingsImpl;
import org.hibernate.sql.exec.spi.Callback;
import org.hibernate.sql.exec.spi.JdbcParametersList;
import org.hibernate.sql.exec.spi.JdbcSelect;
import org.hibernate.sql.results.internal.RowTransformerStandardImpl;
import org.hibernate.sql.results.spi.ListResultsConsumer;
import org.hibernate.sql.results.spi.RowTransformer;

import java.util.List;

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
	private final JdbcSelect jdbcSelect;
	private final JdbcParametersList jdbcParameters;

	public SingleIdLoadPlan(
			@Nonnull EntityMappingType entityMappingType,
			@Nonnull ModelPart restrictivePart,
			@Nonnull SelectStatement sqlAst,
			@Nonnull JdbcParametersList jdbcParameters,
			@Nonnull LockOptions lockOptions,
			@Nonnull SessionFactoryImplementor sessionFactory) {
		this.entityMappingType = entityMappingType;
		this.restrictivePart = restrictivePart;
		this.lockOptions = lockOptions.makeDefensiveCopy();
		this.jdbcParameters = jdbcParameters;
		this.jdbcSelect =
				sessionFactory.getJdbcServices().getJdbcEnvironment()
						.getSqlAstTranslatorFactory()
						.buildTranslator( new SqlAstTranslationRequest.Select( sessionFactory, sqlAst ) )
						.translate(
								null,
								new QueryOptionsAdapter() {
									@Override
									@Nonnull
									public LockOptions getLockOptions() {
										return lockOptions;
									}
								}
						);
	}

	@Nonnull
	protected LockOptions getLockOptions() {
		return lockOptions;
	}

	@Nonnull
	protected JdbcParametersList getJdbcParameters() {
		return jdbcParameters;
	}

	@Nonnull
	@Override
	public Loadable getLoadable() {
		return entityMappingType;
	}

	@Nonnull
	@Override
	public ModelPart getRestrictivePart() {
		return restrictivePart;
	}

	@Nonnull
	@Override
	public JdbcSelect getJdbcSelect() {
		return jdbcSelect;
	}

	@Nonnull
	protected RowTransformer<T> getRowTransformer() {
		return RowTransformerStandardImpl.instance();
	}

	@Nullable
	public T load(@Nonnull Object restrictedValue, @Nonnull SharedSessionContractImplementor session) {
		return load( restrictedValue, null, null, false, session );
	}

	@Nullable
	public T load(@Nonnull Object restrictedValue, @Nullable Boolean readOnly, @Nonnull SharedSessionContractImplementor session) {
		return load( restrictedValue, null, readOnly, false, session );
	}

	@Nullable
	public T load(
			@Nonnull Object restrictedValue,
			@Nullable Boolean readOnly,
			@Nonnull Boolean singleResultExpected,
			@Nonnull SharedSessionContractImplementor session) {
		return load( restrictedValue, null, readOnly, singleResultExpected, session );
	}

	@Nullable
	public T load(
			@Nonnull Object restrictedValue,
			@Nullable Object entityInstance,
			@Nullable Boolean readOnly,
			@Nonnull Boolean singleResultExpected,
			@Nonnull SharedSessionContractImplementor session) {
		final int jdbcTypeCount = restrictivePart.getJdbcTypeCount();
		assert jdbcParameters.size() % jdbcTypeCount == 0;

		final var jdbcParameterBindings = new JdbcParameterBindingsImpl( jdbcTypeCount );

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
		final var queryOptions = new SimpleQueryOptions( lockOptions, readOnly );
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
		else {
			final T entity = list.get( 0 );
			callback.invokeAfterLoadActions( entity, entityMappingType, session );
			return entity;
		}
	}

	private static class SingleIdExecutionContext extends BaseExecutionContext {
		@Nullable
		private final Object entityInstance;
		private final Object restrictedValue;
		private final EntityMappingType rootEntityDescriptor;
		private final QueryOptions queryOptions;
		private final Callback callback;

		public SingleIdExecutionContext(
				@Nonnull SharedSessionContractImplementor session,
				@Nullable Object entityInstance,
				@Nonnull Object restrictedValue,
				@Nonnull EntityMappingType rootEntityDescriptor, @Nonnull QueryOptions queryOptions,
				@Nonnull Callback callback) {
			super( session );
			this.entityInstance = entityInstance;
			this.restrictedValue = restrictedValue;
			this.rootEntityDescriptor = rootEntityDescriptor;
			this.queryOptions = queryOptions;
			this.callback = callback;
		}

		@Nullable
		@Override
		public Object getEntityInstance() {
			return entityInstance;
		}

		@Nonnull
		@Override
		public Object getEntityId() {
			return restrictedValue;
		}

		@Nonnull
		@Override
		public EntityMappingType getRootEntityDescriptor() {
			return rootEntityDescriptor;
		}

		@Nonnull
		@Override
		public QueryOptions getQueryOptions() {
			return queryOptions;
		}

		@Nonnull
		@Override
		public Callback getCallback() {
			return callback;
		}

	}
}
