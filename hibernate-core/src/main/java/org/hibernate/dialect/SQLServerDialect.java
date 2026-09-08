/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect;

import org.hibernate.dialect.identifier.spi.KeywordRegistration;

import org.hibernate.dialect.temporaltype.spi.CurrentTimestampSelection;

import org.hibernate.dialect.temporaltype.spi.TemporalOperationSupport;
import org.hibernate.dialect.temporaltype.spi.TemporalOperationSupports;

import org.hibernate.dialect.temporaltype.spi.TemporalFormatSupport;

import org.hibernate.dialect.temporaltype.spi.CurrentTemporalSupport;

import org.hibernate.dialect.temporaltype.spi.TemporalValueSemantics;
import org.hibernate.dialect.type.spi.ObjectNullBindingStrategy;

import org.hibernate.SPI;
import static org.hibernate.SPI.Role.USE;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.SUPPLY;

import org.hibernate.dialect.type.spi.StandardDdlTypes;

import org.hibernate.dialect.type.spi.TypeSizingProfile;

import org.hibernate.dialect.jdbc.spi.ParameterLimits;


import org.hibernate.dialect.function.spi.Replacer;

import org.hibernate.dialect.type.spi.TimeZoneSupport;

import org.hibernate.dialect.sql.ast.spi.DmlTargetColumnQualifierSupport;

import org.hibernate.dialect.sql.ast.spi.CteSupport;
import org.hibernate.dialect.sql.ast.spi.MutationKind;
import org.hibernate.dialect.sql.ast.spi.MutationSyntaxCapability;
import org.hibernate.dialect.sql.ast.spi.MutationSyntaxSupport;
import org.hibernate.dialect.sql.ast.spi.PredicateSupport;
import org.hibernate.dialect.sql.ast.spi.NullOrderingSupport;
import org.hibernate.dialect.sql.ast.spi.RowValueSupport;
import org.hibernate.dialect.sql.ast.spi.SetOperationSupport;
import org.hibernate.dialect.sql.ast.spi.SubquerySupport;
import org.hibernate.dialect.sql.ast.spi.ValuesListSupport;

