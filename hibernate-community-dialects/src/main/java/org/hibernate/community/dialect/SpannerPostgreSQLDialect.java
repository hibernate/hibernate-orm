/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import jakarta.persistence.TemporalType;
import jakarta.persistence.Timeout;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.LockOptions;
import org.hibernate.JDBCException;
import org.hibernate.ScrollMode;
import org.hibernate.Timeouts;
import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.community.dialect.aggregate.SpannerPostgreSQLAggregateSupport;
import org.hibernate.community.dialect.sequence.SpannerPostgreSQLSequenceSupport;
import org.hibernate.community.dialect.sql.ast.SpannerPostgreSQLSqlAstTranslator;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.FunctionalDependencyAnalysisSupport;
import org.hibernate.dialect.FunctionalDependencyAnalysisSupportImpl;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.RowLockStrategy;
import org.hibernate.dialect.aggregate.AggregateSupport;
import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.dialect.function.SpannerConcatFunction;
import org.hibernate.dialect.function.SpannerPostgreSQLRegexpLikeFunction;
import org.hibernate.dialect.lock.PessimisticLockStyle;
import org.hibernate.dialect.lock.internal.LockingSupportSimple;
import org.hibernate.dialect.lock.spi.ConnectionLockTimeoutStrategy;
import org.hibernate.dialect.lock.spi.LockTimeoutType;
import org.hibernate.dialect.lock.spi.LockingSupport;
import org.hibernate.dialect.lock.spi.OuterJoinLockingType;
import org.hibernate.dialect.sequence.SequenceSupport;
import org.hibernate.dialect.temptable.PersistentTemporaryTableStrategy;
import org.hibernate.dialect.temptable.TemporaryTableStrategy;
import org.hibernate.dialect.type.PostgreSQLArrayJdbcTypeConstructor;
import org.hibernate.dialect.type.PostgreSQLCastingJsonArrayJdbcTypeConstructor;
import org.hibernate.dialect.type.PostgreSQLCastingJsonJdbcType;
import org.hibernate.dialect.type.PostgreSQLUUIDJdbcType;
import org.hibernate.dialect.unique.AlterTableUniqueIndexDelegate;
import org.hibernate.dialect.unique.UniqueDelegate;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.config.spi.StandardConverters;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.spi.SQLExceptionConversionDelegate;
import org.hibernate.procedure.internal.StandardCallableStatementSupport;
import org.hibernate.procedure.spi.CallableStatementSupport;
import org.hibernate.query.common.FetchClauseType;
import org.hibernate.query.common.TemporalUnit;
import org.hibernate.query.sqm.IntervalType;
import org.hibernate.query.sqm.mutation.internal.temptable.PersistentTableInsertStrategy;
import org.hibernate.query.sqm.mutation.internal.temptable.PersistentTableMutationStrategy;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableInsertStrategy;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableMutationStrategy;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.spi.LockingClauseStrategy;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.spi.StandardSqlAstTranslatorFactory;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.tool.schema.internal.StandardTableExporter;
import org.hibernate.type.descriptor.jdbc.SpannerTimeJdbcType;
import org.hibernate.type.descriptor.sql.internal.ArrayDdlTypeImpl;
import org.hibernate.type.descriptor.sql.internal.CapacityDependentDdlType;
import org.hibernate.type.descriptor.sql.internal.DdlTypeImpl;
import org.hibernate.type.spi.TypeConfiguration;

import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.regex.Pattern;

