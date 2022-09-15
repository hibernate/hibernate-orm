/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.temporal.TemporalAccessor;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Pattern;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.ScrollMode;
import org.hibernate.boot.TempTableDdlTransactionHandling;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.boot.model.relational.AuxiliaryDatabaseObject;
import org.hibernate.boot.model.relational.Sequence;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.function.CastFunction;
import org.hibernate.dialect.function.CastStrEmulation;
import org.hibernate.dialect.function.CoalesceIfnullEmulation;
import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.dialect.function.CurrentFunction;
import org.hibernate.dialect.function.ExtractFunction;
import org.hibernate.dialect.function.InsertSubstringOverlayEmulation;
import org.hibernate.dialect.function.LocatePositionEmulation;
import org.hibernate.dialect.function.LpadRpadPadEmulation;
import org.hibernate.dialect.function.SqlFunction;
import org.hibernate.dialect.function.TimestampaddFunction;
import org.hibernate.dialect.function.TimestampdiffFunction;
import org.hibernate.dialect.function.TrimFunction;
import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.dialect.identity.IdentityColumnSupportImpl;
import org.hibernate.dialect.lock.LockingStrategy;
import org.hibernate.dialect.lock.OptimisticForceIncrementLockingStrategy;
import org.hibernate.dialect.lock.OptimisticLockingStrategy;
import org.hibernate.dialect.lock.PessimisticForceIncrementLockingStrategy;
import org.hibernate.dialect.lock.PessimisticReadSelectLockingStrategy;
import org.hibernate.dialect.lock.PessimisticWriteSelectLockingStrategy;
import org.hibernate.dialect.lock.SelectLockingStrategy;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.dialect.sequence.NoSequenceSupport;
import org.hibernate.dialect.sequence.SequenceSupport;
import org.hibernate.dialect.temptable.StandardTemporaryTableExporter;
import org.hibernate.dialect.temptable.TemporaryTable;
import org.hibernate.dialect.temptable.TemporaryTableExporter;
import org.hibernate.dialect.temptable.TemporaryTableKind;
import org.hibernate.dialect.unique.DefaultUniqueDelegate;
import org.hibernate.dialect.unique.UniqueDelegate;
import org.hibernate.engine.jdbc.LobCreator;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.jdbc.env.internal.DefaultSchemaNameResolver;
import org.hibernate.engine.jdbc.env.spi.AnsiSqlKeywords;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelperBuilder;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.env.spi.NameQualifierSupport;
import org.hibernate.engine.jdbc.env.spi.SchemaNameResolver;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.exception.spi.ConversionContext;
import org.hibernate.exception.spi.SQLExceptionConversionDelegate;
import org.hibernate.exception.spi.SQLExceptionConverter;
import org.hibernate.exception.spi.ViolatedConstraintNameExtractor;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.MathHelper;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.internal.util.io.StreamCopier;
import org.hibernate.loader.BatchLoadSizingStrategy;
import org.hibernate.mapping.Constraint;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.Index;
import org.hibernate.mapping.Table;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.persister.entity.Lockable;
import org.hibernate.procedure.internal.StandardCallableStatementSupport;
import org.hibernate.procedure.spi.CallableStatementSupport;
import org.hibernate.query.Query;
import org.hibernate.query.hql.HqlTranslator;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.sqm.CastType;
import org.hibernate.query.sqm.FetchClauseType;
import org.hibernate.query.sqm.IntervalType;
import org.hibernate.query.sqm.NullOrdering;
import org.hibernate.query.sqm.TemporalUnit;
import org.hibernate.query.sqm.TrimSpec;
import org.hibernate.query.sqm.mutation.internal.temptable.AfterUseAction;
import org.hibernate.query.sqm.mutation.internal.temptable.BeforeUseAction;
import org.hibernate.query.sqm.mutation.internal.temptable.PersistentTableInsertStrategy;
import org.hibernate.query.sqm.mutation.internal.temptable.PersistentTableMutationStrategy;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableInsertStrategy;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableMutationStrategy;
import org.hibernate.query.sqm.produce.function.StandardFunctions;
import org.hibernate.query.sqm.sql.SqmTranslatorFactory;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.sql.ForUpdateFragment;
import org.hibernate.sql.ast.SqlAstNodeRenderingMode;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.spi.StandardSqlAstTranslatorFactory;
import org.hibernate.sql.ast.spi.StringBuilderSqlAppender;
import org.hibernate.tool.schema.extract.internal.SequenceInformationExtractorLegacyImpl;
import org.hibernate.tool.schema.extract.internal.SequenceInformationExtractorNoOpImpl;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractor;
import org.hibernate.tool.schema.internal.StandardAuxiliaryDatabaseObjectExporter;
import org.hibernate.tool.schema.internal.StandardForeignKeyExporter;
import org.hibernate.tool.schema.internal.StandardIndexExporter;
import org.hibernate.tool.schema.internal.StandardSequenceExporter;
import org.hibernate.tool.schema.internal.StandardTableExporter;
import org.hibernate.tool.schema.internal.StandardUniqueKeyExporter;
import org.hibernate.tool.schema.spi.Exporter;
import org.hibernate.type.BasicType;
import org.hibernate.type.BasicTypeRegistry;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.PrimitiveByteArrayJavaType;
import org.hibernate.type.descriptor.jdbc.ArrayJdbcType;
import org.hibernate.type.descriptor.jdbc.ClobJdbcType;
import org.hibernate.type.descriptor.jdbc.InstantAsTimestampJdbcType;
import org.hibernate.type.descriptor.jdbc.InstantAsTimestampWithTimeZoneJdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcLiteralFormatter;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.LongNVarcharJdbcType;
import org.hibernate.type.descriptor.jdbc.NCharJdbcType;
import org.hibernate.type.descriptor.jdbc.NClobJdbcType;
import org.hibernate.type.descriptor.jdbc.NVarcharJdbcType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;
import org.hibernate.type.descriptor.sql.internal.CapacityDependentDdlType;
import org.hibernate.type.descriptor.sql.internal.DdlTypeImpl;
import org.hibernate.type.descriptor.sql.spi.DdlTypeRegistry;
import org.hibernate.type.spi.TypeConfiguration;

import org.jboss.logging.Logger;

import jakarta.persistence.TemporalType;

import static java.lang.Math.ceil;
import static java.lang.Math.log;
import static org.hibernate.internal.util.StringHelper.parseCommaSeparatedString;
import static org.hibernate.type.SqlTypes.ARRAY;
import static org.hibernate.type.SqlTypes.BIGINT;
import static org.hibernate.type.SqlTypes.BINARY;
import static org.hibernate.type.SqlTypes.BLOB;
import static org.hibernate.type.SqlTypes.BOOLEAN;
import static org.hibernate.type.SqlTypes.CHAR;
import static org.hibernate.type.SqlTypes.CLOB;
import static org.hibernate.type.SqlTypes.DATE;
import static org.hibernate.type.SqlTypes.DECIMAL;
import static org.hibernate.type.SqlTypes.DOUBLE;
import static org.hibernate.type.SqlTypes.FLOAT;
import static org.hibernate.type.SqlTypes.INTEGER;
import static org.hibernate.type.SqlTypes.LONG32NVARCHAR;
import static org.hibernate.type.SqlTypes.LONG32VARBINARY;
import static org.hibernate.type.SqlTypes.LONG32VARCHAR;
import static org.hibernate.type.SqlTypes.NCHAR;
import static org.hibernate.type.SqlTypes.NCLOB;
import static org.hibernate.type.SqlTypes.NUMERIC;
import static org.hibernate.type.SqlTypes.NVARCHAR;
import static org.hibernate.type.SqlTypes.REAL;
import static org.hibernate.type.SqlTypes.SMALLINT;
import static org.hibernate.type.SqlTypes.TIME;
import static org.hibernate.type.SqlTypes.TIMESTAMP;
import static org.hibernate.type.SqlTypes.TIMESTAMP_UTC;
import static org.hibernate.type.SqlTypes.TIMESTAMP_WITH_TIMEZONE;
import static org.hibernate.type.SqlTypes.TIME_WITH_TIMEZONE;
import static org.hibernate.type.SqlTypes.TINYINT;
import static org.hibernate.type.SqlTypes.VARBINARY;
import static org.hibernate.type.SqlTypes.VARCHAR;
import static org.hibernate.type.SqlTypes.isCharacterType;
import static org.hibernate.type.SqlTypes.isFloatOrRealOrDouble;
import static org.hibernate.type.SqlTypes.isIntegral;
import static org.hibernate.type.SqlTypes.isNumericOrDecimal;
import static org.hibernate.type.SqlTypes.isNumericType;
import static org.hibernate.type.SqlTypes.isVarbinaryType;
import static org.hibernate.type.SqlTypes.isVarcharType;
import static org.hibernate.type.descriptor.DateTimeUtils.JDBC_ESCAPE_END;
import static org.hibernate.type.descriptor.DateTimeUtils.JDBC_ESCAPE_START_DATE;
import static org.hibernate.type.descriptor.DateTimeUtils.JDBC_ESCAPE_START_TIME;
import static org.hibernate.type.descriptor.DateTimeUtils.JDBC_ESCAPE_START_TIMESTAMP;
import static org.hibernate.type.descriptor.DateTimeUtils.appendAsDate;
import static org.hibernate.type.descriptor.DateTimeUtils.appendAsTime;
import static org.hibernate.type.descriptor.DateTimeUtils.appendAsTimestampWithMicros;

/**
 * Represents a dialect of SQL implemented by a particular RDBMS. Every
 * subclass of this class implements support for a certain database
 * platform. For example, {@link PostgreSQLDialect} implements support
 * for PostgreSQL, and {@link MySQLDialect} implements support for MySQL.
 * <p>
 * A subclass must provide a public constructor with a single parameter
 * of type {@link DialectResolutionInfo}. Alternatively, for purposes of
 * backward compatibility with older versions of Hibernate, a constructor
 * with no parameters is also allowed.
 * <p>
 * Almost every subclass must, as a bare minimum, override at least:
 * <ul>
 *     <li>{@link #registerColumnTypes(TypeContributions, ServiceRegistry)} to define a mapping from SQL
 *     {@linkplain SqlTypes type codes} to database column types, and
 *     <li>{@link #initializeFunctionRegistry(QueryEngine)} to register
 *     mappings for standard HQL functions with the
 *     {@link org.hibernate.query.sqm.function.SqmFunctionRegistry}.
 * </ul>
 * A subclass representing a dialect of SQL which deviates significantly
 * from ANSI SQL will certainly override many additional operations.
 * <p>
 * Subclasses should be threadsafe and immutable.
 * <p>
 * Since Hibernate 6, a single subclass of {@code Dialect} represents all
 * releases of a given product-specific SQL dialect. The version of the
 * database is exposed at runtime via the {@link DialectResolutionInfo}
 * passed to the constructor, and by the {@link #getVersion()} property.
 * <p>
 * Programs using Hibernate should migrate away from the use of versioned
 * dialect classes like, for example, {@link PostgreSQL95Dialect}. These
 * classes are now deprecated and will be removed in a future release.
 * <p>
 * A custom {@code Dialect} may be specified using the configuration
 * property {@value org.hibernate.cfg.AvailableSettings#DIALECT}, but
 * for supported databases this property is unnecessary, and Hibernate
 * will select the correct {@code Dialect} based on the JDBC URL and
 * {@link DialectResolutionInfo}.
 *
 * @author Gavin King, David Channon
 */
public abstract class Dialect implements ConversionContext {

	/**
	 * Characters used as opening for quoting SQL identifiers
	 */
	public static final String QUOTE = "`\"[";

	/**
	 * Characters used as closing for quoting SQL identifiers
	 */
	public static final String CLOSED_QUOTE = "`\"]";

	private static final Pattern ESCAPE_CLOSING_COMMENT_PATTERN = Pattern.compile( "\\*/" );
	private static final Pattern ESCAPE_OPENING_COMMENT_PATTERN = Pattern.compile( "/\\*" );

	private static final CoreMessageLogger LOG = Logger.getMessageLogger( CoreMessageLogger.class, Dialect.class.getName() );

	//needed for converting precision from decimal to binary digits
	protected static final double LOG_BASE2OF10 = log(10)/log(2);

	private final Properties properties = new Properties();
	private final Set<String> sqlKeywords = new HashSet<>();

	private final UniqueDelegate uniqueDelegate = new DefaultUniqueDelegate( this );

	private final SizeStrategy sizeStrategy = new SizeStrategyImpl();

	private final DatabaseVersion version;

	// constructors and factory methods ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * @deprecated provide a {@link DatabaseVersion}
	 */
	@Deprecated(since = "6.0")
	protected Dialect() {
		this( (DatabaseVersion) null );
	}

	protected Dialect(DatabaseVersion version) {
		this.version = version;
		checkVersion();
		registerDefaultKeywords();
		initDefaultProperties();
	}

	protected Dialect(DialectResolutionInfo info) {
		this.version = info.makeCopy();
		checkVersion();
		registerDefaultKeywords();
		registerKeywords(info);
		initDefaultProperties();
	}

	protected void checkVersion() {
		final DatabaseVersion version = getVersion();
		final DatabaseVersion minimumVersion = getMinimumSupportedVersion();
		if ( version != null && version.isBefore( minimumVersion.getMajor(), minimumVersion.getMinor(), minimumVersion.getMicro() ) ) {
			LOG.unsupportedDatabaseVersion(
					getClass().getName(),
					version.getMajor() + "." + version.getMinor() + "." + version.getMicro(),
					minimumVersion.getMajor() + "." + minimumVersion.getMinor() + "." + minimumVersion.getMicro()
			);
		}
	}

	/**
	 * Set appropriate default values for configuration properties.
	 */
	protected void initDefaultProperties() {
		getDefaultProperties().setProperty( Environment.STATEMENT_BATCH_SIZE,
				Integer.toString( getDefaultStatementBatchSize() ) );
		getDefaultProperties().setProperty( Environment.NON_CONTEXTUAL_LOB_CREATION,
				Boolean.toString( getDefaultNonContextualLobCreation() ) );
		getDefaultProperties().setProperty( Environment.USE_GET_GENERATED_KEYS,
				Boolean.toString( getDefaultUseGetGeneratedKeys() )  );
	}

	/**
	 * Register ANSI-standard column types using the length limits defined
	 * by {@link #getMaxVarcharLength()}, {@link #getMaxNVarcharLength()},
	 * and {@link #getMaxVarbinaryLength()}.
	 * <p>
	 * This method is always called when a {@code Dialect} is instantiated.
	 */
	protected void registerColumnTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		final DdlTypeRegistry ddlTypeRegistry = typeContributions.getTypeConfiguration().getDdlTypeRegistry();

		ddlTypeRegistry.addDescriptor( simpleSqlType( BOOLEAN ) );

		ddlTypeRegistry.addDescriptor( simpleSqlType( TINYINT ) );
		ddlTypeRegistry.addDescriptor( simpleSqlType( SMALLINT ) );
		ddlTypeRegistry.addDescriptor( simpleSqlType( INTEGER ) );
		ddlTypeRegistry.addDescriptor( simpleSqlType( BIGINT ) );

		ddlTypeRegistry.addDescriptor( simpleSqlType( FLOAT ) );
		ddlTypeRegistry.addDescriptor( simpleSqlType( REAL ) );
		ddlTypeRegistry.addDescriptor( simpleSqlType( DOUBLE ) );

		ddlTypeRegistry.addDescriptor( simpleSqlType( NUMERIC ) );
		ddlTypeRegistry.addDescriptor( simpleSqlType( DECIMAL ) );

		ddlTypeRegistry.addDescriptor( simpleSqlType( DATE ) );
		ddlTypeRegistry.addDescriptor( simpleSqlType( TIME ) );
		ddlTypeRegistry.addDescriptor( simpleSqlType( TIME_WITH_TIMEZONE ) );
		ddlTypeRegistry.addDescriptor( simpleSqlType( TIMESTAMP ) );
		ddlTypeRegistry.addDescriptor( simpleSqlType( TIMESTAMP_WITH_TIMEZONE ) );
		ddlTypeRegistry.addDescriptor( simpleSqlType( TIMESTAMP_UTC ) );

		ddlTypeRegistry.addDescriptor( simpleSqlType( CHAR ) );
		ddlTypeRegistry.addDescriptor(
				sqlTypeBuilder( VARCHAR, LONG32VARCHAR, VARCHAR )
						.withTypeCapacity( getMaxVarcharLength(), columnType( VARCHAR ) )
						.build()
		);
		ddlTypeRegistry.addDescriptor( simpleSqlType( CLOB ) );

		ddlTypeRegistry.addDescriptor( simpleSqlType( NCHAR ) );
		ddlTypeRegistry.addDescriptor(
				sqlTypeBuilder( NVARCHAR, LONG32NVARCHAR, NVARCHAR )
						.withTypeCapacity( getMaxNVarcharLength(), columnType( NVARCHAR ) )
						.build()
		);
		ddlTypeRegistry.addDescriptor( simpleSqlType( NCLOB ) );

