/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.dialect;

import java.sql.Types;

import org.hibernate.dialect.function.NoArgSQLFunction;
import org.hibernate.type.StandardBasicTypes;

/**
 * A dialect for Microsoft SQL Server 2008 with JDBC Driver 3.0 and above
 * 
 * @author Gavin King
 */
public class SQLServer2008Dialect extends SQLServerDialect {
	public SQLServer2008Dialect() {
		registerColumnType(Types.DATE, "date");
		registerColumnType(Types.TIME, "time");
		registerColumnType(Types.TIMESTAMP, "datetime2");

		registerFunction("current_timestamp", new NoArgSQLFunction("current_timestamp", StandardBasicTypes.TIMESTAMP, false));
	}

	/**
	 * Add a LIMIT clause to the given SQL SELECT (HHH-2655: ROW_NUMBER for Paging)
	 * 
	 * The LIMIT SQL will look like:
	 * 
	 * <pre>
	 * WITH query AS (SELECT ROW_NUMBER() OVER (ORDER BY orderby) as __hibernate_row_nr__, original_query_without_orderby) 
	 * SELECT * FROM query WHERE __hibernate_row_nr__ BEETWIN offset AND offset + last 
	 * --ORDER BY __hibernate_row_nr__
	 * </pre>
	 * 
	 * I don't think that the last order by clause is mandatory
	 * 
	 * @param querySqlString
	 *            The SQL statement to base the limit query off of.
	 * @param offset
	 *            Offset of the first row to be returned by the query (zero-based)
	 * @param limit
	 *            Maximum number of rows to be returned by the query
	 * @return A new SQL statement with the LIMIT clause applied.
	 */
	@Override
	public String getLimitString(String querySqlString, int offset, int limit) {
		if (offset == 0) return super.getLimitString(querySqlString, offset, limit);

		StringBuilder sb = new StringBuilder(querySqlString.trim());

		String querySqlLowered = querySqlString.trim().toLowerCase();
		int orderByIndex = querySqlLowered.toLowerCase().indexOf("order by");
		String orderby = orderByIndex > 0 ? querySqlString.substring(orderByIndex) : "ORDER BY CURRENT_TIMESTAMP";

		// Delete the order by clause at the end of the query
		if (orderByIndex > 0) sb.delete(orderByIndex, orderByIndex + orderby.length());

		// Find the end of the select statement
		int selectIndex = querySqlLowered.trim().startsWith("select distinct") ? 15 : 6;

		// Isert after the select statement the row_number() function:
		sb.insert(selectIndex, " ROW_NUMBER() OVER (" + orderby + ") as __hibernate_row_nr__,");

		// Wrap the query within a with statement:
		sb.insert(0, "WITH query AS (").append(") SELECT * FROM query ");
		sb.append("WHERE __hibernate_row_nr__ ");
		
		// The row_number() function is not zero based and so we must increment the offset and limit by one
		if (offset > 0) sb.append("BETWEEN ").append(offset + 1).append(" AND ").append(limit + 1);
		else sb.append(" <= ").append(limit);

		// As mentioned before I don't think that we really need this last order by clause
		// sb.append(" ORDER BY __hibernate_row_nr__");
		return sb.toString();
	}

	@Override
	public boolean supportsLimit() {
		return true;
	}

	@Override
	public boolean supportsLimitOffset() {
		return true;
	}

	@Override
	public boolean bindLimitParametersFirst() {
		return false;
	}
}
