/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function;

import java.util.List;

import org.hibernate.query.ReturnableType;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.common.TemporalUnit;
import org.hibernate.query.sqm.function.AbstractSqmFunctionDescriptor;
import org.hibernate.query.sqm.function.FunctionRenderer;
import org.hibernate.query.sqm.function.SelfRenderingSqmFunction;
import org.hibernate.query.sqm.produce.function.ArgumentTypesValidator;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionArgumentTypeResolvers;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.expression.SqmExtractUnit;
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
 * Emulation of {@code trunc(datetime, temporal_unit)} function that leverages
 * formatting the datetime to string and back to truncate it
 *
 * @author Marco Belladelli
 */
public class DateTruncEmulation extends AbstractSqmFunctionDescriptor implements FunctionRenderer {
	protected final String toDateFunction;
	private final SqmFormat sqmFormat;

	protected DateTruncEmulation(String toDateFunction, TypeConfiguration typeConfiguration) {
		super(
				"trunc",
				new ArgumentTypesValidator( StandardArgumentsValidators.exactly( 2 ), TEMPORAL, TEMPORAL_UNIT ),
				StandardFunctionReturnTypeResolvers.useArgType( 1 ),
				StandardFunctionArgumentTypeResolvers.invariant( typeConfiguration, TEMPORAL, TEMPORAL_UNIT )
		);
		this.toDateFunction = toDateFunction;
		this.sqmFormat = new SqmFormat( "yyyy-MM-dd HH:mm:ss", typeConfiguration.getBasicTypeForJavaType( String.class ), null );
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		sqlAppender.appendSql( toDateFunction );
		sqlAppender.append( '(' );
		sqlAstArguments.get( 1 ).accept( walker );
		sqlAppender.append( ',' );
		sqlAstArguments.get( 2 ).accept( walker );
		sqlAppender.append( ')' );
	}

	@Override
	protected <T> SelfRenderingSqmFunction<T> generateSqmFunctionExpression(
			List<? extends SqmTypedNode<?>> arguments,
			ReturnableType<T> impliedResultType,
			QueryEngine queryEngine) {
		final NodeBuilder nodeBuilder = queryEngine.getCriteriaBuilder();
		final TemporalUnit temporalUnit = ( (SqmExtractUnit<?>) arguments.get( 1 ) ).getUnit();
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
		final SqmTypedNode<?> datetime = arguments.get( 0 );
		final SqmExpression<?> formatExpression = queryEngine.getSqmFunctionRegistry()
				.findFunctionDescriptor( "format" )
				.generateSqmExpression(
						asList(
								datetime,
								new SqmFormat(
										pattern,
										nodeBuilder.getTypeConfiguration().getBasicTypeForJavaType( String.class ),
										nodeBuilder
								)
						),
						null,
						queryEngine
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
											nodeBuilder.getTypeConfiguration().getBasicTypeForJavaType( String.class ),
											nodeBuilder
									)
							),
							null,
							queryEngine
					);
		}
		else {
			formattedDatetime = formatExpression;
		}

		return new SelfRenderingSqmFunction<>(
				this,
				this,
				// the first argument is needed for SybaseDateTruncEmulation
				asList( datetime, formattedDatetime, sqmFormat ),
				impliedResultType,
				null,
				getReturnTypeResolver(),
				nodeBuilder,
				getName()
		);
	}
}
