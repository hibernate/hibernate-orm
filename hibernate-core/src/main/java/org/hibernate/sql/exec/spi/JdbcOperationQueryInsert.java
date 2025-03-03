/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.exec.spi;

/**
 * Basic contract for an insert operation
 *
 * @author Steve Ebersole
 */
public interface JdbcOperationQueryInsert extends JdbcOperationQueryMutation {
	String getUniqueConstraintNameThatMayFail();
}
