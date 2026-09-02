/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import org.hibernate.dialect.identifier.spi.KeywordRegistration;

import org.hibernate.dialect.temporaltype.spi.TemporalValueSemantics;

import org.hibernate.dialect.temporaltype.spi.CurrentTimestampSelection;

import org.hibernate.dialect.temporaltype.spi.TemporalOperationSupport;

import org.hibernate.dialect.temporaltype.spi.TemporalFormatSupport;

import org.hibernate.dialect.temporaltype.spi.CurrentTemporalSupport;

import org.hibernate.SPI;
import static org.hibernate.SPI.Role.USE;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.SUPPLY;

import org.hibernate.dialect.type.spi.DdlTypeBuilder;

import org.hibernate.dialect.type.spi.StandardDdlTypes;

import org.hibernate.dialect.type.spi.TypeSizingProfile;

import org.hibernate.dialect.mutation.spi.MultiTableMutationSupport;
import org.hibernate.dialect.sql.ast.spi.CteSupport;
import org.hibernate.dialect.sql.ast.spi.NullOrderingSupport;
import org.hibernate.dialect.sql.ast.spi.MutationKind;
import org.hibernate.dialect.sql.ast.spi.MutationSyntaxCapability;
import org.hibernate.dialect.sql.ast.spi.MutationSyntaxSupport;
import org.hibernate.dialect.sql.ast.spi.RowValueSupport;
import org.hibernate.dialect.sql.ast.spi.SingleRowTableSupport;
import org.hibernate.dialect.sql.ast.spi.ValuesListSupport;
import org.hibernate.dialect.sql.ast.spi.SubquerySupport;

import jakarta.persistence.TemporalType;
import org.hibernate.ScrollMode;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.lob.spi.LobSupport;
import org.hibernate.dialect.lob.spi.LobSupports;
import org.hibernate.dialect.sql.ast.spi.DmlTargetColumnQualifierSupport;
import org.hibernate.dialect.jdbc.spi.HANAServerConfiguration;
import org.hibernate.dialect.sql.ast.spi.NullOrdering;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.aggregate.spi.AggregateSupport;
import org.hibernate.dialect.HANADialect;
import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.dialect.function.spi.WindowFunctionSupport;
import org.hibernate.dialect.function.IntegralTimestampaddFunction;
import org.hibernate.dialect.identifier.spi.DelegatingIdentifierHelper;
import org.hibernate.community.dialect.identity.internal.HANAIdentityColumnSupport;
import org.hibernate.dialect.identity.spi.IdentityColumnSupport;
import org.hibernate.dialect.lock.spi.LockingSupport;
import org.hibernate.dialect.lock.spi.StandardLockingSupports;
import org.hibernate.dialect.pagination.spi.LimitHandler;
import org.hibernate.dialect.pagination.spi.LimitOffsetLimitHandler;
import org.hibernate.community.dialect.sequence.CommunitySequenceSupports;
import org.hibernate.dialect.sequence.spi.SequenceSupport;
import org.hibernate.dialect.schema.spi.ColumnDefinitionRequest;
import org.hibernate.dialect.schema.spi.ConstraintDropMode;
import org.hibernate.dialect.schema.spi.SchemaDropSupport;
import org.hibernate.dialect.schema.spi.TableCreationKind;
import org.hibernate.dialect.temptable.spi.StandardGlobalTemporaryTableStrategy;
import org.hibernate.dialect.temptable.spi.TemporaryTableStrategy;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.config.spi.StandardConverters;
import org.hibernate.engine.jdbc.BinaryStream;
import org.hibernate.engine.jdbc.BlobImplementer;
import org.hibernate.engine.jdbc.CharacterStream;
import org.hibernate.engine.jdbc.ClobImplementer;
import org.hibernate.engine.jdbc.NClobImplementer;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.jdbc.cursor.spi.RefCursorSupportFactory;
import org.hibernate.engine.jdbc.cursor.spi.RefCursorSupports;
import org.hibernate.engine.jdbc.env.spi.IdentifierCaseStrategy;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
import org.hibernate.dialect.identifier.spi.IdentifierHelperBuildRequest;
import org.hibernate.engine.jdbc.env.spi.NameQualifierSupport;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.LockAcquisitionException;
import org.hibernate.exception.LockTimeoutException;
import org.hibernate.exception.SQLGrammarException;
import org.hibernate.exception.spi.SQLExceptionConversionDelegate;
import org.hibernate.jdbc.spi.JdbcExceptionHelper;
import org.hibernate.mapping.Table;
import org.hibernate.procedure.spi.CallableStatementSupport;
import org.hibernate.procedure.spi.CallableStatementSupports;
import org.hibernate.query.common.TemporalUnit;
import org.hibernate.query.sqm.CastType;
import org.hibernate.dialect.temporaltype.spi.IntervalType;
import org.hibernate.query.sqm.produce.function.FunctionParameterType;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.sql.ast.spi.translation.SqlAstNodeRenderingMode;
import org.hibernate.sql.ast.spi.translation.SqlAstTranslator;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslatorFactory;
import org.hibernate.sql.spi.SqlAppender;
import org.hibernate.dialect.sql.ast.spi.StandardSqlAstTranslatorFactory;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslationRequest;
import org.hibernate.sql.ast.spi.Statement;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractor;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractors;
import org.hibernate.tool.schema.spi.StandardTableExporter;
import org.hibernate.tool.schema.spi.Exporter;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.DoubleJavaType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.BasicBinder;
import org.hibernate.type.descriptor.jdbc.BasicExtractor;
import org.hibernate.type.descriptor.jdbc.BlobJdbcType;
import org.hibernate.type.descriptor.jdbc.ClobJdbcType;
import org.hibernate.type.descriptor.jdbc.DecimalJdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.NCharJdbcType;
import org.hibernate.type.descriptor.jdbc.NClobJdbcType;
import org.hibernate.type.descriptor.jdbc.NVarcharJdbcType;
import org.hibernate.type.descriptor.jdbc.NumericJdbcType;
import org.hibernate.type.descriptor.jdbc.TinyIntAsSmallIntJdbcType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;
import org.hibernate.type.descriptor.sql.spi.DdlTypeRegistry;
import org.hibernate.type.spi.TypeConfiguration;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.FilterReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Types;
import java.time.temporal.TemporalAccessor;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.hibernate.dialect.jdbc.spi.HANAServerConfiguration.MAX_LOB_PREFETCH_SIZE_DEFAULT_VALUE;
import static org.hibernate.query.sqm.produce.function.FunctionParameterType.ANY;
import static org.hibernate.type.SqlTypes.BINARY;
import static org.hibernate.type.SqlTypes.BOOLEAN;
import static org.hibernate.type.SqlTypes.CHAR;
import static org.hibernate.type.SqlTypes.CLOB;
import static org.hibernate.type.SqlTypes.DECIMAL;
import static org.hibernate.type.SqlTypes.DOUBLE;
import static org.hibernate.type.SqlTypes.GEOMETRY;
import static org.hibernate.type.SqlTypes.LONG32NVARCHAR;
import static org.hibernate.type.SqlTypes.LONG32VARCHAR;
import static org.hibernate.type.SqlTypes.NCHAR;
import static org.hibernate.type.SqlTypes.NCLOB;
import static org.hibernate.type.SqlTypes.NUMERIC;
import static org.hibernate.type.SqlTypes.NVARCHAR;
import static org.hibernate.type.SqlTypes.POINT;
import static org.hibernate.type.SqlTypes.TIME;
import static org.hibernate.type.SqlTypes.TIMESTAMP;
import static org.hibernate.type.SqlTypes.TIMESTAMP_WITH_TIMEZONE;
import static org.hibernate.type.SqlTypes.TIME_WITH_TIMEZONE;
import static org.hibernate.type.SqlTypes.TINYINT;
import static org.hibernate.type.SqlTypes.VARCHAR;
import static org.hibernate.dialect.literal.spi.StandardDateTimeLiteralRendering.appendAsDate;
import static org.hibernate.dialect.literal.spi.StandardDateTimeLiteralRendering.appendAsLocalTime;
import static org.hibernate.dialect.literal.spi.StandardDateTimeLiteralRendering.appendAsTime;
import static org.hibernate.dialect.literal.spi.StandardDateTimeLiteralRendering.appendAsTimestampWithMicros;
import static org.hibernate.dialect.lob.spi.LobDataExtraction.extractBytes;
import static org.hibernate.dialect.lob.spi.LobDataExtraction.extractString;

