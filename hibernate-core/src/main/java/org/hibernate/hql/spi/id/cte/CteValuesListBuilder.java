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
 * Builds the CTE with VALUES list clause that wraps the identifiers to be updated/deleted.
 *
 * @author Evandro Pires da Silva
 * @author Vlad Mihalcea
 */
public class CteValuesListBuilder {

	private final String tableName;

	private final String[] columns;

	private final List<Object[]> ids;

	private String cteStatement;

	public CteValuesListBuilder(
			String tableName,
			String[] columns,
			List<Object[]> ids) {
		this.tableName = tableName;
		this.columns = columns;
		this.ids = ids;

		this.cteStatement = buildStatement();
	}

	public List<Object[]> getIds() {
		return ids;
	}

	public String toStatement(String statement) {
		return cteStatement + statement;
	}

	private String buildStatement() {
		String columnNames = String.join(",", columns);

		String singleIdValuesParam = '(' + String.join( ",", Collections.nCopies( columns.length, "?")) + ')';
		String parameters = String.join(",", Collections.nCopies(ids.size(), singleIdValuesParam));

		return new StringBuilder()
				.append( "with " )
				.append( tableName )
				.append( " (" )
				.append( columnNames )
				.append( " ) as ( select " )
				.append( columnNames )
				.append( " from ( values  " )
				.append( parameters )
				.append( ") as HT " )
				.append( "(" )
				.append( columnNames )
				.append( ") ) " )
				.toString();
	}
}
