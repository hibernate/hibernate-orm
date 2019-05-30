/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import java.sql.Types;

import org.hibernate.cfg.Environment;
import org.hibernate.dialect.function.CUBRIDExtractEmulation;
import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.query.TemporalUnit;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.dialect.identity.CUBRIDIdentityColumnSupport;
import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.dialect.pagination.CUBRIDLimitHandler;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.query.sqm.SemanticException;
import org.hibernate.tool.schema.extract.internal.SequenceInformationExtractorCUBRIDDatabaseImpl;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractor;
import org.hibernate.type.spi.StandardSpiBasicTypes;

import static org.hibernate.query.TemporalUnit.HOUR;
import static org.hibernate.query.TemporalUnit.MILLISECOND;
import static org.hibernate.query.TemporalUnit.MINUTE;
import static org.hibernate.query.TemporalUnit.NANOSECOND;
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

		registerColumnType( Types.BIGINT, "bigint" );
		registerColumnType( Types.BIT, "bit(8)" );
		registerColumnType( Types.BLOB, "bit varying(65535)" );
		registerColumnType( Types.BOOLEAN, "bit(8)" );
		registerColumnType( Types.CHAR, "char(1)" );
		registerColumnType( Types.CLOB, "string" );
		registerColumnType( Types.DATE, "date" );
		registerColumnType( Types.DECIMAL, "decimal" );
		registerColumnType( Types.DOUBLE, "double" );
		registerColumnType( Types.FLOAT, "float" );
		registerColumnType( Types.INTEGER, "int" );
		registerColumnType( Types.NUMERIC, "numeric($p,$s)" );
		registerColumnType( Types.REAL, "double" );
		registerColumnType( Types.SMALLINT, "short" );
		registerColumnType( Types.TIME, "time" );
		registerColumnType( Types.TIMESTAMP, "timestamp" );
		registerColumnType( Types.TINYINT, "short" );
		registerColumnType( Types.VARBINARY, 2000, "bit varying($l)" );
		registerColumnType( Types.VARCHAR, "string" );
		registerColumnType( Types.VARCHAR, 2000, "varchar($l)" );
		registerColumnType( Types.VARCHAR, 255, "varchar($l)" );

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
	public void initializeFunctionRegistry(QueryEngine queryEngine) {
		super.initializeFunctionRegistry( queryEngine );

		CommonFunctionFactory.trim2( queryEngine );
		CommonFunctionFactory.pad( queryEngine );
		CommonFunctionFactory.space( queryEngine );
		CommonFunctionFactory.reverse( queryEngine );
		CommonFunctionFactory.repeat( queryEngine );
		CommonFunctionFactory.crc32( queryEngine );
		CommonFunctionFactory.cot( queryEngine );
		CommonFunctionFactory.log2( queryEngine );
		CommonFunctionFactory.log10( queryEngine );
		CommonFunctionFactory.pi( queryEngine );
		CommonFunctionFactory.rand( queryEngine );
		CommonFunctionFactory.radians( queryEngine );
		CommonFunctionFactory.degrees( queryEngine );
		CommonFunctionFactory.sysdateSystimestamp( queryEngine );
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
		CommonFunctionFactory.substring_substr( queryEngine );
		CommonFunctionFactory.instr( queryEngine );
		CommonFunctionFactory.translate( queryEngine );
		CommonFunctionFactory.stddev( queryEngine );
		CommonFunctionFactory.variance( queryEngine );
		CommonFunctionFactory.ceiling_ceil( queryEngine );
		CommonFunctionFactory.sha1sha2( queryEngine );
		CommonFunctionFactory.ascii( queryEngine );
		CommonFunctionFactory.char_chr( queryEngine );
		CommonFunctionFactory.datediff( queryEngine );
		CommonFunctionFactory.adddateSubdateAddtimeSubtime( queryEngine );
		CommonFunctionFactory.addMonths( queryEngine );
		CommonFunctionFactory.monthsBetween( queryEngine );
//		CommonFunctionFactory.concat_operator( queryEngine );
		IngresDialect.bitwiseFunctions( queryEngine );

		queryEngine.getSqmFunctionRegistry().register( "extract", new CUBRIDExtractEmulation() );

		queryEngine.getSqmFunctionRegistry().registerNoArgs( "rownum", StandardSpiBasicTypes.INTEGER );

		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "makedate" )
				.setInvariantType( StandardSpiBasicTypes.DATE )
				.setExactArgumentCount( 2 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "maketime" )
				.setInvariantType( StandardSpiBasicTypes.TIME )
				.setExactArgumentCount( 3 )
				.register();
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
	public String getAddColumnString() {
		return "add";
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
	public char openQuote() {
		return '[';
	}

	@Override
	public char closeQuote() {
		return ']';
	}

	@Override
	public String getForUpdateString() {
		return " ";
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
		return CUBRIDLimitHandler.INSTANCE;
	}

	@Override
	public IdentityColumnSupport getIdentityColumnSupport() {
		return new CUBRIDIdentityColumnSupport();
	}

	@Override
	public String translateDatetimeFormat(String format) {
		//I do not know if CUBRID supports FM, but it
		//seems that it does pad by default, so it needs it!
		return Oracle8iDialect.datetimeFormat( format, true )
				.replace("SSSSSS", "FF")
				.replace("SSSSS", "FF")
				.replace("SSSS", "FF")
				.replace("SSS", "FF")
				.replace("SS", "FF")
				.replace("S", "FF")
				.result();
	}

	@Override
	public void timestampadd(TemporalUnit unit, Renderer magnitude, Renderer to, Appender sqlAppender, boolean timestamp) {
		sqlAppender.append("adddate(");
		to.render();
		sqlAppender.append(",interval ");
		if ( unit == MILLISECOND || unit == NANOSECOND ) {
			sqlAppender.append("(");
		}
		magnitude.render();
		if ( unit == MILLISECOND ) {
			sqlAppender.append(")/1e3");
		}
		if ( unit == NANOSECOND ) {
			sqlAppender.append(")/1e6");
		}
		sqlAppender.append(" ");
		if ( unit == MILLISECOND || unit == NANOSECOND ) {
			sqlAppender.append("microsecond");
		}
		else {
			sqlAppender.append( unit.toString() );
		}
		sqlAppender.append(")");
	}

	@Override
	public void timestampdiff(TemporalUnit unit, Renderer from, Renderer to, Appender sqlAppender, boolean fromTimestamp, boolean toTimestamp) {
		switch ( unit ) {
			case DAY:
				sqlAppender.append("datediff(");
				to.render();
				sqlAppender.append(",");
				from.render();
				sqlAppender.append(")");
				break;
			case HOUR:
				timediff(from, to, sqlAppender, HOUR);
				break;
			case MINUTE:
				sqlAppender.append("(");
				timediff(from, to, sqlAppender, MINUTE);
				sqlAppender.append("+60*");
				timediff(from, to, sqlAppender, HOUR);
				sqlAppender.append(")");
			case SECOND:
				sqlAppender.append("(");
				timediff(from, to, sqlAppender, SECOND);
				sqlAppender.append("+60*");
				timediff(from, to, sqlAppender, MINUTE);
				sqlAppender.append("+3600*");
				timediff(from, to, sqlAppender, HOUR);
				sqlAppender.append(")");
			case MILLISECOND:
			case NANOSECOND:
				sqlAppender.append("(");
				timediff(from, to, sqlAppender, MILLISECOND);
				sqlAppender.append("+1e3*");
				timediff(from, to, sqlAppender, SECOND);
				sqlAppender.append("+6e4*");
				timediff(from, to, sqlAppender, MINUTE);
				sqlAppender.append("+36e5*");
				timediff(from, to, sqlAppender, HOUR);
				sqlAppender.append(")");
				if (unit==NANOSECOND) {
					sqlAppender.append("*1e3");
				}
			default:
				throw new SemanticException("unsupported temporal unit for CUBRID: " + unit);
		}
	}

	private void timediff(Renderer from, Renderer to, Appender sqlAppender, TemporalUnit unit) {
		sqlAppender.append("extract(");
		sqlAppender.append( unit.toString() );
		sqlAppender.append(",timediff(");
		to.render();
		sqlAppender.append(",");
		from.render();
		sqlAppender.append("))");
	}
}
