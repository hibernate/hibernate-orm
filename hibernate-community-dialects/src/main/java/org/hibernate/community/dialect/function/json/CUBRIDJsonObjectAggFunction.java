package org.hibernate.community.dialect.function.json;

import org.hibernate.QueryException;
import org.hibernate.community.dialect.aggregate.internal.CUBRIDAggregateSupport;
import org.hibernate.dialect.function.json.JsonObjectAggFunction;
import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.sql.ast.spi.query.expression.Expression;
import org.hibernate.sql.ast.spi.query.expression.JsonNullBehavior;
import org.hibernate.sql.ast.spi.query.expression.JsonObjectAggUniqueKeysBehavior;
import org.hibernate.sql.ast.spi.query.predicate.Predicate;
import org.hibernate.sql.ast.spi.translation.Clause;
import org.hibernate.sql.ast.spi.translation.SqlAstTranslator;
import org.hibernate.sql.spi.SqlAppender;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * CUBRID json_objectagg function.
 */
public class CUBRIDJsonObjectAggFunction extends JsonObjectAggFunction {

	public CUBRIDJsonObjectAggFunction(TypeConfiguration typeConfiguration) {
		super( ",", false, typeConfiguration );
	}

	@Override
	protected void render(
			SqlAppender sqlAppender,
			JsonObjectAggArguments arguments,
			Predicate filter,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> translator) {
		final boolean uniqueKeys = arguments.uniqueKeysBehavior() == JsonObjectAggUniqueKeysBehavior.WITH;
		if ( uniqueKeys ) {
			// CUBRID rejects duplicate keys when it parses JSON
			sqlAppender.appendSql( "json_extract(" );
		}
		sqlAppender.appendSql( "concat('{',group_concat(concat(json_quote(" );
		arguments.key().accept( translator );
		sqlAppender.appendSql( "),':'," );
		if ( filter != null ) {
			if ( arguments.nullBehavior() != JsonNullBehavior.ABSENT ) {
				throw new QueryException( "Can't emulate json_objectagg filter clause when using 'null on null' clause.", (String) null );
			}
			translator.getCurrentClauseStack().push( Clause.WHERE );
			sqlAppender.appendSql( "case when " );
			filter.accept( translator );
			translator.getCurrentClauseStack().pop();
			sqlAppender.appendSql( " then " );
			renderArgument( sqlAppender, arguments.value(), arguments.nullBehavior(), translator );
			sqlAppender.appendSql( " else null end" );
		}
		else {
			renderArgument( sqlAppender, arguments.value(), arguments.nullBehavior(), translator );
		}
		sqlAppender.appendSql( ") separator ','),'}')" );
		if ( uniqueKeys ) {
			sqlAppender.appendSql( ",'$')" );
		}
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
