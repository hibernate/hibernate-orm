/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ops.internal;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.resource.jdbc.spi.LogicalConnectionImplementor;
import org.hibernate.sql.exec.internal.StatementAccessImpl;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcOperationQuerySelect;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.exec.spi.JdbcSelectExecutor;
import org.hibernate.sql.ops.spi.DatabaseSelect;
import org.hibernate.sql.ops.spi.JdbcSelect;
import org.hibernate.sql.ops.spi.LoadedValuesCollector;
import org.hibernate.sql.ops.spi.PostAction;
import org.hibernate.sql.ops.spi.PreAction;
import org.hibernate.sql.results.spi.ResultsConsumer;
import org.hibernate.sql.results.spi.RowTransformer;

import java.sql.Connection;

/**
 * Standard {@linkplain DatabaseSelect} implementation.
 *
 * @author Steve Ebersole
 */
public class DatabaseSelectImpl
		extends AbstractDatabaseOperation<JdbcSelect>
		implements DatabaseSelect {
	private final LoadedValuesCollector loadedValuesCollector;

	public DatabaseSelectImpl(JdbcOperationQuerySelect primaryOperation) {
		this( primaryOperation, null );
	}

	public DatabaseSelectImpl(JdbcOperationQuerySelect primaryOperation, LoadedValuesCollector loadedValuesCollector) {
		this( primaryOperation, loadedValuesCollector, null, null );
	}

	public DatabaseSelectImpl(
			JdbcOperationQuerySelect primaryOperation,
			PreAction[] preActions,
			PostAction[] postActions) {
		this( primaryOperation, null, preActions, postActions );
	}

	public DatabaseSelectImpl(
			JdbcOperationQuerySelect primaryOperation,
			LoadedValuesCollector loadedValuesCollector,
			PreAction[] preActions,
			PostAction[] postActions) {
		super( primaryOperation, preActions, postActions );
		this.loadedValuesCollector = loadedValuesCollector;
	}

	@Override
	public JdbcOperationQuerySelect getPrimaryOperation() {
		return (JdbcOperationQuerySelect) super.getPrimaryOperation();
	}

	@Override
	public LoadedValuesCollector getLoadedValuesCollector() {
		return loadedValuesCollector;
	}

	@Override
	public <T, R> T execute(
			Class<R> resultType,
			int expectedNumberOfRows,
			JdbcSelectExecutor.StatementCreator statementCreator,
			JdbcParameterBindings jdbcParameterBindings,
			RowTransformer<R> rowTransformer,
			ResultsConsumer<T, R> resultsConsumer,
			ExecutionContext executionContext) {
		if ( preActions == null && postActions == null ) {
			return performPrimaryOperation(
					resultType,
					statementCreator,
					jdbcParameterBindings,
					rowTransformer,
					resultsConsumer,
					executionContext
			);
		}

		final SharedSessionContractImplementor session = executionContext.getSession();
		final LogicalConnectionImplementor logicalConnection = session.getJdbcCoordinator().getLogicalConnection();
		final SessionFactoryImplementor sessionFactory = session.getSessionFactory();

		final Connection connection = logicalConnection.getPhysicalConnection();
		final StatementAccessImpl statementAccess = new StatementAccessImpl(
				connection,
				logicalConnection,
				sessionFactory
		);

		try {
			try {
				performPreActions( statementAccess, connection, executionContext );
				return performPrimaryOperation(
						resultType,
						statementCreator,
						jdbcParameterBindings,
						rowTransformer,
						resultsConsumer,
						executionContext
				);
			}
			finally {
				performPostActions( statementAccess, connection, executionContext );
			}
		}
		finally {
			statementAccess.release();
		}
	}

	private <T, R> T performPrimaryOperation(
			Class<R> resultType,
			JdbcSelectExecutor.StatementCreator statementCreator,
			JdbcParameterBindings jdbcParameterBindings,
			RowTransformer<R> rowTransformer,
			ResultsConsumer<T, R> resultsConsumer,
			ExecutionContext executionContext) {
//		final SessionFactoryImplementor sessionFactory = executionContext.getSession().getFactory();
//		final JdbcServices jdbcServices = sessionFactory.getJdbcServices();
//		final JdbcSelectExecutor jdbcSelectExecutor = jdbcServices.getJdbcSelectExecutor();
		final JdbcSelectExecutorImpl jdbcSelectExecutor = JdbcSelectExecutorImpl.INSTANCE;

		return jdbcSelectExecutor.executeQuery(
				getPrimaryOperation(),
				jdbcParameterBindings,
				rowTransformer,
				resultType,
				1,
				(c, options) ->
						new ProposedProcessingState( loadedValuesCollector, options, executionContext ),
				statementCreator,
				resultsConsumer,
				executionContext
		);
	}

	public static Builder builder(JdbcOperationQuerySelect primaryAction) {
		return new Builder( primaryAction );
	}

	public static class Builder extends AbstractDatabaseOperation.Builder<Builder> {
		private final JdbcOperationQuerySelect primaryAction;
		private LoadedValuesCollector loadedValuesCollector;

		private Builder(JdbcOperationQuerySelect primaryAction) {
			this.primaryAction = primaryAction;
		}

		@Override
		protected Builder getThis() {
			return this;
		}

		public Builder setLoadedValuesCollector(LoadedValuesCollector loadedValuesCollector) {
			this.loadedValuesCollector = loadedValuesCollector;
			return this;
		}

		public DatabaseSelectImpl build() {
			if ( preActions == null && postActions == null ) {
				return new DatabaseSelectImpl( primaryAction, loadedValuesCollector );
			}
			final PreAction[] preActions = toPreActionArray( this.preActions );
			final PostAction[] postActions = toPostActionArray( this.postActions );
			return new DatabaseSelectImpl( primaryAction, loadedValuesCollector, preActions, postActions );
		}
	}
}
