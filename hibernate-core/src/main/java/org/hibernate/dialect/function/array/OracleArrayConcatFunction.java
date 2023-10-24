/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function.array;

import java.util.List;

import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;

/**
 * Oracle concatenation function for arrays.
 */
public class OracleArrayConcatFunction extends ArrayConcatFunction {

	public OracleArrayConcatFunction() {
		super( "(", ",", ")" );
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			SqlAstTranslator<?> walker) {
		JdbcMappingContainer expressionType = null;
		for ( SqlAstNode sqlAstArgument : sqlAstArguments ) {
			expressionType = ( (Expression) sqlAstArgument ).getExpressionType();
			if ( expressionType != null ) {
				break;
			}
		}

		final String arrayTypeName = DdlTypeHelper.getTypeName( expressionType, walker );
		sqlAppender.append( arrayTypeName );
		sqlAppender.append( "_concat" );
		super.render( sqlAppender, sqlAstArguments, walker );
	}
}
