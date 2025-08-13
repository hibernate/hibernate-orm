/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.exec.internal;

import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.resource.jdbc.spi.LogicalConnectionImplementor;
import org.hibernate.sql.exec.spi.DatabaseOperationSelect;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcOperationQuerySelect;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.exec.spi.JdbcSelectExecutor;
import org.hibernate.sql.exec.spi.PostAction;
import org.hibernate.sql.exec.spi.PreAction;
import org.hibernate.sql.results.spi.ResultsConsumer;
import org.hibernate.sql.results.spi.RowTransformer;

import java.sql.Connection;
import java.util.Set;

/**
 * Standard DatabaseOperationSelect implementation.
 *
 * @author Steve Ebersole
 */
public class DatabaseOperationSelectImpl
		extends AbstractDatabaseOperation
		implements DatabaseOperationSelect {
	private final JdbcOperationQuerySelect primaryOperation;

	public DatabaseOperationSelectImpl(JdbcOperationQuerySelect primaryOperation) {
		this( null, null, primaryOperation );
	}

	public DatabaseOperationSelectImpl(
			PreAction[] preActions,
			PostAction[] postActions,
			JdbcOperationQuerySelect primaryOperation) {
		super( preActions, postActions );
		this.primaryOperation = primaryOperation;
	}

	@Override
	public JdbcOperationQuerySelect getPrimaryOperation() {
		return primaryOperation;
	}

	@Override
	public Set<String> getAffectedTableNames() {
		return primaryOperation.getAffectedTableNames();
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
		final SessionFactoryImplementor sessionFactory = executionContext.getSession().getFactory();
		final JdbcServices jdbcServices = sessionFactory.getJdbcServices();
		final JdbcSelectExecutor jdbcSelectExecutor = jdbcServices.getJdbcSelectExecutor();
		return jdbcSelectExecutor.executeQuery(
				primaryOperation,
				jdbcParameterBindings,
				executionContext,
				rowTransformer,
				resultType,
				statementCreator,
				resultsConsumer
		);
	}

	public static Builder builder(JdbcOperationQuerySelect primaryAction) {
		return new Builder( primaryAction );
	}

	public static class Builder extends AbstractDatabaseOperation.Builder<Builder> {
		private final JdbcOperationQuerySelect primaryAction;

		private Builder(JdbcOperationQuerySelect primaryAction) {
			this.primaryAction = primaryAction;
		}

		@Override
		protected Builder getThis() {
			return this;
		}

		public DatabaseOperationSelectImpl build() {
			if ( preActions == null && postActions == null ) {
				return new DatabaseOperationSelectImpl( primaryAction );
			}
			final PreAction[] preActions = toArray( PreAction.class, this.preActions );
			final PostAction[] postActions = toArray( PostAction.class, this.postActions );
			return new DatabaseOperationSelectImpl( preActions, postActions, primaryAction );
		}
	}
}
