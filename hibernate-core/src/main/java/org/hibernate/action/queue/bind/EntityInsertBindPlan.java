/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.bind;

import org.hibernate.action.internal.AbstractEntityInsertAction;
import org.hibernate.action.queue.cyclebreak.CycleBreakPatcher;
import org.hibernate.action.queue.exec.OperationResultChecker;
import org.hibernate.action.queue.meta.EntityTableDescriptor;
import org.hibernate.action.queue.op.PlannedOperation;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.mutation.ColumnDetails;

import java.sql.SQLException;
import java.util.Map;

/**
 * @author Steve Ebersole
 */
public class EntityInsertBindPlan implements BindPlan, OperationResultChecker {
	private final EntityTableDescriptor tableDescriptor;
	private final EntityPersister entityPersister;
	private final Object entity;
	private final Object identifier;
	private final Map<ColumnDetails, Object> columnValues;
	private final boolean[] insertable;
	private final AbstractEntityInsertAction action;
	private final GeneratedValuesCollector generatedValuesCollector;

	public EntityInsertBindPlan(
			EntityTableDescriptor tableDescriptor,
			EntityPersister entityPersister,
			Object entity,
			Object identifier,
			Map<ColumnDetails, Object> columnValues,
			boolean[] insertable,
			AbstractEntityInsertAction action,
			GeneratedValuesCollector generatedValuesCollector) {
		this.tableDescriptor = tableDescriptor;
		this.entityPersister = entityPersister;
		this.entity = entity;
		this.identifier = identifier;
		this.columnValues = columnValues;
		this.insertable = insertable;
		this.action = action;
		this.generatedValuesCollector = generatedValuesCollector;
	}

	@Override
	public Object getEntityId() {
		return action.getId();
	}

	@Override
	public Object getEntityInstance() {
		return entity;
	}

	@Override
	public GeneratedValuesCollector getGeneratedValuesCollector() {
		return generatedValuesCollector;
	}

	@Override
	public void bindValues(
			JdbcValueBindings valueBindings,
			PlannedOperation plannedOperation,
			SharedSessionContractImplementor session) {
		decomposeForInsert( valueBindings, identifier, session );

		if ( plannedOperation.getBindingPatch() != null ) {
			CycleBreakPatcher.applyFixupPatch( valueBindings, plannedOperation, plannedOperation.getBindingPatch() );
		}
	}

	private void decomposeForInsert(JdbcValueBindings valueBindings, Object identifier, SharedSessionContractImplementor session) {
		columnValues.forEach( (columnMapping, columnValue) -> {
			if ( insertable[columnMapping.attributeIndex()]) {
				if ( columnMapping.physicalColumn() && columnMapping.insertable() ) {
					valueBindings.bindValue(
							columnValue,
							columnMapping.columnName(),
							ParameterUsage.SET
					);
				}
			}
		} );

		if ( identifier == null ) {
			assert entityPersister.getInsertDelegate() != null;
		}
		else {
			breakDownKeyJdbcValue( valueBindings, session );
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
							columnMapping.getSelectableName(),
							ParameterUsage.SET
					);
				},
				session
		);
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
				tableDescriptor.insertDetails().getExpectation(),
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
