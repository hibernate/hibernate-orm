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
import org.hibernate.query.sqm.produce.function.SqmFunctionTemplate;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.query.sqm.produce.function.internal.SelfRenderingSqmFunction;
import org.hibernate.query.sqm.produce.function.spi.AbstractSqmFunctionTemplate;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.query.sqm.tree.expression.function.SqmCastTarget;
import org.hibernate.query.sqm.tree.expression.function.SqmExtractUnit;
import org.hibernate.type.spi.StandardSpiBasicTypes;
import org.hibernate.type.spi.TypeConfiguration;

import java.util.List;

import static org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers.invariant;
import static org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers.useArgType;

/**
 * Derby's cast() function doesn't support some
 * basic typecasts, such as casting a string to
 * a floating point value, but we make it work
 * using the double() function.
 *
 * @author Gavin King
 */
public class DerbyCastEmulation
		extends AbstractSqmFunctionTemplate {

	public DerbyCastEmulation() {
		super(
				StandardArgumentsValidators.exactly( 2 ),
				StandardFunctionReturnTypeResolvers.useArgType( 2 )
		);
	}

	@Override
	protected <T> SelfRenderingSqmFunction generateSqmFunctionExpression(
			List<SqmTypedNode<?>> arguments,
			AllowableFunctionReturnType<T> impliedResultType,
			QueryEngine queryEngine,
			TypeConfiguration typeConfiguration) {
		SqmCastTarget<?> targetType = (SqmCastTarget<?>) arguments.get(1);
		AllowableFunctionReturnType<?> type = targetType.getType();
		SqmFunctionTemplate template;
		if ( Float.class.equals( type.getJavaType() ) ) {
			template = queryEngine.getSqmFunctionRegistry().patternTemplateBuilder("cast", "cast(double(?1) as real)")
					.setInvariantType( StandardSpiBasicTypes.FLOAT )
					.setExactArgumentCount( 2 )
					.template();
		}
		else if ( Double.class.equals( type.getJavaType() ) ) {
			template = queryEngine.getSqmFunctionRegistry().patternTemplateBuilder("cast", "double(?1)")
					.setInvariantType( StandardSpiBasicTypes.DOUBLE )
					.setExactArgumentCount( 2 )
					.template();
		}
		else {
			template = queryEngine.getSqmFunctionRegistry()
					.patternTemplateBuilder("cast", "cast(?1 as ?2)")
					.setReturnTypeResolver( useArgType( 2 ) )
					.setExactArgumentCount( 2 )
					.template();
		}
		return template.makeSqmFunctionExpression(
				arguments,
				impliedResultType,
				queryEngine,
				typeConfiguration
		);
	}

	@Override
	public String getArgumentListSignature() {
		return "(arg as type)";
	}

}
