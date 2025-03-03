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
public enum JsonQueryWrapMode implements SqlAstNode {
	WITH_WRAPPER,
	WITHOUT_WRAPPER,
	WITH_CONDITIONAL_WRAPPER;

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		throw new UnsupportedOperationException("JsonQueryWrapMode doesn't support walking");
	}

}
