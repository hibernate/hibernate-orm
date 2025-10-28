/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cfg;

import org.hibernate.Incubating;
import org.hibernate.SessionFactory;
import org.hibernate.annotations.ListIndexBase;
import org.hibernate.annotations.Nationalized;
import org.hibernate.boot.jaxb.hbm.transform.UnsupportedFeatureHandling;
import org.hibernate.boot.registry.selector.spi.StrategySelector;
import org.hibernate.dialect.Dialect;
import org.hibernate.id.enhanced.ImplicitDatabaseObjectNamingStrategy;
import org.hibernate.id.enhanced.StandardOptimizerDescriptor;
import org.hibernate.metamodel.CollectionClassification;
import org.hibernate.type.WrapperArrayHandling;
import org.hibernate.type.descriptor.jdbc.JavaTimeJdbcType;
import org.hibernate.type.format.FormatMapper;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;

/**
 * @author Steve Ebersole
 */
public interface MappingSettings {
	/**
	 * A default database catalog name to use for unqualified database
	 * object (table, sequence, ...) names
	 *
	 * @see org.hibernate.boot.MetadataBuilder#applyImplicitCatalogName
	 */
	String DEFAULT_CATALOG = "hibernate.default_catalog";

	/**
	 * A default database schema (owner) name to use for unqualified
	 * database object (table, sequence, ...) names
	 *
	 * @see org.hibernate.boot.MetadataBuilder#applyImplicitSchemaName
	 */
	String DEFAULT_SCHEMA = "hibernate.default_schema";

	/**
	 * Setting that indicates whether to build the JPA types, either:<ul>
	 *     <li>
	 *         {@code enabled} &mdash; do the build
	 *     </li>
	 *     <li>
	 *         {@code disabled} &mdash; do not do the build
	 *     </li>
	 *     <li>
	 *         {@code ignoreUnsupported} &mdash; do the build, but ignore any
	 *         non-JPA features that would otherwise result in a failure.
	 *     </li>
	 * </ul>
	 *
	 * @settingDefault {@code ignoreUnsupported}
	 */
	String JPA_METAMODEL_POPULATION = "hibernate.jpa.metamodel.population";

	/**
	 * Setting that controls whether we seek out JPA static metamodel classes
	 * and populate them, either:<ul>
	 *     <li>
	 *         {@code enabled} &mdash; do populate the static metamodel,
	 *     </li>
	 *     <li>
	 *         {@code disabled} &mdash; do not populate the static metamodel, or
	 *     </li>
	 *     <li>
	 *         {@code skipUnsupported} &mdash; do populate the static metamodel,
	 *         but ignore any non-JPA features that would otherwise result in
	 *         the process failing.
	 *     </li>
	 * </ul>
	 *
	 * @settingDefault {@code skipUnsupported}
	 */
	String STATIC_METAMODEL_POPULATION = "hibernate.jpa.static_metamodel.population";

	/**
	 * When enabled, all database identifiers are quoted.
	 * <p>
	 * Corollary to the JPA {@code <delimited-identifiers/>} element within
	 * the {@code orm.xml} {@code <persistence-unit-defaults/>} element, but
	 * offered as a global flag.
	 *
	 * @settingDefault {@code false}
	 */
	String GLOBALLY_QUOTED_IDENTIFIERS = "hibernate.globally_quoted_identifiers";

	/**
	 * Controls whether column-definitions ({@link Column#columnDefinition},
	 * {@link JoinColumn#columnDefinition}, etc.) should be auto-quoted as part of
	 * {@linkplain #GLOBALLY_QUOTED_IDENTIFIERS global quoting}.
	 * <p>
	 * When {@linkplain #GLOBALLY_QUOTED_IDENTIFIERS global quoting} is enabled, JPA
	 * <a href="https://jakarta.ee/specifications/persistence/3.1/jakarta-persistence-spec-3.1.html#a988">states</a>
	 * that column-definitions are subject to quoting.  However, this can lead to problems
	 * with definitions such as {@code @Column(..., columnDefinition="INTEGER DEFAULT 20")}.
	 *
	 * @settingDefault {@code false} to avoid the potential problems quoting non-trivial
	 * column-definitions.
	 */
	String GLOBALLY_QUOTED_IDENTIFIERS_SKIP_COLUMN_DEFINITIONS = "hibernate.globally_quoted_identifiers_skip_column_definitions";

