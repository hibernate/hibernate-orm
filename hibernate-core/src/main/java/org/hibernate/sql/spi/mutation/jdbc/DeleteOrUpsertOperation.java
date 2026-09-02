/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.spi.mutation.jdbc;

import org.hibernate.dialect.sql.ast.spi.SqlAstTranslationRequest;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Locale;

import org.hibernate.engine.jdbc.mutation.JdbcValueBindings;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.jdbc.mutation.group.PreparedStatementDetails;
import org.hibernate.engine.jdbc.mutation.internal.MutationQueryOptions;
import org.hibernate.engine.jdbc.mutation.internal.PreparedStatementGroupSingleTable;
import org.hibernate.engine.jdbc.mutation.spi.Binding;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.jdbc.Expectation;
import org.hibernate.persister.entity.mutation.UpdateValuesAnalysis;
import org.hibernate.sql.spi.mutation.MutationTarget;
import org.hibernate.sql.spi.mutation.MutationType;
import org.hibernate.sql.spi.mutation.SelfExecutingUpdateOperation;
import org.hibernate.sql.spi.mutation.TableMapping;
import org.hibernate.sql.spi.mutation.ValuesAnalysis;
import org.hibernate.sql.ast.spi.model.OptionalTableUpdate;
import org.hibernate.sql.ast.spi.model.TableDeleteStandard;

import static java.util.Collections.emptyList;
import static org.hibernate.sql.model.ModelMutationLogging.MODEL_MUTATION_LOGGER;

/**
 * @author Steve Ebersole
 */
public final class DeleteOrUpsertOperation implements SelfExecutingUpdateOperation {
	private final UpsertOperation upsertOperation;

	private final OptionalTableUpdate optionalTableUpdate;

	public DeleteOrUpsertOperation(
			UpsertOperation upsertOperation,
			OptionalTableUpdate optionalTableUpdate) {
		this.upsertOperation = upsertOperation;
		this.optionalTableUpdate = optionalTableUpdate;
	}

	@Override
	public final MutationType getMutationType() {
		return MutationType.UPDATE;
	}

	@Override
	public final MutationTarget getMutationTarget() {
		return upsertOperation.getMutationTarget();
	}

	@Override
	public final TableMapping getTableDetails() {
		return upsertOperation.getTableDetails();
	}

	@Override
	public final JdbcValueDescriptor findValueDescriptor(String columnName, ParameterUsage usage) {
		return upsertOperation.findValueDescriptor( columnName, usage );
	}

	@Override
	public final void performMutation(
			JdbcValueBindings jdbcValueBindings,
			ValuesAnalysis valuesAnalysis,
			SharedSessionContractImplementor session) {
		final var tableMapping = getTableDetails();
		final var analysis = (UpdateValuesAnalysis) valuesAnalysis;
		if ( analysis.getTablesWithNonNullValues().contains( tableMapping ) ) {
			performUpsert( jdbcValueBindings, session );
		}
		else {
			// all the new values are null - delete
			performDelete( jdbcValueBindings, session );
		}
	}

	private void performDelete(JdbcValueBindings jdbcValueBindings, SharedSessionContractImplementor session) {
		final var tableMapping = getTableDetails();
		MODEL_MUTATION_LOGGER.performingDelete( tableMapping.getTableName() );

		final var upsertDeleteAst = new TableDeleteStandard(
				optionalTableUpdate.getMutatingTable(),
				getMutationTarget(),
				"upsert delete",
				optionalTableUpdate.getKeyBindings(),
				emptyList(),
				emptyList()
		);

		final var jdbcServices = session.getJdbcServices();
		final var upsertDelete =
				jdbcServices.getJdbcEnvironment().getSqlAstTranslatorFactory()
						.buildTranslator( new SqlAstTranslationRequest.ModelMutation<>( session.getFactory(), upsertDeleteAst ) )
						.translate( null, MutationQueryOptions.INSTANCE );
		final var statementDetails =
				new PreparedStatementGroupSingleTable( upsertDelete, session )
						.resolvePreparedStatementDetails( tableMapping.getTableName() );
		try {
			final var upsertDeleteStatement = statementDetails.resolveStatement();
			final String sql = statementDetails.getSqlString();
			jdbcServices.getSqlStatementLogger().logStatement( sql );
			session.getJdbcSessionContext().getStatementObserver().performingSql( sql, -1 );
			bindDeleteKeyValues( jdbcValueBindings, statementDetails, session );
			final int rowCount =
					session.getJdbcCoordinator().getResultSetReturn()
							.executeUpdate( upsertDeleteStatement, sql );
			MODEL_MUTATION_LOGGER.upsertDeletedRowCount( rowCount, tableMapping.getTableName() );
			try {
				getExpectation().verifyOutcome( rowCount, upsertDeleteStatement, -1, sql );
			}
			catch (SQLException e) {
				throw jdbcServices.getSqlExceptionHelper().convert(
						e,
						"Unable to verify outcome for upsert delete",
						sql
				);
			}
		}
		finally {
			statementDetails.releaseStatement( session );
		}
	}

