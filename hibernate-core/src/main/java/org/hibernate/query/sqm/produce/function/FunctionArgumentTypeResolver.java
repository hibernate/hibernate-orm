/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
