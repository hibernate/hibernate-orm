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

	public JDataStoreDialect() {
		super();
		//only surviving documentation known to Google:
		//https://web.mit.edu/jbuilder_v2005/distrib/share/doc/jds_devgd.pdf

		//'bit' is a synonym for 'boolean'
		registerColumnType( Types.BIT, "tinyint" );

		//no 'blob' nor 'clob', at least not in 7
		registerColumnType( Types.BLOB, "varbinary" );
		registerColumnType( Types.CLOB, "varchar" );

		//no precision, at least not in 7
		registerColumnType( Types.TIMESTAMP, "timestamp" );
		registerColumnType( Types.TIMESTAMP_WITH_TIMEZONE, "timestamp" );

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
