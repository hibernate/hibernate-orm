package org.hibernate.community.dialect.function.json;

import java.util.List;

import org.hibernate.community.dialect.aggregate.internal.CUBRIDAggregateSupport;
import org.hibernate.dialect.function.json.MySQLJsonArrayFunction;
import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.sql.ast.spi.SqlAstNode;
import org.hibernate.sql.ast.spi.query.expression.Expression;
import org.hibernate.sql.ast.spi.query.expression.JsonNullBehavior;
import org.hibernate.sql.ast.spi.translation.SqlAstTranslator;
import org.hibernate.sql.spi.SqlAppender;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * CUBRID json_array function.
 */
public class CUBRIDJsonArrayFunction extends MySQLJsonArrayFunction {

	public CUBRIDJsonArrayFunction(TypeConfiguration typeConfiguration) {
		super( typeConfiguration );
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		if ( sqlAstArguments.isEmpty() || sqlAstArguments.get( sqlAstArguments.size() - 1 ) != JsonNullBehavior.ABSENT ) {
			super.render( sqlAppender, sqlAstArguments, returnType, walker );
		}
		else {
			// Same emulation as MySQL, which cannot keep the argument order with json_arrayagg
			final int argumentsCount = sqlAstArguments.size() - 1;
			sqlAppender.appendSql( '(' );
			for ( int i = argumentsCount - 1; i > 0; i-- ) {
				sqlAppender.appendSql( "select case when t.v is null then x.v else json_array_append(x.v,'$'," );
				appendJsonWriteExpression( sqlAppender, sqlAstArguments.get( i ), () -> sqlAppender.appendSql( "t.v" ) );
				sqlAppender.appendSql( ") end v from (select " );
				sqlAstArguments.get( i ).accept( walker );
				sqlAppender.appendSql( " v) t,(" );
			}
			sqlAppender.appendSql( "select case when t.v is null then json_array() else json_array(" );
			appendJsonWriteExpression( sqlAppender, sqlAstArguments.get( 0 ), () -> sqlAppender.appendSql( "t.v" ) );
			sqlAppender.appendSql( ") end v from (select " );
			sqlAstArguments.get( 0 ).accept( walker );
			sqlAppender.appendSql( " v) t" );
			for ( int i = 1; i < argumentsCount; i++ ) {
				sqlAppender.appendSql( ") x" );
			}
			sqlAppender.appendSql( ')' );
		}
	}

	@Override
	protected void renderValue(SqlAppender sqlAppender, SqlAstNode value, SqlAstTranslator<?> walker) {
		appendJsonWriteExpression( sqlAppender, value, () -> value.accept( walker ) );
	}

	private static void appendJsonWriteExpression(SqlAppender sqlAppender, SqlAstNode value, Runnable renderFunction) {
		CUBRIDAggregateSupport.INSTANCE.appendJsonWriteExpression(
				sqlAppender,
				renderFunction,
				( (Expression) value ).getExpressionType().getSingleJdbcMapping()
		);
	}
}
