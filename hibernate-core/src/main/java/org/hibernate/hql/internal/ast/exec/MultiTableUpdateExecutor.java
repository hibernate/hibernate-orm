/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.internal.ast.exec;

import org.hibernate.HibernateException;
import org.hibernate.action.internal.BulkOperationCleanupAction;
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.event.spi.EventSource;
import org.hibernate.hql.internal.ast.HqlSqlWalker;
import org.hibernate.hql.spi.id.MultiTableBulkIdStrategy;

/**
 * Implementation of MultiTableUpdateExecutor.
 *
 * @author Steve Ebersole
 */
public class MultiTableUpdateExecutor implements StatementExecutor {
	private final MultiTableBulkIdStrategy.UpdateHandler updateHandler;

	public MultiTableUpdateExecutor(HqlSqlWalker walker) {
		MultiTableBulkIdStrategy strategy = walker.getSessionFactoryHelper()
				.getFactory()
				.getSettings()
				.getMultiTableBulkIdStrategy();
		this.updateHandler = strategy.buildUpdateHandler( walker.getSessionFactoryHelper().getFactory(), walker );
	}

	public String[] getSqlStatements() {
		return updateHandler.getSqlStatements();
	}

	public int execute(QueryParameters parameters, SessionImplementor session) throws HibernateException {
		BulkOperationCleanupAction action = new BulkOperationCleanupAction( session, updateHandler.getTargetedQueryable() );

		if ( session.isEventSource() ) {
			( (EventSource) session ).getActionQueue().addAction( action );
		}
		else {
			action.getAfterTransactionCompletionProcess().doAfterTransactionCompletion( true, session );
		}

		return updateHandler.execute( session, parameters );
	}
}
