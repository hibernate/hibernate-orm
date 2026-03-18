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

/**
 * @author Steve Ebersole
 */
public class NonBatchingPlanStepExecutor extends AbstractStepPlanner {
	public NonBatchingPlanStepExecutor(SharedSessionContractImplementor session) {
		super( session );
	}

	@Override
	public void executePreparable(PreparableJdbcOperation preparable, PlannedOperation plannedOperation) {
		try (var stmnt = session.getJdbcCoordinator()
				.getStatementPreparer()
				.prepareStatement( preparable.getSqlString() ) ) {
			preparable.getExpectation().prepare( stmnt );
			var valueBindings = new JdbcValueBindings( plannedOperation.getMutatingTableDescriptor(), preparable );
			plannedOperation.getBindPlan().bindValues( valueBindings, plannedOperation, session );
			valueBindings.beforeStatement( stmnt, session );

			final int affectedRowCount =
					session.getJdbcCoordinator()
							.getResultSetReturn()
							.executeUpdate( stmnt, preparable.getSqlString() );

			if ( plannedOperation.getBindPlan().getOperationResultChecker() != null ) {
				plannedOperation
						.getBindPlan()
						.getOperationResultChecker()
						.checkResult( affectedRowCount, -1, preparable.getSqlString(),  session.getFactory() );
			}
		}
		catch (SQLException sqle) {
			throw session.getJdbcServices()
					.getSqlExceptionHelper()
					.convert( sqle, "Unable to close PreparedStatement - " + preparable.getSqlString() );
		}
	}

}
