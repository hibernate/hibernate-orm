package org.hibernate.query.sqm.tree.spi.select;

import org.hibernate.query.sqm.tree.spi.expression.SqmExpression;

/**
 * @author Steve Ebersole
 */
public interface SqmAliasedExpressionContainer<T extends SqmAliasedNode<?>> {
	T add(SqmExpression<?> expression, String alias);
	void add(T aliasExpression);
}
