/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.hibernate.Length;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.QueryTimeoutException;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.boot.model.relational.QualifiedSequenceName;
import org.hibernate.boot.model.relational.Sequence;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.dialect.aggregate.AggregateSupport;
import org.hibernate.dialect.aggregate.SQLServerAggregateSupport;
import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.dialect.function.CountFunction;
import org.hibernate.dialect.function.SQLServerFormatEmulation;
import org.hibernate.dialect.function.SqlServerConvertTruncFunction;
import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.dialect.identity.SQLServerIdentityColumnSupport;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.dialect.pagination.SQLServer2012LimitHandler;
import org.hibernate.dialect.sequence.SQLServer16SequenceSupport;
import org.hibernate.dialect.sequence.SQLServerSequenceSupport;
import org.hibernate.dialect.sequence.SequenceSupport;
import org.hibernate.dialect.sql.ast.SQLServerSqlAstTranslator;
import org.hibernate.dialect.type.SQLServerCastingXmlArrayJdbcTypeConstructor;
import org.hibernate.dialect.type.SQLServerCastingXmlJdbcType;
import org.hibernate.dialect.unique.AlterTableUniqueIndexDelegate;
import org.hibernate.dialect.unique.UniqueDelegate;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.engine.jdbc.dialect.spi.BasicSQLExceptionConverter;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.jdbc.env.spi.IdentifierCaseStrategy;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelperBuilder;
import org.hibernate.engine.jdbc.env.spi.NameQualifierSupport;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.ConstraintViolationException.ConstraintKind;
import org.hibernate.exception.LockTimeoutException;
import org.hibernate.exception.spi.SQLExceptionConversionDelegate;
import org.hibernate.exception.spi.TemplatedViolatedConstraintNameExtractor;
import org.hibernate.exception.spi.ViolatedConstraintNameExtractor;
import org.hibernate.mapping.AggregateColumn;
import org.hibernate.mapping.CheckConstraint;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Table;
import org.hibernate.persister.entity.mutation.EntityMutationTarget;
import org.hibernate.procedure.internal.SQLServerCallableStatementSupport;
import org.hibernate.procedure.spi.CallableStatementSupport;
import org.hibernate.query.sqm.CastType;
import org.hibernate.query.common.FetchClauseType;
import org.hibernate.query.sqm.IntervalType;
import org.hibernate.query.common.TemporalUnit;
import org.hibernate.query.sqm.TrimSpec;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.sql.ast.SqlAstNodeRenderingMode;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.spi.StandardSqlAstTranslatorFactory;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.sql.model.MutationOperation;
import org.hibernate.sql.model.internal.OptionalTableUpdate;
import org.hibernate.tool.schema.internal.StandardSequenceExporter;
import org.hibernate.tool.schema.internal.StandardTableExporter;
import org.hibernate.tool.schema.spi.Exporter;
import org.hibernate.type.BasicType;
import org.hibernate.type.BasicTypeRegistry;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.TimestampUtcAsJdbcTimestampJdbcType;
import org.hibernate.type.descriptor.jdbc.TinyIntAsSmallIntJdbcType;
import org.hibernate.type.descriptor.jdbc.UUIDJdbcType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;
import org.hibernate.type.descriptor.sql.internal.DdlTypeImpl;
import org.hibernate.type.descriptor.sql.spi.DdlTypeRegistry;

import jakarta.persistence.TemporalType;

import static org.hibernate.cfg.DialectSpecificSettings.SQL_SERVER_COMPATIBILITY_LEVEL;
import static org.hibernate.exception.spi.TemplatedViolatedConstraintNameExtractor.extractUsingTemplate;
import static org.hibernate.internal.util.JdbcExceptionHelper.extractErrorCode;
import static org.hibernate.internal.util.JdbcExceptionHelper.extractSqlState;
import static org.hibernate.internal.util.StringHelper.isBlank;
import static org.hibernate.internal.util.StringHelper.isNotEmpty;
import static org.hibernate.internal.util.config.ConfigurationHelper.getInteger;
import static org.hibernate.query.common.TemporalUnit.NANOSECOND;
import static org.hibernate.query.sqm.produce.function.FunctionParameterType.INTEGER;
import static org.hibernate.type.SqlTypes.BLOB;
import static org.hibernate.type.SqlTypes.CLOB;
import static org.hibernate.type.SqlTypes.DATE;
import static org.hibernate.type.SqlTypes.DOUBLE;
import static org.hibernate.type.SqlTypes.GEOGRAPHY;
import static org.hibernate.type.SqlTypes.GEOMETRY;
import static org.hibernate.type.SqlTypes.LONG32NVARCHAR;
import static org.hibernate.type.SqlTypes.LONG32VARBINARY;
import static org.hibernate.type.SqlTypes.LONG32VARCHAR;
import static org.hibernate.type.SqlTypes.NCLOB;
import static org.hibernate.type.SqlTypes.NVARCHAR;
import static org.hibernate.type.SqlTypes.OTHER;
import static org.hibernate.type.SqlTypes.SQLXML;
import static org.hibernate.type.SqlTypes.TIME;
import static org.hibernate.type.SqlTypes.TIMESTAMP;
import static org.hibernate.type.SqlTypes.TIMESTAMP_WITH_TIMEZONE;
import static org.hibernate.type.SqlTypes.TIME_WITH_TIMEZONE;
import static org.hibernate.type.SqlTypes.UUID;
import static org.hibernate.type.SqlTypes.VARBINARY;
import static org.hibernate.type.SqlTypes.VARCHAR;
import static org.hibernate.type.SqlTypes.XML_ARRAY;
import static org.hibernate.type.descriptor.DateTimeUtils.appendAsDate;
import static org.hibernate.type.descriptor.DateTimeUtils.appendAsTime;
import static org.hibernate.type.descriptor.DateTimeUtils.appendAsTimestampWithMicros;
import static org.hibernate.type.descriptor.DateTimeUtils.appendAsTimestampWithMillis;

