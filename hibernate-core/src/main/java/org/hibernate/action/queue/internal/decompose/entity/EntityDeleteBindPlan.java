/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.internal.decompose.entity;


import org.hibernate.action.queue.spi.bind.BindPlan;
import org.hibernate.action.queue.spi.bind.Checkers;
import org.hibernate.action.queue.spi.bind.JdbcValueBindings;
import org.hibernate.action.queue.spi.bind.OperationResultChecker;
import org.hibernate.action.queue.spi.meta.EntityTableDescriptor;
import org.hibernate.action.queue.spi.meta.TableKeyDescriptor;
import org.hibernate.action.queue.spi.plan.FlushOperation;
import org.hibernate.engine.OptimisticLockStyle;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.ModelPart.JdbcValueBiConsumer;
import org.hibernate.persister.entity.EntityPersister;

import java.sql.SQLException;

/// @author Steve Ebersole
public class EntityDeleteBindPlan implements BindPlan, OperationResultChecker {

	/// Static, non-capturing [JdbcValueBiConsumer] for handling key values for delete restriction (WHERE).
	private static final JdbcValueBiConsumer<EntityDeleteBindPlan, TableKeyDescriptor> KEY_RESTRICTION_BINDER =
			(index, bindPlan, keyDescriptor, jdbcValue, jdbcValueMapping) -> {
				final var keyColumn = keyDescriptor.getSelectable( index );
				bindPlan.valueBindings.bindRestriction( index, jdbcValue, keyColumn );
			};


	/// Static, non-capturing [JdbcValueBiConsumer] for handling version values for delete restriction (WHERE).
	private static final JdbcValueBiConsumer<EntityDeleteBindPlan, Object> VERSION_RESTRICTION_BINDER =
			(valueIndex, bindPlan, noop, jdbcValue, selectableMapping) -> bindPlan.valueBindings.bindValue(
					jdbcValue,
					selectableMapping.getSelectionExpression(),
					ParameterUsage.RESTRICT
			);


	/// Static, non-capturing [JdbcValueBiConsumer] for handling attribute values for delete restriction (WHERE) - optimistic locking.
	private static final JdbcValueBiConsumer<EntityDeleteBindPlan, Object> ATTRIBUTE_RESTRICTION_BINDER =
			(valueIndex, bindPlan, noop, jdbcValue, selectableMapping) -> {
				if ( !selectableMapping.isFormula() && jdbcValue != null ) {
					bindPlan.valueBindings.bindValue(
							jdbcValue,
							selectableMapping.getSelectionExpression(),
							ParameterUsage.RESTRICT
					);
				}
			};

	private final EntityTableDescriptor tableDescriptor;
	private final EntityPersister entityPersister;
	private final Object identifier;
	private final Object rowId;
	private final Object version;
	private final Object[] state;
	private final Object[] loadedState;
	private final int[] sameFlushUpdatedAttributeIndexes;
	private final OptimisticLockStyle effectiveOptLockStyle;

	// temporary state used during decomposition to allow model-part decomposition to be non-capturing
	private JdbcValueBindings valueBindings;


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
	public void bindValues(
			JdbcValueBindings valueBindings,
			FlushOperation flushOperation,
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
				this.valueBindings = jdbcValueBindings;
				try {
					versionMapping.decompose(
							version,
							0,
							this,
							null,
							VERSION_RESTRICTION_BINDER,
							session
					);
				}
				finally {
					this.valueBindings = null;
				}
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
				decomposeAttributeForRestriction(
						loadedState[attribute.getStateArrayPosition()],
						jdbcValueBindings,
						attribute,
						session
				);
			}
	}

	private void decomposeAttributeForRestriction(
			Object value,
			JdbcValueBindings jdbcValueBindings,
			AttributeMapping attribute,
			SharedSessionContractImplementor session) {
		this.valueBindings = jdbcValueBindings;
		try {
			attribute.decompose(
					value,
					0,
					this,
					null,
					ATTRIBUTE_RESTRICTION_BINDER,
					session
			);
		}
		finally {
			this.valueBindings = null;
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
