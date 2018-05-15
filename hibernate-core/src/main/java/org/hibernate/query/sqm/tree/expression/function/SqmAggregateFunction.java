/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression.function;

import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.sql.ast.produce.metamodel.spi.BasicValuedExpressableType;

/**
 * Marker interface to more readily identify "aggregate functions".
 *
 * @author Steve Ebersole
 */
public interface SqmAggregateFunction extends SqmFunction, Distinctable {
	@Override
	BasicValuedExpressableType getExpressableType();

	SqmExpression getArgument();
	boolean isDistinct();
}
