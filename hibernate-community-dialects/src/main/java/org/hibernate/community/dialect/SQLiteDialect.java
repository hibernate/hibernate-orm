/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import org.hibernate.dialect.schema.spi.ConstraintDropMode;
import org.hibernate.dialect.schema.spi.ExistenceCheckPlacement;
import org.hibernate.dialect.schema.spi.IfExistsSupport;
import org.hibernate.dialect.schema.spi.IndexNameQualification;
import org.hibernate.dialect.schema.spi.SchemaDropSupport;

import org.hibernate.dialect.temporaltype.spi.TemporalOperationSupport;
import org.hibernate.dialect.temporaltype.spi.TemporalOperationSupports;

import org.hibernate.dialect.temporaltype.spi.TemporalFormatSupport;

import org.hibernate.dialect.temporaltype.spi.CurrentTemporalSupport;

import org.hibernate.SPI;
import static org.hibernate.SPI.Role.USE;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.SUPPLY;
import org.hibernate.dialect.type.spi.TypeSizingProfile;

import org.hibernate.dialect.function.spi.TupleCountSupport;
import org.hibernate.dialect.function.spi.WindowFunctionSupport;

import jakarta.persistence.TemporalType;
import org.hibernate.LockOptions;
import org.hibernate.ScrollMode;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.community.dialect.identity.internal.SQLiteIdentityColumnSupport;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.jdbc.spi.ParameterLimits;
import org.hibernate.dialect.type.spi.NationalizationSupport;
import org.hibernate.dialect.sql.ast.spi.NullOrdering;
import org.hibernate.dialect.sql.ast.spi.NullOrderingSupport;
import org.hibernate.dialect.function.spi.Replacer;
import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.dialect.identity.spi.IdentityColumnSupport;
import org.hibernate.dialect.lock.spi.LockingSupport;
import org.hibernate.dialect.lock.spi.StandardLockingClauseStrategies;
import org.hibernate.dialect.lock.spi.StandardLockingSupports;
import org.hibernate.dialect.namespace.spi.NamespaceSupport;
import org.hibernate.dialect.namespace.spi.NamespaceSupports;
import org.hibernate.dialect.pagination.spi.LimitHandler;
import org.hibernate.dialect.pagination.spi.LimitOffsetLimitHandler;
import org.hibernate.dialect.unique.spi.DelegatingUniqueDelegate;
import org.hibernate.dialect.unique.spi.UniqueDelegates;
import org.hibernate.dialect.unique.spi.UniqueDelegate;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.exception.DataException;
import org.hibernate.exception.JDBCConnectionException;
import org.hibernate.exception.LockAcquisitionException;
import org.hibernate.exception.spi.SQLExceptionConversionDelegate;
import org.hibernate.exception.spi.TemplatedViolatedConstraintNameExtractor;
import org.hibernate.exception.spi.ViolatedConstraintNameExtractor;
import org.hibernate.jdbc.spi.JdbcExceptionHelper;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.UniqueKey;
import org.hibernate.query.SemanticException;
import org.hibernate.query.common.TemporalUnit;
import org.hibernate.dialect.temporaltype.spi.IntervalType;
import org.hibernate.query.sqm.TrimSpec;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.sql.ast.spi.translation.SqlAstNodeRenderingMode;
import org.hibernate.sql.ast.spi.translation.SqlAstTranslator;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslatorFactory;
import org.hibernate.dialect.lock.spi.LockingClauseStrategy;
import org.hibernate.sql.spi.SqlAppender;
import org.hibernate.dialect.sql.ast.spi.StandardSqlAstTranslatorFactory;
import org.hibernate.sql.ast.spi.Statement;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslationRequest;
import org.hibernate.sql.ast.spi.query.select.QuerySpec;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.type.BasicType;
import org.hibernate.type.BasicTypeRegistry;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.descriptor.jdbc.BlobJdbcType;
import org.hibernate.type.descriptor.jdbc.ClobJdbcType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;

import java.sql.Types;
import java.time.temporal.TemporalAccessor;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

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
import static org.hibernate.dialect.literal.spi.StandardDateTimeLiteralRendering.appendAsDate;
import static org.hibernate.dialect.literal.spi.StandardDateTimeLiteralRendering.appendAsLocalTime;
import static org.hibernate.dialect.literal.spi.StandardDateTimeLiteralRendering.appendAsTime;
import static org.hibernate.dialect.literal.spi.StandardDateTimeLiteralRendering.appendAsTimestampWithMillis;
import static org.hibernate.dialect.literal.spi.StandardDateTimeLiteralRendering.appendAsTimestampWithNanos;

/**
 * An SQL dialect for SQLite.
 *
 * @author Christian Beikov
 */
