package org.hibernate.community.dialect.function.json;

import org.hibernate.dialect.function.json.JsonExistsFunction;
import org.hibernate.dialect.function.json.JsonPathHelper;
import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.sql.ast.spi.query.expression.JsonPathPassingClause;
import org.hibernate.sql.ast.spi.translation.SqlAstTranslator;
import org.hibernate.sql.spi.SqlAppender;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * CUBRID json_exists function.
 */
public class CUBRIDJsonExistsFunction extends JsonExistsFunction {

	public CUBRIDJsonExistsFunction(TypeConfiguration typeConfiguration) {
		super( typeConfiguration, true, false );
	}

	@Override
	protected void render(
			SqlAppender sqlAppender,
			JsonExistsArguments arguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		// json_contains_path returns 0 or 1, but CUBRID needs a logical expression
		sqlAppender.appendSql( "(json_contains_path(" );
		arguments.jsonDocument().accept( walker );
		sqlAppender.appendSql( ",'one'," );
		final JsonPathPassingClause passingClause = arguments.passingClause();
		if ( passingClause == null ) {
			arguments.jsonPath().accept( walker );
		}
		else {
			JsonPathHelper.appendJsonPathConcatPassingClause( sqlAppender, arguments.jsonPath(), passingClause, walker );
		}
		sqlAppender.appendSql( ")=1)" );
	}
}