/**
 * An SQL dialect for legacy versions of the SAP HANA Platform up tu and including 2.0 SPS 04.
 * <p>
 * For more information on SAP HANA Platform, refer to the
 * <a href="https://help.sap.com/docs/SAP_HANA_PLATFORM/4fe29514fd584807ac9f2a04f6754767/b4b0eec1968f41a099c828a4a6c8ca0f.html?locale=en-US">SAP HANA Platform SQL Reference Guide</a>.
 * <p>
 * Column tables are created by this dialect by default when using the auto-ddl feature.
 */
public class HANALegacyDialect extends Dialect implements CurrentTemporalSupport, TemporalFormatSupport, TemporalOperationSupport {
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
	private static final RefCursorSupportFactory REF_CURSOR_SUPPORT_FACTORY = RefCursorSupports.hana();
	private final TypeSizingProfile typeSizingProfile = TypeSizingProfile.builder( super.getTypeSizingProfile() )
			.defaultTimestampPrecision( 7 ).defaultDecimalPrecision( 34 )
			.maxVarcharLength( 5000 ).maxVarcharCapacity( 5000 )
			.maxNVarcharLength( 5000 ).maxNVarcharCapacity( 5000 )
			.maxVarbinaryLength( 5000 ).maxVarbinaryCapacity( 5000 )
			.build();

	@Override public TypeSizingProfile getTypeSizingProfile() { return typeSizingProfile; }

	static final DatabaseVersion DEFAULT_VERSION = DatabaseVersion.make( 1, 0, 120 );

	private final LockingSupport lockingSupport;

	public HANALegacyDialect(DialectResolutionInfo info) {
		this( HANAServerConfiguration.fromDialectResolutionInfo( info ), true );
	}

	public HANALegacyDialect() {
		this( DEFAULT_VERSION );
	}

	public HANALegacyDialect(DatabaseVersion version) {
		this( new HANAServerConfiguration( version ), true );
	}

	public HANALegacyDialect(DatabaseVersion version, boolean defaultTableTypeColumn) {
		this( new HANAServerConfiguration( version ), defaultTableTypeColumn );
	}

