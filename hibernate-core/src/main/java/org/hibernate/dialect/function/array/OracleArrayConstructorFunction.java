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
import org.hibernate.query.SemanticException;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.type.BasicPluralType;

public class OracleArrayConstructorFunction extends ArrayConstructorFunction {

	public OracleArrayConstructorFunction(boolean list) {
		super( list, false );
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		if ( returnType == null ) {
			throw new SemanticException(
					"Oracle array constructor emulation requires knowledge about the return type, but resolved return type could not be determined"
			);
		}
		final DomainType<?> type = returnType.getSqmType();
		if ( !( type instanceof BasicPluralType<?, ?> ) ) {
			throw new SemanticException(
					"Oracle array constructor emulation requires a basic plural return type, but resolved return type was: " + type
			);
		}
		final BasicPluralType<?, ?> pluralType = (BasicPluralType<?, ?>) type;
		final String arrayTypeName = DdlTypeHelper.getCastTypeName(
				pluralType,
				walker.getSessionFactory().getTypeConfiguration()
		);
		sqlAppender.appendSql( arrayTypeName );
		final int size = sqlAstArguments.size();
		if ( size == 0 ) {
			sqlAppender.append( '(' );
		}
		else {
			char separator = '(';
			for ( int i = 0; i < size; i++ ) {
				SqlAstNode argument = sqlAstArguments.get( i );
				sqlAppender.append( separator );
				argument.accept( walker );
				separator = ',';
			}
		}
		sqlAppender.append( ')' );
	}
}
