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

/**
 * Informix has no extract() function, but we can
 * partially emulate it by using the appropriate
 * named functions, and by using to_char() with
 * a format string.
 *
 * The supported fields are
 * {@link TemporalUnit#HOUR},
 * {@link TemporalUnit#MINUTE},
 * {@link TemporalUnit#SECOND},
 * {@link TemporalUnit#DAY},
 * {@link TemporalUnit#MONTH},
 * {@link TemporalUnit#YEAR},
 * {@link TemporalUnit#QUARTER},
 * {@link TemporalUnit#DAY_OF_MONTH},
 * {@link TemporalUnit#DAY_OF_WEEK}.
 *
 * @author Gavin King
 */
public class InformixExtractEmulation implements SqmFunctionDescriptor {
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
		final String pattern;

		switch (unit) {
			case SECOND:
				pattern = "to_number(to_char(?2,'%S'))";
				break;
			case MINUTE:
				pattern = "to_number(to_char(?2,'%M'))";
				break;
			case HOUR:
				pattern = "to_number(to_char(?2,'%H'))";
				break;
			case DAY_OF_WEEK:
				pattern = "(weekday(?2)+1)";
				break;
			case DAY_OF_MONTH:
				pattern = "day(?2)";
				break;
			default:
				//I think week() returns the ISO week number
				pattern = unit.toString() + "(?2)";
				break;
		}

		final SqmFunctionDescriptor sqmPattern = converter.getCreationContext().getSessionFactory()
				.getQueryEngine()
				.getSqmFunctionRegistry()
				.patternDescriptorBuilder( functionName, pattern )
				.build();

		return sqmPattern.generateSqlExpression( functionName, arguments, inferableTypeAccess, converter, creationState );
	}
}
