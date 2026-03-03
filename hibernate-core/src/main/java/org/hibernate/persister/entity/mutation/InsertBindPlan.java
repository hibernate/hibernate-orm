/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.entity.mutation;

import org.hibernate.action.queue.bind.BindPlan;
import org.hibernate.action.queue.cyclebreak.CycleBreakPatcher;
import org.hibernate.action.queue.plan.PlannedOperation;
import org.hibernate.engine.jdbc.mutation.JdbcValueBindings;
import org.hibernate.engine.jdbc.mutation.MutationExecutor;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.jdbc.mutation.TableInclusionChecker;
import org.hibernate.engine.jdbc.mutation.group.PreparedStatementDetails;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.persister.entity.EntityPersister;

import java.sql.SQLException;
import java.util.function.Supplier;

/**
 * @author Steve Ebersole
 */
public class InsertBindPlan implements BindPlan {
	private final EntityPersister entityPersister;
	private final Object entity;
	private final Object identifier;
	private final Object[] state;
	private final boolean[] insertable;
	private final InsertValuesAnalysis valuesAnalysis;
	private final TableInclusionChecker tableInclusionChecker;
	private final Supplier<Object> identifierSupplier;

	public InsertBindPlan(
			EntityPersister entityPersister,
			Object entity,
			Object identifier,
			Object[] state,
			boolean[] insertable,
			InsertValuesAnalysis valuesAnalysis,
			TableInclusionChecker tableInclusionChecker,
			Supplier<Object> identifierSupplier) {
		this.entityPersister = entityPersister;
		this.entity = entity;
		this.identifier = identifier;
		this.state = state;
		this.insertable = insertable;
		this.valuesAnalysis = valuesAnalysis;
		this.tableInclusionChecker = tableInclusionChecker;
		this.identifierSupplier = identifierSupplier;
	}

	@Override
	public Supplier<Object> getEntityIdAccess() {
		return identifierSupplier;
	}

	@Override
	public Object getEntityInstance() {
		return entity;
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
				state,
				plannedOperation,
				insertable,
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

		executor.execute(
				entity,
				valuesAnalysis,
				tableInclusionChecker,
				InsertBindPlan::verifyOutcome,
				session
		);
	}

	protected void decomposeForInsert(
			MutationExecutor mutationExecutor,
			Object id,
			Object[] values,
			PlannedOperation plannedOperation,
			boolean[] propertyInclusions,
			SharedSessionContractImplementor session) {
		final var jdbcValueBindings = mutationExecutor.getJdbcValueBindings();
		final var attributeMappings = entityPersister.getAttributeMappings();

		final var operation = plannedOperation.getOperation();
		final var tableDetails = (EntityTableMapping) operation.getTableDetails();

		final int[] attributeIndexes = tableDetails.getAttributeIndexes();
		for ( int i = 0; i < attributeIndexes.length; i++ ) {
			final int attributeIndex = attributeIndexes[ i ];
			if ( propertyInclusions[attributeIndex] ) {
				decomposeAttribute( values[attributeIndex], session, jdbcValueBindings,
						tableDetails, attributeMappings.get( attributeIndex ) );
			}
		}

		if ( id == null ) {
			assert entityPersister.getInsertDelegate() != null;
		}
		else {
			breakDownKeyJdbcValue( id, session, jdbcValueBindings, tableDetails );
		}
	}

	protected void decomposeAttribute(
			Object value,
			SharedSessionContractImplementor session,
			JdbcValueBindings jdbcValueBindings,
			EntityTableMapping tableDetails,
			AttributeMapping mapping) {
		if ( mapping instanceof PluralAttributeMapping ) {
			return;
		}

		if ( !tableDetails.getTableName().equals( mapping.getContainingTableExpression() ) ) {
			return;
		}

		mapping.decompose(
				value,
				0,
				jdbcValueBindings,
				null,
				(valueIndex, bindings, noop, jdbcValue, selectableMapping) -> {
					if ( selectableMapping.isInsertable() ) {
						bindings.bindValue(
								jdbcValue,
								entityPersister.physicalTableNameForMutation( selectableMapping ),
								selectableMapping.getSelectionExpression(),
								ParameterUsage.SET
						);
					}
				},
				session
		);
	}

	protected void breakDownKeyJdbcValue(
			Object id,
			SharedSessionContractImplementor session,
			JdbcValueBindings jdbcValueBindings,
			EntityTableMapping tableDetails) {
		final String tableName = tableDetails.getTableName();
		tableDetails.getKeyMapping().breakDownKeyJdbcValues(
				id,
				(jdbcValue, columnMapping) -> {
					jdbcValueBindings.bindValue(
							jdbcValue,
							tableName,
							columnMapping.getColumnName(),
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
