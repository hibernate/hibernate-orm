//$Id: ProgressDialect.java 4609 2004-09-27 03:17:46Z oneovthafew $
// contributed by Phillip Baird
package org.hibernate.dialect;

import java.sql.Types;

/**
 * An SQL dialect compatible with Progress 9.1C<br>
 *<br>
 * Connection Parameters required:
 *<ul>
 * <li>hibernate.dialect org.hibernate.sql.ProgressDialect
 * <li>hibernate.driver com.progress.sql.jdbc.JdbcProgressDriver
 * <li>hibernate.url jdbc:JdbcProgress:T:host:port:dbname;WorkArounds=536870912
 * <li>hibernate.username username
 * <li>hibernate.password password
 *</ul>
 * The WorkArounds parameter in the URL is required to avoid an error
 * in the Progress 9.1C JDBC driver related to PreparedStatements.
 * @author Phillip Baird
 *
 */
public class ProgressDialect extends Dialect {
	public ProgressDialect() {
		super();
		registerColumnType( Types.BIT, "bit" );
		registerColumnType( Types.BIGINT, "numeric" );
		registerColumnType( Types.SMALLINT, "smallint" );
		registerColumnType( Types.TINYINT, "tinyint" );
		registerColumnType( Types.INTEGER, "integer" );
		registerColumnType( Types.CHAR, "character(1)" );
		registerColumnType( Types.VARCHAR, "varchar($l)" );
		registerColumnType( Types.FLOAT, "real" );
		registerColumnType( Types.DOUBLE, "double precision" );
		registerColumnType( Types.DATE, "date" );
		registerColumnType( Types.TIME, "time" );
		registerColumnType( Types.TIMESTAMP, "timestamp" );
		registerColumnType( Types.VARBINARY, "varbinary($l)" );
		registerColumnType( Types.NUMERIC, "numeric($p,$s)" );
	}

	public boolean hasAlterTable(){
		return false;
	}

	public String getAddColumnString() {
		return "add column";
	}

	public boolean qualifyIndexName() {
		return false;
	}
}