	/**
	 * Specifies whether to automatically quote any names that are deemed keywords
	 * on the underlying database.
	 *
	 * @settingDefault {@code false} - auto-quoting of SQL keywords is disabled by default.
	 *
	 * @since 5.0
	 */
	String KEYWORD_AUTO_QUOTING_ENABLED = "hibernate.auto_quote_keyword";

	/**
	 * Specifies an {@linkplain org.hibernate.id.enhanced.Optimizer optimizer}
	 * which should be used when a generator specifies an {@code allocationSize}
	 * and no optimizer is not explicitly specified, either:
	 * <ul>
	 * <li>a class implementing {@link org.hibernate.id.enhanced.Optimizer},
	 * <li>the name of a class implementing {@code Optimizer}, or</li>
	 * <li>an {@linkplain StandardOptimizerDescriptor optimizer short name}.
	 * </ul>
	 *
	 * @settingDefault {@link StandardOptimizerDescriptor#POOLED}
	 *
	 * @see org.hibernate.id.enhanced.PooledOptimizer
	 * @see org.hibernate.id.enhanced.PooledLoOptimizer
	 * @see org.hibernate.id.enhanced.HiLoOptimizer
	 */
	String PREFERRED_POOLED_OPTIMIZER = "hibernate.id.optimizer.pooled.preferred";

	/**
	 * Determines if the identifier value stored in the database table backing a
	 * {@linkplain jakarta.persistence.TableGenerator table generator} is the last
	 * value returned by the identifier generator, or the next value to be returned.
	 *
	 * @settingDefault The value stored in the database table is the last generated value
	 *
	 * @since 5.3
	 */
	String TABLE_GENERATOR_STORE_LAST_USED = "hibernate.id.generator.stored_last_used";

	/**
	 * This setting defines the {@link org.hibernate.id.SequenceMismatchStrategy} used
	 * when Hibernate detects a mismatch between a sequence configuration in an entity
	 * mapping and its database sequence object counterpart.
	 * <p>
	 * Possible values are {@link org.hibernate.id.SequenceMismatchStrategy#EXCEPTION},
	 * {@link org.hibernate.id.SequenceMismatchStrategy#LOG},
	 * {@link org.hibernate.id.SequenceMismatchStrategy#FIX}
	 * and {@link org.hibernate.id.SequenceMismatchStrategy#NONE}.
	 *
	 * @settingDefault {@link org.hibernate.id.SequenceMismatchStrategy#EXCEPTION},  meaning
	 * that an exception is thrown when such a conflict is detected.
	 *
	 * @since 5.4
	 */
	String SEQUENCE_INCREMENT_SIZE_MISMATCH_STRATEGY = "hibernate.id.sequence.increment_size_mismatch_strategy";

	/**
	 * Specifies the preferred JDBC type for storing boolean values.
	 * <p>
	 * Can be overridden locally using {@link org.hibernate.annotations.JdbcType},
	 * {@link org.hibernate.annotations.JdbcTypeCode}, and friends.
	 * <p>
	 * Can also specify the name of the {@link org.hibernate.type.SqlTypes} constant
	 * field, for example, {@code hibernate.type.preferred_boolean_jdbc_type=BIT}.
	 *
	 * @settingDefault {@linkplain Dialect#getPreferredSqlTypeCodeForBoolean dialect-specific type code}
	 *
	 * @since 6.0
	 */
	@Incubating
	String PREFERRED_BOOLEAN_JDBC_TYPE = "hibernate.type.preferred_boolean_jdbc_type";

