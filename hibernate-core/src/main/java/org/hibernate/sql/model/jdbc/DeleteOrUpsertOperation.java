/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.sql.model.jdbc;

import java.sql.PreparedStatement;
import java.util.Collections;

import org.hibernate.engine.jdbc.mutation.JdbcValueBindings;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.jdbc.mutation.group.PreparedStatementDetails;
import org.hibernate.engine.jdbc.mutation.internal.MutationQueryOptions;
import org.hibernate.engine.jdbc.mutation.internal.PreparedStatementGroupSingleTable;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.entity.mutation.EntityMutationTarget;
import org.hibernate.persister.entity.mutation.EntityTableMapping;
import org.hibernate.persister.entity.mutation.UpdateValuesAnalysis;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.model.MutationTarget;
import org.hibernate.sql.model.MutationType;
import org.hibernate.sql.model.SelfExecutingUpdateOperation;
import org.hibernate.sql.model.TableMapping;
import org.hibernate.sql.model.ValuesAnalysis;
import org.hibernate.sql.model.internal.OptionalTableUpdate;
import org.hibernate.sql.model.internal.TableDeleteStandard;

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

		if ( !analysis.getTablesWithNonNullValues().contains( tableMapping ) ) {
			// all the new values are null - delete
			performDelete( jdbcValueBindings, session );
		}
		else {
			performUpsert( jdbcValueBindings, session );
		}
	}

	private void performDelete(JdbcValueBindings jdbcValueBindings, SharedSessionContractImplementor session) {
		MODEL_MUTATION_LOGGER.tracef( "#performDelete(%s)", tableMapping.getTableName() );

		final TableDeleteStandard upsertDeleteAst = new TableDeleteStandard(
				optionalTableUpdate.getMutatingTable(),
				mutationTarget,
				"upsert delete",
				optionalTableUpdate.getKeyBindings(),
				Collections.emptyList(),
				Collections.emptyList()
		);

		final SqlAstTranslator<JdbcDeleteMutation> translator = session
				.getJdbcServices()
				.getJdbcEnvironment()
				.getSqlAstTranslatorFactory()
				.buildModelMutationTranslator( upsertDeleteAst, session.getFactory() );
		final JdbcMutationOperation upsertDelete = translator.translate( null, MutationQueryOptions.INSTANCE );

		final PreparedStatementGroupSingleTable statementGroup = new PreparedStatementGroupSingleTable( upsertDelete, session );
		final PreparedStatementDetails statementDetails = statementGroup.resolvePreparedStatementDetails( tableMapping.getTableName() );
		final PreparedStatement updateStatement = statementDetails.resolveStatement();
		session.getJdbcServices().getSqlStatementLogger().logStatement( statementDetails.getSqlString() );
		jdbcValueBindings.beforeStatement( statementDetails );

		final int rowCount = session.getJdbcCoordinator().getResultSetReturn()
				.executeUpdate( updateStatement, statementDetails.getSqlString() );

		MODEL_MUTATION_LOGGER.tracef( "`%s` rows upsert-deleted from `%s`", rowCount, tableMapping.getTableName() );
	}

	private void performUpsert(JdbcValueBindings jdbcValueBindings, SharedSessionContractImplementor session) {
		MODEL_MUTATION_LOGGER.tracef( "#performUpsert(%s)", tableMapping.getTableName() );

		final PreparedStatementGroupSingleTable statementGroup = new PreparedStatementGroupSingleTable( upsertOperation, session );
		final PreparedStatementDetails statementDetails = statementGroup.resolvePreparedStatementDetails( tableMapping.getTableName() );

		final PreparedStatement updateStatement = statementDetails.resolveStatement();
		session.getJdbcServices().getSqlStatementLogger().logStatement( statementDetails.getSqlString() );

		jdbcValueBindings.beforeStatement( statementDetails );

		final int rowCount = session.getJdbcCoordinator().getResultSetReturn()
				.executeUpdate( updateStatement, statementDetails.getSqlString() );

		MODEL_MUTATION_LOGGER.tracef( "`%s` rows upserted into `%s`", rowCount, tableMapping.getTableName() );
	}
}
