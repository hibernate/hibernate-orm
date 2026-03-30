/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.bind;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.action.queue.cyclebreak.CycleBreakPatcher;
import org.hibernate.action.queue.exec.ExecutionContext;
import org.hibernate.action.queue.exec.OperationResultChecker;
import org.hibernate.action.queue.meta.EntityTableDescriptor;
import org.hibernate.action.queue.plan.PlannedOperation;
import org.hibernate.engine.OptimisticLockStyle;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.mutation.UpdateValuesAnalysisForDecomposer;

import java.sql.SQLException;

import static org.hibernate.internal.util.collections.ArrayHelper.contains;

/**
 * @author Steve Ebersole
 */
public class EntityUpdateBindPlan implements BindPlan, OperationResultChecker {
	private final EntityTableDescriptor tableDescriptor;
	private final EntityPersister entityPersister;
	private final Object entity;
	private final Object identifier;
	private final Object rowId;
	private final Object[] state;
	private final Object[] previousState;
	private final Object version;
	private final int[] dirtyFields;
	private final boolean[] propertyUpdateability;
	private final boolean applyOptimisticLocking;
	private final UpdateValuesAnalysisForDecomposer valuesAnalysis;
	private final GeneratedValuesCollector generatedValuesCollector;

	public EntityUpdateBindPlan(
			EntityTableDescriptor tableDescriptor,
			EntityPersister entityPersister,
			Object entity,
			Object identifier,
			Object rowId,
			Object[] state,
			Object[] previousState,
			Object version,
			int[] dirtyFields,
			boolean[] propertyUpdateability,
			boolean applyOptimisticLocking,
			UpdateValuesAnalysisForDecomposer valuesAnalysis,
			GeneratedValuesCollector generatedValuesCollector) {
		this.tableDescriptor = tableDescriptor;
		this.entityPersister = entityPersister;
		this.entity = entity;
		this.identifier = identifier;
		this.rowId = rowId;
		this.state = state;
		this.previousState = previousState;
		this.version = version;
		this.dirtyFields = dirtyFields;
		this.propertyUpdateability = propertyUpdateability;
		this.applyOptimisticLocking = applyOptimisticLocking;
		this.valuesAnalysis = valuesAnalysis;
		this.generatedValuesCollector = generatedValuesCollector;
	}

	@Override
	public @Nullable Object getEntityId() {
		return identifier;
	}

	@Override
	public @Nullable Object getEntityInstance() {
		return entity;
	}

	@Override
	public GeneratedValuesCollector getGeneratedValuesCollector() {
		return generatedValuesCollector;
	}

	/**
	 * Get the current (new) state of the entity.
	 * Phase 3: Used for unique constraint value extraction.
	 */
	public Object[] getState() {
		return state;
	}

	/**
	 * Get the previous (old) state of the entity.
	 * Phase 3: Used for unique constraint value extraction.
	 */
	public Object[] getPreviousState() {
		return previousState;
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
		decomposeForUpdate( valueBindings, plannedOperation, session );

		if (plannedOperation.getBindingPatch() != null) {
			CycleBreakPatcher.applyFixupPatch(
					valueBindings,
					plannedOperation,
					plannedOperation.getBindingPatch()
			);
		}
	}

	private void decomposeForUpdate(
			JdbcValueBindings valueBindings,
			PlannedOperation plannedOperation,
			SharedSessionContractImplementor session) {
		for ( int i = 0; i < tableDescriptor.attributes().size(); i++ ) {
			var attribute = tableDescriptor.attributes().get( i );
			if ( shouldIncludeInUpdate( attribute.getStateArrayPosition(), propertyUpdateability ) ) {
				decomposeAttributeForSet(
						state[attribute.getStateArrayPosition()],
						attribute, valueBindings, session
				);
			}
		}

		if ( identifier == null ) {
			assert entityPersister.getInsertDelegate() != null;
		}
		else {
			breakDownKeyJdbcValue( valueBindings, session );
		}

		// Apply optimistic locking if needed
		if ( applyOptimisticLocking ) {
			applyOptimisticLockingRestrictions( valueBindings, session );
		}

		// Apply partitioned selection restrictions if needed
		if ( entityPersister.hasPartitionedSelectionMapping() && previousState != null ) {
			applyPartitionedSelectionRestrictions( valueBindings, session );
		}
	}

