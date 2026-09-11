/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.exec.spi;

import java.util.Set;

import org.hibernate.Incubating;

/**
 * A primary operation to be executed using JDBC.
 *
 * @see org.hibernate.sql.exec.internal.JdbcSelectWithActions
 *
 * @author Steve Ebersole
 */
@Incubating
public interface PrimaryOperation extends JdbcOperation {
	/**
	 * The names of tables this operation refers to
	 */
	Set<String> getAffectedTableNames();
}
