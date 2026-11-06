package org.hibernate.community.dialect.function.json;

import org.hibernate.dialect.function.json.MySQLJsonExistsFunction;
import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.sql.ast.spi.translation.SqlAstTranslator;
import org.hibernate.sql.spi.SqlAppender;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * CUBRID json_exists function.
 */
public class CUBRIDJsonExistsFunction extends MySQLJsonExistsFunction {

	public CUBRIDJsonExistsFunction(TypeConfiguration typeConfiguration) {
		super( typeConfiguration );
	}

	@Override
	protected void render(
			SqlAppender sqlAppender,
			JsonExistsArguments arguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		// json_contains_path returns 0 or 1, but CUBRID needs a logical expression
		sqlAppender.appendSql( '(' );
		super.render( sqlAppender, arguments, returnType, walker );
		sqlAppender.appendSql( "=1)" );
	}
}
