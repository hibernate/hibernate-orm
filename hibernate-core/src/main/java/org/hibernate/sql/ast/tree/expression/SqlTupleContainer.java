/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.tree.expression;

import org.hibernate.sql.ast.tree.SqlAstNode;

/**
 * @author Steve Ebersole
 */
public interface SqlTupleContainer {
	SqlTuple getSqlTuple();

	static SqlTuple getSqlTuple(SqlAstNode expression) {
		return expression instanceof SqlTupleContainer sqlTupleContainer
				? sqlTupleContainer.getSqlTuple()
				: null;
	}
}
