/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.decompose.entity;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.action.queue.bind.BindPlan;
import org.hibernate.action.queue.bind.Checkers;
import org.hibernate.action.queue.bind.JdbcValueBindings;
import org.hibernate.action.queue.bind.OperationResultChecker;
import org.hibernate.action.queue.meta.EntityTableDescriptor;
import org.hibernate.action.queue.plan.FlushOperation;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.entity.EntityPersister;

import java.sql.SQLException;

/**
 * @author Steve Ebersole
 */
public class ForceVersionBindPlan implements BindPlan, OperationResultChecker {
	private final EntityTableDescriptor tableDescriptor;
	private final EntityPersister persister;
	private final Object entity;
	private final Object entityId;
	private final Object oldVersion;
	private final Object newVersion;

	public ForceVersionBindPlan(
			EntityTableDescriptor tableDescriptor,
			EntityPersister persister,
			Object entity,
			Object entityId,
			Object oldVersion,
			Object newVersion) {
		this.tableDescriptor = tableDescriptor;
		this.persister = persister;
		this.entity = entity;
		this.entityId = entityId;
		this.oldVersion = oldVersion;
		this.newVersion = newVersion;
	}

	@Override
	public @Nullable Object getEntityId() {
		return entityId;
	}

	@Override
	public @Nullable Object getEntityInstance() {
		return entity;
	}

	@Override
	public void bindValues(
			JdbcValueBindings jdbcValueBindings,
			FlushOperation flushOperation,
			SharedSessionContractImplementor session) {
		jdbcValueBindings.bindAssignment( -1, newVersion, persister.getVersionMapping() );

		final var keyDescriptor = tableDescriptor.keyDescriptor();
		persister.getIdentifierMapping().breakDownJdbcValues(
				entityId,
				(index, jdbcValue, jdbcValueMapping) ->
						jdbcValueBindings.bindValue(
								jdbcValue,
								keyDescriptor.getSelectable( index ).getSelectionExpression(),
								ParameterUsage.RESTRICT
						),
				session
		);
		jdbcValueBindings.bindRestriction( -1, oldVersion, persister.getVersionMapping() );
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
				persister,
				tableDescriptor,
				entityId,
				sqlString,
				sessionFactory
		);
	}
}
