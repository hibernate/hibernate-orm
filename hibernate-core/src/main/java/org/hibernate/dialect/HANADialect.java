/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import jakarta.persistence.TemporalType;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.ScrollMode;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.dialect.function.IntegralTimestampaddFunction;
import org.hibernate.dialect.identity.HANAIdentityColumnSupport;
import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.dialect.pagination.LimitOffsetLimitHandler;
import org.hibernate.dialect.sequence.HANASequenceSupport;
import org.hibernate.dialect.sequence.SequenceSupport;
import org.hibernate.dialect.temptable.TemporaryTable;
import org.hibernate.dialect.temptable.TemporaryTableKind;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.config.spi.StandardConverters;
import org.hibernate.engine.jdbc.BinaryStream;
import org.hibernate.engine.jdbc.BlobImplementer;
import org.hibernate.engine.jdbc.CharacterStream;
import org.hibernate.engine.jdbc.ClobImplementer;
import org.hibernate.engine.jdbc.NClobImplementer;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.jdbc.env.spi.IdentifierCaseStrategy;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelperBuilder;
import org.hibernate.engine.jdbc.env.spi.NameQualifierSupport;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.LockAcquisitionException;
import org.hibernate.exception.LockTimeoutException;
import org.hibernate.exception.SQLGrammarException;
import org.hibernate.exception.spi.SQLExceptionConversionDelegate;
import org.hibernate.internal.util.JdbcExceptionHelper;
import org.hibernate.mapping.Table;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.procedure.internal.StandardCallableStatementSupport;
import org.hibernate.procedure.spi.CallableStatementSupport;
import org.hibernate.query.sqm.CastType;
import org.hibernate.query.sqm.IntervalType;
import org.hibernate.query.common.TemporalUnit;
import org.hibernate.query.sqm.mutation.internal.temptable.GlobalTemporaryTableInsertStrategy;
import org.hibernate.query.sqm.mutation.internal.temptable.GlobalTemporaryTableMutationStrategy;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableInsertStrategy;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableMutationStrategy;
import org.hibernate.query.sqm.produce.function.FunctionParameterType;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.sql.ast.SqlAstNodeRenderingMode;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.spi.StandardSqlAstTranslatorFactory;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.tool.schema.extract.internal.SequenceInformationExtractorHANADatabaseImpl;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractor;
import org.hibernate.tool.schema.internal.StandardTableExporter;
import org.hibernate.tool.schema.spi.Exporter;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.DataHelper;
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
import org.hibernate.type.descriptor.sql.internal.CapacityDependentDdlType;
import org.hibernate.type.descriptor.sql.internal.DdlTypeImpl;
import org.hibernate.type.descriptor.sql.spi.DdlTypeRegistry;
import org.hibernate.type.internal.BasicTypeImpl;
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
import java.sql.DatabaseMetaData;
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

import static org.hibernate.dialect.HANAServerConfiguration.MAX_LOB_PREFETCH_SIZE_DEFAULT_VALUE;
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
import static org.hibernate.type.descriptor.DateTimeUtils.JDBC_ESCAPE_END;
import static org.hibernate.type.descriptor.DateTimeUtils.JDBC_ESCAPE_START_DATE;
import static org.hibernate.type.descriptor.DateTimeUtils.JDBC_ESCAPE_START_TIME;
import static org.hibernate.type.descriptor.DateTimeUtils.JDBC_ESCAPE_START_TIMESTAMP;
import static org.hibernate.type.descriptor.DateTimeUtils.appendAsDate;
import static org.hibernate.type.descriptor.DateTimeUtils.appendAsTime;
import static org.hibernate.type.descriptor.DateTimeUtils.appendAsTimestampWithMicros;

/**
 * An SQL dialect for the SAP HANA Platform and Cloud.
 * <p>
 * For more information on SAP HANA Cloud, refer to the
 * <a href="https://help.sap.com/docs/hana-cloud-database/sap-hana-cloud-sap-hana-database-sql-reference-guide/sap-hana-cloud-sap-hana-database-sql-reference-guide">SAP HANA Cloud SQL Reference Guide</a>.
 * For more information on SAP HANA Platform, refer to the
 * <a href="https://help.sap.com/docs/SAP_HANA_PLATFORM/4fe29514fd584807ac9f2a04f6754767/b4b0eec1968f41a099c828a4a6c8ca0f.html?locale=en-US">SAP HANA Platform SQL Reference Guide</a>.
 * <p>
 * Column tables are created by this dialect by default when using the auto-ddl feature.
 *
 * @author Andrew Clemons
 * @author Jonathan Bregler
 */
