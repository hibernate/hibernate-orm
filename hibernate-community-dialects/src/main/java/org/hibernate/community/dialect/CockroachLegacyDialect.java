/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import jakarta.persistence.GenerationType;
import jakarta.persistence.TemporalType;
import jakarta.persistence.Timeout;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.PessimisticLockException;
import org.hibernate.QueryTimeoutException;
import org.hibernate.Timeouts;
import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.DmlTargetColumnQualifierSupport;
import org.hibernate.dialect.FunctionalDependencyAnalysisSupport;
import org.hibernate.dialect.FunctionalDependencyAnalysisSupportImpl;
import org.hibernate.dialect.NationalizationSupport;
import org.hibernate.dialect.NullOrdering;
import org.hibernate.dialect.PostgreSQLDriverKind;
import org.hibernate.dialect.SimpleDatabaseVersion;
import org.hibernate.dialect.SpannerDialect;
import org.hibernate.dialect.TimeZoneSupport;
import org.hibernate.dialect.aggregate.AggregateSupport;
import org.hibernate.dialect.aggregate.CockroachDBAggregateSupport;
import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.dialect.function.FormatFunction;
import org.hibernate.dialect.function.PostgreSQLTruncFunction;
import org.hibernate.dialect.identity.CockroachDBIdentityColumnSupport;
import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.dialect.lock.spi.LockingSupport;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.dialect.pagination.OffsetFetchLimitHandler;
import org.hibernate.dialect.sequence.PostgreSQLSequenceSupport;
import org.hibernate.dialect.sequence.SequenceSupport;
import org.hibernate.dialect.type.PgJdbcHelper;
import org.hibernate.dialect.type.PostgreSQLArrayJdbcTypeConstructor;
import org.hibernate.dialect.type.PostgreSQLCastingInetJdbcType;
import org.hibernate.dialect.type.PostgreSQLCastingIntervalSecondJdbcType;
import org.hibernate.dialect.type.PostgreSQLCastingJsonArrayJdbcTypeConstructor;
import org.hibernate.dialect.type.PostgreSQLCastingJsonJdbcType;
import org.hibernate.dialect.type.PostgreSQLEnumJdbcType;
import org.hibernate.dialect.type.PostgreSQLOrdinalEnumJdbcType;
import org.hibernate.dialect.type.PostgreSQLUUIDJdbcType;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.jdbc.env.spi.IdentifierCaseStrategy;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelperBuilder;
import org.hibernate.engine.jdbc.env.spi.NameQualifierSupport;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.exception.LockAcquisitionException;
import org.hibernate.exception.spi.SQLExceptionConversionDelegate;
import org.hibernate.exception.spi.TemplatedViolatedConstraintNameExtractor;
import org.hibernate.exception.spi.ViolatedConstraintNameExtractor;
import org.hibernate.internal.util.JdbcExceptionHelper;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.query.SemanticException;
import org.hibernate.query.common.TemporalUnit;
import org.hibernate.query.sqm.IntervalType;
import org.hibernate.query.sqm.produce.function.StandardFunctionArgumentTypeResolvers;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.spi.StandardSqlAstTranslatorFactory;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.tool.schema.extract.spi.ColumnTypeInformation;
import org.hibernate.type.JavaObjectType;
import org.hibernate.type.descriptor.DateTimeUtils;
import org.hibernate.type.descriptor.jdbc.BlobJdbcType;
import org.hibernate.type.descriptor.jdbc.ClobJdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.NClobJdbcType;
import org.hibernate.type.descriptor.jdbc.ObjectNullAsBinaryTypeJdbcType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;
import org.hibernate.type.descriptor.sql.internal.DdlTypeImpl;
import org.hibernate.type.descriptor.sql.internal.NamedNativeEnumDdlTypeImpl;
import org.hibernate.type.descriptor.sql.internal.NamedNativeOrdinalEnumDdlTypeImpl;
import org.hibernate.type.descriptor.sql.internal.Scale6IntervalSecondDdlType;
import org.hibernate.type.descriptor.sql.spi.DdlTypeRegistry;
import org.hibernate.type.spi.TypeConfiguration;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.hibernate.dialect.lock.internal.CockroachLockingSupport.COCKROACH_LOCKING_SUPPORT;
import static org.hibernate.dialect.lock.internal.CockroachLockingSupport.LEGACY_COCKROACH_LOCKING_SUPPORT;
import static org.hibernate.exception.spi.TemplatedViolatedConstraintNameExtractor.extractUsingTemplate;
import static org.hibernate.query.common.TemporalUnit.DAY;
import static org.hibernate.query.common.TemporalUnit.EPOCH;
import static org.hibernate.query.common.TemporalUnit.NATIVE;
import static org.hibernate.type.SqlTypes.ARRAY;
import static org.hibernate.type.SqlTypes.BINARY;
import static org.hibernate.type.SqlTypes.BLOB;
import static org.hibernate.type.SqlTypes.CHAR;
import static org.hibernate.type.SqlTypes.CLOB;
import static org.hibernate.type.SqlTypes.GEOGRAPHY;
import static org.hibernate.type.SqlTypes.GEOMETRY;
import static org.hibernate.type.SqlTypes.INET;
import static org.hibernate.type.SqlTypes.INTEGER;
import static org.hibernate.type.SqlTypes.JSON;
import static org.hibernate.type.SqlTypes.LONG32NVARCHAR;
import static org.hibernate.type.SqlTypes.LONG32VARBINARY;
import static org.hibernate.type.SqlTypes.LONG32VARCHAR;
import static org.hibernate.type.SqlTypes.NCHAR;
import static org.hibernate.type.SqlTypes.NCLOB;
import static org.hibernate.type.SqlTypes.NVARCHAR;
import static org.hibernate.type.SqlTypes.OTHER;
import static org.hibernate.type.SqlTypes.TIME;
import static org.hibernate.type.SqlTypes.TIMESTAMP;
import static org.hibernate.type.SqlTypes.TIMESTAMP_UTC;
import static org.hibernate.type.SqlTypes.TIMESTAMP_WITH_TIMEZONE;
import static org.hibernate.type.SqlTypes.TIME_UTC;
import static org.hibernate.type.SqlTypes.TINYINT;
import static org.hibernate.type.SqlTypes.UUID;
import static org.hibernate.type.SqlTypes.VARBINARY;
import static org.hibernate.type.SqlTypes.VARCHAR;
import static org.hibernate.type.descriptor.DateTimeUtils.appendAsDateWithEraSuffix;
import static org.hibernate.type.descriptor.DateTimeUtils.appendAsLocalTime;
import static org.hibernate.type.descriptor.DateTimeUtils.appendAsTime;
import static org.hibernate.type.descriptor.DateTimeUtils.appendAsTimestampWithMicrosAndEraSuffix;