/**
 * A dialect for Microsoft SQL Server 2014 and above.
 * <p>
 * Please refer to the
 * <a href="https://learn.microsoft.com/en-us/sql/t-sql/language-reference">SQL Server documentation</a>.
 *
 * @author Gavin King
 */
public class SQLServerDialect extends AbstractTransactSQLDialect {
	private final static DatabaseVersion MINIMUM_VERSION = DatabaseVersion.make( 12, 0 );

	/**
	 * NOTE : 2100 is the documented limit supposedly - but in my testing, sending
	 * 2100 parameters fails saying it must be less than 2100.
	 */
	private static final int PARAM_LIST_SIZE_LIMIT = 2048;

	// See microsoft.sql.Types.GEOMETRY
	private static final int GEOMETRY_TYPE_CODE = -157;
	// See microsoft.sql.Types.GEOGRAPHY
	private static final int GEOGRAPHY_TYPE_CODE = -158;

	private final StandardSequenceExporter exporter;
	private final UniqueDelegate uniqueDelegate = new AlterTableUniqueIndexDelegate(this);

	private final SizeStrategy sizeStrategy = new SizeStrategyImpl() {
		@Override
		public Size resolveSize(
				JdbcType jdbcType,
				JavaType<?> javaType,
				Integer precision,
				Integer scale,
				Long length) {
			return switch ( jdbcType.getDdlTypeCode() ) {
				case BLOB, CLOB, NCLOB ->
						super.resolveSize( jdbcType, javaType, precision, scale,
								length == null ? getDefaultLobLength() : length );
				default -> super.resolveSize( jdbcType, javaType, precision, scale, length );
			};
		}
	};
	private final StandardTableExporter sqlServerTableExporter = new StandardTableExporter( this ) {
		@Override
		protected void applyAggregateColumnCheck(StringBuilder buf, AggregateColumn aggregateColumn) {
			final JdbcType jdbcType = aggregateColumn.getType().getJdbcType();
			if ( !jdbcType.isXml() ) { // XML columns can't have check constraints
				super.applyAggregateColumnCheck( buf, aggregateColumn );
			}
		}
	};

	public SQLServerDialect() {
		this( MINIMUM_VERSION );
	}

	public SQLServerDialect(DatabaseVersion version) {
		super(version);
		exporter = new SqlServerSequenceExporter( this );
	}

	public SQLServerDialect(DialectResolutionInfo info) {
		this( staticDetermineDatabaseVersion( info ) );
		registerKeywords( info );
	}

	@Override
	public DatabaseVersion determineDatabaseVersion(DialectResolutionInfo info) {
		return staticDetermineDatabaseVersion(info);
	}

	// Static version necessary to call from constructor
	private static DatabaseVersion staticDetermineDatabaseVersion(DialectResolutionInfo info) {
		final Integer compatibilityLevel = getCompatibilityLevel( info );
		if ( compatibilityLevel != null ) {
			final int majorVersion = compatibilityLevel / 10;
			final int minorVersion = compatibilityLevel % 10;
			return DatabaseVersion.make( majorVersion, minorVersion );
		}
		return info.makeCopyOrDefault( MINIMUM_VERSION );
	}

	private static Integer getCompatibilityLevel(DialectResolutionInfo info) {
		final DatabaseMetaData databaseMetaData = info.getDatabaseMetadata();
		if ( databaseMetaData != null ) {
			try ( var statement = databaseMetaData.getConnection().createStatement() ) {
				final ResultSet resultSet =
						statement.executeQuery( "SELECT compatibility_level FROM sys.databases where name = db_name();" );
				if ( resultSet.next() ) {
					return resultSet.getInt( 1 );
				}
			}
			catch (SQLException e) {
				throw BasicSQLExceptionConverter.INSTANCE.convert( e );
			}
		}

		// default to the dialect-specific configuration setting
		return getInteger( SQL_SERVER_COMPATIBILITY_LEVEL, info.getConfigurationValues() );
	}

	@Override
	protected DatabaseVersion getMinimumSupportedVersion() {
		return MINIMUM_VERSION;
	}

	@Override
	protected void registerDefaultKeywords() {
		super.registerDefaultKeywords();
		registerKeyword( "top" );
		registerKeyword( "key" );
	}

	@Override
	protected String columnType(int sqlTypeCode) {
		return switch (sqlTypeCode) {
			// there is no 'double' type in SQL server
			// but 'float' is double precision by default
			case DOUBLE -> "float";
			// Prefer 'varchar(max)' and 'varbinary(max)' to
			// the deprecated TEXT and IMAGE types. Note that
			// the length of a VARCHAR or VARBINARY column must
			// be either between 1 and 8000 or exactly MAX, and
			// the length of an NVARCHAR column must be either
			// between 1 and 4000 or exactly MAX. (HHH-3965)
			case CLOB -> "varchar(max)";
			case NCLOB -> "nvarchar(max)";
			case BLOB -> "varbinary(max)";
			case DATE -> "date";
			case TIME -> "time";
			case TIMESTAMP -> "datetime2($p)";
			case TIME_WITH_TIMEZONE, TIMESTAMP_WITH_TIMEZONE -> "datetimeoffset($p)";
			default -> super.columnType(sqlTypeCode);
		};
	}

