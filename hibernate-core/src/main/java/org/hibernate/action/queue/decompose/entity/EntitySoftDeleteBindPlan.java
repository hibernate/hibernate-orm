/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.decompose.entity;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.action.queue.exec.BindPlan;
import org.hibernate.action.queue.exec.Checkers;
import org.hibernate.action.queue.exec.ExecutionContext;
import org.hibernate.action.queue.exec.JdbcValueBindings;
import org.hibernate.action.queue.exec.OperationResultChecker;
import org.hibernate.action.queue.meta.EntityTableDescriptor;
import org.hibernate.action.queue.plan.PlannedOperation;
import org.hibernate.engine.OptimisticLockStyle;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.entity.EntityPersister;

import java.sql.SQLException;

/// Specialized BindPlan for soft delete operations.
///
/// Unlike hard deletes, soft deletes are actually `UPDATE` operations that set a
/// soft-delete indicator column.
///
/// @author Steve Ebersole
public class EntitySoftDeleteBindPlan implements BindPlan, OperationResultChecker {
	private final EntityTableDescriptor tableDescriptor;
	private final EntityPersister entityPersister;
	private final Object identifier;
	private final Object version;
	private final Object[] loadedState;
	private final OptimisticLockStyle effectiveOptLockStyle;

	public EntitySoftDeleteBindPlan(
			EntityTableDescriptor tableDescriptor,
			EntityPersister entityPersister,
			Object identifier,
			Object version,
			Object[] loadedState,
			OptimisticLockStyle effectiveOptLockStyle) {
		this.tableDescriptor = tableDescriptor;
		this.entityPersister = entityPersister;
		this.identifier = identifier;
		this.version = version;
		this.loadedState = loadedState;
		this.effectiveOptLockStyle = effectiveOptLockStyle;
	}

	@Override
	public @Nullable Object getEntityId() {
		return identifier;
	}

	@Override
	public void execute(
			ExecutionContext context,
			PlannedOperation plannedOperation,
			SharedSessionContractImplementor session) {
		context.executeRow(
				plannedOperation,
				(jdbcValueBindings, s) -> bindValues( jdbcValueBindings, plannedOperation, session ),
				this
		);
	}

	private void bindValues(
			JdbcValueBindings valueBindings,
			PlannedOperation plannedOperation,
			SharedSessionContractImplementor session) {
		// NOTE: We do NOT bind the soft delete value or non-deleted restriction here.
		// These are literal values (e.g., true/false or CURRENT_TIMESTAMP) that are
		// already embedded in the SQL statement. They have no parameters to bind.

		// Bind the identifier for the WHERE clause
		breakDownKeyJdbcValue( valueBindings, session );

		// Apply optimistic locking if needed
		if ( effectiveOptLockStyle == OptimisticLockStyle.VERSION ) {
			applyVersionBasedOptLocking( valueBindings, session );
		}
		else if ( effectiveOptLockStyle.isAllOrDirty() ) {
			applyNonVersionOptLocking( valueBindings, session );
		}

		// Apply partitioned selection restrictions if needed
		if ( entityPersister.hasPartitionedSelectionMapping() && loadedState != null ) {
			applyPartitionedSelectionRestrictions( valueBindings, session );
		}
	}

	private void breakDownKeyJdbcValue(
			JdbcValueBindings valueBindings,
			SharedSessionContractImplementor session) {
		entityPersister.getIdentifierMapping().breakDownJdbcValues(
				identifier,
				(index, jdbcValue, columnMapping) -> {
					valueBindings.bindValue(
							jdbcValue,
							tableDescriptor.keyDescriptor().columns().get( index ).name(),
							ParameterUsage.RESTRICT
					);
				},
				session
		);
	}

	protected void applyVersionBasedOptLocking(
			JdbcValueBindings jdbcValueBindings,
			SharedSessionContractImplementor session) {
		final var versionMapping = entityPersister.getVersionMapping();
		if ( versionMapping == null ) {
			return;
		}
		if ( tableDescriptor.name().equals( versionMapping.getContainingTableExpression() ) ) {
			versionMapping.decompose(
					version,
					0,
					jdbcValueBindings,
					null,
					(valueIndex, bindings, noop, jdbcValue, selectableMapping) -> {
						bindings.bindValue(
								jdbcValue,
								selectableMapping.getSelectionExpression(),
								ParameterUsage.RESTRICT
						);
					},
					session
			);
		}
	}

	protected void applyNonVersionOptLocking(
			JdbcValueBindings jdbcValueBindings,
			SharedSessionContractImplementor session) {
		if ( loadedState == null ) {
			throw new IllegalStateException( "This should never happen based on previous handling" );
		}

		final boolean[] versionability = entityPersister.getPropertyVersionability();
		for ( int i = 0; i < tableDescriptor.attributes().size(); i++ ) {
			var attribute = tableDescriptor.attributes().get( i );
			if ( !versionability[attribute.getStateArrayPosition()] ) {
				continue;
			}
			var loadedValue = loadedState[attribute.getStateArrayPosition()];
			attribute.decompose(
					loadedValue,
					(valueIndex, jdbcValue, selectableMapping) -> {
						if ( !selectableMapping.isFormula() && jdbcValue != null ) {
							jdbcValueBindings.bindValue(
									jdbcValue,
									selectableMapping.getSelectionExpression(),
									ParameterUsage.RESTRICT
							);
						}
					},
					session
			);
		}
	}

	protected void applyPartitionedSelectionRestrictions(
			JdbcValueBindings jdbcValueBindings,
			SharedSessionContractImplementor session) {
		for ( int i = 0; i < tableDescriptor.attributes().size(); i++ ) {
			var attribute = tableDescriptor.attributes().get( i );

			attribute.forEachSelectable( (selectionIndex, selectableMapping) -> {
				if ( selectableMapping.isPartitioned() ) {
					final String tableNameForMutation = entityPersister.physicalTableNameForMutation( selectableMapping );
					final Object value = loadedState != null
							? loadedState[attribute.getStateArrayPosition()]
							: null;
					if ( value != null ) {
						attribute.decompose(
								value,
								0,
								jdbcValueBindings,
								null,
								(valueIndex, bindings, noop, jdbcValue, selectable) -> {
									if ( selectable.isPartitioned() ) {
										bindings.bindValue(
												jdbcValue,
												( selectable.getSelectionExpression() ),
												ParameterUsage.RESTRICT
										);
									}
								},
								session
						);
					}
				}
			} );
		}
	}

	@Override
	public OperationResultChecker getOperationResultChecker() {
		return this;
	}

	@Override
	public boolean checkResult(
			int affectedRowCount,
			int batchPosition,
			String sqlString,
			SessionFactoryImplementor sessionFactory) throws SQLException {
		return Checkers.identifiedResultsCheck(
				tableDescriptor.deleteDetails().getExpectation(),
				affectedRowCount,
				batchPosition,
				entityPersister,
				tableDescriptor,
				identifier,
				sqlString,
				sessionFactory
		);
	}
}
