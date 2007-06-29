package org.hibernate.dialect;

import java.sql.Types;

/**
 * An SQL dialect for MySQL 5.x specific features.
 *
 * @author Steve Ebersole
 */
public class MySQL5Dialect extends MySQLDialect {
	protected void registerVarcharTypes() {
		registerColumnType( Types.VARCHAR, "longtext" );
		registerColumnType( Types.VARCHAR, 16777215, "mediumtext" );
		registerColumnType( Types.VARCHAR, 65535, "varchar($l)" );
	}
}
