/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.tree.expression;

import org.hibernate.sql.ast.tree.predicate.Predicate;

/**
 * Models a window function expression at the SQL AST level.
 *
 * @author Christian Beikov
 */
public interface WindowFunctionExpression extends FunctionExpression {

	Predicate getFilter();

	Boolean getRespectNulls();

	Boolean getFromFirst();
}
