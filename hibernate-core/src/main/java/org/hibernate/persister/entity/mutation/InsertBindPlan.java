/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.entity.mutation;

import org.hibernate.action.internal.AbstractEntityInsertAction;
import org.hibernate.action.queue.Helper;
import org.hibernate.action.queue.bind.BindPlan;
import org.hibernate.action.queue.cyclebreak.CycleBreakPatcher;
import org.hibernate.action.queue.plan.PlannedOperation;
import org.hibernate.engine.jdbc.mutation.JdbcValueBindings;
import org.hibernate.engine.jdbc.mutation.MutationExecutor;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.jdbc.mutation.TableInclusionChecker;
import org.hibernate.engine.jdbc.mutation.group.PreparedStatementDetails;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.values.GeneratedValues;
import org.hibernate.persister.entity.EntityPersister;

import java.sql.SQLException;
import java.util.Map;

///Bind plan for entity insert operations.
///
/// @see GeneratedValuesCollector
///
/// @author Steve Ebersole
public class InsertBindPlan implements BindPlan {
	private final EntityPersister entityPersister;
	private final Object entity;
	private final Object identifier;
	private final Map<ColumnDetails, Object> columnValues;
	private final boolean[] insertable;
	private final TableInclusionChecker tableInclusionChecker;
	private final AbstractEntityInsertAction action;
	private final GeneratedValuesCollector generatedValuesCollector;

	public InsertBindPlan(
			EntityPersister entityPersister,
			Object entity,
			Object identifier,
			Map<ColumnDetails, Object> columnValues,
			boolean[] insertable,
			TableInclusionChecker tableInclusionChecker,
			AbstractEntityInsertAction action,
			GeneratedValuesCollector generatedValuesCollector) {
		this.entityPersister = entityPersister;
		this.entity = entity;
		this.identifier = identifier;
		this.columnValues = columnValues;
		this.insertable = insertable;
		this.tableInclusionChecker = tableInclusionChecker;
		this.action = action;
		this.generatedValuesCollector = generatedValuesCollector;
	}

	@Override
	public Object getEntityId() {
		return action.getId();
	}

	@Override
	public Object getEntityInstance() {
		return entity;
	}

	public Map<ColumnDetails, Object> getColumnValues() {
		return columnValues;
	}

	@Override
	public void bindAndMaybePatch(
			MutationExecutor executor,
			PlannedOperation plannedOperation,
			SharedSessionContractImplementor session) {
		final var operation = plannedOperation.getOperation();
		final var tableDetails = (EntityTableMapping) operation.getTableDetails();
		if ( !tableInclusionChecker.include( tableDetails ) ) {
			return;
		}

		decomposeForInsert(
				executor,
				identifier,
				plannedOperation,
				session
		);

		// Apply nullable-FK cycle-break patch if planner requested it
		if (plannedOperation.getBindingPatch() != null) {
			CycleBreakPatcher.applyNullInsertPatch( executor, plannedOperation, plannedOperation.getBindingPatch() );
		}
	}

	@Override
	public void execute(
			MutationExecutor executor,
			PlannedOperation plannedOperation,
			SharedSessionContractImplementor session) {
		final var operation = plannedOperation.getOperation();
		final var tableDetails = (EntityTableMapping) operation.getTableDetails();
		if ( !tableInclusionChecker.include( tableDetails ) ) {
			return;
		}

		final GeneratedValues generatedValues = executor.execute(
				entity,
				null,
				tableInclusionChecker,
				InsertBindPlan::verifyOutcome,
				session
		);

		generatedValuesCollector.apply( generatedValues );
	}

	protected void decomposeForInsert(
			MutationExecutor mutationExecutor,
			Object id,
			PlannedOperation plannedOperation,
			SharedSessionContractImplementor session) {
		final var jdbcValueBindings = mutationExecutor.getJdbcValueBindings();

		final var operation = plannedOperation.getOperation();
		final var tableDetails = (EntityTableMapping) operation.getTableDetails();
		final var tableName = Helper.normalizeTableName( tableDetails.getTableName() );

		columnValues.forEach( (columnMapping, columnValue) -> {
			if ( insertable[columnMapping.attributeIndex()]) {
				if ( columnMapping.physicalColumn() && columnMapping.insertable() ) {
					jdbcValueBindings.bindValue(
							columnValue,
							tableName,
							columnMapping.columnName(),
							ParameterUsage.SET
					);
				}
			}
		} );

		if ( id == null ) {
			assert entityPersister.getInsertDelegate() != null;
		}
		else {
			breakDownKeyJdbcValue( id, session, jdbcValueBindings, tableDetails, tableName );
		}
	}

	protected void breakDownKeyJdbcValue(
			Object id,
			SharedSessionContractImplementor session,
			JdbcValueBindings jdbcValueBindings,
			EntityTableMapping tableDetails,
			String tableName) {
		tableDetails.getKeyMapping().breakDownKeyJdbcValues(
				id,
				(jdbcValue, columnMapping) -> {
					jdbcValueBindings.bindValue(
							jdbcValue,
							tableName,
							Helper.normalizeColumnName( columnMapping.getColumnName() ),
							ParameterUsage.SET
					);
				},
				session
		);
	}


	private static boolean verifyOutcome(
			PreparedStatementDetails statementDetails,
			int affectedRowCount,
			int batchPosition) throws SQLException {
		statementDetails.getExpectation().verifyOutcome(
				affectedRowCount,
				statementDetails.getStatement(),
				batchPosition,
				statementDetails.getSqlString()
		);
		return true;
	}
}
