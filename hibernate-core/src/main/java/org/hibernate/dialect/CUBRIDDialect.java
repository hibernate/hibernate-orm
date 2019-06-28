/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import java.sql.Types;

import org.hibernate.HibernateException;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.metamodel.model.relational.spi.Size;
import org.hibernate.query.TemporalUnit;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.dialect.identity.CUBRIDIdentityColumnSupport;
import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.dialect.pagination.LimitLimitHandler;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.query.sqm.SemanticException;
import org.hibernate.tool.schema.extract.internal.SequenceInformationExtractorCUBRIDDatabaseImpl;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractor;

import static org.hibernate.query.TemporalUnit.HOUR;
import static org.hibernate.query.TemporalUnit.MINUTE;
import static org.hibernate.query.TemporalUnit.NANOSECOND;
import static org.hibernate.query.TemporalUnit.NATIVE;
import static org.hibernate.query.TemporalUnit.SECOND;

/**
 * An SQL dialect for CUBRID (8.3.x and later).
 *
 * @author Seok Jeong Il
 */
public class CUBRIDDialect extends Dialect {
	/**
	 * Constructs a CUBRIDDialect
	 */
	public CUBRIDDialect() {
		super();

		registerColumnType( Types.BOOLEAN, "bit" );
		registerColumnType( Types.TINYINT, "smallint" ); //no 'tinyint'

		//'timestamp' has a very limited range
		//'datetime' does not support explicit precision
		//(always 3, millisecond precision)
		registerColumnType(Types.TIMESTAMP, "datetime");
		registerColumnType(Types.TIMESTAMP, "datetimetz");

		//CUBRID has no 'binary' nor 'varbinary', but 'bit' is
		//intended to be used for binary data
		registerColumnType( Types.BINARY, "bit($l)");
		registerColumnType( Types.VARBINARY, "bit varying($l)");
		registerColumnType( Types.LONGVARBINARY, "bit varying($l)");

		getDefaultProperties().setProperty( Environment.USE_STREAMS_FOR_BINARY, "true" );
		getDefaultProperties().setProperty( Environment.STATEMENT_BATCH_SIZE, DEFAULT_BATCH_SIZE );

		registerKeyword( "TYPE" );
		registerKeyword( "YEAR" );
		registerKeyword( "MONTH" );
		registerKeyword( "ALIAS" );
		registerKeyword( "VALUE" );
		registerKeyword( "FIRST" );
		registerKeyword( "ROLE" );
		registerKeyword( "CLASS" );
		registerKeyword( "BIT" );
		registerKeyword( "TIME" );
		registerKeyword( "QUERY" );
		registerKeyword( "DATE" );
		registerKeyword( "USER" );
		registerKeyword( "ACTION" );
		registerKeyword( "SYS_USER" );
		registerKeyword( "ZONE" );
		registerKeyword( "LANGUAGE" );
		registerKeyword( "DICTIONARY" );
		registerKeyword( "DATA" );
		registerKeyword( "TEST" );
		registerKeyword( "SUPERCLASS" );
		registerKeyword( "SECTION" );
		registerKeyword( "LOWER" );
		registerKeyword( "LIST" );
		registerKeyword( "OID" );
		registerKeyword( "DAY" );
		registerKeyword( "IF" );
		registerKeyword( "ATTRIBUTE" );
		registerKeyword( "STRING" );
		registerKeyword( "SEARCH" );
	}

	@Override
	public int getPreferredSqlTypeCodeForBoolean() {
		return Types.BIT;
	}

	//not used for anything right now, but it
	//could be used for timestamp literal format
	@Override
	public int getDefaultTimestampPrecision() {
		return 3;
	}

	@Override
	public String getTypeName(int code, Size size) throws HibernateException {
		//precision of a CUBRID 'float(p)' represents
		//decimal digits instead of binary digits
		return super.getTypeName( code, binaryToDecimalPrecision( code, size ) );
	}