import jakarta.persistence.TemporalType;
import org.hibernate.Length;
import org.hibernate.QueryTimeoutException;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.boot.model.relational.QualifiedSequenceName;
import org.hibernate.boot.model.relational.Sequence;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.dialect.aggregate.spi.AggregateSupport;
import org.hibernate.dialect.aggregate.internal.SQLServerAggregateSupport;
import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.dialect.function.spi.WindowFunctionSupport;
import org.hibernate.dialect.function.CountFunction;
import org.hibernate.dialect.function.SQLServerFormatEmulation;
import org.hibernate.dialect.function.SqlServerConvertTruncFunction;
import org.hibernate.dialect.identity.internal.SQLServerIdentityColumnSupport;
import org.hibernate.dialect.identity.spi.IdentityColumnSupport;
import org.hibernate.dialect.lock.internal.TransactSQLLockingSupport;
import org.hibernate.dialect.lock.spi.LockingSupport;
import org.hibernate.dialect.namespace.spi.NamespaceSupport;
import org.hibernate.dialect.namespace.spi.NamespaceSupports;
import org.hibernate.dialect.schema.spi.AlterColumnTypeRequest;
import org.hibernate.dialect.schema.spi.ColumnDefinitionRequest;
import org.hibernate.dialect.schema.spi.ExistenceCheckPlacement;
import org.hibernate.dialect.schema.spi.IfExistsSupport;
import org.hibernate.dialect.schema.spi.IndexDdlRequest;
import org.hibernate.dialect.pagination.spi.LimitHandler;
import org.hibernate.dialect.pagination.spi.SQLServer2012LimitHandler;
import org.hibernate.dialect.rowsecurity.internal.SQLServerRowLevelSecurity;
import org.hibernate.dialect.rowsecurity.spi.RowLevelSecurity;
import org.hibernate.dialect.rowsecurity.spi.RowLevelSecurityStrategies;
import org.hibernate.dialect.sequence.internal.SQLServer16SequenceSupport;
import org.hibernate.dialect.sequence.internal.SQLServerSequenceSupport;
import org.hibernate.dialect.sequence.spi.SequenceSupport;
import org.hibernate.dialect.sql.ast.internal.SQLServerSqlAstTranslator;
import org.hibernate.dialect.temporal.internal.SQLServerTemporalTableSupport;
import org.hibernate.dialect.temporal.spi.TemporalTableSupport;
import org.hibernate.dialect.temptable.spi.TemporaryTableStrategies;
import org.hibernate.dialect.temptable.spi.TemporaryTableStrategy;
import org.hibernate.dialect.type.spi.SQLServerJdbcTypes;
import org.hibernate.dialect.type.spi.SizeStrategy;
import org.hibernate.dialect.type.spi.StandardSizeStrategy;
import org.hibernate.dialect.unique.spi.UniqueDelegates;
import org.hibernate.dialect.unique.spi.UniqueDelegate;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.engine.jdbc.dialect.spi.BasicSQLExceptionConverter;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.jdbc.env.spi.IdentifierCaseStrategy;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
import org.hibernate.dialect.identifier.spi.IdentifierHelperBuildRequest;
import org.hibernate.engine.jdbc.env.spi.NameQualifierSupport;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.ConstraintViolationException.ConstraintKind;
import org.hibernate.exception.LockTimeoutException;
import org.hibernate.exception.spi.SQLExceptionConversionDelegate;
import org.hibernate.exception.spi.TemplatedViolatedConstraintNameExtractor;
import org.hibernate.exception.spi.ViolatedConstraintNameExtractor;
import org.hibernate.mapping.AggregateColumn;
import org.hibernate.mapping.Table;
import org.hibernate.procedure.spi.CallableStatementSupport;
import org.hibernate.procedure.spi.CallableStatementSupports;
import org.hibernate.dialect.sql.ast.spi.FetchClauseSupport;
import org.hibernate.query.common.TemporalUnit;
import org.hibernate.query.sqm.CastType;
import org.hibernate.query.sqm.SetOperator;
import org.hibernate.dialect.temporaltype.spi.IntervalType;
import org.hibernate.query.sqm.TrimSpec;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.sql.ast.spi.translation.SqlAstNodeRenderingMode;
import org.hibernate.sql.ast.spi.translation.SqlAstTranslator;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslatorFactory;
import org.hibernate.sql.spi.SqlAppender;
import org.hibernate.dialect.sql.ast.spi.StandardSqlAstTranslatorFactory;
import org.hibernate.sql.ast.spi.Statement;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslationRequest;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.sql.spi.mutation.MutationOperation;
import org.hibernate.dialect.sql.ast.spi.OptionalTableUpdateOperationRequest;
import org.hibernate.tool.schema.spi.StandardSequenceExporter;
import org.hibernate.tool.schema.spi.StandardTableExporter;
import org.hibernate.tool.schema.spi.Exporter;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractor;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractors;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.TimestampUtcAsJdbcTimestampJdbcType;
import org.hibernate.type.descriptor.jdbc.TinyIntAsSmallIntJdbcType;
import org.hibernate.type.descriptor.jdbc.UUIDJdbcType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;

import java.sql.SQLException;
import java.sql.Types;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import static org.hibernate.cfg.DialectSpecificSettings.SQL_SERVER_COMPATIBILITY_LEVEL;
import static org.hibernate.exception.spi.TemplatedViolatedConstraintNameExtractor.extractUsingTemplate;
import static org.hibernate.jdbc.spi.JdbcExceptionHelper.extractErrorCode;
import static org.hibernate.jdbc.spi.JdbcExceptionHelper.extractSqlState;
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
import static org.hibernate.dialect.literal.spi.StandardDateTimeLiteralRendering.appendAsDate;
import static org.hibernate.dialect.literal.spi.StandardDateTimeLiteralRendering.appendAsLocalTime;
import static org.hibernate.dialect.literal.spi.StandardDateTimeLiteralRendering.appendAsTime;
import static org.hibernate.dialect.literal.spi.StandardDateTimeLiteralRendering.appendAsTimestampWithMicros;
import static org.hibernate.dialect.literal.spi.StandardDateTimeLiteralRendering.appendAsTimestampWithMillis;

/**
 * A dialect for Microsoft SQL Server 2014 and above.
 * <p>
 * Please refer to the
 * <a href="https://learn.microsoft.com/en-us/sql/t-sql/language-reference">SQL Server documentation</a>.
 *
 * @author Gavin King
 */
