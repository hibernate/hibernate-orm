// $Id: JDataStoreDialect.java 7075 2005-06-08 07:06:50Z oneovthafew $
package org.hibernate.dialect;

import java.sql.Types;

import org.hibernate.cfg.Environment;

/**
 * A <tt>Dialect</tt> for JDataStore.
 * 
 * @author Vishy Kasar
 */
public class JDataStoreDialect extends Dialect {

	/**
	 * Creates new JDataStoreDialect
	 */
	public JDataStoreDialect() {
		super();

		registerColumnType( Types.BIT, "tinyint" );
		registerColumnType( Types.BIGINT, "bigint" );
		registerColumnType( Types.SMALLINT, "smallint" );
		registerColumnType( Types.TINYINT, "tinyint" );
		registerColumnType( Types.INTEGER, "integer" );
		registerColumnType( Types.CHAR, "char(1)" );
		registerColumnType( Types.VARCHAR, "varchar($l)" );
		registerColumnType( Types.FLOAT, "float" );
		registerColumnType( Types.DOUBLE, "double" );
		registerColumnType( Types.DATE, "date" );
		registerColumnType( Types.TIME, "time" );
		registerColumnType( Types.TIMESTAMP, "timestamp" );
		registerColumnType( Types.VARBINARY, "varbinary($l)" );
		registerColumnType( Types.NUMERIC, "numeric($p, $s)" );

		registerColumnType( Types.BLOB, "varbinary" );
		registerColumnType( Types.CLOB, "varchar" );

		getDefaultProperties().setProperty( Environment.STATEMENT_BATCH_SIZE, DEFAULT_BATCH_SIZE );
	}

	public String getAddColumnString() {
		return "add";
	}

	public boolean dropConstraints() {
		return false;
	}

	public String getCascadeConstraintsString() {
		return " cascade";
	}

	public boolean supportsIdentityColumns() {
		return true;
	}

	public String getIdentitySelectString() {
		return null; // NOT_SUPPORTED_SHOULD_USE_JDBC3_PreparedStatement.getGeneratedKeys_method
	}

	public String getIdentityColumnString() {
		return "autoincrement";
	}

	public String getNoColumnsInsertString() {
		return "default values";
	}

	public boolean supportsColumnCheck() {
		return false;
	}

	public boolean supportsTableCheck() {
		return false;
	}

}
