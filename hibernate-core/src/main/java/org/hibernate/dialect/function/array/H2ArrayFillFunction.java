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
 * Implement the array fill function by using {@code system_range}.
 */
public class H2ArrayFillFunction extends AbstractArrayFillFunction {

	public H2ArrayFillFunction(boolean list) {
		super( list );
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		sqlAppender.append( "coalesce((select array_agg(" );
		sqlAstArguments.get( 0 ).accept( walker );
		sqlAppender.append( ") from system_range(1," );
		sqlAstArguments.get( 1 ).accept( walker );
		sqlAppender.append( ")),array[])" );
	}
}
