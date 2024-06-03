/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function.array;

import java.util.List;

import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.query.ReturnableType;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.type.BasicPluralType;
import org.hibernate.type.spi.TypeConfiguration;

public class OracleArrayContainsFunction extends AbstractArrayContainsFunction {

	public OracleArrayContainsFunction(boolean nullable, TypeConfiguration typeConfiguration) {
		super( nullable, typeConfiguration );
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		final Expression haystackExpression = (Expression) sqlAstArguments.get( 0 );
		final Expression needleExpression = (Expression) sqlAstArguments.get( 1 );
		final JdbcMappingContainer needleTypeContainer = needleExpression.getExpressionType();
		final JdbcMapping needleType = needleTypeContainer == null ? null : needleTypeContainer.getSingleJdbcMapping();
		final String arrayTypeName = DdlTypeHelper.getTypeName(
				haystackExpression.getExpressionType(),
				walker.getSessionFactory().getTypeConfiguration()
		);
		sqlAppender.appendSql( arrayTypeName );
		if ( needleType == null || needleType instanceof BasicPluralType<?, ?> ) {
			LOG.deprecatedArrayContainsWithArray();
			sqlAppender.append( "_includes(" );
			haystackExpression.accept( walker );
			sqlAppender.append( ',' );
			sqlAstArguments.get( 1 ).accept( walker );
			sqlAppender.append( ',' );
			sqlAppender.append( nullable ? "1" : "0" );
			sqlAppender.append( ")>0" );
		}
		else {
			sqlAppender.append( "_position(" );
			haystackExpression.accept( walker );
			sqlAppender.append( ',' );
			needleExpression.accept( walker );
			sqlAppender.append( ")>0" );
		}
	}
}
