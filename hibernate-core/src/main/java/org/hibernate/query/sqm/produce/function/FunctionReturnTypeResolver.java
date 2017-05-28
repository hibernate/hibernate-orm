/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.function;

import java.util.List;

import org.hibernate.metamodel.model.domain.spi.AllowableFunctionReturnType;
import org.hibernate.query.sqm.tree.expression.SqmExpression;

/**
 * Pluggable strategy for resolving a function return type for a specific call.
 *
 * @author Steve Ebersole
 */
public interface FunctionReturnTypeResolver {
	/**
	 * Resolve the return type for a function given its context-implied type and
	 * the arguments to this call.
	 * <p/>
	 * NOTE : the _context-implied_ type is the type implied by where the function's
	 * occurs in the query.  E.g., for an equality predicate (`something = some_function`)
	 * the implied type of the return from `some_function` would be defined by the type
	 * of `some_function`.
	 *
	 * @param impliedType the context-impled type
	 * @param arguments the arguments "passed" to this call.
	 *
	 * @return The resolved type.
	 */
	<T> AllowableFunctionReturnType<T> resolveFunctionReturnType(
			AllowableFunctionReturnType<T> impliedType,
			List<SqmExpression> arguments);
}
