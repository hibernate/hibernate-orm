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

import org.hibernate.dialect.function.NoArgsSqmFunctionTemplate;
import org.hibernate.dialect.function.NvlFunction;
import org.hibernate.query.sqm.produce.function.spi.NamedSqmFunctionTemplate;
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

		registerFunction( "ltrim", new NamedSqmFunctionTemplate( "ltrim" ) );
		registerFunction( "rtrim", new NamedSqmFunctionTemplate( "rtrim" ) );
		registerFunction( "soundex", new NamedSqmFunctionTemplate( "soundex" ) );
		registerFunction( "sysdate", new NoArgsSqmFunctionTemplate( "sysdate", StandardSpiBasicTypes.DATE, false ) );
		registerFunction( "rowid", new NoArgsSqmFunctionTemplate( "rowid", StandardSpiBasicTypes.LONG, false ) );
		registerFunction( "rownum", new NoArgsSqmFunctionTemplate( "rownum", StandardSpiBasicTypes.LONG, false ) );
		registerFunction( "instr", new NamedSqmFunctionTemplate( "instr", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "lpad", new NamedSqmFunctionTemplate( "lpad", StandardSpiBasicTypes.STRING ) );
		registerFunction( "replace", new NamedSqmFunctionTemplate( "replace", StandardSpiBasicTypes.STRING ) );
		registerFunction( "rpad", new NamedSqmFunctionTemplate( "rpad", StandardSpiBasicTypes.STRING ) );
		registerFunction( "translate", new NamedSqmFunctionTemplate( "translate", StandardSpiBasicTypes.STRING ) );
		registerFunction( "substring", new NamedSqmFunctionTemplate( "substr", StandardSpiBasicTypes.STRING ) );
		registerFunction( "coalesce", new NvlFunction() );
		registerFunction( "atan2", new NamedSqmFunctionTemplate( "atan2", StandardSpiBasicTypes.FLOAT ) );
		registerFunction( "mod", new NamedSqmFunctionTemplate( "mod", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "nvl", new NamedSqmFunctionTemplate( "nvl" ) );
		registerFunction( "nvl2", new NamedSqmFunctionTemplate( "nvl2" ) );
		registerFunction( "power", new NamedSqmFunctionTemplate( "power", StandardSpiBasicTypes.FLOAT ) );
		registerFunction( "add_months", new NamedSqmFunctionTemplate( "add_months", StandardSpiBasicTypes.DATE ) );
		registerFunction( "months_between", new NamedSqmFunctionTemplate( "months_between", StandardSpiBasicTypes.FLOAT ) );
		registerFunction( "next_day", new NamedSqmFunctionTemplate( "next_day", StandardSpiBasicTypes.DATE ) );
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
