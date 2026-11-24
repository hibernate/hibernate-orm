package org.hibernate.action.queue.internal.decompose.entity;

import org.hibernate.action.queue.spi.bind.OperationExecutionMonitor;
import org.hibernate.dialect.identity.spi.IdentityColumnSupport.IdentityInsertCommands;
import org.hibernate.engine.spi.SessionImplementor;

/// Scope the connection setting to one physical replacement insert, including generated-value retrieval.
final class ExplicitIdentityInsert implements OperationExecutionMonitor {
	private final IdentityInsertCommands commands;
	private boolean enabled;

	ExplicitIdentityInsert(IdentityInsertCommands commands) {
		this.commands = commands;
	}

	@Override
	public void beforeExecution(SessionImplementor session) {
		execute( session, commands.enable() );
		enabled = true;
	}

	@Override
	public void afterSuccessfulExecution(SessionImplementor session) {
		restore( session );
	}

	@Override
	public void afterFailedExecution(SessionImplementor session) {
		restore( session );
	}

	private void restore(SessionImplementor session) {
		if ( enabled ) {
			execute( session, commands.disable() );
			enabled = false;
		}
	}

	private static void execute(SessionImplementor session, String command) {
		session.doWork( connection -> {
			try (var statement = connection.createStatement()) {
				statement.execute( command );
			}
		} );
	}
}
