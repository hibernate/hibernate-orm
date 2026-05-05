/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.decompose.entity;

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

/**
 * @author Steve Ebersole
 */
public class EntityDeleteBindPlan implements BindPlan, OperationResultChecker {
	private final EntityTableDescriptor tableDescriptor;
	private final EntityPersister entityPersister;
	private final Object identifier;
	private final Object rowId;
	private final Object version;
	private final Object[] state;
	private final Object[] loadedState;
	private final int[] sameFlushUpdatedAttributeIndexes;
	private final OptimisticLockStyle effectiveOptLockStyle;

	public EntityDeleteBindPlan(
			EntityTableDescriptor tableDescriptor,
			EntityPersister entityPersister,
			Object identifier,
			Object rowId,
			Object version,
			Object[] state,
			Object[] loadedState,
			int[] sameFlushUpdatedAttributeIndexes,
			OptimisticLockStyle effectiveOptLockStyle) {
		this.tableDescriptor = tableDescriptor;
		this.entityPersister = entityPersister;
		this.identifier = identifier;
		this.rowId = rowId;
		this.version = version;
		this.state = state;
		this.loadedState = loadedState;
		this.sameFlushUpdatedAttributeIndexes = sameFlushUpdatedAttributeIndexes;
		this.effectiveOptLockStyle = effectiveOptLockStyle;
	}

	@Override
	public Object getEntityId() {
		return identifier;
	}

	@Override
	public Object[] getLoadedState() {
		// Prefer loadedState if available (used for optimistic locking scenarios)
		// Otherwise use state (which is always available from EntityDeleteAction)
		return loadedState != null ? loadedState : state;
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

		// Bind the identifier for the WHERE clause
		breakDownKeyJdbcValue( valueBindings, session );

		if ( tableDescriptor.isIdentifierTable() ) {
			entityPersister.bindDiscriminatorForDelete( valueBindings );
		}

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
		if ( rowId != null && tableDescriptor.isIdentifierTable() && entityPersister.getRowIdMapping() != null ) {
			valueBindings.bindValue(
					rowId,
					entityPersister.getRowIdMapping().getRowIdName(),
					ParameterUsage.RESTRICT
			);
			return;
		}

		// Bind using the table's key columns, which match what was used
		// when building the static DELETE operation
		final var keyDescriptor = tableDescriptor.keyDescriptor();

		entityPersister.getIdentifierMapping().breakDownJdbcValues(
				identifier,
				(index, jdbcValue, jdbcValueMapping) -> {
					// Use the table key column (e.g., FK column for joined subclass)
					// not the entity identifier column
					final var keyColumn = keyDescriptor.getSelectable(index);
					valueBindings.bindRestriction(index, jdbcValue, keyColumn);
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
			if ( contains( sameFlushUpdatedAttributeIndexes, attribute.getStateArrayPosition() ) ) {
				continue;
			}
			if ( !versionability[attribute.getStateArrayPosition()] ) {
				continue;
			}
			attribute.decompose(
					loadedState[attribute.getStateArrayPosition()],
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

	private static boolean contains(int[] values, int value) {
		if ( values == null ) {
			return false;
		}
		for ( int candidate : values ) {
			if ( candidate == value ) {
				return true;
			}
		}
		return false;
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
												selectable.getSelectionExpression(),
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
