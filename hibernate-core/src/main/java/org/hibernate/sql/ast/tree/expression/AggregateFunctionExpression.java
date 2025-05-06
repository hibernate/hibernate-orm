/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.tree.expression;

import org.hibernate.sql.ast.tree.predicate.Predicate;

/**
 * Models an aggregate function expression at the SQL AST level.
 *
 * @author Christian Beikov
 */
public interface AggregateFunctionExpression extends FunctionExpression {

	Predicate getFilter();
}