import static org.hibernate.sql.ast.internal.NonLockingClauseStrategy.NON_CLAUSE_STRATEGY;
import static org.hibernate.type.SqlTypes.BIGINT;
import static org.hibernate.type.SqlTypes.BLOB;
import static org.hibernate.type.SqlTypes.CHAR;
import static org.hibernate.type.SqlTypes.CLOB;
import static org.hibernate.type.SqlTypes.DECIMAL;
import static org.hibernate.type.SqlTypes.FLOAT;
import static org.hibernate.type.SqlTypes.INTEGER;
import static org.hibernate.type.SqlTypes.JSON;
import static org.hibernate.type.SqlTypes.NCLOB;
import static org.hibernate.type.SqlTypes.NUMERIC;
import static org.hibernate.type.SqlTypes.SMALLINT;
import static org.hibernate.type.SqlTypes.TIME;
import static org.hibernate.type.SqlTypes.TIMESTAMP;
import static org.hibernate.type.SqlTypes.TIMESTAMP_UTC;
import static org.hibernate.type.SqlTypes.TIMESTAMP_WITH_TIMEZONE;
import static org.hibernate.type.SqlTypes.TIME_UTC;
import static org.hibernate.type.SqlTypes.TINYINT;
import static org.hibernate.type.SqlTypes.UUID;
import static org.hibernate.type.SqlTypes.VARCHAR;

public class SpannerPostgreSQLDialect extends PostgreSQLDialect {

	private final UniqueDelegate SPANNER_UNIQUE_DELEGATE = new AlterTableUniqueIndexDelegate( this );
	private final StandardTableExporter SPANNER_TABLE_EXPORTER = new SpannerPostgreSQLTableExporter( this );
	private final SequenceSupport SPANNER_SEQUENCE_SUPPORT = new SpannerPostgreSQLSequenceSupport(this);

	// This will use a monotonically increasing value that is within the range of a 32-bit integer
	// as the primary key value. Since Spanner only supports bit-reversed sequences, this option
	// range of a 32-bit integer.
	// This workaround that is only intended for testing, and should not be used for primary key
	// values in production.
	private static final String USE_INTEGER_FOR_PRIMARY_KEY = "hibernate.dialect.spannerpg.use_integer_for_primary_key";

	private boolean useIntegerForPrimaryKey;

	private final LockingSupport SPANNER_LOCKING_SUPPORT =
			new LockingSupportSimple(
					PessimisticLockStyle.NONE,
					RowLockStrategy.NONE,
					LockTimeoutType.NONE,
					OuterJoinLockingType.FULL,
					ConnectionLockTimeoutStrategy.NONE );

	protected final static DatabaseVersion MINIMUM_POSTGRES_VERSION = DatabaseVersion.make( 15 );

	private static final Pattern NOT_NULL_CONSTRAINT_PATTERN = Pattern.compile( ".*(must not be NULL in table|does not specify a non-null value for NOT NULL column|Cannot specify a null value for column).*" );
	private static final Pattern FOREIGN_KEY_CONSTRAINT_PATTERN = Pattern.compile( ".*Foreign key.*(constraint violation on table|constraint violation when deleting or updating referenced key|violated on table).*" );
	private static final Pattern CHECK_CONSTRAINT_PATTERN = Pattern.compile( ".*Check constraint.*" );

	public SpannerPostgreSQLDialect() {
		super();
	}

	public SpannerPostgreSQLDialect(DialectResolutionInfo info) {
		super( info );
	}

	public SpannerPostgreSQLDialect(DatabaseVersion version) {
		super( MINIMUM_POSTGRES_VERSION );
	}

	@Override
	public void initializeFunctionRegistry(FunctionContributions functionContributions) {
		super.initializeFunctionRegistry( functionContributions );

		CommonFunctionFactory functionFactory = new CommonFunctionFactory( functionContributions );
		functionFactory.substring_spanner();
		functionFactory.leftRight_substr();

		var functionRegistry = functionContributions.getFunctionRegistry();
		functionRegistry.register( "concat",
				new SpannerConcatFunction( functionContributions.getTypeConfiguration()) );
		functionRegistry.register( "regexp_like",
				new SpannerPostgreSQLRegexpLikeFunction(functionContributions.getTypeConfiguration()));
	}

	@Override
	protected void registerJsonFunction(CommonFunctionFactory functionFactory) {
	}

	@Override
	protected void registerArrayFunctions(CommonFunctionFactory functionFactory) {
	}

	@Override
	protected void registerXmlFunctions(CommonFunctionFactory functionFactory) {
	}

