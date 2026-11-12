package org.hibernate.community.dialect.function.json;

import java.util.List;

import org.hibernate.community.dialect.aggregate.internal.CUBRIDAggregateSupport;
import org.hibernate.dialect.function.json.JsonObjectFunction;
import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.sql.ast.spi.SqlAstNode;
import org.hibernate.sql.ast.spi.query.expression.Expression;
import org.hibernate.sql.ast.spi.query.expression.JsonNullBehavior;
import org.hibernate.sql.ast.spi.translation.SqlAstTranslator;
import org.hibernate.sql.spi.SqlAppender;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * CUBRID json_object function.
 */
public class CUBRIDJsonObjectFunction extends JsonObjectFunction {

	public CUBRIDJsonObjectFunction(TypeConfiguration typeConfiguration) {
		super( typeConfiguration, false );
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		if ( sqlAstArguments.isEmpty() ) {
			sqlAppender.appendSql( "json_object()" );
			return;
		}
		final SqlAstNode lastArgument = sqlAstArguments.get( sqlAstArguments.size() - 1 );
		if ( lastArgument == JsonNullBehavior.ABSENT ) {
			// Same emulation as MySQL, but json_extract returns SQL null for a JSON null,
			// so null values are filtered with "is not null" instead of comparing to a JSON null
			final int argumentsCount = sqlAstArguments.size() - 1;
			sqlAppender.appendSql( "(select json_objectagg(t.k,t.v) from (select " );
			sqlAstArguments.get( 0 ).accept( walker );
			sqlAppender.appendSql( " k,json_extract(json_array(" );
			renderValue( sqlAppender, sqlAstArguments.get( 1 ), walker );
			sqlAppender.appendSql( "),'$[0]') v" );
			for ( int i = 2; i < argumentsCount; i += 2 ) {
				sqlAppender.appendSql( " union all select " );
				sqlAstArguments.get( i ).accept( walker );
				sqlAppender.appendSql( ",json_extract(json_array(" );
				renderValue( sqlAppender, sqlAstArguments.get( i + 1 ), walker );
				sqlAppender.appendSql( "),'$[0]')" );
			}
			sqlAppender.appendSql( ") t where t.v is not null)" );
		}
		else {
			final int argumentsCount = lastArgument instanceof JsonNullBehavior
					? sqlAstArguments.size() - 1
					: sqlAstArguments.size();
			sqlAppender.appendSql( "json_object" );
			char separator = '(';
			for ( int i = 0; i < argumentsCount; i += 2 ) {
				sqlAppender.appendSql( separator );
				sqlAstArguments.get( i ).accept( walker );
				sqlAppender.appendSql( ',' );
				renderValue( sqlAppender, sqlAstArguments.get( i + 1 ), walker );
				separator = ',';
			}
			sqlAppender.appendSql( ')' );
		}
	}

	@Override
	protected void renderValue(SqlAppender sqlAppender, SqlAstNode value, SqlAstTranslator<?> walker) {
		CUBRIDAggregateSupport.INSTANCE.appendJsonWriteExpression(
				sqlAppender,
				() -> value.accept( walker ),
				( (Expression) value ).getExpressionType().getSingleJdbcMapping()
		);
	}
}
