/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect.function.array;

import java.util.List;

import org.hibernate.dialect.function.array.ArrayConstructorFunction;
import org.hibernate.dialect.function.array.DdlTypeHelper;
import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.type.BasicPluralType;
import org.hibernate.type.BasicType;

/**
 * Special array constructor function that also applies a cast to the array literal,
 * based on the inferred result type. GaussDB needs this,
 * because by default it assumes a {@code text[]}, which is not compatible with {@code varchar[]}.
 *
 * @author liubao
 *
 * Notes: Original code of this class is based on PostgreSQLArrayConstructorFunction.
 */
public class GaussDBArrayConstructorFunction extends ArrayConstructorFunction {

	public GaussDBArrayConstructorFunction(boolean list) {
		super( list, true );
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		String arrayTypeName = null;
		if ( returnType instanceof BasicPluralType<?, ?> pluralType ) {
			if ( needsArrayCasting( pluralType.getElementType() ) ) {
				arrayTypeName = DdlTypeHelper.getCastTypeName(
						returnType,
						walker.getSessionFactory().getTypeConfiguration()
				);
				sqlAppender.append( "cast(" );
			}
		}
		super.render( sqlAppender, sqlAstArguments, returnType, walker );
		if ( arrayTypeName != null ) {
			sqlAppender.appendSql( " as " );
			sqlAppender.appendSql( arrayTypeName );
			sqlAppender.appendSql( ')' );
		}
	}

	private static boolean needsArrayCasting(BasicType<?> elementType) {
		// GaussDB doesn't do implicit conversion between text[] and varchar[], so we need casting
		return elementType.getJdbcType().isString();
	}
}