	@Override
	protected void registerUtilityFunctions(FunctionContributions functionContributions) {
	}

	@Override
	protected void initDefaultProperties() {
		super.initDefaultProperties();
		getDefaultProperties().setProperty( AvailableSettings.PREFERRED_POOLED_OPTIMIZER, "none" );
	}

	@Override
	public StandardTableExporter getTableExporter() {
		return SPANNER_TABLE_EXPORTER;
	}

	@Override
	public UniqueDelegate getUniqueDelegate() {
		return SPANNER_UNIQUE_DELEGATE;
	}

	@Override
	public SequenceSupport getSequenceSupport() {
		return SPANNER_SEQUENCE_SUPPORT;
	}

	@Override
	public String getCheckCondition(String columnName, String[] values) {
		final StringBuilder check = new StringBuilder();
		check.append("(");
		String separator = "";
		boolean nullIsValid = false;
		for (String value : values) {
			if (value == null) {
				nullIsValid = true;
				continue;
			}
			check.append(separator).append(columnName).append("='").append(value).append("'");
			separator = " or ";
		}
		check.append(")");
		if (nullIsValid) {
			check.append(" or ").append(columnName).append(" is null");
		}
		return check.toString();
	}

	@Override
	public String getCheckCondition(String columnName, Long[] values) {
		final StringBuilder check = new StringBuilder();
		check.append("(");
		String separator = "";
		boolean nullIsValid = false;
		for (Long value : values) {
			if (value == null) {
				nullIsValid = true;
				continue;
			}
			check.append(separator).append(columnName).append("=").append(value);
			separator = " or ";
		}
		check.append(")");
		if (nullIsValid) {
			check.append(" or ").append(columnName).append(" is null");
		}
		return check.toString();
	}

	@Override
	public String getCheckCondition(String columnName, java.util.Collection<?> valueSet,
			org.hibernate.type.descriptor.jdbc.JdbcType jdbcType) {
		final boolean isCharacterJdbcType = org.hibernate.type.SqlTypes.isCharacterType(jdbcType.getJdbcTypeCode());

		final StringBuilder check = new StringBuilder();
		check.append("(");
		String separator = "";
		boolean nullIsValid = false;
		for (Object value : valueSet) {
			if (value == null) {
				nullIsValid = true;
				continue;
			}
			check.append(separator).append(columnName).append("=");
			if (isCharacterJdbcType) {
				check.append("'").append(String.valueOf(value).replace("'", "''")).append("'");
			}
			else {
				check.append(value);
			}
			separator = " or ";
		}
		check.append(")");
		if (nullIsValid) {
			check.append(" or ").append(columnName).append(" is null");
		}
		return check.toString();
	}

	@Override
	public String getCheckCondition(String columnName, long[] values) {
		final Long[] boxedValues = new Long[values.length];
		for (int i = 0; i < values.length; i++) {
			boxedValues[i] = values[i];
		}
		return getCheckCondition(columnName, boxedValues);
	}

	@Override
	public AggregateSupport getAggregateSupport() {
		return SpannerPostgreSQLAggregateSupport.INSTANCE;
	}

	@Override
	public LockingSupport getLockingSupport() {
		return SPANNER_LOCKING_SUPPORT;
	}

	@Override
	protected Integer resolveSqlTypeCode(String columnTypeName, TypeConfiguration typeConfiguration) {
		return switch (columnTypeName) {
			case "character varying" -> Types.VARCHAR;
			case "timestamp with time zone" -> Types.TIMESTAMP_WITH_TIMEZONE;
			case "bigint" -> Types.BIGINT;
			default -> super.resolveSqlTypeCode( columnTypeName, typeConfiguration );
		};
	}

	@Override
	public boolean supportsFetchClause(FetchClauseType type) {
		return false;
	}

	@Override
	public String getForUpdateString() {
		return " for update";
	}

	@Override
	public String getForUpdateString(String aliases) {
		return getForUpdateString();
	}

