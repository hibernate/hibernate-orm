/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
public class SQLServer2008Dialect extends SQLServer2005Dialect {
	/**
	 * Constructs a SQLServer2008Dialect
	 */
	public SQLServer2008Dialect() {
		registerColumnType( Types.DATE, "date" );
		registerColumnType( Types.TIME, "time" );
		registerColumnType( Types.TIMESTAMP, "datetime2" );

		registerFunction(
				"current_timestamp", new NoArgSQLFunction( "current_timestamp", StandardBasicTypes.TIMESTAMP, false )
		);
	}
}
