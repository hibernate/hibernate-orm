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

import static org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers.invariant;
import static org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers.useArgType;

/**
 * Derby doesn't have an extract() function but we
 * can emulate it using the functions it does have.
 *
 * @author Gavin King
 */
public class DerbyExtractEmulation
		extends AbstractSqmFunctionTemplate {

	public DerbyExtractEmulation() {
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
		return queryEngine.getSqmFunctionRegistry()
				.namedTemplateBuilder( "extract", unit.toString() )
				.setReturnTypeResolver( invariant( StandardSpiBasicTypes.INTEGER) )
				.setExactArgumentCount( 1 )
				.template()
				.makeSqmFunctionExpression(
						arguments.get( 1 ),
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
