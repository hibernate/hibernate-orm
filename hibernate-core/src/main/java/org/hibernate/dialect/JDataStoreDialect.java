/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import java.sql.Types;

import org.hibernate.cfg.Environment;
import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.dialect.identity.JDataStoreIdentityColumnSupport;

/**
 * A Dialect for JDataStore.
 * 
 * @author Vishy Kasar
 */
public class JDataStoreDialect extends Dialect {
	/**
	 * Creates new JDataStoreDialect
	 */
	public JDataStoreDialect() {
		super();

		registerColumnType( Types.BIT, 1, "tinyint" );
		registerColumnType( Types.BIT, "tinyint" );
		registerColumnType( Types.BOOLEAN, "tinyint" );

		registerColumnType( Types.DOUBLE, "double" );

		registerColumnType( Types.VARBINARY, "varbinary($l)" );

		registerColumnType( Types.BLOB, "varbinary" );
		registerColumnType( Types.CLOB, "varchar" );

		getDefaultProperties().setProperty( Environment.STATEMENT_BATCH_SIZE, DEFAULT_BATCH_SIZE );
	}

	@Override
	public String getAddColumnString() {
		return "add";
	}

	@Override
	public boolean dropConstraints() {
		return false;
	}

	@Override
	public String getCascadeConstraintsString() {
		return " cascade";
	}

	@Override
	public String getNoColumnsInsertString() {
		return "default values";
	}

	@Override
	public boolean supportsColumnCheck() {
		return false;
	}

	@Override
	public boolean supportsTableCheck() {
		return false;
	}

	@Override
	public IdentityColumnSupport getIdentityColumnSupport() {
		return new JDataStoreIdentityColumnSupport();
	}

}
