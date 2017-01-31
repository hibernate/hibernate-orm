/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.spi.id.cte;

import java.util.Collections;
import java.util.List;

/**
 * @author Evandro Pires da Silva
 * @author Vlad Mihalcea
 */
public class ValuesListCteBuilder {

	private final String tableName;

	private final String[] columns;

	private final List<Object[]> ids;

	private StringBuilder statementBuilder;

	public ValuesListCteBuilder(
			String tableName,
			String[] columns,
			List<Object[]> ids) {
		this.tableName = tableName;
		this.columns = columns;
		this.ids = ids;

		this.statementBuilder = buildStatement();
	}

	public String toStatement(String statement) {
		return statementBuilder + statement;
	}

	public List<Object[]> getIds() {
		return ids;
	}

	private StringBuilder buildStatement() {
		StringBuilder buffer = new StringBuilder();

		String columnNames = String.join(",", columns);

		buffer
				.append( "with " )
				.append( tableName )
				.append( " (" )
				.append( columnNames );

		StringBuilder parameters = new StringBuilder();
		for ( Object[] result : this.ids ) {
			if ( parameters.length() > 0 ) {
				parameters.append( "," );
			}
			parameters.append( "(" );
			parameters.append(
				String.join( ",", Collections.nCopies( result.length, "?"))
			);
			parameters.append( ")" );
		}

		buffer
				.append( " ) as ( select " )
				.append( columnNames )
				.append( " from ( values  " )
				.append( parameters )
				.append( ") as HT " )
				.append( "(" )
				.append( columnNames )
				.append( ") ) " );

		return buffer;
	}
}
