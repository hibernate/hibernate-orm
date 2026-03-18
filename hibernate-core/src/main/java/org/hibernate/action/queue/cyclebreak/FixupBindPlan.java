/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.cyclebreak;

import org.hibernate.action.queue.bind.BindPlan;
import org.hibernate.action.queue.bind.JdbcValueBindings;
import org.hibernate.action.queue.op.PlannedOperation;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.entity.EntityPersister;

import java.util.Map;

/**
 * @author Steve Ebersole
 */
public class FixupBindPlan implements BindPlan {
	private final EntityPersister entityPersister;
	private final Object identifier;
	private final Map<String,Object> intendedValues;

	public FixupBindPlan(EntityPersister entityPersister, Object identifier, Map<String, Object> intendedValues) {
		this.entityPersister = entityPersister;
		this.identifier = identifier;
		this.intendedValues = intendedValues;
	}

	@Override
	public void bindValues(
			JdbcValueBindings valueBindings,
			PlannedOperation plannedOperation,
			SharedSessionContractImplementor session) {

		// SET fk columns
		for (var e : intendedValues.entrySet()) {
			valueBindings.bindValue(
					e.getValue(),
					e.getKey(),
					ParameterUsage.SET
			);
		}

		// WHERE key columns
		entityPersister.getIdentifierMapping().breakDownJdbcValues(
				identifier,
				(valueIndex, value, jdbcValueMapping) -> {
					valueBindings.bindValue(
							value,
							jdbcValueMapping.getSelectableName(),
							ParameterUsage.RESTRICT
					);
				},
				session
		);
	}
}
