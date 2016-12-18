/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.spi.id.inline;

import java.util.List;

import org.hibernate.dialect.Dialect;
import org.hibernate.type.Type;
import org.hibernate.type.TypeResolver;

/**
 * Builds the where clause using OR expressions for the identifiers to be updated/deleted.
 * This is useful for Dialects that do no support IN clause row value expressions.
 *
 * @author Vlad Mihalcea
 */
public class InlineIdsOrClauseBuilder extends IdsClauseBuilder {

	public InlineIdsOrClauseBuilder(
			Dialect dialect, Type identifierType, TypeResolver typeResolver, String[] columns, List<Object[]> ids) {
		super( dialect, identifierType, typeResolver, columns, ids );
	}

	@Override
	public String toStatement() {
		StringBuilder buffer = new StringBuilder();

		for ( int i = 0; i < getIds().size(); i++ ) {
			Object[] idTokens = getIds().get( i );

			if ( i > 0 ) {
				buffer.append( " or " );
			}

			buffer.append( "(" );

			for ( int j = 0; j < getColumns().length; j++ ) {
				if ( j > 0 ) {
					buffer.append( " and " );
				}
				buffer.append( getColumns()[j] );
				buffer.append( " = " );
				buffer.append( quoteIdentifier( idTokens[j] ) );

			}
			buffer.append( ")" );
		}

		return buffer.toString();
	}
}
