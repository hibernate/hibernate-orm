/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
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
