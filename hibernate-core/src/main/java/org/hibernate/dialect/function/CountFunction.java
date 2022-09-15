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
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.query.sqm.CastType;
import org.hibernate.query.sqm.function.AbstractSqmSelfRenderingFunctionDescriptor;
import org.hibernate.query.sqm.function.FunctionKind;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.query.sqm.produce.function.StandardFunctions;
import org.hibernate.query.sqm.produce.function.internal.PatternRenderer;
import org.hibernate.query.sqm.sql.internal.AbstractSqmPathInterpretation;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.SqlAstNodeRenderingMode;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Distinct;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.FunctionExpression;
import org.hibernate.sql.ast.tree.expression.SqlTuple;
import org.hibernate.sql.ast.tree.expression.SqlTupleContainer;
import org.hibernate.sql.ast.tree.expression.Star;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupJoin;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Christian Beikov
 */
public class CountFunction extends AbstractSqmSelfRenderingFunctionDescriptor {

	private final Dialect dialect;
	private final SqlAstNodeRenderingMode defaultArgumentRenderingMode;
	private final String concatOperator;
	private final String concatArgumentCastType;
	private final boolean castDistinctStringConcat;

	public CountFunction(
			Dialect dialect,
			TypeConfiguration typeConfiguration,
			SqlAstNodeRenderingMode defaultArgumentRenderingMode,
			String concatOperator) {
		this( dialect, typeConfiguration, defaultArgumentRenderingMode, concatOperator, null, false );
	}

