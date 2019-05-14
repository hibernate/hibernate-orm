/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
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
	/**
	 * Constructs a ProgressDialect
	 */
	public ProgressDialect() {
		super();

		registerColumnType( Types.BOOLEAN, "bit" );

		//'float' is a synonym for 'double precision'
		//TODO: return single-precision 'real' when
		//      precision <= 24
		//note: unnecessary in Progress 10
		registerColumnType( Types.FLOAT, "float" );
		registerColumnType( Types.DOUBLE, "float" );

		//note: unnecessary in Progress 10
		registerColumnType( Types.BIGINT, "numeric(19,0)" );

		//no precision
		registerColumnType( Types.TIMESTAMP, "timestamp" );
		registerColumnType( Types.TIMESTAMP_WITH_TIMEZONE, "timestamp with time zone" );
	}

	@Override
	public boolean hasAlterTable(){
		return false;
	}

	@Override
	public String getAddColumnString() {
		return "add column";
	}

	@Override
	public boolean qualifyIndexName() {
		return false;
	}
}
