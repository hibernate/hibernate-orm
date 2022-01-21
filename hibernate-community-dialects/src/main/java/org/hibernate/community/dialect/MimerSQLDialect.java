/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.community.dialect;

import java.sql.Types;

import org.hibernate.HibernateException;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.community.dialect.identity.MimerSQLIdentityColumnSupport;
import org.hibernate.community.dialect.sequence.MimerSequenceSupport;
import org.hibernate.community.dialect.sequence.SequenceInformationExtractorMimerSQLDatabaseImpl;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.dialect.pagination.OffsetFetchLimitHandler;
import org.hibernate.dialect.sequence.SequenceSupport;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.IntervalType;
import org.hibernate.query.SemanticException;
import org.hibernate.query.TemporalUnit;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.spi.StandardSqlAstTranslatorFactory;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractor;

import jakarta.persistence.TemporalType;

import static org.hibernate.dialect.SimpleDatabaseVersion.ZERO_VERSION;

/**
 * A dialect for Mimer SQL 11.
 *
 * @author <a href="mailto:fredrik.alund@mimer.se">Fredrik lund</a>
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
		//no 'tinyint', so use integer with 3 decimal digits
		registerColumnType( Types.TINYINT, "integer(3)" );

		//Mimer CHARs are ASCII!!
		registerColumnType( Types.CHAR, "nchar($l)" );
		registerColumnType( Types.VARCHAR, 5_000, "nvarchar($l)" );
		registerColumnType( Types.VARCHAR, "nclob($l)" );
		registerColumnType( Types.NVARCHAR, 5_000, "nvarchar($l)" );
		registerColumnType( Types.NVARCHAR, "nclob($l)" );

		registerColumnType( Types.VARBINARY, 15_000, "varbinary($l)" );
		registerColumnType( Types.VARBINARY, "blob($l)" );

		//default length is 1M, which is quite low
		registerColumnType( Types.BLOB, "blob($l)" );
		registerColumnType( Types.CLOB, "nclob($l)" );
		registerColumnType( Types.NCLOB, "nclob($l)" );

		registerColumnType( Types.TIMESTAMP_WITH_TIMEZONE, "timestamp($p)" );
	}

	public MimerSQLDialect(DialectResolutionInfo info) {
		this();
		registerKeywords( info );
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
	public DatabaseVersion getVersion() {
		return ZERO_VERSION;
	}

	@Override
	public int getDefaultStatementBatchSize() {
		return 50;
	}

	@Override
	public void initializeFunctionRegistry(QueryEngine queryEngine) {
		super.initializeFunctionRegistry( queryEngine );

		CommonFunctionFactory.soundex( queryEngine );
		CommonFunctionFactory.octetLength( queryEngine );
		CommonFunctionFactory.bitLength( queryEngine );
		CommonFunctionFactory.truncate( queryEngine );
		CommonFunctionFactory.repeat( queryEngine );
		CommonFunctionFactory.pad_repeat( queryEngine );
		CommonFunctionFactory.dayofweekmonthyear( queryEngine );
		CommonFunctionFactory.concat_pipeOperator( queryEngine );
		CommonFunctionFactory.position( queryEngine );
		CommonFunctionFactory.localtimeLocaltimestamp( queryEngine );
	}

	@Override
	public SqlAstTranslatorFactory getSqlAstTranslatorFactory() {
		return new StandardSqlAstTranslatorFactory() {
			@Override
			protected <T extends JdbcOperation> SqlAstTranslator<T> buildTranslator(
					SessionFactoryImplementor sessionFactory, Statement statement) {
				return new MimerSQLSqlAstTranslator<>( sessionFactory, statement );
			}
		};
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
	public String extractPattern(TemporalUnit unit) {
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
				return super.extractPattern(unit);
		}
	}

	public String timestampdiffPattern(TemporalUnit unit, TemporalType fromTemporalType, TemporalType toTemporalType) {
		StringBuilder pattern = new StringBuilder();
		pattern.append("cast((?3-?2) ");
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
	public String timestampaddPattern(TemporalUnit unit, TemporalType temporalType, IntervalType intervalType) {
		switch ( unit ) {
			case NATIVE:
			case NANOSECOND:
				return "(?3+(?2)/1e9*interval '1' second)";
			case QUARTER:
				return "(?3+(?2)*interval '3' month)";
			case WEEK:
				return "(?3+(?2)*interval '7' day)";
			default:
				return "(?3+(?2)*interval '1' ?1)";
		}
	}

	@Override
	public boolean dropConstraints() {
		return false;
	}

	@Override
	public String getCascadeConstraintsString() {
		return " cascade";
	}

	@Override
	public SequenceSupport getSequenceSupport() {
		return MimerSequenceSupport.INSTANCE;
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
	public boolean supportsOuterJoinForUpdate() {
		return false;
	}

	@Override
	public boolean supportsOffsetInSubquery() {
		return true;
	}

	@Override
	public void appendDatetimeFormat(SqlAppender appender, String format) {
		throw new NotYetImplementedFor6Exception("format() function not supported on Mimer SQL");
	}

	@Override
	public boolean useInputStreamToInsertBlob() {
		return false;
	}

	@Override
	public IdentityColumnSupport getIdentityColumnSupport() {
		return new MimerSQLIdentityColumnSupport();
	}
}
