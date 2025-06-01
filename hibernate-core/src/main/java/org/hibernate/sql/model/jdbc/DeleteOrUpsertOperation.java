/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.model.jdbc;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Locale;

import org.hibernate.engine.jdbc.mutation.JdbcValueBindings;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.jdbc.mutation.group.PreparedStatementDetails;
import org.hibernate.engine.jdbc.mutation.internal.MutationQueryOptions;
import org.hibernate.engine.jdbc.mutation.internal.PreparedStatementGroupSingleTable;
import org.hibernate.engine.jdbc.mutation.spi.Binding;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.entity.mutation.EntityMutationTarget;
import org.hibernate.persister.entity.mutation.EntityTableMapping;
import org.hibernate.persister.entity.mutation.UpdateValuesAnalysis;
import org.hibernate.sql.model.MutationTarget;
import org.hibernate.sql.model.MutationType;
import org.hibernate.sql.model.SelfExecutingUpdateOperation;
import org.hibernate.sql.model.TableMapping;
import org.hibernate.sql.model.ValuesAnalysis;
import org.hibernate.sql.model.internal.OptionalTableUpdate;
import org.hibernate.sql.model.internal.TableDeleteStandard;

import static java.util.Collections.emptyList;
import static org.hibernate.sql.model.ModelMutationLogging.MODEL_MUTATION_LOGGER;

/**
 * @author Steve Ebersole
 */
public class DeleteOrUpsertOperation implements SelfExecutingUpdateOperation {
	private final EntityMutationTarget mutationTarget;
	private final EntityTableMapping tableMapping;
	private final UpsertOperation upsertOperation;

	private final OptionalTableUpdate optionalTableUpdate;

	public DeleteOrUpsertOperation(
			EntityMutationTarget mutationTarget,
			EntityTableMapping tableMapping,
			UpsertOperation upsertOperation,
			OptionalTableUpdate optionalTableUpdate) {
		this.mutationTarget = mutationTarget;
		this.tableMapping = tableMapping;
		this.upsertOperation = upsertOperation;
		this.optionalTableUpdate = optionalTableUpdate;
	}

	/*
	 * Used by Hibernate Reactive
	 */
	protected DeleteOrUpsertOperation(DeleteOrUpsertOperation original) {
		this.mutationTarget = original.mutationTarget;
		this.tableMapping = original.tableMapping;
		this.upsertOperation = original.upsertOperation;
		this.optionalTableUpdate = original.optionalTableUpdate;
	}

	@Override
	public MutationType getMutationType() {
		return MutationType.UPDATE;
	}

	@Override
	public MutationTarget<?> getMutationTarget() {
		return mutationTarget;
	}

	@Override
	public TableMapping getTableDetails() {
		return tableMapping;
	}

	@Override
	public JdbcValueDescriptor findValueDescriptor(String columnName, ParameterUsage usage) {
		return upsertOperation.findValueDescriptor( columnName, usage );
	}

	@Override
	public void performMutation(
			JdbcValueBindings jdbcValueBindings,
			ValuesAnalysis valuesAnalysis,
			SharedSessionContractImplementor session) {
		final UpdateValuesAnalysis analysis = (UpdateValuesAnalysis) valuesAnalysis;

		if ( analysis.getTablesWithNonNullValues().contains( tableMapping ) ) {
			performUpsert( jdbcValueBindings, session );
		}
		else {
			// all the new values are null - delete
			performDelete( jdbcValueBindings, session );
		}
	}

	private void performDelete(JdbcValueBindings jdbcValueBindings, SharedSessionContractImplementor session) {
		MODEL_MUTATION_LOGGER.tracef( "#performDelete(%s)", tableMapping.getTableName() );

		final TableDeleteStandard upsertDeleteAst = new TableDeleteStandard(
				optionalTableUpdate.getMutatingTable(),
				mutationTarget,
				"upsert delete",
				optionalTableUpdate.getKeyBindings(),
				emptyList(),
				emptyList()
		);

		final JdbcServices jdbcServices = session.getJdbcServices();
		final var upsertDelete =
				jdbcServices.getJdbcEnvironment().getSqlAstTranslatorFactory()
						.buildModelMutationTranslator( upsertDeleteAst, session.getFactory() )
						.translate( null, MutationQueryOptions.INSTANCE );
		final var statementGroup = new PreparedStatementGroupSingleTable( upsertDelete, session );
		final var statementDetails = statementGroup.resolvePreparedStatementDetails( tableMapping.getTableName() );
		try {
			final PreparedStatement upsertDeleteStatement = statementDetails.resolveStatement();
			jdbcServices.getSqlStatementLogger().logStatement( statementDetails.getSqlString() );
			bindDeleteKeyValues( jdbcValueBindings, statementDetails, session );
			final int rowCount = session.getJdbcCoordinator().getResultSetReturn()
					.executeUpdate( upsertDeleteStatement, statementDetails.getSqlString() );
			MODEL_MUTATION_LOGGER.tracef( "`%s` rows upsert-deleted from `%s`", rowCount, tableMapping.getTableName() );
		}
		finally {
			statementDetails.releaseStatement( session );
		}
	}

	private void bindDeleteKeyValues(
			JdbcValueBindings jdbcValueBindings,
			PreparedStatementDetails statementDetails,
			SharedSessionContractImplementor session) {
		final PreparedStatement statement = statementDetails.resolveStatement();
		int jdbcBindingPosition = 1;
		for ( Binding binding : jdbcValueBindings.getBindingGroup( tableMapping.getTableName() ).getBindings() ) {
			if ( binding.getValueDescriptor().getUsage() == ParameterUsage.RESTRICT ) {
				bindKeyValue(
						jdbcBindingPosition++,
						binding,
						binding.getValueDescriptor(),
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
			EntityTableMapping tableMapping,
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
		MODEL_MUTATION_LOGGER.tracef( "#performUpsert(%s)", tableMapping.getTableName() );

		final var statementGroup = new PreparedStatementGroupSingleTable( upsertOperation, session );
		final var statementDetails = statementGroup.resolvePreparedStatementDetails( tableMapping.getTableName() );
		try {
			final PreparedStatement updateStatement = statementDetails.resolveStatement();
			session.getJdbcServices().getSqlStatementLogger().logStatement( statementDetails.getSqlString() );
			jdbcValueBindings.beforeStatement( statementDetails );
			final int rowCount =
					session.getJdbcCoordinator().getResultSetReturn()
							.executeUpdate( updateStatement, statementDetails.getSqlString() );
			MODEL_MUTATION_LOGGER.tracef( "`%s` rows upserted into `%s`", rowCount, tableMapping.getTableName() );
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
}
