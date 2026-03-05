/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.cyclebreak;

import org.hibernate.action.queue.bind.BindPlan;
import org.hibernate.action.queue.cyclebreak.FkFixupUpdateFactory.UpdateTemplate;
import org.hibernate.action.queue.plan.PlannedOperation;
import org.hibernate.engine.jdbc.mutation.JdbcValueBindings;
import org.hibernate.engine.jdbc.mutation.MutationExecutor;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.entity.EntityPersister;

import java.util.Map;

/**
 * BindPlan specifically for "FK fixup" update operation.
 *
 * @author Steve Ebersole
 */
public class FkFixupUpdateBindPlan implements BindPlan {
	private final EntityPersister entityPersister;
	private final Object identifier;
	private final Map<String,Object> intendedFkValues;
	private final UpdateTemplate updateTemplate;

	public FkFixupUpdateBindPlan(
			EntityPersister entityPersister,
			Object identifier,
			Map<String, Object> intendedFkValues,
			UpdateTemplate updateTemplate) {
		this.entityPersister = entityPersister;
		this.identifier = identifier;
		this.intendedFkValues = intendedFkValues;
		this.updateTemplate = updateTemplate;
	}

	@Override
	public void bindAndMaybePatch(
			MutationExecutor executor,
			PlannedOperation operation,
			SharedSessionContractImplementor session) {
		final JdbcValueBindings jdbcValueBindings = executor.getJdbcValueBindings();

		// SET fk columns
		for (var e : intendedFkValues.entrySet()) {
			jdbcValueBindings.bindValue(e.getValue(), operation.getTableExpression(), e.getKey(), ParameterUsage.SET);
		}

		// WHERE key columns
		entityPersister.getIdentifierMapping().breakDownJdbcValues(
				identifier,
				(valueIndex, value, jdbcValueMapping) -> {
					jdbcValueBindings.bindValue(
							value,
							operation.getTableExpression(),
							jdbcValueMapping.getSelectableName(),
							ParameterUsage.SET
					);
				},
				session
		);
	}

	@Override
	public void execute(MutationExecutor executor, PlannedOperation operation, SharedSessionContractImplementor session) {
		// Comes down to whether we want to allow this to be added back to the flush plan
		// as another "cycle" for possible batching.
		//
		// For now, let's just execute it directly...
		executor.execute(
				operation.getBindPlan().getEntityInstance(),
				null,
				tableMapping -> true,
				(statementDetails, affectedRowCount, batchPosition) -> affectedRowCount == 1,
				session
		);
	}
}
