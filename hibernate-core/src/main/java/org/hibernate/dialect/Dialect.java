/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect;


import org.hibernate.dialect.type.spi.DdlTypeBuilder;
import org.hibernate.dialect.type.spi.EnumSupport;
import org.hibernate.dialect.type.spi.EnumSupports;
import org.hibernate.dialect.type.spi.ObjectNullBindingStrategy;

import org.hibernate.dialect.type.spi.StandardDdlTypes;

import org.hibernate.dialect.array.spi.ArraySupport;
import org.hibernate.dialect.jdbc.spi.JdbcMetadataOverrides;
import org.hibernate.dialect.jdbc.spi.LobMergeStrategy;
import org.hibernate.dialect.jdbc.spi.ParameterLimits;
import org.hibernate.engine.jdbc.cursor.spi.RefCursorSupportFactory;
import org.hibernate.engine.jdbc.cursor.spi.RefCursorSupports;

import org.hibernate.dialect.jdbc.spi.ColumnAliasExtractor;

import org.hibernate.dialect.type.spi.TimeZoneSupport;

import org.hibernate.dialect.type.spi.NationalizationSupport;
import org.hibernate.dialect.type.spi.SizeStrategy;
import org.hibernate.dialect.type.spi.StandardSizeStrategy;
import org.hibernate.dialect.type.spi.TypeSizingProfile;
import org.hibernate.dialect.type.spi.StringValueSemantics;
import org.hibernate.dialect.lob.spi.LobSupport;
import org.hibernate.dialect.lob.spi.LobSupports;
import org.hibernate.dialect.rowid.spi.RowIdSupport;
import org.hibernate.dialect.rowid.spi.RowIdSupports;


import org.hibernate.dialect.lock.spi.RowLockStrategy;

import org.hibernate.dialect.aggregate.spi.FunctionalDependencyAnalysisSupport;


import org.hibernate.dialect.sql.ast.spi.FetchClauseSupport;
import org.hibernate.dialect.sql.ast.spi.SyntheticTableGroupSupport;

import org.hibernate.dialect.sql.ast.spi.DmlTargetColumnQualifierSupport;

import org.hibernate.dialect.sql.ast.spi.CteSupport;
import org.hibernate.dialect.sql.ast.spi.MutationSyntaxSupport;
import org.hibernate.dialect.sql.ast.spi.PredicateSupport;
import org.hibernate.dialect.sql.ast.spi.RowValueSupport;
import org.hibernate.dialect.sql.ast.spi.SetOperationSupport;
import org.hibernate.dialect.sql.ast.spi.SingleRowTableSupport;
import org.hibernate.dialect.sql.ast.spi.SubquerySupport;
import org.hibernate.dialect.sql.ast.spi.NullOrderingSupport;
import org.hibernate.dialect.sql.ast.spi.ValuesListSupport;

import jakarta.persistence.GenerationType;
import jakarta.persistence.TemporalType;
import jakarta.annotation.Nullable;
import org.hibernate.HibernateException;
import org.hibernate.Incubating;
import org.hibernate.LockOptions;
import org.hibernate.ScrollMode;
import org.hibernate.SPI;
import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.boot.model.relational.Sequence;
import org.hibernate.audit.internal.AuditColumnFunction;
import org.hibernate.dialect.aggregate.spi.AggregateSupport;
import org.hibernate.dialect.constraint.spi.CheckConstraintSupport;
import org.hibernate.dialect.constraint.spi.ForeignKeySupport;
import org.hibernate.dialect.aggregate.spi.AggregateSupports;
import org.hibernate.dialect.function.CastFunction;
import org.hibernate.dialect.function.CastStrEmulation;
import org.hibernate.dialect.function.CoalesceIfnullEmulation;
import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.dialect.function.CurrentFunction;
import org.hibernate.dialect.function.ExtractFunction;
import org.hibernate.dialect.function.InsertSubstringOverlayEmulation;
import org.hibernate.dialect.function.LocatePositionEmulation;
import org.hibernate.dialect.function.LpadRpadPadEmulation;
import org.hibernate.dialect.function.OrdinalFunction;
import org.hibernate.dialect.function.SqlFunction;
import org.hibernate.dialect.function.StringFunction;
import org.hibernate.dialect.function.TrimFunction;
import org.hibernate.dialect.function.spi.ExpressionCoercionSupport;
import org.hibernate.dialect.function.spi.TupleCountSupport;
import org.hibernate.dialect.function.spi.WindowFunctionSupport;
import org.hibernate.dialect.generated.spi.GeneratedValuesSupport;
import org.hibernate.dialect.identity.spi.IdentityColumnSupport;
import org.hibernate.dialect.identity.spi.IdentityColumnSupportBase;
import org.hibernate.dialect.identifier.spi.IdentifierSupport;
import org.hibernate.dialect.identifier.spi.KeywordRegistration;
import org.hibernate.dialect.identifier.spi.KeywordSupport;
import org.hibernate.dialect.literal.spi.LiteralSupport;
import org.hibernate.dialect.lock.PessimisticLockStyle;
import org.hibernate.dialect.lock.internal.LockingSupportSimple;
import org.hibernate.dialect.lock.spi.EntityLockingStrategies;
import org.hibernate.dialect.lock.spi.EntityLockingStrategyFactory;
import org.hibernate.dialect.lock.spi.LockingSupport;
import org.hibernate.dialect.namespace.spi.NamespaceSupport;
import org.hibernate.dialect.namespace.spi.NamespaceSupports;
import org.hibernate.dialect.pagination.spi.LimitHandler;
import org.hibernate.dialect.queryhint.spi.QueryHintPlacement;
import org.hibernate.dialect.rowsecurity.spi.RowLevelSecurity;
import org.hibernate.dialect.rowsecurity.spi.RowLevelSecurityStrategies;
import org.hibernate.dialect.schema.internal.StandardTableCleaner;
import org.hibernate.dialect.schema.internal.StandardTableMigrator;
import org.hibernate.dialect.schema.spi.AlterTableSupport;
import org.hibernate.dialect.schema.spi.ColumnDefinitionSupport;
import org.hibernate.dialect.schema.spi.ConstraintControlMode;
import org.hibernate.dialect.schema.spi.ConstraintControlSupport;
import org.hibernate.dialect.schema.spi.IfExistsSupport;
import org.hibernate.dialect.schema.spi.IndexDdlSupport;
import org.hibernate.dialect.schema.spi.SchemaDropSupport;
import org.hibernate.dialect.schema.spi.SchemaCommentSupport;
import org.hibernate.dialect.schema.spi.SchemaCommentSupports;
import org.hibernate.dialect.schema.spi.TableCleaner;
import org.hibernate.dialect.schema.spi.TableCreationSupport;
import org.hibernate.dialect.schema.spi.TableMigrator;
import org.hibernate.dialect.schema.spi.TruncateSupport;
import org.hibernate.dialect.schema.spi.TruncateMode;
import org.hibernate.dialect.sequence.spi.SequenceSupport;
import org.hibernate.dialect.sequence.spi.SequenceSupports;
import org.hibernate.dialect.temporal.spi.TemporalTableSupport;
import org.hibernate.dialect.temporal.spi.TemporalTableSupports;
import org.hibernate.dialect.temporaltype.spi.CurrentTemporalSupport;
import org.hibernate.dialect.temporaltype.spi.CurrentTemporalSupports;
import org.hibernate.dialect.temporaltype.spi.TemporalFormatSupport;
import org.hibernate.dialect.temporaltype.spi.TemporalFormatSupports;
import org.hibernate.dialect.temporaltype.spi.TemporalOperationSupport;
import org.hibernate.dialect.temporaltype.spi.TemporalValueSemantics;
import org.hibernate.dialect.temptable.spi.PersistentTemporaryTableStrategy;
import org.hibernate.dialect.temptable.spi.StandardTemporaryTableExporter;
import org.hibernate.dialect.temptable.spi.TemporaryTableExporter;
import org.hibernate.dialect.temptable.spi.TemporaryTableStrategy;
import org.hibernate.dialect.unique.spi.UniqueDelegate;
import org.hibernate.dialect.unique.spi.UniqueDelegates;
import org.hibernate.boot.registry.selector.spi.DialectSelector;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolver;
import org.hibernate.engine.jdbc.env.internal.DefaultSchemaNameResolver;
import org.hibernate.engine.jdbc.env.spi.AnsiSqlKeywords;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.env.spi.NameQualifierSupport;
import org.hibernate.engine.jdbc.env.spi.SchemaNameResolver;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.exception.spi.ConversionContext;
import org.hibernate.exception.spi.SQLExceptionConversionDelegate;
import org.hibernate.exception.spi.ViolatedConstraintNameExtractor;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.loader.ast.spi.MultiKeyLoadSizingStrategy;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.Index;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.UserDefinedType;
import org.hibernate.metamodel.mapping.SqlTypedMapping;
import org.hibernate.procedure.spi.CallableStatementSupport;
import org.hibernate.procedure.spi.CallableStatementSupports;
import org.hibernate.query.Query;
import org.hibernate.query.sqm.CastType;
import org.hibernate.query.sqm.SetOperator;
import org.hibernate.query.sqm.TrimSpec;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.translation.SqlAstNodeRenderingMode;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslatorFactory;
import org.hibernate.dialect.mutation.spi.MultiTableMutationSupport;
import org.hibernate.dialect.lock.internal.NonLockingClauseStrategy;
import org.hibernate.dialect.lock.spi.PessimisticLockKind;
import org.hibernate.dialect.lock.internal.StandardLockingClauseStrategy;
import org.hibernate.dialect.lock.spi.LockingClauseStrategy;
import org.hibernate.sql.spi.ParameterMarkerStrategy;
import org.hibernate.sql.spi.SqlAppender;
import org.hibernate.sql.ast.spi.query.select.QuerySpec;
import org.hibernate.sql.spi.mutation.MutationOperation;
import org.hibernate.dialect.sql.ast.spi.OptionalTableUpdateOperationRequest;
import org.hibernate.sql.spi.mutation.jdbc.OptionalTableUpdateOperation;
import org.hibernate.tool.schema.extract.spi.ColumnTypeInformation;
import org.hibernate.tool.schema.extract.spi.ExtractionContext;
import org.hibernate.tool.schema.extract.spi.InformationExtractor;
import org.hibernate.tool.schema.extract.spi.InformationExtractors;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractor;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractors;
import org.hibernate.tool.schema.spi.Exporter;
import org.hibernate.tool.schema.spi.StandardForeignKeyExporter;
import org.hibernate.tool.schema.spi.StandardIndexExporter;
import org.hibernate.tool.schema.spi.StandardSequenceExporter;
import org.hibernate.tool.schema.spi.StandardTableExporter;
import org.hibernate.tool.schema.spi.StandardUserDefinedTypeExporter;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.jdbc.ArrayJdbcTypeConstructor;
import org.hibernate.type.descriptor.jdbc.BlobJdbcType;
import org.hibernate.type.descriptor.jdbc.ClobJdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcLiteralFormatter;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.LongNVarcharJdbcType;
import org.hibernate.type.descriptor.jdbc.LongVarbinaryJdbcType;
import org.hibernate.type.descriptor.jdbc.LongVarcharJdbcType;
import org.hibernate.type.descriptor.jdbc.NCharJdbcType;
import org.hibernate.type.descriptor.jdbc.NClobJdbcType;
import org.hibernate.type.descriptor.jdbc.NVarcharJdbcType;
import org.hibernate.type.descriptor.jdbc.TimeUtcAsJdbcTimeJdbcType;
import org.hibernate.type.descriptor.jdbc.TimeUtcAsOffsetTimeJdbcType;
import org.hibernate.type.descriptor.jdbc.TimestampUtcAsJdbcTimestampJdbcType;
import org.hibernate.type.descriptor.jdbc.TimestampUtcAsOffsetDateTimeJdbcType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;
import org.hibernate.type.descriptor.sql.DdlType;
import org.hibernate.type.descriptor.sql.spi.DdlTypeRegistry;
import org.hibernate.type.spi.TypeConfiguration;

import java.io.IOException;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.NClob;
import java.sql.SQLException;
import java.sql.Types;
import java.time.temporal.TemporalAccessor;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.TimeZone;

import static java.lang.String.join;
import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.SUPPLY;
import static org.hibernate.SPI.Role.USE;
import static org.hibernate.dialect.array.spi.ArraySupport.Capability.STANDARD_ARRAY;
import static org.hibernate.cfg.AvailableSettings.NON_CONTEXTUAL_LOB_CREATION;
import static org.hibernate.cfg.AvailableSettings.STATEMENT_BATCH_SIZE;
import static org.hibernate.cfg.AvailableSettings.USE_GET_GENERATED_KEYS;
import static org.hibernate.internal.CoreMessageLogger.CORE_LOGGER;
import static org.hibernate.internal.util.MathHelper.ceilingPowerOfTwo;
import static org.hibernate.internal.util.StringHelper.isEmpty;
import static org.hibernate.dialect.sql.ast.spi.PredicateSupport.Capability.EXPRESSION_PLACEMENT;
import static org.hibernate.dialect.lock.internal.NonLockingClauseStrategy.NON_CLAUSE_STRATEGY;
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
import static org.hibernate.dialect.literal.spi.StandardDateTimeLiteralRendering.appendAsDate;
import static org.hibernate.dialect.literal.spi.StandardDateTimeLiteralRendering.appendAsLocalTime;
import static org.hibernate.dialect.literal.spi.StandardDateTimeLiteralRendering.appendAsTime;
import static org.hibernate.dialect.literal.spi.StandardDateTimeLiteralRendering.appendAsTimestampWithMillis;
import static org.hibernate.dialect.literal.spi.StandardDateTimeLiteralRendering.appendAsTimestampWithNanos;

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
 * @see DialectSelector#resolve(String)
 * @see DialectResolver#resolveDialect(DialectResolutionInfo)
 */