public class HANADialect extends Dialect {

	static final DatabaseVersion MINIMUM_VERSION = DatabaseVersion.make( 1, 0, 120 );

	public HANADialect(DialectResolutionInfo info) {
		this( HANAServerConfiguration.fromDialectResolutionInfo( info ), true );
		registerKeywords( info );
	}

	public HANADialect() {
		// SAP HANA 1.0 SPS12 R0 is the default
		this( MINIMUM_VERSION );
	}

	public HANADialect(DatabaseVersion version) {
		this( new HANAServerConfiguration( version ), true );
	}

	public HANADialect(DatabaseVersion version, boolean defaultTableTypeColumn) {
		this( new HANAServerConfiguration( version ), defaultTableTypeColumn );
	}

	public HANADialect(HANAServerConfiguration configuration, boolean defaultTableTypeColumn) {
		super( configuration.getFullVersion() );
		this.defaultTableTypeColumn = defaultTableTypeColumn;
		this.maxLobPrefetchSize = configuration.getMaxLobPrefetchSize();
		this.useUnicodeStringTypes = useUnicodeStringTypesDefault();
	}

	@Override
	protected DatabaseVersion getMinimumSupportedVersion() {
		return MINIMUM_VERSION;
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
			return quoteTypeIfNecessary( table, sqlCreateStrings, getCreateTableString() );
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


	@Override
	public void contribute(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		// This is the best hook for consuming dialect configuration that we have for now,
		// since this method is called very early in the bootstrap process
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
		super.contribute( typeContributions, serviceRegistry );
	}

	protected boolean isDefaultTableTypeColumn() {
		return defaultTableTypeColumn;
	}

	protected boolean isCloud() {
		return getVersion().isSameOrAfter( 4 );
	}

	@Override
	protected String columnType(int sqlTypeCode) {
		switch ( sqlTypeCode ) {
			case BOOLEAN:
				return useLegacyBooleanType ? "tinyint" : super.columnType( sqlTypeCode );
			case NUMERIC:
				//there is no 'numeric' type in HANA
				return columnType( DECIMAL );
			//'double precision' syntax not supported
			case DOUBLE:
				return "double";
			//no explicit precision
			case TIME:
			case TIME_WITH_TIMEZONE:
				return "time";
			case TIMESTAMP:
			case TIMESTAMP_WITH_TIMEZONE:
				return "timestamp";
			//there is no 'char' or 'nchar' type in HANA
			case CHAR:
			case VARCHAR:
				return isUseUnicodeStringTypes() ? columnType( NVARCHAR ) : super.columnType( VARCHAR );
			case NCHAR:
				return columnType( NVARCHAR );
			case LONG32VARCHAR:
				return isUseUnicodeStringTypes() ? columnType( LONG32NVARCHAR ) : super.columnType( LONG32VARCHAR );
			case CLOB:
				return isUseUnicodeStringTypes() ? columnType( NCLOB ) : super.columnType( CLOB );
			// map tinyint to smallint since tinyint is unsigned on HANA
			case TINYINT:
				return "smallint";
			default:
				return super.columnType( sqlTypeCode );
		}
	}

	@Override
	protected void registerColumnTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		super.registerColumnTypes( typeContributions, serviceRegistry );
		final DdlTypeRegistry ddlTypeRegistry = typeContributions.getTypeConfiguration().getDdlTypeRegistry();

		// varbinary max length 5000
		ddlTypeRegistry.addDescriptor(
				CapacityDependentDdlType.builder( BINARY, CapacityDependentDdlType.LobKind.BIGGEST_LOB, "blob", this )
						.withTypeCapacity( getMaxVarbinaryLength(), "varbinary($l)" )
						.build()
		);

		ddlTypeRegistry.addDescriptor( new DdlTypeImpl( GEOMETRY, "st_geometry", this ) );
		ddlTypeRegistry.addDescriptor( new DdlTypeImpl( POINT, "st_point", this ) );
	}

