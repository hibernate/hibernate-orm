/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.entity.mutation;

import org.hibernate.action.queue.bind.BindPlan;
import org.hibernate.action.queue.plan.PlannedOperation;
import org.hibernate.engine.OptimisticLockStyle;
import org.hibernate.engine.jdbc.mutation.JdbcValueBindings;
import org.hibernate.engine.jdbc.mutation.MutationExecutor;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.jdbc.mutation.group.PreparedStatementDetails;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.values.GeneratedValues;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.persister.entity.EntityPersister;

import java.sql.SQLException;

/// BindPlan for entity update operations.
///
/// @see GeneratedValuesCollector
///
/// @author Steve Ebersole
public class UpdateBindPlan implements BindPlan {
	private final EntityPersister entityPersister;
	private final Object entity;
	private final Object identifier;
	private final Object rowId;
	private final Object[] state;
	private final Object[] previousState;
	private final Object version;
	private final int[] dirtyFields;
	private final boolean[] updateable;
	private final boolean applyOptimisticLocking;
	private final UpdateValuesAnalysisForDecomposer valuesAnalysis;

	public UpdateBindPlan(
			EntityPersister entityPersister,
			Object entity,
			Object identifier,
			Object rowId,
			Object[] state,
			Object[] previousState,
			Object version,
			int[] dirtyFields,
			boolean[] updateable,
			boolean applyOptimisticLocking,
			UpdateValuesAnalysisForDecomposer valuesAnalysis) {
		this.entityPersister = entityPersister;
		this.entity = entity;
		this.identifier = identifier;
		this.rowId = rowId;
		this.state = state;
		this.previousState = previousState;
		this.version = version;
		this.dirtyFields = dirtyFields;
		this.updateable = updateable;
		this.applyOptimisticLocking = applyOptimisticLocking;
		this.valuesAnalysis = valuesAnalysis;
	}

	@Override
	public void bindAndMaybePatch(
			MutationExecutor executor,
			PlannedOperation plannedOperation,
			SharedSessionContractImplementor session) {
		final var operation = plannedOperation.getOperation();
		final var tableDetails = (EntityTableMapping) operation.getTableDetails();

		// Check if this table should be included in the update
		if ( !shouldIncludeTable( tableDetails ) ) {
			return;
		}

		decomposeForUpdate(
				executor,
				identifier,
				rowId,
				state,
				previousState,
				version,
				plannedOperation,
				updateable,
				applyOptimisticLocking,
				session
		);
	}

	@Override
	public void execute(
			MutationExecutor executor,
			PlannedOperation plannedOperation,
			SharedSessionContractImplementor session) {
		final var tableDetails = (EntityTableMapping) plannedOperation.getOperation().getTableDetails();

		// Check if this table should be included in the update
		if ( !shouldIncludeTable( tableDetails ) ) {
			return;
		}

		final GeneratedValues generatedValues = executor.execute(
				null,  // entity instance not needed
				null,  // valuesAnalysis not needed
				null,  // tableInclusionChecker not needed
				UpdateBindPlan::verifyOutcome,
				session
		);

		if ( generatedValues != null ) {
			entityPersister.processUpdateGeneratedProperties( identifier, entity, state, generatedValues, session );
		}
	}

	/**
	 * Determines if a table should be included in the update.
	 * Handles optional tables by checking if they have non-null values.
	 */
	private boolean shouldIncludeTable(EntityTableMapping tableMapping) {
		if ( tableMapping.isOptional() && !valuesAnalysis.hasNonNullValues( tableMapping ) ) {
			// The table is optional, and we have null values for all of its columns
			// Only include if there are dirty attributes (to potentially set nulls)
			return valuesAnalysis.hasDirtyAttributes();
		}

		// For non-optional tables or optional tables with non-null values,
		// include if the table needs updating
		return valuesAnalysis.needsUpdate( tableMapping );
	}

