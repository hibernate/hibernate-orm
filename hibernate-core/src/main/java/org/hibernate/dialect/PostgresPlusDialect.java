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
import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.dialect.function.NoArgSQLFunction;
import org.hibernate.dialect.function.NvlFunction;
import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.type.StandardBasicTypes;

/**
 * An SQL dialect for Postgres Plus
 *
 * @author Jim Mlodgenski
 */
public class PostgresPlusDialect extends PostgreSQLDialect {

	public PostgresPlusDialect() {
		super();

		registerFunction( "ltrim", new StandardSQLFunction( "ltrim" ) );
		registerFunction( "rtrim", new StandardSQLFunction( "rtrim" ) );
		registerFunction( "soundex", new StandardSQLFunction( "soundex" ) );
		registerFunction( "sysdate", new NoArgSQLFunction( "sysdate", StandardBasicTypes.DATE, false ) );
		registerFunction( "rowid", new NoArgSQLFunction( "rowid", StandardBasicTypes.LONG, false ) );
		registerFunction( "rownum", new NoArgSQLFunction( "rownum", StandardBasicTypes.LONG, false ) );
		registerFunction( "instr", new StandardSQLFunction( "instr", StandardBasicTypes.INTEGER ) );
		registerFunction( "lpad", new StandardSQLFunction( "lpad", StandardBasicTypes.STRING ) );
		registerFunction( "replace", new StandardSQLFunction( "replace", StandardBasicTypes.STRING ) );
		registerFunction( "rpad", new StandardSQLFunction( "rpad", StandardBasicTypes.STRING ) );
		registerFunction( "translate", new StandardSQLFunction( "translate", StandardBasicTypes.STRING ) );
		registerFunction( "substring", new StandardSQLFunction( "substr", StandardBasicTypes.STRING ) );
		registerFunction( "coalesce", new NvlFunction() );
		registerFunction( "atan2", new StandardSQLFunction( "atan2", StandardBasicTypes.FLOAT ) );
		registerFunction( "mod", new StandardSQLFunction( "mod", StandardBasicTypes.INTEGER ) );
		registerFunction( "nvl", new StandardSQLFunction( "nvl" ) );
		registerFunction( "nvl2", new StandardSQLFunction( "nvl2" ) );
		registerFunction( "power", new StandardSQLFunction( "power", StandardBasicTypes.FLOAT ) );
		registerFunction( "add_months", new StandardSQLFunction( "add_months", StandardBasicTypes.DATE ) );
		registerFunction( "months_between", new StandardSQLFunction( "months_between", StandardBasicTypes.FLOAT ) );
		registerFunction( "next_day", new StandardSQLFunction( "next_day", StandardBasicTypes.DATE ) );
	}

	public String getCurrentTimestampSelectString() {
		return "select sysdate";
	}

	public String getCurrentTimestampSQLFunctionName() {
		return "sysdate";
	}

	public int registerResultSetOutParameter(CallableStatement statement, int col) throws SQLException {
		statement.registerOutParameter( col, Types.REF );
		col++;
		return col;
	}

	public ResultSet getResultSet(CallableStatement ps) throws SQLException {
		ps.execute();
		return ( ResultSet ) ps.getObject( 1 );
	}

	public String getSelectGUIDString() {
		return "select uuid_generate_v1";
	}

}
