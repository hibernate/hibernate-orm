/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function;

import org.hibernate.metamodel.model.domain.spi.AllowableFunctionReturnType;
import org.hibernate.query.TemporalUnit;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.query.sqm.produce.function.internal.SelfRenderingSqmFunction;
import org.hibernate.query.sqm.produce.function.spi.AbstractSqmFunctionTemplate;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.query.sqm.tree.expression.function.SqmExtractUnit;
import org.hibernate.type.spi.StandardSpiBasicTypes;
import org.hibernate.type.spi.TypeConfiguration;

import java.util.List;

import static org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers.useArgType;

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
public class InformixExtractEmulation
		extends AbstractSqmFunctionTemplate {

	public InformixExtractEmulation() {
		super(
				StandardArgumentsValidators.exactly( 2 ),
				StandardFunctionReturnTypeResolvers.useArgType( 1 )
		);
	}

	@Override
	protected <T> SelfRenderingSqmFunction generateSqmFunctionExpression(
			List<SqmTypedNode<?>> arguments,
			AllowableFunctionReturnType<T> impliedResultType,
			QueryEngine queryEngine,
			TypeConfiguration typeConfiguration) {
		SqmExtractUnit<?> extractUnit = (SqmExtractUnit<?>) arguments.get(0);
		TemporalUnit unit = extractUnit.getUnit();
		String pattern;
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
		return queryEngine.getSqmFunctionRegistry()
				.patternTemplateBuilder("extract", pattern)
				.setReturnTypeResolver( useArgType(1) )
				.setExactArgumentCount( 2 )
				.template()
				.makeSqmFunctionExpression(
						arguments,
						impliedResultType,
						queryEngine,
						typeConfiguration
				);
	}

	@Override
	public String getArgumentListSignature() {
		return "(field from datetime)";
	}

}
