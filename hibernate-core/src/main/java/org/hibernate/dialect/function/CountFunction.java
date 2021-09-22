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
import org.hibernate.query.sqm.function.AbstractSqmSelfRenderingFunctionDescriptor;
import org.hibernate.query.sqm.function.FunctionKind;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.query.sqm.sql.internal.AbstractSqmPathInterpretation;
import org.hibernate.query.sqm.sql.internal.EntityValuedPathInterpretation;
import org.hibernate.query.sqm.sql.internal.NonAggregatedCompositeValuedPathInterpretation;
import org.hibernate.sql.ast.SqlAstNodeRenderingMode;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.CaseSearchedExpression;
import org.hibernate.sql.ast.tree.expression.Distinct;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.NullnessLiteral;
import org.hibernate.sql.ast.tree.expression.QueryLiteral;
import org.hibernate.sql.ast.tree.expression.SqlTuple;
import org.hibernate.sql.ast.tree.expression.SqlTupleContainer;
import org.hibernate.sql.ast.tree.expression.Star;
import org.hibernate.sql.ast.tree.predicate.Junction;
import org.hibernate.sql.ast.tree.predicate.NullnessPredicate;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.type.StandardBasicTypes;

/**
 * @author Christian Beikov
 */
public class CountFunction extends AbstractSqmSelfRenderingFunctionDescriptor {

	public static final String FUNCTION_NAME = "count";
	private final Dialect dialect;

	public CountFunction(Dialect dialect) {
		super(
				FUNCTION_NAME,
				FunctionKind.AGGREGATE,
				StandardArgumentsValidators.exactly( 1 ),
				StandardFunctionReturnTypeResolvers.invariant( StandardBasicTypes.LONG )
		);
		this.dialect = dialect;
	}

	@Override
	public void render(SqlAppender sqlAppender, List<SqlAstNode> sqlAstArguments, SqlAstTranslator<?> walker) {
		render( sqlAppender, sqlAstArguments, null, walker );
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<SqlAstNode> sqlAstArguments,
			Predicate filter,
			SqlAstTranslator<?> translator) {
		final boolean caseWrapper = filter != null && !translator.supportsFilterClause();
		final SqlAstNode arg = sqlAstArguments.get( 0 );
		final SqlAstNode realArg;
		sqlAppender.appendSql( "count(" );
		if ( arg instanceof Distinct ) {
			sqlAppender.appendSql( "distinct " );
			final Expression distinctArg = ( (Distinct) arg ).getExpression();
			// todo (6.0): emulate tuple count distinct if necessary
			realArg = distinctArg;
		}
		else {
			// If the table group supports inner joins, this means that it is non-optional,
			// which means we can omit the tuples and instead use count(*)
			final SqlTuple tuple;
			if ( ( arg instanceof EntityValuedPathInterpretation<?> || arg instanceof NonAggregatedCompositeValuedPathInterpretation<?> )
					&& ( (AbstractSqmPathInterpretation<?>) arg ).getTableGroup().canUseInnerJoins() ) {
				realArg = Star.INSTANCE;
			}
			else if ( !dialect.supportsTupleCounts() && ( tuple = SqlTupleContainer.getSqlTuple( arg ) ) != null ) {
				final List<? extends Expression> expressions = tuple.getExpressions();
				if ( expressions.size() == 1 ) {
					realArg = expressions.get( 0 );
				}
				else {
					final List<CaseSearchedExpression.WhenFragment> whenFragments = new ArrayList<>( 1 );
					final Junction junction = new Junction( Junction.Nature.DISJUNCTION );
					for ( Expression expression : expressions ) {
						junction.add( new NullnessPredicate( expression ) );
					}
					whenFragments.add(
							new CaseSearchedExpression.WhenFragment(
									junction,
									new NullnessLiteral( StandardBasicTypes.INTEGER )
							)
					);
					realArg = new CaseSearchedExpression(
							StandardBasicTypes.INTEGER,
							whenFragments,
							new QueryLiteral<>( 1, StandardBasicTypes.INTEGER )
					);
				}
			}
			else {
				realArg = arg;
			}
		}
		if ( caseWrapper ) {
			sqlAppender.appendSql( "case when " );
			filter.accept( translator );
			sqlAppender.appendSql( " then " );
			if ( realArg instanceof Star ) {
				sqlAppender.appendSql( "1" );
			}
			else {
				translator.render( realArg, SqlAstNodeRenderingMode.DEFAULT );
			}
			sqlAppender.appendSql( " else null end" );
		}
		else {
			translator.render( realArg, SqlAstNodeRenderingMode.DEFAULT );
		}
		sqlAppender.appendSql( ')' );
		if ( filter != null && !caseWrapper ) {
			sqlAppender.appendSql( " filter (where " );
			filter.accept( translator );
			sqlAppender.appendSql( ')' );
		}
	}

	@Override
	public String getArgumentListSignature() {
		return "([distinct ]{arg|*})";
	}

}
