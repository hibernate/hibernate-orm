/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.entity.mutation;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.action.queue.bind.BindPlan;
import org.hibernate.action.queue.bind.Checkers;
import org.hibernate.action.queue.bind.JdbcValueBindings;
import org.hibernate.action.queue.exec.ExecutionContext;
import org.hibernate.action.queue.exec.OperationResultChecker;
import org.hibernate.action.queue.plan.PlannedOperation;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.entity.EntityPersister;

import java.sql.SQLException;

/**
 * @author Steve Ebersole
 */
public class ForceVersionBindPlan implements BindPlan, OperationResultChecker {
	private final EntityPersister persister;
	private final Object entity;
	private final Object entityId;
	private final Object oldVersion;
	private final Object newVersion;

	public ForceVersionBindPlan(
			EntityPersister persister,
			Object entity,
			Object entityId,
			Object oldVersion,
			Object newVersion) {
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
	public void execute(ExecutionContext context, PlannedOperation plannedOperation, SharedSessionContractImplementor session) {
		context.executeRow( plannedOperation, this::bindValues, this );
	}

	private void bindValues(JdbcValueBindings jdbcValueBindings, SharedSessionContractImplementor session) {
		jdbcValueBindings.bindAssignment( -1, newVersion, persister.getVersionMapping() );

		persister.getIdentifierMapping().breakDownJdbcValues(
				entityId,
				jdbcValueBindings::bindRestriction,
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
				persister.getIdentifierTableDescriptor().updateDetails().getExpectation(),
				affectedRowCount,
				batchPosition,
				persister,
				persister.getIdentifierTableDescriptor(),
				entityId,
				sqlString,
				sessionFactory
		);
	}
}
