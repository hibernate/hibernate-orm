/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import java.sql.Types;

import org.hibernate.NullPrecedence;
import org.hibernate.dialect.function.NoArgSQLFunction;
import org.hibernate.type.StandardBasicTypes;

/**
 * A dialect for Microsoft SQL Server 2008 with JDBC Driver 3.0 and above
 *
 * @author Gavin King
 */
public class SQLServer2008Dialect extends SQLServer2005Dialect {

	private static final int NVARCHAR_MAX_LENGTH = 4000;
	/**
	 * Constructs a SQLServer2008Dialect
	 */
	public SQLServer2008Dialect() {
		registerColumnType( Types.DATE, "date" );
		registerColumnType( Types.TIME, "time" );
		registerColumnType( Types.TIMESTAMP, "datetime2" );
		registerColumnType( Types.NVARCHAR, NVARCHAR_MAX_LENGTH, "nvarchar($l)" );
		registerColumnType( Types.NVARCHAR, "nvarchar(MAX)" );

		registerFunction(
				"current_timestamp", new NoArgSQLFunction( "current_timestamp", StandardBasicTypes.TIMESTAMP, false )
		);
	}
	
	@Override
	public String renderOrderByElement(String expression, String collation, String order, NullPrecedence nulls) {
		final StringBuilder orderByElement = new StringBuilder();

		if ( nulls != null && !NullPrecedence.NONE.equals( nulls ) ) {
			// Workaround for NULLS FIRST / LAST support.
			orderByElement.append( "case when " ).append( expression ).append( " is null then " );
			if ( NullPrecedence.FIRST.equals( nulls ) ) {
				orderByElement.append( "0 else 1" );
			}
			else {
				orderByElement.append( "1 else 0" );
			}
			orderByElement.append( " end, " );
		}

		// Nulls precedence has already been handled so passing NONE value.
		orderByElement.append( super.renderOrderByElement( expression, collation, order, NullPrecedence.NONE ) );

		return orderByElement.toString();
	}
}
