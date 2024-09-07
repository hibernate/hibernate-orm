/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.community.dialect;

import java.sql.Types;
import java.time.temporal.TemporalAccessor;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.hibernate.ScrollMode;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.community.dialect.identity.SQLiteIdentityColumnSupport;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.NationalizationSupport;
import org.hibernate.dialect.Replacer;
import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.dialect.pagination.LimitOffsetLimitHandler;
import org.hibernate.dialect.unique.AlterTableUniqueDelegate;
import org.hibernate.dialect.unique.UniqueDelegate;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.exception.DataException;
import org.hibernate.exception.JDBCConnectionException;
import org.hibernate.exception.LockAcquisitionException;
import org.hibernate.exception.spi.SQLExceptionConversionDelegate;
import org.hibernate.exception.spi.TemplatedViolatedConstraintNameExtractor;
import org.hibernate.exception.spi.ViolatedConstraintNameExtractor;
import org.hibernate.internal.util.JdbcExceptionHelper;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.UniqueKey;
import org.hibernate.query.SemanticException;
import org.hibernate.query.sqm.IntervalType;
import org.hibernate.dialect.NullOrdering;
import org.hibernate.query.common.TemporalUnit;
import org.hibernate.query.sqm.TrimSpec;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.sql.ast.SqlAstNodeRenderingMode;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.spi.StandardSqlAstTranslatorFactory;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.type.BasicType;
import org.hibernate.type.BasicTypeRegistry;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.descriptor.jdbc.BlobJdbcType;
import org.hibernate.type.descriptor.jdbc.ClobJdbcType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;

import jakarta.persistence.TemporalType;

import static org.hibernate.exception.spi.TemplatedViolatedConstraintNameExtractor.extractUsingTemplate;
import static org.hibernate.query.common.TemporalUnit.DAY;
import static org.hibernate.query.common.TemporalUnit.EPOCH;
import static org.hibernate.query.common.TemporalUnit.MONTH;
import static org.hibernate.query.common.TemporalUnit.QUARTER;
import static org.hibernate.query.common.TemporalUnit.YEAR;
import static org.hibernate.query.sqm.produce.function.FunctionParameterType.INTEGER;
import static org.hibernate.query.sqm.produce.function.FunctionParameterType.NUMERIC;
import static org.hibernate.query.sqm.produce.function.FunctionParameterType.STRING;
import static org.hibernate.query.sqm.produce.function.FunctionParameterType.TEMPORAL;
import static org.hibernate.type.SqlTypes.BINARY;
import static org.hibernate.type.SqlTypes.CHAR;
import static org.hibernate.type.SqlTypes.DECIMAL;
import static org.hibernate.type.SqlTypes.FLOAT;
import static org.hibernate.type.SqlTypes.NCHAR;
import static org.hibernate.type.SqlTypes.TIMESTAMP;
import static org.hibernate.type.SqlTypes.TIMESTAMP_WITH_TIMEZONE;
import static org.hibernate.type.SqlTypes.TIME_WITH_TIMEZONE;
import static org.hibernate.type.SqlTypes.VARBINARY;
import static org.hibernate.type.descriptor.DateTimeUtils.appendAsDate;
import static org.hibernate.type.descriptor.DateTimeUtils.appendAsTime;
import static org.hibernate.type.descriptor.DateTimeUtils.appendAsTimestampWithMillis;
import static org.hibernate.type.descriptor.DateTimeUtils.appendAsTimestampWithNanos;
import static org.hibernate.type.descriptor.DateTimeUtils.appendAsTimestampWithMillis;

/**
 * An SQL dialect for SQLite.
 *
 * @author Christian Beikov
 */
public class SQLiteDialect extends Dialect {

	private static final DatabaseVersion DEFAULT_VERSION = DatabaseVersion.make( 2, 0 );

	private final UniqueDelegate uniqueDelegate;

	public SQLiteDialect(DialectResolutionInfo info) {
		this( info.makeCopyOrDefault( DEFAULT_VERSION ) );
		registerKeywords( info );
	}

	public SQLiteDialect() {
		this( DEFAULT_VERSION );
	}