	/**
	 * The preferred JDBC type to use for storing {@link java.util.UUID} values.
	 * <p>
	 * Can be overridden locally using {@link org.hibernate.annotations.JdbcType},
	 * {@link org.hibernate.annotations.JdbcTypeCode}, and friends.
	 * <p>
	 * Can also specify the name of the {@link org.hibernate.type.SqlTypes} constant
	 * field, for example, {@code hibernate.type.preferred_uuid_jdbc_type=CHAR}.
	 *
	 * @settingDefault {@link org.hibernate.type.SqlTypes#UUID}.
	 *
	 * @since 6.0
	 */
	@Incubating
	String PREFERRED_UUID_JDBC_TYPE = "hibernate.type.preferred_uuid_jdbc_type";

	/**
	 * The preferred JDBC type to use for storing {@link java.time.Duration} values.
	 * <p>
	 * Can be overridden locally using {@link org.hibernate.annotations.JdbcType},
	 * {@link org.hibernate.annotations.JdbcTypeCode}, and friends.
	 * <p>
	 * Can also specify the name of the {@link org.hibernate.type.SqlTypes} constant
	 * field, for example, {@code hibernate.type.preferred_duration_jdbc_type=INTERVAL_SECOND}.
	 *
	 * @settingDefault {@link org.hibernate.type.SqlTypes#NUMERIC}
	 *
	 * @since 6.0
	 */
	@Incubating
	String PREFERRED_DURATION_JDBC_TYPE = "hibernate.type.preferred_duration_jdbc_type";

	/**
	 * Specifies the preferred JDBC type for storing {@link java.time.Instant} values.
	 * <p>
	 * Can be overridden locally using {@link org.hibernate.annotations.JdbcType},
	 * {@link org.hibernate.annotations.JdbcTypeCode}, and friends.
	 * <p>
	 * Can also specify the name of the {@link org.hibernate.type.SqlTypes} constant
	 * field, for example, {@code hibernate.type.preferred_instant_jdbc_type=TIMESTAMP}
	 * or {@code hibernate.type.preferred_instant_jdbc_type=INSTANT}.
	 *
	 * @settingDefault {@link org.hibernate.type.SqlTypes#TIMESTAMP_UTC}.
	 *
	 * @since 6.0
	 */
	@Incubating
	String PREFERRED_INSTANT_JDBC_TYPE = "hibernate.type.preferred_instant_jdbc_type";

	/**
	 * Indicates whether to use {@linkplain java.time Java Time} references at the JDBC
	 * boundary for binding and extracting temporal values to/from the database using
	 * the support added in JDBC 4.2 via {@linkplain java.sql.PreparedStatement#setObject(int, Object, int)}
	 * and {@linkplain java.sql.ResultSet#getObject(int, Class)}.
	 * <p/>
	 * Used to set the value across the entire system as opposed to scattered, individual
	 * {@linkplain org.hibernate.annotations.JdbcTypeCode} and {@linkplain org.hibernate.annotations.JdbcType}
	 * naming specific {@linkplain JavaTimeJdbcType} implementations.
	 *
	 * @implNote JDBC 4.2 does not define support for {@linkplain java.time.Instant}, so
	 * {@linkplain java.time.Instant} is not included in this.  Some drivers do implement support for this
	 * even though not explicitly part of the JDBC specification.  To use direct binding and extracting of
	 * {@linkplain java.time.Instant} references, use {@code hibernate.type.preferred_instant_jdbc_type=INSTANT}.
	 * See {@linkplain #PREFERRED_INSTANT_JDBC_TYPE}, {@linkplain org.hibernate.type.SqlTypes#INSTANT} and
	 * {@linkplain org.hibernate.type.descriptor.jdbc.InstantJdbcType}.
	 *
	 * @settingDefault false
	 *
	 * @since 6.5
	 */
	@Incubating
	String JAVA_TIME_USE_DIRECT_JDBC = "hibernate.type.java_time_use_direct_jdbc";

	/**
	 * Indicates that named SQL {@code enum} types should be used by default instead
	 * of {@code varchar} on databases which support named enum types.
	 * <p>
	 * A named enum type is declared in DDL using {@code create type ... as enum} or
	 * {@code create type ... as domain}.
	 * <p>
	 * This configuration property is used to specify a global preference, as an
	 * alternative to the use of
	 * {@link org.hibernate.annotations.JdbcTypeCode @JdbcTypeCode(SqlTypes.NAMED_ENUM)}
	 * at the field or property level.
	 *
	 * @settingDefault false
	 *
	 * @since 6.5
	 *
	 * @see org.hibernate.type.SqlTypes#NAMED_ENUM
	 * @see org.hibernate.dialect.type.PostgreSQLEnumJdbcType
	 * @see org.hibernate.dialect.type.OracleEnumJdbcType
	 */
	@Incubating
	String PREFER_NATIVE_ENUM_TYPES = "hibernate.type.prefer_native_enum_types";

