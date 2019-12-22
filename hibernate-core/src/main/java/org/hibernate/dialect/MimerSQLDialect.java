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
import org.hibernate.dialect.identity.MimerSQLIdentityColumnSupport;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.tool.schema.extract.internal.SequenceInformationExtractorMimerSQLDatabaseImpl;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractor;
import org.hibernate.type.StandardBasicTypes;

/**
 * An Hibernate 3 SQL dialect for Mimer SQL. This dialect requires Mimer SQL 9.2.1 or later
 * because of the mappings to NCLOB, BINARY, and BINARY VARYING.
 *
 * @author Fredrik lund <fredrik.alund@mimer.se>
 */
@SuppressWarnings("deprecation")
public class MimerSQLDialect extends Dialect {

	private static final int NATIONAL_CHAR_LENGTH = 2000;
	private static final int BINARY_MAX_LENGTH = 2000;

	/**
	 * Even thoug Mimer SQL supports character and binary columns up to 15 000 in lenght,
	 * this is also the maximum width of the table (exluding LOBs). To avoid breaking the limit all the
	 * time we limit the length of the character columns to CHAR_MAX_LENTH, NATIONAL_CHAR_LENGTH for national
	 * characters, and BINARY_MAX_LENGTH for binary types.
	 */
	public MimerSQLDialect() {
		super();
		registerColumnType( Types.BIT, "ODBC.BIT" );
		registerColumnType( Types.BIGINT, "BIGINT" );
		registerColumnType( Types.SMALLINT, "SMALLINT" );
		registerColumnType( Types.TINYINT, "ODBC.TINYINT" );
		registerColumnType( Types.INTEGER, "INTEGER" );
		registerColumnType( Types.CHAR, "NCHAR(1)" );
		registerColumnType( Types.VARCHAR, NATIONAL_CHAR_LENGTH, "NATIONAL CHARACTER VARYING($l)" );
		registerColumnType( Types.VARCHAR, "NCLOB($l)" );
		registerColumnType( Types.LONGVARCHAR, "CLOB($1)" );
		registerColumnType( Types.FLOAT, "FLOAT" );
		registerColumnType( Types.DOUBLE, "DOUBLE PRECISION" );
		registerColumnType( Types.DATE, "DATE" );
		registerColumnType( Types.TIME, "TIME" );
		registerColumnType( Types.TIMESTAMP, "TIMESTAMP" );
		registerColumnType( Types.VARBINARY, BINARY_MAX_LENGTH, "BINARY VARYING($l)" );
		registerColumnType( Types.VARBINARY, "BLOB($1)" );
		registerColumnType( Types.LONGVARBINARY, "BLOB($1)" );
		registerColumnType( Types.BINARY, BINARY_MAX_LENGTH, "BINARY" );
		registerColumnType( Types.BINARY, "BLOB($1)" );
		registerColumnType( Types.NUMERIC, "NUMERIC(19, $l)" );
		registerColumnType( Types.BLOB, "BLOB($l)" );
		registerColumnType( Types.CLOB, "NCLOB($l)" );

		getDefaultProperties().setProperty( Environment.USE_STREAMS_FOR_BINARY, "true" );
		getDefaultProperties().setProperty( Environment.STATEMENT_BATCH_SIZE, "50" );
	}



