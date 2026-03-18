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
		session.getJdbcServices().getSqlStatementLogger().logStatement( preparable.getSqlString() );
		try (var stmnt = session.getJdbcCoordinator()
				.getStatementPreparer()
				.prepareStatement( preparable.getSqlString() ) ) {
			var valueBindings = new JdbcValueBindings( plannedOperation.getMutatingTableDescriptor(), preparable );
			plannedOperation.getBindPlan().bindValues( valueBindings, plannedOperation, session );
			valueBindings.beforeStatement( stmnt, session );

			session.getJdbcServices().getSqlStatementLogger().logStatement( preparable.getSqlString() );

			final int affectedRowCount =
					session.getJdbcCoordinator()
							.getResultSetReturn()
							.executeUpdate( stmnt, preparable.getSqlString() );

			if ( affectedRowCount == 0 && plannedOperation.getMutatingTableDescriptor().isOptional() ) {
				// the optional table did not have a row
				return;
			}

//			checkResults( resultChecker, statementDetails, affectedRowCount, -1 );
		}
		catch (SQLException sqle) {
			throw session.getJdbcServices()
					.getSqlExceptionHelper()
					.convert( sqle, "Unable to close PreparedStatement - " + preparable.getSqlString() );
		}
	}
}