	public HANALegacyDialect(HANAServerConfiguration configuration, boolean defaultTableTypeColumn) {
		super( configuration.getFullVersion() );
		this.defaultTableTypeColumn = defaultTableTypeColumn;
		this.maxLobPrefetchSize = configuration.getMaxLobPrefetchSize();
		this.useUnicodeStringTypes = useUnicodeStringTypesDefault();

		this.lockingSupport = StandardLockingSupports.hana( configuration.getFullVersion() );
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public DatabaseVersion determineDatabaseVersion(DialectResolutionInfo info) {
		return HANALegacyServerConfiguration.determineDatabaseVersion( info );
	}

	// Use column or row tables by default
	public static final String USE_DEFAULT_TABLE_TYPE_COLUMN = "hibernate.dialect.hana.use_default_table_type_column";
	// Use TINYINT instead of the native BOOLEAN type
	private static final String USE_LEGACY_BOOLEAN_TYPE_PARAMETER_NAME = "hibernate.dialect.hana.use_legacy_boolean_type";
	// Use unicode (NVARCHAR, NCLOB, etc.) instead of non-unicode (VARCHAR, CLOB) string types
	private static final String USE_UNICODE_STRING_TYPES_PARAMETER_NAME = "hibernate.dialect.hana.use_unicode_string_types";
	// Read and write double-typed fields as BigDecimal instead of Double to get around precision issues of the HANA
	// JDBC driver (https://service.sap.com/sap/support/notes/2590160)
	private static final String TREAT_DOUBLE_TYPED_FIELDS_AS_DECIMAL_PARAMETER_NAME = "hibernate.dialect.hana.treat_double_typed_fields_as_decimal";

	private static final Boolean USE_LEGACY_BOOLEAN_TYPE_DEFAULT_VALUE = Boolean.FALSE;
	private static final Boolean TREAT_DOUBLE_TYPED_FIELDS_AS_DECIMAL_DEFAULT_VALUE = Boolean.FALSE;
	private static final String SQL_IGNORE_LOCKED = " ignore locked";

	private final int maxLobPrefetchSize;

	private boolean defaultTableTypeColumn;
	private boolean useLegacyBooleanType = USE_LEGACY_BOOLEAN_TYPE_DEFAULT_VALUE;
	private boolean useUnicodeStringTypes;
	private boolean treatDoubleTypedFieldsAsDecimal;

	/*
	 * Tables named "TYPE" need to be quoted
	 */
	private final StandardTableExporter hanaTableExporter = new StandardTableExporter( this ) {

		@Override
		public String[] getSqlCreateStrings(Table table, Metadata metadata, SqlStringGenerationContext context) {
			String[] sqlCreateStrings = super.getSqlCreateStrings( table, metadata, context );
			return quoteTypeIfNecessary(
					table,
					sqlCreateStrings,
					getTableCreationSupport().createTableCommand( TableCreationKind.STANDARD )
			);
		}

		@Override
		public String[] getSqlDropStrings(Table table, Metadata metadata, SqlStringGenerationContext context) {
			String[] sqlDropStrings = super.getSqlDropStrings( table, metadata, context );
			return quoteTypeIfNecessary( table, sqlDropStrings, "drop table" );
		}

		private String[] quoteTypeIfNecessary(Table table, String[] strings, String prefix) {
			if ( table.getNameIdentifier() == null || table.getNameIdentifier().isQuoted()
					|| !"type".equalsIgnoreCase( table.getNameIdentifier().getText() ) ) {
				return strings;
			}

			Pattern createTableTypePattern = Pattern.compile( "(" + prefix + "\\s+)(" + table.getNameIdentifier().getText() + ")(.+)" );
			Pattern commentOnTableTypePattern = Pattern.compile( "(comment\\s+on\\s+table\\s+)(" + table.getNameIdentifier().getText() + ")(.+)" );
			for ( int i = 0; i < strings.length; i++ ) {
				Matcher createTableTypeMatcher = createTableTypePattern.matcher( strings[i] );
				Matcher commentOnTableTypeMatcher = commentOnTableTypePattern.matcher( strings[i] );
				if ( createTableTypeMatcher.matches() ) {
					strings[i] = createTableTypeMatcher.group( 1 ) + "\"TYPE\"" + createTableTypeMatcher.group( 3 );
				}
				if ( commentOnTableTypeMatcher.matches() ) {
					strings[i] = commentOnTableTypeMatcher.group( 1 ) + "\"TYPE\"" + commentOnTableTypeMatcher.group( 3 );
				}
			}

			return strings;
		}
	};


	protected boolean isDefaultTableTypeColumn() {
		return defaultTableTypeColumn;
	}

	protected boolean isCloud() {
		return getVersion().isSameOrAfter( 4 );
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	protected String columnType(int sqlTypeCode) {
		return switch ( sqlTypeCode ) {
			case BOOLEAN -> useLegacyBooleanType ? "tinyint" : super.columnType( sqlTypeCode );
			//there is no 'numeric' type in HANA
			case NUMERIC -> columnType( DECIMAL );
			//'double precision' syntax not supported
			case DOUBLE -> "double";
			//no explicit precision
			case TIME, TIME_WITH_TIMEZONE -> "time";
			case TIMESTAMP, TIMESTAMP_WITH_TIMEZONE -> "timestamp";
			//there is no 'char' or 'nchar' type in HANA
			case CHAR, VARCHAR -> isUseUnicodeStringTypes() ? columnType( NVARCHAR ) : super.columnType( VARCHAR );
			case NCHAR -> columnType( NVARCHAR );
			case LONG32VARCHAR -> isUseUnicodeStringTypes() ? columnType( LONG32NVARCHAR ) : super.columnType( LONG32VARCHAR );
			case CLOB -> isUseUnicodeStringTypes() ? columnType( NCLOB ) : super.columnType( CLOB );
			// map tinyint to smallint since tinyint is unsigned on HANA
			case TINYINT -> "smallint";
			default -> super.columnType( sqlTypeCode );
		};
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	protected void registerColumnTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		super.registerColumnTypes( typeContributions, serviceRegistry );
		final DdlTypeRegistry ddlTypeRegistry = typeContributions.getTypeConfiguration().getDdlTypeRegistry();

		// varbinary max length 5000
		ddlTypeRegistry.addDescriptor(
				StandardDdlTypes.builder( BINARY, "blob", this )
						.lobKind( DdlTypeBuilder.LobKind.BIGGEST )
						.withTypeCapacity( getTypeSizingProfile().maxVarbinaryLength(), "varbinary($l)" )
						.build()
		);

		ddlTypeRegistry.addDescriptor( StandardDdlTypes.simple( GEOMETRY, "st_geometry", this ) );
		ddlTypeRegistry.addDescriptor( StandardDdlTypes.simple( POINT, "st_point", this ) );
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	protected void contributeDefaultProperties(java.util.Properties properties) {
		super.contributeDefaultProperties( properties );
		// createBlob() and createClob() are not supported by the HANA JDBC driver
		properties.setProperty( org.hibernate.cfg.AvailableSettings.NON_CONTEXTUAL_LOB_CREATION, "true" );
		// getGeneratedKeys() is not supported by the HANA JDBC driver
		properties.setProperty( org.hibernate.cfg.AvailableSettings.USE_GET_GENERATED_KEYS, "false" );
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String castPattern(CastType from, CastType to) {
		if ( to == CastType.BOOLEAN ) {
			switch ( from ) {
				case INTEGER_BOOLEAN:
				case INTEGER:
				case LONG:
					return "case ?1 when 1 then true when 0 then false else null end";
				case YN_BOOLEAN:
					return "case ?1 when 'Y' then true when 'N' then false else null end";
				case TF_BOOLEAN:
					return "case ?1 when 'T' then true when 'F' then false else null end";
			}
		}
		return super.castPattern( from, to );
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public void initializeFunctionRegistry(FunctionContributions functionContributions) {
		super.initializeFunctionRegistry(functionContributions);
		final TypeConfiguration typeConfiguration = functionContributions.getTypeConfiguration();

		functionContributions.getFunctionRegistry().registerBinaryTernaryPattern(
				"locate",
				typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.INTEGER ),
				"locate(?2,?1)",
				"locate(?2,?1,?3)",
				FunctionParameterType.STRING, FunctionParameterType.STRING, FunctionParameterType.INTEGER,
				typeConfiguration
		).setArgumentListSignature("(pattern, string[, start])");

		CommonFunctionFactory functionFactory = new CommonFunctionFactory(functionContributions);

		functionFactory.ceiling_ceil();
		functionFactory.concat_pipeOperator();
		functionFactory.trim2();
		functionFactory.cot();
		functionFactory.cosh();
		functionFactory.sinh();
		functionFactory.tanh();
		functionFactory.trunc_roundMode();
		functionFactory.log10_log();
		functionFactory.log();
		functionFactory.bitand();
		functionFactory.bitor();
		functionFactory.bitxor();
		functionFactory.bitnot();
		functionFactory.hourMinuteSecond();
		functionFactory.yearMonthDay();
		functionFactory.dayofweekmonthyear();
		functionFactory.weekQuarter();
		functionFactory.daynameMonthname();
		functionFactory.lastDay();
		functionFactory.characterLength_length( SqlAstNodeRenderingMode.DEFAULT );
		functionFactory.ascii();
		functionFactory.chr_char();
		functionFactory.addYearsMonthsDaysHoursMinutesSeconds();
		functionFactory.daysBetween();
		functionFactory.secondsBetween();
		functionFactory.format_toVarchar();
		functionFactory.currentUtcdatetimetimestamp();
		functionFactory.everyAny_minMaxCase();
		functionFactory.octetLength_pattern( "length(to_binary(?1))" );
		functionFactory.bitLength_pattern( "length(to_binary(?1))*8" );
		functionFactory.repeat_rpad();

		functionFactory.median();
		functionFactory.windowFunctions();
		functionFactory.listagg_stringAgg( "varchar" );
		functionFactory.inverseDistributionOrderedSetAggregates();
		functionFactory.hypotheticalOrderedSetAggregates_windowEmulation();

		functionFactory.radians_acos();
		functionFactory.degrees_acos();

		functionContributions.getFunctionRegistry().register( "timestampadd",
				new IntegralTimestampaddFunction( this, typeConfiguration ) );

		// full-text search functions
		functionContributions.getFunctionRegistry().registerNamed(
				"score",
				typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.DOUBLE )
		);
		functionContributions.getFunctionRegistry().registerNamed( "snippets" );
		functionContributions.getFunctionRegistry().registerNamed( "highlighted" );
		functionContributions.getFunctionRegistry().registerBinaryTernaryPattern(
				"contains",
				typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.BOOLEAN ),
				"contains(?1,?2)",
				"contains(?1,?2,?3)",
				ANY, ANY, ANY,
				typeConfiguration
		);

		if ( getVersion().isSameOrAfter( 2, 0 ) ) {
			// Introduced in 2.0 SPS 00
			functionFactory.jsonValue_no_passing();
			functionFactory.jsonQuery_no_passing();
			functionFactory.jsonExists_hana();

			functionFactory.unnest_hana();
			functionFactory.jsonTable_hana();

			functionFactory.generateSeries_hana( getMaximumSeriesSize() );

			if ( getVersion().isSameOrAfter(2, 0, 20 ) ) {
				if ( getVersion().isSameOrAfter( 2, 0, 40 ) ) {
					// Introduced in 2.0 SPS 04
					functionFactory.jsonObject_hana();
					functionFactory.jsonArray_hana();
					functionFactory.jsonArrayAgg_hana();
					functionFactory.jsonObjectAgg_hana();
				}

				functionFactory.xmltable_hana();
			}

//			functionFactory.xmlextract();
		}

		functionFactory.regexpLike_like_regexp();
	}

	/**
	 * HANA doesn't support the {@code generate_series} function or {@code lateral} recursive CTEs,
	 * so it has to be emulated with the {@code xmltable} and {@code lpad} functions.
	 */
	protected int getMaximumSeriesSize() {
		return 10000;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public SqlAstTranslatorFactory getSqlAstTranslatorFactory() {
		return new StandardSqlAstTranslatorFactory() {
			@Override
			protected <S extends Statement, T extends JdbcOperation> SqlAstTranslator<T> createTranslator(
					SqlAstTranslationRequest<S, T> request) {
				return new HANALegacySqlAstTranslator<>( request );
			}
		};
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public AggregateSupport getAggregateSupport() {
		return new HANADialect( getVersion() ).getAggregateSupport();
	}

	/**
	 * HANA has no extract() function, but we can emulate
	 * it using the appropriate named functions instead of
	 * extract().
	 *
	 * The supported fields are
	 * {@link TemporalUnit#YEAR},
	 * {@link TemporalUnit#MONTH}
	 * {@link TemporalUnit#DAY},
	 * {@link TemporalUnit#HOUR},
	 * {@link TemporalUnit#MINUTE},
	 * {@link TemporalUnit#SECOND}
	 * {@link TemporalUnit#WEEK},
	 * {@link TemporalUnit#DAY_OF_WEEK},
	 * {@link TemporalUnit#DAY_OF_MONTH},
	 * {@link TemporalUnit#DAY_OF_YEAR}.
	 */
	@Override
	@SPI({ USE, IMPLEMENT })
	public String extractPattern(TemporalUnit unit) {
		return switch (unit) {
			case DAY_OF_WEEK -> "(mod(weekday(?2)+1,7)+1)";
			case DAY, DAY_OF_MONTH -> "dayofmonth(?2)";
			case DAY_OF_YEAR -> "dayofyear(?2)";
			case QUARTER -> "((month(?2)+2)/3)";
			case EPOCH -> "seconds_between('1970-01-01', ?2)";
			//I think week() returns the ISO week number
			default -> "?1(?2)";
		};
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public SQLExceptionConversionDelegate buildSQLExceptionConversionDelegate() {
		return (sqlException, message, sql) -> {
			final int errorCode = JdbcExceptionHelper.extractErrorCode( sqlException );

			if ( errorCode == 131 ) {
				// 131 - Transaction rolled back by lock wait timeout
				return new LockTimeoutException( message, sqlException, sql );
			}

			if ( errorCode == 146 ) {
				// 146 - Resource busy and acquire with NOWAIT specified
				return new LockTimeoutException( message, sqlException, sql );
			}

			if ( errorCode == 132 ) {
				// 132 - Transaction rolled back due to unavailable resource
				return new LockAcquisitionException( message, sqlException, sql );
			}

			if ( errorCode == 133 ) {
				// 133 - Transaction rolled back by detected deadlock
				return new LockAcquisitionException( message, sqlException, sql );
			}

			// 259 - Invalid table name
			// 260 - Invalid column name
			// 261 - Invalid index name
			// 262 - Invalid query name
			// 263 - Invalid alias name
			if ( errorCode == 257 || ( errorCode >= 259 && errorCode <= 263 ) ) {
				return new SQLGrammarException( message, sqlException, sql );
			}

			// 257 - Cannot insert NULL or update to NULL
			// 301 - Unique constraint violated
			// 461 - foreign key constraint violation
			// 462 - failed on update or delete by foreign key constraint violation
			if ( errorCode == 287 || errorCode == 301 || errorCode == 461 || errorCode == 462 ) {
				final String constraintName = getViolatedConstraintNameExtractor()
						.extractConstraintName( sqlException );

				return new ConstraintViolationException(
						message,
						sqlException,
						sql,
						errorCode == 301
								? ConstraintViolationException.ConstraintKind.UNIQUE
								: ConstraintViolationException.ConstraintKind.OTHER,
						constraintName
				);
			}

			return null;
		};
	}

	@Override
	public LockingSupport getLockingSupport() {
		return lockingSupport;
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String createTableCommand(TableCreationKind kind) {
		return isDefaultTableTypeColumn() ? "create column table" : "create row table";
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String addColumnPrefix() {
		return "add (";
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String addColumnSuffix() {
		return ")";
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public synchronized SchemaDropSupport getSchemaDropSupport() {
		if ( schemaDropSupport == null ) {
			schemaDropSupport = new SchemaDropSupport( List.of(), ConstraintDropMode.IMPLICIT, " cascade" );
		}
		return schemaDropSupport;
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public CurrentTimestampSelection getCurrentTimestampSelection() {
		return CurrentTimestampSelection.prepared( "select current_timestamp from sys.dummy" );
	}





	private static final SequenceInformationExtractor SEQUENCE_INFORMATION_EXTRACTOR =
			SequenceInformationExtractors.builder( "select * from sys.sequences" )
					.withoutCatalog()
					.schemaColumn( "schema_name" )
					.startValueColumn( "start_number" )
					.minimumValueColumn( "min_value" )
					.maximumValueColumn( "max_value" )
					.incrementValueColumn( "increment_by" )
					.build();

	@Override
	public SequenceInformationExtractor getSequenceInformationExtractor() {
		return SEQUENCE_INFORMATION_EXTRACTOR;
	}

	@Override
	@SPI({ USE, IMPLEMENT, SUPPLY })
	protected void contributeKeywords(KeywordRegistration registration) {
		super.contributeKeywords( registration );
		// https://help.sap.com/docs/SAP_HANA_PLATFORM/4fe29514fd584807ac9f2a04f6754767/28bcd6af3eb6437892719f7c27a8a285.html?locale=en-US
		registration.registerKeyword( "all" );
		registration.registerKeyword( "alter" );
		registration.registerKeyword( "as" );
		registration.registerKeyword( "before" );
		registration.registerKeyword( "begin" );
		registration.registerKeyword( "both" );
		registration.registerKeyword( "case" );
		registration.registerKeyword( "char" );
		registration.registerKeyword( "condition" );
		registration.registerKeyword( "connect" );
		registration.registerKeyword( "cross" );
		registration.registerKeyword( "cube" );
		registration.registerKeyword( "current_connection" );
		registration.registerKeyword( "current_date" );
		registration.registerKeyword( "current_schema" );
		registration.registerKeyword( "current_time" );
		registration.registerKeyword( "current_timestamp" );
		registration.registerKeyword( "current_transaction_isolation_level" );
		registration.registerKeyword( "current_user" );
		registration.registerKeyword( "current_utcdate" );
		registration.registerKeyword( "current_utctime" );
		registration.registerKeyword( "current_utctimestamp" );
		registration.registerKeyword( "currval" );
		registration.registerKeyword( "cursor" );
		registration.registerKeyword( "declare" );
		registration.registerKeyword( "deferred" );
		registration.registerKeyword( "distinct" );
		registration.registerKeyword( "else" );
		registration.registerKeyword( "elseif" );
		registration.registerKeyword( "end" );
		registration.registerKeyword( "except" );
		registration.registerKeyword( "exception" );
		registration.registerKeyword( "exec" );
		registration.registerKeyword( "false" );
		registration.registerKeyword( "for" );
		registration.registerKeyword( "from" );
		registration.registerKeyword( "full" );
		registration.registerKeyword( "group" );
		registration.registerKeyword( "having" );
		registration.registerKeyword( "if" );
		registration.registerKeyword( "in" );
		registration.registerKeyword( "inner" );
		registration.registerKeyword( "inout" );
		registration.registerKeyword( "intersect" );
		registration.registerKeyword( "into" );
		registration.registerKeyword( "is" );
		registration.registerKeyword( "join" );
		registration.registerKeyword( "lateral" );
		registration.registerKeyword( "leading" );
		registration.registerKeyword( "left" );
		registration.registerKeyword( "limit" );
		registration.registerKeyword( "loop" );
		registration.registerKeyword( "minus" );
		registration.registerKeyword( "natural" );
		registration.registerKeyword( "nchar" );
		registration.registerKeyword( "nextval" );
		registration.registerKeyword( "null" );
		registration.registerKeyword( "on" );
		registration.registerKeyword( "order" );
		registration.registerKeyword( "out" );
		registration.registerKeyword( "prior" );
		registration.registerKeyword( "return" );
		registration.registerKeyword( "returns" );
		registration.registerKeyword( "reverse" );
		registration.registerKeyword( "right" );
		registration.registerKeyword( "rollup" );
		registration.registerKeyword( "rowid" );
		registration.registerKeyword( "select" );
		registration.registerKeyword( "session_user" );
		registration.registerKeyword( "set" );
		registration.registerKeyword( "sql" );
		registration.registerKeyword( "start" );
		registration.registerKeyword( "sysuuid" );
		registration.registerKeyword( "tablesample" );
		registration.registerKeyword( "top" );
		registration.registerKeyword( "trailing" );
		registration.registerKeyword( "true" );
		registration.registerKeyword( "union" );
		registration.registerKeyword( "unknown" );
		registration.registerKeyword( "using" );
		registration.registerKeyword( "utctimestamp" );
		registration.registerKeyword( "values" );
		registration.registerKeyword( "when" );
		registration.registerKeyword( "where" );
		registration.registerKeyword( "while" );
		registration.registerKeyword( "with" );
		if ( isCloud() ) {
			// https://help.sap.com/docs/hana-cloud-database/sap-hana-cloud-sap-hana-database-sql-reference-guide/reserved-words
			registration.registerKeyword( "array" );
			registration.registerKeyword( "at" );
			registration.registerKeyword( "authorization" );
			registration.registerKeyword( "between" );
			registration.registerKeyword( "by" );
			registration.registerKeyword( "collate" );
			registration.registerKeyword( "empty" );
			registration.registerKeyword( "filter" );
			registration.registerKeyword( "grouping" );
			registration.registerKeyword( "no" );
			registration.registerKeyword( "not" );
			registration.registerKeyword( "of" );
			registration.registerKeyword( "over" );
			registration.registerKeyword( "recursive" );
			registration.registerKeyword( "row" );
			registration.registerKeyword( "table" );
			registration.registerKeyword( "to" );
			registration.registerKeyword( "unnest" );
			registration.registerKeyword( "window" );
			registration.registerKeyword( "within" );
		}
	}

	@Override
	@SPI({ USE, IMPLEMENT, SUPPLY })
	public ScrollMode defaultScrollMode() {
		return ScrollMode.FORWARD_ONLY;
	}

	/**
	 * HANA currently does not support check constraints.
	 */
	@Override
	@SPI({ USE, IMPLEMENT })
	public boolean supports(org.hibernate.dialect.constraint.spi.CheckConstraintPlacement placement) {
		return placement == org.hibernate.dialect.constraint.spi.CheckConstraintPlacement.TABLE;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public TemporalValueSemantics getTemporalValueSemantics() {
		return TemporalValueSemantics.TRUNCATING;
	}

	@Override
	public SubquerySupport getSubquerySupport() {
		return SubquerySupport.builder()
				.feature( SubquerySupport.Feature.EXISTS_IN_SELECT, false )
				.feature( SubquerySupport.Feature.OFFSET, true )
				.feature( SubquerySupport.Feature.LATERAL, getVersion().isSameOrAfter( 2, 0, 40 ) )
				.build();
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public SequenceSupport getSequenceSupport() {
		return CommunitySequenceSupports.hana();
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public int getMaxAliasLength() {
		return 128;
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public int getMaxIdentifierLength() {
		return 127;
	}

	@Override
	public LimitHandler getLimitHandler() {
		return LimitOffsetLimitHandler.INSTANCE;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public NameQualifierSupport getNameQualifierSupport() {
		return NameQualifierSupport.SCHEMA;
	}

	@Override
	@SPI({ USE, IMPLEMENT, SUPPLY })
	public IdentifierHelper buildIdentifierHelper(IdentifierHelperBuildRequest request) {
		final var builder = request.builder();
		/*
		 * HANA-specific extensions
		 */
		builder.setQuotedCaseStrategy( IdentifierCaseStrategy.MIXED );
		builder.setUnquotedCaseStrategy( IdentifierCaseStrategy.UPPER );

		final IdentifierHelper identifierHelper = super.buildIdentifierHelper( request );

		return new DelegatingIdentifierHelper( identifierHelper ) {
			@Override
			public Identifier toIdentifier(String text) {
				return normalizeQuoting( Identifier.toIdentifier( text ) );
			}

			@Override
			public Identifier toIdentifier(String text, boolean quoted) {
				return normalizeQuoting( Identifier.toIdentifier( text, quoted ) );
			}

			@Override
			public Identifier toIdentifier(String text, boolean quoted, boolean isExplicit) {
				return normalizeQuoting( Identifier.toIdentifier( text, quoted, false, isExplicit ) );
			}

			@Override
			public Identifier normalizeQuoting(Identifier identifier) {
				Identifier normalizedIdentifier = super.normalizeQuoting( identifier );

				if ( normalizedIdentifier == null ) {
					return null;
				}

				// need to quote names containing special characters like ':'
				if ( !normalizedIdentifier.isQuoted() && !normalizedIdentifier.getText().matches( "\\w+" ) ) {
					normalizedIdentifier = normalizedIdentifier.quoted();
				}

				return normalizedIdentifier;
			}
		};
	}










	@Override
	@SPI({ USE, IMPLEMENT })
	public String getQueryHintString(String query, List<String> hints) {
		return query + " with hint (" + String.join( ",", hints ) + ")";
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public org.hibernate.dialect.schema.spi.SchemaCommentSupport getSchemaCommentSupport() {
		return org.hibernate.dialect.schema.spi.SchemaCommentSupports.hanaInline();
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public void contributeTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		final ConfigurationService configurationService = serviceRegistry.requireService( ConfigurationService.class );
		this.defaultTableTypeColumn = configurationService.getSetting(
				USE_DEFAULT_TABLE_TYPE_COLUMN,
				StandardConverters.BOOLEAN,
				this.defaultTableTypeColumn
		);
		if ( supportsAsciiStringTypes() ) {
			this.useUnicodeStringTypes = configurationService.getSetting(
					USE_UNICODE_STRING_TYPES_PARAMETER_NAME,
					StandardConverters.BOOLEAN,
					useUnicodeStringTypesDefault()
			);
		}
		this.useLegacyBooleanType = configurationService.getSetting(
				USE_LEGACY_BOOLEAN_TYPE_PARAMETER_NAME,
				StandardConverters.BOOLEAN,
				USE_LEGACY_BOOLEAN_TYPE_DEFAULT_VALUE
		);
		this.treatDoubleTypedFieldsAsDecimal = configurationService.getSetting(
				TREAT_DOUBLE_TYPED_FIELDS_AS_DECIMAL_PARAMETER_NAME,
				StandardConverters.BOOLEAN,
				TREAT_DOUBLE_TYPED_FIELDS_AS_DECIMAL_DEFAULT_VALUE
		);
		super.contributeTypes( typeContributions, serviceRegistry );

		final TypeConfiguration typeConfiguration = typeContributions.getTypeConfiguration();
		final JdbcTypeRegistry jdbcTypeRegistry = typeConfiguration.getJdbcTypeRegistry();
		if ( treatDoubleTypedFieldsAsDecimal ) {
			typeConfiguration.getBasicTypeRegistry()
					.register(
							typeConfiguration.getBasicTypeRegistry()
									.resolve( DoubleJavaType.INSTANCE, NumericJdbcType.INSTANCE ),
							Double.class.getName()
					);
			final Map<Integer, Set<String>> jdbcToHibernateTypeContributionMap = typeConfiguration.getJdbcToHibernateTypeContributionMap();
			jdbcToHibernateTypeContributionMap.computeIfAbsent( Types.FLOAT, code -> new HashSet<>() ).clear();
			jdbcToHibernateTypeContributionMap.computeIfAbsent( Types.REAL, code -> new HashSet<>() ).clear();
			jdbcToHibernateTypeContributionMap.computeIfAbsent( Types.DOUBLE, code -> new HashSet<>() ).clear();
			jdbcToHibernateTypeContributionMap.get( Types.FLOAT ).add( StandardBasicTypes.BIG_DECIMAL.getName() );
			jdbcToHibernateTypeContributionMap.get( Types.REAL ).add( StandardBasicTypes.BIG_DECIMAL.getName() );
			jdbcToHibernateTypeContributionMap.get( Types.DOUBLE ).add( StandardBasicTypes.BIG_DECIMAL.getName() );
			jdbcTypeRegistry.addDescriptor( Types.FLOAT, NumericJdbcType.INSTANCE );
			jdbcTypeRegistry.addDescriptor( Types.REAL, NumericJdbcType.INSTANCE );
			jdbcTypeRegistry.addDescriptor( Types.DOUBLE, NumericJdbcType.INSTANCE );
		}

		jdbcTypeRegistry.addDescriptor( Types.CLOB, new HANAClobJdbcType( maxLobPrefetchSize, useUnicodeStringTypes ) );
		jdbcTypeRegistry.addDescriptor( Types.NCLOB, new HANANClobJdbcType( maxLobPrefetchSize ) );
		jdbcTypeRegistry.addDescriptor( Types.BLOB, new HANABlobType( maxLobPrefetchSize ) );
		// tinyint is unsigned on HANA
		jdbcTypeRegistry.addDescriptor( Types.TINYINT, TinyIntAsSmallIntJdbcType.INSTANCE );
		if ( isUseUnicodeStringTypes() ) {
			jdbcTypeRegistry.addDescriptor( Types.VARCHAR, NVarcharJdbcType.INSTANCE );
			jdbcTypeRegistry.addDescriptor( Types.CHAR, NCharJdbcType.INSTANCE );
		}
		if ( treatDoubleTypedFieldsAsDecimal ) {
			jdbcTypeRegistry.addDescriptor( Types.DOUBLE, DecimalJdbcType.INSTANCE );
		}
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public void appendBooleanValueString(SqlAppender appender, boolean bool) {
		if ( this.useLegacyBooleanType ) {
			appender.appendSql( bool ? '1' : '0' );
		}
		else {
			appender.appendSql( bool );
		}
	}

	@Override
	public IdentityColumnSupport getIdentityColumnSupport() {
		return HANAIdentityColumnSupport.INSTANCE;
	}

	@Override
	public Exporter<Table> getTableExporter() {
		return this.hanaTableExporter;
	}

	/*
	 * HANA doesn't really support REF_CURSOR returns from a procedure, but REF_CURSOR support can be emulated by using
	 * procedures or functions with an OUT parameter of type TABLE. The results will be returned as result sets on the
	 * callable statement.
	 */
	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public CallableStatementSupport getCallableStatementSupport() {
		return CallableStatementSupports.standardWithRefCursors();
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public RefCursorSupportFactory getRefCursorSupportFactory() {
		return REF_CURSOR_SUPPORT_FACTORY;
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
	@SPI({ IMPLEMENT, SUPPLY })
	public LobSupport getLobSupport() {
		return LobSupports.noContextualCreation();
	}

	@Override
	public NullOrderingSupport getNullOrderingSupport() {
		return NullOrderingSupport.builder( super.getNullOrderingSupport() )
				.defaultOrdering( NullOrdering.SMALLEST )
				.build();
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public void appendFormat(SqlAppender appender, String format) {
		//I don't think HANA needs FM
		appender.appendSql( OracleDialect.datetimeFormat( format, false, false ).result() );
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public long fractionalSecondPrecisionInNanos() {
		return 100;
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String timestampaddPattern(TemporalUnit unit, TemporalType temporalType, IntervalType intervalType) {
		switch (unit) {
			case NANOSECOND:
				if ( temporalType == TemporalType.TIME ) {
					return "cast(add_nano100(cast('1970-01-01 '||(?3) as timestamp),?2/100) as time)";
				}
				else {
					return "add_nano100(?3,?2/100)";
				}
			case NATIVE:
				if ( temporalType == TemporalType.TIME ) {
					return "cast(add_nano100(cast('1970-01-01 '||(?3) as timestamp),?2) as time)";
				}
				else {
					return "add_nano100(?3,?2)";
				}
			case QUARTER:
				return "add_months(?3,3*?2)";
			case WEEK:
				return "add_days(?3,7*?2)";
			case MINUTE:
				if ( temporalType == TemporalType.TIME ) {
					return "cast(add_seconds(cast('1970-01-01 '||(?3) as timestamp),60*?2) as time)";
				}
				else {
					return "add_seconds(?3,60*?2)";
				}
			case HOUR:
				if ( temporalType == TemporalType.TIME ) {
					return "cast(add_seconds(cast('1970-01-01 '||(?3) as timestamp),3600*?2) as time)";
				}
				else {
					return "add_seconds(?3,3600*?2)";
				}
			case SECOND:
				if ( temporalType == TemporalType.TIME ) {
					return "cast(add_seconds(cast('1970-01-01 '||(?3) as timestamp),?2) as time)";
				}
				// Fall through on purpose
			default:
				return "add_?1s(?3,?2)";
		}
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String timestampdiffPattern(TemporalUnit unit, TemporalType fromTemporalType, TemporalType toTemporalType) {
		switch (unit) {
			case NANOSECOND:
				if ( fromTemporalType == TemporalType.TIME && toTemporalType == TemporalType.TIME ) {
					return "seconds_between(?2,?3)*1000000000";
				}
				else {
					return "nano100_between(?2,?3)*100";
				}
			case NATIVE:
				if ( fromTemporalType == TemporalType.TIME && toTemporalType == TemporalType.TIME ) {
					return "seconds_between(?2,?3)*10000000";
				}
				else {
					return "nano100_between(?2,?3)";
				}
			case QUARTER:
				return "months_between(?2,?3)/3";
			case WEEK:
				return "days_between(?2,?3)/7";
			case MINUTE:
				return "seconds_between(?2,?3)/60";
			case HOUR:
				return "seconds_between(?2,?3)/3600";
			default:
				return "?1s_between(?2,?3)";
		}
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
				appender.appendSql( "{d '" );
				appendAsDate( appender, temporalAccessor );
				appender.appendSql( "'}" );
				break;
			case TIME:
				appender.appendSql( "{t '" );
				appendAsTime( appender, temporalAccessor, getTemporalValueSemantics().supportsLiteralOffset(), jdbcTimeZone );
				appender.appendSql( "'}" );
				break;
			case TIMESTAMP:
				appender.appendSql( "{ts '" );
				appendAsTimestampWithMicros( appender, temporalAccessor, getTemporalValueSemantics().supportsLiteralOffset(), jdbcTimeZone );
				appender.appendSql( "'}" );
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
				appender.appendSql( "{d '" );
				appendAsDate( appender, date );
				appender.appendSql( "'}" );
				break;
			case TIME:
				appender.appendSql( "{t '" );
				appendAsLocalTime( appender, date );
				appender.appendSql( "'}" );
				break;
			case TIMESTAMP:
				appender.appendSql( "{ts '" );
				appendAsTimestampWithMicros( appender, date, jdbcTimeZone );
				appender.appendSql( "'}" );
				break;
			default:
				throw new IllegalArgumentException();
		}
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public void appendDefinition(SqlAppender appender, ColumnDefinitionRequest request) {
		appender.appendSql( ' ' );
		appender.appendSql( request.sqlType() );
		if ( request.renderedCollation() != null ) {
			appender.appendSql( " collate " );
			appender.appendSql( request.renderedCollation() );
		}
		if ( request.defaultExpression() != null ) {
			appender.appendSql( " default " );
			appender.appendSql( request.defaultExpression() );
		}
		if ( request.generatedExpression() != null ) {
			appender.appendSql( " generated always as (" );
			appender.appendSql( request.generatedExpression() );
			appender.appendSql( ')' );
		}
		if ( !request.nullable() ) {
			appender.appendSql( " not null" );
		}
	}

	public boolean isUseUnicodeStringTypes() {
		return this.useUnicodeStringTypes || isDefaultTableTypeColumn() && isCloud();
	}

	protected boolean supportsAsciiStringTypes() {
		return !isDefaultTableTypeColumn() || !isCloud();
	}

	protected Boolean useUnicodeStringTypesDefault() {
		return isDefaultTableTypeColumn() ? isCloud() : Boolean.FALSE;
	}

	private static class CloseSuppressingReader extends FilterReader {

		protected CloseSuppressingReader(final Reader in) {
			super( in );
		}

		@Override
		public void close() {
			// do not close
		}
	}

	private static class CloseSuppressingInputStream extends FilterInputStream {

		protected CloseSuppressingInputStream(final InputStream in) {
			super( in );
		}

		@Override
		public void close() {
			// do not close
		}
	}

	private static class MaterializedBlob implements Blob {

		private byte[] bytes = null;

		public MaterializedBlob(byte[] bytes) {
			this.setBytes( bytes );
		}

		@Override
		public long length() throws SQLException {
			return this.getBytes().length;
		}

		@Override
		public byte[] getBytes(long pos, int length) throws SQLException {
			return Arrays.copyOfRange( this.bytes, (int) ( pos - 1 ), (int) ( pos - 1 + length ) );
		}

		@Override
		public InputStream getBinaryStream() throws SQLException {
			return new ByteArrayInputStream( this.getBytes() );
		}

		@Override
		public long position(byte[] pattern, long start) throws SQLException {
			throw new SQLFeatureNotSupportedException();
		}

		@Override
		public long position(Blob pattern, long start) throws SQLException {
			throw new SQLFeatureNotSupportedException();
		}

		@Override
		public int setBytes(long pos, byte[] bytes) throws SQLException {
			int bytesSet = 0;
			if ( this.bytes.length < pos - 1 + bytes.length ) {
				this.bytes = Arrays.copyOf( this.bytes, (int) ( pos - 1 + bytes.length ) );
			}
			for ( int i = 0; i < bytes.length && i < this.bytes.length; i++, bytesSet++ ) {
				this.bytes[(int) ( i + pos - 1 )] = bytes[i];
			}
			return bytesSet;
		}

		@Override
		public int setBytes(long pos, byte[] bytes, int offset, int len) throws SQLException {
			int bytesSet = 0;
			if ( this.bytes.length < pos - 1 + len ) {
				this.bytes = Arrays.copyOf( this.bytes, (int) ( pos - 1 + len ) );
			}
			for ( int i = offset; i < len && i < this.bytes.length; i++, bytesSet++ ) {
				this.bytes[(int) ( i + pos - 1 )] = bytes[i];
			}
			return bytesSet;
		}

		@Override
		public OutputStream setBinaryStream(long pos) {
			return new ByteArrayOutputStream() {

				{
					this.buf = getBytes();
				}
			};
		}

		@Override
		public void truncate(long len) throws SQLException {
			this.setBytes( Arrays.copyOf( this.getBytes(), (int) len ) );
		}

		@Override
		public void free() throws SQLException {
			this.setBytes( null );
		}

		@Override
		public InputStream getBinaryStream(long pos, long length) throws SQLException {
			return new ByteArrayInputStream( this.getBytes(), (int) ( pos - 1 ), (int) length );
		}

		byte[] getBytes() {
			return this.bytes;
		}

		void setBytes(byte[] bytes) {
			this.bytes = bytes;
		}

	}

	private static class MaterializedNClob implements NClob {

		private String data;

		public MaterializedNClob(String data) {
			this.data = data;
		}

		@Override
		public void truncate(long len) throws SQLException {
			this.data = "";
		}

		@Override
		public int setString(long pos, String str, int offset, int len) throws SQLException {
			this.data = this.data.substring( 0, (int) ( pos - 1 ) ) + str.substring( offset, offset + len )
					+ this.data.substring( (int) ( pos - 1 + len ) );
			return len;
		}

		@Override
		public int setString(long pos, String str) throws SQLException {
			this.data = this.data.substring( 0, (int) ( pos - 1 ) ) + str + this.data.substring( (int) ( pos - 1 + str.length() ) );
			return str.length();
		}

		@Override
		public Writer setCharacterStream(long pos) throws SQLException {
			throw new SQLFeatureNotSupportedException();
		}

		@Override
		public OutputStream setAsciiStream(long pos) throws SQLException {
			throw new SQLFeatureNotSupportedException();
		}

		@Override
		public long position(Clob searchstr, long start) throws SQLException {
			return this.data.indexOf( extractString( searchstr ), (int) ( start - 1 ) );
		}

		@Override
		public long position(String searchstr, long start) throws SQLException {
			return this.data.indexOf( searchstr, (int) ( start - 1 ) );
		}

		@Override
		public long length() throws SQLException {
			return this.data.length();
		}

		@Override
		public String getSubString(long pos, int length) throws SQLException {
			return this.data.substring( (int) ( pos - 1 ), (int) ( pos - 1 + length ) );
		}

		@Override
		public Reader getCharacterStream(long pos, long length) throws SQLException {
			return new StringReader( this.data.substring( (int) ( pos - 1 ), (int) ( pos - 1 + length ) ) );
		}

		@Override
		public Reader getCharacterStream() throws SQLException {
			return new StringReader( this.data );
		}

		@Override
		public InputStream getAsciiStream() {
			return new ByteArrayInputStream( this.data.getBytes( StandardCharsets.ISO_8859_1 ) );
		}

		@Override
		public void free() throws SQLException {
			this.data = null;
		}
	}

	private static class BlobExtractor<X> extends BasicExtractor<X> {
		private final int maxLobPrefetchSize;

		public BlobExtractor(JavaType<X> javaType, JdbcType jdbcType, int maxLobPrefetchSize) {
			super( javaType, jdbcType );
			this.maxLobPrefetchSize = maxLobPrefetchSize;
		}

		private X doExtract(Blob blob, WrapperOptions options) throws SQLException {
			final X result;
			if ( blob == null ) {
				result = getJavaType().wrap( null, options );
			}
			else if ( blob.length() < maxLobPrefetchSize ) {
				result = getJavaType().wrap( blob, options );
				blob.free();
			}
			else {
				final MaterializedBlob materialized = new MaterializedBlob( extractBytes( blob.getBinaryStream() ) );
				blob.free();
				result = getJavaType().wrap( materialized, options );
			}
			return result;
		}

		@Override
		protected X doExtract(ResultSet rs, int paramIndex, WrapperOptions options) throws SQLException {
			return doExtract( rs.getBlob( paramIndex ), options );
		}

		@Override
		protected X doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
			return doExtract( statement.getBlob( index ), options );
		}

		@Override
		protected X doExtract(CallableStatement statement, String name, WrapperOptions options) throws SQLException {
			return doExtract( statement.getBlob( name ), options );
		}
	}

	private static class HANAStreamBlobType implements JdbcType {

		private static final long serialVersionUID = -2476600722093442047L;

		final int maxLobPrefetchSize;

		public HANAStreamBlobType(int maxLobPrefetchSize) {
			this.maxLobPrefetchSize = maxLobPrefetchSize;
		}

		@Override
		public String getFriendlyName() {
			return "BLOB (hana-stream)";
		}

		@Override
		public String toString() {
			return "HANAStreamBlobType";
		}

		@Override
		public int getJdbcTypeCode() {
			return Types.BLOB;
		}

		@Override
		public <X> ValueBinder<X> getBinder(JavaType<X> javaType) {
			return new BasicBinder<>( javaType, this ) {

				@Override
				protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options) throws SQLException {
					final BinaryStream binaryStream = javaType.unwrap( value, BinaryStream.class, options );
					if ( value instanceof BlobImplementer) {
						try ( InputStream is = new CloseSuppressingInputStream( binaryStream.getInputStream() ) ) {
							st.setBinaryStream( index, is, binaryStream.getLength() );
						}
						catch (IOException e) {
							// can't happen => ignore
						}
					}
					else {
						st.setBinaryStream( index, binaryStream.getInputStream(), binaryStream.getLength() );
					}
				}

				@Override
				protected void doBind(CallableStatement st, X value, String name, WrapperOptions options) throws SQLException {
					final BinaryStream binaryStream = javaType.unwrap( value, BinaryStream.class, options );
					if ( value instanceof BlobImplementer ) {
						try ( InputStream is = new CloseSuppressingInputStream( binaryStream.getInputStream() ) ) {
							st.setBinaryStream( name, is, binaryStream.getLength() );
						}
						catch (IOException e) {
							// can't happen => ignore
						}
					}
					else {
						st.setBinaryStream( name, binaryStream.getInputStream(), binaryStream.getLength() );
					}
				}
			};
		}

		@Override
		public <X> ValueExtractor<X> getExtractor(JavaType<X> javaType) {
			return new BlobExtractor<>( javaType, this, maxLobPrefetchSize );
		}
	}

	// the ClobTypeDescriptor and NClobTypeDescriptor for HANA are slightly
	// changed from the standard ones. The HANA JDBC driver currently closes any
	// stream passed in via
	// PreparedStatement.setCharacterStream(int,Reader,long)
	// after the stream has been processed. this causes problems later if we are
	// using non-contextual lob creation and HANA then closes our StringReader.
	// see test case LobLocatorTest

	private static class HANAClobJdbcType extends ClobJdbcType {
		@Override
		public String toString() {
			return "HANAClobTypeDescriptor";
		}

		/** serial version uid. */
		private static final long serialVersionUID = -379042275442752102L;

		final int maxLobPrefetchSize;
		final boolean useUnicodeStringTypes;

		public HANAClobJdbcType(int maxLobPrefetchSize, boolean useUnicodeStringTypes) {
			this.maxLobPrefetchSize = maxLobPrefetchSize;
			this.useUnicodeStringTypes = useUnicodeStringTypes;
		}

		@Override
		public <X> BasicBinder<X> getClobBinder(final JavaType<X> javaType) {
			return new BasicBinder<>( javaType, this ) {

				@Override
				protected void doBind(final PreparedStatement st, final X value, final int index, final WrapperOptions options) throws SQLException {
					final CharacterStream characterStream = javaType.unwrap( value, CharacterStream.class, options );

					if ( value instanceof ClobImplementer) {
						try ( Reader r = new CloseSuppressingReader( characterStream.asReader() ) ) {
							st.setCharacterStream( index, r, characterStream.getLength() );
						}
						catch (IOException e) {
							// can't happen => ignore
						}
					}
					else {
						st.setCharacterStream( index, characterStream.asReader(), characterStream.getLength() );
					}

				}

				@Override
				protected void doBind(CallableStatement st, X value, String name, WrapperOptions options) throws SQLException {
					final CharacterStream characterStream = javaType.unwrap( value, CharacterStream.class, options );

					if ( value instanceof ClobImplementer ) {
						try ( Reader r = new CloseSuppressingReader( characterStream.asReader() ) ) {
							st.setCharacterStream( name, r, characterStream.getLength() );
						}
						catch (IOException e) {
							// can't happen => ignore
						}
					}
					else {
						st.setCharacterStream( name, characterStream.asReader(), characterStream.getLength() );
					}
				}
			};
		}

		@Override
		public <X> ValueExtractor<X> getExtractor(JavaType<X> javaType) {
			return new BasicExtractor<>( javaType, this ) {
				private X doExtract(Clob clob, WrapperOptions options) throws SQLException {
					final X result;
					if ( clob == null ) {
						result = getJavaType().wrap( null, options );
					}
					else if ( clob.length() < maxLobPrefetchSize ) {
						result = getJavaType().wrap(clob, options);
						clob.free();
					}
					else {
						final MaterializedNClob materialized = new MaterializedNClob( extractString( clob ) );
						clob.free();
						result = getJavaType().wrap( materialized, options );
					}
					return result;
				}

				@Override
				protected X doExtract(ResultSet rs, int paramIndex, WrapperOptions options) throws SQLException {
					final Clob clob = useUnicodeStringTypes ? rs.getNClob( paramIndex ) : rs.getClob( paramIndex );
					return doExtract( clob, options );
				}

				@Override
				protected X doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
					final Clob clob = useUnicodeStringTypes ? statement.getNClob( index ) : statement.getClob( index );
					return doExtract( clob, options );
				}

				@Override
				protected X doExtract(CallableStatement statement, String name, WrapperOptions options) throws SQLException {
					final Clob clob = useUnicodeStringTypes ? statement.getNClob( name ) : statement.getClob( name );
					return doExtract( clob, options );
				}
			};
		}

		public int getMaxLobPrefetchSize() {
			return this.maxLobPrefetchSize;
		}

		public boolean isUseUnicodeStringTypes() {
			return this.useUnicodeStringTypes;
		}
	}

	private static class HANANClobJdbcType extends NClobJdbcType {

		/** serial version uid. */
		private static final long serialVersionUID = 5651116091681647859L;

		final int maxLobPrefetchSize;

		public HANANClobJdbcType(int maxLobPrefetchSize) {
			this.maxLobPrefetchSize = maxLobPrefetchSize;
		}

		@Override
		public String toString() {
			return "HANANClobTypeDescriptor";
		}

		@Override
		public <X> BasicBinder<X> getNClobBinder(final JavaType<X> javaType) {
			return new BasicBinder<>( javaType, this ) {

				@Override
				protected void doBind(final PreparedStatement st, final X value, final int index, final WrapperOptions options) throws SQLException {
					final CharacterStream characterStream = javaType.unwrap( value, CharacterStream.class, options );

					if ( value instanceof NClobImplementer) {
						try ( Reader r = new CloseSuppressingReader( characterStream.asReader() ) ) {
							st.setCharacterStream( index, r, characterStream.getLength() );
						}
						catch (IOException e) {
							// can't happen => ignore
						}
					}
					else {
						st.setCharacterStream( index, characterStream.asReader(), characterStream.getLength() );
					}

				}

				@Override
				protected void doBind(CallableStatement st, X value, String name, WrapperOptions options) throws SQLException {
					final CharacterStream characterStream = javaType.unwrap( value, CharacterStream.class, options );

					if ( value instanceof NClobImplementer ) {
						try ( Reader r = new CloseSuppressingReader( characterStream.asReader() ) ) {
							st.setCharacterStream( name, r, characterStream.getLength() );
						}
						catch (IOException e) {
							// can't happen => ignore
						}
					}
					else {
						st.setCharacterStream( name, characterStream.asReader(), characterStream.getLength() );
					}
				}
			};
		}

		@Override
		public <X> ValueExtractor<X> getExtractor(JavaType<X> javaType) {
			return new BasicExtractor<>( javaType, this ) {
				private X doExtract(NClob nclob, WrapperOptions options) throws SQLException {
					final X result;
					if ( nclob == null ) {
						result = getJavaType().wrap( null, options );
					}
					else if ( nclob.length() < maxLobPrefetchSize ) {
						result = javaType.wrap(nclob, options);
						nclob.free();
					}
					else {
						final MaterializedNClob materialized = new MaterializedNClob( extractString( nclob ) );
						nclob.free();
						result = getJavaType().wrap( materialized, options );
					}
					return result;
				}

				@Override
				protected X doExtract(ResultSet rs, int paramIndex, WrapperOptions options) throws SQLException {
					return doExtract( rs.getNClob( paramIndex ), options );
				}

				@Override
				protected X doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
					return doExtract( statement.getNClob( index ), options );
				}

				@Override
				protected X doExtract(CallableStatement statement, String name, WrapperOptions options) throws SQLException {
					return doExtract( statement.getNClob( name ), options );
				}
			};
		}

		public int getMaxLobPrefetchSize() {
			return maxLobPrefetchSize;
		}
	}

	public static class HANABlobType implements JdbcType {

		private static final long serialVersionUID = 5874441715643764323L;
		public static final JdbcType INSTANCE = new HANABlobType( MAX_LOB_PREFETCH_SIZE_DEFAULT_VALUE );

		final int maxLobPrefetchSize;

		final HANAStreamBlobType hanaStreamBlobTypeDescriptor;

		public HANABlobType(int maxLobPrefetchSize) {
			this.maxLobPrefetchSize = maxLobPrefetchSize;
			this.hanaStreamBlobTypeDescriptor = new HANAStreamBlobType( maxLobPrefetchSize );
		}

		@Override
		public int getJdbcTypeCode() {
			return Types.BLOB;
		}

		@Override
		public String getFriendlyName() {
			return "BLOB (HANA)";
		}

		@Override
		public String toString() {
			return "HANABlobType";
		}

		@Override
		public <X> ValueExtractor<X> getExtractor(final JavaType<X> javaType) {
			return new BlobExtractor<>( javaType, this, maxLobPrefetchSize );
		}

		@Override
		public <X> BasicBinder<X> getBinder(final JavaType<X> javaType) {
			return new BasicBinder<>( javaType, this ) {

				@Override
				protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options) throws SQLException {
					JdbcType descriptor = BlobJdbcType.BLOB_BINDING;
					if ( value instanceof byte[] ) {
						// performance shortcut for binding BLOB data in byte[] format
						descriptor = BlobJdbcType.PRIMITIVE_ARRAY_BINDING;
					}
					else if ( options.useStreamForLobBinding() ) {
						descriptor = hanaStreamBlobTypeDescriptor;
					}
					descriptor.getBinder( javaType ).bind( st, value, index, options );
				}

				@Override
				protected void doBind(CallableStatement st, X value, String name, WrapperOptions options) throws SQLException {
					JdbcType descriptor = BlobJdbcType.BLOB_BINDING;
					if ( value instanceof byte[] ) {
						// performance shortcut for binding BLOB data in byte[] format
						descriptor = BlobJdbcType.PRIMITIVE_ARRAY_BINDING;
					}
					else if ( options.useStreamForLobBinding() ) {
						descriptor = hanaStreamBlobTypeDescriptor;
					}
					descriptor.getBinder( javaType ).bind( st, value, name, options );
				}
			};
		}

		public int getMaxLobPrefetchSize() {
			return maxLobPrefetchSize;
		}
	}

	@Override
	public MultiTableMutationSupport getMultiTableMutationSupport() {
		return MultiTableMutationSupport.GLOBAL_TEMPORARY_TABLE;
	}

	@Override
	public TemporaryTableStrategy getGlobalTemporaryTableStrategy() {
		return StandardGlobalTemporaryTableStrategy.INSTANCE;
	}

	@Override
	@SPI({ USE, IMPLEMENT, SUPPLY })
	public DmlTargetColumnQualifierSupport getDmlTargetColumnQualifierSupport() {
		return DmlTargetColumnQualifierSupport.TABLE_ALIAS;
	}

	@Override
	public ValuesListSupport getValuesListSupport() {
		return ValuesListSupport.NONE;
	}

	@Override
	public MutationSyntaxSupport getMutationSyntaxSupport() {
		return isCloud()
				? MutationSyntaxSupport.NONE
				: MutationSyntaxSupport.of( MutationKind.UPDATE, MutationSyntaxCapability.FROM_CLAUSE );
	}



	@Override
	public SingleRowTableSupport getSingleRowTableSupport() {
		final String tableExpression = "sys.dummy";
		return SingleRowTableSupport.builder( super.getSingleRowTableSupport() )
				.tableExpression( tableExpression )
				.selectOnlyFromClause( " from " + tableExpression )
				.build();
	}

	@Override
	public RowValueSupport getRowValueSupport() {
		return RowValueSupport.builder( super.getRowValueSupport() )
				.feature( RowValueSupport.Feature.ORDERING_COMPARISON, false )
				.feature( RowValueSupport.Feature.QUANTIFIED_COMPARISON, false )
				.build();
	}

	@Override
	public CteSupport getCteSupport() {
		// HANA doesn't seem to support correlation, so report top-level support only
		return CteSupport.builder()
				.placement( CteSupport.Placement.TOP_LEVEL )
				.build();
	}

}
