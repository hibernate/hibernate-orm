/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast;

import org.hibernate.sql.ast.tree.expression.JdbcParameter;

/**
 * Parameter information for {@link JdbcParameter} within a SQL query.
 *
 * @since 7.1
 */
public interface JdbcParameterMetadata {
	/**
	 * Returns the parameter id for the given {@link JdbcParameter}.
	 */
	int getParameterId(JdbcParameter jdbcParameter);

	/**
	 * Returns the number of parameters.
	 */
	int getParameterIdCount();
}
