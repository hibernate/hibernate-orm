/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.produce.function;

import org.hibernate.metamodel.mapping.MappingModelExpressible;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.query.sqm.tree.expression.SqmFunction;

/**
 * Pluggable strategy for resolving a function argument type for a specific call.
 *
 * @author Christian Beikov
 */
public interface FunctionArgumentTypeResolver {
	/**
	 * Resolve the argument type for a function given its context-implied return type.
	 * <p>
	 * The <em>context-implied</em> type is the type implied by where the function
	 * occurs in the query.  E.g., for an equality predicate (`something = some_function`)
	 * the implied type would be defined by the type of `something`.
	 *
	 * @return The resolved type.
	 */
	MappingModelExpressible<?> resolveFunctionArgumentType(
			SqmFunction<?> function,
			int argumentIndex,
			SqmToSqlAstConverter converter);
}
