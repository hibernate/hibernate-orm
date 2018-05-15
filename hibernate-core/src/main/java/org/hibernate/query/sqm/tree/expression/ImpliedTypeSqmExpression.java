/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression;

import org.hibernate.sql.ast.produce.metamodel.spi.ExpressableType;

/**
 * Extension for Expressions whose Type can be implied from their surroundings.
 *
 * @author Steve Ebersole
 */
public interface ImpliedTypeSqmExpression extends SqmExpression {
	/**
	 * Used to inject the Type implied by the expression's context.
	 *
	 * @param type The implied type.
	 */
	void impliedType(ExpressableType type);
}
