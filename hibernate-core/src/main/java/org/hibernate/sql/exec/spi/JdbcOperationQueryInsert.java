/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
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
}
