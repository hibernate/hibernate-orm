/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.exec;

import org.hibernate.action.queue.bind.JdbcValueBindings;
import org.hibernate.action.queue.mutation.jdbc.PreparableJdbcOperation;
import org.hibernate.action.queue.op.PlannedOperation;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

import java.sql.SQLException;
import java.util.function.Consumer;

/**
 * @author Steve Ebersole
 */
public class StandardPlanStepExecutor extends AbstractStepExecutor implements ExecutionContext {
	public StandardPlanStepExecutor(SharedSessionContractImplementor session) {
		super( session );
	}

	@Override
	public void executePreparable(PreparableJdbcOperation preparable, PlannedOperation plannedOperation) {
		// Delegate to BindPlan to drive execution
		// The BindPlan will call back to executeRow() for each row it needs to execute
		plannedOperation.getBindPlan().execute( this, plannedOperation, session );
	}

	@Override
	public void executeRow(
			PlannedOperation plannedOperation,
			Consumer<JdbcValueBindings> binder,
			OperationResultChecker resultChecker) {
		final PreparableJdbcOperation preparable = (PreparableJdbcOperation) plannedOperation.getJdbcOperation();

		try (var stmnt = session.getJdbcCoordinator()
				.getStatementPreparer()
				.prepareStatement( preparable.getSqlString() ) ) {
			var valueBindings = new JdbcValueBindings( plannedOperation.getMutatingTableDescriptor(), preparable );
			binder.accept( valueBindings );
			valueBindings.beforeStatement( stmnt, session );

			final int affectedRowCount =
					session.getJdbcCoordinator()
							.getResultSetReturn()
							.executeUpdate( stmnt, preparable.getSqlString() );

			if ( resultChecker != null ) {
				resultChecker.checkResult( affectedRowCount, -1, preparable.getSqlString(),  session.getFactory() );
			}
		}
		catch (SQLException sqle) {
			throw session.getJdbcServices()
					.getSqlExceptionHelper()
					.convert( sqle, "Unable to close PreparedStatement - " + preparable.getSqlString() );
		}
	}

}
