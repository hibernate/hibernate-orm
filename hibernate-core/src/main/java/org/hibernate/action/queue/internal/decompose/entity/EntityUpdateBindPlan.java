/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.internal.decompose.entity;


import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.action.queue.internal.cyclebreak.CycleBreakPatcher;
import org.hibernate.action.queue.spi.bind.BindPlan;
import org.hibernate.action.queue.spi.bind.Checkers;
import org.hibernate.action.queue.spi.bind.GeneratedValuesCollector;
import org.hibernate.action.queue.spi.bind.JdbcValueBindings;
import org.hibernate.action.queue.spi.bind.OperationResultChecker;
import org.hibernate.action.queue.spi.meta.EntityTableDescriptor;
import org.hibernate.action.queue.spi.meta.TableKeyDescriptor;
import org.hibernate.action.queue.spi.plan.FlushOperation;
import org.hibernate.bytecode.enhance.spi.LazyPropertyInitializer;
import org.hibernate.engine.OptimisticLockStyle;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.ModelPart.JdbcValueBiConsumer;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.model.ValuesAnalysis;

import java.sql.SQLException;

import static org.hibernate.action.queue.internal.decompose.entity.BindPlanHelper.shouldBindJdbcValue;
import static org.hibernate.generator.EventType.UPDATE;

/// @author Steve Ebersole
public class EntityUpdateBindPlan implements BindPlan, OperationResultChecker {

	/// Static, non-capturing [JdbcValueBiConsumer] for binding attribute values for update assignment (SET).
	private static final JdbcValueBiConsumer<EntityUpdateBindPlan, AttributeMapping> UPDATE_SET_BINDER =
			(valueIndex, bindPlan, attribute, jdbcValue, selectableMapping) -> {
				if ( selectableMapping.isUpdateable()
						&& !selectableMapping.isFormula()
						&& bindPlan.shouldBindUpdateValue( attribute, valueIndex, bindPlan.bindingSession ) ) {
					bindPlan.valueBindings.bindValue(
							jdbcValue,
							selectableMapping.getSelectionExpression(),
							ParameterUsage.SET
					);
				}
			};

	/// Static, non-capturing [JdbcValueBiConsumer] for binding key values for update restriction (WHERE).
	private static final JdbcValueBiConsumer<EntityUpdateBindPlan, TableKeyDescriptor> KEY_RESTRICTION_BINDER =
			(index, bindPlan, keyDescriptor, jdbcValue, jdbcValueMapping) -> {
				final var keyColumn = keyDescriptor.getSelectable( index );
				bindPlan.valueBindings.bindRestriction( index, jdbcValue, keyColumn );
			};

	private final EntityTableDescriptor tableDescriptor;
	private final EntityPersister entityPersister;
	private final Object entity;
	private final Object identifier;
	private final Object rowId;
	private final Object[] state;
	private final Object[] previousState;
	private final Object version;
	private final boolean[] propertyUpdateability;
	private final OptimisticLockStyle effectiveOptLockStyle;
	private final UpdateValuesAnalysis valuesAnalysis;
	private final boolean isDynamic;
	private final GeneratedValuesCollector generatedValuesCollector;

	// temporary state used during decomposition to allow model-part decomposition to be non-capturing
	private JdbcValueBindings valueBindings;
	private SharedSessionContractImplementor bindingSession;
	private FlushOperation bindingFlushOperation;