	/**
	 * Specifies the preferred JDBC type for storing plural i.e. array/collection values.
	 * <p>
	 * Can be overridden locally using {@link org.hibernate.annotations.JdbcType},
	 * {@link org.hibernate.annotations.JdbcTypeCode}, and friends.
	 * <p>
	 * Can also specify the name of the {@link org.hibernate.type.SqlTypes} constant
	 * field, for example, {@code hibernate.type.preferred_array_jdbc_type=ARRAY}
	 * or {@code hibernate.type.preferred_array_jdbc_type=TABLE}.
	 *
	 * @settingDefault {@link Dialect#getPreferredSqlTypeCodeForArray()}.
	 *
	 * @see org.hibernate.type.SqlTypes#ARRAY
	 * @see org.hibernate.type.SqlTypes#TABLE
	 *
	 * @since 6.6
	 */
	@Incubating
	String PREFERRED_ARRAY_JDBC_TYPE = "hibernate.type.preferred_array_jdbc_type";

	/**
	 * Specifies a {@link org.hibernate.type.format.FormatMapper} used for JSON
	 * serialization and deserialization, either:
	 * <ul>
	 *     <li>an instance of {@code FormatMapper},
	 *     <li>a {@link Class} representing a class that implements {@code FormatMapper},
	 *     <li>the name of a class that implements {@code FormatMapper}, or
	 *     <li>one of the shorthand constants {@code jackson} or {@code jsonb}.
	 * </ul>
	 * <p>
	 * By default, the first of the possible providers that is available at runtime is
	 * used, according to the listing order.
	 *
	 * @since 6.0
	 * @see org.hibernate.boot.SessionFactoryBuilder#applyJsonFormatMapper(FormatMapper)
	 */
	@Incubating
	String JSON_FORMAT_MAPPER = "hibernate.type.json_format_mapper";

	/**
	 * Specifies a {@link org.hibernate.type.format.FormatMapper} used for XML
	 * serialization and deserialization, either:
	 * <ul>
	 *     <li>an instance of {@code FormatMapper},
	 *     <li>a {@link Class} representing a class that implements {@code FormatMapper},
	 *     <li>the name of a class that implements {@code FormatMapper}, or
	 *     <li>one of the shorthand constants {@code jackson} or {@code jaxb}.
	 * </ul>
	 * <p>
	 * By default, the first of the possible providers that is available at runtime is
	 * used, according to the listing order.
	 *
	 * @since 6.0.1
	 * @see org.hibernate.boot.SessionFactoryBuilder#applyXmlFormatMapper(FormatMapper)
	 */
	@Incubating
	String XML_FORMAT_MAPPER = "hibernate.type.xml_format_mapper";

	/**
	 * Specifies whether to use the legacy provider-specific and non-portable XML format for
	 * collections and byte arrays for XML serialization/deserialization.
	 * <p>
	 * {@code false} by default. This property only exists for backwards compatibility.
	 *
	 * @since 7.0
	 */
	@Incubating
	String XML_FORMAT_MAPPER_LEGACY_FORMAT = "hibernate.type.xml_format_mapper.legacy_format";

	/**
	 * Configurable control over how to handle {@code Byte[]} and {@code Character[]} types
	 * encountered in the application domain model.  Allowable semantics are defined by
	 * {@link WrapperArrayHandling}.  Accepted values include:<ol>
	 *     <li>{@link WrapperArrayHandling} instance</li>
	 *     <li>case-insensitive name of a {@link WrapperArrayHandling} instance (e.g. {@code allow})</li>
	 * </ol>
	 *
	 * @since 6.2
	 */
	@Incubating
	String WRAPPER_ARRAY_HANDLING = "hibernate.type.wrapper_array_handling";

