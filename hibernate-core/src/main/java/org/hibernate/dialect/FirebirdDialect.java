/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.dialect.function.FirebirdExtractEmulation;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.dialect.pagination.RowsLimitHandler;
import org.hibernate.query.TemporalUnit;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.tool.schema.extract.internal.SequenceNameExtractorImpl;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractor;
import org.hibernate.type.descriptor.internal.DateTimeUtils;
import org.hibernate.type.spi.StandardSpiBasicTypes;

import javax.persistence.TemporalType;
import java.sql.Types;
import java.time.temporal.TemporalAccessor;
import java.util.Calendar;
import java.util.Date;

import static org.hibernate.type.descriptor.internal.DateTimeUtils.formatAsTimestampWithMillis;

/**
 * An SQL dialect for Firebird.
 *
 * @author Reha CENANI
 * @author Gavin King
 */
public class FirebirdDialect extends Dialect {

	// KNOWN LIMITATIONS:

	// * no support for format()
	// * extremely low maximum decimal precision (18)
	//   making BigInteger/BigDecimal support useless

	public FirebirdDialect() {
		super();

		registerColumnType( Types.BIT, 1, "smallint" );
		registerColumnType( Types.BIT, "smallint" );
		registerColumnType( Types.BOOLEAN, "smallint" );

		registerColumnType( Types.TINYINT, "smallint" );

		//Firebird has a fixed-single-precision 'float'
		//type instead of a 'real' type together with
		//a variable-precision 'float(p)' type
		registerColumnType( Types.REAL, "float");
		registerColumnType( Types.FLOAT, "double precision");

		registerColumnType( Types.TIMESTAMP, "timestamp" );
		registerColumnType( Types.TIMESTAMP_WITH_TIMEZONE, "timestamp" );

		registerColumnType( Types.VARBINARY, "blob" );
		registerColumnType( Types.BLOB, "blob" );
		registerColumnType( Types.CLOB, "blob(,1)" ); //or 'blob sub_type 1'

		getDefaultProperties().setProperty( Environment.STATEMENT_BATCH_SIZE, NO_BATCH );
		getDefaultProperties().setProperty( Environment.QUERY_LITERAL_RENDERING, "literal" );
	}

	@Override
	public void initializeFunctionRegistry(QueryEngine queryEngine) {
		super.initializeFunctionRegistry( queryEngine );

		CommonFunctionFactory.concat_pipeOperator( queryEngine );
		CommonFunctionFactory.cot( queryEngine );
		CommonFunctionFactory.log( queryEngine );
		CommonFunctionFactory.log10( queryEngine );
		CommonFunctionFactory.pi( queryEngine );
		CommonFunctionFactory.rand( queryEngine );
		CommonFunctionFactory.sinh( queryEngine );
		CommonFunctionFactory.tanh( queryEngine );
		CommonFunctionFactory.cosh( queryEngine );
		CommonFunctionFactory.octetLength( queryEngine );
		CommonFunctionFactory.bitLength( queryEngine );
		CommonFunctionFactory.overlay( queryEngine );
		CommonFunctionFactory.position( queryEngine );
		CommonFunctionFactory.reverse( queryEngine );
		CommonFunctionFactory.bitandorxornot_bitAndOrXorNot( queryEngine );
		CommonFunctionFactory.leastGreatest_minMaxValue( queryEngine );

		//TODO: gen_uid() and friends, gen_id()

		queryEngine.getSqmFunctionRegistry().registerBinaryTernaryPattern(
				"locate",
				StandardSpiBasicTypes.INTEGER,
				"position(?1 in ?2)",
				"(position(?1 in substring(?2 from ?3)) + (?3) - 1)"
		).setArgumentListSignature("(pattern, string[, start])");

		queryEngine.getSqmFunctionRegistry().register( "extract", new FirebirdExtractEmulation() );
	}

