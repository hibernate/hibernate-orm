/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import java.sql.Types;

import org.hibernate.cfg.Environment;
import org.hibernate.query.sqm.produce.function.SqmFunctionRegistry;
import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.dialect.identity.MimerSQLIdentityColumnSupport;
import org.hibernate.type.spi.StandardSpiBasicTypes;
import org.hibernate.tool.schema.extract.internal.SequenceInformationExtractorMimerSQLDatabaseImpl;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractor;

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
	public void initializeFunctionRegistry(SqmFunctionRegistry registry) {
		super.initializeFunctionRegistry( registry );

		registry.registerNamed( "abs" );
		registry.registerNamed( "sign", StandardSpiBasicTypes.INTEGER );
		registry.registerNamed( "ceiling" );
		registry.registerNamed( "floor" );
		registry.registerNamed( "round" );

		registry.registerNamed( "dacos", StandardSpiBasicTypes.DOUBLE );
		registry.registerNamed( "acos", StandardSpiBasicTypes.DOUBLE );
		registry.registerNamed( "dasin", StandardSpiBasicTypes.DOUBLE );
		registry.registerNamed( "asin", StandardSpiBasicTypes.DOUBLE );
		registry.registerNamed( "datan", StandardSpiBasicTypes.DOUBLE );
		registry.registerNamed( "atan", StandardSpiBasicTypes.DOUBLE );
		registry.registerNamed( "datan2", StandardSpiBasicTypes.DOUBLE );
		registry.registerNamed( "atan2", StandardSpiBasicTypes.DOUBLE );
		registry.registerNamed( "dcos", StandardSpiBasicTypes.DOUBLE );
		registry.registerNamed( "cos", StandardSpiBasicTypes.DOUBLE );
		registry.registerNamed( "dcot", StandardSpiBasicTypes.DOUBLE );
		registry.registerNamed( "cot", StandardSpiBasicTypes.DOUBLE );
		registry.registerNamed( "ddegrees", StandardSpiBasicTypes.DOUBLE );
		registry.registerNamed( "degrees", StandardSpiBasicTypes.DOUBLE );
		registry.registerNamed( "dexp", StandardSpiBasicTypes.DOUBLE );
		registry.registerNamed( "exp", StandardSpiBasicTypes.DOUBLE );
		registry.registerNamed( "dlog", StandardSpiBasicTypes.DOUBLE );
		registry.registerNamed( "log", StandardSpiBasicTypes.DOUBLE );
		registry.registerNamed( "dlog10", StandardSpiBasicTypes.DOUBLE );
		registry.registerNamed( "log10", StandardSpiBasicTypes.DOUBLE );
		registry.registerNamed( "dradian", StandardSpiBasicTypes.DOUBLE );
		registry.registerNamed( "radian", StandardSpiBasicTypes.DOUBLE );
		registry.registerNamed( "dsin", StandardSpiBasicTypes.DOUBLE );
		registry.registerNamed( "sin", StandardSpiBasicTypes.DOUBLE );
		registry.registerNamed( "soundex", StandardSpiBasicTypes.STRING );
		registry.registerNamed( "dsqrt", StandardSpiBasicTypes.DOUBLE );
		registry.registerNamed( "sqrt", StandardSpiBasicTypes.DOUBLE );
		registry.registerNamed( "dtan", StandardSpiBasicTypes.DOUBLE );
		registry.registerNamed( "tan", StandardSpiBasicTypes.DOUBLE );
		registry.registerNamed( "dpower" );
		registry.registerNamed( "power" );

		registry.registerNamed( "date", StandardSpiBasicTypes.DATE );
		registry.registerNamed( "dayofweek", StandardSpiBasicTypes.INTEGER );
		registry.registerNamed( "dayofyear", StandardSpiBasicTypes.INTEGER );
		registry.registerNamed( "time", StandardSpiBasicTypes.TIME );
		registry.registerNamed( "timestamp", StandardSpiBasicTypes.TIMESTAMP );
		registry.registerNamed( "week", StandardSpiBasicTypes.INTEGER );


		registry.registerNamed( "varchar", StandardSpiBasicTypes.STRING );
		registry.registerNamed( "real", StandardSpiBasicTypes.FLOAT );
		registry.registerNamed( "bigint", StandardSpiBasicTypes.LONG );
		registry.registerNamed( "char", StandardSpiBasicTypes.CHARACTER );
		registry.registerNamed( "integer", StandardSpiBasicTypes.INTEGER );
		registry.registerNamed( "smallint", StandardSpiBasicTypes.SHORT );

		registry.registerNamed( "ascii_char", StandardSpiBasicTypes.CHARACTER );
		registry.registerNamed( "ascii_code", StandardSpiBasicTypes.STRING );
		registry.registerNamed( "unicode_char", StandardSpiBasicTypes.LONG );
		registry.registerNamed( "unicode_code", StandardSpiBasicTypes.STRING );
		registry.registerNamed( "upper" );
		registry.registerNamed( "lower" );
		registry.registerNamed( "char_length", StandardSpiBasicTypes.LONG );
		registry.registerNamed( "bit_length", StandardSpiBasicTypes.STRING );
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