	@Override
	public String getWriteLockString(int timeout) {
		validateSpannerLockTimeout( timeout );
		return getForUpdateString();
	}

	@Override
	public String getWriteLockString(String aliases, int timeout) {
		return getWriteLockString( timeout );
	}

	@Override
	public String getWriteLockString(Timeout timeout) {
		return getWriteLockString( timeout.milliseconds() );
	}

	@Override
	public String getWriteLockString(String aliases, Timeout timeout) {
		return getWriteLockString( aliases, timeout.milliseconds() );
	}

	@Override
	public String getReadLockString(int timeout) {
		validateSpannerLockTimeout( timeout );
		return getForUpdateString();
	}

	@Override
	public String getReadLockString(Timeout timeout) {
		return getWriteLockString( timeout.milliseconds() );
	}

	@Override
	public String getReadLockString(String aliases, Timeout timeout) {
		return getWriteLockString( timeout.milliseconds() );
	}

	@Override
	public String getReadLockString(String aliases, int timeout) {
		return getWriteLockString( timeout );
	}

	@Override
	public String getForUpdateNowaitString() {
		throw new UnsupportedOperationException(
				"Spanner doesn't support for-update with no-wait timeout" );
	}

	@Override
	public String getForUpdateNowaitString(String aliases) {
		return getForUpdateNowaitString();
	}

	@Override
	public String getForUpdateSkipLockedString() {
		throw new UnsupportedOperationException(
				"Spanner doesn't support for-update with skip locked timeout" );
	}

	@Override
	public String getForUpdateSkipLockedString(String aliases) {
		return getForUpdateSkipLockedString();
	}

	@Override
	public LockingClauseStrategy getLockingClauseStrategy(
			QuerySpec querySpec, LockOptions lockOptions) {
		if ( lockOptions == null ) {
			return NON_CLAUSE_STRATEGY;
		}
		validateSpannerLockTimeout( lockOptions.getTimeOut() );
		return super.getLockingClauseStrategy( querySpec, lockOptions );
	}

	private static void validateSpannerLockTimeout(int millis) {
		if ( Timeouts.isRealTimeout( millis ) ) {
			throw new UnsupportedOperationException( "Spanner does not support lock timeout." );
		}
		if ( millis == Timeouts.SKIP_LOCKED_MILLI ) {
			throw new UnsupportedOperationException( "Spanner does not support skip locked." );
		}
		if ( millis == Timeouts.NO_WAIT_MILLI ) {
			throw new UnsupportedOperationException( "Spanner does not support no wait." );
		}
	}

	@Override
	public void contributeTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		super.contributeTypes( typeContributions, serviceRegistry );

		final var configurationService = serviceRegistry.requireService( ConfigurationService.class );