	@Override
	public void initializeFunctionRegistry(QueryEngine queryEngine) {
		super.initializeFunctionRegistry( queryEngine );

		CommonFunctionFactory.trim2( queryEngine );
		CommonFunctionFactory.space( queryEngine );
		CommonFunctionFactory.reverse( queryEngine );
		CommonFunctionFactory.repeat( queryEngine );
		CommonFunctionFactory.crc32( queryEngine );
		CommonFunctionFactory.cot( queryEngine );
		CommonFunctionFactory.log2( queryEngine );
		CommonFunctionFactory.log10( queryEngine );
		CommonFunctionFactory.pi( queryEngine );
		//rand() returns an integer between 0 and 231 on CUBRID
//		CommonFunctionFactory.rand( queryEngine );
		CommonFunctionFactory.radians( queryEngine );
		CommonFunctionFactory.degrees( queryEngine );
		CommonFunctionFactory.systimestamp( queryEngine );
		//TODO: CUBRID also has systime()/sysdate() returning TIME/DATE
		CommonFunctionFactory.localtimeLocaltimestamp( queryEngine );
		CommonFunctionFactory.hourMinuteSecond( queryEngine );
		CommonFunctionFactory.yearMonthDay( queryEngine );
		CommonFunctionFactory.dayofweekmonthyear( queryEngine );
		CommonFunctionFactory.lastDay( queryEngine );
		CommonFunctionFactory.weekQuarter( queryEngine );
		CommonFunctionFactory.octetLength( queryEngine );
		CommonFunctionFactory.md5( queryEngine );
		CommonFunctionFactory.trunc( queryEngine );
		CommonFunctionFactory.truncate( queryEngine );
		CommonFunctionFactory.toCharNumberDateTimestamp( queryEngine );
		CommonFunctionFactory.substr( queryEngine );
		//also natively supports ANSI-style substring()
		CommonFunctionFactory.instr( queryEngine );
		CommonFunctionFactory.translate( queryEngine );
		CommonFunctionFactory.ceiling_ceil( queryEngine );
		CommonFunctionFactory.sha1( queryEngine );
		CommonFunctionFactory.sha2( queryEngine );
		CommonFunctionFactory.ascii( queryEngine );
		CommonFunctionFactory.char_chr( queryEngine );
		CommonFunctionFactory.position( queryEngine );
//		CommonFunctionFactory.concat_pipeOperator( queryEngine );
		CommonFunctionFactory.insert( queryEngine );
		CommonFunctionFactory.nowCurdateCurtime( queryEngine );
		CommonFunctionFactory.makedateMaketime( queryEngine );
		CommonFunctionFactory.bitandorxornot_bitAndOrXorNot( queryEngine );
		CommonFunctionFactory.median( queryEngine );
		CommonFunctionFactory.stddev( queryEngine );
		CommonFunctionFactory.stddevPopSamp( queryEngine );
		CommonFunctionFactory.variance( queryEngine );
		CommonFunctionFactory.varPopSamp( queryEngine );
		CommonFunctionFactory.datediff( queryEngine );
		CommonFunctionFactory.adddateSubdateAddtimeSubtime( queryEngine );
		CommonFunctionFactory.addMonths( queryEngine );
		CommonFunctionFactory.monthsBetween( queryEngine );
		CommonFunctionFactory.rownumInstOrderbyGroupbyNum( queryEngine );
	}

	@Override
	public boolean supportsColumnCheck() {
		return false;
	}

	@Override
	public boolean supportsPooledSequences() {
		return true;
	}

	@Override
	public String getSequenceNextValString(String sequenceName) {
		return "select " + sequenceName + ".next_value from table({1}) as T(X)";
	}

	@Override
	public String getCreateSequenceString(String sequenceName) {
		return "create serial " + sequenceName;
	}

	@Override
	public String getDropSequenceString(String sequenceName) {
		return "drop serial " + sequenceName;
	}

	@Override
	public String getDropForeignKeyString() {
		return " drop foreign key ";
	}

	@Override
	public boolean qualifyIndexName() {
		return false;
	}

	@Override
	public boolean supportsSequences() {
		return true;
	}

	@Override
	public boolean supportsExistsInSelect() {
		return false;
	}

	@Override
	public String getQuerySequencesString() {
		return "select * from db_serial";
	}

	@Override
	public SequenceInformationExtractor getSequenceInformationExtractor() {
		return SequenceInformationExtractorCUBRIDDatabaseImpl.INSTANCE;
	}

	@Override
	public String getFromDual() {
		return "from db_root";
	}

	@Override
	public char openQuote() {
		return '[';
	}

	@Override
	public char closeQuote() {
		return ']';
	}

	@Override
	public String getForUpdateString() {
		return "";
	}

	@Override
	public boolean supportsUnionAll() {
		return true;
	}

	@Override
	public boolean supportsCurrentTimestampSelection() {
		return true;
	}

	@Override
	public String getCurrentTimestampSelectString() {
		return "select now()";
	}

	@Override
	public boolean isCurrentTimestampSelectStringCallable() {
		return false;
	}

	@Override
	public boolean supportsEmptyInList() {
		return false;
	}