	@Override
	protected String castType(int sqlTypeCode) {
		return switch (sqlTypeCode) {
			case VARCHAR, LONG32VARCHAR, CLOB -> "varchar(max)";
			case NVARCHAR, LONG32NVARCHAR, NCLOB -> "nvarchar(max)";
			case VARBINARY, LONG32VARBINARY, BLOB -> "varbinary(max)";
			default -> super.castType( sqlTypeCode );
		};
	}

	@Override
	protected void registerColumnTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		super.registerColumnTypes( typeContributions, serviceRegistry );
		final DdlTypeRegistry ddlTypeRegistry = typeContributions.getTypeConfiguration().getDdlTypeRegistry();
		ddlTypeRegistry.addDescriptor( new DdlTypeImpl( GEOMETRY, "geometry", this ) );
		ddlTypeRegistry.addDescriptor( new DdlTypeImpl( GEOGRAPHY, "geography", this ) );
		ddlTypeRegistry.addDescriptor( new DdlTypeImpl( SQLXML, "xml", this ) );
		ddlTypeRegistry.addDescriptor( new DdlTypeImpl( UUID, "uniqueidentifier", this ) );
	}

	@Override
	public int getPreferredSqlTypeCodeForArray() {
		return XML_ARRAY;
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
				if ( columnTypeName.equals("uniqueidentifier") ) {
					jdbcTypeCode = UUID;
				}
				break;
			case GEOMETRY_TYPE_CODE:
				jdbcTypeCode = GEOMETRY;
				break;
			case GEOGRAPHY_TYPE_CODE:
				jdbcTypeCode = GEOGRAPHY;
				break;
		}
		return super.resolveSqlTypeDescriptor( columnTypeName, jdbcTypeCode, precision, scale, jdbcTypeRegistry );
	}

	@Override
	public int getMaxVarcharLength() {
		return 8000;
	}

	@Override
	public int getMaxNVarcharLength() {
		return 4000;
	}

	@Override
	public TimeZoneSupport getTimeZoneSupport() {
		return TimeZoneSupport.NATIVE;
	}

	@Override
	public long getDefaultLobLength() {
		// this is essentially the only legal length for
		// a "lob" in SQL Server, i.e. the value of MAX
		// (caveat: for NVARCHAR it is half this value)
		return Length.LONG32;
	}

	@Override
	public int getMaxIdentifierLength() {
		return 128;
	}

	@Override
	public void contributeTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		super.contributeTypes( typeContributions, serviceRegistry );
		// Need to bind as java.sql.Timestamp because reading OffsetDateTime from a "datetime2" column fails
		typeContributions.contributeJdbcType( TimestampUtcAsJdbcTimestampJdbcType.INSTANCE );
		typeContributions.getTypeConfiguration().getJdbcTypeRegistry()
				.addDescriptor( Types.TINYINT, TinyIntAsSmallIntJdbcType.INSTANCE );
		typeContributions.contributeJdbcType( SQLServerCastingXmlJdbcType.INSTANCE );
		typeContributions.contributeJdbcType( UUIDJdbcType.INSTANCE );
		typeContributions.contributeJdbcTypeConstructor( SQLServerCastingXmlArrayJdbcTypeConstructor.INSTANCE );
	}

	@Override
	public void initializeFunctionRegistry(FunctionContributions functionContributions) {
		super.initializeFunctionRegistry( functionContributions );

		final BasicTypeRegistry basicTypeRegistry =
				functionContributions.getTypeConfiguration().getBasicTypeRegistry();
		final BasicType<Date> dateType = basicTypeRegistry.resolve( StandardBasicTypes.DATE );
		final BasicType<Date> timeType = basicTypeRegistry.resolve( StandardBasicTypes.TIME );
		final BasicType<Date> timestampType = basicTypeRegistry.resolve( StandardBasicTypes.TIMESTAMP );

		final var functionFactory = new CommonFunctionFactory( functionContributions );

		// For SQL-Server we need to cast certain arguments to varchar(max) to be able to concat them
		functionContributions.getFunctionRegistry().register(
				"count",
				new CountFunction(
						this,
						functionContributions.getTypeConfiguration(),
						SqlAstNodeRenderingMode.DEFAULT,
						"count_big",
						"+",
						"varchar(max)",
						true,
						"varbinary(max)"
				)
		);

		// AVG by default uses the input type, so we possibly need to cast the argument type, hence a special function
		functionFactory.avg_castingNonDoubleArguments( this, SqlAstNodeRenderingMode.DEFAULT );

		functionFactory.log_log();

		functionFactory.round_round();
		functionFactory.everyAny_minMaxIif();
		functionFactory.octetLength_pattern( "datalength(?1)" );
		functionFactory.bitLength_pattern( "datalength(?1)*8" );

		functionFactory.locate_charindex();
		functionFactory.stddevPopSamp_stdevp();
		functionFactory.varPopSamp_varp();

		functionContributions.getFunctionRegistry().register(
				"format",
				new SQLServerFormatEmulation( functionContributions.getTypeConfiguration() )
		);

		//actually translate() was added in 2017 but
		//it's not worth adding a new dialect for that!
		functionFactory.translate();

		functionFactory.median_percentileCont( true );

		functionContributions.getFunctionRegistry().namedDescriptorBuilder( "datefromparts" )
				.setInvariantType( dateType )
				.setExactArgumentCount( 3 )
				.setParameterTypes(INTEGER)
				.register();
		functionContributions.getFunctionRegistry().namedDescriptorBuilder( "timefromparts" )
				.setInvariantType( timeType )
				.setExactArgumentCount( 5 )
				.setParameterTypes(INTEGER)
				.register();
		functionContributions.getFunctionRegistry().namedDescriptorBuilder( "smalldatetimefromparts" )
				.setInvariantType( timestampType )
				.setExactArgumentCount( 5 )
				.setParameterTypes(INTEGER)
				.register();
		functionContributions.getFunctionRegistry().namedDescriptorBuilder( "datetimefromparts" )
				.setInvariantType( timestampType )
				.setExactArgumentCount( 7 )
				.setParameterTypes(INTEGER)
				.register();
		functionContributions.getFunctionRegistry().namedDescriptorBuilder( "datetime2fromparts" )
				.setInvariantType( timestampType )
				.setExactArgumentCount( 8 )
				.setParameterTypes(INTEGER)
				.register();
		functionContributions.getFunctionRegistry().namedDescriptorBuilder( "datetimeoffsetfromparts" )
				.setInvariantType( timestampType )
				.setExactArgumentCount( 10 )
				.setParameterTypes(INTEGER)
				.register();

		functionFactory.windowFunctions();
		functionFactory.inverseDistributionOrderedSetAggregates_windowEmulation();
		functionFactory.hypotheticalOrderedSetAggregates_windowEmulation();
		if ( getVersion().isSameOrAfter( 13 ) ) {
			functionFactory.jsonValue_sqlserver();
			functionFactory.jsonQuery_sqlserver();
			functionFactory.jsonExists_sqlserver( getVersion().isSameOrAfter( 16 ) );
			functionFactory.jsonObject_sqlserver( getVersion().isSameOrAfter( 16 ) );
			functionFactory.jsonArray_sqlserver( getVersion().isSameOrAfter( 16 ) );
			functionFactory.jsonSet_sqlserver();
			functionFactory.jsonRemove_sqlserver();
			functionFactory.jsonReplace_sqlserver( getVersion().isSameOrAfter( 16 ) );
			functionFactory.jsonInsert_sqlserver( getVersion().isSameOrAfter( 16 ) );
			functionFactory.jsonArrayAppend_sqlserver( getVersion().isSameOrAfter( 16 ) );
			functionFactory.jsonArrayInsert_sqlserver();
			functionFactory.jsonTable_sqlserver();
		}

		functionFactory.xmlelement_sqlserver();
		functionFactory.xmlcomment_sqlserver();
		functionFactory.xmlforest_sqlserver();
		functionFactory.xmlconcat_sqlserver();
		functionFactory.xmlpi_sqlserver();
		functionFactory.xmlquery_sqlserver();
		functionFactory.xmlexists_sqlserver();
		functionFactory.xmlagg_sqlserver();
		functionFactory.xmltable_sqlserver();

		functionFactory.unnest_sqlserver();

		if ( getVersion().isSameOrAfter( 14 ) ) {
			functionFactory.listagg_stringAggWithinGroup( "varchar(max)" );
			functionFactory.jsonArrayAgg_sqlserver( getVersion().isSameOrAfter( 16 ) );
			functionFactory.jsonObjectAgg_sqlserver( getVersion().isSameOrAfter( 16 ) );
		}
		if ( getVersion().isSameOrAfter( 16 ) ) {
			functionFactory.leastGreatest();
			functionFactory.dateTrunc_datetrunc();
			functionFactory.trunc_round_datetrunc();
			functionFactory.generateSeries_sqlserver( getMaximumSeriesSize() );
		}
		else {
			functionContributions.getFunctionRegistry().register(
					"trunc",
					new SqlServerConvertTruncFunction( functionContributions.getTypeConfiguration() )
			);
			functionContributions.getFunctionRegistry().registerAlternateKey( "truncate", "trunc" );
			if ( supportsRecursiveCTE() ) {
				functionFactory.generateSeries_recursive( getMaximumSeriesSize(), false, false );
			}
		}

		functionFactory.hex( "convert(varchar(MAX), ?1, 2)" );
		functionFactory.sha( "hashbytes('SHA2_256', ?1)" );
		functionFactory.md5( "hashbytes('MD5', ?1)" );
	}

	/**
	 * SQL Server doesn't support the {@code generate_series} function or {@code lateral} recursive CTEs,
	 * so it has to be emulated with a top level recursive CTE which requires an upper bound on the amount
	 * of elements that the series can return.
	 */
	protected int getMaximumSeriesSize() {
		if ( getVersion().isSameOrAfter( 16 ) ) {
			return 10000;
		}
		else {
			// The maximum recursion depth of SQL Server
			return 100;
		}
	}

	@Override
	public String trimPattern(TrimSpec specification, boolean isWhitespace) {
		if ( getVersion().isSameOrAfter( 16 ) ) {
			return switch (specification) {
				case BOTH -> isWhitespace ? "trim(?1)" : "trim(?2 from ?1)";
				case LEADING -> isWhitespace ? "ltrim(?1)" : "ltrim(?1,?2)";
				case TRAILING -> isWhitespace ? "rtrim(?1)" : "rtrim(?1,?2)";
			};
		}
		return super.trimPattern( specification, isWhitespace );
	}

	@Override
	public SqlAstTranslatorFactory getSqlAstTranslatorFactory() {
		return new StandardSqlAstTranslatorFactory() {
			@Override
			protected <T extends JdbcOperation> SqlAstTranslator<T> buildTranslator(
					SessionFactoryImplementor sessionFactory, Statement statement) {
				return new SQLServerSqlAstTranslator<>( sessionFactory, statement );
			}
		};
	}

	@Override
	public AggregateSupport getAggregateSupport() {
		return SQLServerAggregateSupport.valueOf( this );
	}

	@Override
	public SizeStrategy getSizeStrategy() {
		return sizeStrategy;
	}

	@Override
	public String castPattern(CastType from, CastType to) {
		if ( to == CastType.STRING ) {
			switch ( from ) {
				case TIMESTAMP:
					// SQL Server uses yyyy-MM-dd HH:mm:ss.nnnnnnn by default when doing a cast, but only need second precision
					return "format(?1,'yyyy-MM-dd HH:mm:ss')";
				case TIME:
					// SQL Server uses HH:mm:ss.nnnnnnn by default when doing a cast, but only need second precision
					// SQL Server requires quoting of ':' in time formats and the use of 'hh' instead of 'HH'
					return "format(?1,'hh\\:mm\\:ss')";
			}
		}
		return super.castPattern( from, to );
	}

	@Override
	public String currentTimestamp() {
		return "sysdatetime()";
	}

	@Override
	public IdentifierHelper buildIdentifierHelper(IdentifierHelperBuilder builder, DatabaseMetaData metadata)
			throws SQLException {

		if ( metadata == null ) {
			// TODO: if DatabaseMetaData != null, unquoted case strategy is set to IdentifierCaseStrategy.UPPER
			//       Check to see if this setting is correct.
			builder.setUnquotedCaseStrategy( IdentifierCaseStrategy.MIXED );
			builder.setQuotedCaseStrategy( IdentifierCaseStrategy.MIXED );
		}

		return super.buildIdentifierHelper( builder, metadata );
	}

	@Override
	public String currentTime() {
		return "convert(time,getdate())";
	}

	@Override
	public String currentDate() {
		return "convert(date,getdate())";
	}

	@Override
	public String currentTimestampWithTimeZone() {
		return "sysdatetimeoffset()";
	}

	@Override
	public String getNoColumnsInsertString() {
		return "default values";
	}

	@Override
	public LimitHandler getLimitHandler() {
		return SQLServer2012LimitHandler.INSTANCE;
	}

	@Override
	public boolean supportsValuesList() {
		return true;
	}

	@Override
	public boolean supportsDistinctFromPredicate() {
		return getVersion().isSameOrAfter( 16 );
	}

	@Override
	public char closeQuote() {
		return ']';
	}

	@Override
	public String getCurrentSchemaCommand() {
		return "select schema_name()";
	}

	@Override
	public boolean supportsIfExistsBeforeTableName() {
		return getVersion().isSameOrAfter( 16 ) || super.supportsIfExistsBeforeTableName();
	}

	@Override
	public boolean supportsIfExistsBeforeConstraintName() {
		return getVersion().isSameOrAfter( 16 ) || super.supportsIfExistsBeforeConstraintName();
	}

	@Override
	public char openQuote() {
		return '[';
	}

	@Override
	public String appendLockHint(LockOptions lockOptions, String tableName) {
		final LockMode lockMode = lockModeForAlias( lockOptions, tableName );
		final int timeOut = lockOptions.getTimeOut();

		final String writeLockStr = timeOut == LockOptions.SKIP_LOCKED ? "updlock" : "updlock,holdlock";
		final String readLockStr = timeOut == LockOptions.SKIP_LOCKED ? "updlock" : "holdlock";

		final String noWaitStr = timeOut == LockOptions.NO_WAIT ? ",nowait" : "";
		final String skipLockStr = timeOut == LockOptions.SKIP_LOCKED ? ",readpast" : "";

		return tableName + switch (lockMode) {
			case PESSIMISTIC_WRITE, WRITE -> " with (" + writeLockStr + ",rowlock" + noWaitStr + skipLockStr + ")";
			case PESSIMISTIC_READ ->  " with (" + readLockStr + ",rowlock" + noWaitStr + skipLockStr + ")";
			case UPGRADE_SKIPLOCKED -> " with (updlock,rowlock,readpast" + noWaitStr + ")";
			case UPGRADE_NOWAIT -> " with (updlock,holdlock,rowlock,nowait)";
			default -> "";
		};
	}

	private static LockMode lockModeForAlias(LockOptions lockOptions, String tableName) {
		final LockMode lockMode = lockOptions.getAliasSpecificLockMode( tableName );
		return lockMode == null ? lockOptions.getLockMode() : lockMode;
	}


	/**
	 * The current_timestamp is more accurate, but only known to be supported in SQL Server 7.0 and later and
	 * Sybase not known to support it at all
	 * <p>
	 * {@inheritDoc}
	 */
	@Override
	public String getCurrentTimestampSelectString() {
		return "select current_timestamp";
	}

	// Overridden informational metadata ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public boolean addPartitionKeyToPrimaryKey() {
		return true;
	}

	@Override
	public boolean supportsResultSetPositionQueryMethodsOnForwardOnlyCursor() {
		return false;
	}

	@Override
	public boolean supportsCircularCascadeDeleteConstraints() {
		// SQL Server (at least up through 2005) does not support defining
		// cascade delete constraints which can circle back to the mutating
		// table
		return false;
	}

	@Override
	public boolean supportsLobValueChangePropagation() {
		// note: at least my local SQL Server 2005 Express shows this not working...
		return false;
	}

	@Override
	public boolean doesReadCommittedCauseWritersToBlockReaders() {
		// here assume SQLServer2005 using snapshot isolation, which does not have this problem
		return false;
	}

	@Override
	public boolean doesRepeatableReadCauseReadersToBlockWriters() {
		// here assume SQLServer2005 using snapshot isolation, which does not have this problem
		return false;
	}

	@Override
	public int getInExpressionCountLimit() {
		return PARAM_LIST_SIZE_LIMIT;
	}

	@Override
	public IdentityColumnSupport getIdentityColumnSupport() {
		return SQLServerIdentityColumnSupport.INSTANCE;
	}

	@Override
	public boolean supportsNonQueryWithCTE() {
		return true;
	}

	@Override
	public boolean supportsSkipLocked() {
		return true;
	}

	@Override
	public boolean supportsNoWait() {
		return true;
	}

	@Override
	public boolean supportsWait() {
		return false;
	}

	@Override
	public SequenceSupport getSequenceSupport() {
		return getVersion().isSameOrAfter( 16 )
				? SQLServer16SequenceSupport.INSTANCE
				: SQLServerSequenceSupport.INSTANCE;
	}

	@Override
	public String getQuerySequencesString() {
		// The upper-case name should work on both case-sensitive and case-insensitive collations.
		return "select * from INFORMATION_SCHEMA.SEQUENCES";
	}

	@Override
	public String getQueryHintString(String sql, String hints) {
		final StringBuilder buffer =
				new StringBuilder( sql.length() + hints.length() + 12 );
		final int pos = sql.indexOf( ';' );
		if ( pos > -1 ) {
			buffer.append( sql, 0, pos );
		}
		else {
			buffer.append( sql );
		}
		buffer.append( " OPTION (" ).append( hints ).append( ")" );
		if ( pos > -1 ) {
			buffer.append( ";" );
		}
		sql = buffer.toString();
		return sql;
	}

	@Override
	public boolean supportsNullPrecedence() {
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
		return true;
	}

	@Override
	public boolean supportsRecursiveCTE() {
		return true;
	}

	@Override
	public boolean supportsFetchClause(FetchClauseType type) {
		return true;
	}

	@Override
	public ViolatedConstraintNameExtractor getViolatedConstraintNameExtractor() {
		return new TemplatedViolatedConstraintNameExtractor(
				sqle -> switch ( extractErrorCode( sqle ) ) {
					case 2627, 2601 -> {
						final String message = sqle.getMessage();
						yield message.contains( "unique index " )
								? extractUsingTemplate( "unique index '", "'", message )
								: extractUsingTemplate( "'", "'", message );
					}
					case 547 -> extractUsingTemplate( "constraint \"", "\"", sqle.getMessage() );
					case 515 -> extractUsingTemplate( "column '", "'", sqle.getMessage() );
					default -> null;
				}
		);
	}

	@Override
	public SQLExceptionConversionDelegate buildSQLExceptionConversionDelegate() {
		return (sqlException, message, sql) -> {
			if ( "HY008".equals( extractSqlState( sqlException ) ) ) {
				return new QueryTimeoutException( message, sqlException, sql );
			}

			return switch ( extractErrorCode( sqlException ) ) {
				case 1222 ->
						new LockTimeoutException( message, sqlException, sql );
				case 2627, 2601 ->
						new ConstraintViolationException( message, sqlException, sql, ConstraintKind.UNIQUE,
								getViolatedConstraintNameExtractor().extractConstraintName( sqlException ) );
				case 515 ->
						new ConstraintViolationException( message, sqlException, sql, ConstraintKind.NOT_NULL,
								getViolatedConstraintNameExtractor().extractConstraintName( sqlException ) );
				case 547 -> {
					if ( message.contains( " CHECK " ) ) {
						yield new ConstraintViolationException( message, sqlException, sql, ConstraintKind.CHECK,
								getViolatedConstraintNameExtractor().extractConstraintName( sqlException ) );
					}
					else if ( message.contains( " FOREIGN KEY " ) ) {
						yield new ConstraintViolationException( message, sqlException, sql, ConstraintKind.FOREIGN_KEY,
								getViolatedConstraintNameExtractor().extractConstraintName( sqlException ) );
					}
					else {
						yield null;
					}
				}
				default -> null;
			};
		};
	}

	@Override
	public int getDefaultTimestampPrecision() {
		return 7;
	}

	/**
	 * Even though SQL Server only supports 1/10th microsecond precision,
	 * we use nanosecond as the "native" precision for datetime arithmetic
	 * since it simplifies calculations.
	 */
	@Override
	public long getFractionalSecondPrecisionInNanos() {
//		return 100; // 1/10th microsecond
		return 1;
	}

	@Override
	public String extractPattern(TemporalUnit unit) {
		return switch (unit) {
			case TIMEZONE_HOUR -> "(datepart(tz,?2)/60)";
			case TIMEZONE_MINUTE -> "(datepart(tz,?2)%60)";
			// currently Dialect.extract() doesn't need
			// to handle NANOSECOND (might change that?)
//			case NANOSECOND ->
//				// this should evaluate to a bigint type
//					"(datepart(second,?2)*1000000000+datepart(nanosecond,?2))";
			case SECOND ->
				//this should evaluate to a floating point type
					"(datepart(second,?2)+datepart(nanosecond,?2)/1000000000)";
			case EPOCH -> "datediff_big(second, '1970-01-01', ?2)";
			default -> "datepart(?1,?2)";
		};
	}

	@Override @SuppressWarnings("deprecation")
	public String timestampaddPattern(TemporalUnit unit, TemporalType temporalType, IntervalType intervalType) {
		// dateadd() supports only especially small magnitudes
		// since it casts its argument to int (and unfortunately
		// there's no dateadd_big()) so here we need to use two
		// calls to dateadd() to add a whole duration
		return switch (unit) { //use nanosecond as the "native" precision
			case NANOSECOND, NATIVE -> "dateadd(nanosecond,?2%1000000000,dateadd(second,?2/1000000000,?3))";
			// we could, in principle, use 1/10th microsecond as the "native" precision
//			case NATIVE -> "dateadd(nanosecond,?2%10000000,dateadd(second,?2/10000000,?3))";
			case SECOND -> "dateadd(nanosecond,cast(?2*1e9 as bigint)%1000000000,dateadd(second,?2,?3))";
			default -> "dateadd(?1,?2,?3)";
		};
	}

	@Override @SuppressWarnings("deprecation")
	public String timestampdiffPattern(TemporalUnit unit, TemporalType fromTemporalType, TemporalType toTemporalType) {
		if ( unit == TemporalUnit.NATIVE ) {
			//use nanosecond as the "native" precision
			return "datediff_big(nanosecond,?2,?3)";
		}
		else {
			//datediff() returns an int, and can easily
			//overflow when dealing with "physical"
			//durations, so use datediff_big()
			return unit.normalized() == NANOSECOND
					? "datediff_big(?1,?2,?3)"
					: "datediff(?1,?2,?3)";
		}
	}

	@Override
	public String translateDurationField(TemporalUnit unit) {
		//use nanosecond as the "native" precision
		return unit == TemporalUnit.NATIVE
				? "nanosecond"
				: super.translateDurationField( unit );
	}

	@Override
	public String translateExtractField(TemporalUnit unit) {
		return switch (unit) {
			//the ISO week number (behavior of "week" depends on a system property)
			case WEEK -> "isowk";
			case OFFSET -> "tz";
			default -> super.translateExtractField( unit );
		};
	}

	@Override
	public void appendDatetimeFormat(SqlAppender appender, String format) {
		appender.appendSql( datetimeFormat(format).result() );
	}

	public static Replacer datetimeFormat(String format) {
		return new Replacer( format, "'", "\"" )
				//era
				.replace("G", "g")

				//y nothing to do
				//M nothing to do

				//w no equivalent
				//W no equivalent
				//Y no equivalent

				//day of week
				.replace("EEEE", "dddd")
				.replace("EEE", "ddd")
				//e no equivalent

				//d nothing to do
				//D no equivalent

				//am pm
				.replace("a", "tt")

				//h nothing to do
				//H nothing to do

				//m nothing to do
				//s nothing to do

				//fractional seconds
				.replace("S", "F")

				//timezones
				.replace("XXX", "K") //UTC represented as "Z"
				.replace("xxx", "zzz")
				.replace("x", "zz");
	}

	@Override
	public void appendUUIDLiteral(SqlAppender appender, java.util.UUID literal) {
		appender.appendSql( "cast('" );
		appender.appendSql( literal.toString() );
		appender.appendSql( "' as uniqueidentifier)" );
	}

	@Override
	public void appendDateTimeLiteral(
			SqlAppender appender,
			TemporalAccessor temporalAccessor,
			@SuppressWarnings("deprecation")
			TemporalType precision,
			TimeZone jdbcTimeZone) {
		switch ( precision ) {
			case DATE:
				appender.appendSql( "cast('" );
				appendAsDate( appender, temporalAccessor );
				appender.appendSql( "' as date)" );
				break;
			case TIME:
				//needed because the {t ... } JDBC is just buggy
				appender.appendSql( "cast('" );
				appendAsTime( appender, temporalAccessor, supportsTemporalLiteralOffset(), jdbcTimeZone );
				appender.appendSql( "' as time)" );
				break;
			case TIMESTAMP:
				appender.appendSql( "cast('" );
				//needed because the {ts ... } JDBC escape chokes on microseconds
				if ( supportsTemporalLiteralOffset() && temporalAccessor.isSupported( ChronoField.OFFSET_SECONDS ) ) {
					appendAsTimestampWithMicros( appender, temporalAccessor, true, jdbcTimeZone );
					appender.appendSql( "' as datetimeoffset)" );
				}
				else {
					appendAsTimestampWithMicros( appender, temporalAccessor, false, jdbcTimeZone );
					appender.appendSql( "' as datetime2)" );
				}
				break;
			default:
				throw new IllegalArgumentException();
		}
	}

	@Override
	public void appendDateTimeLiteral(
			SqlAppender appender,
			Date date,
			@SuppressWarnings("deprecation")
			TemporalType precision,
			TimeZone jdbcTimeZone) {
		switch ( precision ) {
			case DATE:
				appender.appendSql( "cast('" );
				appendAsDate( appender, date );
				appender.appendSql( "' as date)" );
				break;
			case TIME:
				//needed because the {t ... } JDBC is just buggy
				appender.appendSql( "cast('" );
				appendAsTime( appender, date );
				appender.appendSql( "' as time)" );
				break;
			case TIMESTAMP:
				appender.appendSql( "cast('" );
				appendAsTimestampWithMicros( appender, date, jdbcTimeZone );
				appender.appendSql( "' as datetimeoffset)" );
				break;
			default:
				throw new IllegalArgumentException();
		}
	}

	@Override
	public void appendDateTimeLiteral(
			SqlAppender appender,
			Calendar calendar,
			@SuppressWarnings("deprecation")
			TemporalType precision,
			TimeZone jdbcTimeZone) {
		switch ( precision ) {
			case DATE:
				appender.appendSql( "cast('" );
				appendAsDate( appender, calendar );
				appender.appendSql( "' as date)" );
				break;
			case TIME:
				//needed because the {t ... } JDBC is just buggy
				appender.appendSql( "cast('" );
				appendAsTime( appender, calendar );
				appender.appendSql( "' as time)" );
				break;
			case TIMESTAMP:
				appender.appendSql( "cast('" );
				appendAsTimestampWithMillis( appender, calendar, jdbcTimeZone );
				appender.appendSql( "' as datetime2)" );
				break;
			default:
				throw new IllegalArgumentException();
		}
	}

	@Override
	public String getCreateTemporaryTableColumnAnnotation(int sqlTypeCode) {
		return switch (sqlTypeCode) {
			case Types.CHAR, Types.NCHAR, Types.VARCHAR, Types.NVARCHAR, Types.LONGVARCHAR, Types.LONGNVARCHAR ->
					"collate database_default";
			default -> "";
		};
	}

	@Override
	public String[] getDropSchemaCommand(String schemaName) {
		return getVersion().isSameOrAfter( 13 )
				? new String[] { "drop schema if exists " + schemaName }
				: super.getDropSchemaCommand( schemaName );
	}

	@Override
	public String getCreateIndexString(boolean unique) {
		// we only create unique indexes, as opposed to unique constraints,
		// when the column is nullable, so safe to infer unique => nullable
		return unique ? "create unique nonclustered index" : "create index";
	}

	@Override
	public String getCreateIndexTail(boolean unique, List<Column> columns) {
		if ( unique ) {
			final StringBuilder tail = new StringBuilder();
			for ( Column column : columns ) {
				if ( column.isNullable() ) {
					tail.append( tail.isEmpty() ? " where " : " and " )
							.append( column.getQuotedName( this ) )
							.append( " is not null" );
				}
			}
			return tail.toString();
		}
		else {
			return "";
		}
	}

	@Override
	public String getAlterColumnTypeString(String columnName, String columnType, String columnDefinition) {
		return "alter column " + columnName + " " + columnType;
	}

	@Override
	public boolean supportsAlterColumnType() {
		return true;
	}

	@Override
	public NameQualifierSupport getNameQualifierSupport() {
		return NameQualifierSupport.BOTH;
	}

	@Override
	public UniqueDelegate getUniqueDelegate() {
		return uniqueDelegate;
	}

	@Override
	public Exporter<Table> getTableExporter() {
		return this.sqlServerTableExporter;
	}

	@Override
	public Exporter<Sequence> getSequenceExporter() {
		return exporter == null ? super.getSequenceExporter() : exporter;
	}

	private static class SqlServerSequenceExporter extends StandardSequenceExporter {

		public SqlServerSequenceExporter(Dialect dialect) {
			super( dialect );
		}

		@Override
		protected String getFormattedSequenceName(QualifiedSequenceName name, Metadata metadata,
				SqlStringGenerationContext context) {
			// SQL Server does not allow the catalog in the sequence name.
			// See https://docs.microsoft.com/en-us/sql/t-sql/statements/create-sequence-transact-sql?view=sql-server-ver15&viewFallbackFrom=sql-server-ver12
			// Keeping the catalog in the name does not break on ORM, but it fails using Vert.X for Reactive.
			return context.formatWithoutCatalog( name );
		}
	}

	@Override
	public String generatedAs(String generatedAs) {
		return " as (" + generatedAs + ") persisted";
	}

	@Override
	public boolean hasDataTypeBeforeGeneratedAs() {
		return false;
	}

	// disabled foreign key constraints still prevent 'truncate table'
	// (these would help if we used 'delete' instead of 'truncate')

