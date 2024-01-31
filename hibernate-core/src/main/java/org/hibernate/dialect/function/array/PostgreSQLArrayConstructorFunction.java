/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function.array;

import java.util.List;

import org.hibernate.metamodel.model.domain.DomainType;
import org.hibernate.query.ReturnableType;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.type.BasicPluralType;
import org.hibernate.type.BasicType;

/**
 * Special array constructor function that also applies a cast to the array literal,
 * based on the inferred result type. PostgreSQL needs this,
 * because by default it assumes a {@code text[]}, which is not compatible with {@code varchar[]}.
 */
public class PostgreSQLArrayConstructorFunction extends ArrayConstructorFunction {

	public PostgreSQLArrayConstructorFunction(boolean list) {
		super( list, true );
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		String arrayTypeName = null;
		if ( returnType != null ) {
			final DomainType<?> type = returnType.getSqmType();
			if ( type instanceof BasicPluralType<?, ?> ) {
				final BasicPluralType<?, ?> pluralType = (BasicPluralType<?, ?>) type;
				if ( needsArrayCasting( pluralType.getElementType() ) ) {
					arrayTypeName = DdlTypeHelper.getCastTypeName(
							returnType,
							walker.getSessionFactory().getTypeConfiguration()
					);
					sqlAppender.append( "cast(" );
				}
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
		// PostgreSQL doesn't do implicit conversion between text[] and varchar[], so we need casting
		return elementType.getJdbcType().isString();
	}
}
