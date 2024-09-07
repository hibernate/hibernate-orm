/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.dialect.Dialect;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.metamodel.mapping.MappingModelExpressible;
import org.hibernate.query.ReturnableType;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.common.TemporalUnit;
import org.hibernate.query.sqm.function.AbstractSqmFunctionDescriptor;
import org.hibernate.query.sqm.function.AbstractSqmSelfRenderingFunctionDescriptor;
import org.hibernate.query.sqm.function.FunctionRenderer;
import org.hibernate.query.sqm.function.MultipatternSqmFunctionDescriptor;
import org.hibernate.query.sqm.function.SelfRenderingFunctionSqlAstExpression;
import org.hibernate.query.sqm.function.SelfRenderingSqmFunction;
import org.hibernate.query.sqm.function.SqmFunctionDescriptor;
import org.hibernate.query.sqm.produce.function.ArgumentTypesValidator;
import org.hibernate.query.sqm.produce.function.ArgumentsValidator;
import org.hibernate.query.sqm.produce.function.FunctionReturnTypeResolver;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.spi.StringBuilderSqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.BinaryArithmeticExpression;
import org.hibernate.sql.ast.tree.expression.CaseSearchedExpression;
import org.hibernate.sql.ast.tree.expression.DurationUnit;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.Format;
import org.hibernate.sql.ast.tree.expression.QueryLiteral;
import org.hibernate.sql.ast.tree.expression.SqlTuple;
import org.hibernate.sql.ast.tree.expression.SqlTupleContainer;
import org.hibernate.sql.ast.tree.predicate.BetweenPredicate;
import org.hibernate.sql.ast.tree.predicate.ComparisonPredicate;
import org.hibernate.type.BasicType;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.spi.TypeConfiguration;

import static org.hibernate.query.sqm.BinaryArithmeticOperator.DIVIDE_PORTABLE;
import static org.hibernate.query.sqm.BinaryArithmeticOperator.MODULO;
import static org.hibernate.query.sqm.ComparisonOperator.GREATER_THAN_OR_EQUAL;
import static org.hibernate.query.sqm.ComparisonOperator.LESS_THAN;
import static org.hibernate.query.sqm.ComparisonOperator.LESS_THAN_OR_EQUAL;
import static org.hibernate.query.sqm.produce.function.FunctionParameterType.STRING;
import static org.hibernate.query.sqm.produce.function.FunctionParameterType.TEMPORAL;
import static org.hibernate.query.sqm.produce.function.StandardArgumentsValidators.exactly;
import static org.hibernate.query.sqm.produce.function.StandardFunctionArgumentTypeResolvers.invariant;
import static org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers.invariant;

/**
 * A format function with support for composite temporal expressions.
 *
 * @author Christian Beikov
 */
public class FormatFunction extends AbstractSqmFunctionDescriptor implements FunctionRenderer {

	private final String nativeFunctionName;
	private final boolean reversedArguments;
	private final boolean concatPattern;
	private final boolean supportsTime;

	public FormatFunction(String nativeFunctionName, TypeConfiguration typeConfiguration) {
		this( nativeFunctionName, false, true, typeConfiguration );
	}

	public FormatFunction(
			String nativeFunctionName,
			boolean reversedArguments,
			boolean concatPattern,
			TypeConfiguration typeConfiguration) {
		this( nativeFunctionName, reversedArguments, concatPattern, true, typeConfiguration );
	}

