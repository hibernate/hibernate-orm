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
import org.hibernate.query.TemporalUnit;
import org.hibernate.query.sqm.function.SqmFunctionDescriptor;
import org.hibernate.query.sqm.produce.function.ArgumentsValidator;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.query.sqm.tree.SqmVisitableNode;
import org.hibernate.query.sqm.tree.expression.SqmExtractUnit;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.tree.expression.Expression;

import static org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers.useArgType;

/**
 * In H2, the extract() function does not return
 * fractional seconds for the the field
 * {@link TemporalUnit#SECOND}. We work around
 * this here with two calls to extract().
 *
 * @author Gavin King
 */
public class H2ExtractEmulation implements SqmFunctionDescriptor {
	private static final ArgumentsValidator ARGS_VALIDATOR = StandardArgumentsValidators.exactly( 2 );

	@Override
	public Expression generateSqlExpression(
			String functionName,
			List<? extends SqmVisitableNode> arguments,
			Supplier<MappingModelExpressable> inferableTypeAccess,
			SqmToSqlAstConverter converter,
			SqlAstCreationState creationState) {
		ARGS_VALIDATOR.validate( arguments );

		final SqmExtractUnit<?> extractUnit = (SqmExtractUnit<?>) arguments.get( 0 );
		final TemporalUnit unit = extractUnit.getTemporalUnit();
		final String pattern = unit == TemporalUnit.SECOND
				? "(extract(second from ?2)+extract(nanosecond from ?2)/1e9)"
				: "extract(?1 from ?2)";
		final SqmFunctionDescriptor sqmPattern = converter.getCreationContext().getSessionFactory()
				.getQueryEngine()
				.getSqmFunctionRegistry()
				.patternDescriptorBuilder( pattern )
				.setReturnTypeResolver( useArgType( 1 ) )
				.build();

		return sqmPattern.generateSqlExpression( functionName, arguments, inferableTypeAccess, converter, creationState );
	}
}
