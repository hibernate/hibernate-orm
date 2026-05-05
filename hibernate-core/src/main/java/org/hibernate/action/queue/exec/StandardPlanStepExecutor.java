/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.exec;

import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.hibernate.action.queue.plan.PlannedOperation;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.sql.model.PreparableMutationOperation;

import java.sql.SQLException;
import java.util.function.BiConsumer;

/**
 * @author Steve Ebersole
 */
public class StandardPlanStepExecutor extends AbstractStepExecutor implements ExecutionContext {
	public StandardPlanStepExecutor(SharedSessionContractImplementor session) {
		super( session );
	}

	@Override
	public void executePreparable(PreparableMutationOperation preparable, PlannedOperation plannedOperation) {
		// Delegate to BindPlan to drive execution
		// The BindPlan will call back to executeRow() for each row it needs to execute
		plannedOperation.getBindPlan().execute( this, plannedOperation, session );
	}

	@Override
	public void executeRow(
			PlannedOperation plannedOperation,
			@MonotonicNonNull BiConsumer<JdbcValueBindings, SharedSessionContractImplementor> binder,
			OperationResultChecker resultChecker) {
		final PreparableMutationOperation preparable = (PreparableMutationOperation) plannedOperation.getJdbcOperation();

		try (var stmnt = session.getJdbcCoordinator()
				.getStatementPreparer()
				.prepareStatement( preparable.getSqlString() ) ) {
			var valueBindings = new JdbcValueBindings( plannedOperation.getMutatingTableDescriptor(), preparable );
			binder.accept( valueBindings, session );
			valueBindings.beforeStatement( stmnt, session );

			final int affectedRowCount = session.getJdbcCoordinator()
					.getResultSetReturn()
					.executeUpdate( stmnt, preparable.getSqlString() );

			if ( resultChecker != null ) {
				resultChecker.checkResult( affectedRowCount, -1, preparable.getSqlString(),  session.getFactory() );
			}

			// Explicitly release the statement from the resource registry to prevent accumulation
			session.getJdbcCoordinator().getLogicalConnection().getResourceRegistry().release( stmnt );
		}
		catch (SQLException sqle) {
			throw session.getJdbcServices()
					.getSqlExceptionHelper()
					.convert( sqle, "Unable to close PreparedStatement - " + preparable.getSqlString() );
		}
	}

}
