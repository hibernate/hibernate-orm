/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.spi;

import java.util.List;

import org.hibernate.Incubating;
import org.hibernate.type.TimeZoneStorageStrategy;
import org.hibernate.boot.model.naming.ImplicitNamingStrategy;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.boot.model.relational.ColumnOrderingStrategy;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.collection.internal.StandardCollectionSemanticsResolver;
import org.hibernate.collection.spi.CollectionSemanticsResolver;
import org.hibernate.dialect.TimeZoneSupport;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.hibernate.metamodel.internal.ManagedTypeRepresentationResolverStandard;
import org.hibernate.metamodel.spi.ManagedTypeRepresentationResolver;
import org.hibernate.type.WrapperArrayHandling;
import org.hibernate.type.spi.TypeConfiguration;
import org.hibernate.usertype.CompositeUserType;

import jakarta.persistence.SharedCacheMode;

/**
 * Describes the options used while building the {@link org.hibernate.boot.Metadata}
 * object during {@link org.hibernate.boot.MetadataBuilder#build()} processing.
 *
 * @author Steve Ebersole
 *
 * @since 5.0
 */
public interface MetadataBuildingOptions {
	/**
	 * Access to the {@link StandardServiceRegistry}.
	 */
	StandardServiceRegistry getServiceRegistry();

	/**
	 * Access to the {@link MappingDefaults}.
	 */
	MappingDefaults getMappingDefaults();

	/**
	 * @return the {@link TimeZoneStorageStrategy} determined by the global configuration
	 *         property and the {@linkplain #getTimeZoneSupport() time zone support} of
	 *         the configured {@link org.hibernate.dialect.Dialect}
	 *
	 * @see org.hibernate.cfg.AvailableSettings#TIMEZONE_DEFAULT_STORAGE
	 * @see org.hibernate.dialect.Dialect#getTimeZoneSupport()
	 */
	TimeZoneStorageStrategy getDefaultTimeZoneStorage();

	/**
	 * @return the {@link TimeZoneSupport} of the configured {@link org.hibernate.dialect.Dialect}
	 *
	 * @see org.hibernate.dialect.Dialect#getTimeZoneSupport()
	 */
	TimeZoneSupport getTimeZoneSupport();

	/**
	 * @return the {@link WrapperArrayHandling} to use for wrapper arrays {@code Byte[]} and {@code Character[]}.
	 *
	 * @see org.hibernate.cfg.AvailableSettings#WRAPPER_ARRAY_HANDLING
	 */
	WrapperArrayHandling getWrapperArrayHandling();

	/**
	 * @deprecated no longer called
	 */
	@Deprecated(since="7.0", forRemoval = true)
	default ManagedTypeRepresentationResolver getManagedTypeRepresentationResolver() {
		// for now always return the standard one
		return ManagedTypeRepresentationResolverStandard.INSTANCE;
	}

	default CollectionSemanticsResolver getPersistentCollectionRepresentationResolver() {
		// for now always return the standard one
		return StandardCollectionSemanticsResolver.INSTANCE;
	}

	/**
	 * Access the list of {@link org.hibernate.type.BasicType} registrations.
	 * <p>
	 * These are the {@code BasicTypes} explicitly registered via calls to:
	 * <ul>
	 * <li>{@link org.hibernate.boot.MetadataBuilder#applyBasicType(org.hibernate.type.BasicType)}
	 * <li>{@link org.hibernate.boot.MetadataBuilder#applyBasicType(org.hibernate.type.BasicType, String[])}
	 * <li>{@link org.hibernate.boot.MetadataBuilder#applyBasicType(org.hibernate.usertype.UserType, String[])}
	 * </ul>
	 *
	 * @return The {@code BasicTypes} registrations
	 */
	List<BasicTypeRegistration> getBasicTypeRegistrations();

	/**
	 * Access the list of {@link CompositeUserType} registrations.
	 */
	List<CompositeUserType<?>> getCompositeUserTypes();

	/**
	 * @see org.hibernate.cfg.AvailableSettings#IMPLICIT_NAMING_STRATEGY
	 */
	ImplicitNamingStrategy getImplicitNamingStrategy();

	/**
	 * @see org.hibernate.cfg.AvailableSettings#PHYSICAL_NAMING_STRATEGY
	 */
	PhysicalNamingStrategy getPhysicalNamingStrategy();

	/**
	 * @see org.hibernate.cfg.AvailableSettings#COLUMN_ORDERING_STRATEGY
	 */
	ColumnOrderingStrategy getColumnOrderingStrategy();

	/**
	 * Access to the {@link SharedCacheMode} to determine if the second-level cache is enabled.
	 *
	 * @return The {@link SharedCacheMode}
	 *
	 * @see org.hibernate.cfg.AvailableSettings#JAKARTA_SHARED_CACHE_MODE
	 */
	SharedCacheMode getSharedCacheMode();

