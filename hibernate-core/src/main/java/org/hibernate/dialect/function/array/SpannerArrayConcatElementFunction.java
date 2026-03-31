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
import org.hibernate.sql.ast.tree.expression.Literal;
import org.hibernate.type.BasicPluralType;

/**
 * Uses typed array literals for {@code null} arrays like {@code ARRAY<type>[null]}, to resolve ambiguity.
 */
public class SpannerArrayConcatElementFunction extends ArrayConcatElementFunction {

	public SpannerArrayConcatElementFunction(boolean prepend) {
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
		final Expression elementArgument = prepend ? firstArgument : secondArgument;

		if ( needsTypedLiteral( elementArgument ) ) {
			String typeName = null;
			if ( returnType instanceof BasicPluralType<?, ?> pluralType ) {
				typeName = DdlTypeHelper.getCastTypeName( pluralType.getElementType(), walker.getSessionFactory().getTypeConfiguration() );
			}

			if ( typeName != null && !typeName.isEmpty() ) {
				final Expression arrayArgument = prepend ? secondArgument : firstArgument;
				if ( prepend ) {
					sqlAppender.append( "ARRAY<" );
					sqlAppender.append( typeName );
					sqlAppender.append( ">[" );
					walker.render( elementArgument, SqlAstNodeRenderingMode.DEFAULT );
					sqlAppender.append( "]||" );
					walker.render( arrayArgument, SqlAstNodeRenderingMode.DEFAULT );
				}
				else {
					walker.render( arrayArgument, SqlAstNodeRenderingMode.DEFAULT );
					sqlAppender.append( "||ARRAY<" );
					sqlAppender.append( typeName );
					sqlAppender.append( ">[" );
					walker.render( elementArgument, SqlAstNodeRenderingMode.DEFAULT );
					sqlAppender.append( "]" );
				}
			}
		}
		else {
			super.render( sqlAppender, sqlAstArguments, returnType, walker );
		}
	}

	private static boolean needsTypedLiteral(Expression elementExpression) {
		if ( elementExpression instanceof Literal literal ) {
			return literal.getLiteralValue() == null;
		}
		return false;
	}
}
