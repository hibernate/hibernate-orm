/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.tree.expression;

import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.tree.SqlAstNode;

/**
 * @since 7.0
 */
public enum JsonExistsErrorBehavior implements SqlAstNode {
	TRUE,
	FALSE,
	ERROR;

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		throw new UnsupportedOperationException("JsonExistsErrorBehavior doesn't support walking");
	}

}