		this.useIntegerForPrimaryKey = configurationService.getSetting(
				USE_INTEGER_FOR_PRIMARY_KEY,
				StandardConverters.BOOLEAN,
				false
		);
	}

	@Override
	public FunctionalDependencyAnalysisSupport getFunctionalDependencyAnalysisSupport() {
		return FunctionalDependencyAnalysisSupportImpl.NONE;
	}

	@Override
	protected void contributePostgreSQLTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		final var jdbcTypeRegistry = typeContributions.getTypeConfiguration()
				.getJdbcTypeRegistry();

		jdbcTypeRegistry.addDescriptor(SpannerTimeJdbcType.INSTANCE);

		jdbcTypeRegistry.addDescriptor(PostgreSQLUUIDJdbcType.INSTANCE);

		// Replace the standard array constructor
		jdbcTypeRegistry.addTypeConstructor(PostgreSQLArrayJdbcTypeConstructor.INSTANCE);

		jdbcTypeRegistry.addDescriptorIfAbsent(PostgreSQLCastingJsonJdbcType.JSONB_INSTANCE);
		jdbcTypeRegistry.addTypeConstructorIfAbsent(PostgreSQLCastingJsonArrayJdbcTypeConstructor.JSONB_INSTANCE);
	}

	@Override
	public SqlAstTranslatorFactory getSqlAstTranslatorFactory() {
		return new StandardSqlAstTranslatorFactory() {
			@Override
			protected <T extends JdbcOperation> SqlAstTranslator<T> buildTranslator(SessionFactoryImplementor sessionFactory, Statement statement) {
				return new SpannerPostgreSQLSqlAstTranslator<T>( sessionFactory, statement );
			}
		};
	}

	@Override
	protected void registerPostgreSQLColumnTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		final var ddlTypeRegistry = typeContributions.getTypeConfiguration().getDdlTypeRegistry();

		// We need to configure that the array type uses the raw element type for casts
		ddlTypeRegistry.addDescriptor( new ArrayDdlTypeImpl( this, true ) );
		ddlTypeRegistry.addDescriptor( new DdlTypeImpl( UUID, "uuid", this ) );
		ddlTypeRegistry.addDescriptor( new DdlTypeImpl( JSON, "jsonb", this ) );

		ddlTypeRegistry.addDescriptor(
				CapacityDependentDdlType.builder( FLOAT, columnType( FLOAT ), castType( FLOAT ), this )
						.withTypeCapacity( 24, "real" )
						.withTypeCapacity( 53, "double precision" )
						.build()
		);
	}

	@Override
	public void appendDateTimeLiteral(
			SqlAppender appender,
			TemporalAccessor temporalAccessor,
			@SuppressWarnings("deprecation")
			TemporalType precision,
			TimeZone jdbcTimeZone) {
		if ( precision == TemporalType.TIME || (precision == TemporalType.TIMESTAMP && !temporalAccessor.isSupported( ChronoField.OFFSET_SECONDS ))) {
			precision = TemporalType.TIMESTAMP;
			if ( temporalAccessor instanceof LocalTime localTime) {
				temporalAccessor = localTime.atDate( LocalDate.of( 1970, 1, 1 ) )
						.atOffset( ZoneOffset.UTC );
			}
			else if ( temporalAccessor instanceof OffsetTime offsetTime ) {
				temporalAccessor = offsetTime.atDate( LocalDate.of( 1970, 1, 1 ) );
			}
			else if ( temporalAccessor instanceof LocalDateTime localDateTime) {
				temporalAccessor = localDateTime.atOffset( ZoneOffset.UTC );
			}
			else if ( temporalAccessor instanceof Instant instant) {
				temporalAccessor = instant.atOffset(  ZoneOffset.UTC );
			}
			else {
				throw new UnsupportedOperationException( "Unsupported temporal type: " + temporalAccessor.getClass().getName() );
			}
		}

		super.appendDateTimeLiteral(  appender, temporalAccessor, precision, jdbcTimeZone );
	}

	@Override
	public void appendDateTimeLiteral(
			SqlAppender appender,
			Date date,
			@SuppressWarnings("deprecation")
			TemporalType precision,
			TimeZone jdbcTimeZone) {
		if ( precision == TemporalType.TIME ) {
			precision = TemporalType.TIMESTAMP;
		}
		super.appendDateTimeLiteral( appender, date, precision, jdbcTimeZone );
	}

	public void appendDateTimeLiteral(
			SqlAppender appender,
			Calendar calendar,
			@SuppressWarnings("deprecation")
			TemporalType precision,
			TimeZone jdbcTimeZone) {
		if ( precision == TemporalType.TIME ) {
			precision = TemporalType.TIMESTAMP;
		}

		super.appendDateTimeLiteral( appender, calendar, precision, jdbcTimeZone );
	}

	@Override
	protected String castType(int sqlTypeCode) {
		return switch (sqlTypeCode) {
			case TIME, TIME_UTC, TIMESTAMP, TIMESTAMP_UTC -> columnType(TIMESTAMP_WITH_TIMEZONE);
			default -> super.castType(sqlTypeCode);
		};
	}

	@Override
	public String timestampaddPattern(TemporalUnit unit, TemporalType temporalType, IntervalType intervalType) {
		return intervalType != null
				? "(?2+?3)"
				: "cast(?3+" + intervalPattern( unit ) + " as " + castTemporalType( temporalType ) + ")";
	}

	private static String intervalPattern(TemporalUnit unit) {
		return switch (unit) {
			case NANOSECOND -> "cast(concat(cast((?2)/1e3 as text), ' microsecond') as interval)";
			case NATIVE -> "cast(concat(cast((?2) as text), ' second') as interval)";
			case QUARTER -> "cast(concat(cast((?2) as text), ' quarter') as interval)";
			case WEEK -> "cast(concat(cast((?2) as text), ' week') as interval)";
			default -> "cast(concat(cast((?2) as text), ' " + unit + "') as interval)";
		};
	}

	private String castTemporalType(TemporalType temporalType) {
		return switch (temporalType) {
			case TIME, TIMESTAMP -> castType( TIMESTAMP );
			default -> temporalType.name().toLowerCase();
		};
	}

	@Override
	protected String columnType(int sqlTypeCode) {
		return switch (sqlTypeCode) {
			// Spanner doesn't support precision with the timestamp
			case TIME, TIME_UTC, TIMESTAMP, TIMESTAMP_UTC, TIMESTAMP_WITH_TIMEZONE -> "timestamp with time zone";
			case BLOB -> "bytea";
			case CLOB, NCLOB -> "character varying";
			// Spanner doesn't support NUMERIC with precision and scale
			case NUMERIC ->  "numeric";
			case DECIMAL ->  "decimal";
			// Spanner doesn't support CHAR so we should use VARCHAR
			case CHAR -> columnType( VARCHAR );
			case SMALLINT, INTEGER, TINYINT ->  columnType( BIGINT );
			default -> super.columnType(sqlTypeCode);
		};
	}

	@Override
	public ScrollMode defaultScrollMode() {
		return ScrollMode.FORWARD_ONLY;
	}

	@Override
	public boolean supportsUserDefinedTypes() {
		return false;
	}

	@Override
	public boolean supportsFilterClause() {
		return false;
	}

	@Override
	public boolean supportsRecursiveCycleUsingClause() {
		return false;
	}

	@Override
	public boolean supportsRecursiveSearchClause() {
		return false;
	}

	@Override
	public boolean supportsUniqueConstraints() {
		return false;
	}

	@Override
	public boolean supportsRowValueConstructorGtLtSyntax() {
		return false;
	}

	// ALL subqueries with operators other than <>/!= are not supported
	@Override
	public boolean supportsRowValueConstructorSyntaxInQuantifiedPredicates() {
		return false;
	}

	@Override
	public boolean supportsRowValueConstructorSyntaxInInSubQuery() {
		return false;
	}

	@Override
	public boolean supportsCaseInsensitiveLike() {
		return false;
	}

	@Override
	public String currentTimestamp() {
		return currentTimestampWithTimeZone();
	}

	@Override
	public String currentTime() {
		return currentTimestampWithTimeZone();
	}

	@Override
	public boolean supportsLateral() {
		return false;
	}

	@Override
	public boolean supportsFromClauseInUpdate() {
		return false;
	}

	@Override
	public int getMaxVarcharLength() {
		return 2_621_440;
	}

	@Override
	public int getMaxVarbinaryLength() {
		//max is equivalent 10 MiB
		return 10_485_760;
	}

	@Override
	public String getCurrentSchemaCommand() {
		return "";
	}

	@Override
	public boolean supportsCommentOn() {
		return false;
	}

	@Override
	public boolean supportsWindowFunctions() {
		return false;
	}

	@Override
	public String getAddForeignKeyConstraintString(
			String constraintName,
			String[] foreignKey,
			String referencedTable,
			String[] primaryKey,
			boolean referencesPrimaryKey) {
		// Cloud Spanner requires the referenced columns to specified in all cases, including
		// if the foreign key is referencing the primary key of the referenced table. Setting referencesPrimaryKey to
		// false will add all the referenced columns.
		return super.getAddForeignKeyConstraintString( constraintName, foreignKey, referencedTable, primaryKey, false );
	}

	@Override
	public boolean canBatchTruncate() {
		return false;
	}

	@Override
	public String rowId(String rowId) {
		return null;
	}

	@Override
	public boolean supportsRowConstructor() {
		return false;
	}

	@Override
	public String getTruncateTableStatement(String tableName) {
		return "delete from " + tableName;
	}

	@Override
	public String getBeforeDropStatement() {
		return null;
	}

	@Override
	public String getCascadeConstraintsString() {
		return "";
	}

	@Override
	public boolean supportsIfExistsBeforeConstraintName() {
		return false;
	}

	@Override
	public boolean supportsIfExistsAfterAlterTable() {
		return false;
	}

	@Override
	public boolean supportsDistinctFromPredicate() {
		return false;
	}

	@Override
	public boolean supportsPartitionBy() {
		return false;
	}

	@Override
	public String getDual() {
		return "unnest(ARRAY[1])";
	}

	@Override
	public String getFromDualForSelectOnly() {
		return " from " + getDual() + " dual";
	}

	@Override
	public boolean supportsRecursiveCTE() {
		return false;
	}

	public boolean supportsWithClauseInSubquery() {
		return false;
	}

	public boolean supportsNestedWithClause() {
		return false;
	}

	public boolean supportsCteHeaderColumnList() {
		return false;
	}

	@Override
	public SqmMultiTableMutationStrategy getFallbackSqmMutationStrategy(
			EntityMappingType rootEntityDescriptor,
			RuntimeModelCreationContext runtimeModelCreationContext) {
		return new PersistentTableMutationStrategy( rootEntityDescriptor, runtimeModelCreationContext );
	}

	@Override
	public SqmMultiTableInsertStrategy getFallbackSqmInsertStrategy(
			EntityMappingType rootEntityDescriptor,
			RuntimeModelCreationContext runtimeModelCreationContext) {
		return new PersistentTableInsertStrategy( rootEntityDescriptor, runtimeModelCreationContext );
	}

	@Override
	public TemporaryTableStrategy getLocalTemporaryTableStrategy() {
		return null;
	}

	@Override
	public @Nullable TemporaryTableStrategy getGlobalTemporaryTableStrategy() {
		return null;
	}

	@Override
	public TemporaryTableStrategy getPersistentTemporaryTableStrategy() {
		return new PersistentTemporaryTableStrategy( this );
	}

	@Override
	public SQLExceptionConversionDelegate buildSQLExceptionConversionDelegate() {
		return this::handleConstraintViolatedException;
	}

	private @Nullable JDBCException handleConstraintViolatedException(SQLException sqlException, String message, String sql) {
		if (sqlException.getErrorCode() == 6) {
			return new ConstraintViolationException( message, sqlException, ConstraintViolationException.ConstraintKind.UNIQUE, null );
		}
		else if (matches( NOT_NULL_CONSTRAINT_PATTERN, message )) {
			return new ConstraintViolationException( message, sqlException, ConstraintViolationException.ConstraintKind.NOT_NULL, null );
		}
		else if (matches( CHECK_CONSTRAINT_PATTERN, message )) {
			return new ConstraintViolationException( message, sqlException, ConstraintViolationException.ConstraintKind.CHECK, null );
		}
		else if(matches( FOREIGN_KEY_CONSTRAINT_PATTERN, message )) {
			return new ConstraintViolationException( message, sqlException, ConstraintViolationException.ConstraintKind.FOREIGN_KEY, null );
		}
		else {
			return null;
		}
	}

	private boolean matches(Pattern pattern, String message) {
		return pattern.matcher( message ).matches();
	}

	@Override
	public CallableStatementSupport getCallableStatementSupport() {
		return StandardCallableStatementSupport.NO_REF_CURSOR_INSTANCE;
	}

	public boolean useIntegerForPrimaryKey() {
		return useIntegerForPrimaryKey;
	}
}