/**
 * A {@linkplain Dialect SQL dialect} for CockroachDB.
 *
 * @author Gavin King
 */
public class CockroachLegacyDialect extends Dialect {

	// KNOWN LIMITATIONS:
	// * no support for java.sql.Clob

	// Pre-compile and reuse pattern
	private static final Pattern CRDB_VERSION_PATTERN = Pattern.compile( "v[\\d]+(\\.[\\d]+)?(\\.[\\d]+)?" );
	protected static final DatabaseVersion DEFAULT_VERSION = DatabaseVersion.make( 19, 2 );
	protected final PostgreSQLDriverKind driverKind;

	public CockroachLegacyDialect() {
		this( DEFAULT_VERSION );
	}

	public CockroachLegacyDialect(DialectResolutionInfo info) {
		this( fetchDataBaseVersion( info ), PostgreSQLDriverKind.determineKind( info ) );
		registerKeywords( info );
	}

	public CockroachLegacyDialect(DatabaseVersion version) {
		super(version);
		driverKind = PostgreSQLDriverKind.PG_JDBC;
	}

	public CockroachLegacyDialect(DatabaseVersion version, PostgreSQLDriverKind driverKind) {
		super(version);
		this.driverKind = driverKind;
	}

	@Override
	public DatabaseVersion determineDatabaseVersion(DialectResolutionInfo info) {
		return fetchDataBaseVersion( info );
	}

	protected static DatabaseVersion fetchDataBaseVersion(DialectResolutionInfo info ) {
		String versionString = null;
		if ( info.getDatabaseMetadata() != null ) {
			try (java.sql.Statement s = info.getDatabaseMetadata().getConnection().createStatement() ) {
				final ResultSet rs = s.executeQuery( "SELECT version()" );
				if ( rs.next() ) {
					versionString = rs.getString( 1 );
				}
			}
			catch (SQLException ex) {
				// Ignore
			}
		}
		return parseVersion( versionString );
	}

	protected static DatabaseVersion parseVersion(String versionString ) {
		DatabaseVersion databaseVersion = null;
		// What the DB select returns is similar to "CockroachDB CCL v21.2.10 (x86_64-unknown-linux-gnu, built 2022/05/02 17:38:58, go1.16.6)"
		Matcher m = CRDB_VERSION_PATTERN.matcher( versionString == null ? "" : versionString );
		if ( m.find() ) {
			String[] versionParts = StringHelper.split( ".", m.group().substring( 1 ) );
			// if we got to this point, there is at least a major version, so no need to check [].length > 0
			int majorVersion = Integer.parseInt( versionParts[0] );
			int minorVersion = versionParts.length > 1 ? Integer.parseInt( versionParts[1] ) : 0;
			int microVersion = versionParts.length > 2 ? Integer.parseInt( versionParts[2] ) : 0;

			databaseVersion=  new SimpleDatabaseVersion( majorVersion, minorVersion, microVersion);
		}
		if ( databaseVersion == null ) {
			// Recur to the default version of the no-args constructor
			databaseVersion = DEFAULT_VERSION;
		}
		return databaseVersion;
	}


	@Override
	protected String columnType(int sqlTypeCode) {
		switch ( sqlTypeCode ) {
			case TINYINT:
				return "smallint"; //no tinyint
			case INTEGER:
				return "int4";

			case NCHAR:
				return columnType( CHAR );
			case NVARCHAR:
				return columnType( VARCHAR );

			case NCLOB:
			case CLOB:
				return "string";

			case BINARY:
			case VARBINARY:
			case BLOB:
				return "bytes";

			// We do not use the time with timezone type because PG deprecated it and it lacks certain operations like subtraction
//			case TIME_UTC:
//				return columnType( TIME_WITH_TIMEZONE );

			case TIMESTAMP_UTC:
				return columnType( TIMESTAMP_WITH_TIMEZONE );

			default:
				return super.columnType( sqlTypeCode );
		}
	}

	@Override
	protected String castType(int sqlTypeCode) {
		switch ( sqlTypeCode ) {
			case CHAR:
			case NCHAR:
			case VARCHAR:
			case NVARCHAR:
			case LONG32VARCHAR:
			case LONG32NVARCHAR:
				return "string";
			case BINARY:
			case VARBINARY:
			case LONG32VARBINARY:
				return "bytes";
		}
		return super.castType( sqlTypeCode );
	}

	@Override
	protected void registerColumnTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		super.registerColumnTypes( typeContributions, serviceRegistry );
		final DdlTypeRegistry ddlTypeRegistry = typeContributions.getTypeConfiguration().getDdlTypeRegistry();

