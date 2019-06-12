/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function;

import java.util.Collections;
import java.util.List;

import org.hibernate.dialect.Dialect;
import org.hibernate.metamodel.model.domain.spi.AllowableFunctionReturnType;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.query.sqm.produce.function.internal.SelfRenderingSqmFunction;
import org.hibernate.query.sqm.produce.function.spi.AbstractSqmFunctionTemplate;
import org.hibernate.query.sqm.tree.expression.function.SqmTrimSpecification;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.expression.SqmLiteral;
import org.hibernate.sql.TrimSpec;
import org.hibernate.type.spi.StandardSpiBasicTypes;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Gavin King
 */
public class TrimFunction extends AbstractSqmFunctionTemplate {

	private Dialect dialect;

	public TrimFunction(Dialect dialect) {
		super(
				StandardArgumentsValidators.exactly( 3 ),
				StandardFunctionReturnTypeResolvers.invariant( StandardSpiBasicTypes.STRING )
		);
		this.dialect = dialect;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> SelfRenderingSqmFunction<T>  generateSqmFunctionExpression(
			List<SqmTypedNode<?>> arguments,
			AllowableFunctionReturnType<T> impliedResultType,
			QueryEngine queryEngine,
			TypeConfiguration typeConfiguration) {
		final TrimSpec specification = ( (SqmTrimSpecification) arguments.get( 0 ) ).getSpecification();
		final char trimCharacter = ( (SqmLiteral<Character>) arguments.get( 1 ) ).getLiteralValue();
		final SqmExpression sourceExpr = (SqmExpression) arguments.get( 2 );

		String trim = dialect.trimPattern( specification, trimCharacter );
		return queryEngine.getSqmFunctionRegistry()
				.patternTemplateBuilder( "trim", trim )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setExactArgumentCount( 1 )
				.template() //TODO: we could cache the 6 variations here
				.makeSqmFunctionExpression(
						Collections.singletonList( sourceExpr ),
						impliedResultType,
						queryEngine,
						typeConfiguration
				);
	}

	@Override
	public String getArgumentListSignature() {
		return "([[{leading|trailing|both} ][arg0 ]from] arg1)";
	}
}
