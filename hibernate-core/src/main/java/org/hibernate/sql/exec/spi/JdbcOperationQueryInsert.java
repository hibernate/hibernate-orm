package org.hibernate.sql.exec.spi;

import org.hibernate.Incubating;

/**
 * Basic contract for an insert operation
 *
 * @author Steve Ebersole
 */
@Incubating(since = "6.0", group = "sql-execution")
public interface JdbcOperationQueryInsert extends JdbcOperationQueryMutation {
	String getUniqueConstraintNameThatMayFail();

	/**
	 * Whether the insert may be executed once more after it failed with a unique constraint violation.
	 * <p>
	 * This is the case for a {@code merge} statement which emulates an insert conflict clause:
	 * a concurrent transaction may insert a conflicting row after the {@code merge} found no
	 * matching row, and a second execution then sees that row.
	 *
	 * @since 8.1
	 */
	default boolean isRetryableOnUniqueConstraintViolation() {
		return false;
	}
}
