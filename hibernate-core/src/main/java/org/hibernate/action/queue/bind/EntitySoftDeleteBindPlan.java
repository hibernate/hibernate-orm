/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.bind;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.action.queue.Helper;
import org.hibernate.action.queue.meta.EntityTableDescriptor;
import org.hibernate.action.queue.op.PlannedOperation;
import org.hibernate.engine.OptimisticLockStyle;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.jdbc.mutation.group.PreparedStatementDetails;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.SoftDeleteMapping;
import org.hibernate.persister.entity.EntityPersister;

import java.sql.SQLException;

/// Specialized BindPlan for soft delete operations.
///
/// Unlike hard deletes, soft deletes are actually `UPDATE` operations that set a
/// soft-delete indicator column.
///
/// @author Steve Ebersole
public class EntitySoftDeleteBindPlan implements BindPlan {
	private final EntityTableDescriptor tableDescriptor;
	private final EntityPersister entityPersister;
	private final Object identifier;
	private final Object version;
	private final Object[] loadedState;
	private final boolean applyOptimisticLocking;
	private final SoftDeleteMapping softDeleteMapping;

	public EntitySoftDeleteBindPlan(
			EntityTableDescriptor tableDescriptor,
			EntityPersister entityPersister,
			Object identifier,
			Object version,
			Object[] loadedState,
			boolean applyOptimisticLocking,
			SoftDeleteMapping softDeleteMapping) {
		this.tableDescriptor = tableDescriptor;
		this.entityPersister = entityPersister;
		this.identifier = identifier;
		this.version = version;
		this.loadedState = loadedState;
		this.applyOptimisticLocking = applyOptimisticLocking;
		this.softDeleteMapping = softDeleteMapping;
	}

	@Override
	public @Nullable Object getEntityId() {
		return identifier;
	}

	@Override
	public void bindValues(
			JdbcValueBindings valueBindings,
			PlannedOperation plannedOperation,
			SharedSessionContractImplementor session) {
		// NOTE: We do NOT bind the soft delete value or non-deleted restriction here.
		// These are literal values (e.g., true/false or CURRENT_TIMESTAMP) that are
		// already embedded in the SQL statement. They have no parameters to bind.

		// Bind the identifier for the WHERE clause
		breakDownKeyJdbcValue( valueBindings, session );

		// Apply optimistic locking if needed
		if ( applyOptimisticLocking ) {
			applyOptimisticLockingRestrictions( valueBindings, session );
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
							tableDescriptor.keyDescriptor().columns().get( index ).normalizedName(),
							ParameterUsage.RESTRICT
					);
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
		else if ( loadedState != null && optimisticLockStyle.isAllOrDirty() ) {
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
		if ( tableDescriptor.physicalName().equals( versionMapping.getContainingTableExpression() ) ) {
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
		final boolean[] versionability = entityPersister.getPropertyVersionability();
		for ( int i = 0; i < tableDescriptor.attributes().size(); i++ ) {
			var attribute = tableDescriptor.attributes().get( i );
			if ( !versionability[attribute.getStateArrayPosition()] ) {
				continue;
			}
			decomposeAttributeForRestriction(
					loadedState[attribute.getStateArrayPosition()],
					jdbcValueBindings,
					attribute,
					session
			);
		}
	}

	protected void decomposeAttributeForRestriction(
			Object value,
			JdbcValueBindings jdbcValueBindings,
			AttributeMapping mapping,
			SharedSessionContractImplementor session) {
		mapping.decompose(
				value,
				0,
				jdbcValueBindings,
				null,
				(valueIndex, bindings, noop, jdbcValue, selectableMapping) -> {
					if ( selectableMapping.isUpdateable() ) {
						bindings.bindValue(
								jdbcValue,
								selectableMapping.getSelectionExpression(),
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
												Helper.normalizeColumnName( selectable.getSelectionExpression() ),
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