	/**
	 * Access to any implicit cache {@link AccessType}.
	 *
	 * @return The implicit cache {@link AccessType}
	 *
	 * @see org.hibernate.cfg.AvailableSettings#DEFAULT_CACHE_CONCURRENCY_STRATEGY
	 */
	AccessType getImplicitCacheAccessType();

	/**
	 * Is multi-tenancy enabled?
	 * <p>
	 * Multi-tenancy is enabled implicitly if a {@link MultiTenantConnectionProvider} is available.
	 *
	 * @return {@code true} is multi-tenancy is enabled
	 *
	 * @see org.hibernate.cfg.AvailableSettings#MULTI_TENANT_CONNECTION_PROVIDER
	 */
	boolean isMultiTenancyEnabled();

	/**
	 * Whether to use the legacy format for serializing/deserializing XML data.
	 *
	 * @since 7.0
	 * @see org.hibernate.cfg.MappingSettings#XML_FORMAT_MAPPER_LEGACY_FORMAT
	 */
	@Incubating
	boolean isXmlFormatMapperLegacyFormatEnabled();

	/**
	 * @return the {@link TypeConfiguration} belonging to the {@link BootstrapContext}
	 */
	TypeConfiguration getTypeConfiguration();

	/**
	 * Whether explicit discriminator declarations should be ignored for joined
	 * subclass style inheritance.
	 *
	 * @return {@code true} indicates they should be ignored; {@code false}
	 * indicates they should not be ignored.
	 *
	 * @see org.hibernate.boot.MetadataBuilder#enableExplicitDiscriminatorsForJoinedSubclassSupport
	 * @see org.hibernate.cfg.AvailableSettings#IGNORE_EXPLICIT_DISCRIMINATOR_COLUMNS_FOR_JOINED_SUBCLASS
	 */
	boolean ignoreExplicitDiscriminatorsForJoinedInheritance();

	/**
	 * Whether we should do discrimination implicitly joined subclass style inheritance when no
	 * discriminator info is provided.
	 *
	 * @return {@code true} indicates we should do discrimination; {@code false} we should not.
	 *
	 * @see org.hibernate.boot.MetadataBuilder#enableImplicitDiscriminatorsForJoinedSubclassSupport
	 * @see org.hibernate.cfg.AvailableSettings#IMPLICIT_DISCRIMINATOR_COLUMNS_FOR_JOINED_SUBCLASS
	 */
	boolean createImplicitDiscriminatorsForJoinedInheritance();

	/**
	 * Whether we should implicitly force discriminators into SQL selects.  By default,
	 * Hibernate will not.  This can be specified per discriminator in the mapping as well.
	 *
	 * @return {@code true} indicates we should force the discriminator in selects for any mappings
	 * which do not say explicitly.
	 *
	 * @see org.hibernate.cfg.AvailableSettings#FORCE_DISCRIMINATOR_IN_SELECTS_BY_DEFAULT
	 */
	boolean shouldImplicitlyForceDiscriminatorInSelect();

	/**
	 * Should we use nationalized variants of character data by default?
	 * <p>
	 * For example, should {@code NVARCHAR} be used in preference to  {@code VARCHAR}?
	 *
	 * @see org.hibernate.boot.MetadataBuilder#enableGlobalNationalizedCharacterDataSupport
	 * @see org.hibernate.cfg.AvailableSettings#USE_NATIONALIZED_CHARACTER_DATA
	 *
	 * @return {@code true} if nationalized character data should be used by default; {@code false} otherwise.
	 */
	boolean useNationalizedCharacterData();

	/**
	 * Should we <em>disable</em> constraint creation when
	 * {@link jakarta.persistence.ConstraintMode#PROVIDER_DEFAULT}?
	 *
	 * @see jakarta.persistence.ConstraintMode#PROVIDER_DEFAULT
	 * @see org.hibernate.cfg.AvailableSettings#HBM2DDL_DEFAULT_CONSTRAINT_MODE
	 *
	 * @return {@code true} if we should <em>not</em> create constraints by default;
	 *         {@code false} if we should.
	 */
	boolean isNoConstraintByDefault();

	/**
	 * @see org.hibernate.cfg.AvailableSettings#HBM2DDL_CHARSET_NAME
	 */
	default String getSchemaCharset() {
		return null;
	}

	/**
	 * @see org.hibernate.cfg.AvailableSettings#XML_MAPPING_ENABLED
	 */
	default boolean isXmlMappingEnabled() {
		return true;
	}

	/**
	 * Check to see if extensions can be hosted in CDI
	 */
	boolean isAllowExtensionsInCdi();
}
