/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function;

import org.hibernate.dialect.Dialect;
import org.hibernate.metamodel.model.domain.spi.AllowableFunctionReturnType;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.produce.function.SqmFunctionTemplate;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.query.sqm.produce.function.internal.SelfRenderingSqmFunction;
import org.hibernate.query.sqm.produce.function.spi.AbstractSqmFunctionTemplate;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.expression.function.SqmCastTarget;
import org.hibernate.query.sqm.tree.expression.function.SqmExtractUnit;
import org.hibernate.sql.ast.produce.metamodel.spi.ExpressableType;
import org.hibernate.type.spi.StandardSpiBasicTypes;
import org.hibernate.type.spi.TypeConfiguration;

import java.util.List;

import static org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers.useArgType;

/**
 * @author Gavin King
 */
public class CastFunction
		extends AbstractSqmFunctionTemplate {

	private Dialect dialect;

	public CastFunction(Dialect dialect) {
		super(
				StandardArgumentsValidators.exactly( 2 ),
				StandardFunctionReturnTypeResolvers.useArgType( 2 )
		);
		this.dialect = dialect;
	}

	@Override
	protected <T> SelfRenderingSqmFunction<T> generateSqmFunctionExpression(
			List<SqmTypedNode<?>> arguments,
			AllowableFunctionReturnType<T> impliedResultType,
			QueryEngine queryEngine,
			TypeConfiguration typeConfiguration) {
		SqmCastTarget<?> targetType = (SqmCastTarget<?>) arguments.get(1);
		SqmExpression<?> arg = (SqmExpression<?>) arguments.get(0);
		AllowableFunctionReturnType<?> type = targetType.getType();
		ExpressableType<?> argType = arg.getExpressableType();

		String pattern;
		if ( argType!=null
				&& Float.class.equals( type.getJavaType() )
				&& String.class.equals( argType.getJavaType() ) ) {
			pattern = dialect.castStringToFloat();
		}
		else if ( argType!=null
				&& Double.class.equals( type.getJavaType() )
				&& String.class.equals( argType.getJavaType() ) ) {
			pattern = dialect.castStringToDouble();
		}
		else if ( argType!=null
				&& String.class.equals( type.getJavaType() )
				&& Boolean.class.equals( argType.getJavaType() ) ) {
			pattern = dialect.castBooleanToString();
		}
		else if ( argType!=null
				&& Boolean.class.equals( type.getJavaType() )
				&& Number.class.isAssignableFrom( argType.getJavaType() ) ) {
			pattern = dialect.castNumberToBoolean();
		}
		else if ( argType!=null
				&& Boolean.class.equals( type.getJavaType() )
				&& String.class.equals( argType.getJavaType() ) ) {
			pattern = dialect.castStringToBoolean();
		}
		else {
			pattern = dialect.cast();
		}
		return queryEngine.getSqmFunctionRegistry()
				.patternTemplateBuilder( "cast", pattern )
				.setExactArgumentCount( 2 )
				.setReturnTypeResolver( useArgType( 2 ) )
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
		return "(arg as Type)";
	}

}
