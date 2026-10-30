package org.hibernate.sql.exec.spi;

import java.sql.Connection;
import java.util.List;

import org.hibernate.Incubating;
import org.hibernate.sql.exec.internal.StandardJdbcActionExecution;

/// Invocation-local execution of a SELECT's secondary actions. A runner is
/// single-use, must not be called concurrently, and does not require thread
/// affinity. Retain it across asynchronous continuation until completion.
///
/// @see JdbcSelect#createActionExecution()
///
/// @since 8.0
/// @author Steve Ebersole
@Incubating(since = "8.0", group = "sql-execution")
public interface JdbcActionExecution {
	/// Immutable no-op runner for operations without actions.
	JdbcActionExecution NONE = new JdbcActionExecution() {};

	/// Materializes definitions without JDBC work. Identity pairs the same
	/// definition across phases; duplicates within a phase are rejected.
	static JdbcActionExecution fromActions(PreAction[] preActions, PostAction[] postActions) {
		return StandardJdbcActionExecution.create( preActions, postActions );
	}

	/// Materializes paired instances for integrations which execute callbacks
	/// asynchronously and own their phase sequencing and failure cleanup.
	/// No callback is invoked; the returned lists are immutable.
	static ActionInstances materializeActions(PreAction[] preActions, PostAction[] postActions) {
		return StandardJdbcActionExecution.materialize( preActions, postActions );
	}

	/// Invocation-local instances preserving order and identity pairing.
	record ActionInstances(List<PreAction> preActions, List<PostAction> postActions) {
		public ActionInstances {
			preActions = List.copyOf( preActions );
			postActions = List.copyOf( postActions );
		}
	}

	default void performPreActions(StatementAccess statementAccess, Connection connection, ExecutionContext context) {
	}

	default void performPostActions(
			boolean succeeded, StatementAccess statementAccess, Connection connection,
			ExecutionContext context, LoadedValuesCollector collector) {
	}

	/// Runs failure cleanup, attaching failures to the original exception instead
	/// of replacing it. The collector may be null when setup did not complete.
	/// The caller remains responsible for rethrowing the original exception.
	default void cleanupAfterFailure(
			Throwable failure, StatementAccess statementAccess, Connection connection,
			ExecutionContext context, LoadedValuesCollector collector) {
		try {
			performPostActions( false, statementAccess, connection, context, collector );
		}
		catch (RuntimeException | Error cleanupFailure) {
			if ( cleanupFailure != failure ) {
				failure.addSuppressed( cleanupFailure );
			}
		}
	}
}
