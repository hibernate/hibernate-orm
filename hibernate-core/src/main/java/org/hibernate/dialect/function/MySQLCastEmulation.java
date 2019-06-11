/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function;

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
import org.hibernate.sql.ast.produce.metamodel.spi.ExpressableType;
import org.hibernate.type.spi.StandardSpiBasicTypes;
import org.hibernate.type.spi.TypeConfiguration;

import java.util.List;

import static org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers.useArgType;

/**
 * MySQL's cast() function has many limitations, and, in
 * particular, since MySQL doesn't have a proper boolean
 * type, it can't be used to cast string data to boolean.
 *
 * @author Gavin King
 */
public class MySQLCastEmulation
		extends AbstractSqmFunctionTemplate {

	public MySQLCastEmulation() {
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
		SqmExpression<?> arg = (SqmExpression<?>) arguments.get(0);
		AllowableFunctionReturnType<?> type = targetType.getType();
		ExpressableType<?> argType = arg.getExpressableType();
		SqmFunctionTemplate template;
		if ( argType!=null
				&& String.class.equals( type.getJavaType() )
				&& Boolean.class.equals( argType.getJavaType() ) ) {
			template = queryEngine.getSqmFunctionRegistry()
					.patternTemplateBuilder("cast", booleanToStringPattern())
					.setInvariantType( StandardSpiBasicTypes.STRING )
					.setExactArgumentCount( 2 )
					.template();
		}
		//Identical code to DerbyCastEmulation:
		else if ( argType!=null
				&& Boolean.class.equals( type.getJavaType() )
				&& Number.class.isAssignableFrom( argType.getJavaType() ) ) {
			template = queryEngine.getSqmFunctionRegistry()
					.patternTemplateBuilder("cast", numberToBooleanPattern())
					.setInvariantType( StandardSpiBasicTypes.BOOLEAN )
					.setExactArgumentCount( 2 )
					.template();
		}
		else if ( argType!=null
				&& Boolean.class.equals( type.getJavaType() )
				&& String.class.equals( argType.getJavaType() ) ) {
			template = queryEngine.getSqmFunctionRegistry()
					.patternTemplateBuilder("cast", stringToBooleanPattern())
					.setInvariantType( StandardSpiBasicTypes.BOOLEAN )
					.setExactArgumentCount( 2 )
					.template();
		}
		else {
			template = queryEngine.getSqmFunctionRegistry()
					.patternTemplateBuilder("cast", defaultPattern())
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

	protected String defaultPattern() {
		return "cast(?1 as ?2)";
	}

	protected String stringToBooleanPattern() {
		return "if(?1 rlike '^(t|f|true|false)$', ?1 like 't%', null)";
	}

	protected String numberToBooleanPattern() {
		return "(?1<>0)";
	}

	protected String booleanToStringPattern() {
		return "if(?1,'true','false')";
	}

	@Override
	public String getArgumentListSignature() {
		return "(arg as type)";
	}

}
