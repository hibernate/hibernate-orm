/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.spatial.dialect.oracle;

import java.util.List;

import org.hibernate.query.ReturnableType;
import org.hibernate.query.sqm.function.NamedSqmFunctionDescriptor;
import org.hibernate.query.sqm.produce.function.ArgumentsValidator;
import org.hibernate.query.sqm.produce.function.FunctionReturnTypeResolver;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;

public class OracleSpatialFunction extends NamedSqmFunctionDescriptor {

	public OracleSpatialFunction(String name, boolean useParenthesesWhenNoArgs, ArgumentsValidator argValidator, FunctionReturnTypeResolver returnTypeResolver){
		super( name, useParenthesesWhenNoArgs, argValidator, returnTypeResolver );
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		super.render( sqlAppender, sqlAstArguments, returnType, walker );
	}
}
