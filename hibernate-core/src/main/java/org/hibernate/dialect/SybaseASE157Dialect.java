/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
import java.util.Map;

import org.hibernate.dialect.function.AnsiTrimEmulationFunction;
import org.hibernate.dialect.function.NoArgSQLFunction;
import org.hibernate.dialect.function.SQLFunctionTemplate;
import org.hibernate.dialect.function.VarArgsSQLFunction;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.sql.ForUpdateFragment;

/**
 * An SQL dialect targeting Sybase Adaptive Server Enterprise (ASE) 15.7 and higher.
 * <p/>
 *
 * @author Junyan Ren
 */
public class SybaseASE157Dialect extends SybaseASE15Dialect {

	public SybaseASE157Dialect() {
		super();

		registerFunction( "create_locator", new SQLFunctionTemplate( StandardBasicTypes.BINARY, "create_locator(?1, ?2)" ) );
		registerFunction( "locator_literal", new SQLFunctionTemplate( StandardBasicTypes.BINARY, "locator_literal(?1, ?2)" ) );
		registerFunction( "locator_valid", new SQLFunctionTemplate( StandardBasicTypes.BOOLEAN, "locator_valid(?1)" ) );
		registerFunction( "return_lob", new SQLFunctionTemplate( StandardBasicTypes.BINARY, "return_lob(?1, ?2)" ) );
		registerFunction( "setdata", new SQLFunctionTemplate( StandardBasicTypes.BOOLEAN, "setdata(?1, ?2, ?3)" ) );
		registerFunction( "charindex", new SQLFunctionTemplate( StandardBasicTypes.INTEGER, "charindex(?1, ?2, ?3)" ) );
	}

	// support Lob Locator
	public boolean supportsExpectedLobUsagePattern() {
		return true;
	}
	
	public boolean supportsLobValueChangePropogation() {
		return false;
	}

	// support 'select ... for update [of columns]'
	public boolean forUpdateOfColumns() {
		return true;
	}
	
	public String getForUpdateString() {
		return " for update";
	}
	
	public String getForUpdateString(String aliases) {
		return getForUpdateString() + " of " + aliases;
	}

	public String appendLockHint(LockMode mode, String tableName) {
		return tableName;
	}

	public String applyLocksToSql(String sql, LockOptions aliasedLockOptions, Map keyColumnNames) {
		return sql + new ForUpdateFragment( this, aliasedLockOptions, keyColumnNames ).toFragmentString();
	}
	
}
