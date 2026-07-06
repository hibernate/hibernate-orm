/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.pipeline.internal;

import java.util.List;

import jakarta.annotation.Nonnull;
import org.hibernate.Incubating;
import org.hibernate.boot.spi.BasicTypeRegistration;
import org.hibernate.boot.spi.GlobalMappingDefaults;
import org.hibernate.boot.mapping.internal.context.MappingPreferences;
import org.hibernate.type.TimeZoneStorageStrategy;
import org.hibernate.boot.model.naming.ImplicitNamingStrategy;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.boot.model.relational.ColumnOrderingStrategy;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.collection.internal.StandardCollectionSemanticsResolver;
import org.hibernate.collection.spi.CollectionSemanticsResolver;
import org.hibernate.dialect.TimeZoneSupport;
import org.hibernate.type.WrapperArrayHandling;
import org.hibernate.type.spi.TypeConfiguration;
import org.hibernate.usertype.CompositeUserType;

import jakarta.persistence.SharedCacheMode;

/// Describes the resolved options used while resolving mapping details.
///
/// @since 9.0
/// @author Steve Ebersole
public interface MappingResolutionOptions {
	/// Access to the [StandardServiceRegistry].
	StandardServiceRegistry getServiceRegistry();

	/// Access to the [GlobalMappingDefaults].
	GlobalMappingDefaults getMappingDefaults();

	/// Access to resolved model preferences which influence how Java and mapping
	/// model concepts are represented while binding metadata.
	default MappingPreferences getMappingPreferences() {
		return MappingPreferences.from( getServiceRegistry() );
	}

	/// Convenience form of [MappingPreferences#getPreferredSqlTypeCodeForBoolean] using [#getMappingPreferences]
	default int getPreferredSqlTypeCodeForBoolean() {
		return getMappingPreferences().getPreferredSqlTypeCodeForBoolean();
	}

	/// Convenience form of [MappingPreferences#getPreferredSqlTypeCodeForDuration] using [#getMappingPreferences]
	default int getPreferredSqlTypeCodeForDuration() {
		return getMappingPreferences().getPreferredSqlTypeCodeForDuration();
	}

	/// Convenience form of [MappingPreferences#getPreferredSqlTypeCodeForUuid] using [#getMappingPreferences]
	default int getPreferredSqlTypeCodeForUuid() {
		return getMappingPreferences().getPreferredSqlTypeCodeForUuid();
	}

	/// Convenience form of [MappingPreferences#getPreferredSqlTypeCodeForInstant] using [#getMappingPreferences]
	default int getPreferredSqlTypeCodeForInstant() {
		return getMappingPreferences().getPreferredSqlTypeCodeForInstant();
	}

	/// Convenience form of [MappingPreferences#getPreferredSqlTypeCodeForArray] using [#getMappingPreferences]
	default int getPreferredSqlTypeCodeForArray() {
		return getMappingPreferences().getPreferredSqlTypeCodeForArray();
	}

	/// Convenience form of [MappingPreferences#isPreferJavaTimeJdbcTypesEnabled] using [#getMappingPreferences]
	default boolean isPreferJavaTimeJdbcTypesEnabled() {
		return getMappingPreferences().isPreferJavaTimeJdbcTypesEnabled();
	}

	/// Convenience form of [MappingPreferences#isPreferNativeEnumTypesEnabled] using [#getMappingPreferences]
	default boolean isPreferNativeEnumTypesEnabled() {
		return getMappingPreferences().isPreferNativeEnumTypesEnabled();
	}

	/// Convenience form of [MappingPreferences#isPreferLocaleLanguageTagEnabled] using [#getMappingPreferences]
	default boolean isPreferLocaleLanguageTagEnabled() {
		return getMappingPreferences().isPreferLocaleLanguageTagEnabled();
	}

	/// @return the [TimeZoneStorageStrategy] determined by the global configuration
	///         property and the {@linkplain #getTimeZoneSupport() time zone support} of
	///         the configured [org.hibernate.dialect.Dialect]
	///
	/// @see org.hibernate.cfg.AvailableSettings#TIMEZONE_DEFAULT_STORAGE
	/// @see org.hibernate.dialect.Dialect#getTimeZoneSupport()
	@Nonnull
	TimeZoneStorageStrategy getDefaultTimeZoneStorage();

	/// @return the [TimeZoneSupport] of the configured [org.hibernate.dialect.Dialect]
	///
	/// @see org.hibernate.dialect.Dialect#getTimeZoneSupport()
	TimeZoneSupport getTimeZoneSupport();

	/// @return the [WrapperArrayHandling] to use for wrapper arrays `Byte[]` and `Character[]`.
	///
	/// @see org.hibernate.cfg.AvailableSettings#WRAPPER_ARRAY_HANDLING
	WrapperArrayHandling getWrapperArrayHandling();

	default CollectionSemanticsResolver getPersistentCollectionRepresentationResolver() {
		// for now always return the standard one
		return StandardCollectionSemanticsResolver.INSTANCE;
	}