	public SQLiteDialect(DatabaseVersion version) {
		super( version );
		uniqueDelegate = new SQLiteUniqueDelegate( this );
	}

	@Override
	protected String columnType(int sqlTypeCode) {
		switch ( sqlTypeCode ) {
			case DECIMAL:
				return getVersion().isBefore( 3 ) ? columnType( SqlTypes.NUMERIC ) : super.columnType( sqlTypeCode );
			case CHAR:
				return getVersion().isBefore( 3 ) ? "char" : super.columnType( sqlTypeCode );
			case NCHAR:
				return getVersion().isBefore( 3 ) ? "nchar" : super.columnType( sqlTypeCode );
			// No precision support
			case FLOAT:
				return "float";
			case TIMESTAMP:
			case TIMESTAMP_WITH_TIMEZONE:
				return "timestamp";
			case TIME_WITH_TIMEZONE:
				return "time";
			case BINARY:
			case VARBINARY:
				return "blob";
			default:
				return super.columnType( sqlTypeCode );
		}
	}

	@Override
	public int getMaxVarbinaryLength() {
		//no varbinary type
		return -1;
	}

	private static class SQLiteUniqueDelegate extends AlterTableUniqueDelegate {
		public SQLiteUniqueDelegate(Dialect dialect) {
			super( dialect );
		}
		@Override
		public String getColumnDefinitionUniquenessFragment(Column column, SqlStringGenerationContext context) {
			return " unique";
		}

		/**
		 * Alter table support in SQLite is very limited and does
		 * not include adding a unique constraint (as of 9/2023).
		 *
		 * @return always empty String
		 * @see <a href="https://www.sqlite.org/omitted.html">SQLite SQL omissions</a>
		 */
		@Override
		public String getAlterTableToAddUniqueKeyCommand(UniqueKey uniqueKey, Metadata metadata, SqlStringGenerationContext context) {
			return "";
		}

	}

	@Override
	public UniqueDelegate getUniqueDelegate() {
		return uniqueDelegate;
	}

	/**
	 * The {@code extract()} function returns {@link TemporalUnit#DAY_OF_WEEK}
	 * numbered from 0 to 6. This isn't consistent with what most other
	 * databases do, so here we adjust the result by generating
	 * {@code (extract(dow,arg)+1))}.
	 */
	@Override
	public String extractPattern(TemporalUnit unit) {
		switch ( unit ) {
			case SECOND:
				return "cast(strftime('%S.%f',?2) as double)";
			case MINUTE:
				return "strftime('%M',?2)";
			case HOUR:
				return "strftime('%H',?2)";
			case DAY:
			case DAY_OF_MONTH:
				return "(strftime('%d',?2)+1)";
			case MONTH:
				return "strftime('%m',?2)";
			case YEAR:
				return "strftime('%Y',?2)";
			case DAY_OF_WEEK:
				return "(strftime('%w',?2)+1)";
			case DAY_OF_YEAR:
				return "strftime('%j',?2)";
			case EPOCH:
				return "strftime('%s',?2)";
			case WEEK:
				// Thanks https://stackoverflow.com/questions/15082584/sqlite-return-wrong-week-number-for-2013
				return "((strftime('%j',date(?2,'-3 days','weekday 4'))-1)/7+1)";
			default:
				return super.extractPattern(unit);
		}
	}

	@Override
	public String timestampaddPattern(TemporalUnit unit, TemporalType temporalType, IntervalType intervalType) {
		final String function = temporalType == TemporalType.DATE ? "date" : "datetime";
		switch ( unit ) {
			case NANOSECOND:
			case NATIVE:
				return "datetime(?3,'+?2 seconds')";
			case QUARTER: //quarter is not supported in interval literals
				return function + "(?3,'+'||(?2*3)||' months')";
			case WEEK: //week is not supported in interval literals
				return function + "(?3,'+'||(?2*7)||' days')";
			default:
				return function + "(?3,'+?2 ?1s')";
		}
	}

