/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.exec.spi;

import java.util.Set;

/**
 * One-or-more {@link JdbcOperation operations} performed against the database using JDBC.
 *
 * @apiNote By design, we expect one of the underlying {@link JdbcOperation operations} to be a
 * {@linkplain #getPrimaryOperation "primary operation"} along with zero-or-more support operations
 * to be performed before and/or after the primary operation.
 *
 * @author Steve Ebersole
 */
public interface DatabaseOperation {
	/**
	 * The primary operation for the group.
	 */
	JdbcOperation getPrimaryOperation();

	/**
	 * The names of tables referenced or affected by this operation.
	 */
	Set<String> getAffectedTableNames();
}
