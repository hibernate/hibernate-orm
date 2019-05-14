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
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.dialect.identity.CUBRIDIdentityColumnSupport;
import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.dialect.pagination.CUBRIDLimitHandler;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.tool.schema.extract.internal.SequenceInformationExtractorCUBRIDDatabaseImpl;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractor;
import org.hibernate.type.spi.StandardSpiBasicTypes;

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
		CommonFunctionFactory.pad( queryEngine );
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
		CommonFunctionFactory.bitandorxornot_bitAndOrXorNot( queryEngine );

		CommonFunctionFactory.median( queryEngine );
		CommonFunctionFactory.stddev( queryEngine );
		CommonFunctionFactory.stddevPopSamp( queryEngine );
		CommonFunctionFactory.variance( queryEngine );
		CommonFunctionFactory.varPopSamp( queryEngine );

		queryEngine.getSqmFunctionRegistry().registerNoArgs( "rownum", StandardSpiBasicTypes.INTEGER );
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
	public boolean supportsPartitionBy() {
		return true;
	}
}