//	@Override
//	public String getDisableConstraintStatement(String tableName, String name) {
//		return "alter table " + tableName + " nocheck constraint " + name;
//	}
//
//	@Override
//	public String getEnableConstraintStatement(String tableName, String name) {
//		return "alter table " + tableName + " with check check constraint " + name;
//	}


	@Override
	public MutationOperation createOptionalTableUpdateOperation(
			EntityMutationTarget mutationTarget,
			OptionalTableUpdate optionalTableUpdate,
			SessionFactoryImplementor factory) {
		return new SQLServerSqlAstTranslator<>( factory, optionalTableUpdate )
				.createMergeOperation( optionalTableUpdate );
	}

	@Override
	public DmlTargetColumnQualifierSupport getDmlTargetColumnQualifierSupport() {
		return DmlTargetColumnQualifierSupport.TABLE_ALIAS;
	}

	@Override
	public boolean supportsFromClauseInUpdate() {
		return true;
	}

	@Override
	public CallableStatementSupport getCallableStatementSupport() {
		return SQLServerCallableStatementSupport.INSTANCE;
	}

	@Override
	public String getCheckConstraintString(CheckConstraint checkConstraint) {
		// The only useful option is 'NOT FOR REPLICATION'
		// and it comes before the constraint expression
		final String constraintName = checkConstraint.getName();
		final String checkWithName =
				isBlank( constraintName )
						? " check"
						: " constraint " + constraintName + " check";
		return appendCheckConstraintOptions( checkConstraint, checkWithName )
			+ " (" + checkConstraint.getConstraint() + ")";
	}

	@Override
	public String appendCheckConstraintOptions(CheckConstraint checkConstraint, String sqlCheckConstraint) {
		return isNotEmpty( checkConstraint.getOptions() )
				? sqlCheckConstraint + " " + checkConstraint.getOptions()
				: sqlCheckConstraint;
	}

	@Override
	public boolean supportsBindingNullForSetObject() {
		return true;
	}

	@Override
	public boolean supportsJoinsInDelete() {
		return true;
	}

	@Override
	public boolean supportsSimpleQueryGrouping() {
		// SQL Server is quite strict i.e. it requires `select ... union all select * from (select ...)`
		// rather than `select ... union all (select ...)` because parenthesis followed by select
		// is always treated as a subquery, which is not supported in a set operation
		return false;
	}

	@Override
	public boolean supportsRowValueConstructorSyntax() {
		return false;
	}

	@Override
	public boolean supportsWithClauseInSubquery() {
		return false;
	}

	@Override
	public boolean supportsRowValueConstructorSyntaxInQuantifiedPredicates() {
		return false;
	}

	@Override
	public boolean supportsRowValueConstructorSyntaxInInList() {
		return false;
	}

}
