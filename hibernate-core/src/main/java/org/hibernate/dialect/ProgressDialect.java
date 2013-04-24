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
