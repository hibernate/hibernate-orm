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
 * Oracle supports a limited list of temporal fields in the
 * extract() function, but we can emulate some of them by
 * using to_char() with a format string instead of extract().
 *
 * Thus, the additional supported fields are
 * {@link TemporalUnit#DAY_OF_YEAR},
 * {@link TemporalUnit#DAY_OF_MONTH},
 * {@link TemporalUnit#DAY_OF_YEAR},
 * and {@link TemporalUnit#WEEK}.
 *
 * @author Gavin King
 */
public class OracleExtractEmulation
		extends AbstractSqmFunctionTemplate {

	public OracleExtractEmulation() {
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
		TemporalUnit unit = ((SqmExtractUnit<?>) arguments.get(0)).getUnit();
		String pattern;
		switch (unit) {
			case DAY_OF_WEEK:
				pattern = "to_number(to_char(?2,'D'))";
				break;
			case DAY_OF_MONTH:
				pattern = "to_number(to_char(?2,'DD'))";
				break;
			case DAY_OF_YEAR:
				pattern = "to_number(to_char(?2,'DDD'))";
				break;
			case WEEK:
				pattern = "to_number(to_char(?2,'IW'))"; //the ISO week number
				break;
			default:
				pattern = "extract(?1 from ?2)";
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
