/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect.function.array;

import java.util.Collections;
import java.util.List;

import org.hibernate.query.ReturnableType;
import org.hibernate.query.sqm.function.AbstractSqmSelfRenderingFunctionDescriptor;
import org.hibernate.query.sqm.function.FunctionKind;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionArgumentTypeResolvers;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Distinct;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.ast.tree.select.SortSpecification;

/**
 * @author Christian Beikov
 */
public class ArrayAggFunction extends AbstractSqmSelfRenderingFunctionDescriptor {

	public static final String FUNCTION_NAME = "array_agg";

	private final String functionName;
	private final boolean withinGroupClause;
	private final boolean supportsFilter;

	public ArrayAggFunction(String functionName, boolean withinGroupClause, boolean supportsFilter) {
		super(
				FUNCTION_NAME,
				FunctionKind.ORDERED_SET_AGGREGATE,
				StandardArgumentsValidators.exactly( 1 ),
				ArrayViaElementArgumentReturnTypeResolver.VARARGS_INSTANCE,
				StandardFunctionArgumentTypeResolvers.NULL
		);
		this.functionName = functionName;
		this.withinGroupClause = withinGroupClause;
		this.supportsFilter = supportsFilter;
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		render( sqlAppender, sqlAstArguments, null, Collections.emptyList(), returnType, walker );
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			Predicate filter,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		render( sqlAppender, sqlAstArguments, filter, Collections.emptyList(), returnType, walker );
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			Predicate filter,
			List<SortSpecification> withinGroup,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> translator) {
		final boolean caseWrapper = filter != null && ( !supportsFilter || !translator.supportsFilterClause() );
		sqlAppender.appendSql( functionName );
		sqlAppender.appendSql( '(' );
		final SqlAstNode firstArg = sqlAstArguments.get( 0 );
		final Expression arg;
		if ( firstArg instanceof Distinct ) {
			sqlAppender.appendSql( "distinct " );
			arg = ( (Distinct) firstArg ).getExpression();
		}
		else {
			arg = (Expression) firstArg;
		}
		if ( caseWrapper ) {
			translator.getCurrentClauseStack().push( Clause.WHERE );
			sqlAppender.appendSql( "case when " );
			filter.accept( translator );
			sqlAppender.appendSql( " then " );
			arg.accept( translator );
			sqlAppender.appendSql( " else null end" );
			translator.getCurrentClauseStack().pop();
		}
		else {
			arg.accept( translator );
		}
		if ( withinGroup != null && !withinGroup.isEmpty() ) {
			if ( withinGroupClause ) {
				sqlAppender.appendSql( ')' );
				sqlAppender.appendSql( " within group (" );
			}
			translator.getCurrentClauseStack().push( Clause.WITHIN_GROUP );
			sqlAppender.appendSql( " order by " );
			withinGroup.get( 0 ).accept( translator );
			for ( int i = 1; i < withinGroup.size(); i++ ) {
				sqlAppender.appendSql( ',' );
				withinGroup.get( i ).accept( translator );
			}
			translator.getCurrentClauseStack().pop();
		}
		sqlAppender.appendSql( ')' );
		if ( !caseWrapper && filter != null ) {
			translator.getCurrentClauseStack().push( Clause.WHERE );
			sqlAppender.appendSql( " filter (where " );
			filter.accept( translator );
			sqlAppender.appendSql( ')' );
			translator.getCurrentClauseStack().pop();
		}
	}

}
