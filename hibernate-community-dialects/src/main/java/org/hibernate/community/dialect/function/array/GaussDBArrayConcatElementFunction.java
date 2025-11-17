/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect.function.array;

import java.util.List;

import org.hibernate.dialect.function.array.ArrayConcatElementFunction;
import org.hibernate.dialect.function.array.DdlTypeHelper;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.metamodel.mapping.SqlTypedMapping;
import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.sql.ast.SqlAstNodeRenderingMode;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.Literal;
import org.hibernate.type.BasicPluralType;

/**
 * GaussDB variant of the function to properly return {@code null} when the array argument is null.
 *
 * @author liubao
 *
 * Notes: Original code of this class is based on PostgreSQLArrayConcatElementFunction.
 */
public class GaussDBArrayConcatElementFunction extends ArrayConcatElementFunction {

	public GaussDBArrayConcatElementFunction(boolean prepend) {
		super( "", "||", "", prepend );
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		final Expression firstArgument = (Expression) sqlAstArguments.get( 0 );
		final Expression secondArgument = (Expression) sqlAstArguments.get( 1 );
		final Expression arrayArgument;
		final Expression elementArgument;
		if ( prepend ) {
			elementArgument = firstArgument;
			arrayArgument = secondArgument;
		}
		else {
			arrayArgument = firstArgument;
			elementArgument = secondArgument;
		}
		final String elementCastType;
		if ( needsElementCasting( elementArgument ) ) {
			final JdbcMappingContainer arrayType = arrayArgument.getExpressionType();
			final Size size = arrayType instanceof SqlTypedMapping ? ( (SqlTypedMapping) arrayType ).toSize() : null;
			elementCastType = DdlTypeHelper.getCastTypeName(
					( (BasicPluralType<?, ?>) returnType ).getElementType(),
					size,
					walker.getSessionFactory().getTypeConfiguration()
			);
		}
		else {
			elementCastType = null;
		}
		sqlAppender.append( "case when " );
		walker.render( arrayArgument, SqlAstNodeRenderingMode.DEFAULT );
		sqlAppender.append( " is not null then " );
		if ( prepend && elementCastType != null) {
			sqlAppender.append( "cast(" );
			walker.render( firstArgument, SqlAstNodeRenderingMode.DEFAULT );
			sqlAppender.append( " as " );
			sqlAppender.append( elementCastType );
			sqlAppender.append( ')' );
		}
		else {
			walker.render( firstArgument, SqlAstNodeRenderingMode.DEFAULT );
		}
		sqlAppender.append( "||" );
		if ( !prepend && elementCastType != null) {
			sqlAppender.append( "cast(" );
			walker.render( secondArgument, SqlAstNodeRenderingMode.DEFAULT );
			sqlAppender.append( " as " );
			sqlAppender.append( elementCastType );
			sqlAppender.append( ')' );
		}
		else {
			walker.render( secondArgument, SqlAstNodeRenderingMode.DEFAULT );
		}
		sqlAppender.append( " end" );
	}

	private static boolean needsElementCasting(Expression elementExpression) {
		// GaussDB needs casting of null and string literal expressions
		return elementExpression instanceof Literal && (
				elementExpression.getExpressionType().getSingleJdbcMapping().getJdbcType().isString()
						|| ( (Literal) elementExpression ).getLiteralValue() == null
		);
	}
}
