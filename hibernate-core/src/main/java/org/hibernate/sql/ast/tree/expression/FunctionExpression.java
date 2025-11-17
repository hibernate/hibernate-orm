/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.tree.expression;

import java.util.List;

import org.hibernate.sql.ast.tree.SqlAstNode;

/**
 * Models a function expression at the SQL AST level.
 *
 * @author Christian Beikov
 */
public interface FunctionExpression extends Expression {

	String getFunctionName();

	List<? extends SqlAstNode> getArguments();
}
