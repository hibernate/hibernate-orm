/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import java.io.IOException;
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
import java.sql.ResultSetMetaData;
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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
import java.util.regex.Pattern;

import org.hibernate.HibernateException;
import org.hibernate.Incubating;
import org.hibernate.Length;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.ScrollMode;
import org.hibernate.boot.TempTableDdlTransactionHandling;
import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.FunctionContributor;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.boot.model.TypeContributor;
import org.hibernate.boot.model.relational.AuxiliaryDatabaseObject;
import org.hibernate.boot.model.relational.Sequence;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.dialect.aggregate.AggregateSupport;
import org.hibernate.dialect.aggregate.AggregateSupportImpl;
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
import org.hibernate.dialect.unique.AlterTableUniqueDelegate;
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
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.exception.spi.ConversionContext;
import org.hibernate.exception.spi.SQLExceptionConversionDelegate;
import org.hibernate.exception.spi.SQLExceptionConverter;
import org.hibernate.exception.spi.ViolatedConstraintNameExtractor;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.MathHelper;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.loader.ast.spi.MultiKeyLoadSizingStrategy;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Constraint;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.Index;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.UserDefinedType;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.persister.entity.Lockable;
import org.hibernate.persister.entity.mutation.EntityMutationTarget;
import org.hibernate.procedure.internal.StandardCallableStatementSupport;
import org.hibernate.procedure.spi.CallableStatementSupport;
import org.hibernate.query.Query;
import org.hibernate.query.hql.HqlTranslator;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.sqm.CastType;
import org.hibernate.query.sqm.FetchClauseType;
import org.hibernate.query.sqm.IntervalType;
import org.hibernate.query.sqm.TemporalUnit;
import org.hibernate.query.sqm.TrimSpec;
import org.hibernate.query.sqm.mutation.internal.temptable.AfterUseAction;
import org.hibernate.query.sqm.mutation.internal.temptable.BeforeUseAction;
import org.hibernate.query.sqm.mutation.internal.temptable.PersistentTableInsertStrategy;
import org.hibernate.query.sqm.mutation.internal.temptable.PersistentTableMutationStrategy;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableInsertStrategy;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableMutationStrategy;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableMutationStrategyProvider;
import org.hibernate.query.sqm.sql.SqmTranslatorFactory;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.sql.ForUpdateFragment;
import org.hibernate.sql.ast.SqlAstNodeRenderingMode;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.internal.ParameterMarkerStrategyStandard;
import org.hibernate.sql.ast.spi.ParameterMarkerStrategy;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.spi.StringBuilderSqlAppender;
import org.hibernate.sql.model.MutationOperation;
import org.hibernate.sql.model.internal.OptionalTableUpdate;
import org.hibernate.sql.model.jdbc.OptionalTableUpdateOperation;
import org.hibernate.tool.schema.extract.internal.SequenceInformationExtractorLegacyImpl;
import org.hibernate.tool.schema.extract.internal.SequenceInformationExtractorNoOpImpl;
import org.hibernate.tool.schema.extract.spi.ColumnTypeInformation;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractor;
import org.hibernate.tool.schema.internal.HibernateSchemaManagementTool;
import org.hibernate.tool.schema.internal.StandardAuxiliaryDatabaseObjectExporter;
import org.hibernate.tool.schema.internal.StandardForeignKeyExporter;
import org.hibernate.tool.schema.internal.StandardIndexExporter;
import org.hibernate.tool.schema.internal.StandardSequenceExporter;
import org.hibernate.tool.schema.internal.StandardTableCleaner;
import org.hibernate.tool.schema.internal.StandardTableExporter;
import org.hibernate.tool.schema.internal.StandardTableMigrator;
import org.hibernate.tool.schema.internal.StandardUniqueKeyExporter;
import org.hibernate.tool.schema.internal.StandardUserDefinedTypeExporter;
import org.hibernate.tool.schema.internal.TableMigrator;
import org.hibernate.tool.schema.spi.Cleaner;
import org.hibernate.tool.schema.spi.Exporter;
import org.hibernate.tool.schema.spi.SchemaManagementTool;
import org.hibernate.type.BasicType;
import org.hibernate.type.BasicTypeRegistry;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.PrimitiveByteArrayJavaType;
import org.hibernate.type.descriptor.jdbc.ArrayJdbcTypeConstructor;
import org.hibernate.type.descriptor.jdbc.BlobJdbcType;
import org.hibernate.type.descriptor.jdbc.ClobJdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcLiteralFormatter;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.LongNVarcharJdbcType;
import org.hibernate.type.descriptor.jdbc.NCharJdbcType;
import org.hibernate.type.descriptor.jdbc.NClobJdbcType;
import org.hibernate.type.descriptor.jdbc.NVarcharJdbcType;
import org.hibernate.type.descriptor.jdbc.TimeUtcAsJdbcTimeJdbcType;
import org.hibernate.type.descriptor.jdbc.TimeUtcAsOffsetTimeJdbcType;
import org.hibernate.type.descriptor.jdbc.TimestampUtcAsJdbcTimestampJdbcType;
import org.hibernate.type.descriptor.jdbc.TimestampUtcAsOffsetDateTimeJdbcType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;
import org.hibernate.type.descriptor.sql.internal.ArrayDdlTypeImpl;
import org.hibernate.type.descriptor.sql.internal.CapacityDependentDdlType;
import org.hibernate.type.descriptor.sql.internal.DdlTypeImpl;
import org.hibernate.type.descriptor.sql.spi.DdlTypeRegistry;
import org.hibernate.type.spi.TypeConfiguration;

import org.jboss.logging.Logger;

import jakarta.persistence.TemporalType;