public class SQLiteDialect extends Dialect implements CurrentTemporalSupport, TemporalFormatSupport, TemporalOperationSupport {
	private IfExistsSupport ifExistsSupport;
	private SchemaDropSupport schemaDropSupport;


	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public TemporalOperationSupport getTemporalOperationSupport() {
		return this;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public TemporalFormatSupport getTemporalFormatSupport() {
		return this;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public CurrentTemporalSupport getCurrentTemporalSupport() {
		return this;
	}
	private final TypeSizingProfile typeSizingProfile = TypeSizingProfile.builder( super.getTypeSizingProfile() )
			.maxVarbinaryLength( TypeSizingProfile.UNSUPPORTED )
			.maxVarbinaryCapacity( TypeSizingProfile.UNSUPPORTED )
			.build();

	@Override public TypeSizingProfile getTypeSizingProfile() { return typeSizingProfile; }

	private static final DatabaseVersion DEFAULT_VERSION = DatabaseVersion.make( 2, 0 );

	private final UniqueDelegate uniqueDelegate;

	public SQLiteDialect(DialectResolutionInfo info) {
		this( info.makeCopyOrDefault( DEFAULT_VERSION ) );
	}

	public SQLiteDialect() {
		this( DEFAULT_VERSION );
	}

	public SQLiteDialect(DatabaseVersion version) {
		super( version );
		uniqueDelegate = new SQLiteUniqueDelegate( UniqueDelegates.alterTable( this ) );
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	protected String columnType(int sqlTypeCode) {
		return switch ( sqlTypeCode ) {
			case DECIMAL -> getVersion().isBefore( 3 ) ? columnType( SqlTypes.NUMERIC ) : super.columnType( sqlTypeCode );
			case CHAR -> getVersion().isBefore( 3 ) ? "char" : super.columnType( sqlTypeCode );
			case NCHAR -> getVersion().isBefore( 3 ) ? "nchar" : super.columnType( sqlTypeCode );
			// No precision support
			case FLOAT -> "float";
			case TIMESTAMP, TIMESTAMP_WITH_TIMEZONE -> "timestamp";
			case TIME_WITH_TIMEZONE -> "time";
			case BINARY, VARBINARY -> "blob";
			default ->  super.columnType( sqlTypeCode );
		};
	}

	private static class SQLiteUniqueDelegate extends DelegatingUniqueDelegate {
		public SQLiteUniqueDelegate(UniqueDelegate delegate) {
			super( delegate );
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
	@SPI({ IMPLEMENT, SUPPLY })
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
	@SPI({ USE, IMPLEMENT })
	public String extractPattern(TemporalUnit unit) {
		return switch ( unit ) {
			case SECOND -> "cast(strftime('%S.%f',?2) as double)";
			case MINUTE -> "strftime('%M',?2)";
			case HOUR -> "strftime('%H',?2)";
			case DAY, DAY_OF_MONTH -> "(strftime('%d',?2)+1)";
			case MONTH -> "strftime('%m',?2)";
			case YEAR -> "strftime('%Y',?2)";
			case DAY_OF_WEEK -> "(strftime('%w',?2)+1)";
			case DAY_OF_YEAR -> "strftime('%j',?2)";
			case EPOCH -> "strftime('%s',?2)";
			// Thanks https://stackoverflow.com/questions/15082584/sqlite-return-wrong-week-number-for-2013
			case WEEK -> "((strftime('%j',date(?2,'-3 days','weekday 4'))-1)/7+1)";
			default -> TemporalOperationSupports.standard().extractPattern(unit);
		};
	}

	@Override
	@SPI({ USE, IMPLEMENT })
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
	@SPI({ USE, IMPLEMENT })
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
	@SPI({ IMPLEMENT, SUPPLY })
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
		functionFactory.regexpLike_regexp();
	}

	@Override
	@SPI({ USE, IMPLEMENT })
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
	@SPI({ IMPLEMENT, SUPPLY })
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
	public LockingSupport getLockingSupport() {
		return StandardLockingSupports.none();
	}

	@Override
	public LockingClauseStrategy getLockingClauseStrategy(QuerySpec querySpec, LockOptions lockOptions) {
		// SQLite does not support the FOR UPDATE clause
		return StandardLockingClauseStrategies.none();
	}


	@Override
	public NullOrderingSupport getNullOrderingSupport() {
		return NullOrderingSupport.builder( super.getNullOrderingSupport() )
				.defaultOrdering( NullOrdering.SMALLEST )
				.capability(
						NullOrderingSupport.Capability.NULLS_FIRST_LAST,
						getVersion().isSameOrAfter( 3, 3 )
				)
				.build();
	}

	/**
	 * Generated keys are not supported by the (standard) Xerial driver (9/2022).
	 */
	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	protected void contributeDefaultProperties(java.util.Properties properties) {
		super.contributeDefaultProperties( properties );
		properties.setProperty( org.hibernate.cfg.AvailableSettings.USE_GET_GENERATED_KEYS, "false" );
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public SqlAstTranslatorFactory getSqlAstTranslatorFactory() {
		return new StandardSqlAstTranslatorFactory() {
			@Override
			protected <S extends Statement, T extends JdbcOperation> SqlAstTranslator<T> createTranslator(
					SqlAstTranslationRequest<S, T> request) {
				return new SQLiteSqlAstTranslator<>( request );
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
	@SPI({ IMPLEMENT, SUPPLY })
	public ViolatedConstraintNameExtractor getViolatedConstraintNameExtractor() {
		return EXTRACTOR;
	}

	private static final ViolatedConstraintNameExtractor EXTRACTOR =
			new TemplatedViolatedConstraintNameExtractor( sqle -> {
				final int errorCode = JdbcExceptionHelper.extractErrorCode( sqle ) & 0xFF;
				if (errorCode == SQLITE_CONSTRAINT) {
					return extractUsingTemplate( "constraint failed: ", "\n", sqle.getMessage() );
				}
				return null;
			} );

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
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
	@SPI({ IMPLEMENT, SUPPLY })
	public NamespaceSupport getNamespaceSupport() {
		return NamespaceSupports.none();
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public boolean supportsAlterTableConstraints() {
		// As specified in NHibernate dialect
		return false;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public synchronized SchemaDropSupport getSchemaDropSupport() {
		if ( schemaDropSupport == null ) {
			schemaDropSupport = new SchemaDropSupport( java.util.List.of(), ConstraintDropMode.IMPLICIT, "" );
		}
		return schemaDropSupport;
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public IndexNameQualification nameQualification() {
		return IndexNameQualification.UNQUALIFIED;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public org.hibernate.dialect.schema.spi.SchemaCommentSupport getSchemaCommentSupport() {
		return org.hibernate.dialect.schema.spi.SchemaCommentSupports.commentOn();
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public synchronized IfExistsSupport getIfExistsSupport() {
		if ( ifExistsSupport == null ) {
			ifExistsSupport = new IfExistsSupport(
				ExistenceCheckPlacement.NONE,
				ExistenceCheckPlacement.BEFORE_NAME,
				ExistenceCheckPlacement.NONE,
				ExistenceCheckPlacement.NONE
		);
		}
		return ifExistsSupport;
	}

	@Override
	public TupleCountSupport getTupleCountSupport() {
		return TupleCountSupport.NONE;
	}

	@Override
	public ParameterLimits getParameterLimits() {
		// Compile/runtime time option: http://sqlite.org/limits.html#max_variable_number
		return ParameterLimits.of( 1000 );
	}

	@Override
	public WindowFunctionSupport getWindowFunctionSupport() {
		if ( getVersion().isBefore( 3, 25 ) ) {
			return WindowFunctionSupport.NONE;
		}
		final WindowFunctionSupport.Builder builder = WindowFunctionSupport.builder()
				.features(
						WindowFunctionSupport.Feature.WINDOW_FUNCTIONS,
						WindowFunctionSupport.Feature.PARTITION_BY,
						WindowFunctionSupport.Feature.ROWS_FRAME,
						WindowFunctionSupport.Feature.RANGE_FRAME
				);
		if ( getVersion().isSameOrAfter( 3, 28 ) ) {
			builder.features(
					WindowFunctionSupport.Feature.GROUPS_FRAME,
					WindowFunctionSupport.Feature.FRAME_EXCLUSION
			);
		}
		return builder.build();
	}

	@Override
	public IdentityColumnSupport getIdentityColumnSupport() {
		return SQLiteIdentityColumnSupport.INSTANCE;
	}

	@Override
	@SPI({ USE, IMPLEMENT, SUPPLY })
	public ScrollMode defaultScrollMode() {
		return ScrollMode.FORWARD_ONLY;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public NationalizationSupport getNationalizationSupport() {
		return NationalizationSupport.IMPLICIT;
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String currentDate() {
		return "date('now')";
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String currentTime() {
		return "time('now')";
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String currentTimestamp() {
		return "datetime('now')";
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public void appendFormat(SqlAppender appender, String format) {
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
	@SPI({ USE, IMPLEMENT })
	public String translateExtractField(TemporalUnit unit) {
		// All units should be handled in extractPattern so we should never hit this method
		throw new UnsupportedOperationException( "Unsupported unit: " + unit );
	}

	@Override
	@SPI({ USE, IMPLEMENT })
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
				appendAsTime( appender, temporalAccessor, getTemporalValueSemantics().supportsLiteralOffset(), jdbcTimeZone );
				appender.appendSql( ')' );
				break;
			case TIMESTAMP:
				appender.appendSql( "datetime(" );
				appendAsTimestampWithNanos( appender, temporalAccessor, getTemporalValueSemantics().supportsLiteralOffset(), jdbcTimeZone );
				appender.appendSql( ')' );
				break;
			default:
				throw new IllegalArgumentException();
		}
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public void appendDateTimeLiteral(SqlAppender appender, Date date, TemporalType precision, TimeZone jdbcTimeZone) {
		switch ( precision ) {
			case DATE:
				appender.appendSql( "date(" );
				appendAsDate( appender, date );
				appender.appendSql( ')' );
				break;
			case TIME:
				appender.appendSql( "time(" );
				appendAsLocalTime( appender, date );
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
	@SPI({ USE, IMPLEMENT })
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
				appendAsLocalTime( appender, calendar );
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

	@Override
	public boolean supportsFilterClause() {
		return getVersion().isSameOrAfter( 3, 3 );
	}

}
