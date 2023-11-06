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
 * PostgreSQL array_trim emulation, since the function was only introduced in version 14.
 */
public class PostgreSQLArrayTrimEmulation extends AbstractArrayTrimFunction {

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		final SqlAstNode arrayExpression = sqlAstArguments.get( 0 );
		final SqlAstNode elementCountExpression = sqlAstArguments.get( 1 );
		sqlAppender.append( "coalesce((select array_agg(t.val order by t.idx) from unnest(" );
		arrayExpression.accept( walker );
		sqlAppender.append( ") with ordinality t(val,idx) where t.idx<=cardinality(" );
		arrayExpression.accept( walker );
		sqlAppender.append( ")-" );
		elementCountExpression.accept( walker );

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
				}
			}
		}
		if ( arrayTypeName != null ) {
			sqlAppender.append( "),cast(array[] as " );
			sqlAppender.appendSql( arrayTypeName );
			sqlAppender.appendSql( "))" );
		}
		else {
			sqlAppender.append( "),array[])" );
		}
	}

	private static boolean needsArrayCasting(BasicType<?> elementType) {
		// PostgreSQL doesn't do implicit conversion between text[] and varchar[], so we need casting
		return elementType.getJdbcType().isString();
	}
}
