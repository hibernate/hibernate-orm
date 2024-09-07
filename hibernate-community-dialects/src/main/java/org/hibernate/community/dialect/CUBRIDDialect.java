/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.community.dialect;

import java.sql.Types;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.community.dialect.identity.CUBRIDIdentityColumnSupport;
import org.hibernate.community.dialect.sequence.CUBRIDSequenceSupport;
import org.hibernate.community.dialect.sequence.SequenceInformationExtractorCUBRIDDatabaseImpl;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.SimpleDatabaseVersion;
import org.hibernate.dialect.TimeZoneSupport;
import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.dialect.pagination.LimitLimitHandler;
import org.hibernate.dialect.sequence.SequenceSupport;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.SemanticException;
import org.hibernate.query.sqm.IntervalType;
import org.hibernate.query.common.TemporalUnit;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.spi.StandardSqlAstTranslatorFactory;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractor;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;
import org.hibernate.type.descriptor.sql.internal.BinaryFloatDdlType;
import org.hibernate.type.descriptor.sql.internal.CapacityDependentDdlType;
import org.hibernate.type.descriptor.sql.internal.DdlTypeImpl;
import org.hibernate.type.descriptor.sql.spi.DdlTypeRegistry;

import jakarta.persistence.TemporalType;

import static org.hibernate.query.common.TemporalUnit.HOUR;
import static org.hibernate.query.common.TemporalUnit.MINUTE;
import static org.hibernate.query.common.TemporalUnit.NANOSECOND;
import static org.hibernate.query.common.TemporalUnit.NATIVE;
import static org.hibernate.query.common.TemporalUnit.SECOND;
import static org.hibernate.type.SqlTypes.BINARY;
import static org.hibernate.type.SqlTypes.BLOB;
import static org.hibernate.type.SqlTypes.BOOLEAN;
import static org.hibernate.type.SqlTypes.TIMESTAMP;
import static org.hibernate.type.SqlTypes.TIMESTAMP_WITH_TIMEZONE;
import static org.hibernate.type.SqlTypes.TIME_WITH_TIMEZONE;
import static org.hibernate.type.SqlTypes.TINYINT;
import static org.hibernate.type.SqlTypes.VARBINARY;

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
		super( SimpleDatabaseVersion.ZERO_VERSION );
	}

	@Override
	protected String columnType(int sqlTypeCode) {
		switch ( sqlTypeCode ) {
			case BOOLEAN:
				return "bit";
			case TINYINT:
				return "smallint";
			//'timestamp' has a very limited range
			//'datetime' does not support explicit precision
			//(always 3, millisecond precision)
			case TIMESTAMP:
				return "datetime";
			case TIME_WITH_TIMEZONE:
			case TIMESTAMP_WITH_TIMEZONE:
				return "datetimetz";
			default:
				return super.columnType( sqlTypeCode );
		}
	}

	@Override
	protected void registerColumnTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		super.registerColumnTypes( typeContributions, serviceRegistry );
		final DdlTypeRegistry ddlTypeRegistry = typeContributions.getTypeConfiguration().getDdlTypeRegistry();

		//precision of a Mimer 'float(p)' represents
		//decimal digits instead of binary digits
		ddlTypeRegistry.addDescriptor( new BinaryFloatDdlType( this ) );

		//CUBRID has no 'binary' nor 'varbinary', but 'bit' is
		//intended to be used for binary data (unfortunately the
		//length parameter is measured in bits, not bytes)
		ddlTypeRegistry.addDescriptor( new DdlTypeImpl( BINARY, "bit($l)", this ) );
		ddlTypeRegistry.addDescriptor(
				CapacityDependentDdlType.builder(
								VARBINARY,
								CapacityDependentDdlType.LobKind.BIGGEST_LOB,
								columnType( BLOB ),
								this
						)
						.withTypeCapacity( getMaxVarbinaryLength(), "bit varying($l)" )
						.build()
		);
	}

	@Override
	protected void registerDefaultKeywords() {
		super.registerDefaultKeywords();
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

	public CUBRIDDialect(DialectResolutionInfo info) {
		this();
		registerKeywords( info );
	}

	@Override
	public int getDefaultStatementBatchSize() {
		return 15;
	}

	@Override
	public int getMaxVarcharLength() {
		return 1_073_741_823;
	}

	@Override
	public int getMaxVarbinaryLength() {
		//note that the length of BIT VARYING in CUBRID is actually in bits
		return 1_073_741_823;
	}

	@Override
	public JdbcType resolveSqlTypeDescriptor(
			String columnTypeName,
			int jdbcTypeCode,
			int precision,
			int scale,
			JdbcTypeRegistry jdbcTypeRegistry) {
		if ( jdbcTypeCode == Types.BIT ) {
			return jdbcTypeRegistry.getDescriptor( Types.BOOLEAN );
		}
		return super.resolveSqlTypeDescriptor(
				columnTypeName,
				jdbcTypeCode,
				precision,
				scale,
				jdbcTypeRegistry
		);
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
	public int getFloatPrecision() {
		return 21; // -> 7 decimal digits
	}

	@Override
	public void initializeFunctionRegistry(FunctionContributions functionContributions) {
		super.initializeFunctionRegistry(functionContributions);

		CommonFunctionFactory functionFactory = new CommonFunctionFactory(functionContributions);
		functionFactory.trim2();
		functionFactory.space();
		functionFactory.reverse();
		functionFactory.repeat();
		functionFactory.crc32();
		functionFactory.cot();
		functionFactory.log2();
		functionFactory.log10();
		functionFactory.pi();
		//rand() returns an integer between 0 and 231 on CUBRID
//		functionFactory.rand();
		functionFactory.radians();
		functionFactory.degrees();
		functionFactory.systimestamp();
		//TODO: CUBRID also has systime()/sysdate() returning TIME/DATE
		functionFactory.localtimeLocaltimestamp();
		functionFactory.hourMinuteSecond();
		functionFactory.yearMonthDay();
		functionFactory.dayofweekmonthyear();
		functionFactory.lastDay();
		functionFactory.weekQuarter();
		functionFactory.octetLength();
		functionFactory.bitLength();
		functionFactory.md5();
		functionFactory.trunc();
//		functionFactory.truncate();
		functionFactory.toCharNumberDateTimestamp();
		functionFactory.substr();
		//also natively supports ANSI-style substring()
		functionFactory.instr();
		functionFactory.translate();
		functionFactory.ceiling_ceil();
		functionFactory.sha1();
		functionFactory.sha2();
		functionFactory.ascii();
		functionFactory.char_chr();
		functionFactory.position();
//		functionFactory.concat_pipeOperator();
		functionFactory.insert();
		functionFactory.nowCurdateCurtime();
		functionFactory.makedateMaketime();
		functionFactory.bitandorxornot_bitAndOrXorNot();
		functionFactory.median();
		functionFactory.stddev();
		functionFactory.stddevPopSamp();
		functionFactory.variance();
		functionFactory.varPopSamp();
		functionFactory.datediff();
		functionFactory.adddateSubdateAddtimeSubtime();
		functionFactory.addMonths();
		functionFactory.monthsBetween();
		functionFactory.rownumInstOrderbyGroupbyNum();
	}

	@Override
	public boolean supportsColumnCheck() {
		return false;
	}

	@Override
	public SequenceSupport getSequenceSupport() {
		return CUBRIDSequenceSupport.INSTANCE;
	}

	@Override
	public String getDropForeignKeyString() {
		return "drop foreign key";
	}

	@Override
	public String getDropUniqueKeyString() {
		return "drop index";
	}

	@Override
	public boolean qualifyIndexName() {
		return false;
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
	public boolean supportsIfExistsBeforeTableName() {
		return true;
	}

	@Override
	public boolean supportsTupleDistinctCounts() {
		return false;
	}

	@Override
	public boolean supportsOffsetInSubquery() {
		return true;
	}

	@Override
	public boolean supportsTemporaryTables() {
		return false;
	}

	@Override
	public SqlAstTranslatorFactory getSqlAstTranslatorFactory() {
		return new StandardSqlAstTranslatorFactory() {
			@Override
			protected <T extends JdbcOperation> SqlAstTranslator<T> buildTranslator(
					SessionFactoryImplementor sessionFactory, Statement statement) {
				return new CUBRIDSqlAstTranslator<>( sessionFactory, statement );
			}
		};
	}

	@Override
	public LimitHandler getLimitHandler() {
		return LimitLimitHandler.INSTANCE;
	}

	@Override
	public IdentityColumnSupport getIdentityColumnSupport() {
		return CUBRIDIdentityColumnSupport.INSTANCE;
	}

	@Override
	public boolean supportsPartitionBy() {
		return true;
	}

	@Override
	public void appendDatetimeFormat(SqlAppender appender, String format) {
		//I do not know if CUBRID supports FM, but it
		//seems that it does pad by default, so it needs it!
		appender.appendSql(
				OracleDialect.datetimeFormat( format, true, false )
				.replace("SSSSSS", "FF")
				.replace("SSSSS", "FF")
				.replace("SSSS", "FF")
				.replace("SSS", "FF")
				.replace("SS", "FF")
				.replace("S", "FF")
				.result()
		);
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
	public TimeZoneSupport getTimeZoneSupport() {
		return TimeZoneSupport.NATIVE;
	}

	@Override
	public String timestampaddPattern(TemporalUnit unit, TemporalType temporalType, IntervalType intervalType) {
		switch (unit) {
			case NANOSECOND:
				return "adddate(?3,interval (?2)/1e6 millisecond)";
			case NATIVE:
				return "adddate(?3,interval ?2 millisecond)";
			default:
				return "adddate(?3,interval ?2 ?1)";
		}
	}

	@Override
	public String timestampdiffPattern(TemporalUnit unit, TemporalType fromTemporalType, TemporalType toTemporalType) {
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
