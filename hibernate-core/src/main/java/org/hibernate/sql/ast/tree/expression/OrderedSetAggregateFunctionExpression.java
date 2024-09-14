/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql.ast.tree.expression;

import java.util.List;

import org.hibernate.sql.ast.tree.select.SortSpecification;

/**
 * Models an ordered set-aggregate function expression at the SQL AST level.
 *
 * @author Christian Beikov
 */
public interface OrderedSetAggregateFunctionExpression extends AggregateFunctionExpression {

	List<SortSpecification> getWithinGroup();
}