public class SQLServerDialect extends AbstractTransactSQLDialect implements CurrentTemporalSupport, TemporalFormatSupport, TemporalOperationSupport {
	private IfExistsSupport ifExistsSupport;


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
	public TemporalValueSemantics getTemporalValueSemantics() {
		return TemporalValueSemantics.ROUND_MAX;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public CurrentTemporalSupport getCurrentTemporalSupport() {
		return this;
	}
	private final TypeSizingProfile typeSizingProfile = TypeSizingProfile.builder( super.getTypeSizingProfile() )
			.defaultTimestampPrecision( 7 )
			.maxTimestampPrecision( 7 )
			.defaultLobLength( Length.LONG32 )
			.maxVarcharLength( 8000 ).maxVarcharCapacity( 8000 )
			.maxNVarcharLength( 4000 ).maxNVarcharCapacity( 4000 )
			.maxVarbinaryLength( 8000 ).maxVarbinaryCapacity( 8000 )
			.build();

	@Override
	public TypeSizingProfile getTypeSizingProfile() {
		return typeSizingProfile;
	}
	private final static DatabaseVersion MINIMUM_VERSION = DatabaseVersion.make( 12, 0 );

	/**
	 * NOTE : 2100 is the documented limit supposedly - but in my testing, sending
	 * 2100 parameters fails saying it must be less than 2100.
	 */
	private static final int PARAM_LIST_SIZE_LIMIT = 2048;
	private static final CallableStatementSupport CALLABLE_STATEMENT_SUPPORT =
			CallableStatementSupports.builder()
					.namedParameterRenderer( (sqlAppender, parameterName) -> {
						sqlAppender.appendSql( '@' );
						sqlAppender.appendSql( parameterName );
						sqlAppender.appendSql( " = ?" );
					} )
					.build();

	// See microsoft.sql.Types.GEOMETRY
	private static final int GEOMETRY_TYPE_CODE = -157;
	// See microsoft.sql.Types.GEOGRAPHY
	private static final int GEOGRAPHY_TYPE_CODE = -158;

	private final StandardSequenceExporter exporter;
	private final UniqueDelegate uniqueDelegate = UniqueDelegates.nullableIndex( this );

	private final SizeStrategy sizeStrategy = new StandardSizeStrategy( this ) {
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
								length == null ? getTypeSizingProfile().defaultLobLength() : length );
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
	}

