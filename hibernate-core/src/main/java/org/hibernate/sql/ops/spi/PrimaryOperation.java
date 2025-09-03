/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ops.spi;

import org.hibernate.sql.exec.spi.JdbcOperation;

import java.util.Set;

/**
 * {@linkplain DatabaseOperation#getPrimaryOperation() Primary} JDBC operation for a
 * DatabaseOperation.
 *
 * @author Steve Ebersole
 */
public interface PrimaryOperation extends JdbcOperation {
	/**
	 * The names of tables this operation refers to
	 */
	Set<String> getAffectedTableNames();
}
