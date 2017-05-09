/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression;

import org.hibernate.persister.queryable.spi.BasicValuedExpressableType;

/**
 * @author Steve Ebersole
 */
public interface ConstantSqmExpression<T> extends ImpliedTypeSqmExpression {
	T getValue();

	@Override
	BasicValuedExpressableType getExpressionType();

	@Override
	BasicValuedExpressableType getInferableType();
}
