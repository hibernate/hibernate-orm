/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.community.dialect;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.TypeContributions;
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
import org.hibernate.type.descriptor.sql.internal.BinaryFloatDdlType;
import org.hibernate.type.descriptor.sql.internal.CapacityDependentDdlType;
import org.hibernate.type.descriptor.sql.spi.DdlTypeRegistry;

import jakarta.persistence.TemporalType;

import static org.hibernate.dialect.SimpleDatabaseVersion.ZERO_VERSION;
import static org.hibernate.type.SqlTypes.BLOB;
import static org.hibernate.type.SqlTypes.CHAR;
import static org.hibernate.type.SqlTypes.CLOB;
import static org.hibernate.type.SqlTypes.LONG32NVARCHAR;
import static org.hibernate.type.SqlTypes.LONG32VARCHAR;
import static org.hibernate.type.SqlTypes.NCHAR;
import static org.hibernate.type.SqlTypes.NCLOB;
import static org.hibernate.type.SqlTypes.NVARCHAR;
import static org.hibernate.type.SqlTypes.TIMESTAMP;
import static org.hibernate.type.SqlTypes.TIMESTAMP_WITH_TIMEZONE;
import static org.hibernate.type.SqlTypes.TINYINT;
import static org.hibernate.type.SqlTypes.VARCHAR;

/**
 * A dialect for Mimer SQL 11.
 *
 * @author Fredrik lund
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
		super( DatabaseVersion.make( 11 ) );
	}

	public MimerSQLDialect(DialectResolutionInfo info) {
		super( info );
	}

	@Override
	protected String columnType(int sqlTypeCode) {
		switch ( sqlTypeCode ) {
			//no 'tinyint', so use integer with 3 decimal digits
			case TINYINT:
				return "integer(3)";
			case TIMESTAMP_WITH_TIMEZONE:
				return columnType( TIMESTAMP );
			//Mimer CHARs are ASCII!!
			case CHAR:
				return columnType( NCHAR );
			case VARCHAR:
				return columnType( NVARCHAR );
			case LONG32VARCHAR:
				return columnType( LONG32NVARCHAR );
			//default length is 1M, which is quite low
			case BLOB:
				return "blob(2G)";
			case CLOB:
			case NCLOB:
				return "nclob(2G)";
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

		//Mimer CHARs are ASCII!!
		ddlTypeRegistry.addDescriptor(
				CapacityDependentDdlType.builder(
								VARCHAR,
								isLob( LONG32VARCHAR ) ?
										CapacityDependentDdlType.LobKind.BIGGEST_LOB :
										CapacityDependentDdlType.LobKind.NONE,
								columnType( LONG32VARCHAR ),
								"nvarchar(" + getMaxNVarcharLength() + ")",
								this
						)
						.withTypeCapacity( getMaxNVarcharLength(), columnType( VARCHAR ) )
						.build()
		);
	}

//	@Override
//	public int getDefaultDecimalPrecision() {
//		//the maximum, but I guess it's too high
//		return 45;
//	}

	@Override
	public int getMaxVarbinaryLength() {
		return 15000;
	}

	@Override
	public int getMaxVarcharLength() {
		return 15000;
	}

	@Override
	public int getMaxNVarcharLength() {
		return 5000;
	}

	@Override
	public DatabaseVersion getVersion() {
		return ZERO_VERSION;
	}

	@Override
	public int getDefaultStatementBatchSize() {
		return 50;
	}

	@Override
	public void initializeFunctionRegistry(FunctionContributions functionContributions) {
		super.initializeFunctionRegistry(functionContributions);

		CommonFunctionFactory functionFactory = new CommonFunctionFactory(functionContributions);
		functionFactory.soundex();
		functionFactory.octetLength();
		functionFactory.bitLength();
		functionFactory.trunc_truncate();
		functionFactory.repeat();
		functionFactory.pad_repeat();
		functionFactory.dayofweekmonthyear();
		functionFactory.concat_pipeOperator();
		functionFactory.position();
		functionFactory.localtimeLocaltimestamp();
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
		throw new UnsupportedOperationException("format() function not supported on Mimer SQL");
	}

	@Override
	public boolean useInputStreamToInsertBlob() {
		return false;
	}

	@Override
	public boolean useConnectionToCreateLob() {
		return false;
	}

	@Override
	public IdentityColumnSupport getIdentityColumnSupport() {
		return MimerSQLIdentityColumnSupport.INSTANCE;
	}
}