	public EntityUpdateBindPlan(
			EntityTableDescriptor tableDescriptor,
			EntityPersister entityPersister,
			Object entity,
			Object identifier,
			Object rowId,
			Object[] state,
			Object[] previousState,
			Object version,
			boolean[] propertyUpdateability,
			OptimisticLockStyle effectiveOptLockStyle,
			UpdateValuesAnalysis valuesAnalysis,
			boolean isDynamic,
			GeneratedValuesCollector generatedValuesCollector) {
		this.tableDescriptor = tableDescriptor;
		this.entityPersister = entityPersister;
		this.entity = entity;
		this.identifier = identifier;
		this.rowId = rowId;
		this.state = state;
		this.previousState = previousState;
		this.version = version;
		this.propertyUpdateability = propertyUpdateability;
		this.effectiveOptLockStyle = effectiveOptLockStyle;
		this.valuesAnalysis = valuesAnalysis;
		this.isDynamic = isDynamic;
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

	@Override
	public ValuesAnalysis getValuesAnalysis() {
		return valuesAnalysis;
	}

	/// Get the current (new) state of the entity.
	/// Phase 3: Used for unique constraint value extraction.
	public Object[] getState() {
		return state;
	}

	/// Get the previous (old) state of the entity.
	/// Phase 3: Used for unique constraint value extraction.
	public Object[] getPreviousState() {
		return previousState;
	}

	@Override
	public void bindValues(
			JdbcValueBindings valueBindings,
			FlushOperation flushOperation,
			SharedSessionContractImplementor session) {
		decomposeForUpdate( valueBindings, flushOperation, session );

		if (flushOperation.getBindingPatch() != null) {
			CycleBreakPatcher.applyFixupPatch(
					valueBindings,
					flushOperation,
					flushOperation.getBindingPatch()
			);
		}
	}

	private void decomposeForUpdate(
			JdbcValueBindings valueBindings,
			FlushOperation flushOperation,
			SharedSessionContractImplementor session) {
		this.bindingFlushOperation = flushOperation;
		try {
			for ( int i = 0; i < tableDescriptor.attributes().size(); i++ ) {
				var attribute = tableDescriptor.attributes().get( i );
				if ( shouldIncludeInUpdate( attribute, session ) ) {
					decomposeAttributeForSet(
							state[attribute.getStateArrayPosition()],
							attribute,
							valueBindings,
							session
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
			if ( effectiveOptLockStyle.isVersion() ) {
				assert entityPersister.getVersionMapping() != null;
				applyVersionBasedOptLocking( valueBindings, session );
			}
			else if ( effectiveOptLockStyle.isAllOrDirty() ) {
				assert previousState != null;
				applyNonVersionOptLocking( valueBindings, session );
			}

			// Apply partitioned selection restrictions if needed
			if ( entityPersister.hasPartitionedSelectionMapping() && previousState != null ) {
				applyPartitionedSelectionRestrictions( valueBindings, session );
			}
		}
		finally {
			this.bindingFlushOperation = null;
		}
	}

	private void decomposeAttributeForSet(
			Object value,
			AttributeMapping attribute,
			JdbcValueBindings valueBindings,
			SharedSessionContractImplementor session) {
		assert !attribute.isPluralAttributeMapping();

		if ( value == LazyPropertyInitializer.UNFETCHED_PROPERTY ) {
			// it was not fetched and so could not have changed, skip it
			// EARLY EXIT!!!
			return;
		}

		this.valueBindings = valueBindings;
		this.bindingSession = session;
		try {
			attribute.decompose(
					value,
					0,
					this,
					attribute,
					UPDATE_SET_BINDER,
					session
			);
		}
		finally {
			this.valueBindings = null;
			this.bindingSession = null;
		}
	}

	private boolean shouldBindUpdateValue(
			AttributeMapping attribute,
			int selectableIndex,
			SharedSessionContractImplementor session) {
		return shouldBindJdbcValue(
				attribute,
				selectableIndex,
				bindingFlushOperation,
				entityPersister,
				UPDATE,
				entity,
				session
		);
	}

	protected boolean shouldIncludeInUpdate(
			AttributeMapping attribute,
			SharedSessionContractImplementor session) {
		final int attributeIndex = attribute.getStateArrayPosition();
		final var generator = attribute.getGenerator();

		final var versionMapping = entityPersister.getVersionMapping();
		if ( versionMapping != null
				&& versionMapping.getVersionAttribute() == attribute ) {
			return true;
		}

		if ( generator != null
				&& generator.generatesOnUpdate()
				&& generator.generatedBeforeExecution( entity, session ) ) {
			return true;
		}

		// Check if property is updateable
		if ( !propertyUpdateability[attributeIndex] ) {
			return false;
		}

		// If dynamic update with dirty fields, check if field is dirty
		if ( isDynamic && valuesAnalysis.hasDirtyAttributes() ) {
			return valuesAnalysis.getDirtiness()[attributeIndex];
		}

		return true;
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
		// when building the static UPDATE operation
		final var keyDescriptor = tableDescriptor.keyDescriptor();

		this.valueBindings = valueBindings;
		try {
			entityPersister.getIdentifierMapping().breakDownJdbcValues(
					identifier,
					0,
					this,
					keyDescriptor,
					KEY_RESTRICTION_BINDER,
					session
			);
		}
		finally {
			this.valueBindings = null;
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
		if ( previousState == null ) {
			throw new IllegalStateException( "This should never happen based on previous handling" );
		}

		final boolean[] versionability = entityPersister.getPropertyVersionability();
		final boolean isDirtyOptLock = effectiveOptLockStyle.isDirty();

		for ( int i = 0; i < tableDescriptor.attributes().size(); i++ ) {
			var attribute = tableDescriptor.attributes().get( i );
			assert !attribute.isPluralAttributeMapping();
			if ( !versionability[attribute.getStateArrayPosition()] ) {
				continue;
			}

			// For DIRTY optimistic locking, only include dirty fields in WHERE clause
			if ( isDirtyOptLock && valuesAnalysis != null && valuesAnalysis.hasDirtyAttributes() ) {
				if ( !valuesAnalysis.getDirtiness()[attribute.getStateArrayPosition()] ) {
					continue;
				}
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
					if ( selectableMapping.isUpdateable() && !selectableMapping.isFormula() && jdbcValue != null ) {
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
									if ( selectable.isPartitioned() && jdbcValue != null ) {
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
			FlushOperation flushOperation,
			int affectedRowCount,
			int batchPosition,
			String sqlString,
			SessionFactoryImplementor sessionFactory) throws SQLException {
		return checkResult(
				(EntityTableDescriptor) flushOperation.getMutatingTableDescriptor(),
				affectedRowCount,
				batchPosition,
				sqlString,
				sessionFactory
		);
	}

	@Override
	public boolean checkResult(
			int affectedRowCount,
			int batchPosition,
			String sqlString,
			SessionFactoryImplementor sessionFactory) throws SQLException {
		return checkResult( tableDescriptor, affectedRowCount, batchPosition, sqlString, sessionFactory );
	}

	private boolean checkResult(
			EntityTableDescriptor tableDescriptor,
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
