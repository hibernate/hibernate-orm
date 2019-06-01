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
 * Postgres extract() function returns {@link TemporalUnit#DAY_OF_WEEK}
 * numbered from 0 to 6. This isn't consistent with what most other
 * databases do, so here we adjust the result by generating
 * {@code (extract(dow,arg)+1)).
 *
 *
 * @author Gavin King
 */
public class PostgresExtractEmulation
		extends AbstractSqmFunctionTemplate {

	public PostgresExtractEmulation() {
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
		TemporalUnit unit = ((SqmExtractUnit<?>) arguments.get(0)).getUnit();
		String pattern;
		pattern = unit == TemporalUnit.DAY_OF_WEEK
				? "(extract(?1 from ?2)+1)"
				: "extract(?1 from ?2)";
		return queryEngine.getSqmFunctionRegistry()
						.patternTemplateBuilder("extract", pattern)
						.setReturnTypeResolver( useArgType(1) )
						.setExactArgumentCount(2)
						.template()
						.makeSqmFunctionExpression(
								arguments,
								impliedResultType,
								queryEngine,
								typeConfiguration
						);
	}

}