		ddlTypeRegistry.addDescriptor( simpleSqlType( BINARY ) );
		ddlTypeRegistry.addDescriptor(
				sqlTypeBuilder( VARBINARY, LONG32VARBINARY, VARBINARY )
						.withTypeCapacity( getMaxVarbinaryLength(), columnType( VARBINARY ) )
						.build()
		);
		ddlTypeRegistry.addDescriptor( simpleSqlType( BLOB ) );

		// by default use the LOB mappings for the "long" types
		ddlTypeRegistry.addDescriptor( simpleSqlType( LONG32VARCHAR ) );
		ddlTypeRegistry.addDescriptor( simpleSqlType( LONG32NVARCHAR ) );
		ddlTypeRegistry.addDescriptor( simpleSqlType( LONG32VARBINARY ) );
	}

	private DdlTypeImpl simpleSqlType(int sqlTypeCode) {
		return new DdlTypeImpl( sqlTypeCode, columnType( sqlTypeCode ), castType( sqlTypeCode ), this );
	}

	private CapacityDependentDdlType.Builder sqlTypeBuilder(int sqlTypeCode, int biggestSqlTypeCode, int castTypeCode) {
		return CapacityDependentDdlType.builder(
				sqlTypeCode,
				columnType( biggestSqlTypeCode ),
				castType( castTypeCode ),
				this
		);
	}

	/**
	 * The column type name for a given JDBC type code defined in {@link Types} or
	 * {@link SqlTypes}. This default implementation returns the ANSI-standard type
	 * name.
	 * <p>
	 * This method may be overridden by concrete {@code Dialect}s as an alternative
	 * to {@link #registerColumnTypes(TypeContributions, ServiceRegistry)} for simple registrations.
	 *
	 * @param sqlTypeCode a SQL type code
	 * @return a column type name, with $l, $p, $s placeholders for length, precision, scale
	 *
	 * @see SqlTypes
	 */
	protected String columnType(int sqlTypeCode) {
		switch ( sqlTypeCode ) {
			case BOOLEAN:
				return "boolean";

			case TINYINT:
				return "tinyint";
			case SMALLINT:
				return "smallint";
			case INTEGER:
				return "integer";
			case BIGINT:
				return "bigint";

			case FLOAT:
				// this is the floating point type we prefer!
				return "float($p)";
			case REAL:
				// this type has very unclear semantics in ANSI SQL,
				// so we avoid it and prefer float with an explicit
				// precision
				return "real";
			case DOUBLE:
				// this is just a more verbose way to write float(19)
				return "double precision";

			// these are pretty much synonyms, but are considered
			// separate types by the ANSI spec, and in some dialects
			case NUMERIC:
				return "numeric($p,$s)";
			case DECIMAL:
				return "decimal($p,$s)";

			case DATE:
				return "date";
			case TIME:
				return "time";
			case TIME_WITH_TIMEZONE:
				// type included here for completeness but note that
				// very few databases support it, and the general
				// advice is to caution against its use (for reasons,
				// check the comments in the Postgres documentation).
				return "time with time zone";
			case TIMESTAMP:
				return "timestamp($p)";
			case TIMESTAMP_WITH_TIMEZONE:
				return "timestamp($p) with time zone";
			case TIMESTAMP_UTC:
				return getTimeZoneSupport() == TimeZoneSupport.NATIVE
						? columnType( TIMESTAMP_WITH_TIMEZONE )
						: columnType( TIMESTAMP );

			case CHAR:
				return "char($l)";
			case VARCHAR:
				return "varchar($l)";
			case CLOB:
				return "clob";

			case NCHAR:
				return "nchar($l)";
			case NVARCHAR:
				return "nvarchar($l)";
			case NCLOB:
				return "nclob";

			case BINARY:
				return "binary($l)";
			case VARBINARY:
				return "varbinary($l)";
			case BLOB:
				return "blob";

			// by default use the LOB mappings for the "long" types
			case LONG32VARCHAR:
				return columnType( CLOB );
			case LONG32NVARCHAR:
				return columnType( NCLOB );
			case LONG32VARBINARY:
				return columnType( BLOB );

			default:
				throw new IllegalArgumentException( "unknown type: " + sqlTypeCode );
		}
	}

	protected String castType(int sqlTypeCode) {
		return columnType( sqlTypeCode );
	}

	protected void registerDefaultKeywords() {
		for ( String keyword : AnsiSqlKeywords.INSTANCE.sql2003() ) {
			registerKeyword( keyword );
		}
	}

	protected void registerKeywords(DialectResolutionInfo info) {
		for ( String keyword : parseCommaSeparatedString( info.getSQLKeywords() ) ) {
			registerKeyword( keyword );
		}
	}

	public DatabaseVersion getVersion() {
		return version;
	}

	protected DatabaseVersion getMinimumSupportedVersion() {
		return SimpleDatabaseVersion.ZERO_VERSION;
	}

	/**
	 * Resolves the {@link SqlTypes} type code for the given column type name as reported by the database,
	 * or <code>null</code> if it can't be resolved.
	 */
	protected Integer resolveSqlTypeCode(String columnTypeName, TypeConfiguration typeConfiguration) {
		final int parenthesisIndex = columnTypeName.lastIndexOf( '(' );
		final String baseTypeName;
		if ( parenthesisIndex == -1 ) {
			baseTypeName = columnTypeName;
		}
		else {
			baseTypeName = columnTypeName.substring( 0, parenthesisIndex ).trim();
		}
		return resolveSqlTypeCode( columnTypeName, baseTypeName, typeConfiguration );
	}

	/**
	 * Resolves the {@link SqlTypes} type code for the given column type name as reported by the database
	 * and the base type name (i.e. without precision/length and scale), or <code>null</code> if it can't be resolved.
	 */
	protected Integer resolveSqlTypeCode(String typeName, String baseTypeName, TypeConfiguration typeConfiguration) {
		return typeConfiguration.getDdlTypeRegistry().getSqlTypeCode( baseTypeName );
	}

	public JdbcType resolveSqlTypeDescriptor(
			String columnTypeName,
			int jdbcTypeCode,
			int precision,
			int scale,
			JdbcTypeRegistry jdbcTypeRegistry) {
		final JdbcType jdbcType = jdbcTypeRegistry.getDescriptor( jdbcTypeCode );
		if ( jdbcTypeCode == Types.ARRAY && jdbcType instanceof ArrayJdbcType ) {
			// Special handling for array types, because we need the proper element/component type
			// To determine the element JdbcType, we pass the database reported type to #resolveSqlTypeCode
			final int arraySuffixIndex = columnTypeName.toLowerCase( Locale.ROOT ).indexOf( " array" );
			if ( arraySuffixIndex != -1 ) {
				final String componentTypeName = columnTypeName.substring( 0, arraySuffixIndex );
				final Integer sqlTypeCode = resolveSqlTypeCode( componentTypeName, jdbcTypeRegistry.getTypeConfiguration() );
				if ( sqlTypeCode != null ) {
					return ( (ArrayJdbcType) jdbcType ).resolveType(
							jdbcTypeRegistry.getTypeConfiguration(),
							jdbcTypeRegistry.getTypeConfiguration().getServiceRegistry()
									.getService( JdbcServices.class )
									.getDialect(),
							jdbcTypeRegistry.getDescriptor( sqlTypeCode ),
							null
					);
				}
			}
		}
		return jdbcType;
	}

	public int resolveSqlTypeLength(
			String columnTypeName,
			int jdbcTypeCode,
			int precision,
			int scale,
			int displaySize) {
		// It seems MariaDB/MySQL return the precision in bytes depending on the charset,
		// so to detect whether we have a single character here, we check the display size
		if ( jdbcTypeCode == Types.CHAR && precision <= 4 ) {
			return displaySize;
		}
		else {
			return precision;
		}
	}

	/**
	 * Render a SQL check condition for a column that represents a boolean value.
	 */
	public String getBooleanCheckCondition(String columnName, int sqlType, char falseChar, char trueChar) {
		if ( isCharacterType(sqlType) ) {
			return columnName + " in ('" + falseChar + "','" + trueChar + "')";
		}
		else if ( isNumericType(sqlType) && !supportsBitType() ) {
			return columnName + " in (0,1)";
		}
		else {
			return null;
		}
	}

	/**
	 * Render a SQL check condition for a column that represents an enumerated value.
	 */
	public String getEnumCheckCondition(String columnName, int sqlType, Class<? extends Enum<?>> enumClass) {
		if ( isCharacterType(sqlType) ) {
			StringBuilder check = new StringBuilder();
			check.append( columnName ).append( " in (" );
			String separator = "";
			for ( Enum<?> value : enumClass.getEnumConstants() ) {
				check.append( separator ).append('\'').append( value.name() ).append('\'');
				separator = ",";
			}
			return check.append( ')' ).toString();
		}
		else if ( isNumericType(sqlType) ) {
			int last = enumClass.getEnumConstants().length - 1;
			return columnName + " between 0 and " + last;
		}
		else {
			return null;
		}
	}

	/**
	 * Initialize the given registry with any dialect-specific functions.
	 * <p>
	 * Support for certain SQL functions is required, and if the database
	 * does not support a required function, then the dialect must define
	 * a way to emulate it.
	 * <p>
	 * These required functions include the functions defined by the JPA
	 * query language specification:
	 *
	 * <ul>
	 * <li> avg(arg)							- aggregate function
	 * <li> count([distinct ]arg)				- aggregate function
	 * <li> max(arg)							- aggregate function
	 * <li> min(arg)							- aggregate function
	 * <li> sum(arg)							- aggregate function
	 * </ul>
	 *
	 * <ul>
	 * <li> coalesce(arg0, arg1, ...)
	 * <li> nullif(arg0, arg1)
	 * </ul>
	 *
	 * <ul>
	 * <li> lower(arg)
	 * <li> upper(arg)
	 * <li> length(arg)
	 * <li> concat(arg0, arg1, ...)
	 * <li> locate(pattern, string[, start])
	 * <li> substring(string, start[, length])
	 * <li> trim([[spec ][character ]from] string)
	 * </ul>
	 *
	 * <ul>
	 * <li> abs(arg)
	 * <li> mod(arg0, arg1)
	 * <li> sqrt(arg)
	 * </ul>
	 *
	 * <ul>
	 * <li> current date
	 * <li> current time
	 * <li> current timestamp
	 * </ul>
	 *
	 * Along with an additional set of functions defined by ANSI SQL:
	 *
	 * <ul>
	 * <li> any(arg)							- aggregate function
	 * <li> every(arg)							- aggregate function
	 * </ul>
	 *
	 * <ul>
	 * <li> cast(arg as Type)
	 * <li> extract(field from arg)
	 * </ul>
	 *
	 * <ul>
	 * <li> ln(arg)
	 * <li> exp(arg)
	 * <li> power(arg0, arg1)
	 * <li> floor(arg)
	 * <li> ceiling(arg)
	 * </ul>
	 *
	 * <ul>
	 * <li> position(pattern in string)
	 * <li> substring(string from start[ for length])
	 * <li> overlay(string placing replacement from start[ for length])
	 * </ul>
	 *
	 * And the following functions for working with java.time types:
	 *
	 * <ul>
	 * <li> local date
	 * <li> local time
	 * <li> local datetime
	 * <li> offset datetime
	 * <li> instant
	 * </ul>
	 *
	 * And a number of additional "standard" functions:
	 *
	 * <ul>
	 * <li> left(string, length)
	 * <li> right(string, length)
	 * <li> replace(string, pattern, replacement)
	 * <li> pad(string with length spec[ character])
	 * </ul>
	 *
	 * <ul>
	 * <li> log10(arg)
	 * <li> sign(arg)
	 * <li> sin(arg)
	 * <li> cos(arg)
	 * <li> tan(arg)
	 * <li> asin(arg)
	 * <li> acos(arg)
	 * <li> atan(arg)
	 * <li> atan2(arg0, arg1)
	 * <li> round(arg0, arg1)
	 * <li> least(arg0, arg1, ...)
	 * <li> greatest(arg0, arg1, ...)
	 * </ul>
	 *
	 * <ul>
	 * <li> format(datetime as pattern)
	 * <li> collate(string as collation)
	 * <li> str(arg)					- synonym of cast(a as String)
	 * <li> ifnull(arg0, arg1)			- synonym of coalesce(a, b)
	 * </ul>
	 *
	 * Finally, the following functions are defined as abbreviations
	 * for extract(), and desugared by the parser:
	 *
	 * <ul>
	 * <li> second(arg)					- synonym of extract(second from a)
	 * <li> minute(arg)					- synonym of extract(minute from a)
	 * <li> hour(arg)					- synonym of extract(hour from a)
	 * <li> day(arg)					- synonym of extract(day from a)
	 * <li> month(arg)					- synonym of extract(month from a)
	 * <li> year(arg)					- synonym of extract(year from a)
	 * </ul>
	 *
	 * Note that according to this definition, the second() function returns
	 * a floating point value, contrary to the integer type returned by the
	 * native function with this name on many databases. Thus, we don't just
	 * naively map these HQL functions to the native SQL functions with the
	 * same names.
	 */
	public void initializeFunctionRegistry(QueryEngine queryEngine) {
		final TypeConfiguration typeConfiguration = queryEngine.getTypeConfiguration();
		final BasicTypeRegistry basicTypeRegistry = typeConfiguration.getBasicTypeRegistry();
		final BasicType<Date> timestampType = basicTypeRegistry.resolve( StandardBasicTypes.TIMESTAMP );
		final BasicType<Date> dateType = basicTypeRegistry.resolve( StandardBasicTypes.DATE );
		final BasicType<Date> timeType = basicTypeRegistry.resolve( StandardBasicTypes.TIME );
		final BasicType<Instant> instantType = basicTypeRegistry.resolve( StandardBasicTypes.INSTANT );
		final BasicType<OffsetDateTime> offsetDateTimeType = basicTypeRegistry.resolve( StandardBasicTypes.OFFSET_DATE_TIME );
		final BasicType<LocalDateTime> localDateTimeType = basicTypeRegistry.resolve( StandardBasicTypes.LOCAL_DATE_TIME );
		final BasicType<LocalTime> localTimeType = basicTypeRegistry.resolve( StandardBasicTypes.LOCAL_TIME );
		final BasicType<LocalDate> localDateType = basicTypeRegistry.resolve( StandardBasicTypes.LOCAL_DATE );

		CommonFunctionFactory functionFactory = new CommonFunctionFactory(queryEngine);

		//aggregate functions, supported on every database

		functionFactory.aggregates( this, SqlAstNodeRenderingMode.DEFAULT );

		//the ANSI SQL-defined aggregate functions any() and every() are only
		//supported on one database, but can be emulated using sum() and case,
		//though there is a more natural mapping on some databases

		functionFactory.everyAny_sumCase();

		//math functions supported on almost every database

		//Note that while certain mathematical functions return the same type
		//as their arguments, this is not the case in general - any function
		//involving exponentiation by a non-integer power, logarithms,
		//trigonometric functions, etc., should be considered to be of type
		//Double. In particular, there is no meaningful concept of an "exact
		//decimal" version of these functions, and if any database attempted
		//to implement such a silly thing, it would be dog slow.

		functionFactory.math();

		//trig functions supported on almost every database

		functionFactory.trigonometry();

		//coalesce() function, supported by most databases, must be emulated
		//in terms of nvl() for platforms which don't support it natively

		functionFactory.coalesce();

		//nullif() function, supported on almost every database

		functionFactory.nullif();

		//string functions, must be emulated where not supported

		functionFactory.leftRight();
		functionFactory.replace();
		functionFactory.concat();
		functionFactory.lowerUpper();

		//there are two forms of substring(), the JPA standard syntax, which
		//separates arguments using commas, and the ANSI SQL standard syntax
		//with named arguments (we support both)

		functionFactory.substring();

		//the JPA locate() function is especially tricky to emulate, calling
		//for lots of Dialect-specific customization

		functionFactory.locate();

		//JPA string length() function, a synonym for ANSI SQL character_length()

		functionFactory.length_characterLength();

		//only some databases support the ANSI SQL-style position() function, so
		//define it here as an alias for locate()

		queryEngine.getSqmFunctionRegistry().register( StandardFunctions.POSITION,
				new LocatePositionEmulation( typeConfiguration ) );

		//very few databases support ANSI-style overlay() function, so emulate
		//it here in terms of either insert() or concat()/substring()

		queryEngine.getSqmFunctionRegistry().register( StandardFunctions.OVERLAY,
				new InsertSubstringOverlayEmulation( typeConfiguration, false ) );

		//ANSI SQL trim() function is supported on almost all of the databases
		//we care about, but on some it must be emulated using ltrim(), rtrim(),
		//and replace()

		queryEngine.getSqmFunctionRegistry().register( StandardFunctions.TRIM,
				new TrimFunction( this, typeConfiguration ) );

		//ANSI SQL cast() function is supported on the databases we care most
		//about but in certain cases it doesn't allow some useful typecasts,
		//which must be emulated in a dialect-specific way

		//Note that two case are especially tricky to make portable:
		// - casts to and from Boolean, and
		// - casting Double or Float to String.

		queryEngine.getSqmFunctionRegistry().register(
				StandardFunctions.CAST,
				new CastFunction(
						this,
						queryEngine.getPreferredSqlTypeCodeForBoolean()
				)
		);

		//There is a 'collate' operator in a number of major databases

		functionFactory.collate();

		//ANSI SQL extract() function is supported on the databases we care most
		//about (though it is called datepart() in some of them) but HQL defines
		//additional non-standard temporal field types, which must be emulated in
		//a very dialect-specific way

		queryEngine.getSqmFunctionRegistry().register( StandardFunctions.EXTRACT,
				new ExtractFunction( this, typeConfiguration ) );

		//comparison functions supported on most databases, emulated on others
		//using a case expression

		functionFactory.leastGreatest();

		//two-argument synonym for coalesce() supported on most but not every
		//database, so define it here as an alias for coalesce(arg1,arg2)

		queryEngine.getSqmFunctionRegistry().register( StandardFunctions.IFNULL,
				new CoalesceIfnullEmulation() );

		//rpad() and pad() are supported on almost every database, and emulated
		//where not supported, but they're not considered "standard" ... instead
		//they're used to implement pad()

		functionFactory.pad();

		//pad() is a function we've designed to look like ANSI trim()

		queryEngine.getSqmFunctionRegistry().register( StandardFunctions.PAD,
				new LpadRpadPadEmulation( typeConfiguration ) );

		//legacy Hibernate convenience function for casting to string, defined
		//here as an alias for cast(arg as String)

		queryEngine.getSqmFunctionRegistry().register( StandardFunctions.STR,
				new CastStrEmulation( typeConfiguration ) );

		//format() function for datetimes, emulated on many databases using the
		//Oracle-style to_char() function, and on others using their native
		//formatting functions

		functionFactory.format_toChar();

		//timestampadd()/timestampdiff() delegated back to the Dialect itself
		//since there is a great variety of different ways to emulate them

		queryEngine.getSqmFunctionRegistry().register( StandardFunctions.TIMESTAMPADD,
				new TimestampaddFunction( this, typeConfiguration ) );
		queryEngine.getSqmFunctionRegistry().register( StandardFunctions.TIMESTAMPDIFF,
				new TimestampdiffFunction( this, typeConfiguration ) );
		queryEngine.getSqmFunctionRegistry().registerAlternateKey( StandardFunctions.DATEADD, StandardFunctions.TIMESTAMPADD );
		queryEngine.getSqmFunctionRegistry().registerAlternateKey( StandardFunctions.DATEDIFF, StandardFunctions.TIMESTAMPDIFF );

		//ANSI SQL (and JPA) current date/time/timestamp functions, supported
		//natively on almost every database, delegated back to the Dialect

		queryEngine.getSqmFunctionRegistry().register(
				StandardFunctions.CURRENT_DATE,
				new CurrentFunction(
						StandardFunctions.CURRENT_DATE,
						currentDate(),
						dateType
				)
		);
		queryEngine.getSqmFunctionRegistry().register(
				StandardFunctions.CURRENT_TIME,
				new CurrentFunction(
						StandardFunctions.CURRENT_TIME,
						currentTime(),
						timeType
				)
		);
		queryEngine.getSqmFunctionRegistry().register(
				StandardFunctions.CURRENT_TIMESTAMP,
				new CurrentFunction(
						StandardFunctions.CURRENT_TIMESTAMP,
						currentTimestamp(),
						timestampType
				)
		);
		queryEngine.getSqmFunctionRegistry().registerAlternateKey( "current date", StandardFunctions.CURRENT_DATE );
		queryEngine.getSqmFunctionRegistry().registerAlternateKey( "current time", StandardFunctions.CURRENT_TIME );
		queryEngine.getSqmFunctionRegistry().registerAlternateKey( "current timestamp", StandardFunctions.CURRENT_TIMESTAMP );
		//HQL current instant/date/time/datetime functions, delegated back to the Dialect

		queryEngine.getSqmFunctionRegistry().register(
				StandardFunctions.LOCAL_DATE,
				new CurrentFunction(
						StandardFunctions.LOCAL_DATE,
						currentDate(),
						localDateType
				)
		);
		queryEngine.getSqmFunctionRegistry().register(
				StandardFunctions.LOCAL_TIME,
				new CurrentFunction(
						StandardFunctions.LOCAL_TIME,
						currentLocalTime(),
						localTimeType
				)
		);
		queryEngine.getSqmFunctionRegistry().register(
				StandardFunctions.LOCAL_DATETIME,
				new CurrentFunction(
						StandardFunctions.LOCAL_DATETIME,
						currentLocalTimestamp(),
						localDateTimeType
				)
		);
		queryEngine.getSqmFunctionRegistry().register(
				StandardFunctions.OFFSET_DATETIME,
				new CurrentFunction(
						StandardFunctions.OFFSET_DATETIME,
						currentTimestampWithTimeZone(),
						offsetDateTimeType
				)
		);
		queryEngine.getSqmFunctionRegistry().registerAlternateKey( "local date", StandardFunctions.LOCAL_DATE );
		queryEngine.getSqmFunctionRegistry().registerAlternateKey( "local time", StandardFunctions.LOCAL_TIME );
		queryEngine.getSqmFunctionRegistry().registerAlternateKey( "local datetime", StandardFunctions.LOCAL_DATETIME );
		queryEngine.getSqmFunctionRegistry().registerAlternateKey( "offset datetime", StandardFunctions.OFFSET_DATETIME );

		queryEngine.getSqmFunctionRegistry().register(
				StandardFunctions.INSTANT,
				new CurrentFunction(
						StandardFunctions.INSTANT,
						currentTimestampWithTimeZone(),
						instantType
				)
		);
		queryEngine.getSqmFunctionRegistry().registerAlternateKey( "current_instant", StandardFunctions.INSTANT ); //deprecated legacy!

		queryEngine.getSqmFunctionRegistry().register( StandardFunctions.SQL, new SqlFunction() );
	}

	/**
	 * Translation of the HQL/JPQL {@code current_date} function, which
	 * maps to the Java type {@code java.sql.Date}, and of the HQL
	 * {@code local_date} function which maps to the Java type
	 * {@code java.sql.LocalDate}.
	 */
	public String currentDate() {
		return "current_date";
	}

	/**
	 * Translation of the HQL/JPQL {@code current_time} function, which
	 * maps to the Java type {@code java.sql.Time} which is a time with
	 * no time zone. This contradicts ANSI SQL where {@code current_time}
	 * has the type {@code TIME WITH TIME ZONE}.
	 * <p>
	 * It is recommended to override this in dialects for databases which
	 * support {@code localtime} or {@code time at local}.
	 */
	public String currentTime() {
		return "current_time";
	}

	/**
	 * Translation of the HQL/JPQL {@code current_timestamp} function,
	 * which maps to the Java type {@code java.sql.Timestamp} which is
	 * a datetime with no time zone. This contradicts ANSI SQL where
	 * {@code current_timestamp} has the type
	 * {@code TIMESTAMP WITH TIME ZONE}.
	 * <p>
	 * It is recommended to override this in dialects for databases which
	 * support {@code localtimestamp} or {@code timestamp at local}.
	 */
	public String currentTimestamp() {
		return "current_timestamp";
	}

	/**
	 * Translation of the HQL {@code local_time} function, which maps to
	 * the Java type {@code java.time.LocalTime} which is a time with no
	 * time zone. It should usually be the same SQL function as for
	 * {@link #currentTime()}.
	 * <p>
	 * It is recommended to override this in dialects for databases which
	 * support {@code localtime} or {@code current_time at local}.
	 */
	public String currentLocalTime() {
		return currentTime();
	}

	/**
	 * Translation of the HQL {@code local_datetime} function, which maps
	 * to the Java type {@code java.time.LocalDateTime} which is a datetime
	 * with no time zone. It should usually be the same SQL function as for
	 * {@link #currentTimestamp()}.
	 * <p>
	 * It is recommended to override this in dialects for databases which
	 * support {@code localtimestamp} or {@code current_timestamp at local}.
	 */
	public String currentLocalTimestamp() {
		return currentTimestamp();
	}

	/**
	 * Translation of the HQL {@code offset_datetime} function, which maps
	 * to the Java type {@code java.time.OffsetDateTime} which is a datetime
	 * with a time zone. This in principle correctly maps to the ANSI SQL
	 * {@code current_timestamp} which has the type
	 * {@code TIMESTAMP WITH TIME ZONE}.
	 */
	public String currentTimestampWithTimeZone() {
		return currentTimestamp();
	}

	/**
	 * Obtain a pattern for the SQL equivalent to an
	 * {@code extract()} function call. The resulting
	 * pattern must contain ?1 and ?2 placeholders
	 * for the arguments.
	 * <p>
	 * This method does not need to handle
	 * {@link TemporalUnit#NANOSECOND},
	 * {@link TemporalUnit#NATIVE},
	 * {@link TemporalUnit#OFFSET},
	 * {@link TemporalUnit#DATE},
	 * {@link TemporalUnit#TIME},
	 * {@link TemporalUnit#WEEK_OF_YEAR}, or
	 * {@link TemporalUnit#WEEK_OF_MONTH},
	 * which are already desugared by
	 * {@link ExtractFunction}.
	 *
	 * @param unit the first argument
	 */
	public String extractPattern(TemporalUnit unit) {
		return "extract(?1 from ?2)";
	}

	/**
	 * Obtain a pattern for the SQL equivalent to a
	 * {@code cast()} function call. The resulting
	 * pattern must contain ?1 and ?2 placeholders
	 * for the arguments.
	 *
	 * @param from a {@link CastType} indicating the
	 *             type of the value argument
	 * @param to a {@link CastType} indicating the
	 *           type the value argument is cast to
	 */
	public String castPattern(CastType from, CastType to) {
		switch ( to ) {
			case STRING:
				switch ( from ) {
					case INTEGER_BOOLEAN:
						return "case ?1 when 1 then 'true' when 0 then 'false' else null end";
					case YN_BOOLEAN:
						return "case ?1 when 'Y' then 'true' when 'N' then 'false' else null end";
					case TF_BOOLEAN:
						return "case ?1 when 'T' then 'true' when 'F' then 'false' else null end";
				}
				break;
			case INTEGER:
			case LONG:
				switch ( from ) {
					case YN_BOOLEAN:
						return "case ?1 when 'Y' then 1 when 'N' then 0 else null end";
					case TF_BOOLEAN:
						return "case ?1 when 'T' then 1 when 'F' then 0 else null end";
					case BOOLEAN:
						return "case ?1 when true then 1 when false then 0 else null end";
				}
				break;
			case INTEGER_BOOLEAN:
				switch ( from ) {
					case STRING:
						return "case ?1 when 'T' then 1 when 'Y' then 1 when 'F' then 0 when 'N' then 0 else null end";
					case INTEGER:
					case LONG:
						return "abs(sign(?1))";
					case YN_BOOLEAN:
						return "case ?1 when 'Y' then 1 when 'N' then 0 else null end";
					case TF_BOOLEAN:
						return "case ?1 when 'T' then 1 when 'F' then 0 else null end";
					case BOOLEAN:
						return "case ?1 when true then 1 when false then 0 else null end";
				}
				break;
			case YN_BOOLEAN:
				switch ( from ) {
					case STRING:
						return "case ?1 when 'T' then 'Y' when 'Y' then 'Y' when 'F' then 'N' when 'N' then 'N' else null end";
					case INTEGER_BOOLEAN:
						return "case ?1 when 1 then 'Y' when 0 then 'N' else null end";
					case INTEGER:
					case LONG:
						return "case abs(sign(?1)) when 1 then 'Y' when 0 then 'N' else null end";
					case TF_BOOLEAN:
						return "case ?1 when 'T' then 'Y' when 'F' then 'N' else null end";
					case BOOLEAN:
						return "case ?1 when true then 'Y' when false then 'N' else null end";
				}
				break;
			case TF_BOOLEAN:
				switch ( from ) {
					case STRING:
						return "case ?1 when 'T' then 'T' when 'Y' then 'T' when 'F' then 'F' when 'N' then 'F' else null end";
					case INTEGER_BOOLEAN:
						return "case ?1 when 1 then 'T' when 0 then 'F' else null end";
					case INTEGER:
					case LONG:
						return "case abs(sign(?1)) when 1 then 'T' when 0 then 'F' else null end";
					case YN_BOOLEAN:
						return "case ?1 when 'Y' then 'T' when 'N' then 'F' else null end";
					case BOOLEAN:
						return "case ?1 when true then 'T' when false then 'F' else null end";
				}
				break;
			case BOOLEAN:
				switch ( from ) {
					case STRING:
						return "case ?1 when 'T' then true when 'Y' then true when 'F' then false when 'N' then false else null end";
					case INTEGER_BOOLEAN:
					case INTEGER:
					case LONG:
						return "(?1<>0)";
					case YN_BOOLEAN:
						return "(?1<>'N')";
					case TF_BOOLEAN:
						return "(?1<>'F')";
				}
				break;
		}
		return "cast(?1 as ?2)";
	}

	/**
	 * Obtain a pattern for the SQL equivalent to a
	 * {@code trim()} function call. The resulting
	 * pattern must contain a ?1 placeholder for the
	 * argument of type {@link String}.
	 *
	 * @param specification {@code leading} or {@code trailing}
	 * @param character the character to trim
	 */
	public String trimPattern(TrimSpec specification, char character) {
		return character == ' '
				? "trim(" + specification + " from ?1)"
				: "trim(" + specification + " '" + character + "' from ?1)";
	}

	/**
	 * Whether the database supports adding a fractional interval to a timestamp
	 * e.g. `timestamp + 0.5 second`
	 */
	public boolean supportsFractionalTimestampArithmetic() {
		return true;
	}

	/**
	 * Obtain a pattern for the SQL equivalent to a
	 * {@code timestampdiff()} function call. The resulting
	 * pattern must contain ?1, ?2, and ?3 placeholders
	 * for the arguments.
	 * @param unit the first argument
	 * @param fromTemporalType true if the first argument is
	 *                      a timestamp, false if a date
	 * @param toTemporalType true if the second argument is
	 */
	public String timestampdiffPattern(TemporalUnit unit, TemporalType fromTemporalType, TemporalType toTemporalType) {
		throw new NotYetImplementedFor6Exception();
	}

	/**
	 * Obtain a pattern for the SQL equivalent to a
	 * {@code timestampadd()} function call. The resulting
	 * pattern must contain ?1, ?2, and ?3 placeholders
	 * for the arguments.
	 * @param unit The unit to add to the temporal
	 * @param temporalType The type of the temporal
	 * @param intervalType The type of interval to add or null if it's not a native interval
	 */
	public String timestampaddPattern(TemporalUnit unit, TemporalType temporalType, IntervalType intervalType) {
		throw new NotYetImplementedFor6Exception();
	}

	/**
	 * Do the given JDBC type codes, as defined in {@link Types} represent
	 * essentially the same type in this dialect of SQL?
	 * <p>
	 * The default implementation treats {@link Types#NUMERIC NUMERIC} and
	 * {@link Types#DECIMAL DECIMAL} as the same type, and
	 * {@link Types#FLOAT FLOAT}, {@link Types#REAL REAL}, and
	 * {@link Types#DOUBLE DOUBLE} as essentially the same type, since the
	 * ANSI SQL specification fails to meaningfully distinguish them.
	 * <p>
	 * The default implementation also treats {@link Types#VARCHAR VARCHAR},
	 * {@link Types#NVARCHAR NVARCHAR}, {@link Types#LONGVARCHAR LONGVARCHAR},
	 * and {@link Types#LONGNVARCHAR LONGNVARCHAR} as the same type, and
	 * {@link Types#VARBINARY BINARY} and
	 * {@link Types#LONGVARBINARY LONGVARBINARY} as the same type, since
	 * Hibernate doesn't really differentiate these types.
	 *
	 * @param typeCode1 the first column type info
	 * @param typeCode2 the second column type info
	 *
	 * @return {@code true} if the two type codes are equivalent
	 */
	public boolean equivalentTypes(int typeCode1, int typeCode2) {
		return typeCode1==typeCode2
			|| isNumericOrDecimal(typeCode1) && isNumericOrDecimal(typeCode2)
			|| isIntegral(typeCode1) && isIntegral(typeCode2)
			|| isFloatOrRealOrDouble(typeCode1) && isFloatOrRealOrDouble(typeCode2)
			|| isVarcharType(typeCode1) && isVarcharType(typeCode2)
			|| isVarbinaryType(typeCode1) && isVarbinaryType(typeCode2);
	}

	/**
	 * Retrieve a set of default Hibernate properties for this database.
	 *
	 * @return a set of Hibernate properties
	 */
	public final Properties getDefaultProperties() {
		return properties;
	}

	/**
	 * The default value to use for the configuration property
	 * {@value Environment#STATEMENT_BATCH_SIZE}.
	 */
	public int getDefaultStatementBatchSize() {
		return 1;
	}

	/**
	 * The default value to use for the configuration property
	 * {@value Environment#NON_CONTEXTUAL_LOB_CREATION}.
	 */
	public boolean getDefaultNonContextualLobCreation() {
		return false;
	}

	/**
	 * The default value to use for the configuration property
	 * {@value Environment#USE_GET_GENERATED_KEYS}.
	 */
	public boolean getDefaultUseGetGeneratedKeys() {
		return true;
	}

	@Override
	public String toString() {
		return getClass().getName();
	}


	// database type mapping support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Allows the Dialect to contribute additional types
	 *
	 * @param typeContributions Callback to contribute the types
	 * @param serviceRegistry The service registry
	 */
	public void contributeTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		// by default, not much to do...
		registerColumnTypes( typeContributions, serviceRegistry );
		final NationalizationSupport nationalizationSupport = getNationalizationSupport();
		if ( nationalizationSupport == NationalizationSupport.EXPLICIT ) {
			typeContributions.contributeJdbcType( NCharJdbcType.INSTANCE );
			typeContributions.contributeJdbcType( NVarcharJdbcType.INSTANCE );
			typeContributions.contributeJdbcType( LongNVarcharJdbcType.INSTANCE );
			typeContributions.contributeJdbcType( NClobJdbcType.DEFAULT );
		}

		if ( useInputStreamToInsertBlob() ) {
			typeContributions.getTypeConfiguration().getJdbcTypeRegistry().addDescriptor(
					Types.CLOB,
					ClobJdbcType.STREAM_BINDING
			);
		}

		if ( getTimeZoneSupport() == TimeZoneSupport.NATIVE ) {
			typeContributions.contributeJdbcType( InstantAsTimestampWithTimeZoneJdbcType.INSTANCE );
		}
		else {
			typeContributions.contributeJdbcType( InstantAsTimestampJdbcType.INSTANCE );
		}

		if ( supportsStandardArrays() ) {
			typeContributions.contributeJdbcType( ArrayJdbcType.INSTANCE );
		}
	}

	/**
	 * The legacy behavior of Hibernate.  LOBs are not processed by merge
	 */
	@SuppressWarnings("unused")
	protected static final LobMergeStrategy LEGACY_LOB_MERGE_STRATEGY = new LobMergeStrategy() {
		@Override
		public Blob mergeBlob(Blob original, Blob target, SharedSessionContractImplementor session) {
			return target;
		}

		@Override
		public Clob mergeClob(Clob original, Clob target, SharedSessionContractImplementor session) {
			return target;
		}

		@Override
		public NClob mergeNClob(NClob original, NClob target, SharedSessionContractImplementor session) {
			return target;
		}
	};

	/**
	 * Merge strategy based on transferring contents based on streams.
	 */
	@SuppressWarnings("unused")
	protected static final LobMergeStrategy STREAM_XFER_LOB_MERGE_STRATEGY = new LobMergeStrategy() {
		@Override
		public Blob mergeBlob(Blob original, Blob target, SharedSessionContractImplementor session) {
			if ( original != target ) {
				try {
					// the BLOB just read during the load phase of merge
					final OutputStream connectedStream = target.setBinaryStream( 1L );
					// the BLOB from the detached state
					final InputStream detachedStream = original.getBinaryStream();
					StreamCopier.copy( detachedStream, connectedStream );
					return target;
				}
				catch (SQLException e ) {
					throw session.getFactory().getJdbcServices().getSqlExceptionHelper()
							.convert( e, "unable to merge BLOB data" );
				}
			}
			else {
				return NEW_LOCATOR_LOB_MERGE_STRATEGY.mergeBlob( original, target, session );
			}
		}

		@Override
		public Clob mergeClob(Clob original, Clob target, SharedSessionContractImplementor session) {
			if ( original != target ) {
				try {
					// the CLOB just read during the load phase of merge
					final OutputStream connectedStream = target.setAsciiStream( 1L );
					// the CLOB from the detached state
					final InputStream detachedStream = original.getAsciiStream();
					StreamCopier.copy( detachedStream, connectedStream );
					return target;
				}
				catch (SQLException e ) {
					throw session.getFactory().getJdbcServices().getSqlExceptionHelper().convert( e, "unable to merge CLOB data" );
				}
			}
			else {
				return NEW_LOCATOR_LOB_MERGE_STRATEGY.mergeClob( original, target, session );
			}
		}

		@Override
		public NClob mergeNClob(NClob original, NClob target, SharedSessionContractImplementor session) {
			if ( original != target ) {
				try {
					// the NCLOB just read during the load phase of merge
					final OutputStream connectedStream = target.setAsciiStream( 1L );
					// the NCLOB from the detached state
					final InputStream detachedStream = original.getAsciiStream();
					StreamCopier.copy( detachedStream, connectedStream );
					return target;
				}
				catch (SQLException e ) {
					throw session.getFactory().getJdbcServices().getSqlExceptionHelper().convert( e, "unable to merge NCLOB data" );
				}
			}
			else {
				return NEW_LOCATOR_LOB_MERGE_STRATEGY.mergeNClob( original, target, session );
			}
		}
	};

	/**
	 * Merge strategy based on creating a new LOB locator.
	 */
	protected static final LobMergeStrategy NEW_LOCATOR_LOB_MERGE_STRATEGY = new LobMergeStrategy() {
		@Override
		public Blob mergeBlob(Blob original, Blob target, SharedSessionContractImplementor session) {
			if ( original == null && target == null ) {
				return null;
			}
			try {
				final LobCreator lobCreator = session.getFactory().getServiceRegistry().getService( JdbcServices.class ).getLobCreator(
						session
				);
				return original == null
						? lobCreator.createBlob( ArrayHelper.EMPTY_BYTE_ARRAY )
						: lobCreator.createBlob( original.getBinaryStream(), original.length() );
			}
			catch (SQLException e) {
				throw session.getFactory().getJdbcServices().getSqlExceptionHelper().convert( e, "unable to merge BLOB data" );
			}
		}

		@Override
		public Clob mergeClob(Clob original, Clob target, SharedSessionContractImplementor session) {
			if ( original == null && target == null ) {
				return null;
			}
			try {
				final LobCreator lobCreator = session.getFactory().getServiceRegistry().getService( JdbcServices.class ).getLobCreator( session );
				return original == null
						? lobCreator.createClob( "" )
						: lobCreator.createClob( original.getCharacterStream(), original.length() );
			}
			catch (SQLException e) {
				throw session.getFactory().getJdbcServices().getSqlExceptionHelper().convert( e, "unable to merge CLOB data" );
			}
		}

		@Override
		public NClob mergeNClob(NClob original, NClob target, SharedSessionContractImplementor session) {
			if ( original == null && target == null ) {
				return null;
			}
			try {
				final LobCreator lobCreator = session.getFactory().getServiceRegistry().getService( JdbcServices.class ).getLobCreator( session );
				return original == null
						? lobCreator.createNClob( "" )
						: lobCreator.createNClob( original.getCharacterStream(), original.length() );
			}
			catch (SQLException e) {
				throw session.getFactory().getJdbcServices().getSqlExceptionHelper().convert( e, "unable to merge NCLOB data" );
			}
		}
	};

	public LobMergeStrategy getLobMergeStrategy() {
		return NEW_LOCATOR_LOB_MERGE_STRATEGY;
	}


	// native identifier generation ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Resolves the native generation strategy associated to this dialect.
	 * <p/>
	 * Comes into play whenever the user specifies the native generator.
	 *
	 * @return The native generator strategy.
	 */
	public String getNativeIdentifierGeneratorStrategy() {
		return getIdentityColumnSupport().supportsIdentityColumns()
				? "identity"
				: "sequence";
	}

	// IDENTITY support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Get the appropriate {@link IdentityColumnSupport}
	 *
	 * @return the IdentityColumnSupport
	 * @since 5.1
	 */
	public IdentityColumnSupport getIdentityColumnSupport(){
		return new IdentityColumnSupportImpl();
	}

	// SEQUENCE support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public SequenceSupport getSequenceSupport() {
		return NoSequenceSupport.INSTANCE;
	}

	/**
	 * Get the select command used retrieve the names of all sequences.
	 *
	 * @return The select command; or null if sequences are not supported.
	 */
	public String getQuerySequencesString() {
		return null;
	}

	/**
	 * A source of {@link org.hibernate.tool.schema.extract.spi.SequenceInformation}.
	 */
	public SequenceInformationExtractor getSequenceInformationExtractor() {
		return getQuerySequencesString() == null
				? SequenceInformationExtractorNoOpImpl.INSTANCE
				: SequenceInformationExtractorLegacyImpl.INSTANCE;
	}

	// GUID support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Get the command used to select a GUID from the underlying database.
	 * <p/>
	 * Optional operation.
	 *
	 * @return The appropriate command.
	 */
	public String getSelectGUIDString() {
		throw new UnsupportedOperationException( getClass().getName() + " does not support GUIDs" );
	}

	public boolean supportsTemporaryTables() {
		// Most databases do
		return true;
	}

	public boolean supportsTemporaryTablePrimaryKey() {
		// Most databases do
		return true;
	}

	// limit/offset support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Returns a {@link LimitHandler} that implements support for
	 * {@link Query#setMaxResults(int)} and
	 * {@link Query#setFirstResult(int)} for
	 * this dialect.
	 */
	public LimitHandler getLimitHandler() {
		throw new UnsupportedOperationException("this dialect does not support query pagination");
	}

	// lock acquisition support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Informational metadata about whether this dialect is known to support
	 * specifying timeouts for requested lock acquisitions.
	 *
	 * @return True is this dialect supports specifying lock timeouts.
	 */
	public boolean supportsLockTimeouts() {
		return true;
	}

	/**
	 * If this dialect supports specifying lock timeouts, are those timeouts
	 * rendered into the {@code SQL} string as parameters.  The implication
	 * is that Hibernate will need to bind the timeout value as a parameter
	 * in the {@link PreparedStatement}.  If true, the param position
	 * is always handled as the last parameter; if the dialect specifies the
	 * lock timeout elsewhere in the {@code SQL} statement then the timeout
	 * value should be directly rendered into the statement and this method
	 * should return false.
	 *
	 * @return True if the lock timeout is rendered into the {@code SQL}
	 * string as a parameter; false otherwise.
	 */
	public boolean isLockTimeoutParameterized() {
		return false;
	}

	/**
	 * Get a strategy instance which knows how to acquire a database-level lock
	 * of the specified mode for this dialect.
	 *
	 * @param lockable The persister for the entity to be locked.
	 * @param lockMode The type of lock to be acquired.
	 * @return The appropriate locking strategy.
	 * @since 3.2
	 */
	public LockingStrategy getLockingStrategy(Lockable lockable, LockMode lockMode) {
		switch ( lockMode ) {
			case PESSIMISTIC_FORCE_INCREMENT:
				return new PessimisticForceIncrementLockingStrategy( lockable, lockMode );
			case PESSIMISTIC_WRITE:
				return new PessimisticWriteSelectLockingStrategy( lockable, lockMode );
			case PESSIMISTIC_READ:
				return new PessimisticReadSelectLockingStrategy( lockable, lockMode );
			case OPTIMISTIC:
				return new OptimisticLockingStrategy( lockable, lockMode );
			case OPTIMISTIC_FORCE_INCREMENT:
				return new OptimisticForceIncrementLockingStrategy( lockable, lockMode );
			default:
				return new SelectLockingStrategy( lockable, lockMode );
		}
	}

	/**
	 * Given LockOptions (lockMode, timeout), determine the appropriate for update fragment to use.
	 *
	 * @param lockOptions contains the lock mode to apply.
	 * @return The appropriate for update fragment.
	 */
	public String getForUpdateString(LockOptions lockOptions) {
		return getForUpdateString( lockOptions.getLockMode(), lockOptions.getTimeOut() );
	}

	private String getForUpdateString(LockMode lockMode, int timeout){
		switch ( lockMode ) {
			case PESSIMISTIC_READ: {
				return getReadLockString( timeout );
			}
			case PESSIMISTIC_WRITE: {
				return getWriteLockString( timeout );
			}
			case UPGRADE_NOWAIT:
			case PESSIMISTIC_FORCE_INCREMENT: {
				return getForUpdateNowaitString();
			}
			case UPGRADE_SKIPLOCKED: {
				return getForUpdateSkipLockedString();
			}
			default: {
				return "";
			}
		}
	}

	/**
	 * Given a lock mode, determine the appropriate for update fragment to use.
	 *
	 * @param lockMode The lock mode to apply.
	 * @return The appropriate for update fragment.
	 */
	public String getForUpdateString(LockMode lockMode) {
		return getForUpdateString( lockMode, LockOptions.WAIT_FOREVER );
	}

	/**
	 * Get the string to append to SELECT statements to acquire locks
	 * for this dialect.
	 *
	 * @return The appropriate {@code FOR UPDATE} clause string.
	 */
	public String getForUpdateString() {
		return " for update";
	}

	/**
	 * Get the string to append to SELECT statements to acquire WRITE locks
	 * for this dialect.  Location of the returned string is treated
	 * the same as getForUpdateString.
	 *
	 * @param timeout in milliseconds, -1 for indefinite wait and 0 for no wait.
	 * @return The appropriate {@code LOCK} clause string.
	 */
	public String getWriteLockString(int timeout) {
		return getForUpdateString();
	}

	/**
	 * Get the string to append to SELECT statements to acquire WRITE locks
	 * for this dialect given the aliases of the columns to be write locked.
	 * Location of the of the returned string is treated
	 * the same as getForUpdateString.
	 *
	 * @param aliases The columns to be read locked.
	 * @param timeout in milliseconds, -1 for indefinite wait and 0 for no wait.
	 * @return The appropriate {@code LOCK} clause string.
	 */
	public String getWriteLockString(String aliases, int timeout) {
		// by default we simply return the getWriteLockString(timeout) result since
		// the default is to say no support for "FOR UPDATE OF ..."
		return getWriteLockString( timeout );
	}

	/**
	 * Get the string to append to SELECT statements to acquire READ locks
	 * for this dialect.  Location of the returned string is treated
	 * the same as getForUpdateString.
	 *
	 * @param timeout in milliseconds, -1 for indefinite wait and 0 for no wait.
	 * @return The appropriate {@code LOCK} clause string.
	 */
	public String getReadLockString(int timeout) {
		return getForUpdateString();
	}

	/**
	 * Get the string to append to SELECT statements to acquire READ locks
	 * for this dialect given the aliases of the columns to be read locked.
	 * Location of the returned string is treated
	 * the same as getForUpdateString.
	 *
	 * @param aliases The columns to be read locked.
	 * @param timeout in milliseconds, -1 for indefinite wait and 0 for no wait.
	 * @return The appropriate {@code LOCK} clause string.
	 */
	public String getReadLockString(String aliases, int timeout) {
		// by default we simply return the getReadLockString(timeout) result since
		// the default is to say no support for "FOR UPDATE OF ..."
		return getReadLockString( timeout );
	}

	/**
	 * The row lock strategy to use for write locks.
	 */
	public RowLockStrategy getWriteRowLockStrategy() {
		// by default we report no support
		return RowLockStrategy.NONE;
	}

	/**
	 * The row lock strategy to use for read locks.
	 */
	public RowLockStrategy getReadRowLockStrategy() {
		return getWriteRowLockStrategy();
	}

	/**
	 * Does this dialect support {@code FOR UPDATE} in conjunction with
	 * outer joined rows?
	 *
	 * @return True if outer joined rows can be locked via {@code FOR UPDATE}.
	 */
	public boolean supportsOuterJoinForUpdate() {
		return true;
	}

	/**
	 * Get the {@code FOR UPDATE OF column_list} fragment appropriate for this
	 * dialect given the aliases of the columns to be write locked.
	 *
	 * @param aliases The columns to be write locked.
	 * @return The appropriate {@code FOR UPDATE OF column_list} clause string.
	 */
	public String getForUpdateString(String aliases) {
		// by default we simply return the getForUpdateString() result since
		// the default is to say no support for "FOR UPDATE OF ..."
		return getForUpdateString();
	}

	/**
	 * Get the {@code FOR UPDATE OF column_list} fragment appropriate for this
	 * dialect given the aliases of the columns to be write locked.
	 *
	 * @param aliases The columns to be write locked.
	 * @param lockOptions the lock options to apply
	 * @return The appropriate {@code FOR UPDATE OF column_list} clause string.
	 */
	public String getForUpdateString(String aliases, LockOptions lockOptions) {
		LockMode lockMode = lockOptions.getLockMode();
		final Iterator<Map.Entry<String, LockMode>> itr = lockOptions.getAliasLockIterator();
		while ( itr.hasNext() ) {
			// seek the highest lock mode
			final Map.Entry<String, LockMode>entry = itr.next();
			final LockMode lm = entry.getValue();
			if ( lm.greaterThan( lockMode ) ) {
				lockMode = lm;
			}
		}
		lockOptions.setLockMode( lockMode );
		return getForUpdateString( lockOptions );
	}

	/**
	 * Retrieves the {@code FOR UPDATE NOWAIT} syntax specific to this dialect.
	 *
	 * @return The appropriate {@code FOR UPDATE NOWAIT} clause string.
	 */
	public String getForUpdateNowaitString() {
		// by default we report no support for NOWAIT lock semantics
		return getForUpdateString();
	}

	/**
	 * Retrieves the {@code FOR UPDATE SKIP LOCKED} syntax specific to this dialect.
	 *
	 * @return The appropriate {@code FOR UPDATE SKIP LOCKED} clause string.
	 */
	public String getForUpdateSkipLockedString() {
		// by default we report no support for SKIP_LOCKED lock semantics
		return getForUpdateString();
	}

	/**
	 * Get the {@code FOR UPDATE OF column_list NOWAIT} fragment appropriate
	 * for this dialect given the aliases of the columns to be write locked.
	 *
	 * @param aliases The columns to be write locked.
	 * @return The appropriate {@code FOR UPDATE OF colunm_list NOWAIT} clause string.
	 */
	public String getForUpdateNowaitString(String aliases) {
		return getForUpdateString( aliases );
	}

	/**
	 * Get the {@code FOR UPDATE OF column_list SKIP LOCKED} fragment appropriate
	 * for this dialect given the aliases of the columns to be write locked.
	 *
	 * @param aliases The columns to be write locked.
	 * @return The appropriate {@code FOR UPDATE colunm_list SKIP LOCKED} clause string.
	 */
	public String getForUpdateSkipLockedString(String aliases) {
		return getForUpdateString( aliases );
	}

	/**
	 * Some dialects support an alternative means to {@code SELECT FOR UPDATE},
	 * whereby a "lock hint" is appended to the table name in the from clause.
	 * <p/>
	 * contributed by <a href="http://sourceforge.net/users/heschulz">Helge Schulz</a>
	 *
	 * @param lockOptions The lock options to apply
	 * @param tableName The name of the table to which to apply the lock hint.
	 * @return The table with any required lock hints.
	 */
	public String appendLockHint(LockOptions lockOptions, String tableName){
		return tableName;
	}

	/**
	 * Modifies the given SQL by applying the appropriate updates for the specified
	 * lock modes and key columns.
	 * <p/>
	 * The behavior here is that of an ANSI SQL {@code SELECT FOR UPDATE}.  This
	 * method is really intended to allow dialects which do not support
	 * {@code SELECT FOR UPDATE} to achieve this in their own fashion.
	 *
	 * @param sql the SQL string to modify
	 * @param aliasedLockOptions lock options indexed by aliased table names.
	 * @param keyColumnNames a map of key columns indexed by aliased table names.
	 * @return the modified SQL string.
	 */
	public String applyLocksToSql(String sql, LockOptions aliasedLockOptions, Map<String, String[]> keyColumnNames) {
		return sql + new ForUpdateFragment( this, aliasedLockOptions, keyColumnNames ).toFragmentString();
	}


	// table support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Command used to create a table.
	 *
	 * @return The command used to create a table.
	 */
	public String getCreateTableString() {
		return "create table";
	}

	/**
	 * Command used to alter a table.
	 *
	 * @param tableName The name of the table to alter
	 * @return The command used to alter a table.
	 * @since 5.2.11
	 */
	public String getAlterTableString(String tableName) {
		final StringBuilder sb = new StringBuilder( "alter table " );
		if ( supportsIfExistsAfterAlterTable() ) {
			sb.append( "if exists " );
		}
		sb.append( tableName );
		return sb.toString();
	}

	/**
	 * Slight variation on {@link #getCreateTableString}.  Here, we have the
	 * command used to create a table when there is no primary key and
	 * duplicate rows are expected.
	 * <p/>
	 * Most databases do not care about the distinction; originally added for
	 * Teradata support which does care.
	 *
	 * @return The command used to create a multiset table.
	 */
	public String getCreateMultisetTableString() {
		return getCreateTableString();
	}

	public SqmMultiTableMutationStrategy getFallbackSqmMutationStrategy(
			EntityMappingType entityDescriptor,
			RuntimeModelCreationContext runtimeModelCreationContext) {
		return new PersistentTableMutationStrategy(
				TemporaryTable.createIdTable(
						entityDescriptor,
						basename -> TemporaryTable.ID_TABLE_PREFIX + basename,
						this,
						runtimeModelCreationContext
				),
				runtimeModelCreationContext.getSessionFactory()
		);
	}

	public SqmMultiTableInsertStrategy getFallbackSqmInsertStrategy(
			EntityMappingType entityDescriptor,
			RuntimeModelCreationContext runtimeModelCreationContext) {
		return new PersistentTableInsertStrategy(
				TemporaryTable.createEntityTable(
						entityDescriptor,
						name -> TemporaryTable.ENTITY_TABLE_PREFIX + name,
						this,
						runtimeModelCreationContext
				),
				runtimeModelCreationContext.getSessionFactory()
		);
	}

	// callable statement support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Registers a parameter (either OUT, or the new REF_CURSOR param type available in Java 8) capable of
	 * returning {@link ResultSet} *by position*.  Pre-Java 8, registering such ResultSet-returning
	 * parameters varied greatly across database and drivers; hence its inclusion as part of the Dialect contract.
	 *
	 * @param statement The callable statement.
	 * @param position The bind position at which to register the output param.
	 *
	 * @return The number of (contiguous) bind positions used.
	 *
	 * @throws SQLException Indicates problems registering the param.
	 */
	public int registerResultSetOutParameter(CallableStatement statement, int position) throws SQLException {
		throw new UnsupportedOperationException(
				getClass().getName() +
						" does not support resultsets via stored procedures"
		);
	}

	/**
	 * Registers a parameter (either OUT, or the new REF_CURSOR param type available in Java 8) capable of
	 * returning {@link ResultSet} *by name*.  Pre-Java 8, registering such ResultSet-returning
	 * parameters varied greatly across database and drivers; hence its inclusion as part of the Dialect contract.
	 *
	 * @param statement The callable statement.
	 * @param name The parameter name (for drivers which support named parameters).
	 *
	 * @return The number of (contiguous) bind positions used.
	 *
	 * @throws SQLException Indicates problems registering the param.
	 */
	@SuppressWarnings("UnusedParameters")
	public int registerResultSetOutParameter(CallableStatement statement, String name) throws SQLException {
		throw new UnsupportedOperationException(
				getClass().getName() +
						" does not support resultsets via stored procedures"
		);
	}

	/**
	 * Given a callable statement previously processed by {@link #registerResultSetOutParameter},
	 * extract the {@link ResultSet} from the OUT parameter.
	 *
	 * @param statement The callable statement.
	 * @return The extracted result set.
	 * @throws SQLException Indicates problems extracting the result set.
	 */
	public ResultSet getResultSet(CallableStatement statement) throws SQLException {
		throw new UnsupportedOperationException(
				getClass().getName() + " does not support resultsets via stored procedures"
		);
	}

	/**
	 * Given a callable statement previously processed by {@link #registerResultSetOutParameter},
	 * extract the {@link ResultSet}.
	 *
	 * @param statement The callable statement.
	 * @param position The bind position at which to register the output param.
	 *
	 * @return The extracted result set.
	 *
	 * @throws SQLException Indicates problems extracting the result set.
	 */
	@SuppressWarnings("UnusedParameters")
	public ResultSet getResultSet(CallableStatement statement, int position) throws SQLException {
		throw new UnsupportedOperationException(
				getClass().getName() + " does not support resultsets via stored procedures"
		);
	}

	/**
	 * Given a callable statement previously processed by {@link #registerResultSetOutParameter},
	 * extract the {@link ResultSet} from the OUT parameter.
	 *
	 * @param statement The callable statement.
	 * @param name The parameter name (for drivers which support named parameters).
	 *
	 * @return The extracted result set.
	 *
	 * @throws SQLException Indicates problems extracting the result set.
	 */
	@SuppressWarnings("UnusedParameters")
	public ResultSet getResultSet(CallableStatement statement, String name) throws SQLException {
		throw new UnsupportedOperationException(
				getClass().getName() + " does not support resultsets via stored procedures"
		);
	}

	// current timestamp support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Does this dialect support a way to retrieve the database's current
	 * timestamp value?
	 *
	 * @return True if the current timestamp can be retrieved; false otherwise.
	 */
	public boolean supportsCurrentTimestampSelection() {
		return false;
	}

	/**
	 * Should the value returned by {@link #getCurrentTimestampSelectString}
	 * be treated as callable.  Typically this indicates that JDBC escape
	 * syntax is being used...
	 *
	 * @return True if the {@link #getCurrentTimestampSelectString} return
	 * is callable; false otherwise.
	 */
	public boolean isCurrentTimestampSelectStringCallable() {
		throw new UnsupportedOperationException( "Database not known to define a current timestamp function" );
	}

	/**
	 * Retrieve the command used to retrieve the current timestamp from the
	 * database.
	 *
	 * @return The command.
	 */
	public String getCurrentTimestampSelectString() {
		throw new UnsupportedOperationException( "Database not known to define a current timestamp function" );
	}


	// SQLException support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Build an instance of a {@link SQLExceptionConversionDelegate} for
	 * interpreting dialect-specific error or SQLState codes.
	 * <p/>
	 * If this method is overridden to return a non-null value,
	 * the default {@link SQLExceptionConverter} will use the returned
	 * {@link SQLExceptionConversionDelegate} in addition to the following
	 * standard delegates:
	 * <ol>
	 *     <li>a "static" delegate based on the JDBC 4 defined SQLException hierarchy;</li>
	 *     <li>a delegate that interprets SQLState codes for either X/Open or SQL-2003 codes,
	 *         depending on java.sql.DatabaseMetaData#getSQLStateType</li>
	 * </ol>
	 * <p/>
	 * It is strongly recommended that specific Dialect implementations override this
	 * method, since interpretation of a SQL error is much more accurate when based on
	 * the vendor-specific ErrorCode rather than the SQLState.
	 * <p/>
	 * Specific Dialects may override to return whatever is most appropriate for that vendor.
	 *
	 * @return The SQLExceptionConversionDelegate for this dialect
	 */
	public SQLExceptionConversionDelegate buildSQLExceptionConversionDelegate() {
		return null;
	}

	private static final ViolatedConstraintNameExtractor EXTRACTOR = sqle -> null;

	public ViolatedConstraintNameExtractor getViolatedConstraintNameExtractor() {
		return EXTRACTOR;
	}


	// union subclass support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Given a {@link Types} type code, determine an appropriate
	 * null value to use in a select clause.
	 * <p/>
	 * One thing to consider here is that certain databases might
	 * require proper casting for the nulls here since the select here
	 * will be part of a UNION/UNION ALL.
	 *
	 * @param sqlType The {@link Types} type code.
	 * @param typeConfiguration The type configuration
	 * @return The appropriate select clause value fragment.
	 */
	public String getSelectClauseNullString(int sqlType, TypeConfiguration typeConfiguration) {
		return "null";
	}

	/**
	 * Does this dialect support UNION ALL.
	 *
	 * @return True if UNION ALL is supported; false otherwise.
	 */
	public boolean supportsUnionAll() {
		return true;
	}

	/**
	 * Does this dialect support UNION in a subquery.
	 *
	 * @return True if UNION is supported ina subquery; false otherwise.
	 */
	public boolean supportsUnionInSubquery() {
		return supportsUnionAll();
	}

	// miscellaneous support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * The fragment used to insert a row without specifying any column values.
	 * This is not possible on some databases.
	 *
	 * @return The appropriate empty values clause.
	 */
	public String getNoColumnsInsertString() {
		return "values ( )";
	}

	/**
	 * Check if the INSERT statement is allowed to contain no column.
	 *
	 * @return if the Dialect supports no-column INSERT.
	 */
	public boolean supportsNoColumnsInsert() {
		return true;
	}

	/**
	 * The name of the SQL function that transforms a string to
	 * lowercase
	 *
	 * @return The dialect-specific lowercase function.
	 */
	public String getLowercaseFunction() {
		return "lower";
	}

	/**
	 * The name of the SQL function that can do case insensitive <b>like</b> comparison.
	 *
	 * @return  The dialect-specific "case insensitive" like function.
	 */
	public String getCaseInsensitiveLike(){
		return "like";
	}

	/**
	 * Does this dialect support case insensitive LIKE restrictions?
	 *
	 * @return {@code true} if the underlying database supports case insensitive like comparison,
	 * {@code false} otherwise.  The default is {@code false}.
	 */
	public boolean supportsCaseInsensitiveLike(){
		return false;
	}

	/**
	 * Does this dialect support truncation of values to a specified length through a cast?
	 *
	 * @return {@code true} if the underlying database supports truncation through a cast,
	 * {@code false} otherwise.  The default is {@code true}.
	 */
	public boolean supportsTruncateWithCast(){
		return true;
	}

	/**
	 * Meant as a means for end users to affect the select strings being sent
	 * to the database and perhaps manipulate them in some fashion.
	 *
	 * @param select The select command
	 * @return The mutated select command, or the same as was passed in.
	 */
	public String transformSelectString(String select) {
		return select;
	}

	/**
	 * What is the maximum length Hibernate can use for generated aliases?
	 * <p/>
	 * The maximum here should account for the fact that Hibernate often needs to append "uniqueing" information
	 * to the end of generated aliases.  That "uniqueing" information will be added to the end of a identifier
	 * generated to the length specified here; so be sure to leave some room (generally speaking 5 positions will
	 * suffice).
	 *
	 * @return The maximum length.
	 */
	public int getMaxAliasLength() {
		return 10;
	}

	/**
	 * What is the maximum identifier length supported by the dialect?
	 *
	 * @return The maximum length.
	 */
	public int getMaxIdentifierLength() {
		return Integer.MAX_VALUE;
	}

	/**
	 * The SQL literal value to which this database maps boolean values.
	 *
	 * @param bool The boolean value
	 * @return The appropriate SQL literal.
	 */
	public String toBooleanValueString(boolean bool) {
		final StringBuilder sb = new StringBuilder();
		appendBooleanValueString( new StringBuilderSqlAppender( sb ), bool );
		return sb.toString();
	}

	public void appendBooleanValueString(SqlAppender appender, boolean bool) {
		appender.appendSql( bool ? '1' : '0' );
	}


	// keyword support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	protected void registerKeyword(String word) {
		// When tokens are checked for keywords, they are always compared against the lower-case version of the token.
		// For instance, Template#renderWhereStringTemplate transforms all tokens to lower-case too.
		sqlKeywords.add( word.toLowerCase( Locale.ROOT ) );
	}

	/**
	 * The keywords of the SQL dialect
	 */
	public Set<String> getKeywords() {
		return sqlKeywords;
	}

	/**
	 * Build the IdentifierHelper indicated by this Dialect for handling identifier conversions.
	 * Returning {@code null} is allowed and indicates that Hibernate should fallback to building a
	 * "standard" helper.  In the fallback path, any changes made to the IdentifierHelperBuilder
	 * during this call will still be incorporated into the built IdentifierHelper.
	 * <p/>
	 * The incoming builder will have the following set:<ul>
	 *     <li>{@link IdentifierHelperBuilder#isGloballyQuoteIdentifiers()}</li>
	 *     <li>{@link IdentifierHelperBuilder#getUnquotedCaseStrategy()} - initialized to UPPER</li>
	 *     <li>{@link IdentifierHelperBuilder#getQuotedCaseStrategy()} - initialized to MIXED</li>
	 * </ul>
	 * <p/>
	 * By default Hibernate will do the following:<ul>
	 *     <li>Call {@link IdentifierHelperBuilder#applyIdentifierCasing(DatabaseMetaData)}
	 *     <li>Call {@link IdentifierHelperBuilder#applyReservedWords(DatabaseMetaData)}
	 *     <li>Applies {@link AnsiSqlKeywords#sql2003()} as reserved words</li>
	 *     <li>Applies the {#link #sqlKeywords} collected here as reserved words</li>
	 *     <li>Applies the Dialect's NameQualifierSupport, if it defines one</li>
	 * </ul>
	 *
	 * @param builder A semi-configured IdentifierHelper builder.
	 * @param dbMetaData Access to the metadata returned from the driver if needed and if available.  WARNING: may be {@code null}
	 *
	 * @return The IdentifierHelper instance to use, or {@code null} to indicate Hibernate should use its fallback path
	 *
	 * @throws SQLException Accessing the DatabaseMetaData can throw it.  Just re-throw and Hibernate will handle.
	 *
	 * @see #getNameQualifierSupport()
	 */
	public IdentifierHelper buildIdentifierHelper(
			IdentifierHelperBuilder builder,
			DatabaseMetaData dbMetaData) throws SQLException {
		builder.applyIdentifierCasing( dbMetaData );

		builder.applyReservedWords( sqlKeywords );

		builder.setNameQualifierSupport( getNameQualifierSupport() );

		return builder.build();
	}


	// identifier quoting support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * The character specific to this dialect used to begin a quoted identifier.
	 *
	 * @return The dialect's specific open quote character.
	 */
	public char openQuote() {
		return '"';
	}

	/**
	 * The character specific to this dialect used to close a quoted identifier.
	 *
	 * @return The dialect's specific close quote character.
	 */
	public char closeQuote() {
		return '"';
	}

	/**
	 * Apply dialect-specific quoting.
	 * <p/>
	 * By default, the incoming value is checked to see if its first character
	 * is the back-tick (`).  If so, the dialect specific quoting is applied.
	 *
	 * @param name The value to be quoted.
	 * @return The quoted (or unmodified, if not starting with back-tick) value.
	 * @see #openQuote()
	 * @see #closeQuote()
	 */
	public final String quote(String name) {
		if ( name == null ) {
			return null;
		}

		if ( name.charAt( 0 ) == '`' ) {
			return openQuote() + name.substring( 1, name.length() - 1 ) + closeQuote();
		}
		else {
			return name;
		}
	}


	// DDL support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	private final StandardTableExporter tableExporter = new StandardTableExporter( this );
	private final StandardSequenceExporter sequenceExporter = new StandardSequenceExporter( this );
	private final StandardIndexExporter indexExporter = new StandardIndexExporter( this );
	private final StandardForeignKeyExporter foreignKeyExporter = new StandardForeignKeyExporter( this );
	private final StandardUniqueKeyExporter uniqueKeyExporter = new StandardUniqueKeyExporter( this );
	private final StandardAuxiliaryDatabaseObjectExporter auxiliaryObjectExporter = new StandardAuxiliaryDatabaseObjectExporter( this );
	private final StandardTemporaryTableExporter temporaryTableExporter = new StandardTemporaryTableExporter( this );

	public Exporter<Table> getTableExporter() {
		return tableExporter;
	}

	public Exporter<Sequence> getSequenceExporter() {
		return sequenceExporter;
	}

	public Exporter<Index> getIndexExporter() {
		return indexExporter;
	}

	public Exporter<ForeignKey> getForeignKeyExporter() {
		return foreignKeyExporter;
	}

	public Exporter<Constraint> getUniqueKeyExporter() {
		return uniqueKeyExporter;
	}

	public Exporter<AuxiliaryDatabaseObject> getAuxiliaryDatabaseObjectExporter() {
		return auxiliaryObjectExporter;
	}

	public TemporaryTableExporter getTemporaryTableExporter() {
		return temporaryTableExporter;
	}

	public TemporaryTableKind getSupportedTemporaryTableKind() {
		return TemporaryTableKind.PERSISTENT;
	}

	public String getTemporaryTableCreateOptions() {
		return null;
	}

	public String getTemporaryTableCreateCommand() {
		final TemporaryTableKind kind = getSupportedTemporaryTableKind();
		switch ( kind ) {
			case PERSISTENT:
				return "create table";
			case LOCAL:
				return "create local temporary table";
			case GLOBAL:
				return "create global temporary table";
		}
		throw new UnsupportedOperationException( "Unsupported kind: " + kind );
	}

	public String getTemporaryTableDropCommand() {
		return "drop table";
	}

	public String getTemporaryTableTruncateCommand() {
		return "delete from";
	}

	/**
	 * Annotation to be appended to the end of each COLUMN clause for temporary tables.
	 *
	 * @param sqlTypeCode The SQL type code
	 * @return The annotation to be appended (e.g. "COLLATE DATABASE_DEFAULT" in SQLServer SQL)
	 */
	public String getCreateTemporaryTableColumnAnnotation(int sqlTypeCode) {
		return "";
	}

	public TempTableDdlTransactionHandling getTemporaryTableDdlTransactionHandling() {
		return TempTableDdlTransactionHandling.NONE;
	}

	public AfterUseAction getTemporaryTableAfterUseAction() {
		return AfterUseAction.CLEAN;
	}

	public BeforeUseAction getTemporaryTableBeforeUseAction() {
		return BeforeUseAction.NONE;
	}

	/**
	 * Does this dialect support catalog creation?
	 *
	 * @return True if the dialect supports catalog creation; false otherwise.
	 */
	public boolean canCreateCatalog() {
		return false;
	}

	/**
	 * Get the SQL command used to create the named catalog
	 *
	 * @param catalogName The name of the catalog to be created.
	 *
	 * @return The creation commands
	 */
	public String[] getCreateCatalogCommand(String catalogName) {
		throw new UnsupportedOperationException( "No create catalog syntax supported by " + getClass().getName() );
	}

	/**
	 * Get the SQL command used to drop the named catalog
	 *
	 * @param catalogName The name of the catalog to be dropped.
	 *
	 * @return The drop commands
	 */
	public String[] getDropCatalogCommand(String catalogName) {
		throw new UnsupportedOperationException( "No drop catalog syntax supported by " + getClass().getName() );
	}

	/**
	 * Does this dialect support schema creation?
	 *
	 * @return True if the dialect supports schema creation; false otherwise.
	 */
	public boolean canCreateSchema() {
		return true;
	}

	/**
	 * Get the SQL command used to create the named schema
	 *
	 * @param schemaName The name of the schema to be created.
	 *
	 * @return The creation commands
	 */
	public String[] getCreateSchemaCommand(String schemaName) {
		return new String[] {"create schema " + schemaName};
	}

	/**
	 * Get the SQL command used to drop the named schema
	 *
	 * @param schemaName The name of the schema to be dropped.
	 *
	 * @return The drop commands
	 */
	public String[] getDropSchemaCommand(String schemaName) {
		return new String[] {"drop schema " + schemaName};
	}

	/**
	 * Get the SQL command used to retrieve the current schema name.  Works in conjunction
	 * with {@link #getSchemaNameResolver()}, unless the return from there does not need this
	 * information.  E.g., a custom impl might make use of the Java 1.7 addition of
	 * the {@link Connection#getSchema()} method
	 *
	 * @return The current schema retrieval SQL
	 */
	public String getCurrentSchemaCommand() {
		return null;
	}

	/**
	 * Get the strategy for determining the schema name of a Connection
	 *
	 * @return The schema name resolver strategy
	 */
	public SchemaNameResolver getSchemaNameResolver() {
		return DefaultSchemaNameResolver.INSTANCE;
	}

	/**
	 * Does this dialect support the {@code ALTER TABLE} syntax?
	 *
	 * @return True if we support altering of tables; false otherwise.
	 */
	public boolean hasAlterTable() {
		return true;
	}

	/**
	 * Do we need to drop constraints before dropping tables in this dialect?
	 *
	 * @return True if constraints must be dropped prior to dropping
	 * the table; false otherwise.
	 */
	public boolean dropConstraints() {
		return true;
	}

	/**
	 * Do we need to qualify index names with the schema name?
	 *
	 * @return boolean
	 */
	public boolean qualifyIndexName() {
		return true;
	}

	/**
	 * The syntax used to add a column to a table (optional).
	 *
	 * @return The "add column" fragment.
	 */
	public String getAddColumnString() {
		return "add column";
	}

	/**
	 * The syntax for the suffix used to add a column to a table (optional).
	 *
	 * @return The suffix "add column" fragment.
	 */
	public String getAddColumnSuffixString() {
		return "";
	}

	public String getDropForeignKeyString() {
		return " drop constraint ";
	}

	public String getTableTypeString() {
		// grrr... for differentiation of mysql storage engines
		return "";
	}

	/**
	 * The syntax used to add a foreign key constraint to a table.
	 *
	 * @param constraintName The FK constraint name.
	 * @param foreignKey The names of the columns comprising the FK
	 * @param referencedTable The table referenced by the FK
	 * @param primaryKey The explicit columns in the referencedTable referenced
	 * by this FK.
	 * @param referencesPrimaryKey if false, constraint should be
	 * explicit about which column names the constraint refers to
	 *
	 * @return the "add FK" fragment
	 */
	public String getAddForeignKeyConstraintString(
			String constraintName,
			String[] foreignKey,
			String referencedTable,
			String[] primaryKey,
			boolean referencesPrimaryKey) {
		final StringBuilder res = new StringBuilder( 30 );

		res.append( " add constraint " )
				.append( quote( constraintName ) )
				.append( " foreign key (" )
				.append( String.join( ", ", foreignKey ) )
				.append( ") references " )
				.append( referencedTable );

		if ( !referencesPrimaryKey ) {
			res.append( " (" )
					.append( String.join( ", ", primaryKey ) )
					.append( ')' );
		}

		return res.toString();
	}

	public String getAddForeignKeyConstraintString(
			String constraintName,
			String foreignKeyDefinition) {
		return " add constraint " + quote(constraintName)
				+ " " + foreignKeyDefinition;
	}

	/**
	 * The syntax used to add a primary key constraint to a table.
	 *
	 * @param constraintName The name of the PK constraint.
	 * @return The "add PK" fragment
	 */
	public String getAddPrimaryKeyConstraintString(String constraintName) {
		return " add constraint " + constraintName + " primary key ";
	}

	/**
	 * Does the database/driver have bug in deleting rows that refer to other rows being deleted in the same query?
	 *
	 * @return {@code true} if the database/driver has this bug
	 */
	public boolean hasSelfReferentialForeignKeyBug() {
		return false;
	}

	/**
	 * The keyword used to specify a nullable column.
	 *
	 * @return String
	 */
	public String getNullColumnString() {
		return "";
	}

	/**
	 * The keyword used to specify a nullable column.
	 *
	 * @return String
	 */
	public String getNullColumnString(String columnType) {
		return getNullColumnString();
	}

	/**
	 * Does this dialect/database support commenting on tables, columns, etc?
	 *
	 * @return {@code true} if commenting is supported
	 */
	public boolean supportsCommentOn() {
		return false;
	}

	/**
	 * Get the comment into a form supported for table definition.
	 *
	 * @param comment The comment to apply
	 *
	 * @return The comment fragment
	 */
	public String getTableComment(String comment) {
		return "";
	}

	/**
	 * Get the comment into a form supported for column definition.
	 *
	 * @param comment The comment to apply
	 *
	 * @return The comment fragment
	 */
	public String getColumnComment(String comment) {
		return "";
	}

	/**
	 * For dropping a table, can the phrase "if exists" be applied before the table name?
	 * <p/>
	 * NOTE : Only one or the other (or neither) of this and {@link #supportsIfExistsAfterTableName} should return true
	 *
	 * @return {@code true} if the "if exists" can be applied before the table name
	 */
	public boolean supportsIfExistsBeforeTableName() {
		return false;
	}

	/**
	 * For dropping a table, can the phrase "if exists" be applied after the table name?
	 * <p/>
	 * NOTE : Only one or the other (or neither) of this and {@link #supportsIfExistsBeforeTableName} should return true
	 *
	 * @return {@code true} if the "if exists" can be applied after the table name
	 */
	public boolean supportsIfExistsAfterTableName() {
		return false;
	}

	/**
	 * For dropping a constraint with an "alter table", can the phrase "if exists" be applied before the constraint name?
	 * <p/>
	 * NOTE : Only one or the other (or neither) of this and {@link #supportsIfExistsAfterConstraintName} should return true
	 *
	 * @return {@code true} if the "if exists" can be applied before the constraint name
	 */
	public boolean supportsIfExistsBeforeConstraintName() {
		return false;
	}

	/**
	 * For dropping a constraint with an "alter table", can the phrase "if exists" be applied after the constraint name?
	 * <p/>
	 * NOTE : Only one or the other (or neither) of this and {@link #supportsIfExistsBeforeConstraintName} should return true
	 *
	 * @return {@code true} if the "if exists" can be applied after the constraint name
	 */
	public boolean supportsIfExistsAfterConstraintName() {
		return false;
	}

	/**
	 * For an "alter table", can the phrase "if exists" be applied?
	 *
	 * @return {@code true} if the "if exists" can be applied after ALTER TABLE
	 * @since 5.2.11
	 */
	public boolean supportsIfExistsAfterAlterTable() {
		return false;
	}

	/**
	 * Generate a DROP TABLE statement
	 *
	 * @param tableName The name of the table to drop
	 *
	 * @return The DROP TABLE command
	 */
	public String getDropTableString(String tableName) {
		final StringBuilder buf = new StringBuilder( "drop table " );
		if ( supportsIfExistsBeforeTableName() ) {
			buf.append( "if exists " );
		}
		buf.append( tableName ).append( getCascadeConstraintsString() );
		if ( supportsIfExistsAfterTableName() ) {
			buf.append( " if exists" );
		}
		return buf.toString();
	}

	/**
	 * Does this dialect support column-level check constraints?
	 *
	 * @return True if column-level CHECK constraints are supported; false
	 * otherwise.
	 */
	public boolean supportsColumnCheck() {
		return true;
	}

	/**
	 * Does this dialect support table-level check constraints?
	 *
	 * @return True if table-level CHECK constraints are supported; false
	 * otherwise.
	 */
	public boolean supportsTableCheck() {
		return true;
	}

	/**
	 * Does this dialect support cascaded delete on foreign key definitions?
	 *
	 * @return {@code true} indicates that the dialect does support cascaded delete on foreign keys.
	 */
	public boolean supportsCascadeDelete() {
		return true;
	}

	/**
	 * The keyword that specifies that a {@code drop table} operation
	 * should be cascaded to its constraints, typically
	 * {@code " cascade"} where the leading space is required, or
	 * the empty string if there is no such keyword in this dialect.
	 *
	 * @return The cascade drop keyword, if any, with a leading space
	 */
	public String getCascadeConstraintsString() {
		return "";
	}

	public ColumnAliasExtractor getColumnAliasExtractor() {
		return ColumnAliasExtractor.COLUMN_LABEL_EXTRACTOR;
	}

	// Informational metadata ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Should LOBs (both BLOB and CLOB) be bound using stream operations (i.e.
	 * {@link PreparedStatement#setBinaryStream}).
	 *
	 * @return True if BLOBs and CLOBs should be bound using stream operations.
	 * @since 3.2
	 */
	public boolean useInputStreamToInsertBlob() {
		return true;
	}

	/**
	 * Does this dialect support parameters within the {@code SELECT} clause of
	 * {@code INSERT ... SELECT ...} statements?
	 *
	 * @return True if this is supported; false otherwise.
	 * @since 3.2
	 */
	public boolean supportsParametersInInsertSelect() {
		return true;
	}

	/**
	 * Does this dialect support references to result variables
	 * (i.e, select items) by column positions (1-origin) as defined
	 * by the select clause?

	 * @return true if result variable references by column positions are supported;
	 *         false otherwise.
	 * @since 6.0.0
	 */
	public boolean supportsOrdinalSelectItemReference() {
		return true;
	}

	/**
	 * Returns the ordering of null.
	 *
	 * @since 6.0.0
	 */
	public NullOrdering getNullOrdering() {
		return NullOrdering.GREATEST;
	}

	public boolean supportsNullPrecedence() {
		return true;
	}

	public boolean isAnsiNullOn() {
		return true;
	}

	/**
	 * Does this dialect require that integer divisions be wrapped in {@code cast()}
	 * calls to tell the db parser the expected type.
	 *
	 * @return True if integer divisions must be cast()ed to float
	 */
	public boolean requiresFloatCastingOfIntegerDivision() {
		return false;
	}

	/**
	 * Does this dialect support asking the result set its positioning
	 * information on forward only cursors.  Specifically, in the case of
	 * scrolling fetches, Hibernate needs to use
	 * {@link ResultSet#isAfterLast} and
	 * {@link ResultSet#isBeforeFirst}.  Certain drivers do not
	 * allow access to these methods for forward only cursors.
	 * <p/>
	 * NOTE : this is highly driver dependent!
	 *
	 * @return True if methods like {@link ResultSet#isAfterLast} and
	 * {@link ResultSet#isBeforeFirst} are supported for forward
	 * only cursors; false otherwise.
	 * @since 3.2
	 */
	public boolean supportsResultSetPositionQueryMethodsOnForwardOnlyCursor() {
		return true;
	}

	/**
	 * Does this dialect support definition of cascade delete constraints
	 * which can cause circular chains?
	 *
	 * @return True if circular cascade delete constraints are supported; false
	 * otherwise.
	 * @since 3.2
	 */
	public boolean supportsCircularCascadeDeleteConstraints() {
		return true;
	}

	/**
	 * Are subselects supported as the left-hand-side (LHS) of
	 * IN-predicates.
	 * <p/>
	 * In other words, is syntax like {@code ... <subquery> IN (1, 2, 3) ...} supported?
	 *
	 * @return True if subselects can appear as the LHS of an in-predicate;
	 * false otherwise.
	 * @since 3.2
	 */
	public boolean supportsSubselectAsInPredicateLHS() {
		return true;
	}

	/**
	 * Expected LOB usage pattern is such that I can perform an insert
	 * via prepared statement with a parameter binding for a LOB value
	 * without crazy casting to JDBC driver implementation-specific classes...
	 * <p/>
	 * Part of the trickiness here is the fact that this is largely
	 * driver dependent.  For example, Oracle (which is notoriously bad with
	 * LOB support in their drivers historically) actually does a pretty good
	 * job with LOB support as of the 10.2.x versions of their drivers...
	 *
	 * @return True if normal LOB usage patterns can be used with this driver;
	 * false if driver-specific hookiness needs to be applied.
	 * @since 3.2
	 */
	public boolean supportsExpectedLobUsagePattern() {
		return true;
	}

	/**
	 * Does the dialect support propagating changes to LOB
	 * values back to the database?  Talking about mutating the
	 * internal value of the locator as opposed to supplying a new
	 * locator instance...
	 * <p/>
	 * For BLOBs, the internal value might be changed by:
	 * {@link Blob#setBinaryStream},
	 * {@link Blob#setBytes(long, byte[])},
	 * {@link Blob#setBytes(long, byte[], int, int)},
	 * or {@link Blob#truncate(long)}.
	 * <p/>
	 * For CLOBs, the internal value might be changed by:
	 * {@link Clob#setAsciiStream(long)},
	 * {@link Clob#setCharacterStream(long)},
	 * {@link Clob#setString(long, String)},
	 * {@link Clob#setString(long, String, int, int)},
	 * or {@link Clob#truncate(long)}.
	 * <p/>
	 * NOTE : I do not know the correct answer currently for
	 * databases which (1) are not part of the cruise control process
	 * or (2) do not {@link #supportsExpectedLobUsagePattern}.
	 *
	 * @return True if the changes are propagated back to the
	 * database; false otherwise.
	 * @since 3.2
	 */
	public boolean supportsLobValueChangePropagation() {
		// todo : pretty sure this is the same as the java.sql.DatabaseMetaData.locatorsUpdateCopy method added in JDBC 4, see HHH-6046
		return true;
	}

	/**
	 * Is it supported to materialize a LOB locator outside the transaction in
	 * which it was created?
	 * <p/>
	 * Again, part of the trickiness here is the fact that this is largely
	 * driver dependent.
	 * <p/>
	 * NOTE: all database I have tested which {@link #supportsExpectedLobUsagePattern()}
	 * also support the ability to materialize a LOB outside the owning transaction...
	 *
	 * @return True if unbounded materialization is supported; false otherwise.
	 * @since 3.2
	 */
	public boolean supportsUnboundedLobLocatorMaterialization() {
		return true;
	}

	/**
	 * Does this dialect support referencing the table being mutated in
	 * a subquery.  The "table being mutated" is the table referenced in
	 * an UPDATE or a DELETE query.  And so can that table then be
	 * referenced in a subquery of said UPDATE/DELETE query.
	 * <p/>
	 * For example, would the following two syntaxes be supported:<ul>
	 * <li>delete from TABLE_A where ID not in ( select ID from TABLE_A )</li>
	 * <li>update TABLE_A set NON_ID = 'something' where ID in ( select ID from TABLE_A)</li>
	 * </ul>
	 *
	 * @return True if this dialect allows references the mutating table from
	 * a subquery.
	 */
	public boolean supportsSubqueryOnMutatingTable() {
		return true;
	}

	/**
	 * Does the dialect support an exists statement in the select clause?
	 *
	 * @return True if exists checks are allowed in the select clause; false otherwise.
	 */
	public boolean supportsExistsInSelect() {
		return true;
	}

	/**
	 * For the underlying database, is READ_COMMITTED isolation implemented by
	 * forcing readers to wait for write locks to be released?
	 *
	 * @return True if writers block readers to achieve READ_COMMITTED; false otherwise.
	 */
	public boolean doesReadCommittedCauseWritersToBlockReaders() {
		return false;
	}

	/**
	 * For the underlying database, is REPEATABLE_READ isolation implemented by
	 * forcing writers to wait for read locks to be released?
	 *
	 * @return True if readers block writers to achieve REPEATABLE_READ; false otherwise.
	 */
	public boolean doesRepeatableReadCauseReadersToBlockWriters() {
		return false;
	}

	/**
	 * Does this dialect support using a JDBC bind parameter as an argument
	 * to a function or procedure call?
	 *
	 * @return Returns {@code true} if the database supports accepting bind params as args, {@code false} otherwise. The
	 * default is {@code true}.
	 */
	@SuppressWarnings("UnusedDeclaration")
	public boolean supportsBindAsCallableArgument() {
		return true;
	}

	/**
	 * Does this dialect support `count(a,b)`?
	 *
	 * @return True if the database supports counting tuples; false otherwise.
	 */
	public boolean supportsTupleCounts() {
		return false;
	}

	/**
	 * If {@link #supportsTupleCounts()} is true, does the Dialect require the tuple to be wrapped with parens?
	 *
	 * @return boolean
	 */
	public boolean requiresParensForTupleCounts() {
		return supportsTupleCounts();
	}

	/**
	 * Does this dialect support `count(distinct a,b)`?
	 *
	 * @return True if the database supports counting distinct tuples; false otherwise.
	 */
	public boolean supportsTupleDistinctCounts() {
		// oddly most database in fact seem to, so true is the default.
		return true;
	}

	/**
	 * If {@link #supportsTupleDistinctCounts()} is true, does the Dialect require the tuple to be wrapped with parens?
	 *
	 * @return boolean
	 */
	public boolean requiresParensForTupleDistinctCounts() {
		return false;
	}

	/**
	 * Return the limit that the underlying database places on the number of elements in an {@code IN} predicate.
	 * If the database defines no such limits, simply return zero or less-than-zero.
	 *
	 * @return int The limit, or zero-or-less to indicate no limit.
	 */
	public int getInExpressionCountLimit() {
		return 0;
	}

	/**
	 * HHH-4635
	 * Oracle expects all Lob values to be last in inserts and updates.
	 *
	 * @return boolean True if Lob values should be last, false if it
	 * does not matter.
	 */
	public boolean forceLobAsLastValue() {
		return false;
	}

	/**
	 * Return whether the dialect considers an empty-string value as null.
	 *
	 * @return boolean True if an empty string is treated as null, false otherwise.
	 */
	public boolean isEmptyStringTreatedAsNull() {
		return false;
	}

	/**
	 * Some dialects have trouble applying pessimistic locking depending upon what other query options are
	 * specified (paging, ordering, etc).  This method allows these dialects to request that locking be applied
	 * by subsequent selects.
	 *
	 * @return {@code true} indicates that the dialect requests that locking be applied by subsequent select;
	 * {@code false} (the default) indicates that locking should be applied to the main SQL statement..
	 *
	 * @since 5.2
	 */
	public boolean useFollowOnLocking(String sql, QueryOptions queryOptions) {
		return false;
	}

	/**
	 * Get the UniqueDelegate supported by this dialect
	 *
	 * @return The UniqueDelegate
	 */
	public UniqueDelegate getUniqueDelegate() {
		return uniqueDelegate;
	}

	/**
	 * Apply a hint to the query.  The entire query is provided, allowing the Dialect full control over the placement
	 * and syntax of the hint.  By default, ignore the hint and simply return the query.
	 *
	 * @param query The query to which to apply the hint.
	 * @param hintList The  hints to apply
	 * @return The modified SQL
	 */
	public String getQueryHintString(String query, List<String> hintList) {
		final String hints = String.join( ", ", hintList );
		return StringHelper.isEmpty( hints ) ? query : getQueryHintString( query, hints);
	}

	/**
	 * Apply a hint to the query.  The entire query is provided, allowing the Dialect full control over the placement
	 * and syntax of the hint.  By default, ignore the hint and simply return the query.
	 *
	 * @param query The query to which to apply the hint.
	 * @param hints The  hints to apply
	 * @return The modified SQL
	 */
	public String getQueryHintString(String query, String hints) {
		return query;
	}

	/**
	 * Certain dialects support a subset of ScrollModes.  Provide a default to be used by Criteria and Query.
	 *
	 * @return ScrollMode
	 */
	public ScrollMode defaultScrollMode() {
		return ScrollMode.SCROLL_INSENSITIVE;
	}

	/**
	 * Does this dialect support offset in subqueries?  Ex:
	 * select * from Table1 where col1 in (select col1 from Table2 order by col2 limit 1 offset 1)
	 *
	 * @return boolean
	 */
	public boolean supportsOffsetInSubquery() {
		return false;
	}

	/**
	 * Does this dialect support the order by clause in subqueries?  Ex:
	 * select * from Table1 where col1 in (select col1 from Table2 order by col2 limit 1)
	 *
	 * @return boolean
	 */
	public boolean supportsOrderByInSubquery() {
		return true;
	}

	/**
	 * Does this dialect support subqueries in the select clause?  Ex:
	 * select col1, (select col2 from Table2 where ..) from Table1
	 *
	 * @return boolean
	 */
	public boolean supportsSubqueryInSelect() {
		return true;
	}

	/**
	 * Does this dialect support the given fetch clause type.
	 *
	 * @param type The fetch clause type
	 * @return {@code true} if the underlying database supports the given fetch clause type,
	 * {@code false} otherwise.  The default is {@code false}.
	 */
	public boolean supportsFetchClause(FetchClauseType type) {
		return false;
	}

	/**
	 * Does this dialect support window functions like {@code row_number() over (..)}?
	 *
	 * @return {@code true} if the underlying database supports window functions,
	 * {@code false} otherwise.  The default is {@code false}.
	 */
	public boolean supportsWindowFunctions() {
		return false;
	}

	/**
	 * Does this dialect support the SQL lateral keyword or a proprietary alternative?
	 *
	 * @return {@code true} if the underlying database supports lateral,
	 * {@code false} otherwise.  The default is {@code false}.
	 */
	public boolean supportsLateral() {
		return false;
	}

	public CallableStatementSupport getCallableStatementSupport() {
		// most databases do not support returning cursors (ref_cursor)...
		return StandardCallableStatementSupport.NO_REF_CURSOR_INSTANCE;
	}

	/**
	 * By default interpret this based on DatabaseMetaData.
	 *
	 * @return The NameQualifierSupport.
	 */
	public NameQualifierSupport getNameQualifierSupport() {
		return null;
	}

	protected final BatchLoadSizingStrategy STANDARD_DEFAULT_BATCH_LOAD_SIZING_STRATEGY =
			(numberOfKeyColumns, numberOfKeys, inClauseParameterPaddingEnabled) -> {
		final int paddedSize;

		if ( inClauseParameterPaddingEnabled ) {
			paddedSize = MathHelper.ceilingPowerOfTwo( numberOfKeys );
		}
		else {
			paddedSize = numberOfKeys;
		}

		// For tuples, there is no limit, so we can just use the power of two padding approach
		if ( numberOfKeyColumns > 1 ) {
			return paddedSize;
		}
		final int inExpressionCountLimit = getInExpressionCountLimit();
		if ( inExpressionCountLimit > 0 ) {
			if ( paddedSize < inExpressionCountLimit ) {
				return paddedSize;
			}
			else if ( numberOfKeys < inExpressionCountLimit ) {
				return numberOfKeys;
			}
			return getInExpressionCountLimit();
		}
		return paddedSize;
	};

	public BatchLoadSizingStrategy getDefaultBatchLoadSizingStrategy() {
		return STANDARD_DEFAULT_BATCH_LOAD_SIZING_STRATEGY;
	}

	/**
	 * Is JDBC statement warning logging enabled by default?
	 *
	 * @since 5.1
	 */
	public boolean isJdbcLogWarningsEnabledByDefault() {
		return true;
	}

	public void augmentPhysicalTableTypes(List<String> tableTypesList) {
		// nothing to do
	}

	public void augmentRecognizedTableTypes(List<String> tableTypesList) {
		// nothing to do
	}

	/**
	 * Does the underlying database support partition by?
	 *
	 * @since 5.2
	 */
	public boolean supportsPartitionBy() {
		return false;
	}

	/**
	 * Override the DatabaseMetaData#supportsNamedParameters()
	 *
	 * @return boolean
	 *
	 * @throws SQLException Accessing the DatabaseMetaData can throw it.  Just re-throw and Hibernate will handle.
	 */
	public boolean supportsNamedParameters(DatabaseMetaData databaseMetaData) throws SQLException {
		return databaseMetaData != null && databaseMetaData.supportsNamedParameters();
	}

	/**
	 * Determines whether this database requires the use
	 * of explicit nationalized character data types.
	 * <p>
	 * That is, whether the use of {@link Types#NCHAR},
	 * {@link Types#NVARCHAR}, and {@link Types#NCLOB} is
	 * required for nationalized character data (Unicode).
	 */
	public NationalizationSupport getNationalizationSupport() {
		return NationalizationSupport.EXPLICIT;
	}

	/**
	 * Database has native support for SQL standard arrays which can be referred to through base type name.
	 * Oracle for example doesn't support this, but instead has support for named arrays.
	 *
	 * @return boolean
	 * @since 6.1
	 */
	public boolean supportsStandardArrays() {
		return false;
	}

	/**
	 * The SQL type name for the array of the given type name.
	 *
	 * @since 6.1
	 */
	public String getArrayTypeName(String elementTypeName) {
		if ( supportsStandardArrays() ) {
			return elementTypeName + " array";
		}
		return null;
	}

	public void appendArrayLiteral(
			SqlAppender appender,
			Object[] literal,
			JdbcLiteralFormatter<Object> elementFormatter,
			WrapperOptions wrapperOptions) {
		if ( !supportsStandardArrays() ) {
			throw new UnsupportedOperationException( getClass().getName() + " does not support array literals" );
		}
		appender.appendSql( "ARRAY[" );
		if ( literal.length != 0 ) {
			if ( literal[0] == null ) {
				appender.appendSql( "null" );
			}
			else {
				elementFormatter.appendJdbcLiteral( appender, literal[0], this, wrapperOptions );
			}
			for ( int i = 1; i < literal.length; i++ ) {
				appender.appendSql( ',' );
				if ( literal[i] == null ) {
					appender.appendSql( "null" );
				}
				else {
					elementFormatter.appendJdbcLiteral( appender, literal[i], this, wrapperOptions );
				}
			}
		}
		appender.appendSql( ']' );
	}

	/**
	 * Is this SQL dialect known to support some kind of distinct from predicate.
	 * <p/>
	 * Basically, does it support syntax like
	 * "... where FIRST_NAME IS DISTINCT FROM LAST_NAME"
	 *
	 * @return True if this SQL dialect is known to support some kind of distinct from predicate; false otherwise
	 * @since 6.1
	 */
	public boolean supportsDistinctFromPredicate() {
		return false;
	}

	/**
	 * The JDBC {@link SqlTypes type code} to use for mapping
	 * properties of basic Java array or {@code Collection} types.
	 * <p>
	 * Usually {@link SqlTypes#ARRAY} or {@link SqlTypes#VARBINARY}.
	 *
	 * @return one of the type codes defined by {@link SqlTypes}.
	 * @since 6.1
	 */
	public int getPreferredSqlTypeCodeForArray() {
		return supportsStandardArrays() ? ARRAY : VARBINARY;
	}

	/**
	 * The JDBC {@link Types type code} to use for mapping
	 * properties of Java type {@code boolean}.
	 * <p>
	 * Usually {@link Types#BOOLEAN} or {@link Types#BIT}.
	 *
	 * @return one of the type codes defined by {@link Types}.
	 */
	public int getPreferredSqlTypeCodeForBoolean() {
		return Types.BOOLEAN;
	}

	/**
	 * Does this dialect/database support non-query statements (e.g. INSERT, UPDATE, DELETE) with CTE (Common Table Expressions)?
	 *
	 * @return {@code true} if non-query statements are supported with CTE
	 */
	public boolean supportsNonQueryWithCTE() {
		return false;
	}

	/**
	 * Does this dialect/database support VALUES list (e.g. VALUES (1), (2), (3) )
	 *
	 * @return {@code true} if VALUES list are supported
	 */
	public boolean supportsValuesList() {
		return false;
	}

	/**
	 * Does this dialect/database support VALUES list (e.g. VALUES (1), (2), (3) ) for insert statements.
	 *
	 * @return {@code true} if VALUES list are supported for insert statements
	 */
	public boolean supportsValuesListForInsert() {
		return true;
	}

	/**
	 * Does this dialect/database support SKIP_LOCKED timeout.
	 *
	 * @return {@code true} if SKIP_LOCKED is supported
	 */
	public boolean supportsSkipLocked() {
		return false;
	}

	/**
	 * Does this dialect/database support NO_WAIT timeout.
	 *
	 * @return {@code true} if NO_WAIT is supported
	 */
	public boolean supportsNoWait() {
		return false;
	}

	/**
	 * Does this dialect/database support WAIT timeout.
	 *
	 * @return {@code true} if WAIT is supported
	 */
	public boolean supportsWait() {
		return supportsNoWait();
	}

	/**
	 * Inline String literal.
	 *
	 * @return escaped String
	 */
	public String inlineLiteral(String literal) {
		final StringBuilder sb = new StringBuilder( literal.length() + 2 );
		appendLiteral( new StringBuilderSqlAppender( sb ), literal );
		return sb.toString();
	}

	public void appendLiteral(SqlAppender appender, String literal) {
		appender.appendSql( '\'' );
		for ( int i = 0; i < literal.length(); i++ ) {
			final char c = literal.charAt( i );
			if ( c == '\'' ) {
				appender.appendSql( '\'' );
			}
			appender.appendSql( c );
		}
		appender.appendSql( '\'' );
	}

	/**
	 * Check whether the JDBC {@link Connection} supports creating LOBs via {@link Connection#createBlob()},
	 * {@link Connection#createNClob()} or {@link Connection#createClob()}.
	 *
	 * @param databaseMetaData JDBC {@link DatabaseMetaData} which can be used if LOB creation is supported only starting from a given Driver version
	 *
	 * @return {@code true} if LOBs can be created via the JDBC Connection.
	 */
	public boolean supportsJdbcConnectionLobCreation(DatabaseMetaData databaseMetaData) {
		return true;
	}

	/**
	 * Modify the SQL, adding hints or comments, if necessary
	 */
	public String addSqlHintOrComment(
			String sql,
			QueryOptions queryOptions,
			boolean commentsEnabled) {
		// Keep this here, rather than moving to Select.  Some Dialects may need the hint to be appended to the very
		// end or beginning of the finalized SQL statement, so wait until everything is processed.
		if ( queryOptions.getDatabaseHints() != null && queryOptions.getDatabaseHints().size() > 0 ) {
			sql = getQueryHintString( sql, queryOptions.getDatabaseHints() );
		}
		if ( commentsEnabled && queryOptions.getComment() != null ) {
			sql = prependComment( sql, queryOptions.getComment() );
		}

		return sql;
	}

	protected String prependComment(String sql, String comment) {
		return "/* " + escapeComment( comment ) + " */ " + sql;
	}

	public static String escapeComment(String comment) {
		if ( StringHelper.isNotEmpty( comment ) ) {
			final String escaped = ESCAPE_CLOSING_COMMENT_PATTERN.matcher( comment ).replaceAll( "*\\\\/" );
			return ESCAPE_OPENING_COMMENT_PATTERN.matcher( escaped ).replaceAll( "/\\\\*" );
		}
		return comment;
	}

	/**
	 * Return an HqlTranslator specific for the Dialect.  Return {@code null}
	 * to use Hibernate's standard translator.
	 *
	 * Note that {@link SessionFactoryOptions#getCustomHqlTranslator()} has higher precedence
	 *
	 * @see org.hibernate.query.hql.internal.StandardHqlTranslator
	 * @see QueryEngine#getHqlTranslator()
	 */
	public HqlTranslator getHqlTranslator() {
		return null;
	}

	/**
	 * Return an SqmToSqlAstConverterFactory specific for the Dialect.  Return {@code null}
	 * to use Hibernate's standard translator.
	 *
	 * Note that {@link SessionFactoryOptions#getCustomSqmTranslatorFactory()} has higher
	 * precedence as it comes directly from the user config
	 *
	 * @see org.hibernate.query.sqm.sql.internal.StandardSqmTranslator
	 * @see QueryEngine#getSqmTranslatorFactory()
	 */
	public SqmTranslatorFactory getSqmTranslatorFactory() {
		return null;
	}

	/**
	 * Return an SqlAstTranslatorFactory specific for the Dialect.  Return {@code null}
	 * to use Hibernate's standard translator.
	 *
	 * @see StandardSqlAstTranslatorFactory
	 * @see JdbcEnvironment#getSqlAstTranslatorFactory()
	 */
	public SqlAstTranslatorFactory getSqlAstTranslatorFactory() {
		return null;
	}

	public SelectItemReferenceStrategy getGroupBySelectItemReferenceStrategy() {
		return SelectItemReferenceStrategy.EXPRESSION;
	}

	public SizeStrategy getSizeStrategy() {
		return sizeStrategy;
	}

	/**
	 * The longest possible length of a {@link java.sql.Types#VARCHAR}-like column.
	 * For longer column lengths, use some sort of {@code text}-like type for the
	 * column.
	 */
	public int getMaxVarcharLength() {
		//the longest possible length of a Java string
		return Integer.MAX_VALUE;
	}

	/**
	 * The longest possible length of a {@link java.sql.Types#NVARCHAR}-like column.
	 * For longer column lengths, use some sort of {@code text}-like type for the
	 * column.
	 */
	public int getMaxNVarcharLength() {
		//for most databases it's the same as for VARCHAR
		return getMaxVarcharLength();
	}

	/**
	 * The longest possible length of a {@link java.sql.Types#VARBINARY}-like column.
	 * For longer column lengths, use some sort of {@code image}-like type for the
	 * column.
	 */
	public int getMaxVarbinaryLength() {
		//for most databases it's the same as for VARCHAR
		return getMaxVarcharLength();
	}

	public long getDefaultLobLength() {
		return Size.DEFAULT_LOB_LENGTH;
	}

	/**
	 * This is the default precision for a generated
	 * column mapped to a {@link java.math.BigInteger}
	 * or {@link java.math.BigDecimal}.
	 * <p>
	 * Usually returns the maximum precision of the
	 * database, except when there is no such maximum
	 * precision, or the maximum precision is very high.
	 *
	 * @return the default precision, in decimal digits
	 */
	public int getDefaultDecimalPrecision() {
		//this is the maximum for Oracle, SQL Server,
		//Sybase, and Teradata, so it makes a reasonable
		//default (uses 17 bytes on SQL Server and MySQL)
		return 38;
	}

	/**
	 * This is the default precision for a generated
	 * column mapped to a {@link Timestamp} or
	 * {@link java.time.LocalDateTime}.
	 * <p>
	 * Usually 6 (microseconds) or 3 (milliseconds).
	 *
	 * @return the default precision, in decimal digits,
	 *         of the fractional seconds field
	 */
	public int getDefaultTimestampPrecision() {
		//milliseconds or microseconds is the maximum
		//for most dialects that support explicit
		//precision, with the exception of DB2 which
		//accepts up to 12 digits!
		return 6; //microseconds!
	}

	/**
	 * This is the default precision for a generated
	 * column mapped to a Java {@link Float} or
	 * {@code float}. That is, a value representing
	 * "single precision".
	 * <p>
	 * Usually 24 binary digits, at least for
	 * databases with a conventional interpretation
	 * of the ANSI SQL specification.
	 *
	 * @return a value representing "single precision",
	 *         usually in binary digits, but sometimes
	 *         in decimal digits
	 */
	public int getFloatPrecision() {
		return 24;
	}

	/**
	 * This is the default precision for a generated
	 * column mapped to a Java {@link Double} or
	 * {@code double}. That is, a value representing
	 * "double precision".
	 * <p>
	 * Usually 53 binary digits, at least for
	 * databases with a conventional interpretation
	 * of the ANSI SQL specification.
	 *
	 * @return a value representing "double precision",
	 *         usually in binary digits, but sometimes
	 *         in decimal digits
	 */
	public int getDoublePrecision() {
		return 53;
	}

	/**
	 * The "native" precision for arithmetic with datetimes
	 * and day-to-second durations. Datetime differences
	 * will be calculated with this precision except when a
	 * precision is explicitly specified as a
	 * {@link TemporalUnit}.
	 * <p>
	 * Usually 1 (nanoseconds), 1_000 (microseconds), or
	 * 1_000_000 (milliseconds).
	 *
	 * @return the precision, specified as a quantity of
	 *         nanoseconds
	 * @see TemporalUnit#NATIVE
	 */
	public long getFractionalSecondPrecisionInNanos() {
		return 1; //default to nanoseconds for now
	}

	/**
	 * Does this dialect have a true SQL {@link Types#BIT BIT} type
	 * with just two values (0 and 1) or, even better, a proper SQL
	 * {@link Types#BOOLEAN BOOLEAN} type, or does {@link Types#BIT}
	 * get mapped to a numeric type with more than two values?
	 *
	 * @return true if there is a {@code BIT} or {@code BOOLEAN} type
	 */
	public boolean supportsBitType() {
		return true;
	}

	public void appendBinaryLiteral(SqlAppender appender, byte[] bytes) {
		appender.appendSql( "X'" );
		PrimitiveByteArrayJavaType.INSTANCE.appendString( appender, bytes );
		appender.appendSql( '\'' );
	}

	public RowLockStrategy getLockRowIdentifier(LockMode lockMode) {
		switch ( lockMode ) {
			case PESSIMISTIC_READ:
				return getReadRowLockStrategy();
			case WRITE:
			case PESSIMISTIC_FORCE_INCREMENT:
			case PESSIMISTIC_WRITE:
			case UPGRADE_SKIPLOCKED:
			case UPGRADE_NOWAIT: {
				return getWriteRowLockStrategy();
			}
			default: {
				return RowLockStrategy.NONE;
			}
		}
	}

	public String generatedAs(String generatedAs) {
		return " generated always as (" + generatedAs + ") stored";
	}

	public boolean hasDataTypeBeforeGeneratedAs() {
		return true;
	}

    /**
	 * Pluggable strategy for determining the Size to use for columns of
	 * a given SQL type.
	 *
	 * Allows Dialects, integrators and users a chance to apply
	 * column size defaults and limits in certain situations based on the mapped
	 * SQL and Java types.  E.g. when mapping a UUID to a VARCHAR column
	 * we know the default Size should be `Size#length == 36`.
	 */
	public interface SizeStrategy {
		/**
		 * Resolve the {@link Size} to use for columns of the given
		 * {@link JdbcType SQL type} and {@link JavaType Java type}.
		 *
		 * @return a non-null {@link Size}
		 */
		Size resolveSize(
				JdbcType jdbcType,
				JavaType<?> javaType,
				Integer precision,
				Integer scale,
				Long length);
	}

	public class SizeStrategyImpl implements SizeStrategy {
		@Override
		public Size resolveSize(
				JdbcType jdbcType,
				JavaType<?> javaType,
				Integer precision,
				Integer scale,
				Long length) {
			final Size size = new Size();
			int jdbcTypeCode = jdbcType.getDefaultSqlTypeCode();
			// Set the explicit length to null if we encounter the JPA default 255
			if ( length != null && length == Size.DEFAULT_LENGTH ) {
				length = null;
			}

			switch ( jdbcTypeCode ) {
				case SqlTypes.BIT:
				case SqlTypes.CHAR:
				case SqlTypes.NCHAR:
				case SqlTypes.VARCHAR:
				case SqlTypes.NVARCHAR:
				case SqlTypes.BINARY:
				case SqlTypes.VARBINARY:
				case SqlTypes.CLOB:
				case SqlTypes.BLOB:
					size.setLength( javaType.getDefaultSqlLength( Dialect.this, jdbcType ) );
					break;
				case SqlTypes.LONGVARCHAR:
				case SqlTypes.LONGNVARCHAR:
				case SqlTypes.LONGVARBINARY:
					size.setLength( javaType.getLongSqlLength() );
					break;
				case SqlTypes.FLOAT:
				case SqlTypes.DOUBLE:
				case SqlTypes.REAL:
					// this is almost always the thing we use:
					length = null;
					size.setPrecision( javaType.getDefaultSqlPrecision( Dialect.this, jdbcType ) );
					if ( scale != null && scale != 0 ) {
						throw new IllegalArgumentException("scale has no meaning for floating point numbers");
					}
					// but if the user explicitly specifies a precision, we need to convert it:
					if (precision != null) {
						// convert from base 10 (as specified in @Column) to base 2 (as specified by SQL)
						// using the magic of high school math: log_2(10^n) = n*log_2(10) = n*ln(10)/ln(2)
						precision = (int) ceil( precision * LOG_BASE2OF10 );
					}
					break;
				case SqlTypes.TIMESTAMP:
				case SqlTypes.TIMESTAMP_WITH_TIMEZONE:
				case SqlTypes.TIMESTAMP_UTC:
					length = null;
					size.setPrecision( javaType.getDefaultSqlPrecision( Dialect.this, jdbcType ) );
					if ( scale != null && scale != 0 ) {
						throw new IllegalArgumentException("scale has no meaning for timestamps");
					}
					break;
				case SqlTypes.NUMERIC:
				case SqlTypes.DECIMAL:
				case SqlTypes.INTERVAL_SECOND:
					size.setPrecision( javaType.getDefaultSqlPrecision( Dialect.this, jdbcType ) );
					break;
			}

			size.setScale( javaType.getDefaultSqlScale( Dialect.this, jdbcType ) );

			if ( precision != null ) {
				size.setPrecision( precision );
			}
			if ( scale != null ) {
				size.setScale( scale );
			}
			if ( length != null ) {
				size.setLength( length );
			}
			return size;
		}
	}

	/**
	 * Translate the given datetime format string from
	 * the pattern language defined by Java's
	 * {@link java.time.format.DateTimeFormatter} to
	 * whatever pattern language is understood by the
	 * native datetime formatting function for this
	 * database (often the {@code to_char()} function).
	 * <p>
	 * Since it's never possible to translate all of
	 * the pattern letter sequences understood by
	 * {@code DateTimeFormatter}, only the following
	 * subset of pattern letters is accepted by
	 * Hibernate:
	 * <ul>
	 *     <li>G: era</li>
	 *     <li>y: year of era</li>
	 *     <li>Y: year of week-based year</li>
	 *     <li>M: month of year</li>
	 *     <li>w: week of week-based year (ISO week number)</li>
	 *     <li>W: week of month</li>
	 *     <li>E: day of week (name)</li>
	 *     <li>e: day of week (number)</li>
	 *     <li>d: day of month</li>
	 *     <li>D: day of year</li>
	 *     <li>a: AM/PM</li>
	 *     <li>H: hour of day (24 hour time)</li>
	 *     <li>h: hour of AM/PM (12 hour time)</li>
	 *     <li>m: minutes</li>
	 *     <li>s: seconds</li>
	 *     <li>z,Z,x: timezone offset</li>
	 * </ul>
	 * In addition, punctuation characters and
	 * single-quoted literal strings are accepted.
	 * <p>
	 * Appends a pattern accepted by the function that
	 * formats dates and times in this dialect to a
	 * SQL fragment that is being constructed.
	 */
	public void appendDatetimeFormat(SqlAppender appender, String format) {
		//most databases support a datetime format
		//copied from Oracle's to_char() function,
		//with some minor variation
		appender.appendSql( OracleDialect.datetimeFormat( format, true, false ).result() );
	}

	/**
	 * Return the name used to identify the given field
	 * as an argument to the {@code extract()} function,
	 * or of this dialect's {@linkplain #extractPattern equivalent}
	 * function.
	 * <p>
	 * This method does not need to handle
	 * {@link TemporalUnit#NANOSECOND},
	 * {@link TemporalUnit#NATIVE},
	 * {@link TemporalUnit#OFFSET},
	 * {@link TemporalUnit#DATE},
	 * {@link TemporalUnit#TIME},
	 * {@link TemporalUnit#WEEK_OF_YEAR}, nor
	 * {@link TemporalUnit#WEEK_OF_MONTH},
	 * which are already desugared by
	 * {@link ExtractFunction}.
	 */
	public String translateExtractField(TemporalUnit unit) {
		switch ( unit ) {
			case DAY_OF_MONTH: return "dd";
			case DAY_OF_YEAR: return "dy";
			case DAY_OF_WEEK: return "dw";

			//all the following fields are desugared
			//by ExtractFunction, so we should never
			//see them here!
			case OFFSET:
			case NATIVE:
			case NANOSECOND:
			case DATE:
			case TIME:
			case WEEK_OF_MONTH:
			case WEEK_OF_YEAR:
				throw new IllegalArgumentException("illegal field: " + unit);

			default: return unit.toString();
		}
	}

	/**
	 * Return the name used to identify the given unit of
	 * duration as an argument to {@code #timestampadd()}
	 * or {@code #timestampdiff()}, or of this dialect's
	 * {@linkplain #timestampaddPattern equivalent}
	 * {@linkplain #timestampdiffPattern functions}.
	 * <p>
	 * This method does not need to handle
	 * {@link TemporalUnit#NANOSECOND},
	 * {@link TemporalUnit#NATIVE},
	 * {@link TemporalUnit#OFFSET},
	 * {@link TemporalUnit#DAY_OF_WEEK},
	 * {@link TemporalUnit#DAY_OF_MONTH},
	 * {@link TemporalUnit#DAY_OF_YEAR},
	 * {@link TemporalUnit#DATE},
	 * {@link TemporalUnit#TIME},
	 * {@link TemporalUnit#TIMEZONE_HOUR},
	 * {@link TemporalUnit#TIMEZONE_MINUTE},
	 * {@link TemporalUnit#WEEK_OF_YEAR}, nor
	 * {@link TemporalUnit#WEEK_OF_MONTH},
	 * which are not units of duration.
	 */
	public String translateDurationField(TemporalUnit unit) {
		switch ( unit ) {
			case DAY_OF_MONTH:
			case DAY_OF_YEAR:
			case DAY_OF_WEEK:
			case OFFSET:
			case TIMEZONE_HOUR:
			case TIMEZONE_MINUTE:
			case DATE:
			case TIME:
			case WEEK_OF_MONTH:
			case WEEK_OF_YEAR:
				throw new IllegalArgumentException("illegal unit: " + unit);

			case NATIVE: return "nanosecond"; //default to nanosecond for now
			default: return unit.toString();
		}
	}

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

	public void appendDateTimeLiteral(
			SqlAppender appender,
			Calendar calendar,
			TemporalType precision,
			TimeZone jdbcTimeZone) {
		switch ( precision ) {
			case DATE:
				appender.appendSql( JDBC_ESCAPE_START_DATE );
				appendAsDate( appender, calendar );
				appender.appendSql( JDBC_ESCAPE_END );
				break;
			case TIME:
				appender.appendSql( JDBC_ESCAPE_START_TIME );
				appendAsTime( appender, calendar );
				appender.appendSql( JDBC_ESCAPE_END );
				break;
			case TIMESTAMP:
				appender.appendSql( JDBC_ESCAPE_START_TIMESTAMP );
				appendAsTimestampWithMicros( appender, calendar, jdbcTimeZone );
				appender.appendSql( JDBC_ESCAPE_END );
				break;
			default:
				throw new IllegalArgumentException();
		}
	}

	public void appendIntervalLiteral(SqlAppender appender, Duration literal) {
		appender.appendSql( "interval '" );
		appender.appendSql( literal.getSeconds() );
		appender.appendSql( '.' );
		appender.appendSql( literal.getNano() );
		appender.appendSql( '\'' );
	}

	/**
	 * Whether the Dialect supports timezone offset in temporal literals.
	 */
	public boolean supportsTemporalLiteralOffset() {
		return false;
	}

	/**
	 * How the Dialect supports time zone types like {@link Types#TIMESTAMP_WITH_TIMEZONE}.
	 */
	public TimeZoneSupport getTimeZoneSupport() {
		return TimeZoneSupport.NONE;
	}
}