	/**
	 * Specifies the default strategy for storage of the timezone information for the zoned
	 * datetime types {@link java.time.OffsetDateTime} and {@link java.time.ZonedDateTime}.
	 * The possible options for this setting are enumerated by
	 * {@link org.hibernate.annotations.TimeZoneStorageType}.
	 *
	 * @apiNote For backward compatibility with older versions of Hibernate, set this property
	 * to {@link org.hibernate.annotations.TimeZoneStorageType#NORMALIZE NORMALIZE}.
	 *
	 * @settingDefault {@link org.hibernate.annotations.TimeZoneStorageType#DEFAULT DEFAULT},
	 * which guarantees that the {@linkplain java.time.OffsetDateTime#toInstant() instant}
	 * represented by a zoned datetime type is preserved by a round trip to the database.
	 * It does <em>not</em> guarantee that the time zone or offset is preserved.
	 *
	 * @see org.hibernate.annotations.TimeZoneStorageType
	 * @see org.hibernate.annotations.TimeZoneStorage
	 *
	 * @since 6.0
	 */
	String TIMEZONE_DEFAULT_STORAGE = "hibernate.timezone.default_storage";

	/**
	 * Used to specify the {@link org.hibernate.boot.model.naming.ImplicitNamingStrategy}
	 * class to use. The following shortcut names are defined for this setting:
	 * <ul>
	 *     <li>{@code "default"} and {@code "jpa"} are abbreviations for
	 *     {@link org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl}
	 *     <li>{@code "legacy-jpa"} is an abbreviation for
	 *     {@link org.hibernate.boot.model.naming.ImplicitNamingStrategyLegacyJpaImpl}
	 *     <li>{@code "legacy-hbm"} is an abbreviation for
	 *     {@link org.hibernate.boot.model.naming.ImplicitNamingStrategyLegacyHbmImpl}
	 *     <li>{@code "component-path"} is an abbreviation for
	 *     {@link org.hibernate.boot.model.naming.ImplicitNamingStrategyComponentPathImpl}
	 * </ul>
	 *
	 * @settingDefault {@code "default"}
	 *
	 * @see org.hibernate.boot.MetadataBuilder#applyImplicitNamingStrategy
	 *
	 * @since 5.0
	 */
	String IMPLICIT_NAMING_STRATEGY = "hibernate.implicit_naming_strategy";

	/**
	 * Specifies the {@link org.hibernate.boot.model.naming.PhysicalNamingStrategy} to use.
	 *
	 * @settingDefault {@link org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl},
	 * in which case physical names are taken to be identical to logical names.
	 *
	 * @see org.hibernate.boot.MetadataBuilder#applyPhysicalNamingStrategy
	 *
	 * @since 5.0
	 */
	String PHYSICAL_NAMING_STRATEGY = "hibernate.physical_naming_strategy";

	/**
	 * An implicit naming strategy for database structures (tables, sequences) related
	 * to identifier generators.
	 * <p>
	 * Resolution uses the {@link org.hibernate.boot.registry.selector.spi.StrategySelector}
	 * service and accepts any of the forms discussed on
	 * {@link StrategySelector#resolveDefaultableStrategy(Class, Object, java.util.concurrent.Callable)}.
	 * <p>
	 * The recognized short names being:<ul>
	 *     <li>{@value org.hibernate.id.enhanced.SingleNamingStrategy#STRATEGY_NAME}</li>
	 *     <li>{@value org.hibernate.id.enhanced.LegacyNamingStrategy#STRATEGY_NAME}</li>
	 *     <li>{@value org.hibernate.id.enhanced.StandardNamingStrategy#STRATEGY_NAME}</li>
	 * </ul>
	 *
	 * @settingDefault {@link org.hibernate.id.enhanced.StandardNamingStrategy}
	 *
	 * @since 6
	 *
	 * @see ImplicitDatabaseObjectNamingStrategy
	 */
	@Incubating
	String ID_DB_STRUCTURE_NAMING_STRATEGY = "hibernate.id.db_structure_naming_strategy";