	public FormatFunction(
			String nativeFunctionName,
			boolean reversedArguments,
			boolean concatPattern,
			boolean supportsTime,
			TypeConfiguration typeConfiguration) {
		super(
				"format",
				new ArgumentTypesValidator( exactly( 2 ), TEMPORAL, STRING ),
				invariant( typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.STRING ) ),
				invariant( typeConfiguration, TEMPORAL, STRING )
		);
		this.nativeFunctionName = nativeFunctionName;
		this.reversedArguments = reversedArguments;
		this.concatPattern = concatPattern;
		this.supportsTime = supportsTime;
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		sqlAppender.appendSql( nativeFunctionName );
		sqlAppender.append( '(' );
		final SqlAstNode expression = sqlAstArguments.get( 0 );
		final SqlAstNode format = sqlAstArguments.get( 1 );
		if ( reversedArguments ) {
			format.accept( walker );
			sqlAppender.append( ',' );
			if ( !supportsTime && isTimeTemporal( expression ) ) {
				sqlAppender.append( "date'1970-01-01'+" );
			}
			expression.accept( walker );
		}
		else {
			if ( !supportsTime && isTimeTemporal( expression ) ) {
				sqlAppender.append( "date'1970-01-01'+" );
			}
			expression.accept( walker );
			sqlAppender.append( ',' );
			format.accept( walker );
		}
		sqlAppender.append( ')' );
	}

	private boolean isTimeTemporal(SqlAstNode expression) {
		if ( expression instanceof Expression ) {
			final JdbcMappingContainer expressionType = ( (Expression) expression ).getExpressionType();
			if ( expressionType.getJdbcTypeCount() == 1 ) {
				switch ( expressionType.getSingleJdbcMapping().getJdbcType().getDefaultSqlTypeCode() ) {
					case SqlTypes.TIME:
					case SqlTypes.TIME_WITH_TIMEZONE:
					case SqlTypes.TIME_UTC:
						return true;
					default:
						break;
				}
			}
		}
		return false;
	}

	@Override
	protected <T> SelfRenderingSqmFunction<T> generateSqmFunctionExpression(
			List<? extends SqmTypedNode<?>> arguments,
			ReturnableType<T> impliedResultType,
			QueryEngine queryEngine) {
		return new FormatSqmFunction<>(
				this,
				this,
				arguments,
				impliedResultType,
				getArgumentsValidator(),
				getReturnTypeResolver(),
				concatPattern,
				queryEngine
		);
	}

	@Override
	public String getArgumentListSignature() {
		return "(TEMPORAL datetime as STRING pattern)";
	}

	protected static class FormatSqmFunction<T> extends SelfRenderingSqmFunction<T> {

		private final boolean supportsPatternLiterals;
		private final TypeConfiguration typeConfiguration;

		public FormatSqmFunction(
				SqmFunctionDescriptor descriptor,
				FunctionRenderer renderer,
				List<? extends SqmTypedNode<?>> arguments,
				ReturnableType<T> impliedResultType,
				ArgumentsValidator argumentsValidator,
				FunctionReturnTypeResolver returnTypeResolver,
				boolean supportsPatternLiterals,
				QueryEngine queryEngine) {
			super(
					descriptor,
					renderer,
					arguments,
					impliedResultType,
					argumentsValidator,
					returnTypeResolver,
					queryEngine.getCriteriaBuilder(),
					"format"
			);
			this.supportsPatternLiterals = supportsPatternLiterals;
			this.typeConfiguration = queryEngine.getTypeConfiguration();
		}

		@Override
		public Expression convertToSqlAst(SqmToSqlAstConverter walker) {
			final List<SqlAstNode> arguments = resolveSqlAstArguments( getArguments(), walker );
			final ReturnableType<?> resultType = resolveResultType( walker );
			final MappingModelExpressible<?> mappingModelExpressible = resultType == null ? null : getMappingModelExpressible(
					walker,
					resultType,
					arguments
			);
			final SqlAstNode expression = arguments.get( 0 );
			if ( expression instanceof SqlTupleContainer ) {
				// SqlTupleContainer means this is a composite temporal type i.e. uses `@TimeZoneStorage(COLUMN)`
				// The support for this kind of type requires that we inject the offset from the second column
				// as literal into the pattern, and apply the formatting on the date time part
				final SqlTuple sqlTuple = ( (SqlTupleContainer) expression ).getSqlTuple();
				final AbstractSqmSelfRenderingFunctionDescriptor timestampaddFunction = getFunction(
						walker,
						"timestampadd"
				);
				final BasicType<Integer> integerType = typeConfiguration.getBasicTypeRegistry()
						.resolve( StandardBasicTypes.INTEGER );
				arguments.set( 0, getOffsetAdjusted( sqlTuple, timestampaddFunction, integerType ) );
				if ( getArgumentsValidator() != null ) {
					getArgumentsValidator().validateSqlTypes( arguments, getFunctionName() );
				}
				final Format format = (Format) arguments.get( 1 );
				// If the format contains a time zone or offset, we must replace that with the offset column
				if ( format.getFormat().contains( "x" ) || !supportsPatternLiterals ) {
					final AbstractSqmSelfRenderingFunctionDescriptor concatFunction = getFunction(
							walker,
							"concat"
					);
					final AbstractSqmSelfRenderingFunctionDescriptor substringFunction = getFunction(
							walker,
							"substring",
							3
					);
					final BasicType<String> stringType = typeConfiguration.getBasicTypeRegistry()
							.resolve( StandardBasicTypes.STRING );
					final Dialect dialect = walker.getCreationContext()
							.getSessionFactory()
							.getJdbcServices()
							.getDialect();
					Expression formatExpression = null;
					final StringBuilder sb = new StringBuilder();
					final StringBuilderSqlAppender sqlAppender = new StringBuilderSqlAppender( sb );
					final String delimiter;
					if ( supportsPatternLiterals ) {
						dialect.appendDatetimeFormat( sqlAppender, "'a'" );
						delimiter = sb.substring( 0, sb.indexOf( "a" ) ).replace( "''", "'" );
					}
					else {
						delimiter = "";
					}
					final String[] chunks = StringHelper.splitFull( "'", format.getFormat() );
					final Expression offsetExpression = sqlTuple.getExpressions().get( 1 );
					// Splitting by `'` will put actual format pattern parts to even indices and literal pattern parts
					// to uneven indices. We will only replace the time zone and offset pattern in the format pattern parts
					for ( int i = 0; i < chunks.length; i += 2 ) {
						// The general idea is to replace the various patterns `xxx`, `xx` and `x` by concatenating
						// the offset column as literal i.e. `HH:mmxxx` is translated to `HH:mm'''||offset||'''`
						// xxx stands for the full offset i.e. `+01:00`
						// xx stands for the medium offset i.e. `+0100`
						// x stands for the small offset i.e. `+01`
						final String[] fullParts = StringHelper.splitFull( "xxx", chunks[i] );
						for ( int j = 0; j < fullParts.length; j++ ) {
							if ( fullParts[j].isEmpty() ) {
								continue;
							}
							final String[] mediumParts = StringHelper.splitFull( "xx", fullParts[j] );
							for ( int k = 0; k < mediumParts.length; k++ ) {
								if ( mediumParts[k].isEmpty() ) {
									continue;
								}
								final String[] smallParts = StringHelper.splitFull( "x", mediumParts[k] );
								for ( int l = 0; l < smallParts.length; l++ ) {
									if ( smallParts[l].isEmpty() ) {
										continue;
									}
									sb.setLength( 0 );
									dialect.appendDatetimeFormat( sqlAppender, smallParts[l] );
									final String formatPart = sb.toString();
									if ( supportsPatternLiterals ) {
										formatExpression = concat(
												concatFunction,
												stringType,
												formatExpression,
												new QueryLiteral<>( formatPart, stringType )
										);
									}
									else {
										formatExpression = concat(
												concatFunction,
												stringType,
												formatExpression,
												new SelfRenderingFunctionSqlAstExpression(
														getFunctionName(),
														getFunctionRenderer(),
														List.of(
																arguments.get( 0 ),
																new QueryLiteral<>( formatPart, stringType )
														),
														resultType,
														mappingModelExpressible
												)
										);
									}
									if ( l + 1 < smallParts.length ) {
										// This is for `x` patterns, which require `+01`
										// so we concat `substring(offset, 1, 4)`
										// Since the offset is always in the full format
										formatExpression = concatAsLiteral(
												concatFunction,
												stringType,
												delimiter,
												formatExpression,
												createSmallOffset(
														concatFunction,
														substringFunction,
														stringType,
														integerType,
														offsetExpression
												)
										);
									}
								}
								if ( k + 1 < mediumParts.length ) {
									// This is for `xx` patterns, which require `+0100`
									// so we concat `substring(offset, 1, 4)||substring(offset, 4, 6)`
									// Since the offset is always in the full format
									formatExpression = concatAsLiteral(
											concatFunction,
											stringType,
											delimiter,
											formatExpression,
											createMediumOffset(
													concatFunction,
													substringFunction,
													stringType,
													integerType,
													offsetExpression
											)
									);
								}
							}
							if ( j + 1 < fullParts.length ) {
								formatExpression = concatAsLiteral(
										concatFunction,
										stringType,
										delimiter,
										formatExpression,
										createFullOffset(
												concatFunction,
												stringType,
												integerType,
												offsetExpression
										)
								);
							}
						}

						if ( i + 1 < chunks.length ) {
							// Handle the pattern literal content
							final String formatLiteralPart;
							if ( supportsPatternLiterals ) {
								sb.setLength( 0 );
								dialect.appendDatetimeFormat( sqlAppender, "'" + chunks[i + 1] + "'" );
								formatLiteralPart = sb.toString().replace( "''", "'" );
							}
							else {
								formatLiteralPart = chunks[i + 1];
							}
							formatExpression = concat(
									concatFunction,
									stringType,
									formatExpression,
									new QueryLiteral<>(
											formatLiteralPart,
											stringType
									)
							);
						}
					}

					if ( supportsPatternLiterals ) {
						arguments.set( 1, formatExpression );
					}
					else {
						return formatExpression;
					}
				}
			}
			else {
				if ( getArgumentsValidator() != null ) {
					getArgumentsValidator().validateSqlTypes( arguments, getFunctionName() );
				}

				if ( !supportsPatternLiterals ) {
					final AbstractSqmSelfRenderingFunctionDescriptor concatFunction = getFunction(
							walker,
							"concat"
					);
					final BasicType<String> stringType = typeConfiguration.getBasicTypeRegistry()
							.resolve( StandardBasicTypes.STRING );
					Expression formatExpression = null;
					final Format format = (Format) arguments.get( 1 );
					final String[] chunks = StringHelper.splitFull( "'", format.getFormat() );
					// Splitting by `'` will put actual format pattern parts to even indices and literal pattern parts
					// to uneven indices. We need to apply the format parts and then concatenate because the pattern
					// doesn't support literals
					for ( int i = 0; i < chunks.length; i += 2 ) {
						formatExpression = concat(
								concatFunction,
								stringType,
								formatExpression,
								new SelfRenderingFunctionSqlAstExpression(
										getFunctionName(),
										getFunctionRenderer(),
										List.of( arguments.get( 0 ), new Format( chunks[i] ) ),
										resultType,
										mappingModelExpressible
								)
						);
						if ( i + 1 < chunks.length ) {
							// Handle the pattern literal content
							formatExpression = concat(
									concatFunction,
									stringType,
									formatExpression,
									new QueryLiteral<>( chunks[i + 1], stringType )
							);
						}
					}
					return formatExpression;
				}
			}
			return new SelfRenderingFunctionSqlAstExpression(
					getFunctionName(),
					getFunctionRenderer(),
					arguments,
					resultType,
					mappingModelExpressible
			);
		}

		private AbstractSqmSelfRenderingFunctionDescriptor getFunction(SqmToSqlAstConverter walker, String name) {
			return (AbstractSqmSelfRenderingFunctionDescriptor) walker.getCreationContext()
					.getSessionFactory()
					.getQueryEngine()
					.getSqmFunctionRegistry()
					.findFunctionDescriptor( name );
		}

		private AbstractSqmSelfRenderingFunctionDescriptor getFunction(SqmToSqlAstConverter walker, String name, int argumentCount) {
			final SqmFunctionDescriptor functionDescriptor = walker.getCreationContext()
					.getSessionFactory()
					.getQueryEngine()
					.getSqmFunctionRegistry()
					.findFunctionDescriptor( name );
			if ( functionDescriptor instanceof MultipatternSqmFunctionDescriptor ) {
				return (AbstractSqmSelfRenderingFunctionDescriptor)
						( (MultipatternSqmFunctionDescriptor) functionDescriptor )
								.getFunction( argumentCount );
			}
			return (AbstractSqmSelfRenderingFunctionDescriptor) functionDescriptor;
		}

		private SqlAstNode getOffsetAdjusted(
				SqlTuple sqlTuple,
				AbstractSqmSelfRenderingFunctionDescriptor timestampaddFunction,
				BasicType<Integer> integerType) {
			final Expression instantExpression = sqlTuple.getExpressions().get( 0 );
			final Expression offsetExpression = sqlTuple.getExpressions().get( 1 );

			return new SelfRenderingFunctionSqlAstExpression(
					"timestampadd",
					timestampaddFunction,
					List.of(
							new DurationUnit( TemporalUnit.SECOND, integerType ),
							offsetExpression,
							instantExpression
					),
					(ReturnableType<?>) instantExpression.getExpressionType(),
					instantExpression.getExpressionType()
			);
		}

		private Expression createFullOffset(
				AbstractSqmSelfRenderingFunctionDescriptor concatFunction,
				BasicType<String> stringType,
				BasicType<Integer> integerType,
				Expression offsetExpression) {
			if ( offsetExpression.getExpressionType().getSingleJdbcMapping().getJdbcType().isString() ) {
				return offsetExpression;
			}
			else {
				// ZoneOffset as seconds
				final CaseSearchedExpression caseSearchedExpression =
						zoneOffsetSeconds( stringType, integerType, offsetExpression );
				final Expression hours = getHours( integerType, offsetExpression );
				final Expression minutes = getMinutes( integerType, offsetExpression );

				final CaseSearchedExpression minuteStart = new CaseSearchedExpression( stringType );
				minuteStart.getWhenFragments().add(
						new CaseSearchedExpression.WhenFragment(
								new BetweenPredicate(
										minutes,
										new QueryLiteral<>( -9, integerType ),
										new QueryLiteral<>( 9, integerType ),
										false,
										null
								),
								new QueryLiteral<>( ":0", stringType )
						)
				);
				minuteStart.otherwise( new QueryLiteral<>( ":", stringType ) );
				return concat(
						concatFunction,
						stringType,
						concat(
							concatFunction,
							stringType,
							concat( concatFunction, stringType, caseSearchedExpression, hours ),
							minuteStart
						),
						minutes
				);
			}
		}

		private Expression createMediumOffset(
				AbstractSqmSelfRenderingFunctionDescriptor concatFunction,
				AbstractSqmSelfRenderingFunctionDescriptor substringFunction,
				BasicType<String> stringType,
				BasicType<Integer> integerType,
				Expression offsetExpression) {
			if ( offsetExpression.getExpressionType().getSingleJdbcMapping().getJdbcType().isString() ) {
				return concat(
						concatFunction,
						stringType,
						createSmallOffset(
								concatFunction,
								substringFunction,
								stringType,
								integerType,
								offsetExpression
						),
						new SelfRenderingFunctionSqlAstExpression(
								"substring",
								substringFunction,
								List.of(
										offsetExpression,
										new QueryLiteral<>( 4, integerType ),
										new QueryLiteral<>( 6, integerType )
								),
								stringType,
								stringType
						)
				);
			}
			else {
				// ZoneOffset as seconds
				final CaseSearchedExpression caseSearchedExpression =
						zoneOffsetSeconds( stringType, integerType, offsetExpression );

				final Expression hours = getHours( integerType, offsetExpression );
				final Expression minutes = getMinutes( integerType, offsetExpression );

				final CaseSearchedExpression minuteStart = new CaseSearchedExpression( stringType );
				minuteStart.getWhenFragments().add(
						new CaseSearchedExpression.WhenFragment(
								new BetweenPredicate(
										minutes,
										new QueryLiteral<>( -9, integerType ),
										new QueryLiteral<>( 9, integerType ),
										false,
										null
								),
								new QueryLiteral<>( "0", stringType )
						)
				);
				minuteStart.otherwise( new QueryLiteral<>( "", stringType ) );
				return concat(
						concatFunction,
						stringType,
						concat(
								concatFunction,
								stringType,
								concat( concatFunction, stringType, caseSearchedExpression, hours ),
								minuteStart
						),
						minutes
				);
			}
		}

		private Expression createSmallOffset(
				AbstractSqmSelfRenderingFunctionDescriptor concatFunction,
				AbstractSqmSelfRenderingFunctionDescriptor substringFunction,
				BasicType<String> stringType,
				BasicType<Integer> integerType,
				Expression offsetExpression) {
			if ( offsetExpression.getExpressionType().getSingleJdbcMapping().getJdbcType().isString() ) {
				return new SelfRenderingFunctionSqlAstExpression(
						"substring",
						substringFunction,
						List.of(
								offsetExpression,
								new QueryLiteral<>( 1, integerType ),
								new QueryLiteral<>( 4, integerType )
						),
						stringType,
						stringType
				);
			}
			else {
				// ZoneOffset as seconds
				final CaseSearchedExpression caseSearchedExpression =
						zoneOffsetSeconds( stringType, integerType, offsetExpression );
				final Expression hours = getHours( integerType, offsetExpression );
				return concat( concatFunction, stringType, caseSearchedExpression, hours );
			}
		}

		private Expression concatAsLiteral(
				AbstractSqmSelfRenderingFunctionDescriptor concatFunction,
				BasicType<String> stringType,
				String delimiter,
				Expression expression,
				Expression expression2) {
			return concat(
					concatFunction,
					stringType,
					concat(
							concatFunction,
							stringType,
							concat(
									concatFunction,
									stringType,
									expression,
									new QueryLiteral<>( delimiter, stringType )
							),
							expression2
					),
					new QueryLiteral<>( delimiter, stringType )
			);
		}

		private Expression concat(
				AbstractSqmSelfRenderingFunctionDescriptor concatFunction,
				BasicType<String> stringType,
				Expression expression,
				Expression expression2) {
			if ( expression == null ) {
				return expression2;
			}
			else if ( expression instanceof SelfRenderingFunctionSqlAstExpression
					&& "concat".equals( ( (SelfRenderingFunctionSqlAstExpression) expression ).getFunctionName() ) ) {
				List<SqlAstNode> list = (List<SqlAstNode>) ( (SelfRenderingFunctionSqlAstExpression) expression ).getArguments();
				final SqlAstNode lastOperand = list.get( list.size() - 1 );
				if ( expression2 instanceof QueryLiteral<?> && lastOperand instanceof QueryLiteral<?> ) {
					list.set(
							list.size() - 1,
							new QueryLiteral<>(
									( (QueryLiteral<?>) lastOperand ).getLiteralValue().toString() +
											( (QueryLiteral<?>) expression2 ).getLiteralValue().toString(),
									stringType
							)
					);
				}
				else {
					list.add( expression2 );
				}
				return expression;
			}
			else if ( expression2 instanceof SelfRenderingFunctionSqlAstExpression
					&& "concat".equals( ( (SelfRenderingFunctionSqlAstExpression) expression2 ).getFunctionName() ) ) {
				final List<SqlAstNode> list = (List<SqlAstNode>)
						( (SelfRenderingFunctionSqlAstExpression) expression2 ).getArguments();
				final SqlAstNode firstOperand = list.get( 0 );
				if ( expression instanceof QueryLiteral<?> && firstOperand instanceof QueryLiteral<?> ) {
					list.set(
							list.size() - 1,
							new QueryLiteral<>(
									( (QueryLiteral<?>) expression ).getLiteralValue().toString() +
											( (QueryLiteral<?>) firstOperand ).getLiteralValue().toString(),
									stringType
							)
					);
				}
				else {
					list.add( 0, expression );
				}
				return expression2;
			}
			else if ( expression instanceof QueryLiteral<?> && expression2 instanceof QueryLiteral<?> ) {
				return new QueryLiteral<>(
						( (QueryLiteral<?>) expression ).getLiteralValue().toString() +
								( (QueryLiteral<?>) expression2 ).getLiteralValue().toString(),
						stringType
				);
			}
			else {
				final List<Expression> list = new ArrayList<>( 2 );
				list.add( expression );
				list.add( expression2 );
				return new SelfRenderingFunctionSqlAstExpression(
							"concat",
							concatFunction,
							list,
							stringType,
							stringType
					);
			}
		}

		private Expression getHours(
				BasicType<Integer> integerType,
				Expression offsetExpression) {
			return /*new SelfRenderingFunctionSqlAstExpression(
					"cast",
					castFunction,
					List.of(*/
							new BinaryArithmeticExpression(
									offsetExpression,
									DIVIDE_PORTABLE,
									new QueryLiteral<>( 3600, integerType ),
									integerType
							)/*,
							new CastTarget( integerType )
					),
					integerType,
					integerType
			)*/;
		}

		private Expression getMinutes(
				BasicType<Integer> integerType,
				Expression offsetExpression){
			return /*new SelfRenderingFunctionSqlAstExpression(
					"cast",
					castFunction,
					List.of(*/
							new BinaryArithmeticExpression(
									new BinaryArithmeticExpression(
											offsetExpression,
											MODULO,
											new QueryLiteral<>( 3600, integerType ),
											integerType
									),
									DIVIDE_PORTABLE,
									new QueryLiteral<>( 60, integerType ),
									integerType
							)/*,
							new CastTarget( integerType )
					),
					integerType,
					integerType
			)*/;
		}
	}

	private static CaseSearchedExpression zoneOffsetSeconds(BasicType<String> stringType, BasicType<Integer> integerType, Expression offsetExpression) {
		final CaseSearchedExpression caseSearchedExpression = new CaseSearchedExpression(stringType);
		caseSearchedExpression.getWhenFragments().add(
				new CaseSearchedExpression.WhenFragment(
						new ComparisonPredicate(
								offsetExpression,
								LESS_THAN_OR_EQUAL,
								new QueryLiteral<>( -36000, integerType)
						),
						new QueryLiteral<>( "-", stringType)
				)
		);
		caseSearchedExpression.getWhenFragments().add(
				new CaseSearchedExpression.WhenFragment(
						new ComparisonPredicate(
								offsetExpression,
								LESS_THAN,
								new QueryLiteral<>( 0, integerType)
						),
						new QueryLiteral<>( "-0", stringType)
				)
		);
		caseSearchedExpression.getWhenFragments().add(
				new CaseSearchedExpression.WhenFragment(
						new ComparisonPredicate(
								offsetExpression,
								GREATER_THAN_OR_EQUAL,
								new QueryLiteral<>( 36000, integerType)
						),
						new QueryLiteral<>( "+", stringType)
				)
		);
		caseSearchedExpression.otherwise( new QueryLiteral<>( "+0", stringType) );
		return caseSearchedExpression;
	}
}
