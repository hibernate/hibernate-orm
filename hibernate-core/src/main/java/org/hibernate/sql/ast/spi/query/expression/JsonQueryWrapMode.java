/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.spi.query.expression;

import org.hibernate.sql.ast.spi.SqlAstWalker;
import org.hibernate.sql.ast.spi.SqlAstNode;

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