	@Override
	@SPI({ USE, IMPLEMENT })
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
		final var databaseMetaData = info.getDatabaseMetadata();
		if ( databaseMetaData != null ) {
			try ( var statement = databaseMetaData.getConnection().createStatement();
					var resultSet = statement.executeQuery(
							"SELECT compatibility_level FROM sys.databases where name = db_name();" ) ) {
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
	@SPI({ IMPLEMENT, SUPPLY })
	protected DatabaseVersion getMinimumSupportedVersion() {
		return MINIMUM_VERSION;
	}

	@Override
	@SPI({ USE, IMPLEMENT, SUPPLY })
	protected void contributeKeywords(KeywordRegistration registration) {
		super.contributeKeywords( registration );
		registration.registerKeyword( "top" );
		registration.registerKeyword( "key" );
	}

	@Override
	@SPI({ USE, IMPLEMENT })
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
	@SPI({ USE, IMPLEMENT })
	protected String castType(int sqlTypeCode) {
		return switch (sqlTypeCode) {
			case VARCHAR, LONG32VARCHAR, CLOB -> "varchar(max)";
			case NVARCHAR, LONG32NVARCHAR, NCLOB -> "nvarchar(max)";
			case VARBINARY, LONG32VARBINARY, BLOB -> "varbinary(max)";
			default -> super.castType( sqlTypeCode );
		};
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	protected void registerColumnTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		super.registerColumnTypes( typeContributions, serviceRegistry );
		final var ddlTypeRegistry = typeContributions.getTypeConfiguration().getDdlTypeRegistry();
		ddlTypeRegistry.addDescriptor( StandardDdlTypes.simple( GEOMETRY, "geometry", this ) );
		ddlTypeRegistry.addDescriptor( StandardDdlTypes.simple( GEOGRAPHY, "geography", this ) );
		ddlTypeRegistry.addDescriptor( StandardDdlTypes.simple( SQLXML, "xml", this ) );
		ddlTypeRegistry.addDescriptor( StandardDdlTypes.simple( UUID, "uniqueidentifier", this ) );
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public int getPreferredSqlTypeCodeForArray() {
		return XML_ARRAY;
	}

	@Override
	@SPI({ USE, IMPLEMENT })
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
	@SPI({ IMPLEMENT, SUPPLY })
	public TimeZoneSupport getTimeZoneSupport() {
		return TimeZoneSupport.NATIVE;
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public int getMaxIdentifierLength() {
		return 128;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public void contributeTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		super.contributeTypes( typeContributions, serviceRegistry );
		// Need to bind as java.sql.Timestamp because reading OffsetDateTime from a "datetime2" column fails
		typeContributions.contributeJdbcType( TimestampUtcAsJdbcTimestampJdbcType.INSTANCE );
		typeContributions.getTypeConfiguration().getJdbcTypeRegistry()
				.addDescriptor( Types.TINYINT, TinyIntAsSmallIntJdbcType.INSTANCE );
		typeContributions.contributeJdbcType( SQLServerJdbcTypes.castingXml() );
		typeContributions.contributeJdbcType( UUIDJdbcType.INSTANCE );
		typeContributions.contributeJdbcTypeConstructor( SQLServerJdbcTypes.castingXmlArrayConstructor() );
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public void initializeFunctionRegistry(FunctionContributions functionContributions) {
		super.initializeFunctionRegistry( functionContributions );

		final var basicTypeRegistry =
				functionContributions.getTypeConfiguration().getBasicTypeRegistry();
		final var dateType = basicTypeRegistry.resolve( StandardBasicTypes.DATE );
		final var timeType = basicTypeRegistry.resolve( StandardBasicTypes.TIME );
		final var timestampType = basicTypeRegistry.resolve( StandardBasicTypes.TIMESTAMP );

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

		registerJsonFunctions( functionFactory );
		registerXmlFunctions( functionFactory );

		functionFactory.unnest_sqlserver();

		if ( getVersion().isSameOrAfter( 14 ) ) {
			functionFactory.listagg_stringAggWithinGroup( "varchar(max)" );
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
			if ( getCteSupport().supports( CteSupport.RecursiveFeature.RECURSIVE ) ) {
				functionFactory.generateSeries_recursive( getMaximumSeriesSize(), false, false );
			}
		}

		functionFactory.hex( "convert(varchar(MAX), ?1, 2)" );
		functionFactory.sha( "hashbytes('SHA2_256', ?1)" );
		functionFactory.md5( "hashbytes('MD5', ?1)" );
		if ( getVersion().isSameOrAfter( 17 ) ) {
			functionFactory.regexpLike_predicateFunction();
		}
	}

	private static void registerXmlFunctions(CommonFunctionFactory functionFactory) {
		functionFactory.xmlelement_sqlserver();
		functionFactory.xmlcomment_sqlserver();
		functionFactory.xmlforest_sqlserver();
		functionFactory.xmlconcat_sqlserver();
		functionFactory.xmlpi_sqlserver();
		functionFactory.xmlquery_sqlserver();
		functionFactory.xmlexists_sqlserver();
		functionFactory.xmlagg_sqlserver();
		functionFactory.xmltable_sqlserver();
	}

	private void registerJsonFunctions(CommonFunctionFactory functionFactory) {
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
		if ( getVersion().isSameOrAfter( 14 ) ) {
			functionFactory.jsonArrayAgg_sqlserver( getVersion().isSameOrAfter( 16 ) );
			functionFactory.jsonObjectAgg_sqlserver( getVersion().isSameOrAfter( 16 ) );
		}
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
	@SPI({ USE, IMPLEMENT })
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
	@SPI({ IMPLEMENT, SUPPLY })
	public SqlAstTranslatorFactory getSqlAstTranslatorFactory() {
		return new StandardSqlAstTranslatorFactory() {
			@Override
			protected <S extends Statement, T extends JdbcOperation> SqlAstTranslator<T> createTranslator(
					SqlAstTranslationRequest<S, T> request) {
				return new SQLServerSqlAstTranslator<>( request );
			}
		};
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public AggregateSupport getAggregateSupport() {
		return SQLServerAggregateSupport.valueOf( this );
	}

	@Override
	public SizeStrategy getSizeStrategy() {
		return sizeStrategy;
	}

	@Override
	@SPI({ USE, IMPLEMENT })
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
		else if ( to == CastType.JSON ) {
			return "json_query(cast(?1 as nvarchar(max)))";
		}
		return super.castPattern( from, to );
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String currentTimestamp() {
		return "sysdatetime()";
	}

	@Override
	@SPI({ USE, IMPLEMENT, SUPPLY })
	public IdentifierHelper buildIdentifierHelper(IdentifierHelperBuildRequest request) {
		final var builder = request.builder();

		if ( !request.jdbcMetadata().isJdbcMetadataAccessible() ) {
			// TODO: if DatabaseMetaData != null, unquoted case strategy is set to IdentifierCaseStrategy.UPPER
			//       Check to see if this setting is correct.
			builder.setUnquotedCaseStrategy( IdentifierCaseStrategy.MIXED );
			builder.setQuotedCaseStrategy( IdentifierCaseStrategy.MIXED );
		}

		return super.buildIdentifierHelper( request );
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String currentTime() {
		return "convert(time,getdate())";
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String currentDate() {
		return "convert(date,getdate())";
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String currentTimestampWithTimeZone() {
		return "sysdatetimeoffset()";
	}

	@Override
	public LimitHandler getLimitHandler() {
		return SQLServer2012LimitHandler.INSTANCE;
	}

	@Override
	public ValuesListSupport getValuesListSupport() {
		return ValuesListSupport.STANDARD;
	}

	@Override
	public PredicateSupport getPredicateSupport() {
		return PredicateSupport.builder( super.getPredicateSupport() )
				.capability( PredicateSupport.Capability.DISTINCT_FROM, getVersion().isSameOrAfter( 16 ) )
				.build();
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public char closeQuote() {
		return ']';
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public synchronized IfExistsSupport getIfExistsSupport() {
		final var placement = getVersion().isSameOrAfter( 16 )
				? ExistenceCheckPlacement.BEFORE_NAME
				: ExistenceCheckPlacement.NONE;
		if ( ifExistsSupport == null ) {
			ifExistsSupport = new IfExistsSupport(
				ExistenceCheckPlacement.NONE,
				placement,
				placement,
				placement
		);
		}
		return ifExistsSupport;
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public char openQuote() {
		return '[';
	}

	@Override
	public LockingSupport getLockingSupport() {
		return TransactSQLLockingSupport.SQL_SERVER;
	}

	/**
	 * The current_timestamp is more accurate, but only known to be supported in SQL Server 7.0 and later and
	 * Sybase not known to support it at all
	 * <p>
	 * {@inheritDoc}
	 */
	@Override
	@SPI({ USE, IMPLEMENT })
	public CurrentTimestampSelection getCurrentTimestampSelection() {
		return CurrentTimestampSelection.prepared( "select current_timestamp" );
	}

	// Overridden informational metadata ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	@SPI({ USE, IMPLEMENT })
	public boolean addPartitionKeyToPrimaryKey() {
		return true;
	}

	@Override
	public ParameterLimits getParameterLimits() {
		return ParameterLimits.of( PARAM_LIST_SIZE_LIMIT );
	}

	@Override
	public IdentityColumnSupport getIdentityColumnSupport() {
		return SQLServerIdentityColumnSupport.INSTANCE;
	}

	@Override
	public CteSupport getCteSupport() {
		return CteSupport.builder()
				.placement( CteSupport.Placement.TOP_LEVEL )
				.recursiveFeatures( CteSupport.RecursiveFeature.RECURSIVE )
				.mutationFeatures( CteSupport.MutationFeature.NON_QUERY )
				.requiresRecursiveKeyword( false )
				.build();
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public SequenceSupport getSequenceSupport() {
		return getVersion().isSameOrAfter( 16 )
				? SQLServer16SequenceSupport.getInstance()
				: SQLServerSequenceSupport.getInstance();
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public RowLevelSecurity getRowLevelSecurity() {
		return getVersion().isSameOrAfter( 13 )
				? SQLServerRowLevelSecurity.INSTANCE
				: RowLevelSecurityStrategies.none();
	}

	private static final SequenceInformationExtractor SEQUENCE_INFORMATION_EXTRACTOR =
			// The upper-case name works on both case-sensitive and case-insensitive collations.
			SequenceInformationExtractors.builder( "select * from INFORMATION_SCHEMA.SEQUENCES" ).build();

	@Override
	public SequenceInformationExtractor getSequenceInformationExtractor() {
		return SEQUENCE_INFORMATION_EXTRACTOR;
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String getQueryHintString(String sql, String hints) {
		final var buffer =
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
	public NullOrderingSupport getNullOrderingSupport() {
		return NullOrderingSupport.builder( super.getNullOrderingSupport() )
				.capability( NullOrderingSupport.Capability.NULLS_FIRST_LAST, false )
				.build();
	}

	@Override
	public SubquerySupport getSubquerySupport() {
		return SubquerySupport.builder( super.getSubquerySupport() )
				.features( SubquerySupport.Feature.OFFSET, SubquerySupport.Feature.LATERAL )
				.build();
	}

	@Override
	public WindowFunctionSupport getWindowFunctionSupport() {
		return WindowFunctionSupport.builder()
				.features(
						WindowFunctionSupport.Feature.WINDOW_FUNCTIONS,
						WindowFunctionSupport.Feature.PARTITION_BY,
						WindowFunctionSupport.Feature.ROWS_FRAME,
						WindowFunctionSupport.Feature.RANGE_FRAME
				)
				.build();
	}

	@Override
	@SPI({ USE, IMPLEMENT, SUPPLY })
	public FetchClauseSupport getFetchClauseSupport() {
		return FetchClauseSupport.ALL;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
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
	@SPI({ IMPLEMENT, SUPPLY })
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

	/**
	 * Even though SQL Server only supports 1/10th microsecond precision,
	 * we use nanosecond as the "native" precision for datetime arithmetic
	 * since it simplifies calculations.
	 */
	@Override
	@SPI({ USE, IMPLEMENT })
	public long fractionalSecondPrecisionInNanos() {
//		return 100; // 1/10th microsecond
		return 1;
	}

	@Override
	@SPI({ USE, IMPLEMENT })
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
	@SPI({ USE, IMPLEMENT })
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
	@SPI({ USE, IMPLEMENT })
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
	@SPI({ USE, IMPLEMENT })
	public String translateDurationField(TemporalUnit unit) {
		//use nanosecond as the "native" precision
		return unit == TemporalUnit.NATIVE
				? "nanosecond"
				: TemporalOperationSupports.standard().translateDurationField( unit );
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String translateExtractField(TemporalUnit unit) {
		return switch (unit) {
			//the ISO week number (behavior of "week" depends on a system property)
			case WEEK -> "isowk";
			case OFFSET -> "tz";
			default -> TemporalOperationSupports.standard().translateExtractField( unit );
		};
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public void appendFormat(SqlAppender appender, String format) {
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
	@SPI({ USE, IMPLEMENT })
	public void appendUUIDLiteral(SqlAppender appender, java.util.UUID literal) {
		appender.appendSql( "cast('" );
		appender.appendSql( literal.toString() );
		appender.appendSql( "' as uniqueidentifier)" );
	}

	@Override
	@SPI({ USE, IMPLEMENT })
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
				appendAsTime( appender, temporalAccessor, getTemporalValueSemantics().supportsLiteralOffset(), jdbcTimeZone );
				appender.appendSql( "' as time)" );
				break;
			case TIMESTAMP:
				appender.appendSql( "cast('" );
				//needed because the {ts ... } JDBC escape chokes on microseconds
				if ( getTemporalValueSemantics().supportsLiteralOffset() && temporalAccessor.isSupported( ChronoField.OFFSET_SECONDS ) ) {
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
	@SPI({ USE, IMPLEMENT })
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
				appendAsLocalTime( appender, date );
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
	@SPI({ USE, IMPLEMENT })
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
				appendAsLocalTime( appender, calendar );
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
	@SPI({ USE, IMPLEMENT })
	public void appendDefinition(SqlAppender appender, ColumnDefinitionRequest request) {
		if ( request.generatedExpression() == null ) {
			appender.appendSql( ' ' );
			appender.appendSql( request.sqlType() );
		}
		if ( request.renderedCollation() != null ) {
			appender.appendSql( " collate " );
			appender.appendSql( request.renderedCollation() );
		}
		if ( request.defaultExpression() != null ) {
			appender.appendSql( " default " );
			appender.appendSql( request.defaultExpression() );
		}
		if ( request.generatedExpression() != null ) {
			if ( request.generatedExpression().startsWith( "row " ) ) {
				appender.appendSql( " datetime2 generated always as " );
				appender.appendSql( request.generatedExpression() );
			}
			else {
				appender.appendSql( " as (" );
				appender.appendSql( request.generatedExpression() );
				appender.appendSql( ") persisted" );
			}
		}
		if ( !request.nullable() ) {
			appender.appendSql( " not null" );
		}
	}

	@Override
	public TemporaryTableStrategy getLocalTemporaryTableStrategy() {
		return TemporaryTableStrategies.sqlServerLocal();
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public NamespaceSupport getNamespaceSupport() {
		return NamespaceSupports.standard( false, getVersion().isSameOrAfter( 13 ) );
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String createCommand(IndexDdlRequest request) {
		// we only create unique indexes, as opposed to unique constraints,
		// when the column is nullable, so safe to infer unique => nullable
		return request.unique() ? "create unique nonclustered index" : "create index";
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String createTail(IndexDdlRequest request) {
		if ( request.unique() ) {
			final var tail = new StringBuilder();
			for ( var column : request.columns() ) {
				if ( column.nullable() ) {
					tail.append( tail.isEmpty() ? " where " : " and " )
							.append( column.expression() )
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
	@SPI({ USE, IMPLEMENT })
	public String alterColumnType(AlterColumnTypeRequest request) {
		return "alter column " + request.columnName() + " " + request.columnType();
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public NameQualifierSupport getNameQualifierSupport() {
		return NameQualifierSupport.BOTH;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
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
	@SPI({ USE, IMPLEMENT, SUPPLY })
	public MutationOperation createOptionalTableUpdateOperation(
			OptionalTableUpdateOperationRequest request) {
		final var optionalTableUpdate = request.update();
		final var factory = request.sessionFactory();
		return new SQLServerSqlAstTranslator<>( new SqlAstTranslationRequest.ModelMutation<>( factory, optionalTableUpdate ) )
				.createMergeOperation( optionalTableUpdate );
	}

	@Override
	@SPI({ USE, IMPLEMENT, SUPPLY })
	public DmlTargetColumnQualifierSupport getDmlTargetColumnQualifierSupport() {
		return DmlTargetColumnQualifierSupport.TABLE_ALIAS;
	}

	@Override
	public MutationSyntaxSupport getMutationSyntaxSupport() {
		return MutationSyntaxSupport.builder()
				.capability( MutationKind.UPDATE, MutationSyntaxCapability.FROM_CLAUSE )
				.capability( MutationKind.DELETE, MutationSyntaxCapability.JOIN )
				.build();
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public CallableStatementSupport getCallableStatementSupport() {
		return CALLABLE_STATEMENT_SUPPORT;
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String render(org.hibernate.dialect.constraint.spi.CheckConstraintRenderRequest request) {
		// The only useful option is 'NOT FOR REPLICATION'
		// and it comes before the constraint expression
		final String constraintName = request.name();
		final String checkWithName =
				isBlank( constraintName )
						? "check"
						: "constraint " + constraintName + " check";
		return (isNotEmpty( request.options() ) ? checkWithName + " " + request.options() : checkWithName)
				+ " (" + request.expression() + ")";
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public ObjectNullBindingStrategy getObjectNullBindingStrategy() {
		return ObjectNullBindingStrategy.SET_OBJECT;
	}

	@Override
	public SetOperationSupport getSetOperationSupport() {
		// SQL Server is quite strict i.e. it requires `select ... union all select * from (select ...)`
		// rather than `select ... union all (select ...)` because parenthesis followed by select
		// is always treated as a subquery, which is not supported in a set operation
		return SetOperationSupport.builder()
				.operator( SetOperator.INTERSECT_ALL, false )
				.operator( SetOperator.EXCEPT_ALL, false )
				.capability( SetOperationSupport.Capability.SIMPLE_QUERY_GROUPING, false )
				.build();
	}

	@Override
	public RowValueSupport getRowValueSupport() {
		return RowValueSupport.NONE;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public TemporalTableSupport getTemporalTableSupport() {
		return new SQLServerTemporalTableSupport( this );
	}
}
