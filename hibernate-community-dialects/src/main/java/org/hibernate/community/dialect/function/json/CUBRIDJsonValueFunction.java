package org.hibernate.community.dialect.function.json;

import org.hibernate.dialect.function.json.JsonPathHelper;
import org.hibernate.dialect.function.json.MySQLJsonValueFunction;
import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.sql.ast.spi.query.expression.CastTarget;
import org.hibernate.sql.ast.spi.query.expression.JsonPathPassingClause;
import org.hibernate.sql.ast.spi.query.expression.JsonValueEmptyBehavior;
import org.hibernate.sql.ast.spi.query.expression.JsonValueErrorBehavior;
import org.hibernate.sql.ast.spi.translation.SqlAstTranslator;
import org.hibernate.sql.spi.SqlAppender;
import org.hibernate.type.spi.TypeConfiguration;

import static org.hibernate.type.SqlTypes.BINARY;
import static org.hibernate.type.SqlTypes.LONG32VARBINARY;
import static org.hibernate.type.SqlTypes.TIMESTAMP;
import static org.hibernate.type.SqlTypes.TIMESTAMP_UTC;
import static org.hibernate.type.SqlTypes.VARBINARY;

/**
 * CUBRID json_value function.
 */
public class CUBRIDJsonValueFunction extends MySQLJsonValueFunction {

	public CUBRIDJsonValueFunction(TypeConfiguration typeConfiguration) {
		super( typeConfiguration );
	}

	@Override
	protected void render(
			SqlAppender sqlAppender,
			JsonValueArguments arguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		final CastTarget returningType = arguments.returningType();
		if ( returningType != null
				&& ( arguments.errorBehavior() == null || arguments.errorBehavior() == JsonValueErrorBehavior.ERROR )
				&& ( arguments.emptyBehavior() == null || arguments.emptyBehavior() == JsonValueEmptyBehavior.NULL ) ) {
			switch ( returningType.getJdbcMapping().getJdbcType().getDefaultSqlTypeCode() ) {
				case BINARY, VARBINARY, LONG32VARBINARY -> {
					// binary data is written as hex
					sqlAppender.appendSql( "cast(" );
					renderText( sqlAppender, arguments, walker );
					sqlAppender.appendSql( " as bit varying)" );
					return;
				}
				case TIMESTAMP, TIMESTAMP_UTC -> {
					// casting text with a 'T' separator to datetime silently drops the time
					sqlAppender.appendSql( "cast(replace(" );
					renderText( sqlAppender, arguments, walker );
					sqlAppender.appendSql( ",'T',' ') as " );
					returningType.accept( walker );
					sqlAppender.appendSql( ')' );
					return;
				}
			}
		}
		super.render( sqlAppender, arguments, returnType, walker );
	}

	private static void renderText(SqlAppender sqlAppender, JsonValueArguments arguments, SqlAstTranslator<?> walker) {
		sqlAppender.appendSql( "json_unquote(nullif(json_extract(" );
		arguments.jsonDocument().accept( walker );
		sqlAppender.appendSql( ',' );
		final JsonPathPassingClause passingClause = arguments.passingClause();
		if ( passingClause == null ) {
			arguments.jsonPath().accept( walker );
		}
		else {
			JsonPathHelper.appendJsonPathConcatPassingClause( sqlAppender, arguments.jsonPath(), passingClause, walker );
		}
		sqlAppender.appendSql( "),cast('null' as json)))" );
	}
}
