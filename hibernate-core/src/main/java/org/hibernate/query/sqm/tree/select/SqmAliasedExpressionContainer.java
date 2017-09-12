/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.select;

import org.hibernate.query.sqm.tree.expression.SqmExpression;

/**
 * @author Steve Ebersole
 */
public interface SqmAliasedExpressionContainer<T extends SqmAliasedNode> {
	T add(SqmExpression expression, String alias);
	void add(T aliasExpression);
}