@SPI({ USE, IMPLEMENT, SUPPLY })
public abstract class Dialect implements ConversionContext, AlterTableSupport,
		TableCreationSupport, ColumnDefinitionSupport, IndexDdlSupport,
		ConstraintControlSupport, TruncateSupport, ForeignKeySupport,
		CheckConstraintSupport, IdentifierSupport, KeywordSupport, LiteralSupport,
		TemporalOperationSupport {

	private volatile Properties defaultProperties;
	private volatile Set<String> keywords;

	private final SizeStrategy sizeStrategy = new StandardSizeStrategy( this );
	private final PersistentTemporaryTableStrategy persistentTemporaryTableStrategy = new PersistentTemporaryTableStrategy( this );
	private final UniqueDelegate uniqueDelegate = UniqueDelegates.alterTable( this );

	private final DatabaseVersion version;

	// constructors and factory methods ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@SPI( IMPLEMENT )
	protected Dialect(DatabaseVersion version) {
		this.version = version;
		checkVersion();
	}

	@SPI( IMPLEMENT )
	protected Dialect(DialectResolutionInfo info) {
		this.version = determineDatabaseVersion( info );
		checkVersion();
	}

	private void checkVersion() {
		if ( version != null ) {
			final var minimumVersion = getMinimumSupportedVersion();
			if ( version.isBefore( minimumVersion.getMajor(), minimumVersion.getMinor(), minimumVersion.getMicro() ) ) {
				CORE_LOGGER.unsupportedDatabaseVersion(
						getClass().getName(),
						version.getMajor() + "." + version.getMinor() + "." + version.getMicro(),
						minimumVersion.getMajor() + "." + minimumVersion.getMinor() + "." + minimumVersion.getMicro()
				);
			}
		}
	}

	/**
	 * Determine the database version, as precise as possible and using Dialect-specific techniques,
	 * from a {@link DialectResolutionInfo} object.
	 * <p>
	 * This method is called during superclass construction by
	 * {@link #Dialect(DialectResolutionInfo)}. Implementations must not depend on
	 * subclass initialization.
	 * Integration providers may also invoke this method on an existing Dialect
	 * when database metadata becomes available after the Dialect was constructed.
	 *
	 * @param info The dialect resolution info that would be passed by Hibernate ORM
	 * to the constructor of a Dialect of the same type.
	 * @return The corresponding database version.
	 */
	@SPI({ USE, IMPLEMENT })
	public DatabaseVersion determineDatabaseVersion(DialectResolutionInfo info) {
		return info.makeCopyOrDefault( getMinimumSupportedVersion() );
	}

	/// Contribute provider configuration defaults after Dialect construction.
	///
	/// Invoke `super` first to extend the inherited defaults. Omitting `super`
	/// deliberately replaces them. Mutate the supplied isolated property bag
	/// only during this callback; do not retain it or call
	/// [#getDefaultProperties()] from a Dialect constructor.
	///
	/// @param properties the isolated contribution target
	/// @since 8.0
	@SPI({ USE, IMPLEMENT, SUPPLY })
	protected void contributeDefaultProperties(Properties properties) {
		properties.setProperty( STATEMENT_BATCH_SIZE, "1" );
		properties.setProperty( NON_CONTEXTUAL_LOB_CREATION, "false" );
		properties.setProperty( USE_GET_GENERATED_KEYS, "true" );
	}

	/// Register the inherited ANSI-oriented DDL type descriptors during Dialect
	/// type contribution.
	///
	/// Override this focused sub-hook to extend or replace column type
	/// registrations. Invoke `super` first to retain the inherited descriptor
	/// set; omitting it deliberately replaces that set. Mutate the supplied
	/// boot-scoped registry only during this callback, and do not retain the
	/// contribution context or registry afterward. Implement an independent
	/// [org.hibernate.boot.model.TypeContributor] instead when the contribution
	/// is not owned by a Dialect.
	///
	/// @param typeContributions the Hibernate-supplied contribution target
	/// @param serviceRegistry the Hibernate-supplied bootstrap services
	/// @see #contributeTypes(TypeContributions, ServiceRegistry)
	/// @see #getTypeSizingProfile()
	/// @since 8.0
	@SPI({ USE, IMPLEMENT, SUPPLY })
	protected void registerColumnTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		final var ddlTypeRegistry = typeContributions.getTypeConfiguration().getDdlTypeRegistry();

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
		registerCapacityDependentType(
				ddlTypeRegistry,
				VARCHAR,
				LONG32VARCHAR,
				getTypeSizingProfile().maxVarcharLength()
		);
		ddlTypeRegistry.addDescriptor( simpleSqlType( CLOB ) );

		ddlTypeRegistry.addDescriptor( simpleSqlType( NCHAR ) );
		registerCapacityDependentType(
				ddlTypeRegistry,
				NVARCHAR,
				LONG32NVARCHAR,
				getTypeSizingProfile().maxNVarcharLength()
		);
		ddlTypeRegistry.addDescriptor( simpleSqlType( NCLOB ) );

		ddlTypeRegistry.addDescriptor( simpleSqlType( BINARY ) );
		registerCapacityDependentType(
				ddlTypeRegistry,
				VARBINARY,
				LONG32VARBINARY,
				getTypeSizingProfile().maxVarbinaryLength()
		);
		ddlTypeRegistry.addDescriptor( simpleSqlType( BLOB ) );

		// by default use the LOB mappings for the "long" types
		ddlTypeRegistry.addDescriptor( simpleSqlType( LONG32VARCHAR ) );
		ddlTypeRegistry.addDescriptor( simpleSqlType( LONG32NVARCHAR ) );
		ddlTypeRegistry.addDescriptor( simpleSqlType( LONG32VARBINARY ) );

		if ( getArraySupport().supports( STANDARD_ARRAY ) ) {
			ddlTypeRegistry.addDescriptor( StandardDdlTypes.standardArray( this, false ) );
		}
		if ( getRowIdSupport().isSupported() ) {
			ddlTypeRegistry.addDescriptor( simpleSqlType( ROWID ) );
		}
	}

	private DdlType simpleSqlType(int sqlTypeCode) {
		return StandardDdlTypes.builder( sqlTypeCode, columnType( sqlTypeCode ), this )
				.lobKind( getLobSupport().isLobType( sqlTypeCode ) ? DdlTypeBuilder.LobKind.ALL : DdlTypeBuilder.LobKind.NONE )
				.castTypeName( castType( sqlTypeCode ) )
				.narrowCastTypeName( narrowCastType( sqlTypeCode ) )
				.build();
	}

	private void registerCapacityDependentType(
			DdlTypeRegistry ddlTypeRegistry,
			int sqlTypeCode,
			int biggestSqlTypeCode,
			long capacity) {
		final DdlTypeBuilder builder = sqlTypeBuilder( sqlTypeCode, biggestSqlTypeCode, sqlTypeCode );
		if ( capacity == TypeSizingProfile.UNSUPPORTED ) {
			builder.lobKind( getLobSupport().isLobType( biggestSqlTypeCode )
					? DdlTypeBuilder.LobKind.ALL
					: DdlTypeBuilder.LobKind.NONE );
		}
		else {
			builder.withTypeCapacity( capacity, columnType( sqlTypeCode ) );
		}
		ddlTypeRegistry.addDescriptor( builder.build() );
	}

	/**
	 * Obtain a builder object for a family of capacity-dependent SQL types.
	 *
	 * @param sqlTypeCode the JDBC type code abstracting over the capacity-limited types
	 * @param biggestSqlTypeCode the real JDBC type code of the largest type
	 * @param castTypeCode the real JDBC type code to use to look at the type to use in typecasts
	 * @return the builder object
	 */
	private DdlTypeBuilder sqlTypeBuilder(int sqlTypeCode, int biggestSqlTypeCode, int castTypeCode) {
		return StandardDdlTypes.builder( sqlTypeCode, columnType( biggestSqlTypeCode ), this )
				.lobKind( getLobSupport().isLobType( sqlTypeCode )
						? DdlTypeBuilder.LobKind.ALL
						: getLobSupport().isLobType( biggestSqlTypeCode )
								? DdlTypeBuilder.LobKind.BIGGEST
								: DdlTypeBuilder.LobKind.NONE )
				.castTypeName( castType( castTypeCode ) );
	}

	/// Return the complete database column declaration pattern for a JDBC
	/// [Types] or Hibernate [SqlTypes] code used during type registration.
	///
	/// Use only `$l`, `$p`, and `$s` for length, precision, and scale. Define
	/// distinct mappings for the `LONG32` codes; the JDBC `LONG` synonyms are
	/// normalized before this hook. Delegate unknown inherited codes to the
	/// superclass so its standard exception remains authoritative.
	@SPI({ USE, IMPLEMENT })
	protected String columnType(int sqlTypeCode) {
		return switch (sqlTypeCode) {
			case ROWID -> "rowid";

			case BOOLEAN -> "boolean";

			case TINYINT -> "tinyint";
			case SMALLINT -> "smallint";
			case INTEGER -> "integer";
			case BIGINT -> "bigint";

			case FLOAT ->
				// this is the floating point type we prefer!
					"float($p)";
			case REAL ->
				// this type has very unclear semantics in ANSI SQL,
				// so we avoid it and prefer float with an explicit
				// precision
					"real";
			case DOUBLE ->
				// this is just a more verbose way to write float(19)
					"double precision";

			// these are pretty much synonyms, but are considered
			// separate types by the ANSI spec, and in some dialects
			case NUMERIC -> "numeric($p,$s)";
			case DECIMAL -> "decimal($p,$s)";

			case DATE -> "date";
			case TIME -> "time($p)";
			case TIME_WITH_TIMEZONE ->
				// type included here for completeness but note that
				// very few databases support it, and the general
				// advice is to caution against its use (for reasons,
				// check the comments in the Postgres documentation).
					"time($p) with time zone";
			case TIMESTAMP -> "timestamp($p)";
			case TIMESTAMP_WITH_TIMEZONE -> "timestamp($p) with time zone";
			case TIME_UTC ->
					getTimeZoneSupport() == TimeZoneSupport.NATIVE
							? columnType( TIME_WITH_TIMEZONE )
							: columnType( TIME );
			case TIMESTAMP_UTC ->
					getTimeZoneSupport() == TimeZoneSupport.NATIVE
							? columnType( TIMESTAMP_WITH_TIMEZONE )
							: columnType( TIMESTAMP );

			case CHAR -> "char($l)";
			case VARCHAR -> "varchar($l)";
			case CLOB -> "clob";

			case NCHAR -> "nchar($l)";
			case NVARCHAR -> "nvarchar($l)";
			case NCLOB -> "nclob";

			case BINARY -> "binary($l)";
			case VARBINARY -> "varbinary($l)";
			case BLOB -> "blob";

			// by default use the LOB mappings for the "long" types
			case LONG32VARCHAR -> columnType( CLOB );
			case LONG32NVARCHAR -> columnType( NCLOB );
			case LONG32VARBINARY -> columnType( BLOB );

			default -> throw new IllegalArgumentException( "unknown type: " + sqlTypeCode );
		};
	}

	/// Return the complete cast-target type pattern for `sqlTypeCode`.
	///
	/// Override this hook when cast syntax differs from column declaration
	/// syntax; otherwise delegate to [#columnType].
	@SPI({ USE, IMPLEMENT })
	protected String castType(int sqlTypeCode) {
		return columnType( sqlTypeCode );
	}

	/// Return the complete type pattern for a cast or set-returning-function
	/// declaration position which rejects locator LOB names.
	///
	/// The standard implementation maps locator and `LONG32` LOB codes to the
	/// corresponding varying type and delegates every other code to
	/// [#columnType].
	///
	/// @since 7.4
	@Incubating
	@SPI({ USE, IMPLEMENT })
	protected String narrowCastType(int sqlTypeCode) {
		return columnType(
				switch ( sqlTypeCode ) {
					case CLOB, LONG32VARCHAR -> VARCHAR;
					case NCLOB, LONG32NVARCHAR -> NVARCHAR;
					case BLOB, LONG32VARBINARY -> VARBINARY;
					default -> sqlTypeCode;
				}
		);
	}

	/// Contribute ANSI and database-specific SQL keywords.
	///
	/// Invoke `super` first to retain the SQL:2003 words. Omitting `super`
	/// deliberately replaces the inherited keyword profile. Register words only
	/// during this callback and do not retain the registration target.
	///
	/// @param registration the scoped keyword target
	/// @since 8.0
	@SPI({ USE, IMPLEMENT, SUPPLY })
	protected void contributeKeywords(KeywordRegistration registration) {
		registration.registerKeywords( new AnsiSqlKeywords().sql2003() );
		getPredicateSupport().getCaseInsensitiveLikeOperator().ifPresent( registration::registerKeyword );
	}

	/**
	 * Get the version of the SQL dialect that is the target of this instance.
	 */
	@SPI( USE )
	public final DatabaseVersion getVersion() {
		return version;
	}

	/**
	 * Get the version of the SQL dialect that is the minimum supported by this implementation.
	 * <p>
	 * This method is called during superclass construction. Implementations must
	 * return construction-safe immutable data and must not depend on subclass
	 * initialization.
	 */
	@SPI({ IMPLEMENT, SUPPLY })
	protected DatabaseVersion getMinimumSupportedVersion() {
		return SimpleDatabaseVersion.ZERO_VERSION;
	}

	/// Resolve a database-reported type name before base-name extraction.
	///
	/// `columnTypeName` is complete and may include parameters or an array
	/// suffix. Return null when it cannot be resolved.
	@SPI({ USE, IMPLEMENT })
	protected @Nullable Integer resolveSqlTypeCode(String columnTypeName, TypeConfiguration typeConfiguration) {
		final int parenthesisIndex = columnTypeName.lastIndexOf( '(' );
		final String baseTypeName =
				parenthesisIndex == -1
						? columnTypeName
						: columnTypeName.substring( 0, parenthesisIndex ).trim();
		return resolveSqlTypeCode( columnTypeName, baseTypeName, typeConfiguration );
	}

	/// Resolve a database-reported type using both its complete and normalized
	/// base names. Return null when neither identifies a registered type.
	@SPI({ USE, IMPLEMENT })
	protected @Nullable Integer resolveSqlTypeCode(
			String typeName,
			String baseTypeName,
			TypeConfiguration typeConfiguration) {
		return typeConfiguration.getDdlTypeRegistry().getSqlTypeCode( baseTypeName );
	}

	/// Resolve the non-null JDBC descriptor for result-set metadata.
	///
	/// Preserve vendor replacement and array-component resolution, and delegate
	/// unmatched codes to `jdbcTypeRegistry`.
	@SPI({ USE, IMPLEMENT })
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

	/// Normalize JDBC result-set metadata to Hibernate's resolved column length.
	///
	/// JDBC `precision` may represent character length. Providers should retain
	/// their vendor formulas using all supplied metadata values.
	@SPI({ USE, IMPLEMENT })
	public int resolveSqlTypeLength(
			String columnTypeName,
			int jdbcTypeCode,
			int precision,
			int scale,
			int displaySize) {
		return precision;
	}

	/// Supply the database's finite-domain declaration, lifecycle, and check
	/// strategy. Return one stable, thread-safe strategy for this Dialect.
	///
	/// @return the non-null enum support strategy
	/// @since 8.0
	/// @see EnumSupport
	@SPI({ IMPLEMENT, SUPPLY })
	public EnumSupport getEnumSupport() {
		return EnumSupports.standard();
	}

	/*
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
	 * <ul>
	 * <li> <code>ordinal(arg)</code>
	 * <li> <code>string(arg)</code>
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
	/// Contribute the database-specific function descriptors after independent
	/// function contributors have run.
	///
	/// Override this callback to extend or replace the inherited function set.
	/// Invoke `super` first to retain Hibernate's standard and emulated
	/// registrations; omitting it deliberately replaces that set. Mutate the
	/// supplied boot-scoped function registry only during this callback, and do
	/// not retain the contribution context or registry afterward. Implement an
	/// independent [org.hibernate.boot.model.FunctionContributor] instead when
	/// the contribution is not owned by a Dialect.
	///
	/// @param functionContributions the Hibernate-supplied function contribution
	/// target
	/// @see FunctionContributions
	/// @see org.hibernate.boot.model.FunctionContributor
	/// @see org.hibernate.query.sqm.function.SqmFunctionRegistry
	/// @see org.hibernate.query.sqm.function.SqmFunctionDescriptor
	/// @see CommonFunctionFactory
	/// @since 8.0
	@SPI({ USE, IMPLEMENT, SUPPLY })
	public void initializeFunctionRegistry(FunctionContributions functionContributions) {
		final var typeConfiguration = functionContributions.getTypeConfiguration();
		final var currentTemporalSupport = getCurrentTemporalSupport();
		final var basicTypeRegistry = typeConfiguration.getBasicTypeRegistry();
		final var timestampType = basicTypeRegistry.resolve( StandardBasicTypes.TIMESTAMP );
		final var dateType = basicTypeRegistry.resolve( StandardBasicTypes.DATE );
		final var timeType = basicTypeRegistry.resolve( StandardBasicTypes.TIME );
		final var instantType = basicTypeRegistry.resolve( StandardBasicTypes.INSTANT );
		final var offsetDateTimeType = basicTypeRegistry.resolve( StandardBasicTypes.OFFSET_DATE_TIME );
		final var localDateTimeType = basicTypeRegistry.resolve( StandardBasicTypes.LOCAL_DATE_TIME );
		final var localTimeType = basicTypeRegistry.resolve( StandardBasicTypes.LOCAL_TIME );
		final var localDateType = basicTypeRegistry.resolve( StandardBasicTypes.LOCAL_DATE );

		final var functionRegistry = functionContributions.getFunctionRegistry();
		final var functionFactory = new CommonFunctionFactory( functionContributions );

		//standard aggregate functions count(), sum(), max(), min(), avg(),
		//supported on every database

		//Note that we don't include median() in this list, since it's difficult
		//to implement on MySQL and Sybase ASE

		functionFactory.aggregates( this, SqlAstNodeRenderingMode.DEFAULT );

		//the ANSI SQL-defined aggregate functions any() and every() are only
		//supported on one database but can be emulated using sum() and case,
		//though there is a more natural mapping on some databases

		functionFactory.everyAny_sumCase( getPredicateSupport().supports( EXPRESSION_PLACEMENT ) );

		//math functions supported on almost every database

		//Note that while certain mathematical functions return the same type
		//as their arguments, this is not the case in general - any function
		//involving exponentiation by a noninteger power, logarithms,
		//trigonometric functions, and so on should be treated as returning
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

		//only some databases support the ANSI SQL-style position() function,
		//so define it here as an alias for locate()

		functionRegistry.register( "position",
				new LocatePositionEmulation( typeConfiguration ) );

		//very few databases support ANSI-style overlay() function, so emulate
		//it here in terms of either insert() or concat()/substring()

		functionRegistry.register( "overlay",
				new InsertSubstringOverlayEmulation( typeConfiguration, false ) );

		//ANSI SQL trim() function is supported on almost all databases we
		//care about, but on some it must be emulated using ltrim(), rtrim(),
		//and replace()

		functionRegistry.register( "trim",
				new TrimFunction( this, typeConfiguration ) );

		//ANSI SQL cast() function is supported on the databases we care most
		//about, but in certain cases it doesn't allow some useful typecasts,
		//which must be emulated in a dialect-specific way

		//Note that two case are especially tricky to make portable:
		// - casts to and from Boolean, and
		// - casting Double or Float to String.

		functionRegistry.register(
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

		functionRegistry.register( "extract",
				new ExtractFunction( this, typeConfiguration ) );

		//comparison functions supported on most databases, emulated on others
		//using a case expression

		functionFactory.leastGreatest();

		//two-argument synonym for coalesce() supported on most but not every
		//database, so define it here as an alias for coalesce(arg1,arg2)

		functionRegistry.register( "ifnull",
				new CoalesceIfnullEmulation() );

		//rpad() and pad() are supported on almost every database, and emulated
		//where not supported, but they're not considered "standard" ... instead
		//they're used to implement pad()

		functionFactory.pad();

		//pad() is a function we've designed to look like ANSI trim()

		functionRegistry.register( "pad",
				new LpadRpadPadEmulation( typeConfiguration ) );

		//legacy Hibernate convenience function for casting to string, defined
		//here as an alias for cast(arg as String)

		functionRegistry.register( "str",
				new CastStrEmulation( typeConfiguration ) );

		// Function to convert enum mapped as Ordinal to their ordinal value

		functionRegistry.register( "ordinal",
				new OrdinalFunction( typeConfiguration ) );

		// Function to convert enum mapped as String to their string value

		functionRegistry.register( "string",
				new StringFunction( typeConfiguration ) );

		//format() function for datetimes, emulated on many databases using the
		//Oracle-style to_char() function, and on others using their native
		//formatting functions

		functionFactory.format_toChar();

		//timestampadd()/timestampdiff() delegated back to the Dialect itself
		//since there is a great variety of different ways to emulate them

		functionFactory.timestampaddAndDiff( this );
		functionRegistry.registerAlternateKey( "dateadd", "timestampadd" );
		functionRegistry.registerAlternateKey( "datediff", "timestampdiff" );

		//ANSI SQL (and JPA) current date/time/timestamp functions, supported
		//natively on almost every database, delegated back to the Dialect

		functionRegistry.register(
				"current_date",
				new CurrentFunction(
						"current_date",
						currentTemporalSupport.currentDate(),
						dateType
				)
		);
		functionRegistry.register(
				"current_time",
				new CurrentFunction(
						"current_time",
						currentTemporalSupport.currentTime(),
						timeType
				)
		);
		functionRegistry.register(
				"current_timestamp",
				new CurrentFunction(
						"current_timestamp",
						currentTemporalSupport.currentTimestamp(),
						timestampType
				)
		);
		functionRegistry.registerAlternateKey( "current date", "current_date" );
		functionRegistry.registerAlternateKey( "current time", "current_time" );
		functionRegistry.registerAlternateKey( "current timestamp", "current_timestamp" );

		//HQL current instant/date/time/datetime functions, delegated back to the Dialect

		functionRegistry.register(
				"local_date",
				new CurrentFunction(
						"local_date",
						currentTemporalSupport.currentDate(),
						localDateType
				)
		);
		functionRegistry.register(
				"local_time",
				new CurrentFunction(
						"local_time",
						currentTemporalSupport.currentLocalTime(),
						localTimeType
				)
		);
		functionRegistry.register(
				"local_datetime",
				new CurrentFunction(
						"local_datetime",
						currentTemporalSupport.currentLocalTimestamp(),
						localDateTimeType
				)
		);
		functionRegistry.register(
				"offset_datetime",
				new CurrentFunction(
						"offset_datetime",
						currentTemporalSupport.currentTimestampWithTimeZone(),
						offsetDateTimeType
				)
		);
		functionRegistry.registerAlternateKey( "local date", "local_date" );
		functionRegistry.registerAlternateKey( "local time", "local_time" );
		functionRegistry.registerAlternateKey( "local datetime", "local_datetime" );
		functionRegistry.registerAlternateKey( "offset datetime", "offset_datetime" );

		functionRegistry.register(
				"instant",
				new CurrentFunction(
						"instant",
						currentTemporalSupport.currentTimestampWithTimeZone(),
						instantType
				)
		);
		functionRegistry.registerAlternateKey( "current_instant", "instant" ); //deprecated legacy!

		functionRegistry.register( "sql", new SqlFunction() );

		//audit column accessor functions for @Audited entities

		functionRegistry.register(
				AuditColumnFunction.CHANGESET_ID_FUNCTION,
				new AuditColumnFunction( AuditColumnFunction.CHANGESET_ID_FUNCTION, true, typeConfiguration )
		);
		functionRegistry.register(
				AuditColumnFunction.MODIFICATION_TYPE_FUNCTION,
				new AuditColumnFunction( AuditColumnFunction.MODIFICATION_TYPE_FUNCTION, false, typeConfiguration )
		);
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
	@SPI({ USE, IMPLEMENT })
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
						return buildStringToBooleanCast( "1", "0" );
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
						return buildStringToBooleanCast( "'Y'", "'N'" );
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
						return buildStringToBooleanCast( "'T'", "'F'" );
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
						return buildStringToBooleanCast( "true", "false" );
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

	private static final List<String> TRUE_BOOLEAN_SPELLINGS = List.of( "t", "true", "y", "1" );
	private static final List<String> FALSE_BOOLEAN_SPELLINGS = List.of( "f", "false", "n", "0" );

	/// Build the complete portable string-to-Boolean cast expression using the
	/// supplied SQL fragments for the true and false results.
	///
	/// The accepted input spellings are `t`, `true`, `y`, `1`, `f`, `false`,
	/// `n`, and `0`, compared in lowercase. Null or unrecognized input produces
	/// null. Providers should call this final helper from [#castPattern] rather
	/// than reproduce its VALUES-list and select/union alternatives.
	@SPI(USE)
	protected final String buildStringToBooleanCast(String trueValue, String falseValue) {
		final boolean supportsValuesList = getValuesListSupport().supports( ValuesListSupport.Context.QUERY );
		final var fragment = new StringBuilder();
		fragment.append( "(select v.x from (" );
		if ( supportsValuesList ) {
			fragment.append( "values (" );
			fragment.append( trueValue );
			fragment.append( "),(" );
			fragment.append( falseValue );
			fragment.append( ")) v(x)" );
		}
		else {
			fragment.append( "select " );
			fragment.append( trueValue );
			fragment.append( " x");
			fragment.append( getSingleRowTableSupport().getSelectOnlyFromClause() );
			fragment.append(" union all select " );
			fragment.append( falseValue );
			fragment.append( getSingleRowTableSupport().getSelectOnlyFromClause() );
			fragment.append( ") v" );
		}
		fragment.append( " left join (" );
		if ( supportsValuesList ) {
			fragment.append( "values" );
			char separator = ' ';
			for ( String trueStringValue : TRUE_BOOLEAN_SPELLINGS ) {
				fragment.append( separator );
				fragment.append( "('" );
				fragment.append( trueStringValue );
				fragment.append( "'," );
				fragment.append( trueValue );
				fragment.append( ')' );
				separator = ',';
			}
			for ( String falseStringValue : FALSE_BOOLEAN_SPELLINGS ) {
				fragment.append( ",('" );
				fragment.append( falseStringValue );
				fragment.append( "'," );
				fragment.append( falseValue );
				fragment.append( ')' );
			}
			fragment.append( ") t(k,v)" );
		}
		else {
			fragment.append( "select '" );
			fragment.append( TRUE_BOOLEAN_SPELLINGS.get( 0 ) );
			fragment.append( "' k," );
			fragment.append( trueValue );
			fragment.append( " v" );
			fragment.append( getSingleRowTableSupport().getSelectOnlyFromClause() );
			for ( int i = 1; i < TRUE_BOOLEAN_SPELLINGS.size(); i++ ) {
				fragment.append( " union all select '" );
				fragment.append( TRUE_BOOLEAN_SPELLINGS.get( i ) );
				fragment.append( "'," );
				fragment.append( trueValue );
				fragment.append( getSingleRowTableSupport().getSelectOnlyFromClause() );
			}
			for ( String falseStringValue : FALSE_BOOLEAN_SPELLINGS ) {
				fragment.append( " union all select '" );
				fragment.append( falseStringValue );
				fragment.append( "'," );
				fragment.append( falseValue );
				fragment.append( getSingleRowTableSupport().getSelectOnlyFromClause() );
			}
			fragment.append( ") t" );
		}
		fragment.append( " on " );
		fragment.append( getLowercaseFunction() );
		fragment.append( "(?1)=t.k where t.v is null or v.x=t.v)" );
		return fragment.toString();
	}

	/// Build the complete string-to-Boolean cast expression using the database
	/// `decode` function and the supplied SQL fragments for true and false.
	///
	/// The accepted input spellings and null behavior are identical to
	/// [#buildStringToBooleanCast]. Providers should call this final helper from
	/// [#castPattern] when `decode` is the appropriate native rendering.
	@SPI(USE)
	protected final String buildStringToBooleanCastDecode(String trueValue, String falseValue) {
		final boolean supportsValuesList = getValuesListSupport().supports( ValuesListSupport.Context.QUERY );
		final var fragment = new StringBuilder();
		fragment.append( "(select v.x from (" );
		if ( supportsValuesList ) {
			fragment.append( "values (" );
			fragment.append( trueValue );
			fragment.append( "),(" );
			fragment.append( falseValue );
			fragment.append( ")) v(x)" );
		}
		else {
			fragment.append( "select " );
			fragment.append( trueValue );
			fragment.append( " x");
			fragment.append( getSingleRowTableSupport().getSelectOnlyFromClause() );
			fragment.append(" union all select " );
			fragment.append( falseValue );
			fragment.append( getSingleRowTableSupport().getSelectOnlyFromClause() );
			fragment.append( ") v" );
		}
		fragment.append( ", (" );
		if ( supportsValuesList ) {
			fragment.append( "values (" );
			fragment.append( buildStringToBooleanDecode( trueValue, falseValue ) );
			fragment.append( ")) t(v)" );
		}
		else {
			fragment.append( "select " );
			fragment.append( buildStringToBooleanDecode( trueValue, falseValue ) );
			fragment.append( " v");
			fragment.append( getSingleRowTableSupport().getSelectOnlyFromClause() );
			fragment.append(") t" );
		}
		fragment.append( " where t.v is null or v.x=t.v)" );
		return fragment.toString();
	}

	private String buildStringToBooleanDecode(String trueValue, String falseValue) {
		final var fragment = new StringBuilder();
		fragment.append( "decode(" );
		fragment.append( getLowercaseFunction() );
		fragment.append( "(?1)" );
		for ( String trueStringValue : TRUE_BOOLEAN_SPELLINGS ) {
			fragment.append( ",'" );
			fragment.append( trueStringValue );
			fragment.append( "'," );
			fragment.append( trueValue );
		}
		for ( String falseStringValue : FALSE_BOOLEAN_SPELLINGS ) {
			fragment.append( ",'" );
			fragment.append( falseStringValue );
			fragment.append( "'," );
			fragment.append( falseValue );
		}
		fragment.append( ",null)" );
		return fragment.toString();
	}

	/// Supply the SQL renderings used for a reusable single-row table and for an
	/// otherwise table-less select.
	///
	/// Override this method with one stable, non-null profile when either
	/// rendering differs from [SingleRowTableSupport#STANDARD]. The table
	/// expression and the complete select-only `from` fragment are independent;
	/// an empty fragment means that no `from` clause is required. Dialect
	/// subclasses refining a family profile should copy
	/// [#getSingleRowTableSupport] from the superclass and change only the values
	/// which differ.
	///
	/// @return the stable, non-null single-row-table-support profile
	/// @since 8.0
	@SPI({ IMPLEMENT, SUPPLY })
	public SingleRowTableSupport getSingleRowTableSupport() {
		return SingleRowTableSupport.STANDARD;
	}

	/**
	 * Obtain a pattern for the SQL equivalent to a
	 * {@code trim()} function call. The resulting
	 * pattern must contain a ?1 placeholder for the
	 * argument of type {@link String} and a ?2 placeholder
	 * for the trim character if {@code isWhitespace}
	 * was false.
	 *
	 * @param specification
	 * {@linkplain TrimSpec#LEADING leading},
	 * {@linkplain TrimSpec#TRAILING trailing},
	 * or {@linkplain TrimSpec#BOTH both}
	 *
	 * @param isWhitespace
	 * {@code true} if trimming whitespace, and the ?2
	 * placeholder for the trim character should be omitted,
	 * {@code false} if the trim character is explicit and
	 * the ?2 placeholder must be included in the pattern
	 */
	@SPI({ USE, IMPLEMENT })
	public String trimPattern(TrimSpec specification, boolean isWhitespace) {
		return "trim(" + specification + ( isWhitespace ? "" : " ?2" ) + " from ?1)";
	}

	/// Determine directional compatibility of two JDBC or extended SQL type
	/// codes.
	///
	/// Preserve widening integral compatibility and enum-to-character
	/// compatibility as directional relationships; accepting `(a,b)` does not
	/// imply that `(b,a)` is accepted. Provider overrides should delegate
	/// unmatched pairs.
	@SPI({ USE, IMPLEMENT })
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
		return switch (typeCode1) {
			case TINYINT -> typeCode2 == TINYINT
					|| typeCode2 == SMALLINT
					|| typeCode2 == INTEGER
					|| typeCode2 == BIGINT;
			case SMALLINT -> typeCode2 == SMALLINT
					|| typeCode2 == INTEGER
					|| typeCode2 == BIGINT;
			case INTEGER -> typeCode2 == INTEGER
					|| typeCode2 == BIGINT;
			default -> false;
		};
	}

	private boolean sameColumnType(int typeCode1, int typeCode2) {
		try {
			return Objects.equals( columnType(typeCode1), columnType(typeCode2) );
		}
		catch (IllegalArgumentException iae) {
			return false;
		}
	}

	/// Retrieve a defensive copy of the completed configuration defaults.
	///
	/// This engine bootstrap accessor materializes provider contributions once,
	/// after normal Dialect construction. Mutating the returned copy does not
	/// affect the Dialect or a later caller.
	public Properties getDefaultProperties() {
		Properties snapshot = defaultProperties;
		if ( snapshot == null ) {
			synchronized ( this ) {
				snapshot = defaultProperties;
				if ( snapshot == null ) {
					snapshot = new Properties();
					contributeDefaultProperties( snapshot );
					defaultProperties = snapshot;
				}
			}
		}
		final var copy = new Properties();
		copy.putAll( snapshot );
		return copy;
	}

	@Override
	@SPI( USE )
	public final String toString() {
		return getClass().getName() + ", version: " + getVersion();
	}


	// database type mapping support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/// Contribute database-specific Java, JDBC, DDL, and basic type descriptors.
	///
	/// Override this method when the database or JDBC driver requires descriptors
	/// beyond the inherited registrations. Invoke `super` first unless the
	/// Dialect intentionally replaces the complete inherited contribution
	/// protocol. Obtain reusable dialect-specific JDBC descriptors from the
	/// family facades in [org.hibernate.dialect.type.spi], and preserve whether a
	/// descriptor replaces an existing registration or is added only when absent.
	///
	/// @param typeContributions the Hibernate-supplied contribution registry
	/// @param serviceRegistry the Hibernate-supplied services, including class
	/// loading used by driver-backed descriptor factories
	///
	/// Mutate the supplied boot-scoped registries only during this callback and
	/// do not retain the contribution context or a mutable registry afterward.
	/// Implement an independent [org.hibernate.boot.model.TypeContributor]
	/// instead when the contribution is not owned by a Dialect.
	/// @since 8.0
	@SPI({ USE, IMPLEMENT, SUPPLY })
	public void contributeTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		// by default, not much to do...
		registerColumnTypes( typeContributions, serviceRegistry );
		final var nationalizationSupport = getNationalizationSupport();
		final var jdbcTypeRegistry = typeContributions.getTypeConfiguration().getJdbcTypeRegistry();
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

		if ( getArraySupport().supports( STANDARD_ARRAY ) ) {
			jdbcTypeRegistry.addTypeConstructorIfAbsent( ArrayJdbcTypeConstructor.INSTANCE );
		}
		if ( getLobSupport().supportsMaterializedLobAccess() ) {
			jdbcTypeRegistry.addDescriptor( SqlTypes.MATERIALIZED_BLOB, BlobJdbcType.MATERIALIZED );
			jdbcTypeRegistry.addDescriptor( SqlTypes.MATERIALIZED_CLOB, ClobJdbcType.MATERIALIZED );
			jdbcTypeRegistry.addDescriptor( SqlTypes.MATERIALIZED_NCLOB, NClobJdbcType.MATERIALIZED );
		}
		if ( getLobSupport().isLobType( LONG32VARCHAR ) ) {
			jdbcTypeRegistry.addDescriptor( new LongVarcharJdbcType( SqlTypes.LONG32VARCHAR, CLOB ) );
		}
		if ( getLobSupport().isLobType( LONG32NVARCHAR ) && nationalizationSupport == NationalizationSupport.EXPLICIT ) {
			jdbcTypeRegistry.addDescriptor( new LongNVarcharJdbcType( SqlTypes.LONG32NVARCHAR, NCLOB ) );
		}
		if ( getLobSupport().isLobType( LONG32VARBINARY ) ) {
			jdbcTypeRegistry.addDescriptor( new LongVarbinaryJdbcType( LONG32VARBINARY, BLOB ) );
		}
	}

	/**
	 * A {@link LobMergeStrategy} representing the legacy behavior of Hibernate.
	 * LOBs are not processed by merge.
	 */
	@SuppressWarnings("unused")
	@SPI( USE )
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
	@SPI( USE )
	protected static final LobMergeStrategy STREAM_XFER_LOB_MERGE_STRATEGY = new LobMergeStrategy() {
		@Override
		public Blob mergeBlob(Blob original, Blob target, SharedSessionContractImplementor session) {
			if ( original != target ) {
				try {
					// the BLOB just read during the load phase of merge
					final var connectedStream = target.setBinaryStream( 1L );
					// the BLOB from the detached state
					final var detachedStream = original.getBinaryStream();
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
					final var connectedStream = target.setAsciiStream( 1L );
					// the CLOB from the detached state
					final var detachedStream = original.getAsciiStream();
					detachedStream.transferTo( connectedStream );
					return target;
				}
				catch (IOException e ) {
					throw new HibernateException( "Unable to copy stream content", e );
				}
				catch (SQLException e ) {
					throw session.getFactory().getJdbcServices().getSqlExceptionHelper()
							.convert( e, "unable to merge CLOB data" );
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
					final var connectedStream = target.setAsciiStream( 1L );
					// the NCLOB from the detached state
					final var detachedStream = original.getAsciiStream();
					detachedStream.transferTo( connectedStream );
					return target;
				}
				catch (IOException e ) {
					throw new HibernateException( "Unable to copy stream content", e );
				}
				catch (SQLException e ) {
					throw session.getFactory().getJdbcServices().getSqlExceptionHelper()
							.convert( e, "unable to merge NCLOB data" );
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
	@SPI( USE )
	protected static final LobMergeStrategy NEW_LOCATOR_LOB_MERGE_STRATEGY = new LobMergeStrategy() {
		@Override
		public Blob mergeBlob(Blob original, Blob target, SharedSessionContractImplementor session) {
			if ( original == null && target == null ) {
				return null;
			}
			final var jdbcServices = session.getFactory().getJdbcServices();
			try {
				final var lobCreator = jdbcServices.getLobCreator( session );
				return original == null
						? lobCreator.createBlob( ArrayHelper.EMPTY_BYTE_ARRAY )
						: lobCreator.createBlob( original.getBinaryStream(), original.length() );
			}
			catch (SQLException e) {
				throw jdbcServices.getSqlExceptionHelper()
						.convert( e, "unable to merge BLOB data" );
			}
		}

		@Override
		public Clob mergeClob(Clob original, Clob target, SharedSessionContractImplementor session) {
			if ( original == null && target == null ) {
				return null;
			}
			final var jdbcServices = session.getFactory().getJdbcServices();
			try {
				final var lobCreator = jdbcServices.getLobCreator( session );
				return original == null
						? lobCreator.createClob( "" )
						: lobCreator.createClob( original.getCharacterStream(), original.length() );
			}
			catch (SQLException e) {
				throw jdbcServices.getSqlExceptionHelper()
						.convert( e, "unable to merge CLOB data" );
			}
		}

		@Override
		public NClob mergeNClob(NClob original, NClob target, SharedSessionContractImplementor session) {
			if ( original == null && target == null ) {
				return null;
			}
			final var jdbcServices = session.getFactory().getJdbcServices();
			try {
				final var lobCreator = jdbcServices.getLobCreator( session );
				return original == null
						? lobCreator.createNClob( "" )
						: lobCreator.createNClob( original.getCharacterStream(), original.length() );
			}
			catch (SQLException e) {
				throw jdbcServices.getSqlExceptionHelper()
						.convert( e, "unable to merge NCLOB data" );
			}
		}
	};

	/**
	 * Get the {@link LobMergeStrategy} to use, {@link #NEW_LOCATOR_LOB_MERGE_STRATEGY}
	 * by default.
	 */
	@SPI({ IMPLEMENT, SUPPLY })
	public LobMergeStrategy getLobMergeStrategy() {
		return NEW_LOCATOR_LOB_MERGE_STRATEGY;
	}


	// native identifier generation ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/// Supply the semantic identifier-generation type used for native generation.
	///
	/// Return a non-null typed value instead of a legacy generator name or
	/// implementation class. The inherited implementation selects [GenerationType#IDENTITY]
	/// when this Dialect's [IdentityColumnSupport] supports identity columns and
	/// [GenerationType#SEQUENCE] otherwise. Override it when the database's native
	/// choice differs from that rule.
	///
	/// Legacy `"native"` mappings and [org.hibernate.annotations.NativeGenerator]
	/// consume this same provider decision.
	///
	/// @return the non-null semantic native identifier-generation type
	/// @see org.hibernate.annotations.NativeGenerator
	/// @since 7.0
	@Incubating
	@SPI({ IMPLEMENT, SUPPLY })
	public GenerationType getNativeValueGenerationStrategy() {
		return getIdentityColumnSupport().supportsIdentityColumns()
				? GenerationType.IDENTITY
				: GenerationType.SEQUENCE;
	}

	// IDENTITY support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/// Supply the stable identity-column DDL, insert syntax, and
	/// identity-specific retrieval behavior for this Dialect.
	///
	/// Override this method to supply a provider implementation of
	/// [IdentityColumnSupport]. Prefer extending [IdentityColumnSupportBase] and
	/// return the same immutable or otherwise stable support instance on every
	/// invocation. Do not construct Hibernate mutation delegates in this supply
	/// point.
	///
	/// @since 8.0
	@SPI({ IMPLEMENT, SUPPLY })
	public IdentityColumnSupport getIdentityColumnSupport() {
		return IdentityColumnSupportBase.NONE;
	}

	// SEQUENCE support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/// Supplies the complete sequence value-expression and lifecycle-DDL
	/// strategy for this Dialect.
	///
	/// Return the same stable, thread-safe strategy for the Dialect's lifetime.
	/// Use [SequenceSupports#none()] when sequences are unsupported, select one
	/// of the other stock strategies only when its complete grammar matches the
	/// database, or supply a provider implementation of [SequenceSupport].
	///
	/// @return the non-null sequence-support strategy
	/// @since 8.0
	/// @see SequenceSupport
	@SPI({ USE, IMPLEMENT, SUPPLY })
	public SequenceSupport getSequenceSupport() {
		return SequenceSupports.none();
	}

	/// Supplies the complete database-native row-level-security strategy for
	/// this Dialect.
	///
	/// Return the same stable, thread-safe strategy for the Dialect's lifetime.
	/// Use [RowLevelSecurityStrategies#none()] when native row-level security is
	/// unsupported, and return declarative DDL instead of mutating boot metadata.
	///
	/// @return the non-null row-level-security strategy
	/// @since 8.0
	/// @see RowLevelSecurity
	@Incubating
	@SPI({ USE, IMPLEMENT, SUPPLY })
	public RowLevelSecurity getRowLevelSecurity() {
		return RowLevelSecurityStrategies.none();
	}

	/// Supply the strategy Hibernate uses to discover existing database
	/// sequences and their metadata.
	///
	/// Return [SequenceInformationExtractors#none()] when the database version
	/// does not expose sequence metadata. Configure ordinary single-query
	/// extraction with [SequenceInformationExtractors#builder(String)], and
	/// implement [SequenceInformationExtractor] directly only for a genuinely
	/// multi-stage algorithm.
	///
	/// @see SequenceInformationExtractor
	/// @see SequenceInformationExtractors
	@SPI({ USE, IMPLEMENT, SUPPLY })
	public SequenceInformationExtractor getSequenceInformationExtractor() {
		return SequenceInformationExtractors.none();
	}

	/// Supply the context-bound strategy Hibernate uses to discover database
	/// tables, keys, indexes, and related schema metadata.
	///
	/// Return a new extractor for `extractionContext`. Prefer a stock profile
	/// from [InformationExtractors], and implement [InformationExtractor]
	/// directly only when none of those complete profiles matches the database.
	///
	/// @since 7.2
	/// @see InformationExtractor
	/// @see InformationExtractors
	@SPI({ IMPLEMENT, SUPPLY })
	public InformationExtractor getInformationExtractor(ExtractionContext extractionContext) {
		return InformationExtractors.jdbcMetadata( extractionContext );
	}

	// GUID support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	// limit/offset support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/// Supplies execution-time pagination for already-rendered SQL requested by
	/// [Query#setMaxResults] or [Query#setFirstResult].
	///
	/// This is distinct from SQL AST
	/// [org.hibernate.dialect.sql.ast.spi.PaginationRenderingSupport], which
	/// handles offset and fetch clauses represented in the semantic tree. A
	/// custom Dialect should normally return a stock [LimitHandler] or extend a
	/// supported handler family. The returned handler may be reused concurrently
	/// and must not retain per-query state.
	///
	/// @return a non-null handler
	/// @throws UnsupportedOperationException if execution-time pagination is not
	/// supported
	@SPI(SUPPLY)
	public LimitHandler getLimitHandler() {
		throw new UnsupportedOperationException("this dialect does not support query pagination");
	}


	// lock acquisition support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/// Supplies the cohesive pessimistic-locking profile for this Dialect.
	///
	/// The profile coordinates capability metadata, statement-clause rendering,
	/// table hints, already-rendered SQL rewriting, connection timeout handling,
	/// and follow-on locking. A custom Dialect should supply one internally
	/// consistent profile instead of overriding those behaviors independently.
	/// The returned support must be non-null and stable for the Dialect lifetime;
	/// its components may be reused concurrently and must not retain per-query
	/// requests.
	///
	/// @see LockingSupport
	@SPI({ IMPLEMENT, SUPPLY })
	public LockingSupport getLockingSupport() {
		return LockingSupportSimple.STANDARD_SUPPORT;
	}

	private LockingSupport.Metadata getLockingMetadata() {
		return getLockingSupport().getMetadata();
	}

	/// Supplies the per-translation strategy for a statement-level
	/// [PessimisticLockStyle#CLAUSE locking clause].
	///
	/// @see LockingClauseStrategy
	@SPI({ USE, IMPLEMENT, SUPPLY })
	public LockingClauseStrategy getLockingClauseStrategy(QuerySpec querySpec, LockOptions lockOptions) {
		if ( getLockingMetadata().getPessimisticLockStyle() != PessimisticLockStyle.CLAUSE || lockOptions == null ) {
			return NON_CLAUSE_STRATEGY;
		}

		final var lockKind = PessimisticLockKind.interpret( lockOptions.getLockMode() );
		if ( lockKind == PessimisticLockKind.NONE ) {
			return NonLockingClauseStrategy.NON_CLAUSE_STRATEGY;
		}

		final var lockingMetadata = getLockingMetadata();
		final var rowLockStrategy = switch ( lockKind ) {
			case SHARE -> lockingMetadata.getReadRowLockStrategy();
			case UPDATE -> lockingMetadata.getWriteRowLockStrategy();
			case NONE -> throw new IllegalStateException( "Should never happen due to checks above" );
		};

		return buildLockingClauseStrategy( lockKind, rowLockStrategy, lockOptions, querySpec.getRootPathsForLocking() );
	}

	/// Builds the per-translation statement-clause strategy after the lock kind,
	/// row-targeting strategy, and root paths have been resolved.
	///
	/// @see LockingClauseStrategy
	@SPI({ USE, IMPLEMENT, SUPPLY })
	protected LockingClauseStrategy buildLockingClauseStrategy(
			PessimisticLockKind lockKind,
			RowLockStrategy rowLockStrategy,
			LockOptions lockOptions,
			Set<NavigablePath> rootPathsForLocking) {
		return new StandardLockingClauseStrategy(
				getLockingSupport().getLockingClauseRenderer(),
				lockKind,
				rowLockStrategy,
				lockOptions,
				rootPathsForLocking
		);
	}

	/// The stable factory which creates executable entity-locking strategies for
	/// this Dialect.
	///
	/// Override this method to select a stock profile from [EntityLockingStrategies]
	/// or to supply a thread-safe custom factory. The factory must not retain a
	/// request or its target, and must return a non-null strategy.
	///
	/// @see EntityLockingStrategyFactory
	///
	/// @since 8.0
	@SPI({ IMPLEMENT, SUPPLY })
	public EntityLockingStrategyFactory getEntityLockingStrategyFactory() {
		return EntityLockingStrategies.standard();
	}

	// table support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/// Select the Hibernate-owned fallback families used for SQM update/delete
	/// and insert operations targeting entities mapped to multiple tables.
	///
	/// A custom Dialect should return one of the named profiles when both
	/// operation families use the same strategy kind, or construct an asymmetric
	/// profile when they differ. Local, global, and persistent-table selections
	/// require the corresponding non-null temporary-table strategy, while `CTE`
	/// requires non-query CTE support.
	///
	/// Configured global and entity-specific custom strategies take precedence
	/// over this fallback. Supply a custom execution implementation with
	/// [org.hibernate.cfg.QuerySettings#QUERY_MULTI_TABLE_MUTATION_STRATEGY] or
	/// [org.hibernate.cfg.QuerySettings#QUERY_MULTI_TABLE_INSERT_STRATEGY]
	/// instead of depending on Hibernate's internal fallback implementations.
	///
	/// @return a stable, non-null fallback selection profile
	///
	/// @since 8.0
	@SPI({ IMPLEMENT, SUPPLY })
	public MultiTableMutationSupport getMultiTableMutationSupport() {
		return MultiTableMutationSupport.PERSISTENT_TABLE;
	}

	/// Supply the catalog separator used when JDBC metadata is unavailable.
	///
	/// This is independent of [#getNameQualifierSupport()]. JDBC metadata remains
	/// authoritative when it supplies a separator.
	///
	/// @return the non-null fallback catalog separator
	@SPI({ IMPLEMENT, SUPPLY })
	public String getCatalogSeparator() {
		return ".";
	}

	/// Supply current temporal expressions and database-side timestamp retrieval.
	/// Return one stable, non-null, thread-safe implementation. Providers may
	/// implement [CurrentTemporalSupport] directly and return `this`.
	///
	/// @since 8.0
	/// @see CurrentTemporalSupport
	@SPI({ IMPLEMENT, SUPPLY })
	public CurrentTemporalSupport getCurrentTemporalSupport() {
		return CurrentTemporalSupports.standard();
	}

	/// Supply datetime-format translation. Return one stable, non-null,
	/// thread-safe implementation.
	///
	/// @since 8.0
	/// @see TemporalFormatSupport
	@SPI({ IMPLEMENT, SUPPLY })
	public TemporalFormatSupport getTemporalFormatSupport() {
		return TemporalFormatSupports.standard();
	}

	/// Supply extraction and timestamp-arithmetic syntax. Return one stable,
	/// non-null, thread-safe implementation. Providers may implement
	/// [TemporalOperationSupport] directly and return `this`.
	///
	/// @since 8.0
	/// @see TemporalOperationSupport
	@SPI({ IMPLEMENT, SUPPLY })
	public TemporalOperationSupport getTemporalOperationSupport() {
		return this;
	}

	/// Supply temporal precision-adjustment and literal-offset semantics.
	/// Return one stable, non-null immutable profile.
	///
	/// @since 8.0
	/// @see TemporalValueSemantics
	@SPI({ IMPLEMENT, SUPPLY })
	public TemporalValueSemantics getTemporalValueSemantics() {
		return TemporalValueSemantics.STANDARD;
	}


	// SQLException support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/// Supplies vendor-specific SQL-exception conversion for this Dialect.
	///
	/// The returned delegate runs in addition to Hibernate's standard JDBC
	/// exception-hierarchy and SQL-state delegates. Its `null` result means that
	/// it declined the exception so conversion may continue. Return `null` from
	/// this method when the Dialect has no vendor-specific delegate.
	///
	/// Prefer stable vendor error codes or SQL states and preserve the original
	/// [SQLException].
	///
	/// @return the vendor conversion delegate, or `null` when none is supplied
	/// @since 8.0
	/// @see SQLExceptionConversionDelegate
	@SPI({ USE, IMPLEMENT, SUPPLY })
	public @Nullable SQLExceptionConversionDelegate buildSQLExceptionConversionDelegate() {
		return null;
	}

	private static final ViolatedConstraintNameExtractor EXTRACTOR = sqle -> null;

	/// Supplies the extractor used to recover violated constraint names from
	/// database exceptions.
	///
	/// The extractor returns `null` when a name cannot be recovered. Do not use
	/// an empty string or sentinel to represent absence.
	///
	/// @return the non-null constraint-name extractor
	/// @since 8.0
	/// @see ViolatedConstraintNameExtractor
	@SPI({ USE, IMPLEMENT, SUPPLY })
	public ViolatedConstraintNameExtractor getViolatedConstraintNameExtractor() {
		return EXTRACTOR;
	}


	// union subclass support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/// Render the complete typed-null select-item expression for one arm of a
	/// union query.
	///
	/// Use the complete mapping when the database requires an explicit cast so
	/// that named and aggregate types, length, precision, scale, and temporal
	/// precision are retained. Return the complete expression, not only a type
	/// name. The standard form is the untyped SQL literal `null`.
	///
	/// @param sqlTypeMapping the complete mapping of the union select item
	/// @param typeConfiguration the active type configuration
	/// @return the complete select-item expression
	@SPI({ USE, IMPLEMENT })
	public String getSelectClauseNullString(SqlTypedMapping sqlTypeMapping, TypeConfiguration typeConfiguration) {
		return "null";
	}

	/**
	 * Get the SQL string representation for a specific set operator (UNION, INTERSECT, EXCEPT).
	 * <p>
	 * The default implementation delegates to {@link SetOperator#sqlString()}.
	 * Dialects like Cloud Spanner that require explicit 'DISTINCT' keywords can override this.
	 *
	 * @param operator The set operator
	 * @return The SQL fragment (e.g., "union", "union all", "union distinct")
	 */
	@SPI({ USE, IMPLEMENT })
	public String getSetOperatorSqlString(SetOperator operator) {
		return operator.sqlString();
	}

	// miscellaneous support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/// The unquoted SQL function name which transforms a string to lowercase.
	///
	/// Return only the stable function name, without parentheses or arguments;
	/// Hibernate composes the invocation. The standard name is `lower`.
	///
	/// @return the non-null lowercase function name
	@SPI({ USE, IMPLEMENT })
	public String getLowercaseFunction() {
		return "lower";
	}

	/// The database's native predicate syntax and predicate-placement profile.
	///
	/// Override this method to supply independent support for a native
	/// case-insensitive-`like` operator, scalar `distinct from`, truthness
	/// predicates, and predicates used as value expressions. An absent
	/// case-insensitive-`like` operator selects Hibernate's lowercase-expression
	/// emulation. Return one immutable, non-null profile whose values remain
	/// stable for the lifetime of this Dialect; do not override individual
	/// consumers to reproduce these decisions.
	///
	/// @return this Dialect's stable predicate-support profile
	/// @since 8.0
	@SPI({ IMPLEMENT, SUPPLY })
	public PredicateSupport getPredicateSupport() {
		return PredicateSupport.STANDARD;
	}

	/// The native row-value syntax of this Dialect.
	///
	/// Override this method to supply explicit `row(a, b)` construction, row
	/// equality, ordering, and distinctness comparisons, row-valued `IN` lists,
	/// row-valued `IN` subqueries, and quantified row comparisons. Treat each
	/// feature as an exact native-syntax declaration: do not infer `IN`-subquery
	/// support from `IN`-list support or explicit row construction from
	/// comparison support. Copy the superclass profile when refining an inherited
	/// Dialect family. Return one immutable, non-null profile whose values remain
	/// stable for the lifetime of this Dialect.
	///
	/// @return this Dialect's stable row-value-support profile
	/// @since 8.0
	@SPI({ IMPLEMENT, SUPPLY })
	public RowValueSupport getRowValueSupport() {
		return RowValueSupport.STANDARD;
	}

	/// Supplies the database's set operators and structural query-group
	/// capabilities.
	///
	/// Return one non-null, immutable profile whose values remain stable for this
	/// Dialect's lifetime. Declare each [SetOperator] independently; support for
	/// a distinct operator never implies its `ALL` form. Structural capabilities
	/// such as `UNION` placement in subqueries are separate from operator support.
	/// Continue to override [#getSetOperatorSqlString(SetOperator)] when an
	/// operator requires database-specific spelling. When refining a family
	/// Dialect, start from
	/// `SetOperationSupport.builder(super.getSetOperationSupport())` and change
	/// only the differing values.
	///
	/// @return this Dialect's stable set-operation profile
	/// @since 8.0
	@SPI({ IMPLEMENT, SUPPLY })
	public SetOperationSupport getSetOperationSupport() {
		return SetOperationSupport.STANDARD;
	}

	/// Supplies the database's supported subquery placements.
	///
	/// Return one non-null, immutable profile whose values remain stable for this
	/// Dialect's lifetime. Declare every placement independently. In particular,
	/// scalar subqueries in the select list and `exists` predicates in the select
	/// list are separate capabilities. Lateral spelling and fallback rendering
	/// remain translator responsibilities. When refining a family Dialect, start
	/// from `SubquerySupport.builder(super.getSubquerySupport())` and change only
	/// the differing placements.
	///
	/// @return this Dialect's stable subquery-support profile
	/// @since 8.0
	@SPI({ IMPLEMENT, SUPPLY })
	public SubquerySupport getSubquerySupport() {
		return SubquerySupport.STANDARD;
	}

	/// Supplies the expression-coercion adaptations required by this database.
	///
	/// Return one non-null, immutable profile whose values remain stable for this
	/// Dialect's lifetime. Each requirement directs a focused rendering
	/// adaptation; it does not report general database cast support. When refining
	/// a family Dialect, start from
	/// `ExpressionCoercionSupport.builder(super.getExpressionCoercionSupport())`
	/// and change only the differing requirement.
	///
	/// @return this Dialect's stable expression-coercion requirement profile
	/// @since 8.0
	@SPI({ IMPLEMENT, SUPPLY })
	public ExpressionCoercionSupport getExpressionCoercionSupport() {
		return ExpressionCoercionSupport.STANDARD;
	}

	/// Supplies the native window-function syntax supported by this database.
	///
	/// Return one non-null, immutable profile whose values remain stable for this
	/// Dialect's lifetime. Baseline window calls, window partitioning, each frame
	/// unit, and frame exclusion are independent grammar facts. Every refinement
	/// requires baseline window support, and frame exclusion additionally requires
	/// a supported frame unit. Function-specific restrictions and individual
	/// frame-bound restrictions remain the responsibility of focused rendering.
	///
	/// When refining a family Dialect, start from
	/// `WindowFunctionSupport.builder(super.getWindowFunctionSupport())` and
	/// change only the differing features.
	///
	/// @return this Dialect's stable window-function-support profile
	/// @since 8.0
	@SPI({ IMPLEMENT, SUPPLY })
	public WindowFunctionSupport getWindowFunctionSupport() {
		return WindowFunctionSupport.STANDARD;
	}

	// identifier, keyword, and literal support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/// Supply identifier quoting, limits, and helper construction.
	///
	/// Return one stable, non-null strategy for this Dialect's lifetime.
	///
	/// @since 8.0
	/// @see IdentifierSupport
	@SPI({ IMPLEMENT, SUPPLY })
	public IdentifierSupport getIdentifierSupport() {
		return this;
	}

	/// Supply the stable Dialect keyword profile and JDBC-keyword filter.
	///
	/// Return one stable, non-null strategy for this Dialect's lifetime.
	///
	/// @since 8.0
	/// @see KeywordSupport
	@SPI({ IMPLEMENT, SUPPLY })
	public KeywordSupport getKeywordSupport() {
		return this;
	}

	/// Return the immutable ANSI and Dialect-defined keyword profile.
	@Override
	@SPI(USE)
	public final Set<String> getKeywords() {
		final KeywordSupport support = getKeywordSupport();
		if ( support != this ) {
			return support.getKeywords();
		}
		Set<String> snapshot = keywords;
		if ( snapshot == null ) {
			synchronized ( this ) {
				snapshot = keywords;
				if ( snapshot == null ) {
					final var collected = new HashSet<String>();
					contributeKeywords( keyword -> {
						if ( keyword != null ) {
							final String normalized = keyword.trim().toLowerCase( Locale.ROOT );
							if ( !normalized.isEmpty() ) {
								collected.add( normalized );
							}
						}
					} );
					snapshot = Set.copyOf( collected );
					keywords = snapshot;
				}
			}
		}
		return snapshot;
	}

	/// Supply SQL literal rendering.
	///
	/// Return one stable, non-null strategy for this Dialect's lifetime.
	///
	/// @since 8.0
	/// @see LiteralSupport
	@SPI({ IMPLEMENT, SUPPLY })
	public LiteralSupport getLiteralSupport() {
		return this;
	}


	// DDL support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/// Supply existence-check placement for schema DDL targets.
	///
	/// Return one immutable profile for this Dialect's lifetime.
	///
	/// @since 8.0
	/// @see IfExistsSupport
	@SPI({ IMPLEMENT, SUPPLY })
	public IfExistsSupport getIfExistsSupport() {
		return IfExistsSupport.NONE;
	}

	/// Supply alter-table and add-column grammar.
	///
	/// Return a stable strategy and do not retain [org.hibernate.dialect.schema.spi.AlterColumnTypeRequest]
	/// instances. A Dialect may implement [AlterTableSupport] directly and return
	/// itself so subclass overrides retain ordinary Java dispatch.
	///
	/// @since 8.0
	/// @see AlterTableSupport
	@SPI({ IMPLEMENT, SUPPLY })
	public AlterTableSupport getAlterTableSupport() {
		return this;
	}

	/// Supply table and view creation grammar.
	///
	/// Return a stable strategy after configured storage-engine and version
	/// choices are resolved.
	///
	/// @since 8.0
	/// @see TableCreationSupport
	@SPI({ IMPLEMENT, SUPPLY })
	public TableCreationSupport getTableCreationSupport() {
		return this;
	}

	/// Supply non-identity column-definition composition.
	///
	/// Consume rendered request values without retaining mapping or call-scoped
	/// state.
	///
	/// @since 8.0
	/// @see ColumnDefinitionSupport
	@SPI({ IMPLEMENT, SUPPLY })
	public ColumnDefinitionSupport getColumnDefinitionSupport() {
		return this;
	}

	/// Supply index creation grammar and name qualification policy.
	///
	/// Consume supported immutable column views without depending on mutable
	/// mapping-model internals.
	///
	/// @since 8.0
	/// @see IndexDdlSupport
	@SPI({ IMPLEMENT, SUPPLY })
	public IndexDdlSupport getIndexDdlSupport() {
		return this;
	}

	/// Supply the strategy for controlling constraints during table cleaning.
	///
	/// Implement only the command family selected by
	/// [ConstraintControlSupport#constraintControlMode()].
	///
	/// @since 8.0
	/// @see ConstraintControlSupport
	@SPI({ IMPLEMENT, SUPPLY })
	public ConstraintControlSupport getConstraintControlSupport() {
		return this;
	}

	@Override
	public ConstraintControlMode constraintControlMode() {
		return ConstraintControlMode.NONE;
	}

	/// Supply ordered table-truncation commands.
	///
	/// Return an empty command list for an empty request and preserve table order.
	///
	/// @since 8.0
	/// @see TruncateSupport
	@SPI({ IMPLEMENT, SUPPLY })
	public TruncateSupport getTruncateSupport() {
		return this;
	}

	@Override
	public TruncateMode truncateMode() {
		return TruncateMode.PER_TABLE;
	}

	/// Supply schema-drop composition and ordered pre-drop commands.
	///
	/// Return one immutable profile for this Dialect's lifetime.
	///
	/// @since 8.0
	/// @see SchemaDropSupport
	@SPI({ IMPLEMENT, SUPPLY })
	public SchemaDropSupport getSchemaDropSupport() {
		return SchemaDropSupport.STANDARD;
	}

	private final StandardTableExporter tableExporter = new StandardTableExporter( this );
	private final StandardUserDefinedTypeExporter userDefinedTypeExporter = new StandardUserDefinedTypeExporter( this );
	private final StandardSequenceExporter sequenceExporter = new StandardSequenceExporter( this );
	private final StandardIndexExporter indexExporter = new StandardIndexExporter( this );
	private final StandardForeignKeyExporter foreignKeyExporter = new StandardForeignKeyExporter( this );
	private final StandardTemporaryTableExporter temporaryTableExporter = new StandardTemporaryTableExporter( this );
	private final StandardTableMigrator tableMigrator = new StandardTableMigrator( this );
	private final StandardTableCleaner tableCleaner = new StandardTableCleaner( this );

	/// Supply the exporter used to create and drop relational tables.
	///
	/// Return one stable exporter owned by this Dialect. Prefer
	/// [StandardTableExporter] when only its focused table-DDL template hooks are
	/// needed.
	@SPI({ USE, IMPLEMENT, SUPPLY })
	public Exporter<Table> getTableExporter() {
		return tableExporter;
	}

	/// Supply the strategy which migrates relational table definitions.
	///
	/// Return one stable migrator for this Dialect's lifetime.
	///
	/// @since 8.0
	/// @see TableMigrator
	@SPI({ IMPLEMENT, SUPPLY })
	public TableMigrator getTableMigrator() {
		return tableMigrator;
	}

	/// Supply the strategy which produces complete table-cleaning commands.
	///
	/// Return one stable cleaner whose reported modes agree with its command
	/// families.
	///
	/// @since 8.0
	/// @see TableCleaner
	@SPI({ IMPLEMENT, SUPPLY })
	public TableCleaner getTableCleaner() {
		return tableCleaner;
	}

	/// Supply the exporter used to create and drop user-defined types.
	///
	/// Return one stable exporter owned by this Dialect. Use
	/// [StandardUserDefinedTypeExporter] when standard object-type DDL is
	/// sufficient.
	@SPI({ USE, IMPLEMENT, SUPPLY })
	public Exporter<UserDefinedType> getUserDefinedTypeExporter() {
		return userDefinedTypeExporter;
	}

	/// Supply the exporter used to create and drop relational sequences.
	///
	/// Return one stable exporter owned by this Dialect. Prefer
	/// [StandardSequenceExporter] when only sequence-name qualification differs.
	@SPI({ USE, IMPLEMENT, SUPPLY })
	public Exporter<Sequence> getSequenceExporter() {
		return sequenceExporter;
	}

	/// Supply the exporter used to create and drop relational indexes.
	///
	/// Return one stable exporter owned by this Dialect. Use
	/// [StandardIndexExporter] when standard index DDL is sufficient.
	@SPI({ IMPLEMENT, SUPPLY })
	public Exporter<Index> getIndexExporter() {
		return indexExporter;
	}

	/// Supply the exporter used to create and drop foreign-key constraints.
	///
	/// Return one stable exporter owned by this Dialect. Compose
	/// [StandardForeignKeyExporter] when the database decorates otherwise
	/// standard foreign-key DDL.
	@SPI({ USE, IMPLEMENT, SUPPLY })
	public Exporter<ForeignKey> getForeignKeyExporter() {
		return foreignKeyExporter;
	}

	// Temporary table support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/// Supplies the runtime DDL exporter for temporary-table descriptors.
	///
	/// The exporter must agree with the [TemporaryTableStrategy] instances
	/// supplied by this Dialect. Prefer [StandardTemporaryTableExporter] unless
	/// the database requires a different assembly of table or column fragments.
	/// The returned exporter must be non-null and safe for reuse.
	@SPI({ IMPLEMENT, SUPPLY })
	public TemporaryTableExporter getTemporaryTableExporter() {
		return temporaryTableExporter;
	}

	/// Supplies the fallback persistent-table strategy used when no native local
	/// or global temporary-table strategy is selected.
	///
	/// The returned strategy must be non-null and must describe behavior
	/// consistent with [#getTemporaryTableExporter].
	///
	/// @since 7.1
	@SPI({ IMPLEMENT, SUPPLY })
	public TemporaryTableStrategy getPersistentTemporaryTableStrategy() {
		return persistentTemporaryTableStrategy;
	}

	/// Supplies this Dialect's local temporary-table strategy.
	///
	/// Return `null` when the database has no supported local temporary-table
	/// form. A non-null strategy must be stable for the Dialect lifetime and
	/// consistent with [#getTemporaryTableExporter].
	///
	/// @since 7.1
	@SPI({ IMPLEMENT, SUPPLY })
	public @Nullable TemporaryTableStrategy getLocalTemporaryTableStrategy() {
		return null;
	}

	/// Supplies this Dialect's global temporary-table strategy.
	///
	/// Return `null` when the database has no supported global temporary-table
	/// form. A non-null strategy must be stable for the Dialect lifetime and
	/// consistent with [#getTemporaryTableExporter].
	///
	/// @since 7.1
	@SPI({ IMPLEMENT, SUPPLY })
	public @Nullable TemporaryTableStrategy getGlobalTemporaryTableStrategy() {
		return null;
	}

	// Catalog / schema creation ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/// Supply catalog and schema lifecycle behavior to schema tooling.
	///
	/// Return one stable, non-null strategy. Prefer a complete immutable profile
	/// from [NamespaceSupports], and implement [NamespaceSupport] directly only
	/// for custom or multi-command namespace SQL.
	///
	/// @since 8.0
	/// @see NamespaceSupport
	@SPI({ IMPLEMENT, SUPPLY })
	public NamespaceSupport getNamespaceSupport() {
		return NamespaceSupports.standard();
	}

	/// Supply the strategy which resolves the current schema for a JDBC
	/// connection.
	///
	/// Return a stable, non-null resolver. Use the default resolver unless the
	/// driver cannot implement [java.sql.Connection#getSchema()] correctly.
	///
	/// @see SchemaNameResolver
	@SPI({ IMPLEMENT, SUPPLY })
	public SchemaNameResolver getSchemaNameResolver() {
		return DefaultSchemaNameResolver.INSTANCE;
	}

	/// Supply this Dialect's foreign-key DDL and delete-semantics strategy.
	///
	/// The default strategy is this Dialect. Return one stable, non-null strategy
	/// when the database requires composition instead of direct overrides.
	///
	/// @see ForeignKeySupport
	/// @since 8.0
	@SPI({ IMPLEMENT, SUPPLY })
	public ForeignKeySupport getForeignKeySupport() {
		return this;
	}

	/// Supply this Dialect's check-constraint placement and rendering strategy.
	///
	/// The default strategy is this Dialect. A strategy supporting named column
	/// checks must also support anonymous column checks.
	///
	/// @see CheckConstraintSupport
	/// @since 8.0
	@SPI({ IMPLEMENT, SUPPLY })
	public CheckConstraintSupport getCheckConstraintSupport() {
		return this;
	}

	/// Supply schema-export support for comments attached to database objects.
	///
	/// This strategy does not control query comments, hints, SQL AST decoration,
	/// or statement inspection. Return one stable, non-null strategy.
	///
	/// @see SchemaCommentSupport
	/// @since 8.0
	@SPI({ IMPLEMENT, SUPPLY })
	public SchemaCommentSupport getSchemaCommentSupport() {
		return SchemaCommentSupports.none();
	}

	/// Supplies the strategy used to extract projected column aliases from JDBC
	/// result-set metadata during native-query auto-discovery.
	///
	/// Return [ColumnAliasExtractor#COLUMN_LABEL_EXTRACTOR] unless the JDBC
	/// driver requires extraction through
	/// [java.sql.ResultSetMetaData#getColumnName].
	/// The returned strategy must be stable and thread-safe.
	///
	/// @return the non-null alias extractor
	/// @since 8.0
	/// @see ColumnAliasExtractor
	@SPI({ USE, IMPLEMENT, SUPPLY })
	public ColumnAliasExtractor getColumnAliasExtractor() {
		return ColumnAliasExtractor.COLUMN_LABEL_EXTRACTOR;
	}

	// Informational metadata ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/// Supply the database and driver policy for LOB creation, binding,
	/// materialization, type classification, mutation ordering, and VALUE LOB
	/// DDL. Return one stable, thread-safe strategy for this Dialect.
	///
	/// @return the non-null LOB support strategy
	/// @since 8.0
	/// @see LobSupport
	@SPI({ IMPLEMENT, SUPPLY })
	public LobSupport getLobSupport() {
		return LobSupports.standard();
	}

	/// Determine whether query clauses may refer to select items by their
	/// one-based ordinal position.
	///
	/// Override this method when the database does not accept ordinal select-item
	/// references. SQL AST translators use the result to choose between rendering
	/// the ordinal and rendering the underlying expression.
	///
	/// @return `true` when ordinal select-item references are supported
	/// @since 8.0
	@SPI({ IMPLEMENT, SUPPLY })
	public boolean supportsOrdinalSelectItemReference() {
		return true;
	}

	/// Supply the database's default null ordering and explicit null-precedence
	/// syntax capabilities.
	///
	/// Override this method with a stable, non-null profile when the database's
	/// default null placement differs from [NullOrderingSupport#STANDARD], or when
	/// it cannot render `nulls first` and `nulls last` natively. Dialect subclasses
	/// refining a family profile should copy [#getNullOrderingSupport] from the
	/// superclass and change only the values which differ.
	///
	/// @return the stable, non-null null-ordering-support profile
	/// @since 8.0
	@SPI({ IMPLEMENT, SUPPLY })
	public NullOrderingSupport getNullOrderingSupport() {
		return NullOrderingSupport.STANDARD;
	}

	/// Supplies the database syntax for ordinary and distinct tuple counts.
	///
	/// Return one non-null, immutable profile whose values remain stable for
	/// this Dialect's lifetime. Select
	/// [TupleCountSupport.Syntax#UNSUPPORTED] to use Hibernate's existing
	/// emulation, [TupleCountSupport.Syntax#ARGUMENT_LIST] for syntax such as
	/// `count(a, b)`, or
	/// [TupleCountSupport.Syntax#PARENTHESIZED_TUPLE] for syntax such as
	/// `count((a, b))`. Configure the ordinary and distinct forms independently;
	/// a database may require different forms for them. When refining a family
	/// Dialect, copy [#getTupleCountSupport()] from the superclass and replace
	/// only the differing syntax choice.
	///
	/// @return the non-null tuple-count-syntax profile
	/// @since 8.0
	@SPI({ IMPLEMENT, SUPPLY })
	public TupleCountSupport getTupleCountSupport() {
		return TupleCountSupport.STANDARD;
	}

	/// Supplies the database limits for `IN`-expression elements and total JDBC
	/// statement parameters.
	///
	/// Return a non-null, immutable profile whose values remain stable for this
	/// Dialect's lifetime. Use [ParameterLimits#UNLIMITED] when neither limit
	/// applies and [ParameterLimits#of(int)] when both limits are identical.
	/// Keep the dimensions independent when the database imposes different
	/// expression and statement limits; Hibernate uses them for different
	/// planning decisions.
	///
	/// @return the non-null parameter-limit profile
	/// @since 8.0
	@SPI({ IMPLEMENT, SUPPLY })
	public ParameterLimits getParameterLimits() {
		return ParameterLimits.UNLIMITED;
	}

	/// Supply the database's stable string-value semantics.
	///
	/// @return the non-null string-value profile
	/// @since 8.0
	/// @see StringValueSemantics
	@SPI({ IMPLEMENT, SUPPLY })
	public StringValueSemantics getStringValueSemantics() {
		return StringValueSemantics.STANDARD;
	}

	/// Supply the stable strategy used for unique-key representation and DDL.
	///
	/// Return a stock profile from [UniqueDelegates], or a stable provider-owned
	/// implementation or decorator. Do not import a Hibernate `.internal` leaf.
	///
	/// @see UniqueDelegate
	/// @since 8.0
	@SPI({ IMPLEMENT, SUPPLY })
	public UniqueDelegate getUniqueDelegate() {
		return uniqueDelegate;
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
	@SPI({ USE, IMPLEMENT })
	public String getQueryHintString(String query, List<String> hintList) {
		if ( hintList.isEmpty() ) {
			return query;
		}
		else {
			final String hints = join( ", ", hintList );
			return isEmpty( hints ) ? query : getQueryHintString( query, hints );
		}
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
	@SPI({ USE, IMPLEMENT })
	public String getQueryHintString(String query, String hints) {
		return query;
	}

	/// Supply the final placement of a leading database hint relative to a user
	/// SQL comment.
	///
	/// Return one stable value. Select `BEFORE_COMMENT` only when the database
	/// requires its hint to be the first leading SQL comment.
	///
	/// @since 8.0
	/// @see QueryHintPlacement
	@SPI({ IMPLEMENT, SUPPLY })
	public QueryHintPlacement getQueryHintPlacement() {
		return QueryHintPlacement.AFTER_COMMENT;
	}

	/**
	 * A default {@link ScrollMode} to be used by {@link Query#scroll()}.
	 *
	 * @apiNote Certain dialects support a subset of {@link ScrollMode}s.
	 *
	 * @return the default {@link ScrollMode} to use.
	 */
	@SPI({ USE, IMPLEMENT, SUPPLY })
	public ScrollMode defaultScrollMode() {
		return ScrollMode.SCROLL_INSENSITIVE;
	}

	/// Supply the stable profile describing immediate retrieval of arbitrary
	/// database-generated values for this Dialect.
	///
	/// Override this method to declare native insert or update returning, row-id
	/// returning, or JDBC generated-key support for arbitrary generated columns.
	/// Identity-only retrieval belongs to [#getIdentityColumnSupport()], and an
	/// ordinary select after mutation is a Hibernate fallback rather than a
	/// capability of this profile. Refine a family profile by copying the value
	/// returned by the superclass and changing only the differing capabilities.
	///
	/// @since 8.0
	@SPI({ IMPLEMENT, SUPPLY })
	public GeneratedValuesSupport getGeneratedValuesSupport() {
		return GeneratedValuesSupport.STANDARD;
	}

	/// Supply the stable overrides applied to raw JDBC metadata reports.
	///
	/// Return one non-null immutable profile whose identity and answers remain
	/// stable for this Dialect's lifetime. Use `REPORTED` to retain Hibernate's
	/// interpreted driver answer, and force `SUPPORTED` or `UNSUPPORTED` only
	/// when the selected driver report is known to be inaccurate. This profile
	/// controls effective JDBC behavior, not callable SQL syntax.
	///
	/// @return the effective JDBC metadata overrides
	/// @see JdbcMetadataOverrides
	/// @since 8.0
	@SPI({ IMPLEMENT, SUPPLY })
	public JdbcMetadataOverrides getJdbcMetadataOverrides() {
		return JdbcMetadataOverrides.STANDARD;
	}

	/// Supply the `fetch` clause forms accepted by this database.
	///
	/// Return one non-null immutable profile whose identity and answers remain
	/// stable for this Dialect's lifetime. The four forms are independent; do
	/// not infer percent or ties support from ordinary row-count support.
	///
	/// @return the stable `fetch` clause capability profile
	/// @see FetchClauseSupport
	/// @since 8.0
	@SPI({ USE, IMPLEMENT, SUPPLY })
	public FetchClauseSupport getFetchClauseSupport() {
		return FetchClauseSupport.NONE;
	}

	/// Supply the strategy for synthetic table roots required while converting
	/// grouping and ordering expressions to SQL AST.
	///
	/// Return one non-null immutable or thread-safe strategy whose answers remain
	/// stable for this Dialect's lifetime. This strategy controls query structure;
	/// it does not apply to DDL, temporary tables, or no-column inserts.
	///
	/// @return the synthetic-table strategy
	/// @see SyntheticTableGroupSupport
	/// @since 8.0
	@SPI({ IMPLEMENT, SUPPLY })
	public SyntheticTableGroupSupport getSyntheticTableGroupSupport() {
		return SyntheticTableGroupSupport.NONE;
	}

	/// Supply the callable-statement protocol for this database and JDBC driver.
	///
	/// Return one non-null strategy whose behavior remains stable for this
	/// Dialect's lifetime. Use [CallableStatementSupports] for the standard and
	/// stock database protocols.
	///
	/// @return the callable-statement strategy
	/// @see CallableStatementSupport
	/// @since 8.0
	@SPI({ IMPLEMENT, SUPPLY })
	public CallableStatementSupport getCallableStatementSupport() {
		return CallableStatementSupports.standard();
	}

	/// Supply the stable factory which creates this Dialect's JDBC REF_CURSOR
	/// registration and extraction service.
	///
	/// Return one non-null factory whose identity and selection behavior remain
	/// stable for this Dialect's lifetime. This factory controls JDBC cursor
	/// access; [#getCallableStatementSupport()] independently controls whether and
	/// how the callable SQL protocol admits a REF_CURSOR parameter.
	///
	/// @return the REF_CURSOR JDBC access factory
	/// @see RefCursorSupportFactory
	/// @since 8.0
	@SPI({ IMPLEMENT, SUPPLY })
	public RefCursorSupportFactory getRefCursorSupportFactory() {
		return RefCursorSupports.metadataSelected();
	}

	/// Supply catalog and schema qualification support, or return `null` to use
	/// [java.sql.DatabaseMetaData].
	///
	/// This value does not define the catalog separator; override
	/// [#getCatalogSeparator()] independently when the metadata-free fallback is
	/// not `.`.
	///
	/// @see NameQualifierSupport
	@SPI({ USE, IMPLEMENT, SUPPLY })
	public @Nullable NameQualifierSupport getNameQualifierSupport() {
		return null;
	}

	/**
	 * The strategy used to determine the appropriate number of keys
	 * to load in a single SQL query with multi-key loading.
	 * @see org.hibernate.Session#byMultipleIds
	 * @see org.hibernate.Session#byMultipleNaturalId
	 */
	@SPI({ IMPLEMENT, SUPPLY })
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
	@SPI({ IMPLEMENT, SUPPLY })
	public MultiKeyLoadSizingStrategy getBatchLoadSizingStrategy() {
		return getMultiKeyLoadSizingStrategy();
	}

	private int calculateBatchSize(int numberOfColumns, int numberOfKeys, boolean padToPowerOfTwo) {
		final int batchSize = padToPowerOfTwo ? ceilingPowerOfTwo( numberOfKeys ) : numberOfKeys;
		final int maxBatchSize = getParameterLimits().parameterCountLimit() / numberOfColumns;
		return maxBatchSize > 0 && batchSize > maxBatchSize ? maxBatchSize : batchSize;
	}

	@SPI( USE )
	protected final MultiKeyLoadSizingStrategy STANDARD_MULTI_KEY_LOAD_SIZING_STRATEGY = this::calculateBatchSize;

	/**
	 * Is JDBC statement warning logging enabled by default?
	 *
	 * @since 5.1
	 */
	@SPI({ IMPLEMENT, SUPPLY })
	public boolean isJdbcLogWarningsEnabledByDefault() {
		return true;
	}

	/// Append database-specific physical table types to `tableTypesList`.
	///
	/// The initial list contains configured extra physical types in configuration
	/// order. Preserve existing elements, case, order, and duplicates, and do not
	/// retain the mutable list.
	@SPI({ USE, IMPLEMENT })
	public void augmentPhysicalTableTypes(List<String> tableTypesList) {
		// nothing to do
	}

	/// Append database-specific recognized table types to `tableTypesList`.
	///
	/// The initial list starts with `TABLE`, `VIEW`, enabled synonym types, and
	/// every physical type. Preserve existing elements, case, order, and
	/// duplicates, and do not retain the mutable list.
	@SPI({ USE, IMPLEMENT })
	public void augmentRecognizedTableTypes(List<String> tableTypesList) {
		// nothing to do
	}

	/// Whether every table partition key must also occur in its primary key.
	///
	/// Return `true` only when the database requires the rule for both ordinary
	/// and temporal-history tables. This has nothing to do with window-function
	/// partitioning.
	///
	/// @since 7.1
	@Incubating
	@SPI({ USE, IMPLEMENT })
	public boolean addPartitionKeyToPrimaryKey() {
		return false;
	}

	/// Describe how this database represents nationalized character data.
	///
	/// This database SQL-type capability is independent of whether the JDBC
	/// driver correctly implements nationalized access methods. Override
	/// [#supportsNationalizedMethods()] separately when the driver cannot use
	/// those methods.
	///
	/// @return the database nationalization profile
	/// @see NationalizationSupport
	/// @see #supportsNationalizedMethods()
	///
	/// @since 8.0
	@SPI({ USE, IMPLEMENT, SUPPLY })
	public NationalizationSupport getNationalizationSupport() {
		return NationalizationSupport.EXPLICIT;
	}

	/// Determine whether the JDBC driver correctly implements the nationalized
	/// `ResultSet`, `PreparedStatement`, and `CallableStatement` methods.
	///
	/// This driver capability is independent of the database SQL-type semantics
	/// described by [#getNationalizationSupport()]. A provider should return
	/// `false` when the driver requires the ordinary character methods even if
	/// the database supports explicit nationalized types.
	///
	/// @return `true` when nationalized JDBC access methods may be used
	/// @see NationalizationSupport
	/// @see #getNationalizationSupport()
	///
	/// @since 8.0
	@SPI({ USE, IMPLEMENT, SUPPLY })
	public boolean supportsNationalizedMethods(){
		return true;
	}

	/// Supply the aggregate-column mapping, DDL, read, assignment, and write behavior
	/// for this dialect. Override this method to return a provider implementation of
	/// [AggregateSupport], or retain the standard behavior returned by the default.
	///
	/// @return this dialect's aggregate support
	/// @see AggregateSupport
	/// @since 8.0
	@SPI({ IMPLEMENT, SUPPLY })
	public AggregateSupport getAggregateSupport() {
		return AggregateSupports.standard();
	}

	/// Whether mapped aggregate structures may be represented as database
	/// user-defined types.
	///
	/// This capability is independent of [#getUserDefinedTypeExporter()], which
	/// supplies schema commands for any UDT definitions that are present.
	///
	/// @see org.hibernate.annotations.Struct
	/// @since 7.1
	@SPI({ IMPLEMENT, SUPPLY })
	public boolean supportsUserDefinedTypes() {
		return false;
	}

	/// The independent array syntax and multi-valued parameter-binding behavior
	/// of this Dialect.
	///
	/// Override this method to supply standard element-type array support, SQL
	/// array-constructor support, and the multi-valued parameter-binding strategy.
	/// Treat these as independent dimensions: do not infer syntax support from
	/// array binding, and do not infer array binding from either syntax
	/// capability. Return one immutable, non-null profile whose values remain
	/// stable for the lifetime of this Dialect.
	///
	/// @return this Dialect's stable array-support profile
	/// @since 8.0
	@SPI({ IMPLEMENT, SUPPLY })
	public ArraySupport getArraySupport() {
		return ArraySupport.NONE;
	}

	/// Render an array type name from the independent Java element name,
	/// rendered SQL element name, and optional maximum length.
	///
	/// Return null when array types are unsupported. The standard forms are
	/// `element array` and `element array[length]`.
	///
	/// @since 6.1
	@SPI({ USE, IMPLEMENT })
	public @Nullable String getArrayTypeName(
			String javaElementTypeName,
			String elementTypeName,
			@Nullable Integer maxLength) {
		if ( getArraySupport().supports( STANDARD_ARRAY ) ) {
			return maxLength == null
					? elementTypeName + " array"
					: elementTypeName + " array[" + maxLength + "]";
		}
		else {
			return null;
		}
	}

	/// Append an array literal using this Dialect's standard array support.
	///
	/// Append immediately and do not retain the appender, values, formatter, or
	/// wrapper options.
	@Override
	@SPI({ USE, IMPLEMENT })
	public void appendArrayLiteral(
			SqlAppender appender,
			Object[] literal,
			JdbcLiteralFormatter<Object> elementFormatter,
			WrapperOptions wrapperOptions) {
		if ( !getArraySupport().supports( STANDARD_ARRAY ) ) {
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

	/// Select the default [SqlTypes] code for a basic Java array or collection.
	///
	/// Return the container type Hibernate should request when no local mapping
	/// selects one. Typical choices are [SqlTypes#ARRAY], [SqlTypes#TABLE],
	/// [SqlTypes#JSON_ARRAY], [SqlTypes#XML_ARRAY], and [SqlTypes#VARBINARY].
	/// The `hibernate.type.preferred_array_jdbc_type` setting takes precedence
	/// over this Dialect default. This method selects a type code; contribute a
	/// JDBC descriptor or customize DDL rendering through their dedicated
	/// contracts.
	///
	/// @return a container type code defined by [SqlTypes]
	/// @since 8.0
	/// @see org.hibernate.cfg.MappingSettings#PREFERRED_ARRAY_JDBC_TYPE
	@SPI({ USE, IMPLEMENT, SUPPLY })
	public int getPreferredSqlTypeCodeForArray() {
		return getArraySupport().supports( STANDARD_ARRAY ) ? ARRAY : VARBINARY;
	}

	/// Select the default [SqlTypes] code for Java `boolean` mappings.
	///
	/// Return a Boolean-compatible code such as [SqlTypes#BOOLEAN],
	/// [SqlTypes#BIT], [SqlTypes#SMALLINT], [SqlTypes#TINYINT], or
	/// [SqlTypes#INTEGER]. The `hibernate.type.preferred_boolean_jdbc_type`
	/// setting takes precedence over this Dialect default. This method selects a
	/// type code; contribute a JDBC descriptor or customize DDL rendering through
	/// their dedicated contracts.
	///
	/// @return a Boolean-compatible type code defined by [SqlTypes]
	/// @since 8.0
	/// @see org.hibernate.cfg.MappingSettings#PREFERRED_BOOLEAN_JDBC_TYPE
	@SPI({ USE, IMPLEMENT, SUPPLY })
	public int getPreferredSqlTypeCodeForBoolean() {
		return Types.BOOLEAN;
	}

	/// The common-table-expression capabilities of this Dialect.
	///
	/// The returned immutable profile must be non-null and stable for the
	/// lifetime of this Dialect. Hibernate may cache and reuse it. Overrides may
	/// construct a new profile or copy [CteSupport#STANDARD]; invoking `super` is
	/// not required. This method is called only after Dialect construction, has
	/// no corresponding shutdown lifecycle, and failures propagate to the
	/// operation which obtains the profile.
	@SPI({ IMPLEMENT, SUPPLY })
	public CteSupport getCteSupport() {
		return CteSupport.STANDARD;
	}

	/// The native SQL mutation-syntax capabilities of this Dialect.
	///
	/// The returned immutable profile must be non-null and stable for the
	/// lifetime of this Dialect. Report syntax which the translator may use
	/// directly; do not report a capability merely because it can be emulated.
	/// [org.hibernate.dialect.sql.ast.spi.QueryMutationRenderingSupport] consumes
	/// this profile when selecting a native or emulated plan.
	@SPI({ IMPLEMENT, SUPPLY })
	public MutationSyntaxSupport getMutationSyntaxSupport() {
		return MutationSyntaxSupport.NONE;
	}

	/// The contexts in which this Dialect supports native multi-row `values`
	/// syntax.
	///
	/// The returned immutable profile must be non-null and stable for the
	/// lifetime of this Dialect. Report only native syntax support; translator
	/// emulation is selected separately.
	@SPI({ IMPLEMENT, SUPPLY })
	public ValuesListSupport getValuesListSupport() {
		return ValuesListSupport.INSERT_ONLY;
	}

	/// Supply the factory for this Dialect's single-use SQL AST translators.
	///
	/// Return `null` to use
	/// [org.hibernate.dialect.sql.ast.spi.StandardSqlAstTranslatorFactory]. A custom Dialect
	/// should normally return a reusable factory derived from that class and
	/// create translators derived from
	/// [org.hibernate.dialect.sql.ast.spi.AbstractSqlAstTranslator] or an
	/// appropriate supported family base. The factory must not retain
	/// translation requests, and every translator it returns is used for one
	/// translation.
	///
	/// @see SqlAstTranslatorFactory
	/// @see JdbcEnvironment#getSqlAstTranslatorFactory()
	@SPI({ IMPLEMENT, SUPPLY })
	public SqlAstTranslatorFactory getSqlAstTranslatorFactory() {
		return null;
	}

	/// Supply the strategy which resolves column sizes for mapped Java and JDBC
	/// types.
	///
	/// Return the standard strategy unless this database changes column-size
	/// resolution. For focused deviations, construct a stable
	/// [StandardSizeStrategy] with this Dialect, override the affected cases,
	/// and delegate all other cases to the standard implementation. The result
	/// must be non-null and stable for the lifetime of this Dialect.
	///
	/// @since 8.0
	@SPI({ IMPLEMENT, SUPPLY })
	public SizeStrategy getSizeStrategy() {
		return sizeStrategy;
	}

	/// Supplies this Dialect's resolved type-sizing limits and defaults.
	///
	/// Return [TypeSizingProfile#STANDARD] when no dimension differs. Construct
	/// one stable immutable profile after version and server configuration are
	/// known. A family Dialect may copy its superclass profile, but must set every
	/// effective dimension which formerly changed through scalar-method
	/// delegation because profile builder setters do not cascade.
	///
	/// @since 8.0
	@SPI({ IMPLEMENT, SUPPLY })
	public TypeSizingProfile getTypeSizingProfile() {
		return TypeSizingProfile.STANDARD;
	}

	/// Creates the JDBC mutation operation for an optional-table update.
	///
	/// Override this factory when the database needs an operation other than the
	/// standard update-then-insert behavior. The request contains the semantic
	/// update and bootstrap services; implementations must return an operation
	/// for the same [OptionalTableUpdateOperationRequest#mutationTarget] and must
	/// not retain the request.
	///
	/// @return a non-null operation for the supplied optional-table update
	/// @see OptionalTableUpdateOperationRequest
	/// @see MutationOperation
	@SPI({ USE, IMPLEMENT, SUPPLY })
	public MutationOperation createOptionalTableUpdateOperation(
			OptionalTableUpdateOperationRequest request) {
		return new OptionalTableUpdateOperation( request.mutationTarget(), request.update() );
	}

	/// Supply the native parameter-marker strategy selected for this database
	/// and JDBC driver.
	///
	/// Return `null` to use JDBC-standard `?` markers. A non-null result must remain
	/// stable after bootstrap and must not retain JDBC types passed to it.
	///
	/// @return the native strategy, or `null` for JDBC-standard markers
	/// @see ParameterMarkerStrategy
	@SPI({ USE, IMPLEMENT, SUPPLY })
	public ParameterMarkerStrategy getNativeParameterMarkerStrategy() {
		return null;
	}

	/// Supplies the preferred default ordinality-column name for a set-returning
	/// function.
	///
	/// Return a stable, unqualified SQL column name, or `null` when the database
	/// does not define a preferred name. Hibernate uses `i` as the fallback and
	/// applies collision suffixing where necessary. A null result does not mean
	/// that set-returning functions are unsupported.
	///
	/// @return the preferred unqualified name, or `null` to use Hibernate's
	/// fallback
	/// @since 8.0
	@SPI({ IMPLEMENT, SUPPLY })
	public @Nullable String getDefaultOrdinalityColumnName() {
		return null;
	}

	/// Determine whether this database marks the current transaction for
	/// rollback after the given statement failure.
	///
	/// Inspect the original database exception when the policy depends on its
	/// error code, SQL state, or subtype. This decision is independent of how
	/// Hibernate converts the exception.
	///
	/// @param sqlException the original database exception
	/// @return `true` when the failed statement invalidates the transaction
	/// @since 8.0
	@SPI({ IMPLEMENT, SUPPLY })
	public boolean causesRollback(SQLException sqlException) {
		return false;
	}

	/// Supplies the complete temporal-table DDL and historical-query strategy
	/// for this Dialect.
	///
	/// Return an immutable strategy and use the supported request values without
	/// retaining them. Prefer [TemporalTableSupports#standard(int, int, boolean)]
	/// when only the column type, precision, and table-check capability differ.
	///
	/// @return the non-null temporal-table support strategy
	/// @since 8.0
	/// @see TemporalTableSupport
	@Incubating
	@SPI({ IMPLEMENT, SUPPLY })
	public TemporalTableSupport getTemporalTableSupport() {
		return TemporalTableSupports.standard(
				SqlTypes.TIMESTAMP,
				getTypeSizingProfile().defaultTimestampPrecision(),
				getCheckConstraintSupport().supports(
						org.hibernate.dialect.constraint.spi.CheckConstraintPlacement.TABLE
				)
		);
	}

	/// Append a datetime literal for a `java.time` value.
	///
	/// Preserve the value's offset only when the Dialect's temporal-value
	/// semantics permit it. Append immediately and retain no invocation state.
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
				appender.appendSql( JDBC_ESCAPE_START_DATE );
				appendAsDate( appender, temporalAccessor );
				appender.appendSql( JDBC_ESCAPE_END );
				break;
			case TIME:
				appender.appendSql( JDBC_ESCAPE_START_TIME );
				appendAsTime( appender, temporalAccessor, getTemporalValueSemantics().supportsLiteralOffset(), jdbcTimeZone );
				appender.appendSql( JDBC_ESCAPE_END );
				break;
			case TIMESTAMP:
				appender.appendSql( JDBC_ESCAPE_START_TIMESTAMP );
				appendAsTimestampWithNanos( appender, temporalAccessor, getTemporalValueSemantics().supportsLiteralOffset(), jdbcTimeZone );
				appender.appendSql( JDBC_ESCAPE_END );
				break;
			default:
				throw new IllegalArgumentException();
		}
	}

	/// Append a datetime literal for a legacy [Date] value.
	///
	/// Apply the supplied JDBC time zone where required, append immediately,
	/// and retain no invocation state.
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

	/// Append a datetime literal for a [Calendar] value.
	///
	/// Preserve Calendar-specific time-zone and millisecond semantics, append
	/// immediately, and retain no invocation state.
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

	/// Select the database's support profile for SQL `with time zone` types.
	///
	/// Override this method to report whether the database preserves the
	/// original zone, normalizes values to UTC, or has no native support. This
	/// value participates in mapping and JDBC type selection; it does not
	/// control temporal-literal syntax.
	///
	/// @return the database time-zone type support profile
	/// @since 8.0
	/// @see TimeZoneSupport
	@SPI({ IMPLEMENT, SUPPLY })
	public TimeZoneSupport getTimeZoneSupport() {
		return TimeZoneSupport.NONE;
	}

	/// Supply the database's row-locator expression, JDBC type, and optional
	/// physical-column declaration policy.
	///
	/// @return the stable non-null row-id strategy
	/// @since 8.0
	/// @see RowIdSupport
	@SPI({ IMPLEMENT, SUPPLY })
	public RowIdSupport getRowIdSupport() {
		return RowIdSupports.none();
	}

	/// The qualifier form accepted for target-column references in update and
	/// delete statements.
	///
	/// Translators use this value when rendering assignments and predicates. A
	/// custom Dialect should return the least permissive form required by the
	/// database, not a form used only by an emulation.
	///
	/// @return the non-null target-column qualifier support
	/// @see DmlTargetColumnQualifierSupport
	@SPI({ USE, IMPLEMENT, SUPPLY })
	public DmlTargetColumnQualifierSupport getDmlTargetColumnQualifierSupport() {
		return DmlTargetColumnQualifierSupport.NONE;
	}

	/// The database's support for recognizing primary-key functional dependency
	/// in `group by` and `order by` clauses.
	///
	/// This profile controls when Hibernate may omit functionally dependent
	/// columns. Report only behavior guaranteed by the database across the
	/// Dialect's supported versions.
	///
	/// @return a non-null immutable capability profile
	/// @since 8.0
	/// @see FunctionalDependencyAnalysisSupport
	@SPI({ IMPLEMENT, SUPPLY })
	public FunctionalDependencyAnalysisSupport getFunctionalDependencyAnalysisSupport() {
		return FunctionalDependencyAnalysisSupport.NONE;
	}

	/// Supply the JDBC strategy for binding an untyped Java `null`.
	///
	/// @return the non-null binding strategy
	/// @since 8.0
	/// @see ObjectNullBindingStrategy
	@SPI({ IMPLEMENT, SUPPLY })
	public ObjectNullBindingStrategy getObjectNullBindingStrategy() {
		return ObjectNullBindingStrategy.SET_NULL;
	}

	/// Whether schema-management DDL commands require a short delay before
	/// execution to accommodate database-side propagation.
	///
	/// @deprecated This temporary schema-management workaround will be removed
	/// once affected databases no longer require it.
	@Incubating
	@Deprecated(forRemoval = true)
	public boolean throttleDdl() {
		return false;
	}

	/// Whether this database supports native aggregate `filter (where ...)`
	/// syntax.
	///
	/// Return a stable answer for this Dialect's lifetime. A false result directs
	/// aggregate rendering to use the existing case-expression emulation; it does
	/// not mean that filtered aggregates are unsupported.
	///
	/// @since 8.0
	@SPI({ IMPLEMENT, SUPPLY })
	public boolean supportsFilterClause() {
		// By default, we report false because not many dialects support this
		return false;
	}

	/// Whether this database supports ANSI `cross join` syntax.
	///
	/// Return a stable answer for this Dialect's lifetime. A false result directs
	/// SQL AST rendering to use its unqualified join with a true predicate and is
	/// independent of the database's [#getSingleRowTableSupport] profile.
	///
	/// @since 8.0
	@SPI({ IMPLEMENT, SUPPLY })
	public boolean supportsCrossJoin() {
		return true;
	}

}
