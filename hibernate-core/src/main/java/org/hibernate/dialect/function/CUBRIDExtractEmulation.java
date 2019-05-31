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
 * CUBRID supports a limited list of temporal fields in the
 * extract() function, but we can emulate some of them by
 * using the appropriate named functions instead of
 * extract().
 *
 * Thus, the additional supported fields are
 * {@link TemporalUnit#DAY_OF_YEAR},
 * {@link TemporalUnit#DAY_OF_MONTH},
 * {@link TemporalUnit#DAY_OF_YEAR}.
 *
 * In addition, the field {@link TemporalUnit#SECOND} is
 * redefined to include milliseconds.
 *
 * This class is very nearly a duplicate of
 * {@link MySQLExtractEmulation}.
 *
 * @author Gavin King
 */
public class CUBRIDExtractEmulation
		extends AbstractSqmFunctionTemplate {

	public CUBRIDExtractEmulation() {
		super(
				StandardArgumentsValidators.exactly( 2 ),
				StandardFunctionReturnTypeResolvers.invariant( StandardSpiBasicTypes.STRING )
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
				pattern = "(second(?2)+extract(millisecond from ?2)/1e3)";
				break;
			case DAY_OF_WEEK:
				pattern = "dayofweek(?2)";
				break;
			case DAY_OF_MONTH:
				pattern = "dayofmonth(?2)";
				break;
			case DAY_OF_YEAR:
				pattern = "dayofyear(?2)";
				break;
			case WEEK:
				pattern = "week(?2,3)"; //mode 3 is the ISO week
				break;
			default:
				pattern = unit + "(?2)";
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

}
