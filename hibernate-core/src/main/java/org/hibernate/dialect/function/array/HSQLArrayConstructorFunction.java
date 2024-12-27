/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.array;

import java.util.List;

import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.type.BottomType;

public class HSQLArrayConstructorFunction extends ArrayConstructorFunction {

	public HSQLArrayConstructorFunction(boolean list) {
		super( list, true );
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> arguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		final String castTypeName;
		if ( returnType != null && hasOnlyBottomArguments( arguments ) ) {
			castTypeName = DdlTypeHelper.getCastTypeName(
					returnType,
					walker.getSessionFactory().getTypeConfiguration()
			);
			sqlAppender.append( "cast(" );
		}
		else {
			castTypeName = null;
		}
		sqlAppender.append( "array" );
		final int size = arguments.size();
		if ( size == 0 ) {
			sqlAppender.append( '[' );
		}
		else {
			char separator = '[';
			for ( int i = 0; i < size; i++ ) {
				SqlAstNode argument = arguments.get( i );
				sqlAppender.append( separator );
				argument.accept( walker );
				separator = ',';
			}
		}
		sqlAppender.append( ']' );
		if ( castTypeName != null ) {
			sqlAppender.append( " as " );
			sqlAppender.append( castTypeName );
			sqlAppender.append( ')' );
		}
	}

	private boolean hasOnlyBottomArguments(List<? extends SqlAstNode> arguments) {
		for ( int i = 0; i < arguments.size(); i++ ) {
			final Expression argument = (Expression) arguments.get( i );
			if ( !( argument.getExpressionType().getSingleJdbcMapping() instanceof BottomType ) ) {
				return false;
			}
		}
		return !arguments.isEmpty();
	}
}
