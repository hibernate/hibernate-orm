/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.dialect.OracleDialect;
import org.hibernate.query.ReturnableType;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.TemporalUnit;
import org.hibernate.query.sqm.function.AbstractSqmFunctionDescriptor;
import org.hibernate.query.sqm.function.FunctionRenderingSupport;
import org.hibernate.query.sqm.function.SelfRenderingSqmFunction;
import org.hibernate.query.sqm.produce.function.ArgumentTypesValidator;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionArgumentTypeResolvers;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.query.sqm.tree.expression.SqmDurationUnit;
import org.hibernate.query.sqm.tree.expression.SqmLiteral;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.type.spi.TypeConfiguration;

import static org.hibernate.query.sqm.produce.function.FunctionParameterType.TEMPORAL;
import static org.hibernate.query.sqm.produce.function.FunctionParameterType.TEMPORAL_UNIT;

/**
 * Trunc function which converts the {@link TemporalUnit}
 * to the format required for {@code trunc()}
 *
 * @author Marco Belladelli
 */
public class DateTruncTrunc extends AbstractSqmFunctionDescriptor implements FunctionRenderingSupport {

	public DateTruncTrunc(TypeConfiguration typeConfiguration) {
		super(
				"date_trunc",
				new ArgumentTypesValidator( StandardArgumentsValidators.exactly( 2 ), TEMPORAL_UNIT, TEMPORAL ),
				StandardFunctionReturnTypeResolvers.useArgType( 2 ),
				StandardFunctionArgumentTypeResolvers.invariant( typeConfiguration, TEMPORAL_UNIT, TEMPORAL )
		);
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			SqlAstTranslator<?> walker) {
		sqlAppender.appendSql( "trunc(" );
		sqlAstArguments.get( 1 ).accept( walker );
		sqlAppender.append( ',' );
		sqlAstArguments.get( 0 ).accept( walker );
		sqlAppender.append( ')' );
	}

	@Override
	protected <T> SelfRenderingSqmFunction<T> generateSqmFunctionExpression(
			List<? extends SqmTypedNode<?>> arguments,
			ReturnableType<T> impliedResultType,
			QueryEngine queryEngine,
			TypeConfiguration typeConfiguration) {
		final NodeBuilder nodeBuilder = queryEngine.getCriteriaBuilder();
		final TemporalUnit temporalUnit = ( (SqmDurationUnit<?>) arguments.get( 0 ) ).getUnit();
		final String pattern;
		switch ( temporalUnit ) {
			case YEAR:
				pattern = "YYYY";
				break;
			case MONTH:
				pattern = "MM";
				break;
			case WEEK:
				pattern = "IW";
				break;
			case DAY:
				pattern = "DD";
				break;
			case HOUR:
				pattern = "HH";
				break;
			case MINUTE:
				pattern = "MI";
				break;
			case SECOND:
				if ( nodeBuilder.getSessionFactory().getJdbcServices().getDialect() instanceof OracleDialect ) {
					// Oracle does not support truncating to seconds with the native function, use emulation
					return new DateTruncEmulation( "to_date", false, typeConfiguration )
							.generateSqmFunctionExpression(
									arguments,
									impliedResultType,
									queryEngine,
									typeConfiguration
							);
				}
				pattern = "SS";
				break;
			default:
				throw new UnsupportedOperationException( "Temporal unit not supported [" + temporalUnit + "]" );
		}

		final List<SqmTypedNode<?>> args = new ArrayList<>( 2 );
		args.add( new SqmLiteral<>(
				pattern,
				typeConfiguration.getBasicTypeForJavaType( String.class ),
				nodeBuilder
		) );
		args.add( arguments.get( 1 ) );
		return new SelfRenderingSqmFunction<>(
				this,
				this,
				args,
				impliedResultType,
				getArgumentsValidator(),
				getReturnTypeResolver(),
				nodeBuilder,
				"date_trunc"
		);
	}
}