	@Override
	public String timestampdiffPattern(TemporalUnit unit, TemporalType fromTemporalType, TemporalType toTemporalType) {
		final StringBuilder pattern = new StringBuilder();
		switch ( unit ) {
			case YEAR:
				extractField( pattern, YEAR, unit );
				break;
			case QUARTER:
				pattern.append( "(" );
				extractField( pattern, YEAR, unit );
				pattern.append( "+" );
				extractField( pattern, QUARTER, unit );
				pattern.append( ")" );
				break;
			case MONTH:
				pattern.append( "(" );
				extractField( pattern, YEAR, unit );
				pattern.append( "+" );
				extractField( pattern, MONTH, unit );
				pattern.append( ")" );
				break;
			case WEEK: //week is not supported by extract() when the argument is a duration
			case DAY:
				extractField( pattern, DAY, unit );
				break;
			//in order to avoid multiple calls to extract(),
			//we use extract(epoch from x - y) * factor for
			//all the following units:
			case HOUR:
			case MINUTE:
			case SECOND:
			case NANOSECOND:
			case NATIVE:
				extractField( pattern, EPOCH, unit );
				break;
			default:
				throw new SemanticException( "unrecognized field: " + unit );
		}
		return pattern.toString();
	}

	private void extractField(
			StringBuilder pattern,
			TemporalUnit unit,
			TemporalUnit toUnit) {
		final String rhs = extractPattern( unit );
		final String lhs = rhs.replace( "?2", "?3" );
		pattern.append( '(');
		pattern.append( lhs );
		pattern.append( '-' );
		pattern.append( rhs );
		pattern.append(")").append( unit.conversionFactor( toUnit, this ) );
	}

	@Override
	public void initializeFunctionRegistry(FunctionContributions functionContributions) {
		super.initializeFunctionRegistry(functionContributions);

		final BasicTypeRegistry basicTypeRegistry = functionContributions.getTypeConfiguration().getBasicTypeRegistry();
		final BasicType<String> stringType = basicTypeRegistry.resolve( StandardBasicTypes.STRING );
		final BasicType<Integer> integerType = basicTypeRegistry.resolve( StandardBasicTypes.INTEGER );

		CommonFunctionFactory functionFactory = new CommonFunctionFactory(functionContributions);
		functionFactory.mod_operator();
		functionFactory.leftRight_substr();
		functionFactory.concat_pipeOperator();
		functionFactory.characterLength_length( SqlAstNodeRenderingMode.DEFAULT );
		functionFactory.leastGreatest_minMax();

		functionFactory.radians();
		functionFactory.degrees();
		functionFactory.trunc();
		functionFactory.log();
		functionFactory.trim2();
		functionFactory.substr();
		functionFactory.substring_substr();
		functionFactory.chr_char();

		functionContributions.getFunctionRegistry().registerBinaryTernaryPattern(
				"locate",
				integerType,
				"instr(?2,?1)",
				"instr(?2,?1,?3)",
				STRING, STRING, INTEGER,
				functionContributions.getTypeConfiguration()
		).setArgumentListSignature("(pattern, string[, start])");
		functionContributions.getFunctionRegistry().registerBinaryTernaryPattern(
				"lpad",
				stringType,
				"(substr(replace(hex(zeroblob(?2)),'00',' '),1,?2-length(?1))||?1)",
				"(substr(replace(hex(zeroblob(?2)),'00',?3),1,?2-length(?1))||?1)",
				STRING, INTEGER, STRING,
				functionContributions.getTypeConfiguration()
		).setArgumentListSignature("(string, length[, padding])");
		functionContributions.getFunctionRegistry().registerBinaryTernaryPattern(
				"rpad",
				stringType,
				"(?1||substr(replace(hex(zeroblob(?2)),'00',' '),1,?2-length(?1)))",
				"(?1||substr(replace(hex(zeroblob(?2)),'00',?3),1,?2-length(?1)))",
				STRING, INTEGER, STRING,
				functionContributions.getTypeConfiguration()
		).setArgumentListSignature("(string, length[, padding])");

		functionContributions.getFunctionRegistry().namedDescriptorBuilder("format", "strftime")
				.setInvariantType( stringType )
				.setExactArgumentCount( 2 )
				.setParameterTypes(TEMPORAL, STRING)
				.setArgumentListSignature("(TEMPORAL datetime as STRING pattern)")
				.register();

		if (!supportsMathFunctions() ) {
			functionContributions.getFunctionRegistry().patternDescriptorBuilder(
					"floor",
					"(cast(?1 as int)-(?1<cast(?1 as int)))"
			).setReturnTypeResolver( StandardFunctionReturnTypeResolvers.useArgType( 1 ) )
					.setExactArgumentCount( 1 )
					.setParameterTypes(NUMERIC)
					.register();
			functionContributions.getFunctionRegistry().patternDescriptorBuilder(
					"ceiling",
					"(cast(?1 as int)+(?1>cast(?1 as int)))"
			).setReturnTypeResolver( StandardFunctionReturnTypeResolvers.useArgType( 1 ) )
					.setExactArgumentCount( 1 )
					.setParameterTypes(NUMERIC)
					.register();
		}
		functionFactory.windowFunctions();
		functionFactory.listagg_groupConcat();
	}

