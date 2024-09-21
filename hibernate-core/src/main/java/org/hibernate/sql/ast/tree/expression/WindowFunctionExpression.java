/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
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