	@Override
	public void timestampadd(TemporalUnit unit, Renderer magnitude, Renderer to, Appender sqlAppender, boolean timestamp) {
		sqlAppender.append("dateadd(");
		magnitude.render();
		switch (unit) {
			case NANOSECOND:
				sqlAppender.append("/1e6 millisecond");
				break;
			case WEEK:
				sqlAppender.append("*7 day");
				break;
			case QUARTER:
				sqlAppender.append("*4 month");
				break;
			default:
				sqlAppender.append(" ");
				sqlAppender.append(unit.toString());
				break;
		}
		sqlAppender.append(" to ");
		to.render();
		sqlAppender.append(")");
	}

	@Override
	public void timestampdiff(TemporalUnit unit, Renderer from, Renderer to, Appender sqlAppender, boolean fromTimestamp, boolean toTimestamp) {
		sqlAppender.append("datediff(");
		switch (unit) {
			case NANOSECOND:
				sqlAppender.append("millisecond");
				break;
			case WEEK:
				sqlAppender.append("day");
				break;
			case QUARTER:
				sqlAppender.append("month");
				break;
			default:
				sqlAppender.append(" ");
				sqlAppender.append(unit.toString());
				break;
		}
		sqlAppender.append(" from ");
		from.render();
		sqlAppender.append(" to ");
		to.render();
		sqlAppender.append(")");
		switch (unit) {
			case NANOSECOND:
				sqlAppender.append("*1e6");
				break;
			case WEEK:
				sqlAppender.append("/7");
				break;
			case QUARTER:
				sqlAppender.append("/4");
				break;
		}
	}

	@Override
	public int getDefaultDecimalPrecision() {
		//the extremely low maximum
		return 18;
	}

	@Override
	public String getAddColumnString() {
		return "add";
	}

	@Override
	public boolean supportsSequences() {
		return true;
	}

	@Override
	public String getSequenceNextValString(String sequenceName) {
		return "select " + getSelectSequenceNextValString( sequenceName ) + " " + getFromDual();
	}

	@Override
	public String getSelectSequenceNextValString(String sequenceName) {
		return "next value for " + sequenceName;
	}

	@Override
	public String getQuerySequencesString() {
		return "select rdb$generator_name from rdb$generators";
	}

	@Override
	public SequenceInformationExtractor getSequenceInformationExtractor() {
		return SequenceNameExtractorImpl.INSTANCE;
	}

	@Override
	public String getForUpdateString() {
		return " with lock";
	}

	@Override
	public String getForUpdateString(String aliases) {
		return " for update of " + aliases + " with lock";
	}

	@Override
	public LimitHandler getLimitHandler() {
		return RowsLimitHandler.INSTANCE;
	}

	@Override
	public String getCurrentTimestampSelectString() {
		return "select current_timestamp " + getFromDual();
	}

	@Override
	public boolean isCurrentTimestampSelectStringCallable() {
		return false;
	}

	@Override
	public String getFromDual() {
		return "from rdb$database";
	}

	@Override
	public String translateExtractField(TemporalUnit unit) {
		switch ( unit ) {
			case DAY_OF_MONTH: return "day";
			case DAY_OF_YEAR: return "yearday";
			case DAY_OF_WEEK: return "weekday";
			default: return unit.toString();
		}
	}

	public String formatDateTimeLiteral(TemporalAccessor temporalAccessor, TemporalType precision) {
		switch ( precision ) {
			case TIMESTAMP:
				return wrapTimestampLiteral( DateTimeUtils.formatAsTimestampWithMillis(temporalAccessor) );
			default:
				return super.formatDateTimeLiteral( temporalAccessor, precision );
		}
	}

	public String formatDateTimeLiteral(Date date, TemporalType precision) {
		switch ( precision ) {
			case TIMESTAMP:
				return wrapTimestampLiteral( DateTimeUtils.formatAsTimestampWithMillis(date) );
			default:
				return super.formatDateTimeLiteral( date, precision );
		}
	}

	public String formatDateTimeLiteral(Calendar calendar, TemporalType precision) {
		switch ( precision ) {
			case TIMESTAMP:
				return wrapTimestampLiteral( formatAsTimestampWithMillis(calendar) );
			default:
				return super.formatDateTimeLiteral( calendar, precision );
		}
	}

	@Override
	public String translateDatetimeFormat(String format) {
		throw new NotYetImplementedFor6Exception("format() function not supported on Firebird");
	}
}
