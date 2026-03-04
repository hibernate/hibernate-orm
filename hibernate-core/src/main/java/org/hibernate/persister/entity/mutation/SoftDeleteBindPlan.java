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
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.SoftDeleteMapping;
import org.hibernate.persister.entity.EntityPersister;

import java.sql.SQLException;

/// Specialized BindPlan for soft delete operations.
///
/// Unlike hard deletes, soft deletes are actually `UPDATE` operations that set a
/// soft-delete indicator column.
///
/// @author Steve Ebersole
public class SoftDeleteBindPlan implements BindPlan {
	private final EntityPersister entityPersister;
	private final Object identifier;
	private final Object version;
	private final Object[] loadedState;
	private final boolean applyOptimisticLocking;
	private final SoftDeleteMapping softDeleteMapping;

	public SoftDeleteBindPlan(
			EntityPersister entityPersister,
			Object identifier,
			Object version,
			Object[] loadedState,
			boolean applyOptimisticLocking,
			SoftDeleteMapping softDeleteMapping) {
		this.entityPersister = entityPersister;
		this.identifier = identifier;
		this.version = version;
		this.loadedState = loadedState;
		this.applyOptimisticLocking = applyOptimisticLocking;
		this.softDeleteMapping = softDeleteMapping;
	}

	@Override
	public void bindAndMaybePatch(
			MutationExecutor executor,
			PlannedOperation plannedOperation,
			SharedSessionContractImplementor session) {
		final var operation = plannedOperation.getOperation();
		final var tableDetails = (EntityTableMapping) operation.getTableDetails();

		decomposeForSoftDelete(
				executor,
				identifier,
				version,
				loadedState,
				plannedOperation,
				applyOptimisticLocking,
				session
		);
	}

	@Override
	public void execute(
			MutationExecutor executor,
			PlannedOperation plannedOperation,
			SharedSessionContractImplementor session) {
		executor.execute(
				null,  // entity instance not needed
				null,  // valuesAnalysis not needed
				null,  // tableInclusionChecker not needed
				SoftDeleteBindPlan::verifyOutcome,
				session
		);
	}

	protected void decomposeForSoftDelete(
			MutationExecutor mutationExecutor,
			Object id,
			Object version,
			Object[] loadedState,
			PlannedOperation plannedOperation,
			boolean applyOptimisticLocking,
			SharedSessionContractImplementor session) {
		final var jdbcValueBindings = mutationExecutor.getJdbcValueBindings();
		final var operation = plannedOperation.getOperation();
		final var tableDetails = (EntityTableMapping) operation.getTableDetails();

		// NOTE: We do NOT bind the soft delete value or non-deleted restriction here.
		// These are literal values (e.g., true/false or CURRENT_TIMESTAMP) that are
		// already embedded in the SQL statement. They have no parameters to bind.

		// Bind the identifier for the WHERE clause
		breakDownKeyJdbcValue( id, session, jdbcValueBindings, tableDetails );

		// Apply optimistic locking if needed
		if ( applyOptimisticLocking ) {
			applyOptimisticLockingRestrictions(
					version,
					loadedState,
					jdbcValueBindings,
					tableDetails,
					session
			);
		}
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
							ParameterUsage.RESTRICT
					);
				},
				session
		);
	}

	protected void applyOptimisticLockingRestrictions(
			Object version,
			Object[] loadedState,
			JdbcValueBindings jdbcValueBindings,
			EntityTableMapping tableDetails,
			SharedSessionContractImplementor session) {
		final OptimisticLockStyle optimisticLockStyle = entityPersister.optimisticLockStyle();

		if ( optimisticLockStyle.isVersion() && entityPersister.getVersionMapping() != null ) {
			applyVersionBasedOptLocking( version, jdbcValueBindings, tableDetails, session );
		}
		else if ( loadedState != null && optimisticLockStyle.isAllOrDirty() ) {
			applyNonVersionOptLocking( loadedState, jdbcValueBindings, tableDetails, session );
		}

		// Apply partitioned selection restrictions if needed
		if ( entityPersister.hasPartitionedSelectionMapping() && loadedState != null ) {
			applyPartitionedSelectionRestrictions( loadedState, jdbcValueBindings, tableDetails, session );
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
			Object[] loadedState,
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
					decomposeAttributeForRestriction(
							loadedState[attributeIndex],
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
			Object[] loadedState,
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
						final Object value = loadedState != null ? loadedState[m] : null;
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
					if ( selectableMapping.isUpdateable() ) {
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
