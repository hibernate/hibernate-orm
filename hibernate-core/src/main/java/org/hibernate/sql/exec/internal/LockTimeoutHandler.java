package org.hibernate.sql.exec.internal;

import jakarta.persistence.Timeout;
import org.hibernate.dialect.lock.spi.ConnectionLockTimeoutStrategy;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.LoadedValuesCollector;
import org.hibernate.sql.exec.spi.PostAction;
import org.hibernate.sql.exec.spi.PreAction;
import org.hibernate.sql.exec.spi.StatementAccess;
import org.hibernate.sql.exec.spi.SecondaryAction;

import java.sql.Connection;

/**
 * Handles lock timeouts using setting on the JDBC Connection.
 *
 * @see ConnectionLockTimeoutStrategy
 *
 * @author Steve Ebersole
 */
public class LockTimeoutHandler implements PreAction, PostAction {
	private final ConnectionLockTimeoutStrategy lockTimeoutStrategy;
	private final Timeout timeout;

	public LockTimeoutHandler(Timeout timeout, ConnectionLockTimeoutStrategy lockTimeoutStrategy) {
		this.timeout = timeout;
		this.lockTimeoutStrategy = lockTimeoutStrategy;
	}

	@Override
	public SecondaryAction createExecutionAction() {
		return new Execution();
	}

	@Override
	public boolean shouldRunAfterFail() {
		throw new IllegalStateException( "Lock timeout definitions require an execution action; use createActionExecution()" );
	}

	@Override
	public void performPreAction(StatementAccess access, Connection connection, ExecutionContext context) {
		throw new IllegalStateException( "Lock timeout definitions require an execution action; use createActionExecution()" );
	}

	@Override
	public void performPostAction(StatementAccess access, Connection connection, ExecutionContext context, LoadedValuesCollector collector) {
		throw new IllegalStateException( "Lock timeout definitions require an execution action; use createActionExecution()" );
	}

	private class Execution implements PreAction, PostAction {
		private Timeout baseline;
		private boolean restorationRequired;

		@Override
		public void performPreAction(StatementAccess access, Connection connection, ExecutionContext context) {
			final var factory = context.getSession().getFactory();
			baseline = lockTimeoutStrategy.getLockTimeout( connection, factory );
			// A failing setter may already have changed the connection setting.
			restorationRequired = true;
			lockTimeoutStrategy.setLockTimeout( timeout, connection, factory );
		}

		@Override
		public void performPostAction(StatementAccess access, Connection connection, ExecutionContext context, LoadedValuesCollector collector) {
			if ( restorationRequired ) {
				restorationRequired = false;
				lockTimeoutStrategy.setLockTimeout( baseline, connection, context.getSession().getFactory() );
			}
		}

		@Override
		public boolean shouldRunAfterFail() {
			return restorationRequired;
		}
	}
}