	private void decomposeAttributeForSet(
			Object value,
			AttributeMapping attribute,
			JdbcValueBindings valueBindings,
			SharedSessionContractImplementor session) {
		assert !attribute.isPluralAttributeMapping();

		attribute.decompose(
				value,
				0,
				valueBindings,
				null,
				(valueIndex, bindings, noop, jdbcValue, selectableMapping) -> {
					if ( selectableMapping.isUpdateable() && !selectableMapping.isFormula() ) {
						bindings.bindValue(
								jdbcValue,
								( selectableMapping.getSelectionExpression() ),
								ParameterUsage.SET
						);
					}
				},
				session
		);
	}

	protected boolean shouldIncludeInUpdate(int attributeIndex, boolean[] propertyUpdateability) {
		// Check if property is updateable
		if ( !propertyUpdateability[attributeIndex] ) {
			return false;
		}

		// If dynamic update with dirty fields, check if field is dirty
		if ( entityPersister.isDynamicUpdate() && dirtyFields != null ) {
			return contains( dirtyFields, attributeIndex );
		}

		return true;
	}

	private void breakDownKeyJdbcValue(
			JdbcValueBindings valueBindings,
			SharedSessionContractImplementor session) {
		// Bind using the table's key columns, which match what was used
		// when building the static UPDATE operation
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

	protected void applyOptimisticLockingRestrictions(
			JdbcValueBindings jdbcValueBindings,
			SharedSessionContractImplementor session) {
		final OptimisticLockStyle optimisticLockStyle = entityPersister.optimisticLockStyle();

		if ( optimisticLockStyle.isVersion() && entityPersister.getVersionMapping() != null ) {
			applyVersionBasedOptLocking( jdbcValueBindings, session );
		}
		else if ( previousState != null && optimisticLockStyle.isAllOrDirty() ) {
			applyNonVersionOptLocking( jdbcValueBindings, session );
		}
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
								( selectableMapping.getSelectionExpression() ),
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
		final boolean[] versionability = entityPersister.getPropertyVersionability();

		for ( int i = 0; i < tableDescriptor.attributes().size(); i++ ) {
			var attribute = tableDescriptor.attributes().get( i );
			assert !attribute.isPluralAttributeMapping();
			if ( !versionability[attribute.getStateArrayPosition()] ) {
				continue;
			}

			attribute.decompose(
					previousState[attribute.getStateArrayPosition()],
					(valueIndex, jdbcValue, selectableMapping) -> {
						if ( selectableMapping.isUpdateable() && !selectableMapping.isFormula() && jdbcValue != null) {
							jdbcValueBindings.bindValue(
									jdbcValue,
									( selectableMapping.getSelectionExpression() ),
									ParameterUsage.RESTRICT
							);
						}
					},
					session
			);
		}
	}

	protected void decomposeAttributeForRestriction(
			Object value,
			JdbcValueBindings jdbcValueBindings,
			AttributeMapping attribute,
			SharedSessionContractImplementor session) {
		assert !attribute.isPluralAttributeMapping();

		attribute.decompose(
				value,
				0,
				jdbcValueBindings,
				null,
				(valueIndex, bindings, noop, jdbcValue, selectableMapping) -> {
					if ( selectableMapping.isUpdateable() && !selectableMapping.isFormula() ) {
						bindings.bindValue(
								jdbcValue,
								( selectableMapping.getSelectionExpression() ),
								ParameterUsage.RESTRICT
						);
					}
				},
				session
		);
	}

	protected void applyPartitionedSelectionRestrictions(
			JdbcValueBindings jdbcValueBindings,
			SharedSessionContractImplementor session) {
		for ( int i = 0; i < tableDescriptor.attributes().size(); i++ ) {
			var attribute = tableDescriptor.attributes().get( i );

			attribute.forEachSelectable( (selectionIndex, selectableMapping) -> {
				if ( selectableMapping.isPartitioned() ) {
					final String tableNameForMutation = entityPersister.physicalTableNameForMutation( selectableMapping );
					final Object value = previousState != null
							? previousState[attribute.getStateArrayPosition()]
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
				tableDescriptor.updateDetails().getExpectation(),
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
