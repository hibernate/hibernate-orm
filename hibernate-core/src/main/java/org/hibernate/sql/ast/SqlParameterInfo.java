/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast;

import org.hibernate.sql.ast.tree.expression.JdbcParameter;

/**
 * @since 7.0
 */
public interface SqlParameterInfo {
	int getParameterId(JdbcParameter jdbcParameter);
}
