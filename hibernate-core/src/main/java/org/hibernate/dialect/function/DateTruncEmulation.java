/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function;

import java.util.ArrayList;
import java.util.List;

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
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.expression.SqmFormat;
import org.hibernate.query.sqm.tree.expression.SqmLiteral;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.type.spi.TypeConfiguration;

import static java.util.Arrays.asList;
import static org.hibernate.query.sqm.produce.function.FunctionParameterType.TEMPORAL;
import static org.hibernate.query.sqm.produce.function.FunctionParameterType.TEMPORAL_UNIT;

/**
 * Emulation of {@code datetrunc} function that leverages
 * formatting the datetime to string and back to truncate it
 *
 * @author Marco Belladelli
 */
public class DateTruncEmulation extends AbstractSqmFunctionDescriptor implements FunctionRenderingSupport {
	private final String toDateFunction;

	private final boolean useConvertToFormat;

	public DateTruncEmulation(String toDateFunction, boolean useConvertToFormat, TypeConfiguration typeConfiguration) {
		super(
				"date_trunc",
				new ArgumentTypesValidator( StandardArgumentsValidators.exactly( 2 ), TEMPORAL_UNIT, TEMPORAL ),
				StandardFunctionReturnTypeResolvers.useArgType( 2 ),
				StandardFunctionArgumentTypeResolvers.invariant( typeConfiguration, TEMPORAL_UNIT, TEMPORAL )
		);
		this.toDateFunction = toDateFunction;
		this.useConvertToFormat = useConvertToFormat;
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			SqlAstTranslator<?> walker) {
		sqlAppender.appendSql( toDateFunction );
		sqlAppender.append( '(' );
		if ( !useConvertToFormat ) {
			if ( toDateFunction.equalsIgnoreCase( "convert" ) ) {
				sqlAppender.append( "datetime," );
				sqlAstArguments.get( 0 ).accept( walker );
			}
			else {
				sqlAstArguments.get( 0 ).accept( walker );
				sqlAppender.append( ',' );
				sqlAstArguments.get( 1 ).accept( walker );
			}
		}
		else {
			// custom implementation that uses convert instead of format for Sybase
			// see: https://infocenter.sybase.com/help/index.jsp?topic=/com.sybase.infocenter.dc36271.1600/doc/html/san1393050437990.html
			sqlAppender.append( "datetime,substring(convert(varchar," );
			sqlAstArguments.get( 1 ).accept( walker );
			sqlAppender.append( ",21),1,17-len(" );
			sqlAstArguments.get( 0 ).accept( walker );
			sqlAppender.append( "))+" );
			sqlAstArguments.get( 0  ).accept( walker );
			sqlAppender.append( ",21" );
		}
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
		final String literal;
		switch ( temporalUnit ) {
			case YEAR:
				pattern = "yyyy";
				literal = "-01-01 00:00:00";
				break;
			case MONTH:
				pattern = "yyyy-MM";
				literal = "-01 00:00:00";
				break;
			case DAY:
				pattern = "yyyy-MM-dd";
				literal = " 00:00:00";
				break;
			case HOUR:
				pattern = "yyyy-MM-dd HH";
				literal = ":00:00";
				break;
			case MINUTE:
				pattern = "yyyy-MM-dd HH:mm";
				literal = ":00";
				break;
			case SECOND:
				pattern = "yyyy-MM-dd HH:mm:ss";
				literal = null;
				break;
			default:
				throw new UnsupportedOperationException( "Temporal unit not supported [" + temporalUnit + "]" );
		}

		final SqmTypedNode<?> datetime = arguments.get( 1 );
		final List<SqmTypedNode<?>> args = new ArrayList<>( 2 );
		if ( !useConvertToFormat ) {
			// use standard format function
			final SqmExpression<?> formatExpression = queryEngine.getSqmFunctionRegistry()
					.findFunctionDescriptor( "format" )
					.generateSqmExpression(
							asList(
									datetime,
									new SqmFormat(
											pattern,
											typeConfiguration.getBasicTypeForJavaType( String.class ),
											nodeBuilder
									)
							),
							null,
							queryEngine,
							typeConfiguration
					);
			final SqmExpression<?> formattedDatetime;
			if ( literal != null ) {
				formattedDatetime = queryEngine.getSqmFunctionRegistry()
						.findFunctionDescriptor( "concat" )
						.generateSqmExpression(
								asList(
										formatExpression,
										new SqmLiteral<>(
												literal,
												typeConfiguration.getBasicTypeForJavaType( String.class ),
												nodeBuilder
										)
								),
								null,
								queryEngine,
								typeConfiguration
						);
			}
			else {
				formattedDatetime = formatExpression;
			}
			args.add( formattedDatetime );
			args.add( new SqmFormat(
					"yyyy-MM-dd HH:mm:ss",
					typeConfiguration.getBasicTypeForJavaType( String.class ),
					nodeBuilder
			) );
		}
		else {
			args.add( new SqmLiteral<>(
					literal != null ? literal.replace( "-", "/" ) : "",
					typeConfiguration.getBasicTypeForJavaType( String.class ),
					nodeBuilder
			) );
			args.add( datetime );
		}
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