	public CountFunction(
			Dialect dialect,
			TypeConfiguration typeConfiguration,
			SqlAstNodeRenderingMode defaultArgumentRenderingMode,
			String concatOperator,
			String concatArgumentCastType,
			boolean castDistinctStringConcat) {
		super(
				StandardFunctions.COUNT,
				FunctionKind.AGGREGATE,
				StandardArgumentsValidators.exactly( 1 ),
				StandardFunctionReturnTypeResolvers.invariant(
						typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.LONG )
				),
				null
		);
		this.dialect = dialect;
		this.defaultArgumentRenderingMode = defaultArgumentRenderingMode;
		this.concatOperator = concatOperator;
		this.concatArgumentCastType = concatArgumentCastType;
		this.castDistinctStringConcat = castDistinctStringConcat;
	}

	@Override
	public void render(SqlAppender sqlAppender, List<? extends SqlAstNode> sqlAstArguments, SqlAstTranslator<?> walker) {
		render( sqlAppender, sqlAstArguments, null, walker );
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			Predicate filter,
			SqlAstTranslator<?> translator) {
		final boolean caseWrapper = filter != null && !translator.supportsFilterClause();
		final SqlAstNode arg = sqlAstArguments.get( 0 );
		sqlAppender.appendSql( "count(" );
		final SqlTuple tuple;
		if ( arg instanceof Distinct ) {
			sqlAppender.appendSql( "distinct " );
			final Expression distinctArg = ( (Distinct) arg ).getExpression();
			if ( ( tuple = SqlTupleContainer.getSqlTuple( distinctArg ) ) != null ) {
				final List<? extends Expression> expressions = tuple.getExpressions();
				// Single element tuple
				if ( expressions.size() == 1 ) {
					renderSimpleArgument(
							sqlAppender,
							filter,
							translator,
							caseWrapper,
							expressions.get( 0 )
					);
				}
				// Emulate tuple distinct count
				else if ( !dialect.supportsTupleDistinctCounts() ) {
					// see https://hibernate.atlassian.net/browse/HHH-11042 details about this implementation
					// The idea is to concat all tuple elements, separated by a character that can't appear in the string
					// We choose to map this to the NUL character i.e. \0 with the ASCII code 0
					// To avoid collisions we must take special care of SQL NULL and empty strings,
					// which is why we map them to a special sequence:
					// NULL -> \0
					// '' -> \0 + argumentNumber
					// In the end, the expression looks like the following:
					// count(distinct coalesce(nullif(coalesce(col1 || '', '\0'), ''), '\01') || '\0' || coalesce(nullif(coalesce(col2 || '', '\0'), ''), '\02'))
					if ( caseWrapper ) {
						translator.getCurrentClauseStack().push( Clause.WHERE );
						sqlAppender.appendSql( "case when " );
						filter.accept( translator );
						sqlAppender.appendSql( " then " );
						translator.getCurrentClauseStack().pop();
					}
					if ( castDistinctStringConcat ) {
						sqlAppender.appendSql( "cast(" );
					}
					sqlAppender.appendSql( "coalesce(nullif(coalesce(" );
					boolean needsConcat = renderCastedArgument( sqlAppender, translator, expressions.get( 0 ) );
					int argumentNumber = 1;
					for ( int i = 1; i < expressions.size(); i++, argumentNumber++ ) {
						if ( needsConcat ) {
							// Concat with empty string to get implicit conversion
							sqlAppender.appendSql( concatOperator );
							sqlAppender.appendSql( "''" );
						}
						sqlAppender.appendSql( ",'\\0'),''),'\\0" );
						sqlAppender.appendSql( argumentNumber );
						sqlAppender.appendSql( "')" );
						sqlAppender.appendSql( concatOperator );
						sqlAppender.appendSql( "'\\0'" );
						sqlAppender.appendSql( concatOperator );
						sqlAppender.appendSql( "coalesce(nullif(coalesce(" );
						needsConcat = renderCastedArgument( sqlAppender, translator, expressions.get( i ) );
					}
					if ( needsConcat ) {
						// Concat with empty string to get implicit conversion
						sqlAppender.appendSql( concatOperator );
						sqlAppender.appendSql( "''" );
					}
					sqlAppender.appendSql( ",'\\0'),''),'\\0" );
					sqlAppender.appendSql( argumentNumber );
					sqlAppender.appendSql( "')" );
					if ( castDistinctStringConcat ) {
						sqlAppender.appendSql( " as " );
						sqlAppender.appendSql( concatArgumentCastType );
						sqlAppender.appendSql( ')' );
					}
					if ( caseWrapper ) {
						sqlAppender.appendSql( " else null end" );
					}
				}
				else {
					renderTupleCountSupported(
							sqlAppender,
							filter,
							translator,
							caseWrapper,
							tuple,
							expressions,
							dialect.requiresParensForTupleDistinctCounts()
					);
				}
			}
			else {
				renderSimpleArgument(
						sqlAppender,
						filter,
						translator,
						caseWrapper,
						distinctArg
				);
			}
		}
		else {
			if ( canReplaceWithStar( arg, translator ) ) {
				renderSimpleArgument( sqlAppender, filter, translator, caseWrapper, Star.INSTANCE );
			}
			else if ( ( tuple = SqlTupleContainer.getSqlTuple( arg ) ) != null ) {
				final List<? extends Expression> expressions = tuple.getExpressions();
				// Single element tuple
				if ( expressions.size() == 1 ) {
					renderSimpleArgument( sqlAppender, filter, translator, caseWrapper, expressions.get( 0 ) );
				}
				// Emulate the tuple count with a case when expression
				else if ( !dialect.supportsTupleCounts() ) {
					sqlAppender.appendSql( "case when " );
					if ( caseWrapper ) {
						translator.getCurrentClauseStack().push( Clause.WHERE );
						filter.accept( translator );
						translator.getCurrentClauseStack().pop();
						sqlAppender.appendSql( " and " );
					}
					translator.render( expressions.get( 0 ), defaultArgumentRenderingMode );
					sqlAppender.appendSql( " is not null" );
					for ( int i = 1; i < expressions.size(); i++ ) {
						sqlAppender.appendSql( " and " );
						translator.render( expressions.get( i ), defaultArgumentRenderingMode );
						sqlAppender.appendSql( " is not null" );
					}
					sqlAppender.appendSql( " then 1 else null end" );
				}
				// Tuple counts are supported
				else {
					renderTupleCountSupported(
							sqlAppender,
							filter,
							translator,
							caseWrapper,
							tuple,
							expressions,
							dialect.requiresParensForTupleCounts()
					);
				}
			}
			else {
				renderSimpleArgument( sqlAppender, filter, translator, caseWrapper, arg );
			}
		}
		sqlAppender.appendSql( ')' );
		if ( filter != null && !caseWrapper ) {
			translator.getCurrentClauseStack().push( Clause.WHERE );
			sqlAppender.appendSql( " filter (where " );
			filter.accept( translator );
			sqlAppender.appendSql( ')' );
			translator.getCurrentClauseStack().pop();
		}
	}

	private void renderTupleCountSupported(
			SqlAppender sqlAppender,
			Predicate filter,
			SqlAstTranslator<?> translator,
			boolean caseWrapper,
			SqlTuple tuple,
			List<? extends Expression> expressions,
			boolean requiresParenthesis) {
		if ( caseWrapper ) {
			// Add the case wrapper as first element instead of wrapping everything.
			// Rendering "Star" will result in `case when FILTER then 1 else null end`
			if ( requiresParenthesis ) {
				sqlAppender.appendSql( '(' );
				renderSimpleArgument( sqlAppender, filter, translator, true, Star.INSTANCE );
				sqlAppender.appendSql( ',' );
				renderCommaSeparatedList( sqlAppender, translator, expressions );
				sqlAppender.appendSql( ')' );
			}
			else {
				renderSimpleArgument( sqlAppender, filter, translator, true, Star.INSTANCE );
				sqlAppender.appendSql( ',' );
				renderCommaSeparatedList( sqlAppender, translator, expressions );
			}
		}
		// Rendering the tuple will add parenthesis around
		else if ( requiresParenthesis ) {
			translator.render( tuple, defaultArgumentRenderingMode );
		}
		else {
			renderCommaSeparatedList( sqlAppender, translator, expressions );
		}
	}

	private void renderCommaSeparatedList(
			SqlAppender sqlAppender,
			SqlAstTranslator<?> translator,
			List<? extends Expression> expressions) {
		translator.render( expressions.get( 0 ), defaultArgumentRenderingMode );
		for ( int i = 1; i < expressions.size(); i++ ) {
			sqlAppender.appendSql( ',' );
			translator.render( expressions.get( i ), defaultArgumentRenderingMode );
		}
	}

	private void renderSimpleArgument(
			SqlAppender sqlAppender,
			Predicate filter,
			SqlAstTranslator<?> translator,
			boolean caseWrapper,
			SqlAstNode realArg) {
		if ( caseWrapper ) {
			translator.getCurrentClauseStack().push( Clause.WHERE );
			sqlAppender.appendSql( "case when " );
			filter.accept( translator );
			translator.getCurrentClauseStack().pop();
			sqlAppender.appendSql( " then " );
			if ( realArg instanceof Star ) {
				sqlAppender.appendSql( "1" );
			}
			else {
				translator.render( realArg, defaultArgumentRenderingMode );
			}
			sqlAppender.appendSql( " else null end" );
		}
		else {
			translator.render( realArg, defaultArgumentRenderingMode );
		}
	}

	private boolean renderCastedArgument(SqlAppender sqlAppender, SqlAstTranslator<?> translator, Expression realArg) {
		if ( concatArgumentCastType == null ) {
			translator.render( realArg, defaultArgumentRenderingMode );
			return true;
		}
		else {
			final JdbcMapping sourceMapping = realArg.getExpressionType().getJdbcMappings().get( 0 );
			// No need to cast if we already have a string
			if ( sourceMapping.getCastType() == CastType.STRING ) {
				translator.render( realArg, defaultArgumentRenderingMode );
				return false;
			}
			else {
				final String cast = dialect.castPattern( sourceMapping.getCastType(), CastType.STRING );
				new PatternRenderer( cast.replace( "?2", concatArgumentCastType ) )
						.render( sqlAppender, Collections.singletonList( realArg ), translator );
				return false;
			}
		}
	}

	private boolean canReplaceWithStar(SqlAstNode arg, SqlAstTranslator<?> translator) {
		// To determine if we can replace the argument with a star, we must know if the argument is nullable
		if ( arg instanceof AbstractSqmPathInterpretation<?> ) {
			final AbstractSqmPathInterpretation<?> pathInterpretation = (AbstractSqmPathInterpretation<?>) arg;
			final TableGroup tableGroup = pathInterpretation.getTableGroup();
			final Expression sqlExpression = pathInterpretation.getSqlExpression();
			final JdbcMappingContainer expressionType = sqlExpression.getExpressionType();
			// The entity identifier mapping is always considered non-nullable
			final boolean isNonNullable = expressionType instanceof EntityIdentifierMapping;
			// If canUseInnerJoins is given for a table group, this means that it is non-optional
			// But we also have to check if it contains joins that could alter the nullability (RIGHT or FULL)
			if ( isNonNullable && tableGroup.canUseInnerJoins() && !hasJoinsAlteringNullability( tableGroup ) ) {
				// COUNT can only be used in query specs as query groups can only refer positionally in the order by
				final QuerySpec querySpec = (QuerySpec) translator.getCurrentQueryPart();
				// On top of this, we also have to ensure that there are no neighbouring joins that alter nullability
				for ( TableGroup root : querySpec.getFromClause().getRoots() ) {
					final Boolean result = hasNeighbouringJoinsAlteringNullability( root, tableGroup );
					if ( result != null ) {
						return !result;
					}
				}

				return true;
			}
		}
		return false;
	}

	private Boolean hasNeighbouringJoinsAlteringNullability(TableGroup tableGroup, TableGroup targetTableGroup) {
		if ( tableGroup == targetTableGroup ) {
			return Boolean.FALSE;
		}
		final List<TableGroupJoin> tableGroupJoins = tableGroup.getTableGroupJoins();
		int tableGroupIndex = -1;
		for ( int i = 0; i < tableGroupJoins.size(); i++ ) {
			final TableGroupJoin tableGroupJoin = tableGroupJoins.get( i );
			final Boolean result = hasNeighbouringJoinsAlteringNullability(
					tableGroupJoin.getJoinedGroup(),
					targetTableGroup
			);
			if ( result == Boolean.TRUE ) {
				return Boolean.TRUE;
			}
			else if ( result != null ) {
				tableGroupIndex = i;
				break;
			}
		}
		if ( tableGroupIndex != -1 ) {
			for ( int i = 0; i < tableGroupJoins.size(); i++ ) {
				if ( i == tableGroupIndex ) {
					continue;
				}
				final TableGroupJoin tableGroupJoin = tableGroupJoins.get( i );
				if ( hasJoinsAlteringNullability( tableGroupJoin ) ) {
					return Boolean.TRUE;
				}
			}
			return Boolean.FALSE;
		}
		return null;
	}

	private boolean hasJoinsAlteringNullability(TableGroup tableGroup) {
		for ( TableGroupJoin tableGroupJoin : tableGroup.getTableGroupJoins() ) {
			switch ( tableGroupJoin.getJoinType() ) {
				case INNER:
				case LEFT:
				case CROSS:
					if ( hasJoinsAlteringNullability( tableGroupJoin.getJoinedGroup() ) ) {
						return true;
					}
					break;
				default:
					// Other joins affect the nullability
					return true;
			}
		}

		return false;
	}

	private boolean hasJoinsAlteringNullability(TableGroupJoin neighbourJoin) {
		switch ( neighbourJoin.getJoinType() ) {
			case INNER:
			case LEFT:
			case CROSS:
				if ( hasJoinsAlteringNullability( neighbourJoin.getJoinedGroup() ) ) {
					return true;
				}
				break;
			default:
				// Other joins affect the nullability
				return true;
		}

		return false;
	}

	@Override
	public String getArgumentListSignature() {
		return "([distinct ]{arg|*})";
	}

}