	@Override
	public boolean getDefaultNonContextualLobCreation() {
		// createBlob() and createClob() are not supported by the HANA JDBC driver
		return true;
	}

	@Override
	public boolean getDefaultUseGetGeneratedKeys() {
		// getGeneratedKeys() is not supported by the HANA JDBC driver
		return false;
	}

	@Override
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
	public int getDefaultTimestampPrecision() {
		return 7;
	}

	public int getDefaultDecimalPrecision() {
		//the maximum on HANA
		return 34;
	}

	@Override
	public int getMaxVarcharLength() {
		return 5000;
	}

	@Override
	public int getMaxNVarcharLength() {
		return 5000;
	}

	@Override
	public int getMaxVarbinaryLength() {
		return 5000;
	}

	@Override
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
	}

	@Override
	public SqlAstTranslatorFactory getSqlAstTranslatorFactory() {
		return new StandardSqlAstTranslatorFactory() {
			@Override
			protected <T extends JdbcOperation> SqlAstTranslator<T> buildTranslator(
					SessionFactoryImplementor sessionFactory, org.hibernate.sql.ast.tree.Statement statement) {
				return new HANASqlAstTranslator<>( sessionFactory, statement );
			}
		};
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
	public String extractPattern(TemporalUnit unit) {
		switch (unit) {
			case DAY_OF_WEEK:
				return "(mod(weekday(?2)+1,7)+1)";
			case DAY:
			case DAY_OF_MONTH:
				return "dayofmonth(?2)";
			case DAY_OF_YEAR:
				return "dayofyear(?2)";
			case QUARTER:
				return "((month(?2)+2)/3)";
			case EPOCH:
				return "seconds_between('1970-01-01', ?2)";
			default:
				//I think week() returns the ISO week number
				return "?1(?2)";
		}
	}

	@Override
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
	public RowLockStrategy getWriteRowLockStrategy() {
		return RowLockStrategy.COLUMN;
	}

	@Override
	public String getCreateTableString() {
		return isDefaultTableTypeColumn() ? "create column table" : "create row table";
	}

	@Override
	public String getAddColumnString() {
		return "add (";
	}

	@Override
	public String getAddColumnSuffixString() {
		return ")";
	}

	@Override
	public String getCascadeConstraintsString() {
		return " cascade";
	}

	@Override
	public String getCurrentTimestampSelectString() {
		return "select current_timestamp from sys.dummy";
	}

	@Override
	public String getForUpdateString(final String aliases) {
		return getForUpdateString() + " of " + aliases;
	}

	@Override
	public String getForUpdateString(final String aliases, final LockOptions lockOptions) {
		LockMode lockMode = lockOptions.findGreatestLockMode();
		lockOptions.setLockMode( lockMode );

		// not sure why this is sometimes empty
		if ( aliases == null || aliases.isEmpty() ) {
			return getForUpdateString( lockOptions );
		}

		return getForUpdateString( aliases, lockMode, lockOptions.getTimeOut() );
	}

	@SuppressWarnings({ "deprecation" })
	private String getForUpdateString(String aliases, LockMode lockMode, int timeout) {
		switch ( lockMode ) {
			case PESSIMISTIC_READ: {
				return getReadLockString( aliases, timeout );
			}
			case PESSIMISTIC_WRITE: {
				return getWriteLockString( aliases, timeout );
			}
			case UPGRADE_NOWAIT:
			case PESSIMISTIC_FORCE_INCREMENT: {
				return getForUpdateNowaitString( aliases );
			}
			case UPGRADE_SKIPLOCKED: {
				return getForUpdateSkipLockedString( aliases );
			}
			default: {
				return "";
			}
		}
	}

	@Override
	public String getForUpdateNowaitString() {
		return getForUpdateString() + " nowait";
	}

	@Override
	public String getQuerySequencesString() {
		return "select * from sys.sequences";
	}

	@Override
	public SequenceInformationExtractor getSequenceInformationExtractor() {
		return SequenceInformationExtractorHANADatabaseImpl.INSTANCE;
	}

	@Override
	public boolean isCurrentTimestampSelectStringCallable() {
		return false;
	}

	@Override
	protected void registerDefaultKeywords() {
		super.registerDefaultKeywords();
		// https://help.sap.com/docs/SAP_HANA_PLATFORM/4fe29514fd584807ac9f2a04f6754767/28bcd6af3eb6437892719f7c27a8a285.html?locale=en-US
		registerKeyword( "all" );
		registerKeyword( "alter" );
		registerKeyword( "as" );
		registerKeyword( "before" );
		registerKeyword( "begin" );
		registerKeyword( "both" );
		registerKeyword( "case" );
		registerKeyword( "char" );
		registerKeyword( "condition" );
		registerKeyword( "connect" );
		registerKeyword( "cross" );
		registerKeyword( "cube" );
		registerKeyword( "current_connection" );
		registerKeyword( "current_date" );
		registerKeyword( "current_schema" );
		registerKeyword( "current_time" );
		registerKeyword( "current_timestamp" );
		registerKeyword( "current_transaction_isolation_level" );
		registerKeyword( "current_user" );
		registerKeyword( "current_utcdate" );
		registerKeyword( "current_utctime" );
		registerKeyword( "current_utctimestamp" );
		registerKeyword( "currval" );
		registerKeyword( "cursor" );
		registerKeyword( "declare" );
		registerKeyword( "deferred" );
		registerKeyword( "distinct" );
		registerKeyword( "else" );
		registerKeyword( "elseif" );
		registerKeyword( "end" );
		registerKeyword( "except" );
		registerKeyword( "exception" );
		registerKeyword( "exec" );
		registerKeyword( "false" );
		registerKeyword( "for" );
		registerKeyword( "from" );
		registerKeyword( "full" );
		registerKeyword( "group" );
		registerKeyword( "having" );
		registerKeyword( "if" );
		registerKeyword( "in" );
		registerKeyword( "inner" );
		registerKeyword( "inout" );
		registerKeyword( "intersect" );
		registerKeyword( "into" );
		registerKeyword( "is" );
		registerKeyword( "join" );
		registerKeyword( "lateral" );
		registerKeyword( "leading" );
		registerKeyword( "left" );
		registerKeyword( "limit" );
		registerKeyword( "loop" );
		registerKeyword( "minus" );
		registerKeyword( "natural" );
		registerKeyword( "nchar" );
		registerKeyword( "nextval" );
		registerKeyword( "null" );
		registerKeyword( "on" );
		registerKeyword( "order" );
		registerKeyword( "out" );
		registerKeyword( "prior" );
		registerKeyword( "return" );
		registerKeyword( "returns" );
		registerKeyword( "reverse" );
		registerKeyword( "right" );
		registerKeyword( "rollup" );
		registerKeyword( "rowid" );
		registerKeyword( "select" );
		registerKeyword( "session_user" );
		registerKeyword( "set" );
		registerKeyword( "sql" );
		registerKeyword( "start" );
		registerKeyword( "sysuuid" );
		registerKeyword( "tablesample" );
		registerKeyword( "top" );
		registerKeyword( "trailing" );
		registerKeyword( "true" );
		registerKeyword( "union" );
		registerKeyword( "unknown" );
		registerKeyword( "using" );
		registerKeyword( "utctimestamp" );
		registerKeyword( "values" );
		registerKeyword( "when" );
		registerKeyword( "where" );
		registerKeyword( "while" );
		registerKeyword( "with" );
		if ( isCloud() ) {
			// https://help.sap.com/docs/hana-cloud-database/sap-hana-cloud-sap-hana-database-sql-reference-guide/reserved-words
			registerKeyword( "array" );
			registerKeyword( "at" );
			registerKeyword( "authorization" );
			registerKeyword( "between" );
			registerKeyword( "by" );
			registerKeyword( "collate" );
			registerKeyword( "empty" );
			registerKeyword( "filter" );
			registerKeyword( "grouping" );
			registerKeyword( "no" );
			registerKeyword( "not" );
			registerKeyword( "of" );
			registerKeyword( "over" );
			registerKeyword( "recursive" );
			registerKeyword( "row" );
			registerKeyword( "table" );
			registerKeyword( "to" );
			registerKeyword( "unnest" );
			registerKeyword( "window" );
			registerKeyword( "within" );
		}
	}

	@Override
	public ScrollMode defaultScrollMode() {
		return ScrollMode.FORWARD_ONLY;
	}

	/**
	 * HANA currently does not support check constraints.
	 */
	@Override
	public boolean supportsColumnCheck() {
		return false;
	}

	@Override
	public boolean supportsCurrentTimestampSelection() {
		return true;
	}

	@Override
	public boolean doesRoundTemporalOnOverflow() {
		// HANA does truncation
		return false;
	}

	@Override
	public boolean supportsExistsInSelect() {
		return false;
	}

	@Override
	public boolean supportsExpectedLobUsagePattern() {
		// http://scn.sap.com/thread/3221812
		return false;
	}

	@Override
	public boolean supportsUnboundedLobLocatorMaterialization() {
		return false;
	}

	@Override
	public SequenceSupport getSequenceSupport() {
		return HANASequenceSupport.INSTANCE;
	}

	@Override
	public boolean supportsTableCheck() {
		return true;
	}

	@Override
	public boolean supportsTupleDistinctCounts() {
		return true;
	}

	@Override
	public boolean dropConstraints() {
		return false;
	}

	@Override
	public int getMaxAliasLength() {
		return 128;
	}

	@Override
	public int getMaxIdentifierLength() {
		return 127;
	}

	@Override
	public LimitHandler getLimitHandler() {
		return LimitOffsetLimitHandler.INSTANCE;
	}

	@Override
	public String getSelectGUIDString() {
		return "select sysuuid from sys.dummy";
	}

	@Override
	public NameQualifierSupport getNameQualifierSupport() {
		return NameQualifierSupport.SCHEMA;
	}

	@Override
	public IdentifierHelper buildIdentifierHelper(IdentifierHelperBuilder builder, DatabaseMetaData dbMetaData)
			throws SQLException {
		/*
		 * HANA-specific extensions
		 */
		builder.setQuotedCaseStrategy( IdentifierCaseStrategy.MIXED );
		builder.setUnquotedCaseStrategy( IdentifierCaseStrategy.UPPER );

		final IdentifierHelper identifierHelper = super.buildIdentifierHelper( builder, dbMetaData );

		return new IdentifierHelper() {

			private final IdentifierHelper helper = identifierHelper;

			@Override
			public String toMetaDataSchemaName(Identifier schemaIdentifier) {
				return this.helper.toMetaDataSchemaName( schemaIdentifier );
			}

			@Override
			public String toMetaDataObjectName(Identifier identifier) {
				return this.helper.toMetaDataObjectName( identifier );
			}

			@Override
			public String toMetaDataCatalogName(Identifier catalogIdentifier) {
				return this.helper.toMetaDataCatalogName( catalogIdentifier );
			}

			@Override
			public Identifier toIdentifier(String text) {
				return normalizeQuoting( Identifier.toIdentifier( text ) );
			}

			@Override
			public Identifier toIdentifier(String text, boolean quoted) {
				return normalizeQuoting( Identifier.toIdentifier( text, quoted ) );
			}

			@Override
			public Identifier normalizeQuoting(Identifier identifier) {
				Identifier normalizedIdentifier = this.helper.normalizeQuoting( identifier );

				if ( normalizedIdentifier == null ) {
					return null;
				}

				// need to quote names containing special characters like ':'
				if ( !normalizedIdentifier.isQuoted() && !normalizedIdentifier.getText().matches( "\\w+" ) ) {
					normalizedIdentifier = Identifier.quote( normalizedIdentifier );
				}

				return normalizedIdentifier;
			}

			@Override
			public boolean isReservedWord(String word) {
				return this.helper.isReservedWord( word );
			}

			@Override
			public Identifier applyGlobalQuoting(String text) {
				return this.helper.applyGlobalQuoting( text );
			}
		};
	}

	@Override
	public String getCurrentSchemaCommand() {
		return "select current_schema from sys.dummy";
	}

	@Override
	public String getForUpdateNowaitString(String aliases) {
		return getForUpdateString( aliases ) + " nowait";
	}

	@Override
	public String getReadLockString(int timeout) {
		return getWriteLockString( timeout );
	}

	@Override
	public String getReadLockString(String aliases, int timeout) {
		return getWriteLockString( aliases, timeout );
	}

	@Override
	public String getWriteLockString(int timeout) {
		if ( timeout > 0 ) {
			return getForUpdateString() + " wait " + getTimeoutInSeconds( timeout );
		}
		else if ( timeout == 0 ) {
			return getForUpdateNowaitString();
		}
		else {
			return getForUpdateString();
		}
	}

	@Override
	public String getWriteLockString(String aliases, int timeout) {
		if ( timeout > 0 ) {
			return getForUpdateString( aliases ) + " wait " + getTimeoutInSeconds( timeout );
		}
		else if ( timeout == 0 ) {
			return getForUpdateNowaitString( aliases );
		}
		else {
			return getForUpdateString( aliases );
		}
	}

	@Override
	public String getQueryHintString(String query, List<String> hints) {
		return query + " with hint (" + String.join( ",", hints ) + ")";
	}

	@Override
	public String getTableComment(String comment) {
		return " comment '" + comment + "'";
	}

	@Override
	public String getColumnComment(String comment) {
		return " comment '" + comment + "'";
	}

	@Override
	public boolean supportsCommentOn() {
		return true;
	}

	@Override
	public boolean supportsPartitionBy() {
		return true;
	}

	@Override
	public void contributeTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		super.contributeTypes( typeContributions, serviceRegistry );

		final TypeConfiguration typeConfiguration = typeContributions.getTypeConfiguration();
		final JdbcTypeRegistry jdbcTypeRegistry = typeConfiguration.getJdbcTypeRegistry();
		if ( treatDoubleTypedFieldsAsDecimal ) {
			typeConfiguration.getBasicTypeRegistry()
					.register(
							new BasicTypeImpl<>( DoubleJavaType.INSTANCE, NumericJdbcType.INSTANCE ),
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
	public CallableStatementSupport getCallableStatementSupport() {
		return StandardCallableStatementSupport.REF_CURSOR_INSTANCE;
	}

	@Override
	public int registerResultSetOutParameter(CallableStatement statement, int position) throws SQLException {
		// Result set (TABLE) OUT parameters don't need to be registered
		return position;
	}

	@Override
	public int registerResultSetOutParameter(CallableStatement statement, String name) throws SQLException {
		// Result set (TABLE) OUT parameters don't need to be registered
		return 0;
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
		return getVersion().isSameOrAfter( 2, 0, 40 );
	}

	@Override
	public boolean supportsNoWait() {
		return true;
	}

	@Override
	public boolean supportsJdbcConnectionLobCreation(DatabaseMetaData databaseMetaData) {
		return false;
	}

	@Override
	public boolean supportsNoColumnsInsert() {
		return false;
	}

	@Override
	public boolean supportsOrderByInSubquery() {
		// Seems to work, though I don't know as of which version
		return true;
	}

	@Override
	public NullOrdering getNullOrdering() {
		return NullOrdering.SMALLEST;
	}

	@Override
	public void appendDatetimeFormat(SqlAppender appender, String format) {
		//I don't think HANA needs FM
		appender.appendSql( OracleDialect.datetimeFormat( format, false, false ).result() );
	}

	@Override
	public boolean supportsFractionalTimestampArithmetic() {
		return false;
	}

	@Override
	public long getFractionalSecondPrecisionInNanos() {
		return 100;
	}

	@Override
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
	public void appendDateTimeLiteral(
			SqlAppender appender,
			TemporalAccessor temporalAccessor,
			TemporalType precision,
			TimeZone jdbcTimeZone) {
		switch ( precision ) {
			case DATE:
				appender.appendSql( JDBC_ESCAPE_START_DATE );
				appendAsDate( appender, temporalAccessor );
				appender.appendSql( JDBC_ESCAPE_END );
				break;
			case TIME:
				appender.appendSql( JDBC_ESCAPE_START_TIME );
				appendAsTime( appender, temporalAccessor, supportsTemporalLiteralOffset(), jdbcTimeZone );
				appender.appendSql( JDBC_ESCAPE_END );
				break;
			case TIMESTAMP:
				appender.appendSql( JDBC_ESCAPE_START_TIMESTAMP );
				appendAsTimestampWithMicros( appender, temporalAccessor, supportsTemporalLiteralOffset(), jdbcTimeZone );
				appender.appendSql( JDBC_ESCAPE_END );
				break;
			default:
				throw new IllegalArgumentException();
		}
	}

	@Override
	public void appendDateTimeLiteral(SqlAppender appender, Date date, TemporalType precision, TimeZone jdbcTimeZone) {
		switch ( precision ) {
			case DATE:
				appender.appendSql( JDBC_ESCAPE_START_DATE );
				appendAsDate( appender, date );
				appender.appendSql( JDBC_ESCAPE_END );
				break;
			case TIME:
				appender.appendSql( JDBC_ESCAPE_START_TIME );
				appendAsTime( appender, date );
				appender.appendSql( JDBC_ESCAPE_END );
				break;
			case TIMESTAMP:
				appender.appendSql( JDBC_ESCAPE_START_TIMESTAMP );
				appendAsTimestampWithMicros( appender, date, jdbcTimeZone );
				appender.appendSql( JDBC_ESCAPE_END );
				break;
			default:
				throw new IllegalArgumentException();
		}
	}

	@Override
	public String generatedAs(String generatedAs) {
		return " generated always as (" + generatedAs + ")";
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
			return this.data.indexOf( DataHelper.extractString( searchstr ), (int) ( start - 1 ) );
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
			return new BasicExtractor<>( javaType, this ) {
				private X extract(Blob blob, WrapperOptions options) throws SQLException {
					if ( blob == null ) {
						return null;
					}
					if ( blob.length() < HANAStreamBlobType.this.maxLobPrefetchSize ) {
						X result = javaType.wrap( blob, options );
						blob.free();
						return result;
					}
					Blob materializedBlob = new MaterializedBlob( DataHelper.extractBytes( blob.getBinaryStream() ) );
					blob.free();
					return javaType.wrap( materializedBlob, options );
				}

				@Override
				protected X doExtract(ResultSet rs, int paramIndex, WrapperOptions options) throws SQLException {
					return extract( rs.getBlob( paramIndex ), options );
				}

				@Override
				protected X doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
					return extract( statement.getBlob( index ), options );
				}

				@Override
				protected X doExtract(CallableStatement statement, String name, WrapperOptions options) throws SQLException {
					return extract( statement.getBlob( name ), options );
				}
			};
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
				private X extract(Clob clob, WrapperOptions options) throws SQLException {
					if ( clob == null ) {
						return null;
					}

					if ( clob.length() < HANAClobJdbcType.this.maxLobPrefetchSize ) {
						X retVal = javaType.wrap(clob, options);
						clob.free();
						return retVal;
					}
					NClob materializedNClob = new MaterializedNClob( DataHelper.extractString( clob ) );
					clob.free();
					return javaType.wrap( materializedNClob, options );
				}

				@Override
				protected X doExtract(ResultSet rs, int paramIndex, WrapperOptions options) throws SQLException {
					Clob rsClob;
					if ( HANAClobJdbcType.this.useUnicodeStringTypes ) {
						rsClob = rs.getNClob( paramIndex );
					}
					else {
						rsClob = rs.getClob( paramIndex );
					}
					return extract( rsClob, options );
				}

				@Override
				protected X doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
					Clob rsClob;
					if ( HANAClobJdbcType.this.useUnicodeStringTypes ) {
						rsClob = statement.getNClob( index );
					}
					else {
						rsClob = statement.getClob( index );
					}
					return extract( rsClob, options );
				}

				@Override
				protected X doExtract(CallableStatement statement, String name, WrapperOptions options) throws SQLException {
					Clob rsClob;
					if ( HANAClobJdbcType.this.useUnicodeStringTypes ) {
						rsClob = statement.getNClob( name );
					}
					else {
						rsClob = statement.getClob( name );
					}
					return extract( rsClob, options );
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
				private X extract(NClob nclob, WrapperOptions options) throws SQLException {
					if ( nclob == null ) {
						return null;
					}
					if ( nclob.length() < maxLobPrefetchSize ) {
						X retVal = javaType.wrap(nclob, options);
						nclob.free();
						return retVal;
					}
					NClob materializedNClob = new MaterializedNClob( DataHelper.extractString( nclob ) );
					nclob.free();
					return javaType.wrap( materializedNClob, options );
				}
				@Override
				protected X doExtract(ResultSet rs, int paramIndex, WrapperOptions options) throws SQLException {
					return extract( rs.getNClob( paramIndex ), options );
				}

				@Override
				protected X doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
					return extract( statement.getNClob( index ), options );
				}

				@Override
				protected X doExtract(CallableStatement statement, String name, WrapperOptions options) throws SQLException {
					return extract( statement.getNClob( name ), options );
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
			return new BasicExtractor<>( javaType, this ) {
				private X extract(Blob blob, WrapperOptions options) throws SQLException {
					if ( blob == null ) {
						return null;
					}
					if ( blob.length() < maxLobPrefetchSize ) {
						X retVal = javaType.wrap(blob, options);
						blob.free();
						return retVal;
					}
					Blob materializedBlob = new MaterializedBlob( DataHelper.extractBytes( blob.getBinaryStream() ) );
					blob.free();
					return javaType.wrap( materializedBlob, options );
				}
				@Override
				protected X doExtract(ResultSet rs, int paramIndex, WrapperOptions options) throws SQLException {
					return extract( rs.getBlob( paramIndex ) , options );
				}

				@Override
				protected X doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
					return extract( statement.getBlob( index ), options );
				}

				@Override
				protected X doExtract(CallableStatement statement, String name, WrapperOptions options) throws SQLException {
					return extract( statement.getBlob( name ), options );
				}
			};
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
	public SqmMultiTableMutationStrategy getFallbackSqmMutationStrategy(
			EntityMappingType entityDescriptor,
			RuntimeModelCreationContext runtimeModelCreationContext) {
		return new GlobalTemporaryTableMutationStrategy(
				TemporaryTable.createIdTable(
						entityDescriptor,
						basename -> TemporaryTable.ID_TABLE_PREFIX + basename,
						this,
						runtimeModelCreationContext
				),
				runtimeModelCreationContext.getSessionFactory()
		);
	}

	@Override
	public SqmMultiTableInsertStrategy getFallbackSqmInsertStrategy(
			EntityMappingType entityDescriptor,
			RuntimeModelCreationContext runtimeModelCreationContext) {
		return new GlobalTemporaryTableInsertStrategy(
				TemporaryTable.createEntityTable(
						entityDescriptor,
						name -> TemporaryTable.ENTITY_TABLE_PREFIX + name,
						this,
						runtimeModelCreationContext
				),
				runtimeModelCreationContext.getSessionFactory()
		);
	}

	@Override
	public TemporaryTableKind getSupportedTemporaryTableKind() {
		return TemporaryTableKind.GLOBAL;
	}

	@Override
	public String getTemporaryTableCreateOptions() {
		return "on commit delete rows";
	}

	@Override
	public String getTemporaryTableCreateCommand() {
		return "create global temporary row table";
	}

	@Override
	public String getTemporaryTableTruncateCommand() {
		return "truncate table";
	}

	@Override
	public DmlTargetColumnQualifierSupport getDmlTargetColumnQualifierSupport() {
		return DmlTargetColumnQualifierSupport.TABLE_ALIAS;
	}

	@Override
	public boolean supportsSkipLocked() {
		// HANA supports IGNORE LOCKED since HANA 2.0 SPS3 (2.0.030)
		return getVersion().isSameOrAfter(2, 0, 30);
	}

	@Override
	public String getForUpdateSkipLockedString() {
		return supportsSkipLocked() ? getForUpdateString() + SQL_IGNORE_LOCKED : getForUpdateString();
	}

	@Override
	public String getForUpdateSkipLockedString(String aliases) {
		return supportsSkipLocked() ?
				getForUpdateString(aliases) + SQL_IGNORE_LOCKED : getForUpdateString(aliases);
	}

	@Override
	public String getForUpdateString(LockMode lockMode) {
		return super.getForUpdateString(lockMode);
	}
}