	@Override
	public boolean supportsIfExistsBeforeTableName() {
		return true;
	}

	@Override
	public boolean supportsTupleDistinctCounts() {
		return false;
	}

	@Override
	public LimitHandler getLimitHandler() {
		return LimitLimitHandler.INSTANCE;
	}

	@Override
	public IdentityColumnSupport getIdentityColumnSupport() {
		return new CUBRIDIdentityColumnSupport();
	}

	@Override
	public boolean supportsPartitionBy() {
		return true;
	}

	@Override
	public String translateDatetimeFormat(String format) {
		//I do not know if CUBRID supports FM, but it
		//seems that it does pad by default, so it needs it!
		return OracleDialect.datetimeFormat( format, true )
				.replace("SSSSSS", "FF")
				.replace("SSSSS", "FF")
				.replace("SSSS", "FF")
				.replace("SSS", "FF")
				.replace("SS", "FF")
				.replace("S", "FF")
				.result();
	}

	@Override
	public long getFractionalSecondPrecisionInNanos() {
		return 1_000_000; //milliseconds
	}

	/**
	 * CUBRID supports a limited list of temporal fields in the
	 * extract() function, but we can emulate some of them by
	 * using the appropriate named functions instead of
	 * extract().
	 *
	 * Thus, the additional supported fields are
	 * {@link TemporalUnit#DAY_OF_YEAR},
	 * {@link TemporalUnit#DAY_OF_MONTH},
	 * {@link TemporalUnit#DAY_OF_YEAR}.
	 *
	 * In addition, the field {@link TemporalUnit#SECOND} is
	 * redefined to include milliseconds.
	 */
	@Override
	public String extractPattern(TemporalUnit unit) {
		switch (unit) {
			case SECOND:
				return "(second(?2)+extract(millisecond from ?2)/1e3)";
			case DAY_OF_WEEK:
				return "dayofweek(?2)";
			case DAY_OF_MONTH:
				return "dayofmonth(?2)";
			case DAY_OF_YEAR:
				return "dayofyear(?2)";
			case WEEK:
				return "week(?2,3)"; //mode 3 is the ISO week
			default:
				return "?1(?2)";
		}
	}

	@Override
	public String timestampaddPattern(TemporalUnit unit, boolean timestamp) {
		switch (unit) {
			case NANOSECOND:
				return "adddate(?3, interval (?2)/1e6 millisecond)";
			case NATIVE:
				return "adddate(?3, interval ?2 millisecond)";
			default:
				return "adddate(?3, interval ?2 ?1)";
		}
	}

	@Override
	public String timestampdiffPattern(TemporalUnit unit, boolean fromTimestamp, boolean toTimestamp) {
		StringBuilder pattern = new StringBuilder();
		switch ( unit ) {
			case DAY:
				//note: datediff() is backwards on CUBRID
				return "datediff(?3,?2)";
			case HOUR:
				timediff(pattern, HOUR, unit);
				break;
			case MINUTE:
				pattern.append("(");
				timediff(pattern, MINUTE, unit);
				pattern.append("+");
				timediff(pattern, HOUR, unit);
				pattern.append(")");
				break;
			case SECOND:
				pattern.append("(");
				timediff(pattern, SECOND, unit);
				pattern.append("+");
				timediff(pattern, MINUTE, unit);
				pattern.append("+");
				timediff(pattern, HOUR, unit);
				pattern.append(")");
				break;
			case NATIVE:
			case NANOSECOND:
				pattern.append("(");
				timediff(pattern, unit, unit);
				pattern.append("+");
				timediff(pattern, SECOND, unit);
				pattern.append("+");
				timediff(pattern, MINUTE, unit);
				pattern.append("+");
				timediff(pattern, HOUR, unit);
				pattern.append(")");
				break;
			default:
				throw new SemanticException("unsupported temporal unit for CUBRID: " + unit);
		}
		return pattern.toString();
	}

	private void timediff(
			StringBuilder sqlAppender,
			TemporalUnit diffUnit,
			TemporalUnit toUnit) {
		if ( diffUnit == NANOSECOND ) {
			sqlAppender.append("1e6*");
		}
		sqlAppender.append("extract(");
		if ( diffUnit == NANOSECOND || diffUnit == NATIVE ) {
			sqlAppender.append("millisecond");
		}
		else {
			sqlAppender.append("?1");
		}
		//note: timediff() is backwards on CUBRID
		sqlAppender.append(",timediff(?3,?2))");
		sqlAppender.append( diffUnit.conversionFactor( toUnit, this ) );
	}
}
