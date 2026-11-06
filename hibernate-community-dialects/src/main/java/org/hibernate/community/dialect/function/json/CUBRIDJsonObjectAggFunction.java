package org.hibernate.community.dialect.function.json;

import org.hibernate.community.dialect.aggregate.internal.CUBRIDAggregateSupport;
import org.hibernate.dialect.function.json.MySQLJsonObjectAggFunction;
import org.hibernate.sql.ast.spi.query.expression.Expression;
import org.hibernate.sql.ast.spi.query.expression.JsonNullBehavior;
import org.hibernate.sql.ast.spi.translation.SqlAstTranslator;
import org.hibernate.sql.spi.SqlAppender;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * CUBRID json_objectagg function.
 */
public class CUBRIDJsonObjectAggFunction extends MySQLJsonObjectAggFunction {

	public CUBRIDJsonObjectAggFunction(TypeConfiguration typeConfiguration) {
		super( typeConfiguration );
	}

	@Override
	protected void renderArgument(
			SqlAppender sqlAppender,
			Expression arg,
			JsonNullBehavior nullBehavior,
			SqlAstTranslator<?> translator) {
		// a JSON string turned into text loses its quotes, so take the element text of a one-element array
		if ( nullBehavior != JsonNullBehavior.NULL ) {
			sqlAppender.appendSql( "nullif(" );
		}
		sqlAppender.appendSql( "substr(concat(json_array(" );
		appendValue( sqlAppender, arg, translator );
		sqlAppender.appendSql( ")),2,char_length(concat(json_array(" );
		appendValue( sqlAppender, arg, translator );
		sqlAppender.appendSql( ")))-2)" );
		if ( nullBehavior != JsonNullBehavior.NULL ) {
			sqlAppender.appendSql( ",'null')" );
		}
	}

	private static void appendValue(SqlAppender sqlAppender, Expression arg, SqlAstTranslator<?> translator) {
		CUBRIDAggregateSupport.INSTANCE.appendJsonWriteExpression(
				sqlAppender,
				() -> arg.accept( translator ),
				arg.getExpressionType().getSingleJdbcMapping()
		);
	}
}
