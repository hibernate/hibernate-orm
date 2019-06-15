/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import java.sql.Types;

import org.hibernate.HibernateException;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.dialect.pagination.OffsetFetchLimitHandler;
import org.hibernate.metamodel.model.relational.spi.Size;
import org.hibernate.query.CastType;
import org.hibernate.query.TemporalUnit;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.dialect.identity.MimerSQLIdentityColumnSupport;
import org.hibernate.query.sqm.SemanticException;
import org.hibernate.tool.schema.extract.internal.SequenceInformationExtractorMimerSQLDatabaseImpl;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractor;

import static org.hibernate.query.CastType.BOOLEAN;

/**
 * A dialect for Mimer SQL 11.
 *
 * @author Fredrik lund <fredrik.alund@mimer.se>
 * @author Gavin King
 */
public class MimerSQLDialect extends Dialect {

	// KNOWN LIMITATIONS:

	// * no support for format()
	// * can't cast non-literal String to Binary
	// * no power(), exp(), ln(), sqrt() functions
	// * no trig functions, not even sin()
	// * can't select a parameter unless wrapped
	//   in a cast or function call

	public MimerSQLDialect() {
		super();
		//no 'bit' type
		registerColumnType( Types.BIT, 1, "boolean" );
		//no 'tinyint', so use integer with 3 decimal digits
		registerColumnType( Types.BIT, "integer(3)" );
		registerColumnType( Types.TINYINT, "integer(3)" );

		//Mimer CHARs are ASCII!!
		registerColumnType( Types.CHAR, "nchar($l)" );
		registerColumnType( Types.VARCHAR, "nvarchar($l)" );
		registerColumnType( Types.LONGVARCHAR, "nvarchar($l)" );

		//default length is 1M, which is quite low
		registerColumnType( Types.BLOB, "blob($l)" );
		registerColumnType( Types.CLOB, "nclob($l)" );

		registerColumnType( Types.TIMESTAMP_WITH_TIMEZONE, "timestamp($p)" );

		getDefaultProperties().setProperty( Environment.USE_STREAMS_FOR_BINARY, "true" );
		getDefaultProperties().setProperty( Environment.STATEMENT_BATCH_SIZE, "50" );
		getDefaultProperties().setProperty( Environment.QUERY_LITERAL_RENDERING, "literal" );
	}

	@Override
	public String getTypeName(int code, Size size) throws HibernateException {
		//precision of a Mimer 'float(p)' represents
		//decimal digits instead of binary digits
		return super.getTypeName( code, binaryToDecimalPrecision( code, size ) );
	}

//	@Override
//	public int getDefaultDecimalPrecision() {
//		//the maximum, but I guess it's too high
//		return 45;
//	}

	@Override
	public int getPreferredSqlTypeCodeForBoolean() {
		return Types.BOOLEAN;
	}

	@Override
	public void initializeFunctionRegistry(QueryEngine queryEngine) {
		super.initializeFunctionRegistry( queryEngine );

		CommonFunctionFactory.soundex( queryEngine );
		CommonFunctionFactory.octetLength( queryEngine );
		CommonFunctionFactory.truncate( queryEngine );
		CommonFunctionFactory.repeat( queryEngine );
		CommonFunctionFactory.pad_repeat( queryEngine );
		CommonFunctionFactory.dayofweekmonthyear( queryEngine );
		CommonFunctionFactory.concat_pipeOperator( queryEngine );
		CommonFunctionFactory.position( queryEngine );
		CommonFunctionFactory.localtimeLocaltimestamp( queryEngine );
	}

	/**
	 * Mimer does have a real {@link java.sql.Types#BOOLEAN}
	 * type, but it doesn't know how to cast to it.
	 */
	@Override
	public String cast(CastType from, CastType to) {
		switch (to) {
			case BOOLEAN:
				switch (from) {
					case STRING:
						return "case when regexp_match(lower(?1), '^(t|f|true|false)$') then lower(?1) like 't%' else null end";
					case INTEGER:
						return "(?1<>0)";
				}
			default:
				return super.cast(from, to);
		}
	}

	@Override
	public String currentTimestamp() {
		return "localtimestamp";
	}

	@Override
	public String currentTime() {
		return "localtime";
	}

	/**
	 * Mimer supports a limited list of temporal fields in the
	 * extract() function, but we can emulate some of them by
	 * using the appropriate named functions instead of
	 * extract().
	 *
	 * Thus, the additional supported fields are
	 * {@link TemporalUnit#WEEK},
	 * {@link TemporalUnit#DAY_OF_YEAR},
	 * {@link TemporalUnit#DAY_OF_MONTH},
	 * {@link TemporalUnit#DAY_OF_YEAR}.
	 */
	@Override
	public String extract(TemporalUnit unit) {
		switch (unit) {
			case WEEK:
				return "week(?2)";
			case DAY_OF_WEEK:
				return "dayofweek(?2)";
			case DAY_OF_YEAR:
				return "dayofyear(?2)";
			case DAY_OF_MONTH:
				return "day(?2)";
			default:
				return super.extract(unit);
		}
	}

	public String timestampdiff(TemporalUnit unit, boolean fromTimestamp, boolean toTimestamp) {
		StringBuilder pattern = new StringBuilder();
		pattern.append("cast((?3 - ?2) ");
		switch (unit) {
			case NATIVE:
			case NANOSECOND:
			case SECOND:
				pattern.append("second(12,9)");
				break;
			case MINUTE:
				pattern.append("minute(10)");
				break;
			case HOUR:
				pattern.append("hour(8)");
				break;
			case DAY:
			case WEEK:
				pattern.append("day(7)");
				break;
			case MONTH:
			case QUARTER:
				pattern.append("month(7)");
				break;
			case YEAR:
				pattern.append("year(7)");
				break;
			default:
				throw new SemanticException("unsupported duration unit: " + unit);
		}
		pattern.append(" as bigint)");
		switch (unit) {
			case WEEK:
				pattern.append("/7");
				break;
			case QUARTER:
				pattern.append("/3");
				break;
			case NATIVE:
			case NANOSECOND:
				pattern.append("*1e9");
				break;
		}
		return pattern.toString();
	}

	@Override
	public String timestampadd(TemporalUnit unit, boolean timestamp) {
		switch ( unit ) {
			case NATIVE:
			case NANOSECOND:
				return "(?3 + (?2)/1e9 * interval '1' second)";
			case QUARTER:
				return "(?3 + (?2) * interval '3' month)";
			case WEEK:
				return "(?3 + (?2) * interval '7' day)";
			default:
				return "(?3 + (?2) * interval '1' ?1)";
		}
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
	public boolean supportsPooledSequences() {
		return true;
	}

	@Override
	public String getSequenceNextValString(String sequenceName) {
		return "select next value for " + sequenceName + " from system.onerow";
	}

	@Override
	public String getDropSequenceString(String sequenceName) {
		return super.getDropSequenceString( sequenceName ) + " restrict";
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
	public LimitHandler getLimitHandler() {
		return OffsetFetchLimitHandler.INSTANCE;
	}

	@Override
	public String getFromDual() {
		return "from (values(0))";
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

	@Override
	public String translateDatetimeFormat(String format) {
		throw new NotYetImplementedFor6Exception("format() function not supported on Mimer SQL");
	}
}