		ddlTypeRegistry.addDescriptor( new DdlTypeImpl( UUID, "uuid", this ) );
		ddlTypeRegistry.addDescriptor( new DdlTypeImpl( GEOMETRY, "geometry", this ) );
		ddlTypeRegistry.addDescriptor( new DdlTypeImpl( GEOGRAPHY, "geography", this ) );
		ddlTypeRegistry.addDescriptor( new Scale6IntervalSecondDdlType( this ) );

		// Prefer jsonb if possible
		if ( getVersion().isSameOrAfter( 20 ) ) {
			ddlTypeRegistry.addDescriptor( new DdlTypeImpl( INET, "inet", this ) );
			ddlTypeRegistry.addDescriptor( new DdlTypeImpl( JSON, "jsonb", this ) );
		}
		else {
			ddlTypeRegistry.addDescriptor( new DdlTypeImpl( JSON, "json", this ) );
		}
		ddlTypeRegistry.addDescriptor( new NamedNativeEnumDdlTypeImpl( this ) );
		ddlTypeRegistry.addDescriptor( new NamedNativeOrdinalEnumDdlTypeImpl( this ) );
	}

	@Override
	public JdbcType resolveSqlTypeDescriptor(
			String columnTypeName,
			int jdbcTypeCode,
			int precision,
			int scale,
			JdbcTypeRegistry jdbcTypeRegistry) {
		switch ( jdbcTypeCode ) {
			case OTHER:
				switch ( columnTypeName ) {
					case "uuid":
						jdbcTypeCode = UUID;
						break;
					case "json":
					case "jsonb":
						jdbcTypeCode = JSON;
						break;
					case "inet":
						jdbcTypeCode = INET;
						break;
					case "geometry":
						jdbcTypeCode = GEOMETRY;
						break;
					case "geography":
						jdbcTypeCode = GEOGRAPHY;
						break;
				}
				break;
			case TIME:
				// The PostgreSQL JDBC driver reports TIME for timetz, but we use it only for mapping OffsetTime to UTC
				if ( "timetz".equals( columnTypeName ) ) {
					jdbcTypeCode = TIME_UTC;
				}
				break;
			case TIMESTAMP:
				// The PostgreSQL JDBC driver reports TIMESTAMP for timestamptz, but we use it only for mapping Instant
				if ( "timestamptz".equals( columnTypeName ) ) {
					jdbcTypeCode = TIMESTAMP_UTC;
				}
				break;
			case ARRAY:
				// PostgreSQL names array types by prepending an underscore to the base name
				if ( columnTypeName.charAt( 0 ) == '_' ) {
					final String componentTypeName = columnTypeName.substring( 1 );
					final Integer sqlTypeCode = resolveSqlTypeCode( componentTypeName, jdbcTypeRegistry.getTypeConfiguration() );
					if ( sqlTypeCode != null ) {
						return jdbcTypeRegistry.resolveTypeConstructorDescriptor(
								jdbcTypeCode,
								jdbcTypeRegistry.getDescriptor( sqlTypeCode ),
								ColumnTypeInformation.EMPTY
						);
					}
				}
				break;
		}
		return jdbcTypeRegistry.getDescriptor( jdbcTypeCode );
	}

	@Override
	protected Integer resolveSqlTypeCode(String columnTypeName, TypeConfiguration typeConfiguration) {
		switch ( columnTypeName ) {
			case "bool":
				return Types.BOOLEAN;
			case "float4":
				// Use REAL instead of FLOAT to get Float as recommended Java type
				return Types.REAL;
			case "float8":
				return Types.DOUBLE;
			case "int2":
				return Types.SMALLINT;
			case "int4":
				return Types.INTEGER;
			case "int8":
				return Types.BIGINT;
		}
		return super.resolveSqlTypeCode( columnTypeName, typeConfiguration );
	}

	@Override
	public void contributeTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		super.contributeTypes( typeContributions, serviceRegistry );
		contributeCockroachTypes( typeContributions, serviceRegistry );
	}

	protected void contributeCockroachTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		final JdbcTypeRegistry jdbcTypeRegistry = typeContributions.getTypeConfiguration()
				.getJdbcTypeRegistry();
		// Don't use this type due to https://github.com/pgjdbc/pgjdbc/issues/2862
		//jdbcTypeRegistry.addDescriptor( TimestampUtcAsOffsetDateTimeJdbcType.INSTANCE );
		if ( driverKind == PostgreSQLDriverKind.PG_JDBC ) {
			jdbcTypeRegistry.addDescriptor( PostgreSQLEnumJdbcType.INSTANCE );
			jdbcTypeRegistry.addDescriptor( PostgreSQLOrdinalEnumJdbcType.INSTANCE );
			jdbcTypeRegistry.addDescriptorIfAbsent( PostgreSQLUUIDJdbcType.INSTANCE );
			if ( PgJdbcHelper.isUsable( serviceRegistry ) ) {
				jdbcTypeRegistry.addDescriptorIfAbsent( PgJdbcHelper.getIntervalJdbcType( serviceRegistry ) );

				if ( getVersion().isSameOrAfter( 20, 0 ) ) {
					jdbcTypeRegistry.addDescriptorIfAbsent( PgJdbcHelper.getInetJdbcType( serviceRegistry ) );
					jdbcTypeRegistry.addDescriptorIfAbsent( PgJdbcHelper.getJsonbJdbcType( serviceRegistry ) );
					jdbcTypeRegistry.addTypeConstructorIfAbsent( PgJdbcHelper.getJsonbArrayJdbcType( serviceRegistry ) );
				}
				else {
					jdbcTypeRegistry.addDescriptorIfAbsent( PgJdbcHelper.getJsonJdbcType( serviceRegistry ) );
					jdbcTypeRegistry.addTypeConstructorIfAbsent( PgJdbcHelper.getJsonArrayJdbcType( serviceRegistry ) );
				}
			}
			else {
				jdbcTypeRegistry.addDescriptorIfAbsent( PostgreSQLCastingIntervalSecondJdbcType.INSTANCE );
				if ( getVersion().isSameOrAfter( 20, 0 ) ) {
					jdbcTypeRegistry.addDescriptorIfAbsent( PostgreSQLCastingInetJdbcType.INSTANCE );
					jdbcTypeRegistry.addDescriptorIfAbsent( PostgreSQLCastingJsonJdbcType.JSONB_INSTANCE );
					jdbcTypeRegistry.addTypeConstructorIfAbsent( PostgreSQLCastingJsonArrayJdbcTypeConstructor.JSONB_INSTANCE );
				}
				else {
					jdbcTypeRegistry.addDescriptorIfAbsent( PostgreSQLCastingJsonJdbcType.JSON_INSTANCE );
					jdbcTypeRegistry.addTypeConstructorIfAbsent( PostgreSQLCastingJsonArrayJdbcTypeConstructor.JSON_INSTANCE );
				}
			}
		}
		else {
			jdbcTypeRegistry.addDescriptorIfAbsent( PostgreSQLUUIDJdbcType.INSTANCE );
			jdbcTypeRegistry.addDescriptorIfAbsent( PostgreSQLCastingIntervalSecondJdbcType.INSTANCE );
			if ( getVersion().isSameOrAfter( 20, 0 ) ) {
				jdbcTypeRegistry.addDescriptorIfAbsent( PostgreSQLCastingInetJdbcType.INSTANCE );
				jdbcTypeRegistry.addDescriptorIfAbsent( PostgreSQLCastingJsonJdbcType.JSONB_INSTANCE );
				jdbcTypeRegistry.addTypeConstructorIfAbsent( PostgreSQLCastingJsonArrayJdbcTypeConstructor.JSONB_INSTANCE );
			}
			else {
				jdbcTypeRegistry.addDescriptorIfAbsent( PostgreSQLCastingJsonJdbcType.JSON_INSTANCE );
				jdbcTypeRegistry.addTypeConstructorIfAbsent( PostgreSQLCastingJsonArrayJdbcTypeConstructor.JSON_INSTANCE );
			}
		}

		// Force Blob binding to byte[] for CockroachDB
		jdbcTypeRegistry.addDescriptor( Types.BLOB, BlobJdbcType.MATERIALIZED );
		jdbcTypeRegistry.addDescriptor( Types.CLOB, ClobJdbcType.MATERIALIZED );
		jdbcTypeRegistry.addDescriptor( Types.NCLOB, NClobJdbcType.MATERIALIZED );

		// The next two contributions are the same as for Postgresql
		typeContributions.contributeJdbcType( ObjectNullAsBinaryTypeJdbcType.INSTANCE );

		// Until we remove StandardBasicTypes, we have to keep this
		typeContributions.contributeType(
				new JavaObjectType(
						ObjectNullAsBinaryTypeJdbcType.INSTANCE,
						typeContributions.getTypeConfiguration()
								.getJavaTypeRegistry()
								.getDescriptor( Object.class )
				)
		);

		// Replace the standard array constructor
		jdbcTypeRegistry.addTypeConstructor( PostgreSQLArrayJdbcTypeConstructor.INSTANCE );
	}

	@Override
	public void initializeFunctionRegistry(FunctionContributions functionContributions) {
		super.initializeFunctionRegistry(functionContributions);

		final CommonFunctionFactory functionFactory = new CommonFunctionFactory(functionContributions);
		functionFactory.ascii();
		functionFactory.char_chr();
		functionFactory.overlay();
		functionFactory.position();
		functionFactory.substringFromFor();
		functionFactory.locate_positionSubstring();
		functionFactory.concat_pipeOperator();
		functionFactory.trim2();
		functionFactory.substr();
		functionFactory.reverse();
		functionFactory.repeat();
		functionFactory.md5();
		functionFactory.sha1();
		functionFactory.octetLength();
		functionFactory.bitLength();
		functionFactory.cbrt();
		functionFactory.cot();
		functionFactory.degrees();
		functionFactory.radians();
		functionFactory.pi();
		functionFactory.log();
		functionFactory.log10_log();
		functionFactory.round();

		functionFactory.bitandorxornot_operator();
		functionFactory.bitAndOr();
		functionFactory.everyAny_boolAndOr();
		functionFactory.median_percentileCont_castDouble();
		functionFactory.stddev();
		functionFactory.stddevPopSamp();
		functionFactory.variance();
		functionFactory.varPopSamp();
		functionFactory.covarPopSamp();
		functionFactory.corr();
		functionFactory.regrLinearRegressionAggregates();

		functionContributions.getFunctionRegistry().register(
				"format",
				new FormatFunction(
						"experimental_strftime",
						false,
						true,
						false,
						functionContributions.getTypeConfiguration()
				)
		);
		functionFactory.windowFunctions();
		functionFactory.listagg_stringAgg( "string" );
		functionFactory.inverseDistributionOrderedSetAggregates();
		functionFactory.hypotheticalOrderedSetAggregates_windowEmulation();
		functionFactory.array_postgresql();
		functionFactory.arrayAggregate();
		functionFactory.arrayPosition_postgresql();
		functionFactory.arrayPositions_postgresql();
		functionFactory.arrayLength_cardinality();
		functionFactory.arrayConcat_postgresql();
		functionFactory.arrayPrepend_postgresql();
		functionFactory.arrayAppend_postgresql();
		functionFactory.arrayContains_postgresql();
		functionFactory.arrayIntersects_postgresql();
		functionFactory.arrayGet_bracket();
		functionFactory.arraySet_unnest();
		functionFactory.arrayRemove();
		functionFactory.arrayRemoveIndex_unnest( true );
		functionFactory.arraySlice_operator();
		functionFactory.arrayReplace();
		functionFactory.arrayTrim_unnest();
		functionFactory.arrayFill_cockroachdb();
		functionFactory.arrayToString_postgresql();

		functionFactory.jsonValue_cockroachdb();
		functionFactory.jsonQuery_cockroachdb();
		functionFactory.jsonExists_cockroachdb();
		functionFactory.jsonObject_postgresql();
		functionFactory.jsonArray_postgresql();
		functionFactory.jsonArrayAgg_postgresql( false );
		functionFactory.jsonObjectAgg_postgresql( false );
		functionFactory.jsonSet_postgresql();
		functionFactory.jsonRemove_cockroachdb();
		functionFactory.jsonReplace_postgresql();
		functionFactory.jsonInsert_postgresql();
		// No support for WITH clause in subquery: https://github.com/cockroachdb/cockroach/issues/131011
//		functionFactory.jsonMergepatch_postgresql();
		functionFactory.jsonArrayAppend_postgresql( false );
		functionFactory.jsonArrayInsert_postgresql();

		functionFactory.unnest_postgresql( false );
		functionFactory.generateSeries( null, "ordinality", true );
		functionFactory.jsonTable_cockroachdb();

		// Postgres uses # instead of ^ for XOR
		functionContributions.getFunctionRegistry().patternDescriptorBuilder( "bitxor", "(?1#?2)" )
				.setExactArgumentCount( 2 )
				.setArgumentTypeResolver( StandardFunctionArgumentTypeResolvers.ARGUMENT_OR_IMPLIED_RESULT_TYPE )
				.register();

		functionContributions.getFunctionRegistry().register(
				"trunc",
				new PostgreSQLTruncFunction(
						getVersion().isSameOrAfter( 22, 2 ),
						functionContributions.getTypeConfiguration()
				)
		);
		functionContributions.getFunctionRegistry().registerAlternateKey( "truncate", "trunc" );
	}

	@Override
	public @Nullable String getDefaultOrdinalityColumnName() {
		return "ordinality";
	}

	@Override
	public TimeZoneSupport getTimeZoneSupport() {
		return TimeZoneSupport.NORMALIZE;
	}

	@Override
	public void appendBooleanValueString(SqlAppender appender, boolean bool) {
		appender.appendSql( bool );
	}

	@Override
	public String getCascadeConstraintsString() {
		return " cascade";
	}

	@Override
	public boolean supportsCurrentTimestampSelection() {
		return true;
	}

	@Override
	public boolean isCurrentTimestampSelectStringCallable() {
		return false;
	}

	@Override
	public String getCurrentTimestampSelectString() {
		return "select now()";
	}

	@Override
	public boolean supportsDistinctFromPredicate() {
		return true;
	}

	@Override
	public boolean supportsIfExistsBeforeTableName() {
		return true;
	}

	@Override
	public boolean supportsIfExistsBeforeConstraintName() {
		return true;
	}

	@Override
	public boolean supportsIfExistsAfterAlterTable() {
		return true;
	}

	@Override
	public boolean qualifyIndexName() {
		return false;
	}

	@Override
	public IdentityColumnSupport getIdentityColumnSupport() {
		return CockroachDBIdentityColumnSupport.INSTANCE;
	}

	@Override
	public boolean supportsValuesList() {
		return true;
	}

	@Override
	public boolean supportsPartitionBy() {
		return true;
	}

	@Override
	public boolean supportsNonQueryWithCTE() {
		return true;
	}

	@Override
	public boolean supportsRecursiveCTE() {
		return getVersion().isSameOrAfter( 20, 1 );
	}
	@Override
	public boolean supportsConflictClauseForInsertCTE() {
		return true;
	}

	@Override
	public String getNoColumnsInsertString() {
		return "default values";
	}

	@Override
	public String getCaseInsensitiveLike(){
		return "ilike";
	}

	@Override
	public boolean supportsCaseInsensitiveLike() {
		return true;
	}

	@Override
	public boolean supportsNullPrecedence() {
		// Not yet implemented: https://www.cockroachlabs.com/docs/v20.2/null-handling.html#nulls-and-sorting
		return false;
	}

	@Override
	public NullOrdering getNullOrdering() {
		return NullOrdering.SMALLEST;
	}

	@Override
	public boolean supportsTupleCounts() {
		return true;
	}

	@Override
	public boolean requiresParensForTupleDistinctCounts() {
		return true;
	}

	@Override
	public GenerationType getNativeValueGenerationStrategy() {
		return GenerationType.SEQUENCE;
	}

	@Override
	public SequenceSupport getSequenceSupport() {
		return PostgreSQLSequenceSupport.INSTANCE;
	}

	@Override
	public String getQuerySequencesString() {
		return "select sequence_name,sequence_schema,sequence_catalog,start_value,minimum_value,maximum_value,increment from information_schema.sequences";
	}

	@Override
	public boolean supportsLobValueChangePropagation() {
		return false;
	}

	@Override
	public SqlAstTranslatorFactory getSqlAstTranslatorFactory() {
		return new StandardSqlAstTranslatorFactory() {
			@Override
			protected <T extends JdbcOperation> SqlAstTranslator<T> buildTranslator(
					SessionFactoryImplementor sessionFactory, Statement statement) {
				return new CockroachLegacySqlAstTranslator<>( sessionFactory, statement );
			}
		};
	}

	@Override
	public NationalizationSupport getNationalizationSupport() {
		// TEXT / STRING inherently support nationalized data
		return NationalizationSupport.IMPLICIT;
	}

	@Override
	public AggregateSupport getAggregateSupport() {
		return CockroachDBAggregateSupport.valueOf( this );
	}

	@Override
	public int getMaxIdentifierLength() {
		return 63;
	}

	@Override
	public boolean supportsStandardArrays() {
		return true;
	}

	@Override
	public boolean supportsTemporalLiteralOffset() {
		return true;
	}

	@Override
	public void appendDateTimeLiteral(
			SqlAppender appender,
			TemporalAccessor temporalAccessor,
			TemporalType precision,
			TimeZone jdbcTimeZone) {
		switch ( precision ) {
			case DATE:
				appender.appendSql( "date '" );
				appendAsDateWithEraSuffix( appender, temporalAccessor );
				appender.appendSql( '\'' );
				break;
			case TIME:
				if ( supportsTemporalLiteralOffset() && temporalAccessor.isSupported( ChronoField.OFFSET_SECONDS ) ) {
					appender.appendSql( "time with time zone '" );
					appendAsTime( appender, temporalAccessor, true, jdbcTimeZone );
				}
				else {
					appender.appendSql( "time '" );
					appendAsLocalTime( appender, temporalAccessor );
				}
				appender.appendSql( '\'' );
				break;
			case TIMESTAMP:
				if ( supportsTemporalLiteralOffset() && temporalAccessor.isSupported( ChronoField.OFFSET_SECONDS ) ) {
					appender.appendSql( "timestamp with time zone '" );
					appendAsTimestampWithMicrosAndEraSuffix( appender, temporalAccessor, true, jdbcTimeZone );
					appender.appendSql( '\'' );
				}
				else {
					appender.appendSql( "timestamp '" );
					appendAsTimestampWithMicrosAndEraSuffix( appender, temporalAccessor, false, jdbcTimeZone );
					appender.appendSql( '\'' );
				}
				break;
			default:
				throw new IllegalArgumentException();
		}
	}

	@Override
	public void appendDateTimeLiteral(SqlAppender appender, Date date, TemporalType precision, TimeZone jdbcTimeZone) {
		switch ( precision ) {
			case DATE:
				appender.appendSql( "date '" );
				appendAsDateWithEraSuffix( appender, date );
				appender.appendSql( '\'' );
				break;
			case TIME:
				appender.appendSql( "time with time zone '" );
				appendAsTime( appender, date, jdbcTimeZone );
				appender.appendSql( '\'' );
				break;
			case TIMESTAMP:
				appender.appendSql( "timestamp with time zone '" );
				appendAsTimestampWithMicrosAndEraSuffix( appender,date, jdbcTimeZone );
				appender.appendSql( '\'' );
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
				appender.appendSql( "date '" );
				appendAsDateWithEraSuffix( appender, calendar );
				appender.appendSql( '\'' );
				break;
			case TIME:
				appender.appendSql( "time with time zone '" );
				appendAsTime( appender, calendar, jdbcTimeZone );
				appender.appendSql( '\'' );
				break;
			case TIMESTAMP:
				appender.appendSql( "timestamp with time zone '" );
				DateTimeUtils.appendAsTimestampWithMillisAndEraSuffix( appender, calendar, jdbcTimeZone );
				appender.appendSql( '\'' );
				break;
			default:
				throw new IllegalArgumentException();
		}
	}

	/**
	 * The {@code extract()} function returns {@link TemporalUnit#DAY_OF_WEEK}
	 * numbered from 0 to 6. This isn't consistent with what most other
	 * databases do, so here we adjust the result by generating
	 * {@code (extract(dayofweek,arg)+1))}.
	 */
	@Override
	public String extractPattern(TemporalUnit unit) {
		switch ( unit ) {
			case DAY_OF_WEEK:
				return "(" + super.extractPattern(unit) + "+1)";
			default:
				return super.extractPattern(unit);
		}
	}

	@Override
	public String translateExtractField(TemporalUnit unit) {
		switch ( unit ) {
			case DAY_OF_MONTH: return "day";
			case DAY_OF_YEAR: return "dayofyear";
			case DAY_OF_WEEK: return "dayofweek";
			default: return super.translateExtractField( unit );
		}
	}

	/**
	 * {@code microsecond} is the smallest unit for an {@code interval},
	 * and the highest precision for a {@code timestamp}.
	 */
	@Override
	public long getFractionalSecondPrecisionInNanos() {
		return 1_000; //microseconds
	}

	@Override
	public String timestampaddPattern(TemporalUnit unit, TemporalType temporalType, IntervalType intervalType) {
		return intervalType != null
				? "(?2+?3)"
				: "cast(?3+" + intervalPattern( unit ) + " as " + temporalType.name().toLowerCase() + ")";
	}

	private static String intervalPattern(TemporalUnit unit) {
		switch (unit) {
			case NATIVE:
				return "(?2)*interval '1 microsecond'";
			case NANOSECOND:
				return "(?2)/1e3*interval '1 microsecond'";
			case QUARTER: //quarter is not supported in interval literals
				return "(?2)*interval '3 month'";
			case WEEK: //week is not supported in interval literals
				return "(?2)*interval '7 day'";
			default:
				return "(?2)*interval '1 " + unit + "'";
		}
	}

	@Override
	public String timestampdiffPattern(TemporalUnit unit, TemporalType fromTemporalType, TemporalType toTemporalType) {
		if ( unit == null ) {
			return "(?3-?2)";
		}
		if ( toTemporalType == TemporalType.DATE && fromTemporalType == TemporalType.DATE ) {
			// special case: subtraction of two dates
			// results in an integer number of days
			// instead of an INTERVAL
			switch ( unit ) {
				case YEAR:
				case MONTH:
				case QUARTER:
					// age only supports timestamptz, so we have to cast the date expressions
					return "extract(" + translateDurationField( unit ) + " from age(cast(?3 as timestamptz),cast(?2 as timestamptz)))";
				default:
					return "(?3-?2)" + DAY.conversionFactor( unit, this );
			}
		}
		else {
			if (getVersion().isSameOrAfter( 20, 1 )) {
				switch (unit) {
					case YEAR:
						return "extract(year from ?3-?2)";
					case QUARTER:
						return "(extract(year from ?3-?2)*4+extract(month from ?3-?2)//3)";
					case MONTH:
						return "(extract(year from ?3-?2)*12+extract(month from ?3-?2))";
					case WEEK: //week is not supported by extract() when the argument is a duration
						return "(extract(day from ?3-?2)/7)";
					case DAY:
						return "extract(day from ?3-?2)";
					//in order to avoid multiple calls to extract(),
					//we use extract(epoch from x - y) * factor for
					//all the following units:

					// Note that CockroachDB also has an extract_duration function which returns an int,
					// but we don't use that here because it is deprecated since v20.
					// We need to use round() instead of cast(... as int) because extract epoch returns
					// float8 which can cause loss-of-precision in some cases
					// https://github.com/cockroachdb/cockroach/issues/72523
					case HOUR:
					case MINUTE:
					case SECOND:
					case NANOSECOND:
					case NATIVE:
						return "round(extract(epoch from ?3-?2)" + EPOCH.conversionFactor( unit, this ) + ")::int";
					default:
						throw new SemanticException( "unrecognized field: " + unit );
				}
			}
			else {
				switch (unit) {
					case YEAR:
						return "extract(year from ?3-?2)";
					case QUARTER:
						return "(extract(year from ?3-?2)*4+extract(month from ?3-?2)//3)";
					case MONTH:
						return "(extract(year from ?3-?2)*12+extract(month from ?3-?2))";
					// Prior to v20, Cockroach didn't support extracting from an interval/duration,
					// so we use the extract_duration function
					case WEEK:
						return "extract_duration(hour from ?3-?2)/168";
					case DAY:
						return "extract_duration(hour from ?3-?2)/24";
					case NANOSECOND:
						return "extract_duration(microsecond from ?3-?2)*1e3";
					default:
						return "extract_duration(?1 from ?3-?2)";
				}
			}
		}
	}

	@Override
	public String translateDurationField(TemporalUnit unit) {
		return unit==NATIVE
				? "microsecond"
				: super.translateDurationField(unit);
	}

	@Override
	public void appendDatetimeFormat(SqlAppender appender, String format) {
		appender.appendSql( SpannerDialect.datetimeFormat( format ).result() );
	}

	@Override
	public LimitHandler getLimitHandler() {
		return OffsetFetchLimitHandler.INSTANCE;
	}

	@Override
	public String getForUpdateString(String aliases) {
		return getForUpdateString() + " of " + aliases;
	}

	@Override
	public String getForUpdateString(LockOptions lockOptions) {
		// Support was added in 20.1: https://www.cockroachlabs.com/docs/v20.1/select-for-update.html
		if ( getVersion().isBefore( 20, 1 ) ) {
			return "";
		}
		return super.getForUpdateString( lockOptions );
	}

	@Override
	public String getForUpdateString() {
		return getVersion().isBefore( 20, 1 ) ? "" : " for update";
	}

	@Override
	public String getForUpdateString(String aliases, LockOptions lockOptions) {
		// Support was added in 20.1: https://www.cockroachlabs.com/docs/v20.1/select-for-update.html
		if ( getVersion().isBefore( 20, 1 ) ) {
			return "";
		}
		final LockMode lockMode = lockOptions.getLockMode();
		return switch ( lockMode ) {
			case PESSIMISTIC_READ -> getReadLockString( aliases, lockOptions.getTimeout() );
			case PESSIMISTIC_WRITE -> getWriteLockString( aliases, lockOptions.getTimeout() );
			case UPGRADE_NOWAIT, PESSIMISTIC_FORCE_INCREMENT -> getForUpdateNowaitString( aliases );
			case UPGRADE_SKIPLOCKED -> getForUpdateSkipLockedString( aliases );
			default -> "";
		};
	}

	private String withTimeout(String lockString, Timeout timeout) {
		return withTimeout( lockString, timeout.milliseconds() );
	}

	private String withTimeout(String lockString, int timeout) {
		return switch ( timeout ) {
			case Timeouts.NO_WAIT_MILLI -> supportsNoWait() ? lockString + " nowait" : lockString;
			case Timeouts.SKIP_LOCKED_MILLI -> supportsSkipLocked() ? lockString + " skip locked" : lockString;
			default -> lockString;
		};
	}

	@Override
	public String getWriteLockString(Timeout timeout) {
		return withTimeout( getForUpdateString(), timeout );
	}

	@Override
	public String getWriteLockString(String aliases, Timeout timeout) {
		return withTimeout( getForUpdateString( aliases ), timeout );
	}

	@Override
	public String getReadLockString(Timeout timeout) {
		return withTimeout(" for share", timeout );
	}

	@Override
	public String getReadLockString(String aliases, Timeout timeout) {
		return withTimeout(" for share of " + aliases, timeout );
	}

	@Override
	public String getWriteLockString(int timeout) {
		return withTimeout( getForUpdateString(), timeout );
	}

	@Override
	public String getWriteLockString(String aliases, int timeout) {
		return withTimeout( getForUpdateString( aliases ), timeout );
	}

	@Override
	public String getReadLockString(int timeout) {
		return withTimeout(" for share", timeout );
	}

	@Override
	public String getReadLockString(String aliases, int timeout) {
		return withTimeout(" for share of " + aliases, timeout );
	}

	@Override
	public String getForUpdateNowaitString() {
		return supportsNoWait()
				? getForUpdateString() + " nowait"
				: getForUpdateString();
	}

	@Override
	public String getForUpdateNowaitString(String aliases) {
		return supportsNoWait()
				? getForUpdateString( aliases ) + " nowait"
				: getForUpdateString( aliases );
	}

	@Override
	public String getForUpdateSkipLockedString() {
		return supportsSkipLocked()
				? getForUpdateString() + " skip locked"
				: getForUpdateString();
	}

	@Override
	public String getForUpdateSkipLockedString(String aliases) {
		return supportsSkipLocked()
				? getForUpdateString( aliases ) + " skip locked"
				: getForUpdateString( aliases );
	}

	@Override
	public LockingSupport getLockingSupport() {
		return getVersion().isSameOrAfter( 20, 1 )
				? COCKROACH_LOCKING_SUPPORT
				: LEGACY_COCKROACH_LOCKING_SUPPORT;
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
	public boolean supportsOffsetInSubquery() {
		return true;
	}

	@Override
	public boolean supportsWindowFunctions() {
		return true;
	}

	@Override
	public boolean supportsLateral() {
		return getVersion().isSameOrAfter( 20, 1 );
	}

	@Override
	public FunctionalDependencyAnalysisSupport getFunctionalDependencyAnalysisSupport() {
		return FunctionalDependencyAnalysisSupportImpl.TABLE_REFERENCE;
	}

	@Override
	public NameQualifierSupport getNameQualifierSupport() {
		// This method is overridden so the correct value will be returned when
		// DatabaseMetaData is not available.
		return NameQualifierSupport.SCHEMA;
	}

	@Override
	public IdentifierHelper buildIdentifierHelper(IdentifierHelperBuilder builder, DatabaseMetaData metadata)
			throws SQLException {

		if ( metadata == null ) {
			builder.setUnquotedCaseStrategy( IdentifierCaseStrategy.LOWER );
			builder.setQuotedCaseStrategy( IdentifierCaseStrategy.MIXED );
		}

		return super.buildIdentifierHelper( builder, metadata );
	}

	@Override
	public ViolatedConstraintNameExtractor getViolatedConstraintNameExtractor() {
		return EXTRACTOR;
	}

	/**
	 * Constraint-name extractor for Postgres constraint violation exceptions.
	 * Originally contributed by Denny Bartelt.
	 */
	private static final ViolatedConstraintNameExtractor EXTRACTOR =
			new TemplatedViolatedConstraintNameExtractor( sqle -> {
				final String sqlState = JdbcExceptionHelper.extractSqlState( sqle );
				if ( sqlState == null ) {
					return null;
				}
				switch ( Integer.parseInt( sqlState ) ) {
					// CHECK VIOLATION
					case 23514:
						return extractUsingTemplate( "violates check constraint \"","\"", sqle.getMessage() );
					// UNIQUE VIOLATION
					case 23505:
						return extractUsingTemplate( "violates unique constraint \"","\"", sqle.getMessage() );
					// FOREIGN KEY VIOLATION
					case 23503:
						return extractUsingTemplate( "violates foreign key constraint \"","\"", sqle.getMessage() );
					// NOT NULL VIOLATION
					case 23502:
						return extractUsingTemplate( "null value in column \"","\" violates not-null constraint", sqle.getMessage() );
					// TODO: RESTRICT VIOLATION
					case 23001:
						return null;
					// ALL OTHER
					default:
						return null;
				}
			} );

	@Override
	public SQLExceptionConversionDelegate buildSQLExceptionConversionDelegate() {
		return (sqlException, message, sql) -> {
			final String sqlState = JdbcExceptionHelper.extractSqlState( sqlException );
			if ( sqlState == null ) {
				return null;
			}
			switch ( sqlState ) {
				case "40P01":
					// DEADLOCK DETECTED
					return new LockAcquisitionException( message, sqlException, sql);
				case "55P03":
					// LOCK NOT AVAILABLE
					return new PessimisticLockException( message, sqlException, sql);
				case "57014":
					return new QueryTimeoutException( message, sqlException, sql );
				default:
					// returning null allows other delegates to operate
					return null;
			}
		};
	}

	@Override
	public int getDefaultIntervalSecondScale() {
		// The maximum scale for `interval second` is 6 unfortunately
		return 6;
	}

// CockroachDB doesn't support this by default. See sql.multiple_modifications_of_table.enabled
//
//	@Override
//	public SqmMultiTableMutationStrategy getFallbackSqmMutationStrategy(
//			EntityMappingType rootEntityDescriptor,
//			RuntimeModelCreationContext runtimeModelCreationContext) {
//		return new CteMutationStrategy( rootEntityDescriptor, runtimeModelCreationContext );
//	}
//
//	@Override
//	public SqmMultiTableInsertStrategy getFallbackSqmInsertStrategy(
//			EntityMappingType rootEntityDescriptor,
//			RuntimeModelCreationContext runtimeModelCreationContext) {
//		return new CteInsertStrategy( rootEntityDescriptor, runtimeModelCreationContext );
//	}

	@Override
	public DmlTargetColumnQualifierSupport getDmlTargetColumnQualifierSupport() {
		return DmlTargetColumnQualifierSupport.TABLE_ALIAS;
	}

	@Override
	public boolean supportsFromClauseInUpdate() {
		return true;
	}

	@Override
	public boolean supportsRowConstructor() {
		return true;
	}

	@Override
	public boolean supportsArrayConstructor() {
		return true;
	}

	@Override
	public boolean supportsRowValueConstructorSyntaxInQuantifiedPredicates() {
		return false;
	}

}