	/**
	 * Used to specify the {@link org.hibernate.boot.model.relational.ColumnOrderingStrategy}
	 * class to use. The following shortcut names are defined for this setting:
	 * <ul>
	 *     <li>{@code "default"} is an abbreviations for
	 *     {@link org.hibernate.boot.model.relational.ColumnOrderingStrategyStandard}
	 *     <li>{@code "legacy"} is an abbreviation for
	 *     {@link org.hibernate.boot.model.relational.ColumnOrderingStrategyLegacy}
	 * </ul>
	 *
	 * @settingDefault {@code "default"}
	 *
	 * @see org.hibernate.boot.MetadataBuilder#applyColumnOrderingStrategy
	 *
	 * @since 6.2
	 */
	String COLUMN_ORDERING_STRATEGY = "hibernate.column_ordering_strategy";

	/**
	 * Whether XML mappings should be processed.
	 *
	 * @apiNote This is a performance optimization appropriate when mapping details
	 * are defined exclusively using annotations.
	 *
	 * @settingDefault {@code true} - XML mappings are processed
	 *
	 * @since 5.4.1
	 */
	String XML_MAPPING_ENABLED = "hibernate.xml_mapping_enabled";

	/**
	 * Specifies the {@link CollectionClassification} to use for a plural attribute
	 * typed as {@link java.util.List} with no explicit list index details
	 * ({@link OrderColumn}, {@link ListIndexBase}, etc.).
	 * <p>
	 * Accepts any of:
	 * <ul>
	 *     <li>an instance of {@code CollectionClassification}
	 *     <li>the (case-insensitive) name of a {@code CollectionClassification} (list e.g.)
	 *     <li>a {@link Class} representing either {@link java.util.List} or {@link java.util.Collection}
	 * </ul>
	 *
	 * @settingDefault {@link CollectionClassification#BAG}
	 *
	 * @since 6.0
	 *
	 * @see org.hibernate.annotations.Bag
	 */
	String DEFAULT_LIST_SEMANTICS = "hibernate.mapping.default_list_semantics";

	/**
	 * Whether XML should be validated against their schema as Hibernate reads them.
	 *
	 * @settingDefault {@code true}
	 *
	 * @since 6.1
	 */
	String VALIDATE_XML = "hibernate.validate_xml";

	/**
	 * Enables processing {@code hbm.xml} mappings by transforming them to {@code mapping.xml} and using
	 * that processor.
	 *
	 * @settingDefault {@code false} (opt-in).
	 *
	 * @since 6.1
	 */
	String TRANSFORM_HBM_XML = "hibernate.transform_hbm_xml.enabled";

	/**
	 * How features in a {@code hbm.xml} file which are not supported for transformation
	 * should be handled.  Valid values are defined by {@link UnsupportedFeatureHandling}
	 *
	 * @settingDefault {@link UnsupportedFeatureHandling#ERROR}
	 *
	 * @since 6.1
	 */
	String TRANSFORM_HBM_XML_FEATURE_HANDLING = "hibernate.transform_hbm_xml.unsupported_feature_handling";

	/**
	 * Specifies that Hibernate should always restrict by discriminator values in
	 * SQL {@code select} statements, even when querying the root entity of an
	 * entity inheritance hierarchy.
	 * <p>
	 * By default, Hibernate only restricts by discriminator values when querying
	 * a subtype, or when the root entity is explicitly annotated
	 * {@link org.hibernate.annotations.DiscriminatorOptions#force
	 * DiscriminatorOptions(force=true)}.
	 *
	 * @see org.hibernate.boot.MetadataBuilder#enableImplicitForcingOfDiscriminatorsInSelect
	 * @see org.hibernate.annotations.DiscriminatorOptions#force
	 *
	 * @settingDefault {@code false}
	 */
	String FORCE_DISCRIMINATOR_IN_SELECTS_BY_DEFAULT = "hibernate.discriminator.force_in_select";

