/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.function.spi;

import java.util.List;

import org.hibernate.metamodel.model.domain.spi.AllowableFunctionReturnType;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.expression.function.FunctionSqmExpression;

/**
 * Extension for supplying support for non-standard (ANSI SQL) functions
 * in HQL and Criteria queries.
 * <p/>
 * Ultimately acts as a factory for SQM function expressions.
 *
 * @author David Channon
 * @author Steve Ebersole
 */
public interface SqmFunctionTemplate {
	FunctionSqmExpression makeSqmFunctionExpression(
			List<SqmExpression> arguments,
			AllowableFunctionReturnType impliedResultType);
}
