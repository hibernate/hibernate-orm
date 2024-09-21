/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
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
		if ( expression instanceof SqlTupleContainer ) {
			return ( (SqlTupleContainer) expression ).getSqlTuple();
		}
		return null;
	}
}
