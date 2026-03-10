/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.cyclebreak;

import org.hibernate.action.queue.bind.BindPlan;
import org.hibernate.action.queue.cyclebreak.UniqueSwapUpdateFactory.UpdateTemplate;
import org.hibernate.action.queue.plan.PlannedOperation;
import org.hibernate.engine.jdbc.mutation.JdbcValueBindings;
import org.hibernate.engine.jdbc.mutation.MutationExecutor;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.entity.EntityPersister;

import java.util.Map;

import static org.hibernate.action.queue.Helper.normalizeColumnName;

/**
 * BindPlan specifically for unique constraint swap UPDATE operations.
 *
 * @author Steve Ebersole
 */
public class UniqueSwapUpdateBindPlan implements BindPlan {
	private final EntityPersister entityPersister;
	private final Object identifier;
	private final Map<String,Object> intendedValues;
	private final UpdateTemplate updateTemplate;

	public UniqueSwapUpdateBindPlan(
			EntityPersister entityPersister,
			Object identifier,
			Map<String, Object> intendedValues,
			UpdateTemplate updateTemplate) {
		this.entityPersister = entityPersister;
		this.identifier = identifier;
		this.intendedValues = intendedValues;
		this.updateTemplate = updateTemplate;
	}

	@Override
	public void bindAndMaybePatch(
			MutationExecutor executor,
			PlannedOperation operation,
			SharedSessionContractImplementor session) {
		final JdbcValueBindings jdbcValueBindings = executor.getJdbcValueBindings();

		// SET unique constraint columns
		for (var e : intendedValues.entrySet()) {
			final String columnName = normalizeColumnName(e.getKey());
			jdbcValueBindings.bindValue(e.getValue(), updateTemplate.tableName(), columnName, ParameterUsage.SET);
		}

		// WHERE key columns
		entityPersister.getIdentifierMapping().breakDownJdbcValues(
				identifier,
				(valueIndex, value, jdbcValueMapping) -> {
					jdbcValueBindings.bindValue(
							value,
							updateTemplate.tableName(),
							normalizeColumnName(jdbcValueMapping.getSelectableName()),
							ParameterUsage.RESTRICT
					);
				},
				session
		);
	}

	@Override
	public void execute(MutationExecutor executor, PlannedOperation operation, SharedSessionContractImplementor session) {
		// Execute the UPDATE directly
		executor.execute(
				operation.getBindPlan().getEntityInstance(),
				null,
				tableMapping -> true,
				(statementDetails, affectedRowCount, batchPosition) -> affectedRowCount == 1,
				session
		);
	}
}