	/**
	 * Controls whether Hibernate should infer a discriminator for entity hierarchies
	 * defined with joined inheritance.
	 * <p>
	 * Hibernate does not need a discriminator with joined inheritance.  Therefore, its legacy
	 * behavior is to not infer a discriminator.  However, some JPA providers do require
	 * discriminators with joined inheritance, so in the interest of portability this option
	 * has been added to Hibernate.  When enabled ({@code true}), Hibernate will treat the absence
	 * of discriminator metadata as an indication to use the JPA defined defaults for discriminators.
	 *
	 * @implNote See Hibernate Jira issue HHH-6911 for additional background info.
	 *
	 * @settingDefault {@code false}
	 *
	 * @see org.hibernate.boot.MetadataBuilder#enableImplicitDiscriminatorsForJoinedSubclassSupport
	 * @see #IGNORE_EXPLICIT_DISCRIMINATOR_COLUMNS_FOR_JOINED_SUBCLASS
	 */
	String IMPLICIT_DISCRIMINATOR_COLUMNS_FOR_JOINED_SUBCLASS = "hibernate.discriminator.implicit_for_joined";

	/**
	 * Controls whether Hibernate should ignore explicit discriminator metadata with
	 * joined inheritance.
	 * <p>
	 * Hibernate does not need a discriminator with joined inheritance.  Historically
	 * it simply ignored discriminator metadata.  When enabled ({@code true}), any
	 * discriminator metadata ({@link DiscriminatorColumn}, e.g.) is ignored allowing
	 * for backwards compatibility.
	 *
	 * @implNote See Hibernate Jira issue HHH-6911 for additional background info.
	 *
	 * @settingDefault {@code false}
	 *
	 * @see org.hibernate.boot.MetadataBuilder#enableExplicitDiscriminatorsForJoinedSubclassSupport
	 * @see #IMPLICIT_DISCRIMINATOR_COLUMNS_FOR_JOINED_SUBCLASS
	 */
	String IGNORE_EXPLICIT_DISCRIMINATOR_COLUMNS_FOR_JOINED_SUBCLASS = "hibernate.discriminator.ignore_explicit_for_joined";

	/**
	 * By default, Hibernate maps character data represented by {@link String}s and
	 * {@link java.sql.Clob}s to the JDBC types {@link java.sql.Types#VARCHAR} and
	 * {@link java.sql.Types#CLOB}. This setting, when enabled, turns on the use of
	 * explicit nationalized character support for mappings involving character
	 * data, specifying that the JDBC types {@link java.sql.Types#NVARCHAR} and
	 * {@link java.sql.Types#NCLOB} should be used instead.
	 * <p>
	 * This setting is relevant for use with databases with
	 * {@linkplain org.hibernate.dialect.NationalizationSupport#EXPLICIT explicit
	 * nationalization support}, and it is not needed for databases whose native
	 * {@code varchar} and {@code clob} types support Unicode data. (If you're not
	 * sure how your database handles Unicode, check out the implementation of
	 * {@link Dialect#getNationalizationSupport()} for its
	 * SQL dialect.)
	 * <p>
	 * Enabling this setting has two effects:
	 * <ol>
	 *     <li>when interacting with JDBC, Hibernate uses operations like
	 *         {@link java.sql.PreparedStatement#setNString(int, String)}
	 *         {@link java.sql.PreparedStatement#setNClob(int, java.sql.NClob)}
	 *         to pass character data, and
	 *     <li>when generating DDL, the schema export tool uses {@code nchar},
	 *         {@code nvarchar}, or {@code nclob} as the generated column
	 *         type when no column type is explicitly specified using
	 *         {@link jakarta.persistence.Column#columnDefinition()}.
	 * </ol>
	 *
	 * @apiNote This is a global setting applying to all mappings associated with a given
	 * {@link SessionFactory}. The {@link Nationalized} annotation may be used to
	 * selectively enable nationalized character support for specific columns.
	 *
	 * @settingDefault {@code false} (disabled)
	 *
	 * @see org.hibernate.boot.MetadataBuilder#enableGlobalNationalizedCharacterDataSupport(boolean)
	 * @see Dialect#getNationalizationSupport
	 * @see Nationalized
	 */
	String USE_NATIONALIZED_CHARACTER_DATA = "hibernate.use_nationalized_character_data";

}
