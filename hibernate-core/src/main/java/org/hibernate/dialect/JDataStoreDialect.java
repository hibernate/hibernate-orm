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
