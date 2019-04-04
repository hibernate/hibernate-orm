/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression;

import org.hibernate.query.sqm.tree.select.SqmSelectableNode;
import org.hibernate.sql.ast.produce.metamodel.spi.ExpressableType;

/**
 * The base contract for any kind of expression node in the SQM tree.
 * An expression might be a reference to an attribute, a literal,
 * a function, etc.
 *
 * @author Steve Ebersole
 */
public interface SqmExpression extends SqmSelectableNode {

	/**
	 * The expression's type.
	 *
	 * Can change as a result of calls to {@link #applyInferableType}
	 */
	ExpressableType<?> getExpressableType();

	/**
	 * Used to apply type information based on the expression's usage
	 * within the query.
	 */
	void applyInferableType(ExpressableType<?> type);
}