	private void bindDeleteKeyValues(
			JdbcValueBindings jdbcValueBindings,
			PreparedStatementDetails statementDetails,
			SharedSessionContractImplementor session) {
		final var tableMapping = getTableDetails();
		final var statement = statementDetails.resolveStatement();
		int jdbcBindingPosition = 1;
		for ( var binding :
				jdbcValueBindings.getBindingGroup( tableMapping.getTableName() )
						.getBindings() ) {
			final var valueDescriptor = binding.getValueDescriptor();
			if ( valueDescriptor.getUsage() == ParameterUsage.RESTRICT ) {
				bindKeyValue(
						jdbcBindingPosition++,
						binding,
						valueDescriptor,
						statement,
						statementDetails.getSqlString(),
						tableMapping,
						session
				);
			}
		}
	}

	private static void bindKeyValue(
			int jdbcPosition,
			Binding binding,
			JdbcValueDescriptor valueDescriptor,
			PreparedStatement statement,
			String sql,
			TableMapping tableMapping,
			SharedSessionContractImplementor session) {
		try {
			binding.getValueBinder().bind( statement, binding.getValue(), jdbcPosition, session );
		}
		catch (SQLException e) {
			throw session.getJdbcServices().getSqlExceptionHelper().convert(
					e,
					String.format(
							Locale.ROOT,
							"Unable to bind parameter for upsert insert : %s.%s",
							tableMapping.getTableName(),
							valueDescriptor.getColumnName()
					),
					sql
			);
		}
	}

	private void performUpsert(JdbcValueBindings jdbcValueBindings, SharedSessionContractImplementor session) {
		final var tableMapping = getTableDetails();
		MODEL_MUTATION_LOGGER.performingUpsert( tableMapping.getTableName() );

		final var statementDetails =
				new PreparedStatementGroupSingleTable( upsertOperation, session )
						.resolvePreparedStatementDetails( tableMapping.getTableName() );
		try {
			final var updateStatement = statementDetails.resolveStatement();
			final var jdbcServices = session.getJdbcServices();
			jdbcServices.getSqlStatementLogger().logStatement( statementDetails.getSqlString() );
			session.getJdbcSessionContext().getStatementObserver().performingSql( statementDetails.getSqlString(), -1 );
			jdbcValueBindings.beforeStatement( statementDetails );
			final int rowCount =
					session.getJdbcCoordinator().getResultSetReturn()
							.executeUpdate( updateStatement, statementDetails.getSqlString() );
			MODEL_MUTATION_LOGGER.upsertedRowCount( rowCount, tableMapping.getTableName() );
			try {
				getExpectation().verifyOutcome( rowCount, updateStatement, -1, statementDetails.getSqlString() );
			}
			catch (SQLException e) {
				throw jdbcServices.getSqlExceptionHelper().convert(
						e,
						"Unable to verify outcome for upsert",
						statementDetails.getSqlString()
				);
			}
		}
		finally {
			statementDetails.releaseStatement( session );
		}
	}

	/*
	 * Used by Hibernate Reactive
	 */
	public UpsertOperation getUpsertOperation() {
		return upsertOperation;
	}

	/*
	 * Used by Hibernate Reactive
	 */
	public OptionalTableUpdate getOptionalTableUpdate() {
		return optionalTableUpdate;
	}

	private Expectation getExpectation() {
		return upsertOperation.getExpectation();
	}
}
