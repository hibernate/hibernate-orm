/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.exec;

import org.hibernate.action.queue.plan.FlushOperation;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.sql.model.PreparableMutationOperation;

import java.sql.SQLException;

/**
 * @author Steve Ebersole
 */
public class StandardPlanStepExecutor extends AbstractStepExecutor {
	public StandardPlanStepExecutor(SharedSessionContractImplementor session) {
		super( session );
	}

	@Override
	public void executePreparable(PreparableMutationOperation preparable, FlushOperation flushOperation) {
		try (var stmnt = session.getJdbcCoordinator()
				.getStatementPreparer()
				.prepareStatement( preparable.getSqlString() ) ) {
			var valueBindings = new JdbcValueBindings( flushOperation.getMutatingTableDescriptor(), preparable );
			flushOperation.getBindPlan().bindValues( valueBindings, flushOperation, session );
			valueBindings.beforeStatement( stmnt, session );

			final int affectedRowCount = session.getJdbcCoordinator()
					.getResultSetReturn()
					.executeUpdate( stmnt, preparable.getSqlString() );

			final OperationResultChecker resultChecker = flushOperation.getBindPlan().getOperationResultChecker();
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
