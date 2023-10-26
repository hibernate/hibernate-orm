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
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Implement the overlaps function by using {@code unnest}.
 */
public class ArrayOverlapsUnnestFunction extends AbstractArrayOverlapsFunction {

	public ArrayOverlapsUnnestFunction(boolean nullable, TypeConfiguration typeConfiguration) {
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
		sqlAppender.append( '(' );
		if ( ArrayHelper.isNullable( haystackExpression ) ) {
			haystackExpression.accept( walker );
			sqlAppender.append( " is not null and " );
		}
		if ( ArrayHelper.isNullable( needleExpression ) ) {
			needleExpression.accept( walker );
			sqlAppender.append( " is not null and " );
		}
		if ( !nullable ) {
			sqlAppender.append( "not exists(select 1 from unnest(" );
			needleExpression.accept( walker );
			sqlAppender.append( ") t(i) where t.i is null) and " );
		}
		sqlAppender.append( "exists(select * from unnest(" );
		needleExpression.accept( walker );
		sqlAppender.append( ")" );
		sqlAppender.append( " intersect " );
		sqlAppender.append( "select * from unnest(" );
		haystackExpression.accept( walker );
		sqlAppender.append( ")))" );
	}
}