	protected void decomposeForUpdate(
			MutationExecutor mutationExecutor,
			Object id,
			Object rowId,
			Object[] values,
			Object[] previousValues,
			Object version,
			PlannedOperation plannedOperation,
			boolean[] propertyUpdateability,
			boolean applyOptimisticLocking,
			SharedSessionContractImplementor session) {
		final var jdbcValueBindings = mutationExecutor.getJdbcValueBindings();
		final var operation = plannedOperation.getOperation();
		final var tableDetails = (EntityTableMapping) operation.getTableDetails();
		final var attributeMappings = entityPersister.getAttributeMappings();

		// Bind the SET clause values (updated attributes)
		final int[] attributeIndexes = tableDetails.getAttributeIndexes();
		for ( int i = 0; i < attributeIndexes.length; i++ ) {
			final int attributeIndex = attributeIndexes[i];
			if ( shouldIncludeInUpdate( attributeIndex, propertyUpdateability ) ) {
				final var attributeMapping = attributeMappings.get( attributeIndex );
				decomposeAttributeForSet(
						values[attributeIndex],
						session,
						jdbcValueBindings,
						tableDetails,
						attributeMapping
				);
			}
		}

		// Bind the WHERE clause - identifier
		breakDownKeyJdbcValue( id, rowId, session, jdbcValueBindings, tableDetails );

		// Apply optimistic locking if needed
		if ( applyOptimisticLocking ) {
			applyOptimisticLockingRestrictions(
					version,
					previousValues,
					jdbcValueBindings,
					tableDetails,
					session
			);
		}

		// Apply partitioned selection restrictions if needed
		if ( entityPersister.hasPartitionedSelectionMapping() && previousValues != null ) {
			applyPartitionedSelectionRestrictions( previousValues, jdbcValueBindings, tableDetails, session );
		}
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

	private boolean contains(int[] array, int value) {
		if ( array == null ) {
			return false;
		}
		for ( int i : array ) {
			if ( i == value ) {
				return true;
			}
		}
		return false;
	}

	protected void decomposeAttributeForSet(
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
					if ( selectableMapping.isUpdateable() && !selectableMapping.isFormula() ) {
						bindings.bindValue(
								jdbcValue,
								tableDetails.getTableName(),
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
			Object rowId,
			SharedSessionContractImplementor session,
			JdbcValueBindings jdbcValueBindings,
			EntityTableMapping tableDetails) {
		final String tableName = tableDetails.getTableName();

		// Use rowId if available and applicable
		if ( rowId != null && shouldUseRowId( tableDetails ) ) {
			final var rowIdMapping = entityPersister.getRowIdMapping();
			if ( rowIdMapping != null ) {
				rowIdMapping.decompose(
						rowId,
						0,
						jdbcValueBindings,
						null,
						(valueIndex, bindings, noop, jdbcValue, selectableMapping) -> {
							bindings.bindValue(
									jdbcValue,
									tableName,
									selectableMapping.getSelectionExpression(),
									ParameterUsage.RESTRICT
							);
						},
						session
				);
				return;
			}
		}

		// Otherwise use identifier
		tableDetails.getKeyMapping().breakDownKeyJdbcValues(
				id,
				(jdbcValue, columnMapping) -> {
					jdbcValueBindings.bindValue(
							jdbcValue,
							tableName,
							columnMapping.getColumnName(),
							ParameterUsage.RESTRICT
					);
				},
				session
		);
	}

	private boolean shouldUseRowId(EntityTableMapping tableDetails) {
		// RowId is only used for the identifier table
		return tableDetails.isIdentifierTable();
	}

	protected void applyOptimisticLockingRestrictions(
			Object version,
			Object[] previousValues,
			JdbcValueBindings jdbcValueBindings,
			EntityTableMapping tableDetails,
			SharedSessionContractImplementor session) {
		final OptimisticLockStyle optimisticLockStyle = entityPersister.optimisticLockStyle();

		if ( optimisticLockStyle.isVersion() && entityPersister.getVersionMapping() != null ) {
			applyVersionBasedOptLocking( version, jdbcValueBindings, tableDetails, session );
		}
		else if ( previousValues != null && optimisticLockStyle.isAllOrDirty() ) {
			applyNonVersionOptLocking( previousValues, jdbcValueBindings, tableDetails, session );
		}
	}

	protected void applyVersionBasedOptLocking(
			Object version,
			JdbcValueBindings jdbcValueBindings,
			EntityTableMapping tableDetails,
			SharedSessionContractImplementor session) {
		final var versionMapping = entityPersister.getVersionMapping();
		if ( versionMapping != null && tableDetails.getTableName().equals( versionMapping.getContainingTableExpression() ) ) {
			versionMapping.decompose(
					version,
					0,
					jdbcValueBindings,
					null,
					(valueIndex, bindings, noop, jdbcValue, selectableMapping) -> {
						bindings.bindValue(
								jdbcValue,
								tableDetails.getTableName(),
								selectableMapping.getSelectionExpression(),
								ParameterUsage.RESTRICT
						);
					},
					session
			);
		}
	}

	protected void applyNonVersionOptLocking(
			Object[] previousValues,
			JdbcValueBindings jdbcValueBindings,
			EntityTableMapping tableDetails,
			SharedSessionContractImplementor session) {
		final boolean[] versionability = entityPersister.getPropertyVersionability();
		final var attributeMappings = entityPersister.getAttributeMappings();

		for ( int attributeIndex = 0; attributeIndex < versionability.length; attributeIndex++ ) {
			if ( versionability[attributeIndex] ) {
				final var attribute = attributeMappings.get( attributeIndex );
				if ( !attribute.isPluralAttributeMapping()
						&& tableDetails.getTableName().equals( attribute.getContainingTableExpression() ) ) {
					// Note: Even for DIRTY locking, we must bind all versionable fields to the WHERE clause
					// because the SQL generator includes all versionable fields in the WHERE clause.
					// The "DIRTY" aspect is handled at the SQL generation level, not at the binding level.
					decomposeAttributeForRestriction(
							previousValues[attributeIndex],
							session,
							jdbcValueBindings,
							tableDetails,
							attribute
					);
				}
			}
		}
	}

	protected void applyPartitionedSelectionRestrictions(
			Object[] previousValues,
			JdbcValueBindings jdbcValueBindings,
			EntityTableMapping tableDetails,
			SharedSessionContractImplementor session) {
		final var attributeMappings = entityPersister.getAttributeMappings();

		for ( int m = 0; m < attributeMappings.size(); m++ ) {
			final var attributeMapping = attributeMappings.get( m );
			final int jdbcTypeCount = attributeMapping.getJdbcTypeCount();
			for ( int i = 0; i < jdbcTypeCount; i++ ) {
				final var selectableMapping = attributeMapping.getSelectable( i );
				if ( selectableMapping.isPartitioned() ) {
					final String tableNameForMutation = entityPersister.physicalTableNameForMutation( selectableMapping );
					if ( tableDetails.getTableName().equals( tableNameForMutation ) ) {
						final Object value = previousValues != null ? previousValues[m] : null;
						if ( value != null ) {
							attributeMapping.decompose(
									value,
									0,
									jdbcValueBindings,
									null,
									(valueIndex, bindings, noop, jdbcValue, selectable) -> {
										if ( selectable.isPartitioned() ) {
											bindings.bindValue(
													jdbcValue,
													tableDetails.getTableName(),
													selectable.getSelectionExpression(),
													ParameterUsage.RESTRICT
											);
										}
									},
									session
							);
						}
					}
				}
			}
		}
	}

	protected void decomposeAttributeForRestriction(
			Object value,
			SharedSessionContractImplementor session,
			JdbcValueBindings jdbcValueBindings,
			EntityTableMapping tableDetails,
			AttributeMapping mapping) {
		if ( mapping instanceof PluralAttributeMapping ) {
			return;
		}

		mapping.decompose(
				value,
				0,
				jdbcValueBindings,
				null,
				(valueIndex, bindings, noop, jdbcValue, selectableMapping) -> {
					if ( selectableMapping.isUpdateable() && !selectableMapping.isFormula() ) {
						bindings.bindValue(
								jdbcValue,
								tableDetails.getTableName(),
								selectableMapping.getSelectionExpression(),
								ParameterUsage.RESTRICT
						);
					}
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
