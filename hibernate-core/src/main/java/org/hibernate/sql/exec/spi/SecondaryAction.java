package org.hibernate.sql.exec.spi;

import org.hibernate.Incubating;

/**
 * Common marker interface for {@linkplain PreAction} and {@linkplain PostAction}.
 *
 * @implSpec Split to allow implementing both simultaneously.
 *
 * @author Steve Ebersole
 */
@Incubating(since = "6.0", group = "sql-execution")
public interface SecondaryAction {
	/// Creates the action used by one invocation. Returning `this` declares
	/// that this action retains no invocation-specific state and is reusable.
	/// Creation must not perform JDBC work or other execution side effects.
	/// Definitions registered in both phases are materialized once by identity.
	default SecondaryAction createExecutionAction() {
		return this;
	}
}
