/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function;

import java.util.List;
import java.util.function.Supplier;

import org.hibernate.metamodel.mapping.MappingModelExpressable;
import org.hibernate.query.sqm.function.SqmFunctionDescriptor;
import org.hibernate.query.sqm.produce.function.ArgumentsValidator;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.query.sqm.tree.SqmVisitableNode;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.tree.expression.Expression;

/**
 * Emulation `coalesce` using `nvl` on Oracle 8 and earlier which do not define `coalesce`
 *
 * @author Steve Ebersole
 * @author Gavin King
 */
public class NvlCoalesceEmulation implements SqmFunctionDescriptor {
	private static final ArgumentsValidator ARGUMENTS_VALIDATOR = StandardArgumentsValidators.min( 2 );

	@Override
	public Expression generateSqlExpression(
			String functionName,
			List<? extends SqmVisitableNode> arguments,
			Supplier<MappingModelExpressable> inferableTypeAccess,
			SqmToSqlAstConverter converter,
			SqlAstCreationState creationState) {
		ARGUMENTS_VALIDATOR.validate( arguments );

		// we assume these is a function named `nvl` registered
		final SqmFunctionDescriptor nvlDescriptor = creationState.getCreationContext().getSessionFactory()
				.getQueryEngine()
				.getSqmFunctionRegistry()
				.getFunctionDescriptor( functionName );

		return nvlDescriptor.generateSqlExpression( functionName, arguments, inferableTypeAccess, converter, creationState );
	}
}
