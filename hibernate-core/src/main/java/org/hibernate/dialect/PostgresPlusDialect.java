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

import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.dialect.function.NvlFunctionTemplate;
import org.hibernate.query.sqm.produce.function.SqmFunctionRegistry;
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
	}

	@Override
	public void initializeFunctionRegistry(SqmFunctionRegistry registry) {
		super.initializeFunctionRegistry( registry );

		CommonFunctionFactory.soundex( registry );
		registry.registerNoArgs( "sysdate", StandardSpiBasicTypes.DATE );
		registry.registerNoArgs( "rowid", StandardSpiBasicTypes.LONG );
		registry.registerNoArgs( "rownum", StandardSpiBasicTypes.LONG );
		registry.namedTemplateBuilder( "instr" )
				.setInvariantType( StandardSpiBasicTypes.INTEGER )
				.setArgumentCountBetween( 2, 4 )
				.register();
		registry.register( "coalesce", new NvlFunctionTemplate() );
		registry.namedTemplateBuilder( "nvl" )
				.setExactArgumentCount( 2 )
				.register();
		registry.namedTemplateBuilder( "nvl2" )
				.setExactArgumentCount( 3 )
				.register();

		// Multi-param date dialect functions...
		registry.namedTemplateBuilder( "add_months" )
				.setInvariantType( StandardSpiBasicTypes.DATE )
				.setExactArgumentCount( 2 )
				.register();
		registry.namedTemplateBuilder( "months_between" )
				.setInvariantType( StandardSpiBasicTypes.FLOAT )
				.setExactArgumentCount( 2 )
				.register();
		registry.namedTemplateBuilder( "next_day" )
				.setInvariantType( StandardSpiBasicTypes.DATE )
				.setExactArgumentCount( 2 )
				.register();
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
