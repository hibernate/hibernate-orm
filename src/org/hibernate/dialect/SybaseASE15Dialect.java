//$Id: SybaseDialect.java 15760 2009-01-09 09:52:13Z gbadner $
package org.hibernate.dialect;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Map;
import java.util.Iterator;

import org.hibernate.Hibernate;
import org.hibernate.LockMode;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.function.AnsiTrimEmulationFunction;
import org.hibernate.dialect.function.CharIndexFunction;
import org.hibernate.dialect.function.NoArgSQLFunction;
import org.hibernate.dialect.function.SQLFunctionTemplate;
import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.dialect.function.VarArgsSQLFunction;

/**
 * An SQL dialect compatible with Sybase and MS SQL Server.
 * @author Gavin King
 */

public class SybaseASE15Dialect extends AbstractTransactSQLDialect {
	public SybaseASE15Dialect() {
		super();
		registerFunction( "second", new SQLFunctionTemplate(Hibernate.INTEGER, "datepart(second, ?1)") );
		registerFunction( "minute", new SQLFunctionTemplate(Hibernate.INTEGER, "datepart(minute, ?1)") );
		registerFunction( "hour", new SQLFunctionTemplate(Hibernate.INTEGER, "datepart(hour, ?1)") );
		registerFunction( "extract", new SQLFunctionTemplate( Hibernate.INTEGER, "datepart(?1, ?3)" ) );
		registerFunction( "mod", new SQLFunctionTemplate( Hibernate.INTEGER, "?1 % ?2" ) );
		registerFunction( "bit_length", new SQLFunctionTemplate( Hibernate.INTEGER, "datalength(?1) * 8" ) );
		registerFunction( "trim", new AnsiTrimEmulationFunction( AnsiTrimEmulationFunction.LTRIM, AnsiTrimEmulationFunction.RTRIM, "str_replace" ) ); 
	}

	// Overridden informational metadata ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public boolean supportsCascadeDelete() {
		return false;
	}

	public boolean supportsExpectedLobUsagePattern() {
		return false;
	}
}
