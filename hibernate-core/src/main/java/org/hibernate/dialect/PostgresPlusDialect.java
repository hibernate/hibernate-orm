/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;
import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.dialect.function.NoArgSQLFunction;
import org.hibernate.dialect.function.NvlFunction;
import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.type.spi.StandardSpiBasicTypes;

/**
 * An SQL dialect for Postgres Plus
 *
 * @author Jim Mlodgenski
 */
@SuppressWarnings("deprecation")
public class PostgresPlusDialect extends PostgreSQLDialect {
	/**
	 * Constructs a PostgresPlusDialect
	 */
	public PostgresPlusDialect() {
		super();

		registerFunction( "ltrim", new StandardSQLFunction( "ltrim" ) );
		registerFunction( "rtrim", new StandardSQLFunction( "rtrim" ) );
		registerFunction( "soundex", new StandardSQLFunction( "soundex" ) );
		registerFunction( "sysdate", new NoArgSQLFunction( "sysdate", StandardSpiBasicTypes.DATE, false ) );
		registerFunction( "rowid", new NoArgSQLFunction( "rowid", StandardSpiBasicTypes.LONG, false ) );
		registerFunction( "rownum", new NoArgSQLFunction( "rownum", StandardSpiBasicTypes.LONG, false ) );
		registerFunction( "instr", new StandardSQLFunction( "instr", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "lpad", new StandardSQLFunction( "lpad", StandardSpiBasicTypes.STRING ) );
		registerFunction( "replace", new StandardSQLFunction( "replace", StandardSpiBasicTypes.STRING ) );
		registerFunction( "rpad", new StandardSQLFunction( "rpad", StandardSpiBasicTypes.STRING ) );
		registerFunction( "translate", new StandardSQLFunction( "translate", StandardSpiBasicTypes.STRING ) );
		registerFunction( "substring", new StandardSQLFunction( "substr", StandardSpiBasicTypes.STRING ) );
		registerFunction( "coalesce", new NvlFunction() );
		registerFunction( "atan2", new StandardSQLFunction( "atan2", StandardSpiBasicTypes.FLOAT ) );
		registerFunction( "mod", new StandardSQLFunction( "mod", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "nvl", new StandardSQLFunction( "nvl" ) );
		registerFunction( "nvl2", new StandardSQLFunction( "nvl2" ) );
		registerFunction( "power", new StandardSQLFunction( "power", StandardSpiBasicTypes.FLOAT ) );
		registerFunction( "add_months", new StandardSQLFunction( "add_months", StandardSpiBasicTypes.DATE ) );
		registerFunction( "months_between", new StandardSQLFunction( "months_between", StandardSpiBasicTypes.FLOAT ) );
		registerFunction( "next_day", new StandardSQLFunction( "next_day", StandardSpiBasicTypes.DATE ) );
	}

	@Override
	public String getCurrentTimestampSelectString() {
		return "select sysdate";
	}

	@Override
	public String getCurrentTimestampSQLFunctionName() {
		return "sysdate";
	}

	@Override
	public int registerResultSetOutParameter(CallableStatement statement, int col) throws SQLException {
		statement.registerOutParameter( col, Types.REF );
		col++;
		return col;
	}

	@Override
	public ResultSet getResultSet(CallableStatement ps) throws SQLException {
		ps.execute();
		return (ResultSet) ps.getObject( 1 );
	}

	@Override
	public String getSelectGUIDString() {
		return "select uuid_generate_v1";
	}

}
