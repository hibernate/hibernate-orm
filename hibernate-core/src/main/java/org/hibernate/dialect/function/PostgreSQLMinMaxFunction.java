/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function;

import java.util.List;

import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.query.sqm.function.AbstractSqmSelfRenderingFunctionDescriptor;
import org.hibernate.query.sqm.function.FunctionKind;
import org.hibernate.query.sqm.produce.function.ArgumentTypesValidator;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionArgumentTypeResolvers;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.type.SqlTypes;

import static org.hibernate.query.sqm.produce.function.FunctionParameterType.COMPARABLE;

/**
 * PostgreSQL doesn't support min/max for uuid yet,
 * but since that type is comparable we want to support this operation.
 * The workaround is to cast uuid to text and aggregate that, which preserves the ordering,
 * and finally cast the result back to uuid.
 *
 * @author Christian Beikov
 */
public class PostgreSQLMinMaxFunction extends AbstractSqmSelfRenderingFunctionDescriptor {

	public PostgreSQLMinMaxFunction(String name) {
		super(
				name,
				FunctionKind.AGGREGATE,
				new ArgumentTypesValidator( StandardArgumentsValidators.exactly( 1 ), COMPARABLE ),
				StandardFunctionReturnTypeResolvers.useFirstNonNull(),
				StandardFunctionArgumentTypeResolvers.IMPLIED_RESULT_TYPE
		);
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
		sqlAppender.appendSql( getName() );
		sqlAppender.appendSql( '(' );
		final Expression arg = (Expression) sqlAstArguments.get( 0 );
		final String castTarget;
		if ( caseWrapper ) {
			translator.getCurrentClauseStack().push( Clause.WHERE );
			sqlAppender.appendSql( "case when " );
			filter.accept( translator );
			translator.getCurrentClauseStack().pop();
			sqlAppender.appendSql( " then " );
			castTarget = renderArgument( sqlAppender, translator, arg );
			sqlAppender.appendSql( " else null end)" );
		}
		else {
			castTarget = renderArgument( sqlAppender, translator, arg );
			sqlAppender.appendSql( ')' );
			if ( filter != null ) {
				translator.getCurrentClauseStack().push( Clause.WHERE );
				sqlAppender.appendSql( " filter (where " );
				filter.accept( translator );
				sqlAppender.appendSql( ')' );
				translator.getCurrentClauseStack().pop();
			}
		}
		if ( castTarget != null ) {
			sqlAppender.appendSql( "::" );
			sqlAppender.appendSql( castTarget );
		}
	}

	private String renderArgument(SqlAppender sqlAppender, SqlAstTranslator<?> translator, Expression arg) {
		final JdbcMapping sourceMapping = arg.getExpressionType().getJdbcMappings().get( 0 );
		// Cast uuid expressions to "text" first, aggregate that, and finally cast to uuid again
		if ( sourceMapping.getJdbcType().getDefaultSqlTypeCode() == SqlTypes.UUID ) {
			sqlAppender.appendSql( "cast(" );
			arg.accept( translator );
			sqlAppender.appendSql( " as text)" );
			return "uuid";
		}
		else {
			arg.accept( translator );
			return null;
		}
	}

	@Override
	public String getArgumentListSignature() {
		return "(COMPARABLE arg)";
	}

}
