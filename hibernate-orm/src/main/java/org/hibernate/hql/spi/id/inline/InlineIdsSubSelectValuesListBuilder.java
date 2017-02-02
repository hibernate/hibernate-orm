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
 * Builds the where SELECT FROM VALUES clause that wraps the identifiers to be updated/deleted.
 *
 * @author Vlad Mihalcea
 */
public class InlineIdsSubSelectValuesListBuilder extends IdsClauseBuilder {

	public InlineIdsSubSelectValuesListBuilder(
			Dialect dialect, Type identifierType, TypeResolver typeResolver, String[] columns, List<Object[]> ids) {
		super( dialect, identifierType, typeResolver, columns, ids );
	}

	@Override
	public String toStatement() {
		StringBuilder buffer = new StringBuilder();

		String columnNames = String.join( ",", (CharSequence[]) getColumns() );

		buffer.append( "select " ).append( columnNames ).append(
				" from ( values " );

		for ( int i = 0; i < getIds().size(); i++ ) {
			Object[] idTokens = getIds().get( i );
			if ( i > 0 ) {
				buffer.append( "," );
			}
			buffer.append( "(" );
			buffer.append( quoteIdentifier( idTokens ) );
			buffer.append( ")" );
		}
		buffer.append( ") as HT (" ).append( columnNames ).append( ") " );

		return buffer.toString();
	}
}
