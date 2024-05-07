/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function.array;

import java.util.List;

import org.hibernate.query.ReturnableType;
import org.hibernate.sql.ast.SqlAstNodeRenderingMode;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * HSQLDB has a special syntax.
 */
public class HSQLArrayToStringFunction extends ArrayToStringFunction {

	public HSQLArrayToStringFunction(TypeConfiguration typeConfiguration) {
		super( typeConfiguration );
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		final Expression arrayExpression = (Expression) sqlAstArguments.get( 0 );
		final Expression separatorExpression = (Expression) sqlAstArguments.get( 1 );
		final Expression defaultExpression = sqlAstArguments.size() > 2 ? (Expression) sqlAstArguments.get( 2 ) : null;
		sqlAppender.append( "case when " );
		arrayExpression.accept( walker );
		sqlAppender.append( " is not null then coalesce((select group_concat(" );
		if ( defaultExpression != null ) {
			sqlAppender.append( "coalesce(" );
		}
		sqlAppender.append( "t.val" );
		if ( defaultExpression != null ) {
			sqlAppender.append( "," );
			defaultExpression.accept( walker );
			sqlAppender.append( ")" );
		}
		sqlAppender.append( " order by t.idx separator " );
		// HSQLDB doesn't like non-literals as separator
		walker.render( separatorExpression, SqlAstNodeRenderingMode.INLINE_PARAMETERS );
		sqlAppender.append( ") from unnest(");
		arrayExpression.accept( walker );
		sqlAppender.append(") with ordinality t(val,idx)),'') end" );
	}
}
