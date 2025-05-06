/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.array;

import java.util.List;

import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.sql.ast.SqlAstNodeRenderingMode;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.type.BasicPluralType;
import org.hibernate.type.SqlTypes;
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
		final BasicPluralType<?, ?> pluralType = (BasicPluralType<?, ?>) arrayExpression.getExpressionType().getSingleJdbcMapping();
		final int ddlTypeCode = pluralType.getElementType().getJdbcType().getDdlTypeCode();
		final boolean needsCast = !SqlTypes.isStringType( ddlTypeCode );
		sqlAppender.append( "case when " );
		arrayExpression.accept( walker );
		sqlAppender.append( " is not null then coalesce((select group_concat(" );
		if ( defaultExpression != null ) {
			sqlAppender.append( "coalesce(" );
		}
		if ( needsCast ) {
			if ( ddlTypeCode == SqlTypes.BOOLEAN ) {
				// By default, HSQLDB uses upper case, so lower it for a consistent experience
				sqlAppender.append( "lower(" );
			}
			sqlAppender.append( "cast(" );
		}
		sqlAppender.append( "t.val" );
		if ( needsCast ) {
			sqlAppender.append( " as longvarchar)" );
			if ( ddlTypeCode == SqlTypes.BOOLEAN ) {
				sqlAppender.append( ')' );
			}
		}
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