	@Override
	public void initializeFunctionRegistry(QueryEngine queryEngine) {
		super.initializeFunctionRegistry( queryEngine );

		queryEngine.getSqmFunctionRegistry().registerNamed( "round" );

		queryEngine.getSqmFunctionRegistry().registerNamed( "dacos", StandardBasicTypes.DOUBLE );
		queryEngine.getSqmFunctionRegistry().registerNamed( "dasin", StandardBasicTypes.DOUBLE );
		queryEngine.getSqmFunctionRegistry().registerNamed( "datan", StandardBasicTypes.DOUBLE );
		queryEngine.getSqmFunctionRegistry().registerNamed( "datan2", StandardBasicTypes.DOUBLE );
		queryEngine.getSqmFunctionRegistry().registerNamed( "dcos", StandardBasicTypes.DOUBLE );
		queryEngine.getSqmFunctionRegistry().registerNamed( "dcot", StandardBasicTypes.DOUBLE );
		queryEngine.getSqmFunctionRegistry().registerNamed( "cot", StandardBasicTypes.DOUBLE );
		queryEngine.getSqmFunctionRegistry().registerNamed( "ddegrees", StandardBasicTypes.DOUBLE );
		queryEngine.getSqmFunctionRegistry().registerNamed( "degrees", StandardBasicTypes.DOUBLE );
		queryEngine.getSqmFunctionRegistry().registerNamed( "dexp", StandardBasicTypes.DOUBLE );
		queryEngine.getSqmFunctionRegistry().registerNamed( "dlog", StandardBasicTypes.DOUBLE );
		queryEngine.getSqmFunctionRegistry().registerNamed( "log", StandardBasicTypes.DOUBLE );
		queryEngine.getSqmFunctionRegistry().registerNamed( "dlog10", StandardBasicTypes.DOUBLE );
		queryEngine.getSqmFunctionRegistry().registerNamed( "log10", StandardBasicTypes.DOUBLE );
		queryEngine.getSqmFunctionRegistry().registerNamed( "dradian", StandardBasicTypes.DOUBLE );
		queryEngine.getSqmFunctionRegistry().registerNamed( "radian", StandardBasicTypes.DOUBLE );
		queryEngine.getSqmFunctionRegistry().registerNamed( "dsin", StandardBasicTypes.DOUBLE );
		queryEngine.getSqmFunctionRegistry().registerNamed( "soundex", StandardBasicTypes.STRING );
		queryEngine.getSqmFunctionRegistry().registerNamed( "dsqrt", StandardBasicTypes.DOUBLE );
		queryEngine.getSqmFunctionRegistry().registerNamed( "dtan", StandardBasicTypes.DOUBLE );
		queryEngine.getSqmFunctionRegistry().registerNamed( "dpower" );

		queryEngine.getSqmFunctionRegistry().registerNamed( "date", StandardBasicTypes.DATE );
		queryEngine.getSqmFunctionRegistry().registerNamed( "dayofweek", StandardBasicTypes.INTEGER );
		queryEngine.getSqmFunctionRegistry().registerNamed( "dayofyear", StandardBasicTypes.INTEGER );
		queryEngine.getSqmFunctionRegistry().registerNamed( "time", StandardBasicTypes.TIME );
		queryEngine.getSqmFunctionRegistry().registerNamed( "timestamp", StandardBasicTypes.TIMESTAMP );
		queryEngine.getSqmFunctionRegistry().registerNamed( "week", StandardBasicTypes.INTEGER );


		queryEngine.getSqmFunctionRegistry().registerNamed( "varchar", StandardBasicTypes.STRING );
		queryEngine.getSqmFunctionRegistry().registerNamed( "real", StandardBasicTypes.FLOAT );
		queryEngine.getSqmFunctionRegistry().registerNamed( "bigint", StandardBasicTypes.LONG );
		queryEngine.getSqmFunctionRegistry().registerNamed( "char", StandardBasicTypes.CHARACTER );
		queryEngine.getSqmFunctionRegistry().registerNamed( "integer", StandardBasicTypes.INTEGER );
		queryEngine.getSqmFunctionRegistry().registerNamed( "smallint", StandardBasicTypes.SHORT );

		queryEngine.getSqmFunctionRegistry().registerNamed( "ascii_char", StandardBasicTypes.CHARACTER );
		queryEngine.getSqmFunctionRegistry().registerNamed( "ascii_code", StandardBasicTypes.STRING );
		queryEngine.getSqmFunctionRegistry().registerNamed( "unicode_char", StandardBasicTypes.LONG );
		queryEngine.getSqmFunctionRegistry().registerNamed( "unicode_code", StandardBasicTypes.STRING );
	}

	@Override
	public String getAddColumnString() {
		return "add column";
	}

	@Override
	public boolean dropConstraints() {
		return false;
	}

	@Override
	public boolean supportsSequences() {
		return true;
	}

	@Override
	public String getSequenceNextValString(String sequenceName) {
		return "select next_value of " + sequenceName + " from system.onerow";
	}

	@Override
	public String getCreateSequenceString(String sequenceName) {
		return "create unique sequence " + sequenceName;
	}

	@Override
	public String getDropSequenceString(String sequenceName) {
		return "drop sequence " + sequenceName + " restrict";
	}

	@Override
	public boolean supportsLimit() {
		return false;
	}

	@Override
	public String getCascadeConstraintsString() {
		return " cascade";
	}

	@Override
	public String getQuerySequencesString() {
		return "select * from information_schema.ext_sequences";
	}

	@Override
	public SequenceInformationExtractor getSequenceInformationExtractor() {
		return SequenceInformationExtractorMimerSQLDatabaseImpl.INSTANCE;
	}

	@Override
	public boolean forUpdateOfColumns() {
		return false;
	}

	@Override
	public boolean supportsOuterJoinForUpdate() {
		return false;
	}

	@Override
	public IdentityColumnSupport getIdentityColumnSupport() {
		return new MimerSQLIdentityColumnSupport();
	}
}