import static java.lang.Math.ceil;
import static java.lang.Math.log;
import static org.hibernate.cfg.AvailableSettings.NON_CONTEXTUAL_LOB_CREATION;
import static org.hibernate.cfg.AvailableSettings.STATEMENT_BATCH_SIZE;
import static org.hibernate.cfg.AvailableSettings.USE_GET_GENERATED_KEYS;
import static org.hibernate.internal.util.StringHelper.parseCommaSeparatedString;
import static org.hibernate.internal.util.collections.ArrayHelper.EMPTY_STRING_ARRAY;
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
import static org.hibernate.type.SqlTypes.ROWID;
import static org.hibernate.type.SqlTypes.SMALLINT;
import static org.hibernate.type.SqlTypes.TIME;
import static org.hibernate.type.SqlTypes.TIMESTAMP;
import static org.hibernate.type.SqlTypes.TIMESTAMP_UTC;
import static org.hibernate.type.SqlTypes.TIMESTAMP_WITH_TIMEZONE;
import static org.hibernate.type.SqlTypes.TIME_UTC;
import static org.hibernate.type.SqlTypes.TIME_WITH_TIMEZONE;
import static org.hibernate.type.SqlTypes.TINYINT;
import static org.hibernate.type.SqlTypes.VARBINARY;
import static org.hibernate.type.SqlTypes.VARCHAR;
import static org.hibernate.type.SqlTypes.isEnumType;
import static org.hibernate.type.SqlTypes.isFloatOrRealOrDouble;
import static org.hibernate.type.SqlTypes.isNumericOrDecimal;
import static org.hibernate.type.SqlTypes.isVarbinaryType;
import static org.hibernate.type.SqlTypes.isVarcharType;
import static org.hibernate.type.descriptor.DateTimeUtils.JDBC_ESCAPE_END;
import static org.hibernate.type.descriptor.DateTimeUtils.JDBC_ESCAPE_START_DATE;
import static org.hibernate.type.descriptor.DateTimeUtils.JDBC_ESCAPE_START_TIME;
import static org.hibernate.type.descriptor.DateTimeUtils.JDBC_ESCAPE_START_TIMESTAMP;
import static org.hibernate.type.descriptor.DateTimeUtils.appendAsDate;
import static org.hibernate.type.descriptor.DateTimeUtils.appendAsLocalTime;
import static org.hibernate.type.descriptor.DateTimeUtils.appendAsTime;
import static org.hibernate.type.descriptor.DateTimeUtils.appendAsTimestampWithMillis;
import static org.hibernate.type.descriptor.DateTimeUtils.appendAsTimestampWithNanos;
import static org.hibernate.type.descriptor.converter.internal.EnumHelper.getEnumeratedValues;

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
 *     <li>{@link #columnType(int)} to define a mapping from SQL
 *     {@linkplain SqlTypes type codes} to database column types, and
 *     <li>{@link #initializeFunctionRegistry(FunctionContributions)} to
 *     register mappings for standard HQL functions with the
 *     {@link org.hibernate.query.sqm.function.SqmFunctionRegistry}.
 * </ul>
 * <p>
 * A subclass representing a dialect of SQL which deviates significantly
 * from ANSI SQL will certainly override many additional operations.
 * <p>
 * Subclasses should be thread-safe and immutable.
 * <p>
 * Since Hibernate 6, a single subclass of {@code Dialect} represents all
 * releases of a given product-specific SQL dialect. The version of the
 * database is exposed at runtime via the {@link DialectResolutionInfo}
 * passed to the constructor, and by the {@link #getVersion()} property.
 * <p>
 * Programs using Hibernate should migrate away from the use of versioned
 * dialect classes like, for example, {@code MySQL8Dialect}. These
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
public abstract class Dialect implements ConversionContext, TypeContributor, FunctionContributor {

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
		this.version = info.makeCopyOrDefault( getMinimumSupportedVersion() );
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
	 * <p>
	 * This default implementation sets
	 * {@value org.hibernate.cfg.AvailableSettings#STATEMENT_BATCH_SIZE},
	 * {@value org.hibernate.cfg.AvailableSettings#NON_CONTEXTUAL_LOB_CREATION},
	 * and {@value org.hibernate.cfg.AvailableSettings#USE_GET_GENERATED_KEYS}
	 * to defaults determined by calling
	 * {@link #getDefaultStatementBatchSize()},
	 * {@link #getDefaultNonContextualLobCreation()},
	 * and {@link #getDefaultUseGetGeneratedKeys()}.
	 * <p>
	 * An implementation may set additional configuration properties, but
	 * this is discouraged.
	 */
	protected void initDefaultProperties() {
		getDefaultProperties().setProperty( STATEMENT_BATCH_SIZE,
				Integer.toString( getDefaultStatementBatchSize() ) );
		getDefaultProperties().setProperty( NON_CONTEXTUAL_LOB_CREATION,
				Boolean.toString( getDefaultNonContextualLobCreation() ) );
		getDefaultProperties().setProperty( USE_GET_GENERATED_KEYS,
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
		ddlTypeRegistry.addDescriptor( simpleSqlType( TIME_UTC ) );
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

		if ( supportsStandardArrays() ) {
			ddlTypeRegistry.addDescriptor( new ArrayDdlTypeImpl( this, false ) );
		}
		if ( rowId( null ) != null ) {
			ddlTypeRegistry.addDescriptor( simpleSqlType( ROWID ) );
		}
	}

	protected boolean isLob(int sqlTypeCode) {
		switch ( sqlTypeCode ) {
			case LONG32VARBINARY:
			case LONG32VARCHAR:
			case LONG32NVARCHAR:
			case BLOB:
			case CLOB:
			case NCLOB:
				return true;
			default:
				return false;
		}
	}

	private DdlTypeImpl simpleSqlType(int sqlTypeCode) {
		return new DdlTypeImpl(
				sqlTypeCode,
				isLob( sqlTypeCode ),
				columnType( sqlTypeCode ),
				castType( sqlTypeCode ),
				this
		);
	}

	/**
	 * Obtain a builder object for a family of capacity-dependent SQL types.
	 *
	 * @param sqlTypeCode the JDBC type code abstracting over the capacity-limited types
	 * @param biggestSqlTypeCode the real JDBC type code of the largest type
	 * @param castTypeCode the real JDBC type code to use to look at the type to use in typecasts
	 * @return the builder object
	 */
	private CapacityDependentDdlType.Builder sqlTypeBuilder(int sqlTypeCode, int biggestSqlTypeCode, int castTypeCode) {
		return CapacityDependentDdlType.builder(
				sqlTypeCode,
				isLob( sqlTypeCode )
						? CapacityDependentDdlType.LobKind.ALL_LOB
						: isLob( biggestSqlTypeCode )
								? CapacityDependentDdlType.LobKind.BIGGEST_LOB
								: CapacityDependentDdlType.LobKind.NONE,
				columnType( biggestSqlTypeCode ),
				castType( castTypeCode ),
				this
		);
	}

	/**
	 * The database column type name for a given JDBC type code defined
	 * in {@link Types} or {@link SqlTypes}. This default implementation
	 * returns the ANSI-standard type name.
	 * <p>
	 * This method may be overridden by concrete {@code Dialect}s as an
	 * alternative to
	 * {@link #registerColumnTypes(TypeContributions, ServiceRegistry)}
	 * for simple registrations.
	 * <p>
	 * Note that:
	 * <ol>
	 * <li> Implementations of this method are expected to define a
	 *      sensible mapping for{@link Types#NCLOB} {@link Types#NCHAR},
	 *      and {@link Types#NVARCHAR}. On some database, these types
	 *      are simply remapped to {@code CLOB}, {@code CHAR}, and
	 *      {@code VARCHAR}.
	 * <li> Mappings for {@link Types#TIMESTAMP} and
	 *      {@link Types#TIMESTAMP_WITH_TIMEZONE} should support explicit
	 *      specification of precision if possible.
	 * <li> As specified by {@link DdlTypeRegistry#getDescriptor(int)},
	 *      this method never receives {@link Types#LONGVARCHAR},
	 *      {@link Types#LONGNVARCHAR}, nor {@link Types#LONGVARBINARY},
	 *      which are considered synonyms for their non-{@code LONG}
	 *      counterparts.
	 * <li> On the other hand, the types {@link SqlTypes#LONG32VARCHAR},
	 *      {@link SqlTypes#LONG32NVARCHAR}, and
	 *      {@link SqlTypes#LONG32VARBINARY} are <em>not</em> synonyms,
	 *      and implementations of this method must define sensible
	 *      mappings, for example to database-native {@code TEXT} or
	 *      {@code CLOB} types.
	 * </ol>
	 *
	 * @param sqlTypeCode a SQL {@link SqlTypes type code}
	 * @return a column type name, with {@code $l}, {@code $p}, {@code $s}
	 *         placeholders for length, precision, scale
	 *
	 * @see SqlTypes
	 */
	protected String columnType(int sqlTypeCode) {
		switch ( sqlTypeCode ) {
			case ROWID:
				return "rowid";

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
				return "time($p)";
			case TIME_WITH_TIMEZONE:
				// type included here for completeness but note that
				// very few databases support it, and the general
				// advice is to caution against its use (for reasons,
				// check the comments in the Postgres documentation).
				return "time($p) with time zone";
			case TIMESTAMP:
				return "timestamp($p)";
			case TIMESTAMP_WITH_TIMEZONE:
				return "timestamp($p) with time zone";
			case TIME_UTC:
				return getTimeZoneSupport() == TimeZoneSupport.NATIVE
						? columnType( TIME_WITH_TIMEZONE )
						: columnType( TIME );
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

	/**
	 * The SQL type to use in {@code cast( ... as ... )} expressions when
	 * casting to the target type represented by the given JDBC type code.
	 *
	 * @param sqlTypeCode The JDBC type code representing the target type
	 * @return The SQL type to use in {@code cast()}
	 */
	protected String castType(int sqlTypeCode) {
		return columnType( sqlTypeCode );
	}

	/**
	 * Register the reserved words of ANSI-standard SQL as keywords.
	 *
	 * @see AnsiSqlKeywords
	 */
	protected void registerDefaultKeywords() {
		AnsiSqlKeywords keywords = new AnsiSqlKeywords();
		//Not using #registerKeyword as:
		// # these are already lowercase
		// # better efficiency of addAll as it can pre-size the collections
		sqlKeywords.addAll( keywords.sql2003() );
	}

	/**
	 * Register the reserved words
	 * {@linkplain java.sql.DatabaseMetaData#getSQLKeywords() reported}
	 * by the JDBC driver as keywords.
	 *
	 * @see java.sql.DatabaseMetaData#getSQLKeywords()
	 */
	protected void registerKeywords(DialectResolutionInfo info) {
		for ( String keyword : parseCommaSeparatedString( info.getSQLKeywords() ) ) {
			registerKeyword( keyword );
		}
	}

	/**
	 * Get the version of the SQL dialect that is the target of this instance.
	 */
	public DatabaseVersion getVersion() {
		return version;
	}

	/**
	 * Get the version of the SQL dialect that is the minimum supported by this implementation.
	 */
	protected DatabaseVersion getMinimumSupportedVersion() {
		return SimpleDatabaseVersion.ZERO_VERSION;
	}

	/**
	 * Resolves the {@link SqlTypes} type code for the given column
	 * type name as reported by the database, or <code>null</code>
	 * if it can't be resolved.
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
	 * Resolves the {@link SqlTypes} type code for the given column
	 * type name as reported by the database and the base type name
	 * (i.e. without precision, length and scale), or <code>null</code>
	 * if it can't be resolved.
	 */
	protected Integer resolveSqlTypeCode(String typeName, String baseTypeName, TypeConfiguration typeConfiguration) {
		return typeConfiguration.getDdlTypeRegistry().getSqlTypeCode( baseTypeName );
	}

	/**
	 * Assigns an appropriate {@link JdbcType} to a column of a JDBC
	 * result set based on the column type name, JDBC type code,
	 * precision, and scale.
	 *
	 * @param columnTypeName the column type name
	 * @param jdbcTypeCode the {@link SqlTypes type code}
	 * @param precision the precision or 0
	 * @param scale the scale or 0
	 * @return an appropriate instance of {@link JdbcType}
	 */
	public JdbcType resolveSqlTypeDescriptor(
			String columnTypeName,
			int jdbcTypeCode,
			int precision,
			int scale,
			JdbcTypeRegistry jdbcTypeRegistry) {
		if ( jdbcTypeCode == ARRAY ) {
			// Special handling for array types, because we need the proper element/component type
			// To determine the element JdbcType, we pass the database reported type to #resolveSqlTypeCode
			final int arraySuffixIndex = columnTypeName.toLowerCase( Locale.ROOT ).indexOf( " array" );
			if ( arraySuffixIndex != -1 ) {
				final String componentTypeName = columnTypeName.substring( 0, arraySuffixIndex );
				final Integer sqlTypeCode = resolveSqlTypeCode( componentTypeName, jdbcTypeRegistry.getTypeConfiguration() );
				if ( sqlTypeCode != null ) {
					return jdbcTypeRegistry.resolveTypeConstructorDescriptor(
							jdbcTypeCode,
							jdbcTypeRegistry.getDescriptor( sqlTypeCode ),
							ColumnTypeInformation.EMPTY
					);
				}
			}
		}
		return jdbcTypeRegistry.getDescriptor( jdbcTypeCode );
	}

	/**
	 * Determine the length/precision of a column based on information in the
	 * JDBC {@link java.sql.ResultSetMetaData}. Note that what JDBC reports
	 * as a "precision" {@linkplain java.sql.ResultSetMetaData#getPrecision
	 * might actually be the column length}.
	 *
	 * @param columnTypeName the name of the column type
	 * @param jdbcTypeCode the JDBC type code of the column type
	 * @param precision the (numeric) precision or (character) length of the column
	 * @param scale the scale of a numeric column
	 * @param displaySize the {@linkplain java.sql.ResultSetMetaData#getColumnDisplaySize
	 *                    display size} of the column
	 * @return the precision or length of the column
	 */
	public int resolveSqlTypeLength(
			String columnTypeName,
			int jdbcTypeCode,
			int precision,
			int scale,
			int displaySize) {
		return precision;
	}

	/**
	 * If this database has a special MySQL-style {@code enum} column type,
	 * return the type declaration for the given enumeration of values.
	 * <p>
	 * If the database has no such type, return {@code null}.
	 *
	 * @param values the enumerated values of the type
	 * @return the DDL column type declaration
	 */
	public String getEnumTypeDeclaration(String name, String[] values) {
		return null;
	}

	public String getEnumTypeDeclaration(Class<? extends Enum<?>> enumType) {
		return getEnumTypeDeclaration( enumType.getSimpleName(), getEnumeratedValues( enumType ) );
	}

	public String[] getCreateEnumTypeCommand(String name, String[] values) {
		return EMPTY_STRING_ARRAY;
	}

	public String[] getCreateEnumTypeCommand(Class<? extends Enum<?>> enumType) {
		return getCreateEnumTypeCommand( enumType.getSimpleName(), getEnumeratedValues( enumType ) );
	}

	public String[] getDropEnumTypeCommand(String name) {
		return EMPTY_STRING_ARRAY;
	}

	public String[] getDropEnumTypeCommand(Class<? extends Enum<?>> enumType) {
		return getDropEnumTypeCommand( enumType.getSimpleName() );
	}

	/**
	 * Render a SQL check condition for a column that represents an enumerated value
	 * by its {@linkplain jakarta.persistence.EnumType#STRING string representation}
	 * or a given list of values (with NULL value allowed).
	 *
	 * @return a SQL expression that will occur in a {@code check} constraint
	 */
	public String getCheckCondition(String columnName, String[] values) {
		StringBuilder check = new StringBuilder();
		check.append( columnName ).append( " in (" );
		String separator = "";
		boolean nullIsValid = false;
		for ( String value : values ) {
			if ( value == null ) {
				nullIsValid = true;
				continue;
			}
			check.append( separator ).append('\'').append( value ).append('\'');
			separator = ",";
		}
		check.append( ')' );
		if ( nullIsValid ) {
			check.append( " or " ).append( columnName ).append( " is null" );
		}
		return check.toString();
	}

	public String getCheckCondition(String columnName, Class<? extends Enum<?>> enumType) {
		return getCheckCondition( columnName, getEnumeratedValues( enumType ) );
	}

	/**
	 * Render a SQL check condition for a column that represents an enumerated value.
	 * by its {@linkplain jakarta.persistence.EnumType#ORDINAL ordinal representation}.
	 *
	 * @return a SQL expression that will occur in a {@code check} constraint
	 */
	public String getCheckCondition(String columnName, long min, long max) {
		return columnName + " between " + min + " and " + max;
	}

	/**
	 * Render a SQL check condition for a column that represents an enumerated value
	 * by its {@linkplain jakarta.persistence.EnumType#ORDINAL ordinal representation}.
	 *
	 * @return a SQL expression that will occur in a {@code check} constraint
	 * @deprecated use {@link #getCheckCondition(String, Long[])} instead
	 */
	@Deprecated(forRemoval = true)
	public String getCheckCondition(String columnName, long[] values) {
		Long[] boxedValues = new Long[values.length];
		for ( int i = 0; i<values.length; i++ ) {
			boxedValues[i] = values[i];
		}
		return getCheckCondition( columnName, boxedValues );
	}

	/**
	 * Render a SQL check condition for a column that represents an enumerated value
	 * by its {@linkplain jakarta.persistence.EnumType#ORDINAL ordinal representation}
	 * or a given list of values.
	 *
	 * @return a SQL expression that will occur in a {@code check} constraint
	 */
	public String getCheckCondition(String columnName, Long[] values) {
		StringBuilder check = new StringBuilder();
		check.append( columnName ).append( " in (" );
		String separator = "";
		boolean nullIsValid = false;
		for ( Long value : values ) {
			if ( value == null ) {
				nullIsValid = true;
				continue;
			}
			check.append( separator ).append( value );
			separator = ",";
		}
		check.append( ')' );
		if ( nullIsValid ) {
			check.append( " or " ).append( columnName ).append( " is null" );
		}
		return check.toString();
	}

	@Override
	public void contributeFunctions(FunctionContributions functionContributions) {
		initializeFunctionRegistry( functionContributions );
	}

	@Override
	public int ordinal() {
		// dialect-contributed functions come first
		return 0;
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
	 * <li> <code>avg(arg)</code>						- aggregate function
	 * <li> <code>count([distinct ]arg)</code>			- aggregate function
	 * <li> <code>max(arg)</code>						- aggregate function
	 * <li> <code>min(arg)</code>						- aggregate function
	 * <li> <code>sum(arg)</code>						- aggregate function
	 * </ul>
	 *
	 * <ul>
	 * <li> <code>coalesce(arg0, arg1, ...)</code>
	 * <li> <code>nullif(arg0, arg1)</code>
	 * </ul>
	 *
	 * <ul>
	 * <li> <code>lower(arg)</code>
	 * <li> <code>upper(arg)</code>
	 * <li> <code>length(arg)</code>
	 * <li> <code>concat(arg0, arg1, ...)</code>
	 * <li> <code>locate(pattern, string[, start])</code>
	 * <li> <code>substring(string, start[, length])</code>
	 * <li> <code>trim([[spec ][character ]from] string)</code>
	 * </ul>
	 *
	 * <ul>
	 * <li> <code>abs(arg)</code>
	 * <li> <code>mod(arg0, arg1)</code>
	 * <li> <code>sqrt(arg)</code>
	 * </ul>
	 *
	 * <ul>
	 * <li> <code>current date</code>
	 * <li> <code>current time</code>
	 * <li> <code>current timestamp</code>
	 * </ul>
	 *
	 * Along with an additional set of functions defined by ANSI SQL:
	 *
	 * <ul>
	 * <li> <code>any(arg)</code>						- aggregate function
	 * <li> <code>every(arg)</code>						- aggregate function
	 * </ul>
	 * <ul>
	 * <li> <code>var_samp(arg)</code>					- aggregate function
	 * <li> <code>var_pop(arg)</code>					- aggregate function
	 * <li> <code>stddev_samp(arg)</code>				- aggregate function
	 * <li> <code>stddev_pop(arg)</code>				- aggregate function
	 * </ul>
	 *
	 * <ul>
	 * <li> <code>cast(arg as Type)</code>
	 * <li> <code>extract(field from arg)</code>
	 * </ul>
	 *
	 * <ul>
	 * <li> <code>ln(arg)</code>
	 * <li> <code>exp(arg)</code>
	 * <li> <code>power(arg0, arg1)</code>
	 * <li> <code>floor(arg)</code>
	 * <li> <code>ceiling(arg)</code>
	 * </ul>
	 *
	 * <ul>
	 * <li> <code>position(pattern in string)</code>
	 * <li> <code>substring(string from start[ for length])</code>
	 * <li> <code>overlay(string placing replacement from start[ for length])</code>
	 * </ul>
	 *
	 * And the following functions for working with <code>java.time</code>
	 * types:
	 *
	 * <ul>
	 * <li> <code>local date</code>
	 * <li> <code>local time</code>
	 * <li> <code>local datetime</code>
	 * <li> <code>offset datetime</code>
	 * <li> <code>instant</code>
	 * </ul>
	 *
	 * And a number of additional "standard" functions:
	 *
	 * <ul>
	 * <li> <code>left(string, length)</code>
	 * <li> <code>right(string, length)</code>
	 * <li> <code>replace(string, pattern, replacement)</code>
	 * <li> <code>pad(string with length spec[ character])</code>
	 * <li> <code>repeat(string, times)</code>
	 * </ul>
	 *
	 * <ul>
	 * <li> <code>pi</code>
	 * <li> <code>log10(arg)</code>
	 * <li> <code>log(base, arg)</code>
	 * <li> <code>sign(arg)</code>
	 * <li> <code>sin(arg)</code>
	 * <li> <code>cos(arg)</code>
	 * <li> <code>tan(arg)</code>
	 * <li> <code>asin(arg)</code>
	 * <li> <code>acos(arg)</code>
	 * <li> <code>atan(arg)</code>
	 * <li> <code>atan2(arg0, arg1)</code>
	 * <li> <code>round(arg0[, arg1])</code>
	 * <li> <code>truncate(arg0[, arg1])</code>
	 * <li> <code>sinh(arg)</code>
	 * <li> <code>tanh(arg)</code>
	 * <li> <code>cosh(arg)</code>
	 * <li> <code>least(arg0, arg1, ...)</code>
	 * <li> <code>greatest(arg0, arg1, ...)</code>
	 * <li> <code>degrees(arg)</code>
	 * <li> <code>radians(arg)</code>
	 * <li> <code>bitand(arg1, arg1)</code>
	 * <li> <code>bitor(arg1, arg1)</code>
	 * <li> <code>bitxor(arg1, arg1)</code>
	 * </ul>
	 *
	 * <ul>
	 * <li> <code>format(datetime as pattern)</code>
	 * <li> <code>collate(string as collation)</code>
	 * <li> <code>str(arg)</code>						- synonym of <code>cast(a as String)</code>
	 * <li> <code>ifnull(arg0, arg1)</code>				- synonym of <code>coalesce(a, b)</code>
	 * </ul>
	 *
	 * Finally, the following functions are defined as abbreviations for
	 * <code>extract()</code>, and desugared by the parser:
	 *
	 * <ul>
	 * <li> <code>second(arg)</code>					- synonym of <code>extract(second from a)</code>
	 * <li> <code>minute(arg)</code>					- synonym of <code>extract(minute from a)</code>
	 * <li> <code>hour(arg)</code>						- synonym of <code>extract(hour from a)</code>
	 * <li> <code>day(arg)</code>						- synonym of <code>extract(day from a)</code>
	 * <li> <code>month(arg)</code>						- synonym of <code>extract(month from a)</code>
	 * <li> <code>year(arg)</code>						- synonym of <code>extract(year from a)</code>
	 * </ul>
	 *
	 * Note that according to this definition, the <code>second()</code>
	 * function returns a floating point value, contrary to the integer
	 * type returned by the native function with this name on many databases.
	 * Thus, we don't just naively map these HQL functions to the native SQL
	 * functions with the same names.
	 */
	public void initializeFunctionRegistry(FunctionContributions functionContributions) {
		final TypeConfiguration typeConfiguration = functionContributions.getTypeConfiguration();
		final BasicTypeRegistry basicTypeRegistry = typeConfiguration.getBasicTypeRegistry();
		final BasicType<Date> timestampType = basicTypeRegistry.resolve( StandardBasicTypes.TIMESTAMP );
		final BasicType<Date> dateType = basicTypeRegistry.resolve( StandardBasicTypes.DATE );
		final BasicType<Date> timeType = basicTypeRegistry.resolve( StandardBasicTypes.TIME );
		final BasicType<Instant> instantType = basicTypeRegistry.resolve( StandardBasicTypes.INSTANT );
		final BasicType<OffsetDateTime> offsetDateTimeType = basicTypeRegistry.resolve( StandardBasicTypes.OFFSET_DATE_TIME );
		final BasicType<LocalDateTime> localDateTimeType = basicTypeRegistry.resolve( StandardBasicTypes.LOCAL_DATE_TIME );
		final BasicType<LocalTime> localTimeType = basicTypeRegistry.resolve( StandardBasicTypes.LOCAL_TIME );
		final BasicType<LocalDate> localDateType = basicTypeRegistry.resolve( StandardBasicTypes.LOCAL_DATE );

		CommonFunctionFactory functionFactory = new CommonFunctionFactory(functionContributions);

		//standard aggregate functions count(), sum(), max(), min(), avg(),
		//supported on every database

		//Note that we don't include median() in this list, since it's difficult
		//to implement on MySQL and Sybase ASE

		functionFactory.aggregates( this, SqlAstNodeRenderingMode.DEFAULT );

		//the ANSI SQL-defined aggregate functions any() and every() are only
		//supported on one database, but can be emulated using sum() and case,
		//though there is a more natural mapping on some databases

		functionFactory.everyAny_sumCase( supportsPredicateAsExpression() );

		//math functions supported on almost every database

		//Note that while certain mathematical functions return the same type
		//as their arguments, this is not the case in general - any function
		//involving exponentiation by a non-integer power, logarithms,
		//trigonometric functions, etc., should be considered to be of type
		//Double. In particular, there is no meaningful concept of an "exact
		//decimal" version of these functions, and if any database attempted
		//to implement such a silly thing, it would be dog slow.

		functionFactory.math();
		functionFactory.round();

		//trig functions supported on almost every database

		functionFactory.trigonometry();

		//hyperbolic sinh and tanh are very useful but not supported on most
		//databases, so emulate them here (cosh along for the ride)

		functionFactory.sinh_exp();
		functionFactory.cosh_exp();
		functionFactory.tanh_exp();

		//pi supported on most databases, but emulate it here

		functionFactory.pi_acos();

		//log(base, arg) supported on most databases, but emulate it here

		functionFactory.log_ln();

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

		functionContributions.getFunctionRegistry().register( "position",
				new LocatePositionEmulation( typeConfiguration ) );

		//very few databases support ANSI-style overlay() function, so emulate
		//it here in terms of either insert() or concat()/substring()

		functionContributions.getFunctionRegistry().register( "overlay",
				new InsertSubstringOverlayEmulation( typeConfiguration, false ) );

		//ANSI SQL trim() function is supported on almost all of the databases
		//we care about, but on some it must be emulated using ltrim(), rtrim(),
		//and replace()

		functionContributions.getFunctionRegistry().register( "trim",
				new TrimFunction( this, typeConfiguration ) );

		//ANSI SQL cast() function is supported on the databases we care most
		//about but in certain cases it doesn't allow some useful typecasts,
		//which must be emulated in a dialect-specific way

		//Note that two case are especially tricky to make portable:
		// - casts to and from Boolean, and
		// - casting Double or Float to String.

		functionContributions.getFunctionRegistry().register(
				"cast",
				new CastFunction(
						this,
						functionContributions.getTypeConfiguration()
								.getCurrentBaseSqlTypeIndicators()
								.getPreferredSqlTypeCodeForBoolean()
				)
		);

		//There is a 'collate' operator in a number of major databases

		functionFactory.collate();

		//ANSI SQL extract() function is supported on the databases we care most
		//about (though it is called datepart() in some of them) but HQL defines
		//additional non-standard temporal field types, which must be emulated in
		//a very dialect-specific way

		functionContributions.getFunctionRegistry().register( "extract",
				new ExtractFunction( this, typeConfiguration ) );

		//comparison functions supported on most databases, emulated on others
		//using a case expression

		functionFactory.leastGreatest();

		//two-argument synonym for coalesce() supported on most but not every
		//database, so define it here as an alias for coalesce(arg1,arg2)

		functionContributions.getFunctionRegistry().register( "ifnull",
				new CoalesceIfnullEmulation() );

		//rpad() and pad() are supported on almost every database, and emulated
		//where not supported, but they're not considered "standard" ... instead
		//they're used to implement pad()

		functionFactory.pad();

		//pad() is a function we've designed to look like ANSI trim()

		functionContributions.getFunctionRegistry().register( "pad",
				new LpadRpadPadEmulation( typeConfiguration ) );

		//legacy Hibernate convenience function for casting to string, defined
		//here as an alias for cast(arg as String)

		functionContributions.getFunctionRegistry().register( "str",
				new CastStrEmulation( typeConfiguration ) );

		//format() function for datetimes, emulated on many databases using the
		//Oracle-style to_char() function, and on others using their native
		//formatting functions

		functionFactory.format_toChar();

		//timestampadd()/timestampdiff() delegated back to the Dialect itself
		//since there is a great variety of different ways to emulate them
		//by default, we don't allow plain parameters for the timestamp argument as most database don't support this
		functionFactory.timestampaddAndDiff( this, SqlAstNodeRenderingMode.NO_PLAIN_PARAMETER );
		functionContributions.getFunctionRegistry().registerAlternateKey( "dateadd", "timestampadd" );
		functionContributions.getFunctionRegistry().registerAlternateKey( "datediff", "timestampdiff" );

		//ANSI SQL (and JPA) current date/time/timestamp functions, supported
		//natively on almost every database, delegated back to the Dialect

		functionContributions.getFunctionRegistry().register(
				"current_date",
				new CurrentFunction(
						"current_date",
						currentDate(),
						dateType
				)
		);
		functionContributions.getFunctionRegistry().register(
				"current_time",
				new CurrentFunction(
						"current_time",
						currentTime(),
						timeType
				)
		);
		functionContributions.getFunctionRegistry().register(
				"current_timestamp",
				new CurrentFunction(
						"current_timestamp",
						currentTimestamp(),
						timestampType
				)
		);
		functionContributions.getFunctionRegistry().registerAlternateKey( "current date", "current_date" );
		functionContributions.getFunctionRegistry().registerAlternateKey( "current time", "current_time" );
		functionContributions.getFunctionRegistry().registerAlternateKey( "current timestamp", "current_timestamp" );
		//HQL current instant/date/time/datetime functions, delegated back to the Dialect

		functionContributions.getFunctionRegistry().register(
				"local_date",
				new CurrentFunction(
						"local_date",
						currentDate(),
						localDateType
				)
		);
		functionContributions.getFunctionRegistry().register(
				"local_time",
				new CurrentFunction(
						"local_time",
						currentLocalTime(),
						localTimeType
				)
		);
		functionContributions.getFunctionRegistry().register(
				"local_datetime",
				new CurrentFunction(
						"local_datetime",
						currentLocalTimestamp(),
						localDateTimeType
				)
		);
		functionContributions.getFunctionRegistry().register(
				"offset_datetime",
				new CurrentFunction(
						"offset_datetime",
						currentTimestampWithTimeZone(),
						offsetDateTimeType
				)
		);
		functionContributions.getFunctionRegistry().registerAlternateKey( "local date", "local_date" );
		functionContributions.getFunctionRegistry().registerAlternateKey( "local time", "local_time" );
		functionContributions.getFunctionRegistry().registerAlternateKey( "local datetime", "local_datetime" );
		functionContributions.getFunctionRegistry().registerAlternateKey( "offset datetime", "offset_datetime" );

		functionContributions.getFunctionRegistry().register(
				"instant",
				new CurrentFunction(
						"instant",
						currentTimestampWithTimeZone(),
						instantType
				)
		);
		functionContributions.getFunctionRegistry().registerAlternateKey( "current_instant", "instant" ); //deprecated legacy!

		functionContributions.getFunctionRegistry().register( "sql", new SqlFunction() );
	}

	/**
	 * Translation of the HQL/JPQL {@code current_date} function, which
	 * maps to the Java type {@link java.sql.Date}, and of the HQL
	 * {@code local_date} function which maps to the Java type
	 * {@link java.time.LocalDate}.
	 */
	public String currentDate() {
		return "current_date";
	}

	/**
	 * Translation of the HQL/JPQL {@code current_time} function, which
	 * maps to the Java type {@link java.sql.Time} which is a time with
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
	 * which maps to the Java type {@link java.sql.Timestamp} which is
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
	 * the Java type {@link java.time.LocalTime} which is a time with no
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
	 * to the Java type {@link java.time.LocalDateTime} which is a datetime
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
	 * to the Java type {@link java.time.OffsetDateTime} which is a datetime
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
	 *
	 * @deprecated Use {@link #trimPattern(TrimSpec, boolean)} instead.
	 */
	@Deprecated( forRemoval = true )
	public String trimPattern(TrimSpec specification, char character) {
		return trimPattern( specification, character == ' ' );
	}

	/**
	 * Obtain a pattern for the SQL equivalent to a
	 * {@code trim()} function call. The resulting
	 * pattern must contain a ?1 placeholder for the
	 * argument of type {@link String} and a ?2 placeholder
	 * for the trim character if {@code isWhitespace}
	 * was false.
	 *
	 * @param specification {@linkplain TrimSpec#LEADING leading}, {@linkplain TrimSpec#TRAILING trailing}
	 * or {@linkplain TrimSpec#BOTH both}
	 * @param isWhitespace {@code true} if the trim character is a whitespace and can be omitted,
	 * {@code false} if it must be explicit and a ?2 placeholder should be included in the pattern
	 */
	public String trimPattern(TrimSpec specification, boolean isWhitespace) {
		return "trim(" + specification + ( isWhitespace ? "" : " ?2" ) + " from ?1)";
	}

	/**
	 * Whether the database supports adding a fractional interval to a timestamp,
	 * for example {@code timestamp + 0.5 second}.
	 */
	public boolean supportsFractionalTimestampArithmetic() {
		return true;
	}

	/**
	 * Obtain a pattern for the SQL equivalent to a
	 * {@code timestampdiff()} function call. The resulting
	 * pattern must contain ?1, ?2, and ?3 placeholders
	 * for the arguments.
	 *
	 * @param unit the first argument
	 * @param fromTemporalType true if the first argument is
	 *                      a timestamp, false if a date
	 * @param toTemporalType true if the second argument is
	 */
	public String timestampdiffPattern(TemporalUnit unit, TemporalType fromTemporalType, TemporalType toTemporalType) {
		throw new UnsupportedOperationException( "`" + getClass().getName() + "` does not yet support #timestampdiffPattern" );
	}

	/**
	 * Obtain a pattern for the SQL equivalent to a
	 * {@code timestampadd()} function call. The resulting
	 * pattern must contain ?1, ?2, and ?3 placeholders
	 * for the arguments.
	 *
	 * @param unit The unit to add to the temporal
	 * @param temporalType The type of the temporal
	 * @param intervalType The type of interval to add or null if it's not a native interval
	 */
	public String timestampaddPattern(TemporalUnit unit, TemporalType temporalType, IntervalType intervalType) {
		throw new UnsupportedOperationException( "`" + getClass().getName() + "` does not yet support #timestampaddPattern" );
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
	 * <p>
	 * On the other hand, integral types are not treated as equivalent,
	 * instead, {@link #isCompatibleIntegralType(int, int)} is responsible
	 * for determining if the types are compatible.
	 *
	 * @param typeCode1 the first column type info
	 * @param typeCode2 the second column type info
	 *
	 * @return {@code true} if the two type codes are equivalent
	 */
	public boolean equivalentTypes(int typeCode1, int typeCode2) {
		return typeCode1==typeCode2
			|| isNumericOrDecimal(typeCode1) && isNumericOrDecimal(typeCode2)
			|| isFloatOrRealOrDouble(typeCode1) && isFloatOrRealOrDouble(typeCode2)
			|| isVarcharType(typeCode1) && isVarcharType(typeCode2)
			|| isVarbinaryType(typeCode1) && isVarbinaryType(typeCode2)
			|| isCompatibleIntegralType(typeCode1, typeCode2)
			// HHH-17908: Since the runtime can cope with enum on the DDL side,
			// but varchar on the ORM expectation side, let's treat the types as equivalent
			|| isEnumType(typeCode1) && isVarcharType(typeCode2)
			|| sameColumnType(typeCode1, typeCode2);
	}

	/**
	 * Tolerate storing {@code short} in {@code INTEGER} or {@code BIGINT}
	 * or {@code int} in {@code BIGINT} for the purposes of schema validation
	 * and migration.
	 */
	private boolean isCompatibleIntegralType(int typeCode1, int typeCode2) {
		switch (typeCode1) {
			case TINYINT:
				return typeCode2 == TINYINT
					|| typeCode2 == SMALLINT
					|| typeCode2 == INTEGER
					|| typeCode2 == BIGINT;
			case SMALLINT:
				return typeCode2 == SMALLINT
					|| typeCode2 == INTEGER
					|| typeCode2 == BIGINT;
			case INTEGER:
				return typeCode2 == INTEGER
					|| typeCode2 == BIGINT;
		}
		return false;
	}

	private boolean sameColumnType(int typeCode1, int typeCode2) {
		try {
			return Objects.equals( columnType(typeCode1), columnType(typeCode2) );
		}
		catch (IllegalArgumentException iae) {
			return false;
		}
	}

	/**
	 * Retrieve a set of default Hibernate properties for this database.
	 * <p>
	 * An implementation may set configuration properties from
	 * {@link #initDefaultProperties()}, though it is discouraged.
	 the
	 * @return the Hibernate configuration properties
	 *
	 * @see #initDefaultProperties()
	 */
	public Properties getDefaultProperties() {
		return properties;
	}

	/**
	 * The default value to use for the configuration property
	 * {@value org.hibernate.cfg.Environment#STATEMENT_BATCH_SIZE}.
	 */
	public int getDefaultStatementBatchSize() {
		return 1;
	}

	/**
	 * The default value to use for the configuration property
	 * {@value org.hibernate.cfg.Environment#NON_CONTEXTUAL_LOB_CREATION}.
	 */
	public boolean getDefaultNonContextualLobCreation() {
		return false;
	}

	/**
	 * The default value to use for the configuration property
	 * {@value org.hibernate.cfg.Environment#USE_GET_GENERATED_KEYS}.
	 */
	public boolean getDefaultUseGetGeneratedKeys() {
		return true;
	}

	@Override
	public String toString() {
		return getClass().getName() + ", version: " + getVersion();
	}


	// database type mapping support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public void contribute(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		contributeTypes( typeContributions, serviceRegistry );
	}

	/**
	 * A callback which allows the {@code Dialect} to contribute types.
	 *
	 * @param typeContributions Callback to contribute the types
	 * @param serviceRegistry The service registry
	 */
	public void contributeTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		// by default, not much to do...
		registerColumnTypes( typeContributions, serviceRegistry );
		final NationalizationSupport nationalizationSupport = getNationalizationSupport();
		final JdbcTypeRegistry jdbcTypeRegistry = typeContributions.getTypeConfiguration().getJdbcTypeRegistry();
		if ( nationalizationSupport == NationalizationSupport.EXPLICIT ) {
			jdbcTypeRegistry.addDescriptor( NCharJdbcType.INSTANCE );
			jdbcTypeRegistry.addDescriptor( NVarcharJdbcType.INSTANCE );
			jdbcTypeRegistry.addDescriptor( LongNVarcharJdbcType.INSTANCE );
			jdbcTypeRegistry.addDescriptor( NClobJdbcType.DEFAULT );
		}

		if ( getTimeZoneSupport() == TimeZoneSupport.NATIVE ) {
			jdbcTypeRegistry.addDescriptor( TimestampUtcAsOffsetDateTimeJdbcType.INSTANCE );
			jdbcTypeRegistry.addDescriptor( TimeUtcAsOffsetTimeJdbcType.INSTANCE );
		}
		else {
			jdbcTypeRegistry.addDescriptor( TimestampUtcAsJdbcTimestampJdbcType.INSTANCE );
			jdbcTypeRegistry.addDescriptor( TimeUtcAsJdbcTimeJdbcType.INSTANCE );
		}

		if ( supportsStandardArrays() ) {
			jdbcTypeRegistry.addTypeConstructor( ArrayJdbcTypeConstructor.INSTANCE );
		}
		if ( supportsMaterializedLobAccess() ) {
			jdbcTypeRegistry.addDescriptor( SqlTypes.MATERIALIZED_BLOB, BlobJdbcType.MATERIALIZED );
			jdbcTypeRegistry.addDescriptor( SqlTypes.MATERIALIZED_CLOB, ClobJdbcType.MATERIALIZED );
			jdbcTypeRegistry.addDescriptor( SqlTypes.MATERIALIZED_NCLOB, NClobJdbcType.MATERIALIZED );
		}
	}

	/**
	 * A {@link LobMergeStrategy} representing the legacy behavior of Hibernate.
	 * LOBs are not processed by merge.
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
	 * A {@link LobMergeStrategy} based on transferring contents using streams.
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
					detachedStream.transferTo( connectedStream );
					return target;
				}
				catch (IOException e ) {
					throw new HibernateException( "Unable to copy stream content", e );
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
					detachedStream.transferTo( connectedStream );
					return target;
				}
				catch (IOException e ) {
					throw new HibernateException( "Unable to copy stream content", e );
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
					detachedStream.transferTo( connectedStream );
					return target;
				}
				catch (IOException e ) {
					throw new HibernateException( "Unable to copy stream content", e );
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
	 * A {@link LobMergeStrategy} based on creating a new LOB locator.
	 */
	protected static final LobMergeStrategy NEW_LOCATOR_LOB_MERGE_STRATEGY = new LobMergeStrategy() {
		@Override
		public Blob mergeBlob(Blob original, Blob target, SharedSessionContractImplementor session) {
			if ( original == null && target == null ) {
				return null;
			}
			final JdbcServices jdbcServices = session.getFactory().getFastSessionServices().jdbcServices;
			try {
				final LobCreator lobCreator = jdbcServices.getLobCreator( session );
				return original == null
						? lobCreator.createBlob( ArrayHelper.EMPTY_BYTE_ARRAY )
						: lobCreator.createBlob( original.getBinaryStream(), original.length() );
			}
			catch (SQLException e) {
				throw jdbcServices.getSqlExceptionHelper().convert( e, "unable to merge BLOB data" );
			}
		}

		@Override
		public Clob mergeClob(Clob original, Clob target, SharedSessionContractImplementor session) {
			if ( original == null && target == null ) {
				return null;
			}
			final JdbcServices jdbcServices = session.getFactory().getFastSessionServices().jdbcServices;
			try {
				final LobCreator lobCreator = jdbcServices.getLobCreator( session );
				return original == null
						? lobCreator.createClob( "" )
						: lobCreator.createClob( original.getCharacterStream(), original.length() );
			}
			catch (SQLException e) {
				throw jdbcServices.getSqlExceptionHelper().convert( e, "unable to merge CLOB data" );
			}
		}

		@Override
		public NClob mergeNClob(NClob original, NClob target, SharedSessionContractImplementor session) {
			if ( original == null && target == null ) {
				return null;
			}
			final JdbcServices jdbcServices = session.getFactory().getFastSessionServices().jdbcServices;
			try {
				final LobCreator lobCreator = jdbcServices.getLobCreator( session );
				return original == null
						? lobCreator.createNClob( "" )
						: lobCreator.createNClob( original.getCharacterStream(), original.length() );
			}
			catch (SQLException e) {
				throw jdbcServices.getSqlExceptionHelper().convert( e, "unable to merge NCLOB data" );
			}
		}
	};

	/**
	 * Get the {@link LobMergeStrategy} to use, {@link #NEW_LOCATOR_LOB_MERGE_STRATEGY}
	 * by default.
	 */
	public LobMergeStrategy getLobMergeStrategy() {
		return NEW_LOCATOR_LOB_MERGE_STRATEGY;
	}


	// native identifier generation ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	
	/**
	 * The name identifying the "native" id generation strategy for this dialect.
	 * <p>
	 * This is the name of the id generation strategy which should be used when
	 * {@code "native"} is specified in {@code hbm.xml}.
	 *
	 * @return The name identifying the native generator strategy.
	 */
	public String getNativeIdentifierGeneratorStrategy() {
		return getIdentityColumnSupport().supportsIdentityColumns()
				? "identity"
				: "sequence";
	}

	// IDENTITY support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Get the appropriate {@link IdentityColumnSupport} for this dialect.
	 *
	 * @return the IdentityColumnSupport
	 * @since 5.1
	 */
	public IdentityColumnSupport getIdentityColumnSupport() {
		return IdentityColumnSupportImpl.INSTANCE;
	}

	// SEQUENCE support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Get the appropriate {@link SequenceSupport} for this dialect.
	 **/
	public SequenceSupport getSequenceSupport() {
		return NoSequenceSupport.INSTANCE;
	}

	/**
	 * Get the {@code select} command used retrieve the names of all sequences.
	 *
	 * @return The select command; or null if sequences are not supported.
	 */
	public String getQuerySequencesString() {
		return null;
	}

	/**
	 * A {@link SequenceInformationExtractor} which is able to extract
	 * {@link org.hibernate.tool.schema.extract.spi.SequenceInformation}
	 * from the JDBC result set returned when {@link #getQuerySequencesString()}
	 * is executed.
	 */
	public SequenceInformationExtractor getSequenceInformationExtractor() {
		return getQuerySequencesString() == null
				? SequenceInformationExtractorNoOpImpl.INSTANCE
				: SequenceInformationExtractorLegacyImpl.INSTANCE;
	}

	// GUID support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Get the command used to select a GUID from the database.
	 * <p>
	 * Optional operation.
	 *
	 * @return The appropriate command.
	 */
	public String getSelectGUIDString() {
		throw new UnsupportedOperationException( getClass().getName() + " does not support GUIDs" );
	}

	/**
	 * Does this database have some sort of support for temporary tables?
	 *
	 * @return true by default, since most do
	 */
	public boolean supportsTemporaryTables() {
		// Most databases do
		return true;
	}

	/**
	 * Does this database support primary keys for temporary tables?
	 *
	 * @return true by default, since most do
	 */
	public boolean supportsTemporaryTablePrimaryKey() {
		// Most databases do
		return true;
	}

	// limit/offset support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Obtain a {@link LimitHandler} that implements pagination support for
	 * {@link Query#setMaxResults(int)} and {@link Query#setFirstResult(int)}.
	 */
	public LimitHandler getLimitHandler() {
		throw new UnsupportedOperationException("this dialect does not support query pagination");
	}

	// lock acquisition support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Does this dialect support specifying timeouts when requesting locks.
	 *
	 * @return True is this dialect supports specifying lock timeouts.
	 */
	public boolean supportsLockTimeouts() {
		return true;
	}

	/**
	 * If this dialect supports specifying lock timeouts, are those timeouts
	 * rendered into the {@code SQL} string as parameters? The implication
	 * is that Hibernate will need to bind the timeout value as a parameter
	 * in the {@link PreparedStatement}. If true, the parameter position
	 * is always handled as the last parameter; if the dialect specifies the
	 * lock timeout elsewhere in the {@code SQL} statement then the timeout
	 * value should be directly rendered into the statement and this method
	 * should return false.
	 *
	 * @return True if the lock timeout is rendered into the {@code SQL}
	 *         string as a parameter; false otherwise.
	 *
	 * @deprecated This is never called, and since at least Hibernate 5 has
	 *             just returned {@code false} in every dialect. It will be
	 *             removed.
	 */
	@Deprecated(since = "6", forRemoval = true)
	public boolean isLockTimeoutParameterized() {
		return false;
	}

	/**
	 * A {@link LockingStrategy} which is able to acquire a database-level
	 * lock with the specified {@linkplain LockMode level}.
	 *
	 * @param lockable The persister for the entity to be locked.
	 * @param lockMode The type of lock to be acquired.
	 * @return The appropriate locking strategy.
	 *
	 * @since 3.2
	 */
	public LockingStrategy getLockingStrategy(Lockable lockable, LockMode lockMode) {
		switch ( lockMode ) {
			case PESSIMISTIC_FORCE_INCREMENT:
				return new PessimisticForceIncrementLockingStrategy( lockable, lockMode );
			case UPGRADE_NOWAIT:
			case UPGRADE_SKIPLOCKED:
			case PESSIMISTIC_WRITE:
				return new PessimisticWriteSelectLockingStrategy( lockable, lockMode );
			case PESSIMISTIC_READ:
				return new PessimisticReadSelectLockingStrategy( lockable, lockMode );
			case OPTIMISTIC_FORCE_INCREMENT:
				return new OptimisticForceIncrementLockingStrategy( lockable, lockMode );
			case OPTIMISTIC:
				return new OptimisticLockingStrategy( lockable, lockMode );
			case READ:
				return new SelectLockingStrategy( lockable, lockMode );
			default:
				// WRITE, NONE are not allowed here
				throw new IllegalArgumentException( "Unsupported lock mode" );
		}
	}

	/**
	 * Given a set of {@link LockOptions} (lock level, timeout),
	 * determine the appropriate {@code for update} fragment to
	 * use to obtain the lock.
	 *
	 * @param lockOptions contains the lock mode to apply.
	 * @return The appropriate {@code for update} fragment.
	 */
	public String getForUpdateString(LockOptions lockOptions) {
		return getForUpdateString( lockOptions.getLockMode(), lockOptions.getTimeOut() );
	}

	/**
	 * Given a {@linkplain LockMode lock level} and timeout,
	 * determine the appropriate {@code for update} fragment to
	 * use to obtain the lock.
	 *
	 * @param lockMode the lock mode to apply.
	 * @param timeout the timeout
	 * @return The appropriate {@code for update} fragment.
	 */
	private String getForUpdateString(LockMode lockMode, int timeout) {
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
	 * Given a {@link LockMode}, determine the appropriate
	 * {@code for update} fragment to use to obtain the lock.
	 *
	 * @param lockMode The lock mode to apply.
	 * @return The appropriate for update fragment.
	 */
	public String getForUpdateString(LockMode lockMode) {
		return getForUpdateString( lockMode, LockOptions.WAIT_FOREVER );
	}

	/**
	 * Get the string to append to {@code SELECT} statements to
	 * acquire pessimistic UPGRADE locks for this dialect.
	 *
	 * @return The appropriate {@code FOR UPDATE} clause string.
	 */
	public String getForUpdateString() {
		return " for update";
	}

	/**
	 * Get the string to append to {@code SELECT} statements to
	 * acquire pessimistic WRITE locks for this dialect.
	 * <p>
	 * Location of the returned string is treated the same as
	 * {@link #getForUpdateString()}.
	 *
	 * @param timeout in milliseconds, -1 for indefinite wait and 0 for no wait.
	 * @return The appropriate {@code LOCK} clause string.
	 */
	public String getWriteLockString(int timeout) {
		return getForUpdateString();
	}

	/**
	 * Get the string to append to {@code SELECT} statements to
	 * acquire WRITE locks for this dialect, given the aliases of
	 * the columns to be write locked.
	 * <p>
	 * Location of the returned string is treated the same as
	 * {@link #getForUpdateString()}.
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
	 * Get the string to append to {@code SELECT} statements to
	 * acquire READ locks for this dialect.
	 * <p>
	 * Location of the returned string is treated the same as
	 * {@link #getForUpdateString()}.
	 *
	 * @param timeout in milliseconds, -1 for indefinite wait and 0 for no wait.
	 * @return The appropriate {@code LOCK} clause string.
	 */
	public String getReadLockString(int timeout) {
		return getForUpdateString();
	}

	/**
	 * Get the string to append to {@code SELECT} statements to
	 * acquire READ locks for this dialect, given the aliases of
	 * the columns to be read locked.
	 * <p>
	 * Location of the returned string is treated the same as
	 * {@link #getForUpdateString()}.
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
	 * The {@linkplain RowLockStrategy row lock strategy} to use for write locks.
	 */
	public RowLockStrategy getWriteRowLockStrategy() {
		// by default we report no support
		return RowLockStrategy.NONE;
	}

	/**
	 * The {@linkplain RowLockStrategy row lock strategy} to use for read locks.
	 */
	public RowLockStrategy getReadRowLockStrategy() {
		return getWriteRowLockStrategy();
	}

	/**
	 * Does this dialect support {@code FOR UPDATE} in conjunction with
	 * outer-joined rows?
	 *
	 * @return True if outer-joined rows can be locked via {@code FOR UPDATE}.
	 */
	public boolean supportsOuterJoinForUpdate() {
		return true;
	}

	/**
	 * Get the {@code FOR UPDATE OF column_list} fragment appropriate
	 * for this dialect, given the aliases of the columns to be write
	 * locked.
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
	 * Get the {@code FOR UPDATE OF} or {@code FOR SHARE OF} fragment
	 * appropriate for this dialect, given the aliases of the columns
	 * to be locked.
	 *
	 * @param aliases The columns to be locked.
	 * @param lockOptions the lock options to apply
	 * @return The appropriate {@code FOR UPDATE OF column_list} clause string.
	 */
	public String getForUpdateString(String aliases, LockOptions lockOptions) {
		LockMode lockMode = lockOptions.getLockMode();
		for ( Map.Entry<String, LockMode> entry : lockOptions.getAliasSpecificLocks() ) {
			// seek the highest lock mode
			final LockMode lm = entry.getValue();
			if ( lm.greaterThan(lockMode) ) {
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
		// by default, we report no support for NOWAIT lock semantics
		return getForUpdateString();
	}

	/**
	 * Retrieves the {@code FOR UPDATE SKIP LOCKED} syntax specific to this dialect.
	 *
	 * @return The appropriate {@code FOR UPDATE SKIP LOCKED} clause string.
	 */
	public String getForUpdateSkipLockedString() {
		// by default, we report no support for SKIP_LOCKED lock semantics
		return getForUpdateString();
	}

	/**
	 * Get the {@code FOR UPDATE OF column_list NOWAIT} fragment appropriate
	 * for this dialect, given the aliases of the columns to be write locked.
	 *
	 * @param aliases The columns to be write locked.
	 * @return The appropriate {@code FOR UPDATE OF colunm_list NOWAIT} clause string.
	 */
	public String getForUpdateNowaitString(String aliases) {
		return getForUpdateString( aliases );
	}

	/**
	 * Get the {@code FOR UPDATE OF column_list SKIP LOCKED} fragment appropriate
	 * for this dialect, given the aliases of the columns to be write locked.
	 *
	 * @param aliases The columns to be write locked.
	 * @return The appropriate {@code FOR UPDATE colunm_list SKIP LOCKED} clause string.
	 */
	public String getForUpdateSkipLockedString(String aliases) {
		return getForUpdateString( aliases );
	}

	/**
	 * Some dialects support an alternative means to {@code SELECT FOR UPDATE},
	 * whereby a "lock hint" is appended to the table name in the {@code from}
	 * clause.
	 *
	 * @param lockOptions The lock options to apply
	 * @param tableName The name of the table to which to apply the lock hint.
	 * @return The table with any required lock hints.
	 */
	public String appendLockHint(LockOptions lockOptions, String tableName){
		return tableName;
	}

	/**
	 * Modifies the given SQL, applying the appropriate updates for the specified
	 * lock modes and key columns.
	 * <p>
	 * This allows emulation of {@code SELECT FOR UPDATE} for dialects which do not
	 * support the standard syntax.
	 *
	 * @param sql the SQL string to modify
	 * @param aliasedLockOptions lock options indexed by aliased table names.
	 * @param keyColumnNames a map of key columns indexed by aliased table names.
	 * @return the modified SQL string.
	 */
	public String applyLocksToSql(String sql, LockOptions aliasedLockOptions, Map<String, String[]> keyColumnNames) {
		return sql + new ForUpdateFragment( this, aliasedLockOptions, keyColumnNames ).toFragmentString();
	}

	protected int getTimeoutInSeconds(int millis) {
		return millis == 0 ? 0 : Math.max( 1, Math.round( millis / 1e3f ) );
	}


	// table support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * The command used to create a table, usually {@code create table}.
	 *
	 * @return The command used to create a table.
	 */
	public String getCreateTableString() {
		return "create table";
	}

	/**
	 * An arbitrary fragment appended to the end of the {@code create table}
	 * statement.
	 *
	 * @apiNote An example is the MySQL {@code engine} option specifying a
	 *          storage engine.
	 */
	public String getTableTypeString() {
		return "";
	}

	/**
	 * For dropping a table, can the phrase {@code if exists} be
	 * applied before the table name?
	 *
	 * @apiNote Only one or the other (or neither) of this and
	 *          {@link #supportsIfExistsAfterTableName} should
	 *          return true.
	 *
	 * @return {@code true} if {@code if exists} can be applied
	 *         before the table name
	 */
	public boolean supportsIfExistsBeforeTableName() {
		return false;
	}

	/**
	 * For dropping a table, can the phrase {@code if exists} be
	 * applied after the table name?
	 *
	 * @apiNote Only one or the other (or neither) of this and
	 *          {@link #supportsIfExistsBeforeTableName} should
	 *          return true.
	 *
	 * @return {@code true} if {@code if exists} can be applied
	 *         after the table name
	 */
	public boolean supportsIfExistsAfterTableName() {
		return false;
	}

	/**
	 * A command to execute before dropping tables.
	 *
	 * @return A SQL statement, or {@code null}
	 */
	public String getBeforeDropStatement() {
		return null;
	}

	/**
	 * The command used to drop a table with the given name, usually
	 * {@code drop table tab_name}.
	 *
	 * @param tableName The name of the table to drop
	 *
	 * @return The {@code drop table} statement as a string
	 * 
	 * @deprecated No longer used
	 *
	 * @see StandardTableExporter#getSqlDropStrings
	 */
	@Deprecated(since = "6.6")
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
	 * The command used to create an index, usually {@code create index}
	 * or {@code create unique index}.
	 *
	 * @param unique {@code true} if the index is a unique index
	 * @return The command used to create an index.
	 */
	public String getCreateIndexString(boolean unique) {
		return unique ? "create unique index" : "create index";
	}

	/**
	 * A string to be appended to the end of the {@code create index}
	 * command, usually to specify that {@code null} values are to be
	 * considered distinct.
	 */
	public String getCreateIndexTail(boolean unique, List<Column> columns) {
		return "";
	}

	/**
	 * Do we need to qualify index names with the schema name?
	 *
	 * @return {@code true} if we do
	 */
	public boolean qualifyIndexName() {
		return true;
	}

	/**
	 * Slight variation on {@link #getCreateTableString}. Here, we have
	 * the command used to create a table when there is no primary key
	 * and duplicate rows are expected.
	 *
	 * @apiNote Most databases do not have this distinction; this method
	 *          was originally added for Teradata which does.
	 *
	 * @return The command used to create a multiset table.
	 */
	public String getCreateMultisetTableString() {
		return getCreateTableString();
	}

	/**
	 * Does this dialect support the {@code ALTER TABLE} syntax?
	 *
	 * @return True if we support altering existing tables; false otherwise.
	 */
	public boolean hasAlterTable() {
		return true;
	}

	/**
	 * The command used to alter a table with the given name, usually
	 * {@code alter table tab_name} or
	 * {@code alter table tab_name if exists}.
	 * <p>
	 * We prefer the {@code if exists} form if supported.
	 *
	 * @param tableName The name of the table to alter
	 * @return The command used to alter a table.
	 *
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
	 * For an {@code alter table}, can the phrase {@code if exists} be
	 * applied?
	 *
	 * @return {@code true} if {@code if exists} can be applied after
	 *         {@code alter table}
	 *
	 * @since 5.2.11
	 */
	public boolean supportsIfExistsAfterAlterTable() {
		return false;
	}

	/**
	 * The subcommand of the {@code alter table} command used to add
	 * a column to a table, usually {@code add column} or {@code add}.
	 *
	 * @return The {@code add column} fragment.
	 */
	public String getAddColumnString() {
		return "add column";
	}

	/**
	 * The syntax for the suffix used to add a column to a table.
	 *
	 * @return The suffix of the {@code add column} fragment.
	 */
	public String getAddColumnSuffixString() {
		return "";
	}

	/**
	 * Do we need to drop constraints before dropping tables in this dialect?
	 *
	 * @return True if constraints must be dropped prior to dropping the table;
	 *         false otherwise.
	 */
	public boolean dropConstraints() {
		return true;
	}

	/**
	 * The subcommand of the {@code alter table} command used to drop
	 * a foreign key constraint, usually {@code drop constraint}.
	 */
	public String getDropForeignKeyString() {
		return "drop constraint";
	}

	/**
	 * The subcommand of the {@code alter table} command used to drop
	 * a unique key constraint.
	 */
	public String getDropUniqueKeyString() {
		return "drop constraint";
	}

	/**
	 * For dropping a constraint with an {@code alter table} statement,
	 * can the phrase {@code if exists} be applied before the constraint
	 * name?
	 *
	 * @apiNote Only one or the other (or neither) of this and
	 *          {@link #supportsIfExistsAfterConstraintName} should
	 *          return true
	 *
	 * @return {@code true} if {@code if exists} can be applied before
	 *         the constraint name
	 */
	public boolean supportsIfExistsBeforeConstraintName() {
		return false;
	}

	/**
	 * For dropping a constraint with an {@code alter table}, can the
	 * phrase {@code if exists} be applied after the constraint name?
	 *
	 * @apiNote Only one or the other (or neither) of this and
	 *          {@link #supportsIfExistsBeforeConstraintName} should
	 *          return true.
	 *
	 * @return {@code true} if {@code if exists} can be applied after
	 *         the constraint name
	 */
	public boolean supportsIfExistsAfterConstraintName() {
		return false;
	}

	/**
	 * Does this dialect support modifying the type of an existing column?
	 */
	public boolean supportsAlterColumnType() {
		return false;
	}

	/**
	 * The fragment of an {@code alter table} command which modifies a
	 * column type, or null if column types cannot be modified.
	 * Often {@code alter column col_name set data type col_type}.
	 *
	 * @param columnName the name of the column
	 * @param columnType the new type of the column
	 * @param columnDefinition the full column definition
	 * @return a fragment to be appended to {@code alter table}
	 */
	public String getAlterColumnTypeString(String columnName, String columnType, String columnDefinition) {
		return null;
	}

	/**
	 * The syntax used to add a foreign key constraint to a table,
	 * with the referenced key columns explicitly specified.
	 *
	 * @param constraintName The foreign key constraint name
	 * @param foreignKey The names of the columns comprising the
	 *                   foreign key
	 * @param referencedTable The table referenced by the foreign key
	 * @param primaryKey The explicit columns in the referencedTable
	 *                    referenced by this foreign key.
	 * @param referencesPrimaryKey if false, constraint should be
	 *                             explicit about which column names
	 *                             the constraint refers to
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

	/**
	 * The syntax used to add a foreign key constraint to a table,
	 * given the definition of the foreign key as a string.
	 *
	 * @param constraintName The foreign key constraint name
	 * @param foreignKeyDefinition The whole definition of the
	 *                             foreign key as a fragment
	 */
	public String getAddForeignKeyConstraintString(
			String constraintName,
			String foreignKeyDefinition) {
		return " add constraint " + quote( constraintName )
				+ " " + foreignKeyDefinition;
	}

	/**
	 * Does the dialect also need cross-references to get a complete
	 * list of foreign keys?
	 */
	public boolean useCrossReferenceForeignKeys(){
		return false;
	}

	/**
	 * Some dialects require a not null primaryTable filter.
	 * Sometimes a wildcard entry is sufficient for the like condition.
	 * @return
	 */
	public String getCrossReferenceParentTableFilter(){
		return null;
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
	 * The {@link SqmMultiTableMutationStrategy} to use when not specified by
	 * {@link org.hibernate.query.spi.QueryEngineOptions#getCustomSqmMultiTableMutationStrategy}.
	 *
	 * @see SqmMultiTableMutationStrategyProvider#createMutationStrategy
	 */
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

	/**
	 * The {@link SqmMultiTableInsertStrategy} to use when not specified by
	 * {@link org.hibernate.query.spi.QueryEngineOptions#getCustomSqmMultiTableInsertStrategy}.
	 *
	 * @see SqmMultiTableMutationStrategyProvider#createInsertStrategy
	 */
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

	// UDT support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * The kind of user-defined type to create, or the empty
	 * string if this does not need to be specified. Included
	 * after {@code create type type_name as}, but before the
	 * list of members.
	 */
	public String getCreateUserDefinedTypeKindString() {
		return "";
	}

	/**
	 * An arbitrary extension to append to the end of the UDT
	 * {@code create type} command.
	 */
	public String getCreateUserDefinedTypeExtensionsString() {
		return "";
	}

	/**
	 * For dropping a type, can the phrase {@code if exists} be
	 * applied before the type name?
	 *
	 * @apiNote Only one or the other (or neither) of this and
	 *          {@link #supportsIfExistsAfterTypeName} should
	 *          return true.
	 *
	 * @return {@code true} if {@code if exists} can be applied
	 *         before the type name
	 */
	public boolean supportsIfExistsBeforeTypeName() {
		return false;
	}

	/**
	 * For dropping a type, can the phrase {@code if exists} be
	 * applied after the type name?
	 *
	 * @apiNote Only one or the other (or neither) of this and
	 *          {@link #supportsIfExistsBeforeTypeName} should
	 *          return true.
	 *
	 * @return {@code true} if {@code if exists} can be applied
	 *         after the type name
	 */
	public boolean supportsIfExistsAfterTypeName() {
		return false;
	}

	// callable statement support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Registers a parameter capable of returning a {@link ResultSet}
	 * <em>by position</em>, either an {@code OUT} parameter, or a
	 * {@link Types#REF_CURSOR REF_CURSOR} parameter as defined in Java 8.
	 *
	 * @apiNote Before Java 8, support for {@link ResultSet}-returning
	 *          parameters was very uneven across database and drivers,
	 *          leading to its inclusion as part of the {@code Dialect}
	 *          contract.
	 *
	 * @param statement The callable statement.
	 * @param position The bind position at which to register the output param.
	 *
	 * @return The number of (contiguous) bind positions used.
	 *
	 * @throws SQLException Indicates problems registering the param.
	 */
	public int registerResultSetOutParameter(CallableStatement statement, int position)
			throws SQLException {
		throw new UnsupportedOperationException(
				getClass().getName() +
						" does not support resultsets via stored procedures"
		);
	}

	/**
	 * Registers a parameter capable of returning a {@link ResultSet}
	 * <em>by name</em>, either an {@code OUT} parameter, or a
	 * {@link Types#REF_CURSOR REF_CURSOR} parameter as defined in Java 8.
	 *
	 * @apiNote Before Java 8, support for {@link ResultSet}-returning
	 *          parameters was very uneven across database and drivers,
	 *          leading to its inclusion as part of the {@code Dialect}
	 *          contract.
	 *
	 * @param statement The callable statement.
	 * @param name The parameter name (for drivers which support named parameters).
	 *
	 * @return The number of (contiguous) bind positions used.
	 *
	 * @throws SQLException Indicates problems registering the param.
	 */
	@SuppressWarnings("UnusedParameters")
	public int registerResultSetOutParameter(CallableStatement statement, String name)
			throws SQLException {
		throw new UnsupportedOperationException(
				getClass().getName() +
						" does not support resultsets via stored procedures"
		);
	}

	/**
	 * Given a {@linkplain CallableStatement callable statement} previously
	 * processed by {@link #registerResultSetOutParameter}, extract the
	 * {@link ResultSet} from the {@code OUT} parameter.
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
	 * Given a {@linkplain CallableStatement callable statement} previously
	 * processed by {@link #registerResultSetOutParameter}, extract the
	 * {@link ResultSet} from the positional {@code OUT} parameter.
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
	 * Given a {@linkplain CallableStatement callable statement} previously
	 * processed by {@link #registerResultSetOutParameter}, extract the
	 * {@link ResultSet} from the named {@code OUT} parameter.
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
	 * Does this dialect support some way to retrieve the current timestamp
	 * value from the database?
	 *
	 * @return True if the current timestamp can be retrieved; false otherwise.
	 */
	public boolean supportsCurrentTimestampSelection() {
		return false;
	}

	/**
	 * Is the command returned by {@link #getCurrentTimestampSelectString}
	 * treated as callable?
	 * <p>
	 * Typically, this indicates the use of the JDBC escape syntax.
	 *
	 * @return {@code} if the {@link #getCurrentTimestampSelectString} is
	 *         treated as callable; false otherwise.
	 */
	public boolean isCurrentTimestampSelectStringCallable() {
		throw new UnsupportedOperationException( "Database not known to define a current timestamp function" );
	}

	/**
	 * The command used to retrieve the current timestamp from the database.
	 */
	public String getCurrentTimestampSelectString() {
		throw new UnsupportedOperationException( "Database not known to define a current timestamp function" );
	}

	/**
	 * Does this dialect have an ANSI SQL {@code current_timestamp} function?
	 */
	public boolean supportsStandardCurrentTimestampFunction() {
		return true;
	}


	// SQLException support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * An instance of {@link SQLExceptionConversionDelegate} for interpreting
	 * dialect-specific {@linkplain SQLException#getErrorCode() error} or
	 * {@linkplain SQLException#getSQLState() SQLState} codes.
	 * <p>
	 * If this method is overridden to return a non-null value, the default
	 * {@link SQLExceptionConverter} will use the returned
	 * {@link SQLExceptionConversionDelegate} in addition to the following
	 * standard delegates:
	 * <ol>
	 * <li>a "static" delegate based on the JDBC4-defined {@link SQLException}
	 *     hierarchy, and
	 * <li>a delegate that interprets SQLState codes as either X/Open or
	 *     SQL-2003 codes, depending on what is
	 *     {@linkplain java.sql.DatabaseMetaData#getSQLStateType reported}
	 *     by the JDBC driver.
	 * </ol>
	 * <p>
	 * It is strongly recommended that every {@code Dialect} implementation
	 * override this method, since interpretation of a SQL error is much
	 * more accurate when based on the vendor-specific
	 * {@linkplain SQLException#getErrorCode() error code}, rather than on
	 * the SQLState.
	 *
	 * @return The {@link SQLExceptionConversionDelegate} for this dialect
	 */
	public SQLExceptionConversionDelegate buildSQLExceptionConversionDelegate() {
		return null;
	}

	private static final ViolatedConstraintNameExtractor EXTRACTOR = sqle -> null;

	/**
	 * A {@link ViolatedConstraintNameExtractor} for extracting the name of
	 * a violated constraint from a {@link SQLException}.
	 */
	public ViolatedConstraintNameExtractor getViolatedConstraintNameExtractor() {
		return EXTRACTOR;
	}


	// union subclass support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Given a {@linkplain Types JDBC type code}, return the expression
	 * for a literal null value of that type, to use in a {@code select}
	 * clause.
	 * <p>
	 * The {@code select} query will be an element of a {@code UNION}
	 * or {@code UNION ALL}.
	 *
	 * @implNote Some databases require an explicit type cast.
	 *
	 * @param sqlType The {@link Types} type code.
	 * @param typeConfiguration The type configuration
	 * @return The appropriate select clause value fragment.
	 */
	public String getSelectClauseNullString(int sqlType, TypeConfiguration typeConfiguration) {
		return "null";
	}

	/**
	 * Does this dialect support {@code UNION ALL}?
	 *
	 * @return True if {@code UNION ALL} is supported; false otherwise.
	 */
	public boolean supportsUnionAll() {
		return true;
	}

	/**
	 * Does this dialect support {@code UNION} in a subquery.
	 *
	 * @return True if {@code UNION} is supported in a subquery; false otherwise.
	 */
	public boolean supportsUnionInSubquery() {
		return supportsUnionAll();
	}

	// miscellaneous support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * The fragment used to insert a row without specifying any column values,
	 * usually just {@code ()}, but sometimes {@code default values}.
	 *
	 * @implNote On the other hand, this is simply not possible on some databases!
	 *
	 * @return The appropriate empty values clause.
	 *
	 * @deprecated Override the method {@code renderInsertIntoNoColumns()}
	 *             on the {@link #getSqlAstTranslatorFactory() translator}
	 *             returned by this dialect.
	 */
	@Deprecated( since = "6" )
	public String getNoColumnsInsertString() {
		return "values ( )";
	}

	/**
	 * Is the {@code INSERT} statement is allowed to contain no columns?
	 *
	 * @return if this dialect supports no-column {@code INSERT}.
	 */
	public boolean supportsNoColumnsInsert() {
		return true;
	}

	/**
	 * The name of the SQL function that transforms a string to lowercase,
	 * almost always {@code lower}.
	 *
	 * @return The dialect-specific lowercase function.
	 */
	public String getLowercaseFunction() {
		return "lower";
	}

	/**
	 * The name of the SQL operator that performs case-insensitive {@code LIKE}
	 * comparisons.
	 *
	 * @return The dialect-specific case-insensitive like operator.
	 */
	public String getCaseInsensitiveLike(){
		return "like";
	}

	/**
	 * Does this dialect support case-insensitive {@code LIKE} comparisons?
	 *
	 * @return {@code true} if the database supports case-insensitive like
	 *         comparisons, {@code false} otherwise.
	 *         The default is {@code false}.
	 */
	public boolean supportsCaseInsensitiveLike(){
		return false;
	}

	/**
	 * Does this dialect support truncation of values to a specified length
	 * via a {@code cast}?
	 *
	 * @return {@code true} if the database supports truncation via a cast,
	 *         {@code false} otherwise.
	 *         The default is {@code true}.
	 */
	public boolean supportsTruncateWithCast(){
		return true;
	}

	/**
	 * Does this dialect support the {@code is true} and {@code is false}
	 * operators?
	 *
	 * @return {@code true} if the database supports {@code is true} and
	 *         {@code is false}, or {@code false} if it does not. The
	 *         default is {@code is false}.
	 */
	public boolean supportsIsTrue() {
		return false;
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
	 *
	 * @implNote
	 * The maximum here should account for the fact that Hibernate often needs
	 * to append "uniqueing" information to the end of generated aliases.
	 * That "uniqueing" information will be added to the end of an identifier
	 * generated to the length specified here; so be sure to leave some room
	 * (generally speaking 5 positions will suffice).
	 *
	 * @return The maximum length.
	 */
	public int getMaxAliasLength() {
		return 10;
	}

	/**
	 * What is the maximum identifier length supported by this dialect?
	 *
	 * @return The maximum length.
	 */
	public int getMaxIdentifierLength() {
		return Integer.MAX_VALUE;
	}

	/**
	 * The SQL literal expression representing the given boolean value.
	 *
	 * @param bool The boolean value
	 * @return The appropriate SQL literal.
	 */
	public String toBooleanValueString(boolean bool) {
		final StringBuilder sb = new StringBuilder();
		appendBooleanValueString( new StringBuilderSqlAppender( sb ), bool );
		return sb.toString();
	}

	/**
	 * Append the SQL literal expression representing the given boolean
	 * value to the given {@link SqlAppender}.
	 *
	 * @param bool The boolean value
	 * @param appender The {@link SqlAppender} to append the literal expression to
	 */
	public void appendBooleanValueString(SqlAppender appender, boolean bool) {
		appender.appendSql( bool ? '1' : '0' );
	}


	// keyword support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Register a keyword.
	 *
	 * @param word a reserved word in this SQL dialect
	 */
	protected void registerKeyword(String word) {
		// When tokens are checked for keywords, they are always compared against the lowercase version of the token.
		// For instance, Template#renderWhereStringTemplate transforms all tokens to lowercase too.
		sqlKeywords.add( word.toLowerCase( Locale.ROOT ) );
	}

	/**
	 * The keywords of this SQL dialect.
	 */
	public Set<String> getKeywords() {
		return sqlKeywords;
	}

	/**
	 * The {@link IdentifierHelper} indicated by this dialect for handling identifier conversions.
	 * Returning {@code null} is allowed and indicates that Hibernate should fall back to building
	 * a "standard" helper. In the fallback path, any changes made to the IdentifierHelperBuilder
	 * during this call will still be incorporated into the built IdentifierHelper.
	 * <p>
	 * The incoming builder will have the following set:
	 * <ul>
	 *     <li>{@link IdentifierHelperBuilder#isGloballyQuoteIdentifiers()}</li>
	 *     <li>{@link IdentifierHelperBuilder#getUnquotedCaseStrategy()} - initialized to UPPER</li>
	 *     <li>{@link IdentifierHelperBuilder#getQuotedCaseStrategy()} - initialized to MIXED</li>
	 * </ul>
	 * <p>
	 * By default, Hibernate will do the following:
	 * <ul>
	 *     <li>Call {@link IdentifierHelperBuilder#applyIdentifierCasing(DatabaseMetaData)}
	 *     <li>Call {@link IdentifierHelperBuilder#applyReservedWords(DatabaseMetaData)}
	 *     <li>Applies {@link AnsiSqlKeywords#sql2003()} as reserved words</li>
	 *     <li>Applies the {#link #sqlKeywords} collected here as reserved words</li>
	 *     <li>Applies the Dialect's {@link NameQualifierSupport}, if it defines one</li>
	 * </ul>
	 *
	 * @param builder A partially-configured {@link IdentifierHelperBuilder}.
	 * @param dbMetaData Access to the metadata returned from the driver if needed and if available.
	 *                   <em>WARNING:</em> it may be {@code null}.
	 *
	 * @return The {@link IdentifierHelper} instance to use,
	 *         or {@code null} to indicate Hibernate should use its fallback path
	 *
	 * @throws SQLException Accessing the {@link DatabaseMetaData} can throw it.
	 *                      Just rethrow and Hibernate will handle it.
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
	 * @return The dialect-specific open quote character.
	 */
	public char openQuote() {
		return '"';
	}

	/**
	 * The character specific to this dialect used to close a quoted identifier.
	 *
	 * @return The dialect-specific close quote character.
	 */
	public char closeQuote() {
		return '"';
	}

	/**
	 * Apply dialect-specific quoting.
	 *
	 * @param name The value to be quoted.
	 * @return The quoted value.
	 * @see #openQuote()
	 * @see #closeQuote()
	 */
	public String toQuotedIdentifier(String name) {
		if ( name == null ) {
			return null;
		}

		return openQuote() + name + closeQuote();
	}

	/**
	 * Apply dialect-specific quoting if the given name is quoted using backticks.
	 * <p>
	 * By default, the incoming name is checked to see if its first character is
	 * a backtick ({@code `}). If it is, the dialect specific quoting is applied.
	 *
	 * @param name The value to be quoted.
	 * @return The quoted (or unmodified, if not starting with backtick) value.
	 * @see #openQuote()
	 * @see #closeQuote()
	 */
	public String quote(String name) {
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

	/**
	 * The {@link SchemaManagementTool} to use if none is explicitly specified.
	 *
	 * @apiNote Allows implementations to override how schema tooling works by default
	 *
	 * @return a {@link HibernateSchemaManagementTool} by default
	 */
	@Incubating
	public SchemaManagementTool getFallbackSchemaManagementTool(
			Map<String, Object> configurationValues,
			ServiceRegistryImplementor registry) {
		return new HibernateSchemaManagementTool();
	}

	private final StandardTableExporter tableExporter = new StandardTableExporter( this );
	private final StandardUserDefinedTypeExporter userDefinedTypeExporter = new StandardUserDefinedTypeExporter( this );
	private final StandardSequenceExporter sequenceExporter = new StandardSequenceExporter( this );
	private final StandardIndexExporter indexExporter = new StandardIndexExporter( this );
	private final StandardForeignKeyExporter foreignKeyExporter = new StandardForeignKeyExporter( this );
	private final StandardUniqueKeyExporter uniqueKeyExporter = new StandardUniqueKeyExporter( this );
	private final StandardAuxiliaryDatabaseObjectExporter auxiliaryObjectExporter = new StandardAuxiliaryDatabaseObjectExporter( this );
	private final StandardTemporaryTableExporter temporaryTableExporter = new StandardTemporaryTableExporter( this );
	private final StandardTableMigrator tableMigrator = new StandardTableMigrator( this );
	private final StandardTableCleaner tableCleaner = new StandardTableCleaner( this );

	/**
	 * Get an {@link Exporter} for {@link Table}s,
	 * usually {@link StandardTableExporter}.
	 */
	public Exporter<Table> getTableExporter() {
		return tableExporter;
	}

	/**
	 * Get a {@link TableMigrator},
	 * usually {@link StandardTableMigrator}.
	 */
	public TableMigrator getTableMigrator() {
		return tableMigrator;
	}

	/**
	 * Get a schema {@link Cleaner},
	 * usually {@link StandardTableCleaner}.
	 */
	public Cleaner getTableCleaner() {
		return tableCleaner;
	}

	/**
	 * Get an {@link Exporter} for {@link UserDefinedType}s,
	 * usually {@link StandardUserDefinedTypeExporter}.
	 */
	public Exporter<UserDefinedType> getUserDefinedTypeExporter() {
		return userDefinedTypeExporter;
	}

	/**
	 * Get an {@link Exporter} for {@link Sequence}s,
	 * usually {@link StandardSequenceExporter}.
	 */
	public Exporter<Sequence> getSequenceExporter() {
		return sequenceExporter;
	}

	/**
	 * Get an {@link Exporter} for {@link Index}es,
	 * usually {@link StandardIndexExporter}.
	 */
	public Exporter<Index> getIndexExporter() {
		return indexExporter;
	}

	/**
	 * Get an {@link Exporter} for {@link ForeignKey}s,
	 * usually {@link StandardForeignKeyExporter}.
	 */
	public Exporter<ForeignKey> getForeignKeyExporter() {
		return foreignKeyExporter;
	}

	/**
	 * Get an {@link Exporter} for unique key {@link Constraint}s,
	 * usually {@link StandardUniqueKeyExporter}.
	 */
	public Exporter<Constraint> getUniqueKeyExporter() {
		return uniqueKeyExporter;
	}

	/**
	 * Get an {@link Exporter} for {@link AuxiliaryDatabaseObject}s,
	 * usually {@link StandardAuxiliaryDatabaseObjectExporter}.
	 */
	public Exporter<AuxiliaryDatabaseObject> getAuxiliaryDatabaseObjectExporter() {
		return auxiliaryObjectExporter;
	}

	// Temporary table support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Get a {@link TemporaryTableExporter},
	 * usually {@link StandardTemporaryTableExporter}.
	 */
	public TemporaryTableExporter getTemporaryTableExporter() {
		return temporaryTableExporter;
	}

	/**
	 * The kind of temporary tables that are supported on this database.
	 */
	public TemporaryTableKind getSupportedTemporaryTableKind() {
		return TemporaryTableKind.PERSISTENT;
	}

	/**
	 * An arbitrary SQL fragment appended to the end of the statement to
	 * create a temporary table, specifying dialect-specific options, or
	 * {@code null} if there are no options to specify.
	 */
	public String getTemporaryTableCreateOptions() {
		return null;
	}

	/**
	 * The command to create a temporary table.
	 */
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

	/**
	 * The command to drop a temporary table.
	 */
	public String getTemporaryTableDropCommand() {
		return "drop table";
	}

	/**
	 * The command to truncate a temporary table.
	 */
	public String getTemporaryTableTruncateCommand() {
		return "delete from";
	}

	/**
	 * Annotation to be appended to the end of each COLUMN clause for temporary tables.
	 *
	 * @param sqlTypeCode The SQL type code
	 * @return The annotation to be appended, for example, {@code COLLATE DATABASE_DEFAULT} in SQL Server
	 */
	public String getCreateTemporaryTableColumnAnnotation(int sqlTypeCode) {
		return "";
	}

	/**
	 * The sort of {@linkplain TempTableDdlTransactionHandling transaction handling}
	 * to use when creating or dropping temporary tables.
	 */
	public TempTableDdlTransactionHandling getTemporaryTableDdlTransactionHandling() {
		return TempTableDdlTransactionHandling.NONE;
	}

	/**
	 * The action to take after finishing use of a temporary table.
	 */
	public AfterUseAction getTemporaryTableAfterUseAction() {
		return AfterUseAction.CLEAN;
	}

	/**
	 * The action to take before beginning use of a temporary table.
	 */
	public BeforeUseAction getTemporaryTableBeforeUseAction() {
		return BeforeUseAction.NONE;
	}

	// Catalog / schema creation ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Does this dialect support creating and dropping catalogs?
	 *
	 * @return True if the dialect supports catalog creation; false otherwise.
	 */
	public boolean canCreateCatalog() {
		return false;
	}

	/**
	 * Get the SQL command used to create the named catalog.
	 *
	 * @param catalogName The name of the catalog to be created.
	 *
	 * @return The creation commands
	 */
	public String[] getCreateCatalogCommand(String catalogName) {
		throw new UnsupportedOperationException( "No create catalog syntax supported by " + getClass().getName() );
	}

	/**
	 * Get the SQL command used to drop the named catalog.
	 *
	 * @param catalogName The name of the catalog to be dropped.
	 *
	 * @return The drop commands
	 */
	public String[] getDropCatalogCommand(String catalogName) {
		throw new UnsupportedOperationException( "No drop catalog syntax supported by " + getClass().getName() );
	}

	/**
	 * Does this dialect support creating and dropping schema?
	 *
	 * @return True if the dialect supports schema creation; false otherwise.
	 */
	public boolean canCreateSchema() {
		return true;
	}

	/**
	 * Get the SQL command used to create the named schema.
	 *
	 * @param schemaName The name of the schema to be created.
	 *
	 * @return The creation commands
	 */
	public String[] getCreateSchemaCommand(String schemaName) {
		return new String[] {"create schema " + schemaName};
	}

	/**
	 * Get the SQL command used to drop the named schema.
	 *
	 * @param schemaName The name of the schema to be dropped.
	 *
	 * @return The drop commands
	 */
	public String[] getDropSchemaCommand(String schemaName) {
		return new String[] {"drop schema " + schemaName};
	}

	/**
	 * Get the SQL command used to retrieve the current schema name.
	 * <p>
	 * Works in conjunction with {@link #getSchemaNameResolver()},
	 * unless the resulting {@link SchemaNameResolver} does not need
	 * this information. For example, a custom implementation might
	 * make use of the Java 1.7 {@link Connection#getSchema()} method.
	 *
	 * @return The current schema retrieval SQL
	 */
	public String getCurrentSchemaCommand() {
		return null;
	}

	/**
	 * Get the strategy for determining the schema name from a JDBC
	 * {@link Connection}, usually {@link DefaultSchemaNameResolver}.
	 *
	 * @return The schema name resolver strategy
	 */
	public SchemaNameResolver getSchemaNameResolver() {
		return DefaultSchemaNameResolver.INSTANCE;
	}

	/**
	 * Does the database/driver have bug in deleting rows that refer to
	 * other rows being deleted in the same query?
	 *
	 * @implNote The main culprit is MySQL.
	 *
	 * @return {@code true} if the database/driver has this bug
	 */
	public boolean hasSelfReferentialForeignKeyBug() {
		return false;
	}

	/**
	 * The keyword used to specify a nullable column, usually {@code ""},
	 * but sometimes {@code " null"}.
	 */
	public String getNullColumnString() {
		return "";
	}

	/**
	 * The keyword used to specify a nullable column of the given SQL type.
	 *
	 * @implNote The culprit is {@code timestamp} columns on MySQL.
	 */
	public String getNullColumnString(String columnType) {
		return getNullColumnString();
	}

	/**
	 * Quote the given collation name if necessary.
	 */
	public String quoteCollation(String collation) {
		return collation;
	}

	/**
	 * Does this dialect support commenting on tables and columns?
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
	 * Get the comment into a form supported for UDT definition.
	 *
	 * @param comment The comment to apply
	 *
	 * @return The comment fragment
	 */
	public String getUserDefinedTypeComment(String comment) {
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
	 * Does this dialect support column-level check constraints?
	 *
	 * @return True if column-level {@code check} constraints are supported;
	 *         false otherwise.
	 */
	public boolean supportsColumnCheck() {
		return true;
	}

	/**
	 * Does this dialect support table-level check constraints?
	 *
	 * @return True if table-level {@code check} constraints are supported;
	 *         false otherwise.
	 */
	public boolean supportsTableCheck() {
		return true;
	}

	/**
	 * Does this dialect support {@code on delete} actions in foreign key definitions?
	 *
	 * @return {@code true} if the dialect does support the {@code on delete} clause.
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

	/**
	 * A {@link ColumnAliasExtractor}, usually just {@link ResultSetMetaData#getColumnLabel}.
	 */
	public ColumnAliasExtractor getColumnAliasExtractor() {
		return ColumnAliasExtractor.COLUMN_LABEL_EXTRACTOR;
	}

	// Informational metadata ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Should LOBs (both BLOB and CLOB) be bound using stream operations,
	 * that is, using {@link PreparedStatement#setBinaryStream}).
	 *
	 * @return True if BLOBs and CLOBs should be bound using stream operations.
	 *
	 * @since 3.2
	 */
	public boolean useInputStreamToInsertBlob() {
		return true;
	}

	/**
	 * Should BLOB, CLOB, and NCLOB be created solely using respectively
	 * {@link Connection#createBlob()}, {@link Connection#createClob()},
	 * and {@link Connection#createNClob()}.
	 *
	 * @return True if BLOB, CLOB, and NCLOB should be created using JDBC
	 * {@link Connection}.
	 *
	 * @since 6.6
	 */
	public boolean useConnectionToCreateLob() {
		return !useInputStreamToInsertBlob();
	}

	/**
	 * Does this dialect support parameters within the {@code SELECT} clause
	 * of {@code INSERT ... SELECT ...} statements?
	 *
	 * @return True if this is supported; false otherwise.
	 *
	 * @since 3.2
	 *
	 * @deprecated This seems to be supported on all platforms, and we don't
	 *             call this except in test suite
	 */
	@Deprecated(since = "6", forRemoval = true)
	public boolean supportsParametersInInsertSelect() {
		return true;
	}

	/**
	 * Does this dialect support references to result variables
	 * (i.e, select items) by column positions (1-origin) as defined
	 * by the select clause?

	 * @return true if result variable references by column positions
	 *         are supported; false otherwise.
	 *
	 * @since 6.0.0
	 */
	public boolean supportsOrdinalSelectItemReference() {
		return true;
	}

	/**
	 * Returns the default ordering of null.
	 *
	 * @since 6.0.0
	 */
	public NullOrdering getNullOrdering() {
		return NullOrdering.GREATEST;
	}

	/**
	 * Does this dialect support {@code nulls first} and {@code nulls last}?
	 */
	public boolean supportsNullPrecedence() {
		return true;
	}

	/**
	 * A setting specific to {@link SybaseASEDialect}.
	 *
	 * @deprecated This is only called from {@link SybaseASESqlAstTranslator}
	 *             so it doesn't need to be declared here.
	 */
	@Deprecated(since = "6")
	public boolean isAnsiNullOn() {
		return true;
	}

	/**
	 * Does this dialect/database require casting of non-string arguments
	 * in the {@code concat()} function?
	 *
	 * @return {@code true} if casting using {@code cast()} is required
	 *
	 * @since 6.2
	 */
	public boolean requiresCastForConcatenatingNonStrings() {
		return false;
	}

	/**
	 * Does this dialect require that integer divisions be wrapped in
	 * {@code cast()} calls to tell the db parser the expected type.
	 *
	 * @implNote The culprit is HSQLDB.
	 *
	 * @return True if integer divisions must be {@code cast()}ed to float
	 */
	public boolean requiresFloatCastingOfIntegerDivision() {
		return false;
	}

	/**
	 * Does this dialect support asking the result set its positioning
	 * information on forward-only cursors?
	 * <p>
	 * Specifically, in the case of scrolling fetches, Hibernate needs
	 * to use {@link ResultSet#isAfterLast} and
	 * {@link ResultSet#isBeforeFirst}. Certain drivers do not allow
	 * access to these methods for forward-only cursors.
	 *
	 * @apiNote This is highly driver dependent!
	 *
	 * @return True if methods like {@link ResultSet#isAfterLast} and
	 *         {@link ResultSet#isBeforeFirst} are supported for forward
	 *         only cursors; false otherwise.
	 *
	 * @since 3.2
	 */
	public boolean supportsResultSetPositionQueryMethodsOnForwardOnlyCursor() {
		return true;
	}

	/**
	 * Does this dialect support definition of cascade delete constraints
	 * which can cause circular chains?
	 *
	 * @return True if circular cascade delete constraints are supported;
	 *         false otherwise.
	 *
	 * @since 3.2
	 */
	public boolean supportsCircularCascadeDeleteConstraints() {
		return true;
	}

	/**
	 * Is a subselect supported as the left-hand side (LHS) of an {@code IN}
	 * predicates?
	 * <p>
	 * In other words, is syntax like {@code <subquery> IN (1, 2, 3)} supported?
	 *
	 * @return True if a subselect can appear as the LHS of an in-predicate;
	 *         false otherwise.
	 *
	 * @since 3.2
	 */
	public boolean supportsSubselectAsInPredicateLHS() {
		return true;
	}

	/**
	 * "Expected" LOB usage pattern is such that I can perform an insert via
	 * prepared statement with a parameter binding for a LOB value without
	 * crazy casting to JDBC driver implementation-specific classes.
	 *
	 * @implNote Part of the trickiness here is the fact that this is largely
	 *           driver-dependent. For example, Oracle (which is notoriously
	 *           bad with LOB support in their drivers historically) actually
	 *           does a pretty good job with LOB support as of the 10.2.x v
	 *           ersions of their driver.
	 *
	 * @return True if normal LOB usage patterns can be used with this driver;
	 *         false if driver-specific hookiness needs to be applied.
	 *
	 * @since 3.2
	 */
	public boolean supportsExpectedLobUsagePattern() {
		return true;
	}

	/**
	 * Does the dialect support propagating changes to LOB values back
	 * to the database? Talking about mutating the internal value of
	 * the locator, as opposed to supplying a new locator instance.
	 * <ul>
	 * <li>For BLOBs, the internal value might be changed by:
	 *     {@link Blob#setBinaryStream},
	 *     {@link Blob#setBytes(long, byte[])},
	 *     {@link Blob#setBytes(long, byte[], int, int)},
	 *     or {@link Blob#truncate(long)}.
	 * <li>For CLOBs, the internal value might be changed by:
	 *     {@link Clob#setAsciiStream(long)},
	 *     {@link Clob#setCharacterStream(long)},
	 *     {@link Clob#setString(long, String)},
	 *     {@link Clob#setString(long, String, int, int)},
	 *     or {@link Clob#truncate(long)}.
	 *</ul>
	 *
	 * @implNote I do not know the correct answer currently for databases
	 *           which (1) are not part of the cruise control process, or
	 *           (2) do not {@link #supportsExpectedLobUsagePattern}.
	 *
	 * @return True if the changes are propagated back to the database;
	 *         false otherwise.
	 *
	 * @since 3.2
	 */
	public boolean supportsLobValueChangePropagation() {
		// todo : pretty sure this is the same as the
		//        java.sql.DatabaseMetaData.locatorsUpdateCopy()
		//        method added in JDBC 4, see HHH-6046
		return true;
	}

	/**
	 * Is it supported to materialize a LOB locator outside the transaction
	 * in which it was created?
	 *
	 * @implNote Again, part of the trickiness here is the fact that this is
	 *          largely driver-dependent. All database I have tested which
	 *          {@link #supportsExpectedLobUsagePattern()} also support the
	 *          ability to materialize a LOB outside the owning transaction.
	 *
	 * @return True if unbounded materialization is supported; false otherwise.
	 *
	 * @since 3.2
	 */
	public boolean supportsUnboundedLobLocatorMaterialization() {
		return true;
	}

	/**
	 * Does this dialect support referencing the table being mutated in a
	 * subquery? The "table being mutated" is the table referenced in an
	 * update or delete query. And so can that table then be referenced
	 * in a subquery of the update or delete query?
	 * <p>
	 * For example, would the following two syntaxes be supported:
	 * <ul>
	 * <li>{@code delete from TABLE_A where ID not in (select ID from TABLE_A)}
	 * <li>{@code update TABLE_A set NON_ID = 'something' where ID in (select ID from TABLE_A)}
	 * </ul>
	 *
	 * @return True if this dialect allows references the mutating table
	 *         from a subquery.
	 */
	public boolean supportsSubqueryOnMutatingTable() {
		return true;
	}

	/**
	 * Does the dialect support an exists statement in the select clause?
	 *
	 * @return True if exists checks are allowed in the select clause;
	 *         false otherwise.
	 */
	public boolean supportsExistsInSelect() {
		return true;
	}

	/**
	 * For the underlying database, is {@code READ_COMMITTED} isolation
	 * implemented by forcing readers to wait for write locks to be released?
	 *
	 * @return True if writers block readers to achieve {@code READ_COMMITTED};
	 *         false otherwise.
	 */
	public boolean doesReadCommittedCauseWritersToBlockReaders() {
		return false;
	}

	/**
	 * For the underlying database, is {@code REPEATABLE_READ} isolation
	 * implemented by forcing writers to wait for read locks to be released?
	 *
	 * @return True if readers block writers to achieve {@code REPEATABLE_READ};
	 *         false otherwise.
	 */
	public boolean doesRepeatableReadCauseReadersToBlockWriters() {
		return false;
	}

	/**
	 * Does this dialect support using a JDBC bind parameter as an argument
	 * to a function or procedure call?
	 *
	 * @return Returns {@code true} if the database supports accepting bind
	 *         params as args, {@code false} otherwise. The default is
	 *         {@code true}.
	 */
	public boolean supportsBindAsCallableArgument() {
		return true;
	}

	/**
	 * Does this dialect support {@code count(a,b)}?
	 *
	 * @return True if the database supports counting tuples; false otherwise.
	 */
	public boolean supportsTupleCounts() {
		return false;
	}

	/**
	 * If {@link #supportsTupleCounts()} is true, does this dialect require
	 * the tuple to be delimited with parentheses?
	 *
	 * @return boolean
	 */
	public boolean requiresParensForTupleCounts() {
		return supportsTupleCounts();
	}

	/**
	 * Does this dialect support {@code count(distinct a,b)}?
	 *
	 * @return True if the database supports counting distinct tuples;
	 *         false otherwise.
	 */
	public boolean supportsTupleDistinctCounts() {
		// oddly most database in fact seem to, so true is the default.
		return true;
	}

	/**
	 * If {@link #supportsTupleDistinctCounts()} is true, does this dialect
	 * require the tuple to be delimited with parentheses?
	 *
	 * @return boolean
	 */
	public boolean requiresParensForTupleDistinctCounts() {
		return false;
	}

	/**
	 * Return the limit that the underlying database places on the number of
	 * elements in an {@code IN} predicate. If the database defines no such
	 * limits, simply return zero or a number smaller than zero.
	 *
	 * @return The limit, or a non-positive integer to indicate no limit.
	 */
	public int getInExpressionCountLimit() {
		return 0;
	}

	/**
	 * Return the limit that the underlying database places on the number of parameters
	 * that can be defined for a PreparedStatement.  If the database defines no such
	 * limits, simply return zero or a number smaller than zero.  By default, Dialect
	 * returns the same value as {@link #getInExpressionCountLimit()}.
	 *
	 * @return The limit, or a non-positive integer to indicate no limit.
	 */
	public int getParameterCountLimit() {
		return getInExpressionCountLimit();
	}

	/**
	 * Must LOB values occur last in inserts and updates?
	 *
	 * @implNote Oracle is the culprit here, see HHH-4635.
	 *
	 * @return boolean True if Lob values should be last, false if it
	 *                 does not matter.
	 */
	public boolean forceLobAsLastValue() {
		return false;
	}

	/**
	 * Return whether the dialect considers an empty string value to be null.
	 *
	 * @implNote Once again, the culprit is Oracle.
	 *
	 * @return boolean True if an empty string is treated as null, false otherwise.
	 */
	public boolean isEmptyStringTreatedAsNull() {
		return false;
	}

	/**
	 * Some dialects have trouble applying pessimistic locking depending
	 * upon what other query options are specified (paging, ordering, etc).
	 * This method allows these dialects to request that locking be applied
	 * by subsequent selects.
	 *
	 * @return {@code true} indicates that the dialect requests that locking
	 *                      be applied by subsequent select;
	 *         {@code false} (the default) indicates that locking
	 *                      should be applied to the main SQL statement.
	 *
	 * @since 5.2
	 */
	public boolean useFollowOnLocking(String sql, QueryOptions queryOptions) {
		return false;
	}

	/**
	 * Get the {@link UniqueDelegate} supported by this dialect
	 *
	 * @return The UniqueDelegate
	 */
	public UniqueDelegate getUniqueDelegate() {
		return new AlterTableUniqueDelegate( this );
	}

	/**
	 * Apply a hint to the given SQL query.
	 * <p>
	 * The entire query is provided, allowing full control over the placement
	 * and syntax of the hint.
	 * <p>
	 * By default, ignore the hint and simply return the query.
	 *
	 * @param query The query to which to apply the hint.
	 * @param hintList The hints to apply
	 * @return The modified SQL
	 */
	public String getQueryHintString(String query, List<String> hintList) {
		final String hints = String.join( ", ", hintList );
		return StringHelper.isEmpty( hints ) ? query : getQueryHintString( query, hints);
	}

	/**
	 * Apply a hint to the given SQL query.
	 * <p>
	 * The entire query is provided, allowing full control over the placement
	 * and syntax of the hint.
	 * <p>
	 * By default, ignore the hint and simply return the query.
	 *
	 * @param query The query to which to apply the hint.
	 * @param hints The hints to apply
	 * @return The modified SQL
	 */
	public String getQueryHintString(String query, String hints) {
		return query;
	}

	/**
	 * A default {@link ScrollMode} to be used by {@link Query#scroll()}.
	 *
	 * @apiNote Certain dialects support a subset of {@link ScrollMode}s.
	 *
	 * @return the default {@link ScrollMode} to use.
	 */
	public ScrollMode defaultScrollMode() {
		return ScrollMode.SCROLL_INSENSITIVE;
	}

	/**
	 * Does this dialect support {@code offset} in subqueries?
	 * <p>
	 * For example:
	 * <pre>
	 * select * from Table1 where col1 in (select col1 from Table2 order by col2 limit 1 offset 1)
	 * </pre>
	 *
	 * @return {@code true} if it does
	 */
	public boolean supportsOffsetInSubquery() {
		return false;
	}

	/**
	 * Does this dialect support the {@code order by} clause in subqueries?
	 * <p>
	 * For example:
	 * <pre>
	 * select * from Table1 where col1 in (select col1 from Table2 order by col2 limit 1)
	 * </pre>
	 *
	 * @return {@code true} if it does
	 */
	public boolean supportsOrderByInSubquery() {
		return true;
	}

	/**
	 * Does this dialect support subqueries in the {@code select} clause?
	 * <p>
	 * For example:
	 * <pre>
	 * select col1, (select col2 from Table2 where ...) from Table1
	 * </pre>
	 *
	 * @return {@code true} if it does
	 */
	public boolean supportsSubqueryInSelect() {
		return true;
	}

	/**
	 * Does this dialect fully support returning arbitrary generated column values
	 * after execution of an {@code insert} statement, using native SQL syntax?
	 * <p>
	 * Support for identity columns is insufficient here, we require something like:
	 * <ol>
	 * <li>{@code insert ... returning ...}
	 * <li>{@code select from final table (insert ... )}
	 * </ol>
	 *
	 * @return {@code true} if {@link org.hibernate.id.insert.InsertReturningDelegate}
	 *         works for any sort of primary key column (not just identity columns), or
	 *         {@code false} if {@code InsertReturningDelegate} does not work, or only
	 *         works for specialized identity/"autoincrement" columns
	 *
	 * @see org.hibernate.id.insert.InsertReturningDelegate
	 *
	 * @since 6.2
	 */
	public boolean supportsInsertReturning() {
		return false;
	}

	/**
	 * Does this dialect supports returning the {@link org.hibernate.annotations.RowId} column
	 * after execution of an {@code insert} statement, using native SQL syntax?
	 *
	 * @return {@code true} is the dialect supports returning the rowid column
	 *
	 * @see #supportsInsertReturning()
	 * @since 6.5
	 */
	public boolean supportsInsertReturningRowId() {
		return supportsInsertReturning();
	}

	/**
	 * Does this dialect fully support returning arbitrary generated column values
	 * after execution of an {@code update} statement, using native SQL syntax?
	 * <p>
	 * Defaults to the value of {@link #supportsInsertReturning()} but can be overridden
	 * to explicitly disable this for updates.
	 *
	 * @see #supportsInsertReturning()
	 * @since 6.5
	 */
	public boolean supportsUpdateReturning() {
		return supportsInsertReturning();
	}

	/**
	 * Does this dialect fully support returning arbitrary generated column values
	 * after execution of an {@code insert} statement, using the JDBC method
	 * {@link Connection#prepareStatement(String, String[])}.
	 * <p>
	 * Support for returning the generated value of an identity column via the JDBC
	 * method {@link Connection#prepareStatement(String, int)} is insufficient here.
	 *
	 * @return {@code true} if {@link org.hibernate.id.insert.GetGeneratedKeysDelegate}
	 *         works for any sort of primary key column (not just identity columns), or
	 *         {@code false} if {@code GetGeneratedKeysDelegate} does not work, or only
	 *         works for specialized identity/"autoincrement" columns
	 *
	 * @see org.hibernate.generator.OnExecutionGenerator#getGeneratedIdentifierDelegate
	 * @see org.hibernate.id.insert.GetGeneratedKeysDelegate
	 *
	 * @since 6.2
	 */
	public boolean supportsInsertReturningGeneratedKeys() {
		return false;
	}

	/**
	 * Does this dialect require unquoting identifiers when passing them to the
	 * {@link Connection#prepareStatement(String, String[])} JDBC method.
	 *
	 * @see Dialect#supportsInsertReturningGeneratedKeys()
	 */
	public boolean unquoteGetGeneratedKeys() {
		return false;
	}

	/**
	 * Does this dialect support the given {@code FETCH} clause type.
	 *
	 * @param type The fetch clause type
	 * @return {@code true} if the underlying database supports the given
	 *         fetch clause type, {@code false} otherwise.
	 *         The default is {@code false}.
	 */
	public boolean supportsFetchClause(FetchClauseType type) {
		return false;
	}

	/**
	 * Does this dialect support window functions like {@code row_number() over (..)}?
	 *
	 * @return {@code true} if the underlying database supports window
	 *         functions, {@code false} otherwise.
	 *         The default is {@code false}.
	 */
	public boolean supportsWindowFunctions() {
		return false;
	}

	/**
	 * Does this dialect support the SQL {@code lateral} keyword or a
	 * proprietary alternative?
	 *
	 * @return {@code true} if the underlying database supports lateral,
	 * {@code false} otherwise.  The default is {@code false}.
	 */
	public boolean supportsLateral() {
		return false;
	}

	/**
	 * The {@link CallableStatementSupport} for this database.
	 * Does this database support returning cursors?
	 */
	public CallableStatementSupport getCallableStatementSupport() {
		// most databases do not support returning cursors (ref_cursor)...
		return StandardCallableStatementSupport.NO_REF_CURSOR_INSTANCE;
	}

	/**
	 * The {@linkplain NameQualifierSupport support for qualified identifiers}.
	 * <p>
	 * By default, decide based on {@link DatabaseMetaData}.
	 *
	 * @return The {@link NameQualifierSupport}, or null to use {@link DatabaseMetaData}.
	 */
	public NameQualifierSupport getNameQualifierSupport() {
		return null;
	}

	/**
	 * The strategy used to determine the appropriate number of keys
	 * to load in a single SQL query with multi-key loading.
	 * @see org.hibernate.Session#byMultipleIds
	 * @see org.hibernate.Session#byMultipleNaturalId
	 */
	public MultiKeyLoadSizingStrategy getMultiKeyLoadSizingStrategy() {
		return STANDARD_MULTI_KEY_LOAD_SIZING_STRATEGY;
	}

	/**
	 * The strategy used to determine the appropriate number of keys
	 * to load in a single SQL query with batch-fetch loading.
	 *
	 * @implNote By default, the same as {@linkplain #getMultiKeyLoadSizingStrategy}
	 *
	 * @see org.hibernate.annotations.BatchSize
	 */
	public MultiKeyLoadSizingStrategy getBatchLoadSizingStrategy() {
		return getMultiKeyLoadSizingStrategy();
	}

	protected final MultiKeyLoadSizingStrategy STANDARD_MULTI_KEY_LOAD_SIZING_STRATEGY = (numberOfColumns, numberOfKeys, pad) -> {
		numberOfKeys = pad ? MathHelper.ceilingPowerOfTwo( numberOfKeys ) : numberOfKeys;

		final long parameterCount = (long) numberOfColumns * numberOfKeys;
		final int limit = getParameterCountLimit();

		if ( limit > 0 ) {
			// the Dialect reported a limit -  see if the parameter count exceeds the limit
			if ( parameterCount >= limit ) {
				return limit / numberOfColumns;
			}
		}

		return numberOfKeys;
	};

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
	 * Does is dialect support {@code partition by}?
	 *
	 * @since 5.2
	 */
	public boolean supportsPartitionBy() {
		return false;
	}

	/**
	 * Override {@link DatabaseMetaData#supportsNamedParameters()}.
	 *
	 * @throws SQLException Accessing the {@link DatabaseMetaData} cause
	 *                      an exception. Just rethrow and Hibernate will
	 *                      handle it.
	 */
	public boolean supportsNamedParameters(DatabaseMetaData databaseMetaData) throws SQLException {
		return databaseMetaData != null && databaseMetaData.supportsNamedParameters();
	}

	/**
	 * Determines whether this database requires the use of explicitly
	 * nationalized character (Unicode) data types.
	 * <p>
	 * That is, whether the use of {@link Types#NCHAR}, {@link Types#NVARCHAR},
	 * and {@link Types#NCLOB} is required for nationalized character data.
	 */
	public NationalizationSupport getNationalizationSupport() {
		return NationalizationSupport.EXPLICIT;
	}

	/**
	 * How does this dialect support aggregate types like {@link SqlTypes#STRUCT}.
	 *
	 * @since 6.2
	 */
	public AggregateSupport getAggregateSupport() {
		return AggregateSupportImpl.INSTANCE;
	}

	/**
	 * Does this database have native support for ANSI SQL standard arrays which
	 * are expressed in terms of the element type name: {@code integer array}.
	 *
	 * @implNote Oracle doesn't have this; we must instead use named array types.
	 *
	 * @return boolean
	 * @since 6.1
	 */
	public boolean supportsStandardArrays() {
		return false;
	}

	/**
	 * Does this database prefer to use array types for multi-valued parameters.
	 *
	 * @return boolean
	 *
	 * @since 6.3
	 */
	public boolean useArrayForMultiValuedParameters() {
		return supportsStandardArrays() && getPreferredSqlTypeCodeForArray() == SqlTypes.ARRAY;
	}

	/**
	 * The SQL type name for the array type with elements of the given type name.
	 * <p>
	 * The ANSI-standard syntax is {@code integer array}.
	 *
	 * @since 6.1
	 */
	public String getArrayTypeName(String javaElementTypeName, String elementTypeName, Integer maxLength) {
		if ( supportsStandardArrays() ) {
			return maxLength == null
					? elementTypeName + " array"
					: elementTypeName + " array[" + maxLength + "]";
		}
		else {
			return null;
		}
	}

	/**
	 * Append an array literal with the given elements to the given {@link SqlAppender}.
	 */
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
	 * Does this dialect support some kind of {@code distinct from} predicate?
	 * <p>
	 * That is, does it support syntax like:
	 * <pre>
	 * ... where FIRST_NAME IS DISTINCT FROM LAST_NAME
	 * </pre>
	 *
	 * @return True if this SQL dialect is known to support some kind of
	 *         {@code distinct from} predicate; false otherwise
	 *
	 * @since 6.1
	 */
	public boolean supportsDistinctFromPredicate() {
		return false;
	}

	/**
	 * The JDBC {@linkplain SqlTypes type code} to use for mapping
	 * properties of basic Java array or {@code Collection} types.
	 * <p>
	 * Usually {@link SqlTypes#ARRAY} or {@link SqlTypes#VARBINARY}.
	 *
	 * @return one of the type codes defined by {@link SqlTypes}.
	 *
	 * @since 6.1
	 */
	public int getPreferredSqlTypeCodeForArray() {
		return supportsStandardArrays() ? ARRAY : VARBINARY;
	}

	/**
	 * The JDBC {@linkplain Types type code} to use for mapping
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
	 * Does this dialect support insert, update, and delete statements
	 * with Common Table Expressions (CTEs)?
	 *
	 * @return {@code true} if non-query statements are supported with CTE
	 */
	public boolean supportsNonQueryWithCTE() {
		return false;
	}

	/**
	 * Does this dialect/database support recursive CTEs?
	 *
	 * @return {@code true} if recursive CTEs are supported
	 *
	 * @since 6.2
	 */
	public boolean supportsRecursiveCTE() {
		return false;
	}

	/**
	 * Does this dialect support the {@code conflict} clause for insert statements
	 * that appear in a CTE?
	 *
	 * @return {@code true} if {@code conflict} clause is supported
	 * @since 6.5
	 */
	public boolean supportsConflictClauseForInsertCTE() {
		return false;
	}

	/**
	 * Does this dialect support {@code values} lists of form
	 * {@code VALUES (1), (2), (3)}?
	 *
	 * @return {@code true} if {@code values} list are supported
	 */
	public boolean supportsValuesList() {
		return false;
	}

	/**
	 * Does this dialect support {@code values} lists of form
	 * {@code VALUES (1), (2), (3)} in insert statements?
	 *
	 * @return {@code true} if {@code values} list are allowed
	 *         in insert statements
	 */
	public boolean supportsValuesListForInsert() {
		return true;
	}

	/**
	 * Does this dialect support the {@code from} clause for update statements?
	 *
	 * @return {@code true} if {@code from} clause is supported
	 * @since 6.5
	 */
	public boolean supportsFromClauseInUpdate() {
		return false;
	}

	/**
	 * Does this dialect support {@code SKIP_LOCKED} timeout.
	 *
	 * @return {@code true} if SKIP_LOCKED is supported
	 */
	public boolean supportsSkipLocked() {
		return false;
	}

	/**
	 * Does this dialect support {@code NO_WAIT} timeout.
	 *
	 * @return {@code true} if {@code NO_WAIT} is supported
	 */
	public boolean supportsNoWait() {
		return false;
	}

	/**
	 * Does this dialect support {@code WAIT} timeout.
	 *
	 * @return {@code true} if {@code WAIT} is supported
	 */
	public boolean supportsWait() {
		return supportsNoWait();
	}

	/**
	 * @deprecated This is no longer called
	 */
	@Deprecated(since = "6", forRemoval = true)
	public String inlineLiteral(String literal) {
		final StringBuilder sb = new StringBuilder( literal.length() + 2 );
		appendLiteral( new StringBuilderSqlAppender( sb ), literal );
		return sb.toString();
	}

	/**
	 * Append a literal string to the given {@link SqlAppender}.
	 *
	 * @apiNote Needed because MySQL has nonstandard escape characters
	 */
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
	 * Append a binary literal to the given {@link SqlAppender}.
	 */
	public void appendBinaryLiteral(SqlAppender appender, byte[] bytes) {
		appender.appendSql( "X'" );
		PrimitiveByteArrayJavaType.INSTANCE.appendString( appender, bytes );
		appender.appendSql( '\'' );
	}

	/**
	 * Check whether the JDBC {@link Connection} supports creating LOBs via
	 * {@link Connection#createBlob()}, {@link Connection#createNClob()}, or
	 * {@link Connection#createClob()}.
	 *
	 * @param databaseMetaData JDBC {@link DatabaseMetaData} which can be used
	 *                         if LOB creation is supported only starting from
	 *                         a given driver version
	 *
	 * @return {@code true} if LOBs can be created via the JDBC Connection.
	 */
	public boolean supportsJdbcConnectionLobCreation(DatabaseMetaData databaseMetaData) {
		return true;
	}

	/**
	 * Check whether the JDBC driver allows setting LOBs via
	 * {@link PreparedStatement#setBytes(int, byte[])},
	 * {@link PreparedStatement#setNString(int, String)}, or
	 * {@link PreparedStatement#setString(int, String)} APIs.
	 *
	 * @return {@code true} if LOBs can be set with the materialized APIs.
	 *
	 * @since 6.2
	 */
	public boolean supportsMaterializedLobAccess() {
		// Most drivers support this
		return true;
	}

	/**
	 * Whether to switch:
	 * <ul>
	 * <li>from {@code VARCHAR}-like types to {@link SqlTypes#MATERIALIZED_CLOB} types
	 *     when the requested size for a type exceeds the {@link #getMaxVarcharCapacity()},
	 * <li>from {@code NVARCHAR}-like types to {@link SqlTypes#MATERIALIZED_NCLOB} types
	 *     when the requested size for a type exceeds the {@link #getMaxNVarcharCapacity()},
	 *     and
	 * <li>from {@code VARBINARY}-like types to {@link SqlTypes#MATERIALIZED_BLOB} types
	 *     when the requested size for a type exceeds the {@link #getMaxVarbinaryCapacity()}.
	 * </ul>
	 *
	 * @return {@code true} if materialized LOBs should be used for capacity exceeding types.
	 *
	 * @since 6.2
	 */
	public boolean useMaterializedLobWhenCapacityExceeded() {
		return supportsMaterializedLobAccess();
	}

	/**
	 * Modify the SQL, adding hints or comments, if necessary
	 */
	public String addSqlHintOrComment(
			String sql,
			QueryOptions queryOptions,
			boolean commentsEnabled) {
		// Keep this here, rather than moving to Select.
		// Some Dialects may need the hint to be appended to the very end or beginning
		// of the finalized SQL statement, so wait until everything is processed.
		if ( queryOptions.getDatabaseHints() != null && !queryOptions.getDatabaseHints().isEmpty() ) {
			sql = getQueryHintString( sql, queryOptions.getDatabaseHints() );
		}
		if ( commentsEnabled && queryOptions.getComment() != null ) {
			sql = prependComment( sql, queryOptions.getComment() );
		}
		return sql;
	}

	/**
	 * Prepend a comment to the given SQL fragment.
	 */
	protected String prependComment(String sql, String comment) {
		return "/* " + escapeComment( comment ) + " */ " + sql;
	}

	/**
	 * Perform necessary character escaping on the text of the comment.
	 */
	public static String escapeComment(String comment) {
		if ( StringHelper.isNotEmpty( comment ) ) {
			final String escaped = ESCAPE_CLOSING_COMMENT_PATTERN.matcher( comment ).replaceAll( "*\\\\/" );
			return ESCAPE_OPENING_COMMENT_PATTERN.matcher( escaped ).replaceAll( "/\\\\*" );
		}
		return comment;
	}

	/**
	 * Return an {@link HqlTranslator} specific to this dialect, or {@code null}
	 * to use the {@linkplain org.hibernate.query.hql.internal.StandardHqlTranslator
	 * standard translator}.
	 * <p>
	 * Note that {@link SessionFactoryOptions#getCustomHqlTranslator()} has higher
	 * precedence since it comes directly from the user config.
	 *
	 * @see org.hibernate.query.hql.internal.StandardHqlTranslator
	 * @see org.hibernate.query.spi.QueryEngine#getHqlTranslator()
	 */
	public HqlTranslator getHqlTranslator() {
		return null;
	}

	/**
	 * Return a {@link SqmTranslatorFactory} specific to this dialect, or {@code null}
	 * to use the {@linkplain org.hibernate.query.sqm.sql.internal.StandardSqmTranslator
	 * standard translator}.
	 * <p>
	 * Note that {@link SessionFactoryOptions#getCustomSqmTranslatorFactory()} has higher
	 * precedence since it comes directly from the user config.
	 *
	 * @see org.hibernate.query.sqm.sql.internal.StandardSqmTranslator
	 * @see org.hibernate.query.spi.QueryEngine#getSqmTranslatorFactory()
	 */
	public SqmTranslatorFactory getSqmTranslatorFactory() {
		return null;
	}

	/**
	 * Return a {@link SqlAstTranslatorFactory} specific to this dialect, or {@code null}
	 * to use the {@linkplain org.hibernate.sql.ast.spi.StandardSqlAstTranslatorFactory
	 * standard translator}.
	 *
	 * @see org.hibernate.sql.ast.spi.StandardSqlAstTranslatorFactory
	 * @see JdbcEnvironment#getSqlAstTranslatorFactory()
	 */
	public SqlAstTranslatorFactory getSqlAstTranslatorFactory() {
		return null;
	}

	/**
	 * Determine how selected items are referenced in the {@code group by} clause.
	 */
	public SelectItemReferenceStrategy getGroupBySelectItemReferenceStrategy() {
		return SelectItemReferenceStrategy.EXPRESSION;
	}

	/**
	 * A custom {@link SizeStrategy} for column types.
	 */
	public SizeStrategy getSizeStrategy() {
		return sizeStrategy;
	}

	/**
	 * The biggest size value that can be supplied as argument to a
	 * {@link java.sql.Types#VARCHAR}-like type.
	 * <p>
	 * For longer column lengths, use some sort of {@code text}-like
	 * type for the column.
	 */
	public int getMaxVarcharLength() {
		//the longest possible length of a Java string
		return Length.LONG32;
	}

	/**
	 * The biggest size value that can be supplied as argument to a
	 * {@link java.sql.Types#NVARCHAR}-like type.
	 * <p>
	 * For longer column lengths, use some sort of {@code ntext}-like
	 * type for the column.
	 */
	public int getMaxNVarcharLength() {
		//for most databases it's the same as for VARCHAR
		return getMaxVarcharLength();
	}

	/**
	 * The biggest size value that can be supplied as argument to a
	 * {@link java.sql.Types#VARBINARY}-like type.
	 * <p>
	 * For longer column lengths, use some sort of {@code image}-like
	 * type for the column.
	 */
	public int getMaxVarbinaryLength() {
		//for most databases it's the same as for VARCHAR
		return getMaxVarcharLength();
	}

	/**
	 * The longest possible length of a {@link java.sql.Types#VARCHAR}-like
	 * column.
	 * <p>
	 * For longer column lengths, use some sort of {@code clob}-like type
	 * for the column.
	 */
	public int getMaxVarcharCapacity() {
		return getMaxVarcharLength();
	}

	/**
	 * The longest possible length of a {@link java.sql.Types#NVARCHAR}-like
	 * column.
	 * <p>
	 * For longer column lengths, use some sort of {@code nclob}-like type
	 * for the column.
	 */
	public int getMaxNVarcharCapacity() {
		return getMaxNVarcharLength();
	}

	/**
	 * The longest possible length of a {@link java.sql.Types#VARBINARY}-like
	 * column.
	 * <p>
	 * For longer column lengths, use some sort of {@code blob}-like type for
	 * the column.
	 */
	public int getMaxVarbinaryCapacity() {
		return getMaxVarbinaryLength();
	}

	/**
	 * This is the default length for a generated column of type
	 * {@link SqlTypes#BLOB BLOB} or {@link SqlTypes#CLOB CLOB}
	 * mapped to {@link Blob} or {@link Clob}, if LOB columns
	 * have a length in this dialect.
	 *
	 * @return {@value Size#DEFAULT_LOB_LENGTH} by default
	 *
	 * @see Length#LOB_DEFAULT
	 * @see org.hibernate.type.descriptor.java.BlobJavaType
	 * @see org.hibernate.type.descriptor.java.ClobJavaType
	 */
	public long getDefaultLobLength() {
		return Size.DEFAULT_LOB_LENGTH;
	}

	/**
	 * This is the default precision for a generated column of
	 * exact numeric type {@link SqlTypes#DECIMAL DECIMAL} or
	 * {@link SqlTypes#NUMERIC NUMERIC} mapped to a
	 * {@link java.math.BigInteger} or
	 * {@link java.math.BigDecimal}.
	 * <p>
	 * Usually returns the maximum precision of the
	 * database, except when there is no such maximum
	 * precision, or the maximum precision is very high.
	 *
	 * @return the default precision, in decimal digits
	 *
	 * @see org.hibernate.type.descriptor.java.BigDecimalJavaType
	 * @see org.hibernate.type.descriptor.java.BigIntegerJavaType
	 */
	public int getDefaultDecimalPrecision() {
		//this is the maximum for Oracle, SQL Server,
		//Sybase, and Teradata, so it makes a reasonable
		//default (uses 17 bytes on SQL Server and MySQL)
		return 38;
	}

	/**
	 * This is the default precision for a generated column of
	 * type {@link SqlTypes#TIMESTAMP TIMESTAMP} mapped to a
	 * {@link Timestamp} or {@link java.time.LocalDateTime}.
	 * <p>
	 * Usually 6 (microseconds) or 3 (milliseconds).
	 *
	 * @return the default precision, in decimal digits,
	 *         of the fractional seconds field
	 *
	 * @see org.hibernate.type.descriptor.java.JdbcTimestampJavaType
	 * @see org.hibernate.type.descriptor.java.LocalDateTimeJavaType
	 * @see org.hibernate.type.descriptor.java.OffsetDateTimeJavaType
	 * @see org.hibernate.type.descriptor.java.ZonedDateTimeJavaType
	 * @see org.hibernate.type.descriptor.java.InstantJavaType
	 */
	public int getDefaultTimestampPrecision() {
		//milliseconds or microseconds is the maximum
		//for most dialects that support explicit
		//precision, with the exception of Oracle,
		//which accepts up to 9 digits, and DB2 which
		//accepts up to 12 digits!
		return 6; //microseconds!
	}

	/**
	 * This is the default scale for a generated column of type
	 * {@link SqlTypes#INTERVAL_SECOND INTERVAL SECOND} mapped
	 * to a {@link Duration}.
	 * <p>
	 * Usually 9 (nanoseconds) or 6 (microseconds).
	 *
	 * @return the default scale, in decimal digits,
	 *         of the fractional seconds field
	 *
	 * @see org.hibernate.type.descriptor.java.DurationJavaType
	 */
	public int getDefaultIntervalSecondScale(){
		// The default scale necessary is 9 i.e. nanosecond resolution
		return 9;
	}

	/**
	 * Does this dialect round a temporal when converting from a precision higher to a lower one?
	 *
	 * @return true if rounding is applied, false if truncation is applied
	 */
	public boolean doesRoundTemporalOnOverflow() {
		return true;
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
	 *
	 * @see TemporalUnit#NATIVE
	 *
	 * @implNote Getting this right is very important. It
	 *           would be great if all platforms supported
	 *           datetime arithmetic with nanosecond
	 *           precision, since that is how we represent
	 *           {@link Duration}. But they don't, and we
	 *           don't want to fill up the SQL expression
	 *           with many conversions to/from nanoseconds.
	 *           (Not to mention the problems with numeric
	 *           overflow that this sometimes causes.) So
	 *           we need to pick the right value here,
	 *           and implement {@link #timestampaddPattern}
	 *           and {@link #timestampdiffPattern} consistent
	 *           with our choice.
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

	/**
	 * Whether a predicate like {@code a > 0} can appear in an expression
	 * context, for example, in a {@code select} list item.
	 */
	protected boolean supportsPredicateAsExpression() {
		// Most databases seem to allow that
		return true;
	}

	/**
	 * Obtain a {@link RowLockStrategy} for the given {@link LockMode}.
	 */
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

	/**
	 * The {@code generated as} clause, or similar, for generated column
	 * declarations in DDL statements.
	 *
	 * @param generatedAs a SQL expression used to generate the column value
	 * @return The {@code generated as} clause containing the given expression
	 */
	public String generatedAs(String generatedAs) {
		return " generated always as (" + generatedAs + ") stored";
	}

	/**
	 * Is an explicit column type required for {@code generated as} columns?
	 *
	 * @return {@code true} if an explicit type is required
	 */
	public boolean hasDataTypeBeforeGeneratedAs() {
		return true;
	}

	/**
	 * Create a {@link MutationOperation} for a updating an optional table
	 */
	public MutationOperation createOptionalTableUpdateOperation(
			EntityMutationTarget mutationTarget,
			OptionalTableUpdate optionalTableUpdate,
			SessionFactoryImplementor factory) {
		return new OptionalTableUpdateOperation( mutationTarget, optionalTableUpdate, factory );
	}

	/**
	 * Is there some way to disable foreign key constraint checking while
	 * truncating tables? (If there's no way to do it, and if we can't
	 * {@linkplain #canBatchTruncate() batch truncate}, we must drop and
	 * recreate the constraints instead.)
	 *
	 * @return {@code true} if there is some way to do it
	 *
	 * @see #getDisableConstraintsStatement()
	 * @see #getDisableConstraintStatement(String, String)
	 */
	public boolean canDisableConstraints() {
		return false;
	}

	/**
	 * A SQL statement that temporarily disables foreign key constraint
	 * checking for all tables.
	 */
	public String getDisableConstraintsStatement() {
		return null;
	}

	/**
	 * A SQL statement that re-enables foreign key constraint checking for
	 * all tables.
	 */
	public String getEnableConstraintsStatement() {
		return null;
	}

	/**
	 * A SQL statement that temporarily disables checking of the given
	 * foreign key constraint.
	 *
	 * @param tableName the name of the table
	 * @param name the name of the constraint
	 */
	public String getDisableConstraintStatement(String tableName, String name) {
		return null;
	}

	/**
	 * A SQL statement that re-enables checking of the given foreign key
	 * constraint.
	 *
	 * @param tableName the name of the table
	 * @param name the name of the constraint
	 */
	public String getEnableConstraintStatement(String tableName, String name) {
		return null;
	}

	/**
	 * Does the {@link #getTruncateTableStatement(String) truncate table}
	 * statement accept multiple tables?
	 *
	 * @return {@code true} if it does
	 */
	public boolean canBatchTruncate() {
		return false;
	}

	/**
	 * A SQL statement or statements that truncate the given tables.
	 *
	 * @param tableNames the names of the tables
	 */
	public String[] getTruncateTableStatements(String[] tableNames) {
		if ( canBatchTruncate() ) {
			StringBuilder builder = new StringBuilder();
			for ( String tableName : tableNames ) {
				if ( builder.length() > 0 ) {
					builder.append(", ");
				}
				builder.append( tableName );
			}
			return new String[] { getTruncateTableStatement( builder.toString() ) };
		}
		else {
			String[] statements = new String[tableNames.length];
			for ( int i = 0; i < tableNames.length; i++ ) {
				statements[i] = getTruncateTableStatement( tableNames[i] );
			}
			return statements;
		}
	}

	/**
	 * A SQL statement that truncates the given table.
	 *
	 * @param tableName the name of the table
	 */
	public String getTruncateTableStatement(String tableName) {
		return "truncate table " + tableName;
	}

	/**
	 * Support for native parameter markers.
	 * <p/>
	 * This is generally dependent on both the database and the driver.
	 *
	 * @return May return {@code null} to indicate that the JDBC
	 * {@linkplain ParameterMarkerStrategyStandard standard} strategy should be used
	 */
	public ParameterMarkerStrategy getNativeParameterMarkerStrategy() {
		return null;
	}

	/**
	 * Whether this Dialect supports {@linkplain PreparedStatement#addBatch() batch updates}.
	 *
	 * @return {@code true} indicates it does; {@code false} indicates it does not; {@code null} indicates
	 * it might and that database-metadata should be consulted.
	 *
	 * @see org.hibernate.engine.jdbc.env.spi.ExtractedDatabaseMetaData#supportsBatchUpdates
	 */
	public Boolean supportsBatchUpdates() {
		// are there any databases/drivers which don't?
		return true;
	}

	/**
	 * Whether this Dialect supports the JDBC {@link java.sql.Types#REF_CURSOR} type.
	 *
	 * @return {@code true} indicates it does; {@code false} indicates it does not; {@code null} indicates
	 * it might and that database-metadata should be consulted
	 *
	 * @see org.hibernate.engine.jdbc.env.spi.ExtractedDatabaseMetaData#supportsRefCursors
	 */
	public Boolean supportsRefCursors() {
		return null;
	}

	/**
	 * Pluggable strategy for determining the {@link Size} to use for
	 * columns of a given SQL type.
	 * <p>
	 * Allows dialects, integrators, and users a chance to apply column
	 * size defaults and limits in certain situations based on the mapped
	 * SQL and Java types. For example, when mapping a {@code UUID} to a
	 * {@code VARCHAR} column, we know the default {@code Size} should
	 * have {@link Size#getLength() Size.length == 36}.
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
			final int ddlTypeCode = jdbcType.getDdlTypeCode();
			// Set the explicit length to null if we encounter the JPA default 255
			if ( length != null && length == Size.DEFAULT_LENGTH ) {
				length = null;
			}

			switch ( ddlTypeCode ) {
				case SqlTypes.ARRAY:
					break;
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
						throw new IllegalArgumentException("scale has no meaning for SQL floating point types");
					}
					// but if the user explicitly specifies a precision, we need to convert it:
					if ( precision != null ) {
						// convert from base 10 (as specified in @Column) to base 2 (as specified by SQL)
						// using the magic of high school math: log_2(10^n) = n*log_2(10) = n*ln(10)/ln(2)
						precision = (int) ceil( precision * LOG_BASE2OF10 );
					}
					break;
				case SqlTypes.TIME:
				case SqlTypes.TIME_WITH_TIMEZONE:
				case SqlTypes.TIME_UTC:
				case SqlTypes.TIMESTAMP:
				case SqlTypes.TIMESTAMP_WITH_TIMEZONE:
				case SqlTypes.TIMESTAMP_UTC:
					length = null;
					size.setPrecision( javaType.getDefaultSqlPrecision( Dialect.this, jdbcType ) );
					if ( scale != null && scale != 0 ) {
						throw new IllegalArgumentException("scale has no meaning for SQL time or timestamp types");
					}
					break;
				case SqlTypes.NUMERIC:
				case SqlTypes.DECIMAL:
				case SqlTypes.INTERVAL_SECOND:
					size.setPrecision( javaType.getDefaultSqlPrecision( Dialect.this, jdbcType ) );
					size.setScale( javaType.getDefaultSqlScale( Dialect.this, jdbcType ) );
					break;
			}

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
	 * Since it's never possible to translate every
	 * pattern letter sequences understood by
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
	 * <p>
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

	/**
	 * Append a datetime literal representing the given {@link java.time}
	 * value to the given {@link SqlAppender}.
	 */
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
				appendAsTimestampWithNanos( appender, temporalAccessor, supportsTemporalLiteralOffset(), jdbcTimeZone );
				appender.appendSql( JDBC_ESCAPE_END );
				break;
			default:
				throw new IllegalArgumentException();
		}
	}

	/**
	 * Append a datetime literal representing the given {@link Date}
	 * value to the given {@link SqlAppender}.
	 */
	public void appendDateTimeLiteral(SqlAppender appender, Date date, TemporalType precision, TimeZone jdbcTimeZone) {
		switch ( precision ) {
			case DATE:
				appender.appendSql( JDBC_ESCAPE_START_DATE );
				appendAsDate( appender, date );
				appender.appendSql( JDBC_ESCAPE_END );
				break;
			case TIME:
				appender.appendSql( JDBC_ESCAPE_START_TIME );
				appendAsLocalTime( appender, date );
				appender.appendSql( JDBC_ESCAPE_END );
				break;
			case TIMESTAMP:
				appender.appendSql( JDBC_ESCAPE_START_TIMESTAMP );
				appendAsTimestampWithNanos( appender, date, jdbcTimeZone );
				appender.appendSql( JDBC_ESCAPE_END );
				break;
			default:
				throw new IllegalArgumentException();
		}
	}

	/**
	 * Append a datetime literal representing the given {@link Calendar}
	 * value to the given {@link SqlAppender}.
	 */
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
				appendAsLocalTime( appender, calendar );
				appender.appendSql( JDBC_ESCAPE_END );
				break;
			case TIMESTAMP:
				appender.appendSql( JDBC_ESCAPE_START_TIMESTAMP );
				appendAsTimestampWithMillis( appender, calendar, jdbcTimeZone );
				appender.appendSql( JDBC_ESCAPE_END );
				break;
			default:
				throw new IllegalArgumentException();
		}
	}

	/**
	 * Append a literal SQL {@code interval} representing the given Java
	 * {@link Duration}.
	 */
	public void appendIntervalLiteral(SqlAppender appender, Duration literal) {
		appender.appendSql( "interval '" );
		appender.appendSql( literal.getSeconds() );
		appender.appendSql( '.' );
		appender.appendSql( literal.getNano() );
		appender.appendSql( "' second" );
	}

	/**
	 * Append a literal SQL {@code uuid} representing the given Java
	 * {@link UUID}.
	 * <p>
	 * This is usually a {@code cast()} expression, but it might be
	 * a function call.
	 */
	public void appendUUIDLiteral(SqlAppender appender, UUID literal) {
		appender.appendSql( "cast('" );
		appender.appendSql( literal.toString() );
		appender.appendSql( "' as uuid)" );
	}

	/**
	 * Does this dialect supports timezone offsets in temporal literals.
	 */
	public boolean supportsTemporalLiteralOffset() {
		return false;
	}

	/**
	 * How the dialect supports time zone types like {@link Types#TIMESTAMP_WITH_TIMEZONE}.
	 */
	public TimeZoneSupport getTimeZoneSupport() {
		return TimeZoneSupport.NONE;
	}

	/**
	 * The name of a {@code rowid}-like pseudo-column which
	 * acts as a high-performance row locator, or null if
	 * this dialect has no such pseudo-column.
	 * <p>
	 * If the {@code rowid}-like value is an explicitly-declared
	 * named column instead of an implicit pseudo-column, and if
	 * the given name is nonempty, return the given name.
	 *
	 * @param rowId the name specified by
	 *        {@link org.hibernate.annotations.RowId#value()},
	 *        which is ignored if {@link #getRowIdColumnString}
	 *        is not overridden
	 */
	public String rowId(String rowId) {
		return null;
	}

	/**
	 * The JDBC type code of the {@code rowid}-like pseudo-column
	 * which acts as a high-performance row locator.
	 *
	 * @return {@link Types#ROWID} by default
	 */
	public int rowIdSqlType() {
		return ROWID;
	}

	/**
	 * If this dialect requires that the {@code rowid} column be
	 * declared explicitly, return the DDL column definition.
	 *
	 * @return the DDL column definition, or {@code null} if
	 *         the {@code rowid} is an implicit pseudo-column
	 */
	public String getRowIdColumnString(String rowId) {
		return null;
	}

	/**
	 * Get the minimum {@link DmlTargetColumnQualifierSupport} required by this dialect.
	 *
	 * @return the column qualifier support required by this dialect
	 */
	public DmlTargetColumnQualifierSupport getDmlTargetColumnQualifierSupport() {
		return DmlTargetColumnQualifierSupport.NONE;
	}

	/**
	 * Get this dialect's level of support for primary key functional dependency analysis
	 * within {@code GROUP BY} and {@code ORDER BY} clauses.
	 */
	public FunctionalDependencyAnalysisSupport getFunctionalDependencyAnalysisSupport() {
		return FunctionalDependencyAnalysisSupportImpl.NONE;
	}

}