	/// Access the list of [org.hibernate.type.BasicType] registrations.
	///
	/// These are the `BasicTypes` explicitly registered by the bootstrap entry point.
	///
	///
	/// @return The `BasicTypes` registrations
	List<BasicTypeRegistration> getBasicTypeRegistrations();

	/// Access the list of [CompositeUserType] registrations.
	List<CompositeUserType<?>> getCompositeUserTypes();

	/// @see org.hibernate.cfg.AvailableSettings#IMPLICIT_NAMING_STRATEGY
	ImplicitNamingStrategy getImplicitNamingStrategy();

	/// @see org.hibernate.cfg.AvailableSettings#PHYSICAL_NAMING_STRATEGY
	PhysicalNamingStrategy getPhysicalNamingStrategy();

	/// @see org.hibernate.cfg.AvailableSettings#COLUMN_ORDERING_STRATEGY
	ColumnOrderingStrategy getColumnOrderingStrategy();

	/// Access to the [SharedCacheMode] to determine if the second-level cache is enabled.
	///
	/// @return The [SharedCacheMode]
	///
	/// @see org.hibernate.cfg.AvailableSettings#JAKARTA_SHARED_CACHE_MODE
	SharedCacheMode getSharedCacheMode();

	/// Access to any implicit cache [AccessType].
	///
	/// @return The implicit cache [AccessType]
	///
	/// @see org.hibernate.cfg.AvailableSettings#DEFAULT_CACHE_CONCURRENCY_STRATEGY
	AccessType getImplicitCacheAccessType();

	/// Is multi-tenancy enabled?
	///
	/// Multi-tenancy is enabled implicitly if a
	///  [org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider] is available.
	///
	/// @return `true` is multi-tenancy is enabled
	///
	/// @see org.hibernate.cfg.AvailableSettings#MULTI_TENANT_CONNECTION_PROVIDER
	boolean isMultiTenancyEnabled();

	/// Whether to use the legacy format for serializing/deserializing XML data.
	///
	/// @since 7.0
	/// @see org.hibernate.cfg.MappingSettings#XML_FORMAT_MAPPER_LEGACY_FORMAT
	@Incubating
	boolean isXmlFormatMapperLegacyFormatEnabled();

	/// @return the [TypeConfiguration] belonging to the [BootstrapContext]
	TypeConfiguration getTypeConfiguration();

	/// Whether explicit discriminator declarations should be ignored for joined
	/// subclass style inheritance.
	///
	/// @return `true` indicates they should be ignored; `false`
	/// indicates they should not be ignored.
	///
	/// @see org.hibernate.cfg.AvailableSettings#IGNORE_EXPLICIT_DISCRIMINATOR_COLUMNS_FOR_JOINED_SUBCLASS
	boolean ignoreExplicitDiscriminatorsForJoinedInheritance();

	/// Whether we should do discrimination implicitly joined subclass style inheritance when no
	/// discriminator info is provided.
	///
	/// @return `true` indicates we should do discrimination; `false` we should not.
	///
	/// @see org.hibernate.cfg.AvailableSettings#IMPLICIT_DISCRIMINATOR_COLUMNS_FOR_JOINED_SUBCLASS
	boolean createImplicitDiscriminatorsForJoinedInheritance();

	/// Whether we should implicitly force discriminators into SQL selects.  By default,
	/// Hibernate will not.  This can be specified per discriminator in the mapping as well.
	///
	/// @return `true` indicates we should force the discriminator in selects for any mappings
	/// which do not say explicitly.
	///
	/// @see org.hibernate.cfg.AvailableSettings#FORCE_DISCRIMINATOR_IN_SELECTS_BY_DEFAULT
	boolean shouldImplicitlyForceDiscriminatorInSelect();

	/// Should we use nationalized variants of character data by default?
	///
	/// For example, should `NVARCHAR` be used in preference to  `VARCHAR`?
	///
	/// @see org.hibernate.cfg.AvailableSettings#USE_NATIONALIZED_CHARACTER_DATA
	///
	/// @return `true` if nationalized character data should be used by default; `false` otherwise.
	boolean useNationalizedCharacterData();

	/// Should we _disable_ constraint creation when
	/// [jakarta.persistence.ConstraintMode#PROVIDER_DEFAULT]?
	///
	/// @see jakarta.persistence.ConstraintMode#PROVIDER_DEFAULT
	/// @see org.hibernate.cfg.AvailableSettings#HBM2DDL_DEFAULT_CONSTRAINT_MODE
	///
	/// @return `true` if we should _not_ create constraints by default;
	///         `false` if we should.
	boolean isNoConstraintByDefault();

	/// @see org.hibernate.cfg.AvailableSettings#HBM2DDL_CHARSET_NAME
	default String getSchemaCharset() {
		return null;
	}

	/// @see org.hibernate.cfg.AvailableSettings#XML_MAPPING_ENABLED
	default boolean isXmlMappingEnabled() {
		return true;
	}

	/// Check to see if extensions can be hosted in CDI
	boolean isAllowExtensionsInCdi();
}