	@Override
	public String trimPattern(TrimSpec specification, boolean isWhitespace) {
		switch ( specification ) {
			case BOTH:
				return isWhitespace
						? "trim(?1)"
						: "trim(?1,?2)";
			case LEADING:
				return isWhitespace
						? "ltrim(?1)"
						: "ltrim(?1,?2)";
			case TRAILING:
				return isWhitespace
						? "rtrim(?1)"
						: "rtrim(?1,?2)";
		}
		throw new UnsupportedOperationException( "Unsupported specification: " + specification );
	}

	protected boolean supportsMathFunctions() {
		// Math functions have to be enabled through a compile time option: https://www.sqlite.org/lang_mathfunc.html
		return true;
	}

	@Override
	public void contributeTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		super.contributeTypes( typeContributions, serviceRegistry );
		final JdbcTypeRegistry jdbcTypeRegistry = typeContributions.getTypeConfiguration()
				.getJdbcTypeRegistry();
		jdbcTypeRegistry.addDescriptor( Types.BLOB, BlobJdbcType.PRIMITIVE_ARRAY_BINDING );
		jdbcTypeRegistry.addDescriptor( Types.CLOB, ClobJdbcType.STRING_BINDING );
	}

	@Override
	public LimitHandler getLimitHandler() {
		return LimitOffsetLimitHandler.INSTANCE;
	}

	@Override
	public boolean supportsLockTimeouts() {
		// may be http://sqlite.org/c3ref/db_mutex.html ?
		return false;
	}

	@Override
	public String getForUpdateString() {
		return "";
	}

	@Override
	public boolean supportsOuterJoinForUpdate() {
		return false;
	}

	@Override
	public boolean supportsNullPrecedence() {
		return getVersion().isSameOrAfter( 3, 3 );
	}

	@Override
	public NullOrdering getNullOrdering() {
		return NullOrdering.SMALLEST;
	}

	/**
	 * Generated keys are not supported by the (standard) Xerial driver (9/2022).
	 *
	 * @return false
	 */
	@Override
	public boolean getDefaultUseGetGeneratedKeys() {
		return false;
	}

	@Override
	public SqlAstTranslatorFactory getSqlAstTranslatorFactory() {
		return new StandardSqlAstTranslatorFactory() {
			@Override
			protected <T extends JdbcOperation> SqlAstTranslator<T> buildTranslator(
					SessionFactoryImplementor sessionFactory, Statement statement) {
				return new SQLiteSqlAstTranslator<>( sessionFactory, statement );
			}
		};
	}

	private static final int SQLITE_BUSY = 5;
	private static final int SQLITE_LOCKED = 6;
	private static final int SQLITE_IOERR = 10;
	private static final int SQLITE_CORRUPT = 11;
	private static final int SQLITE_NOTFOUND = 12;
	private static final int SQLITE_FULL = 13;
	private static final int SQLITE_CANTOPEN = 14;
	private static final int SQLITE_PROTOCOL = 15;
	private static final int SQLITE_TOOBIG = 18;
	private static final int SQLITE_CONSTRAINT = 19;
	private static final int SQLITE_MISMATCH = 20;
	private static final int SQLITE_NOTADB = 26;

	@Override
	public ViolatedConstraintNameExtractor getViolatedConstraintNameExtractor() {
		return EXTRACTOR;
	}

	private static final ViolatedConstraintNameExtractor EXTRACTOR =
			new TemplatedViolatedConstraintNameExtractor( sqle -> {
				final int errorCode = JdbcExceptionHelper.extractErrorCode( sqle );
				if (errorCode == SQLITE_CONSTRAINT) {
					return extractUsingTemplate( "constraint ", " failed", sqle.getMessage() );
				}
				return null;
			} );

	@Override
	public SQLExceptionConversionDelegate buildSQLExceptionConversionDelegate() {
		return (sqlException, message, sql) -> {
			final int errorCode = JdbcExceptionHelper.extractErrorCode( sqlException );
			switch ( errorCode ) {
				case SQLITE_TOOBIG:
				case SQLITE_MISMATCH:
					return new DataException( message, sqlException, sql );
				case SQLITE_BUSY:
				case SQLITE_LOCKED:
					return new LockAcquisitionException( message, sqlException, sql );
				case SQLITE_NOTADB:
					return new JDBCConnectionException( message, sqlException, sql );
				default:
					if ( errorCode >= SQLITE_IOERR && errorCode <= SQLITE_PROTOCOL ) {
						return new JDBCConnectionException( message, sqlException, sql );
					}
					return null;
			}
		};
	}

	// DDL support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public boolean canCreateSchema() {
		return false;
	}

	@Override
	public boolean hasAlterTable() {
		// As specified in NHibernate dialect
		return false;
	}

	@Override
	public boolean dropConstraints() {
		return false;
	}

	@Override
	public boolean qualifyIndexName() {
		return false;
	}

	@Override
	public String getDropForeignKeyString() {
		throw new UnsupportedOperationException( "No drop foreign key syntax supported by SQLiteDialect" );
	}

	@Override
	public String getAddForeignKeyConstraintString(
			String constraintName,
			String[] foreignKey,
			String referencedTable,
			String[] primaryKey,
			boolean referencesPrimaryKey) {
		throw new UnsupportedOperationException( "No add foreign key syntax supported by SQLiteDialect" );
	}

	@Override
	public String getAddPrimaryKeyConstraintString(String constraintName) {
		throw new UnsupportedOperationException( "No add primary key syntax supported by SQLiteDialect" );
	}

	@Override
	public boolean supportsCommentOn() {
		return true;
	}

	@Override
	public boolean supportsIfExistsBeforeTableName() {
		return true;
	}

	@Override
	public boolean doesReadCommittedCauseWritersToBlockReaders() {
		// TODO Validate (WAL mode...)
		return true;
	}

	@Override
	public boolean doesRepeatableReadCauseReadersToBlockWriters() {
		return true;
	}

	@Override
	public boolean supportsTupleDistinctCounts() {
		return false;
	}

	public int getInExpressionCountLimit() {
		// Compile/runtime time option: http://sqlite.org/limits.html#max_variable_number
		return 1000;
	}

	@Override
	public boolean supportsWindowFunctions() {
		return true;
	}

	@Override
	public IdentityColumnSupport getIdentityColumnSupport() {
		return SQLiteIdentityColumnSupport.INSTANCE;
	}

	@Override
	public String getSelectGUIDString() {
		return "select hex(randomblob(16))";
	}

	@Override
	public ScrollMode defaultScrollMode() {
		return ScrollMode.FORWARD_ONLY;
	}

	@Override
	public String getNoColumnsInsertString() {
		return "default values";
	}

	@Override
	public NationalizationSupport getNationalizationSupport() {
		return NationalizationSupport.IMPLICIT;
	}

	@Override
	public String currentDate() {
		return "date('now')";
	}

	@Override
	public String currentTime() {
		return "time('now')";
	}

	@Override
	public String currentTimestamp() {
		return "datetime('now')";
	}

	@Override
	public void appendDatetimeFormat(SqlAppender appender, String format) {
		appender.appendSql( datetimeFormat( format ).result() );
	}

	public static Replacer datetimeFormat(String format) {
		return new Replacer( format, "'", "" )
				.replace("%", "%%")

				//year
				.replace("yyyy", "%Y")
				.replace("yyy", "%Y")
				.replace("yy", "%y") //?????
				.replace("y", "%y") //?????

				//month of year
				.replace("MMMM", "%B") //?????
				.replace("MMM", "%b") //?????
				.replace("MM", "%m")
				.replace("M", "%m") //?????

				//day of week
				.replace("EEEE", "%A") //?????
				.replace("EEE", "%a") //?????
				.replace("ee", "%w")
				.replace("e", "%w") //?????

				//day of month
				.replace("dd", "%d")
				.replace("d", "%d") //?????

				//am pm
				.replace("a", "%p") //?????

				//hour
				.replace("hh", "%I") //?????
				.replace("HH", "%H")
				.replace("h", "%I") //?????
				.replace("H", "%H") //?????

				//minute
				.replace("mm", "%M")
				.replace("m", "%M") //?????

				//second
				.replace("ss", "%S")
				.replace("s", "%S") //?????

				//fractional seconds
				.replace("SSSSSS", "%f") //5 is the max
				.replace("SSSSS", "%f")
				.replace("SSSS", "%f")
				.replace("SSS", "%f")
				.replace("SS", "%f")
				.replace("S", "%f");
	}

	@Override
	public String translateExtractField(TemporalUnit unit) {
		// All units should be handled in extractPattern so we should never hit this method
		throw new UnsupportedOperationException( "Unsupported unit: " + unit );
	}

	@Override
	public void appendDateTimeLiteral(
			SqlAppender appender,
			TemporalAccessor temporalAccessor,
			TemporalType precision,
			TimeZone jdbcTimeZone) {
		switch ( precision ) {
			case DATE:
				appender.appendSql( "date(" );
				appendAsDate( appender, temporalAccessor );
				appender.appendSql( ')' );
				break;
			case TIME:
				appender.appendSql( "time(" );
				appendAsTime( appender, temporalAccessor, supportsTemporalLiteralOffset(), jdbcTimeZone );
				appender.appendSql( ')' );
				break;
			case TIMESTAMP:
				appender.appendSql( "datetime(" );
				appendAsTimestampWithNanos( appender, temporalAccessor, supportsTemporalLiteralOffset(), jdbcTimeZone );
				appender.appendSql( ')' );
				break;
			default:
				throw new IllegalArgumentException();
		}
	}

	@Override
	public void appendDateTimeLiteral(SqlAppender appender, Date date, TemporalType precision, TimeZone jdbcTimeZone) {
		switch ( precision ) {
			case DATE:
				appender.appendSql( "date(" );
				appendAsDate( appender, date );
				appender.appendSql( ')' );
				break;
			case TIME:
				appender.appendSql( "time(" );
				appendAsTime( appender, date );
				appender.appendSql( ')' );
				break;
			case TIMESTAMP:
				appender.appendSql( "datetime(" );
				appendAsTimestampWithNanos( appender, date, jdbcTimeZone );
				appender.appendSql( ')' );
				break;
			default:
				throw new IllegalArgumentException();
		}
	}

	@Override
	public void appendDateTimeLiteral(
			SqlAppender appender,
			Calendar calendar,
			TemporalType precision,
			TimeZone jdbcTimeZone) {
		switch ( precision ) {
			case DATE:
				appender.appendSql( "date(" );
				appendAsDate( appender, calendar );
				appender.appendSql( ')' );
				break;
			case TIME:
				appender.appendSql( "time(" );
				appendAsTime( appender, calendar );
				appender.appendSql( ')' );
				break;
			case TIMESTAMP:
				appender.appendSql( "datetime(" );
				appendAsTimestampWithMillis( appender, calendar, jdbcTimeZone );
				appender.appendSql( ')' );
				break;
			default:
				throw new IllegalArgumentException();
		}
	}

}
