/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.internal.decompose.entity;


import java.sql.SQLException;

import org.hibernate.action.queue.spi.bind.BindPlan;
import org.hibernate.action.queue.spi.bind.Checkers;
import org.hibernate.action.queue.spi.bind.JdbcValueBindings;
import org.hibernate.action.queue.spi.bind.OperationResultChecker;
import org.hibernate.action.queue.spi.meta.EntityTableDescriptor;
import org.hibernate.action.queue.spi.plan.FlushOperation;
import org.hibernate.engine.OptimisticLockStyle;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.TemporalMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.mutation.TemporalMutationHelper;

/// Bind plan for temporal operations which end the current row version.
///
/// Used by both temporal deletes and temporal updates which close the current
/// row before inserting a replacement row version.
///
/// @author Steve Ebersole
public class EntityTemporalEndBindPlan implements BindPlan, OperationResultChecker {
	private final EntityTableDescriptor tableDescriptor;
	private final EntityPersister entityPersister;
	private final TemporalMapping temporalMapping;
	private final Object identifier;
	private final Object rowId;
	private final Object version;
	private final Object[] loadedState;
	private final OptimisticLockStyle effectiveOptLockStyle;

	public EntityTemporalEndBindPlan(
			EntityTableDescriptor tableDescriptor,
			EntityPersister entityPersister,
			TemporalMapping temporalMapping,
			Object identifier,
			Object rowId,
			Object version,
			Object[] loadedState,
			OptimisticLockStyle effectiveOptLockStyle) {
		this.tableDescriptor = tableDescriptor;
		this.entityPersister = entityPersister;
		this.temporalMapping = temporalMapping;
		this.identifier = identifier;
		this.rowId = rowId;
		this.version = version;
		this.loadedState = loadedState;
		this.effectiveOptLockStyle = effectiveOptLockStyle;
	}

	@Override
	public Object getEntityId() {
		return identifier;
	}

	@Override
	public Object[] getLoadedState() {
		return loadedState;
	}

	@Override
	public void bindValues(
			JdbcValueBindings valueBindings,
			FlushOperation flushOperation,
			SharedSessionContractImplementor session) {
		bindTemporalEndingValue( valueBindings, session );
		bindKey( valueBindings, session );

		if ( effectiveOptLockStyle == OptimisticLockStyle.VERSION ) {
			bindVersionRestriction( valueBindings, session );
		}
		else if ( effectiveOptLockStyle.isAllOrDirty() ) {
			bindNonVersionOptimisticLockRestrictions( valueBindings, session );
		}

		if ( entityPersister.hasPartitionedSelectionMapping() && loadedState != null ) {
			bindPartitionedSelectionRestrictions( valueBindings, session );
		}
	}

	private void bindTemporalEndingValue(
			JdbcValueBindings valueBindings,
			SharedSessionContractImplementor session) {
		if ( TemporalMutationHelper.isUsingParameters( session ) ) {
			valueBindings.bindValue(
					session.getCurrentChangesetIdentifier(),
					temporalMapping.getEndingColumnMapping().getSelectionExpression(),
					ParameterUsage.SET
			);
		}
	}

	private void bindKey(
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

		final var keyDescriptor = tableDescriptor.keyDescriptor();
		entityPersister.getIdentifierMapping().breakDownJdbcValues(
				identifier,
				(index, jdbcValue, jdbcValueMapping) ->
						valueBindings.bindRestriction( index, jdbcValue, keyDescriptor.getSelectable( index ) ),
				session
		);
	}

	private void bindVersionRestriction(
			JdbcValueBindings valueBindings,
			SharedSessionContractImplementor session) {
		final var versionMapping = entityPersister.getVersionMapping();
		if ( versionMapping == null ) {
			return;
		}
		if ( tableDescriptor.name().equals( versionMapping.getContainingTableExpression() ) ) {
			versionMapping.decompose(
					version,
					0,
					valueBindings,
					null,
					(valueIndex, bindings, noop, jdbcValue, selectableMapping) ->
							bindings.bindValue(
									jdbcValue,
									selectableMapping.getSelectionExpression(),
									ParameterUsage.RESTRICT
							),
					session
			);
		}
	}

	private void bindPartitionedSelectionRestrictions(
			JdbcValueBindings valueBindings,
			SharedSessionContractImplementor session) {
		for ( int i = 0; i < tableDescriptor.attributes().size(); i++ ) {
			final var attribute = tableDescriptor.attributes().get( i );
			attribute.forEachSelectable( (selectionIndex, selectableMapping) -> {
				if ( selectableMapping.isPartitioned() ) {
					final Object value = loadedState[attribute.getStateArrayPosition()];
					if ( value != null ) {
						attribute.decompose(
								value,
								0,
								valueBindings,
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

	private void bindNonVersionOptimisticLockRestrictions(
			JdbcValueBindings valueBindings,
			SharedSessionContractImplementor session) {
		if ( loadedState == null ) {
			return;
		}

		final boolean[] versionability = entityPersister.getPropertyVersionability();
		for ( int i = 0; i < tableDescriptor.attributes().size(); i++ ) {
			final var attribute = tableDescriptor.attributes().get( i );
			if ( attribute.isPluralAttributeMapping() || !versionability[attribute.getStateArrayPosition()] ) {
				continue;
			}
			attribute.decompose(
					loadedState[attribute.getStateArrayPosition()],
					(valueIndex, jdbcValue, selectableMapping) -> {
						if ( !selectableMapping.isFormula() && jdbcValue != null ) {
							valueBindings.bindValue(
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
