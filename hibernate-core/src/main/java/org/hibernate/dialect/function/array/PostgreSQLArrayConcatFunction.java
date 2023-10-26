/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function.array;

import java.util.List;

import org.hibernate.query.ReturnableType;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;

/**
 * PostgreSQL variant of the function to properly return {@code null} when one of the arguments is null.
 */
public class PostgreSQLArrayConcatFunction extends ArrayConcatFunction {

	public PostgreSQLArrayConcatFunction() {
		super( "", "||", "" );
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		sqlAppender.append( "case when " );
		String separator = "";
		for ( SqlAstNode node : sqlAstArguments ) {
			sqlAppender.append( separator );
			node.accept( walker );
			sqlAppender.append( " is not null" );
			separator = " and ";
		}

		sqlAppender.append( " then " );
		super.render( sqlAppender, sqlAstArguments, returnType, walker );
		sqlAppender.append( " end" );
	}
}
