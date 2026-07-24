/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.pipeline.internal;

import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.ConstraintMode;
import jakarta.persistence.FetchType;
import jakarta.persistence.SharedCacheMode;
import org.hibernate.HibernateException;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.TimeZoneStorageType;
import org.hibernate.boot.model.naming.ImplicitNamingStrategy;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl;
import org.hibernate.boot.model.relational.ColumnOrderingStrategy;
import org.hibernate.boot.model.relational.ColumnOrderingStrategyStandard;
import org.hibernate.boot.mapping.internal.context.GlobalMappingDefaultsImpl;
import org.hibernate.boot.mapping.internal.xml.PersistenceUnitMetadata;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.selector.spi.StrategySelector;
import org.hibernate.boot.serial.internal.MappingResolutionDetailsCollector;
import org.hibernate.boot.spi.BasicTypeRegistration;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.boot.spi.JpaOrmXmlPersistenceUnitDefaultAware;
import org.hibernate.boot.spi.GlobalMappingDefaults;
import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cfg.JpaComplianceSettings;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.context.spi.MultiTenancy;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.TimeZoneSupport;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.service.spi.ServiceException;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.TimeZoneStorageStrategy;
import org.hibernate.type.WrapperArrayHandling;
import org.hibernate.type.spi.TypeConfiguration;
import org.hibernate.usertype.CompositeUserType;
import org.hibernate.usertype.UserType;
import org.hibernate.type.BasicType;

import static org.hibernate.cfg.CacheSettings.DEFAULT_CACHE_CONCURRENCY_STRATEGY;
import static org.hibernate.cfg.CacheSettings.JAKARTA_SHARED_CACHE_MODE;
import static org.hibernate.cfg.CacheSettings.JPA_SHARED_CACHE_MODE;
import static org.hibernate.cfg.ManagedBeanSettings.ALLOW_EXTENSIONS_IN_CDI;
import static org.hibernate.cfg.MappingSettings.COLUMN_ORDERING_STRATEGY;
import static org.hibernate.cfg.MappingSettings.FORCE_DISCRIMINATOR_IN_SELECTS_BY_DEFAULT;
import static org.hibernate.cfg.MappingSettings.IGNORE_EXPLICIT_DISCRIMINATOR_COLUMNS_FOR_JOINED_SUBCLASS;
import static org.hibernate.cfg.MappingSettings.IMPLICIT_DISCRIMINATOR_COLUMNS_FOR_JOINED_SUBCLASS;
import static org.hibernate.cfg.MappingSettings.IMPLICIT_NAMING_STRATEGY;
import static org.hibernate.cfg.MappingSettings.METADATA_SERIALIZATION_ENABLED;
import static org.hibernate.cfg.MappingSettings.PHYSICAL_NAMING_STRATEGY;
import static org.hibernate.cfg.MappingSettings.USE_NATIONALIZED_CHARACTER_DATA;
import static org.hibernate.cfg.MappingSettings.XML_FORMAT_MAPPER_LEGACY_FORMAT;
import static org.hibernate.cfg.MappingSettings.XML_MAPPING_ENABLED;
import static org.hibernate.cfg.SchemaToolingSettings.HBM2DDL_CHARSET_NAME;
import static org.hibernate.cfg.SchemaToolingSettings.HBM2DDL_DEFAULT_CONSTRAINT_MODE;
import static org.hibernate.engine.config.spi.StandardConverters.BOOLEAN;
import static org.hibernate.engine.config.spi.StandardConverters.STRING;
import static org.hibernate.internal.log.DeprecationLogger.DEPRECATION_LOGGER;
import static org.hibernate.internal.util.NullnessHelper.coalesceSuppliedValues;
import static org.hibernate.internal.util.StringHelper.nullIfEmpty;

/// Standard mutable mapping-resolution options used by the bootstrap pipeline.
///
/// @since 9.0
/// @author Steve Ebersole
public class MappingResolutionOptionsImpl
		implements MappingResolutionOptions, JpaOrmXmlPersistenceUnitDefaultAware {
	final StandardServiceRegistry serviceRegistry;
	final GlobalMappingDefaultsImpl mappingDefaults;
	private final TimeZoneStorageType defaultTimezoneStorage;
	private final WrapperArrayHandling wrapperArrayHandling;

	// todo (6.0) : remove bootstrapContext property along with the deprecated methods
	private BootstrapContext bootstrapContext;

	final ArrayList<BasicTypeRegistration> basicTypeRegistrations = new ArrayList<>();
	final ArrayList<CompositeUserType<?>> compositeUserTypes = new ArrayList<>();

	ImplicitNamingStrategy implicitNamingStrategy;
	PhysicalNamingStrategy physicalNamingStrategy;
	ColumnOrderingStrategy columnOrderingStrategy;

	SharedCacheMode sharedCacheMode;
	private final AccessType defaultCacheAccessType;
	private final boolean multiTenancyEnabled;
	boolean explicitDiscriminatorsForJoinedInheritanceSupported;
	boolean implicitDiscriminatorsForJoinedInheritanceSupported;
	boolean implicitlyForceDiscriminatorInSelect;
	boolean useNationalizedCharacterData;
	boolean noConstraintByDefault;

	private final String schemaCharset;
	private final boolean xmlMappingEnabled;
	private final boolean allowExtensionsInCdi;
	private final boolean xmlFormatMapperLegacyFormat;
	private final boolean metadataSerializationEnabled;
	private final MappingResolutionDetailsCollector resolutionDetailsCollector;

	public MappingResolutionOptionsImpl(StandardServiceRegistry serviceRegistry) {
		this.serviceRegistry = serviceRegistry;

		final var strategySelector = serviceRegistry.requireService( StrategySelector.class );
		final var configService = serviceRegistry.requireService( ConfigurationService.class );
		metadataSerializationEnabled = configService.getSetting(
				METADATA_SERIALIZATION_ENABLED,
				BOOLEAN,
				false
		);
		resolutionDetailsCollector = metadataSerializationEnabled ? new MappingResolutionDetailsCollector() : null;

		mappingDefaults = new GlobalMappingDefaultsImpl( serviceRegistry );

		defaultTimezoneStorage = resolveTimeZoneStorageStrategy( configService );
		wrapperArrayHandling = resolveWrapperArrayHandling( configService );
		multiTenancyEnabled = MultiTenancy.isMultiTenancyEnabled( serviceRegistry );

		xmlMappingEnabled = configService.getSetting(
				XML_MAPPING_ENABLED,
				BOOLEAN,
				true
		);
		xmlFormatMapperLegacyFormat = configService.getSetting(
				XML_FORMAT_MAPPER_LEGACY_FORMAT,
				BOOLEAN,
				false
		);

		implicitDiscriminatorsForJoinedInheritanceSupported = configService.getSetting(
				IMPLICIT_DISCRIMINATOR_COLUMNS_FOR_JOINED_SUBCLASS,
				BOOLEAN,
				false
		);

		explicitDiscriminatorsForJoinedInheritanceSupported = !configService.getSetting(
				IGNORE_EXPLICIT_DISCRIMINATOR_COLUMNS_FOR_JOINED_SUBCLASS,
				BOOLEAN,
				false
		);

		implicitlyForceDiscriminatorInSelect = configService.getSetting(
				FORCE_DISCRIMINATOR_IN_SELECTS_BY_DEFAULT,
				BOOLEAN,
				false
		);

		sharedCacheMode = getSharedCacheMode( configService );

		defaultCacheAccessType = getDefaultCacheAccessType( configService );

		noConstraintByDefault = isNoConstraintByDefault( configService );

		implicitNamingStrategy = getImplicitNamingStrategy( strategySelector, configService );
		physicalNamingStrategy = getPhysicalNamingStrategy( strategySelector, configService );

		columnOrderingStrategy = getColumnOrderingStrategy( strategySelector, configService );

		useNationalizedCharacterData = configService.getSetting(
				USE_NATIONALIZED_CHARACTER_DATA,
				BOOLEAN,
				false
		);

		schemaCharset = configService.getSetting(
				HBM2DDL_CHARSET_NAME,
				STRING
		);

		allowExtensionsInCdi = configService.getSetting(
				ALLOW_EXTENSIONS_IN_CDI,
				BOOLEAN,
				false
		);
	}

	@Override
	public boolean isMetadataSerializationEnabled() {
		return metadataSerializationEnabled;
	}

	@Override
	public MappingResolutionDetailsCollector getResolutionDetailsCollector() {
		return resolutionDetailsCollector;
	}

	private static boolean isNoConstraintByDefault(ConfigurationService configService) {
		return ConstraintMode.NO_CONSTRAINT.name()
				.equalsIgnoreCase( configService.getSetting(
						HBM2DDL_DEFAULT_CONSTRAINT_MODE,
						STRING
				) );
	}

	private static ColumnOrderingStrategy getColumnOrderingStrategy(
			StrategySelector strategySelector, ConfigurationService configService) {
		return strategySelector.<ColumnOrderingStrategy>resolveDefaultableStrategy(
				ColumnOrderingStrategy.class,
				configService.getSettings().get( COLUMN_ORDERING_STRATEGY ),
				() -> strategySelector.resolveDefaultableStrategy(
						ColumnOrderingStrategy.class,
						"default",
						ColumnOrderingStrategyStandard.INSTANCE
				)
		);
	}

	private static PhysicalNamingStrategy getPhysicalNamingStrategy(
			StrategySelector strategySelector, ConfigurationService configService) {
		return strategySelector.resolveDefaultableStrategy(
				PhysicalNamingStrategy.class,
				configService.getSettings().get( PHYSICAL_NAMING_STRATEGY ),
				PhysicalNamingStrategyStandardImpl.INSTANCE
		);
	}

	private static ImplicitNamingStrategy getImplicitNamingStrategy(
			StrategySelector strategySelector, ConfigurationService configService) {
		return strategySelector.<ImplicitNamingStrategy>resolveDefaultableStrategy(
				ImplicitNamingStrategy.class,
				configService.getSettings().get( IMPLICIT_NAMING_STRATEGY ),
				() -> strategySelector.resolveDefaultableStrategy(
						ImplicitNamingStrategy.class,
						"default",
						ImplicitNamingStrategyJpaCompliantImpl.INSTANCE
				)
		);
	}

	@Nullable
	private AccessType getDefaultCacheAccessType(@Nonnull ConfigurationService configService) {
		final Object value = configService.getSettings().get( DEFAULT_CACHE_CONCURRENCY_STRATEGY );
		if ( value == null ) {
			final var regionFactory = serviceRegistry.getService( RegionFactory.class );
			return regionFactory == null ? null : regionFactory.getDefaultAccessType();
		}
		else {
			if ( value instanceof CacheConcurrencyStrategy cacheConcurrencyStrategy ) {
				return cacheConcurrencyStrategy.toAccessType();
			}
			else if ( value instanceof AccessType accessType ) {
				return accessType;
			}
			else {
				return AccessType.fromExternalName( value.toString() );
			}
		}
	}

	private static SharedCacheMode getSharedCacheMode(ConfigurationService configService) {
		return configService.getSetting(
				JAKARTA_SHARED_CACHE_MODE,
				value -> value instanceof SharedCacheMode cacheMode
						? cacheMode
						: SharedCacheMode.valueOf( value.toString() ),
				configService.getSetting(
						JPA_SHARED_CACHE_MODE,
						value -> {
							DEPRECATION_LOGGER.deprecatedSetting(
									JPA_SHARED_CACHE_MODE,
									JAKARTA_SHARED_CACHE_MODE
							);
							return value instanceof SharedCacheMode cacheMode
									? cacheMode
									: SharedCacheMode.valueOf( value.toString() );
						},
						SharedCacheMode.UNSPECIFIED
				)
		);
	}

	@Override
	public StandardServiceRegistry getServiceRegistry() {
		return serviceRegistry;
	}

	@Override
	public GlobalMappingDefaults getMappingDefaults() {
		return mappingDefaults;
	}

	@Override
	@Nonnull
	public TimeZoneStorageStrategy getDefaultTimeZoneStorage() {
		return toTimeZoneStorageStrategy( getTimeZoneSupport() );
	}

	@Nonnull
	private Dialect getDialect() {
		return serviceRegistry.requireService( JdbcServices.class ).getDialect();
	}

	@Override
	@Nonnull
	public TimeZoneSupport getTimeZoneSupport() {
		try {
			return getDialect().getTimeZoneSupport();
		}
		catch (ServiceException se) {
			return TimeZoneSupport.NONE;
		}
	}

	@Nonnull
	private TimeZoneStorageStrategy toTimeZoneStorageStrategy(@Nonnull TimeZoneSupport timeZoneSupport) {
		return switch (defaultTimezoneStorage) {
			case NATIVE -> {
				if ( timeZoneSupport != TimeZoneSupport.NATIVE ) {
					throw new HibernateException( "The configured time zone storage type NATIVE is not supported with the configured dialect" );
				}
				yield TimeZoneStorageStrategy.NATIVE;
			}
			case COLUMN -> TimeZoneStorageStrategy.COLUMN;
			case NORMALIZE -> TimeZoneStorageStrategy.NORMALIZE;
			case NORMALIZE_UTC -> TimeZoneStorageStrategy.NORMALIZE_UTC;
			case AUTO -> switch (timeZoneSupport) {
				// if the db has native support for timezones, we use that, not a column
				case NATIVE -> TimeZoneStorageStrategy.NATIVE;
				// otherwise we use a separate column
				case NORMALIZE, NONE -> TimeZoneStorageStrategy.COLUMN;
			};
			case DEFAULT -> switch (timeZoneSupport) {
				// if the db has native support for timezones, we use that, and don't normalize
				case NATIVE -> TimeZoneStorageStrategy.NATIVE;
				// otherwise we normalize things to UTC
				case NORMALIZE, NONE -> TimeZoneStorageStrategy.NORMALIZE_UTC;
			};
		};
	}

	@Override
	public WrapperArrayHandling getWrapperArrayHandling() {
		return wrapperArrayHandling == WrapperArrayHandling.PICK
				? pickWrapperArrayHandling( getDialect() )
				: wrapperArrayHandling;
	}

	@Override
	public List<BasicTypeRegistration> getBasicTypeRegistrations() {
		return basicTypeRegistrations;
	}

	@Override
	public List<CompositeUserType<?>> getCompositeUserTypes() {
		return compositeUserTypes;
	}

	@Override
	public TypeConfiguration getTypeConfiguration() {
		return bootstrapContext.getTypeConfiguration();
	}

	@Override
	public ImplicitNamingStrategy getImplicitNamingStrategy() {
		return implicitNamingStrategy;
	}

	@Override
	public PhysicalNamingStrategy getPhysicalNamingStrategy() {
		return physicalNamingStrategy;
	}

	@Override
	public ColumnOrderingStrategy getColumnOrderingStrategy() {
		return columnOrderingStrategy;
	}

	@Override
	public SharedCacheMode getSharedCacheMode() {
		return sharedCacheMode;
	}

	@Override
	public AccessType getImplicitCacheAccessType() {
		return defaultCacheAccessType;
	}

	@Override
	public boolean isMultiTenancyEnabled() {
		return multiTenancyEnabled;
	}

	@Override
	public boolean ignoreExplicitDiscriminatorsForJoinedInheritance() {
		return !explicitDiscriminatorsForJoinedInheritanceSupported;
	}

	@Override
	public boolean createImplicitDiscriminatorsForJoinedInheritance() {
		return implicitDiscriminatorsForJoinedInheritanceSupported;
	}

	@Override
	public boolean shouldImplicitlyForceDiscriminatorInSelect() {
		return implicitlyForceDiscriminatorInSelect;
	}

	@Override
	public boolean useNationalizedCharacterData() {
		return useNationalizedCharacterData;
	}

	@Override
	public boolean isNoConstraintByDefault() {
		return noConstraintByDefault;
	}

	@Override
	public String getSchemaCharset() {
		return schemaCharset;
	}

	@Override
	public boolean isXmlMappingEnabled() {
		return xmlMappingEnabled;
	}

	@Override
	public boolean isAllowExtensionsInCdi() {
		return allowExtensionsInCdi;
	}

	@Override
	public boolean isXmlFormatMapperLegacyFormatEnabled() {
		return xmlFormatMapperLegacyFormat;
	}

	/**
	 * Yuck. This is needed because JPA lets users define "global building defaults"
	 * in {@code orm.xml} mappings. Forget that there are generally multiple
	 * {@code orm.xml} mappings if using XML approach...  Ugh
	 */
	public void apply(JpaOrmXmlPersistenceUnitDefaultAware.JpaOrmXmlPersistenceUnitDefaults jpaOrmXmlPersistenceUnitDefaults) {
		if ( !mappingDefaults.shouldImplicitlyQuoteIdentifiers() ) {
			mappingDefaults.applyImplicitlyQuoteIdentifiers(
					jpaOrmXmlPersistenceUnitDefaults.shouldImplicitlyQuoteIdentifiers()
			);
		}

		if ( mappingDefaults.getImplicitCatalogName() == null ) {
			mappingDefaults.applyImplicitCatalogName(
					nullIfEmpty( jpaOrmXmlPersistenceUnitDefaults.getDefaultCatalogName() )
			);
		}

		if ( mappingDefaults.getImplicitSchemaName() == null ) {
			mappingDefaults.applyImplicitSchemaName(
					nullIfEmpty( jpaOrmXmlPersistenceUnitDefaults.getDefaultSchemaName() )
			);
		}
	}

	@Override
	public void apply(PersistenceUnitMetadata persistenceUnitMetadata) {
		if ( !mappingDefaults.shouldImplicitlyQuoteIdentifiers() ) {
			mappingDefaults.applyImplicitlyQuoteIdentifiers(
					persistenceUnitMetadata.useQuotedIdentifiers()
			);
		}

		if ( mappingDefaults.getImplicitCatalogName() == null ) {
			mappingDefaults.applyImplicitCatalogName(
					nullIfEmpty( persistenceUnitMetadata.getDefaultCatalog() )
			);
		}

		if ( mappingDefaults.getImplicitSchemaName() == null ) {
			mappingDefaults.applyImplicitSchemaName(
					nullIfEmpty( persistenceUnitMetadata.getDefaultSchema() )
			);
		}
	}

	public void setBootstrapContext(BootstrapContext bootstrapContext) {
		this.bootstrapContext = bootstrapContext;
	}

	public void applyDefaultToOneFetchType(FetchType defaultToOneFetchType) {
		mappingDefaults.applyDefaultToOneFetchType( defaultToOneFetchType == FetchType.LAZY );
	}

	public void applyImplicitSchemaName(String implicitSchemaName) {
		mappingDefaults.applyImplicitSchemaName( implicitSchemaName );
	}

	public void applyImplicitCatalogName(String implicitCatalogName) {
		mappingDefaults.applyImplicitCatalogName( implicitCatalogName );
	}

	public void applyImplicitNamingStrategy(ImplicitNamingStrategy namingStrategy) {
		implicitNamingStrategy = namingStrategy;
	}

	public void applyPhysicalNamingStrategy(PhysicalNamingStrategy namingStrategy) {
		physicalNamingStrategy = namingStrategy;
	}

	public void applyColumnOrderingStrategy(ColumnOrderingStrategy columnOrderingStrategy) {
		this.columnOrderingStrategy = columnOrderingStrategy;
	}

	public void applySharedCacheMode(SharedCacheMode sharedCacheMode) {
		this.sharedCacheMode = sharedCacheMode;
	}

	public void applyAccessType(AccessType implicitCacheAccessType) {
		mappingDefaults.applyImplicitCacheAccessType( implicitCacheAccessType );
	}

	public void enableExplicitDiscriminatorsForJoinedSubclassSupport(boolean supported) {
		explicitDiscriminatorsForJoinedInheritanceSupported = supported;
	}

	public void enableImplicitDiscriminatorsForJoinedSubclassSupport(boolean supported) {
		implicitDiscriminatorsForJoinedInheritanceSupported = supported;
	}

	public void enableImplicitForcingOfDiscriminatorsInSelect(boolean supported) {
		implicitlyForceDiscriminatorInSelect = supported;
	}

	public void enableGlobalNationalizedCharacterDataSupport(boolean enabled) {
		useNationalizedCharacterData = enabled;
	}

	public void noConstraintByDefault() {
		noConstraintByDefault = true;
	}

	public void applyBasicType(BasicType<?> type) {
		basicTypeRegistrations.add( new BasicTypeRegistration( type ) );
	}

	public void applyBasicType(BasicType<?> type, String... keys) {
		basicTypeRegistrations.add( new BasicTypeRegistration( type, keys ) );
	}

	public void applyBasicType(UserType<?> type, String... keys) {
		basicTypeRegistrations.add( new BasicTypeRegistration( type, keys, getTypeConfiguration() ) );
	}

	public void contributeCompositeUserType(CompositeUserType<?> type) {
		compositeUserTypes.add( type );
	}

	private static TimeZoneStorageType resolveTimeZoneStorageStrategy(
			ConfigurationService configService) {
		return configService.getSetting(
				MappingSettings.TIMEZONE_DEFAULT_STORAGE,
				value -> TimeZoneStorageType.valueOf( value.toString() ),
				TimeZoneStorageType.DEFAULT
		);
	}

	private static WrapperArrayHandling resolveWrapperArrayHandling(
			ConfigurationService configService) {
		return coalesceSuppliedValues(
				() -> configService.getSetting(
						MappingSettings.WRAPPER_ARRAY_HANDLING,
						WrapperArrayHandling::interpretExternalSettingLeniently
				),
				() -> resolveFallbackWrapperArrayHandling( configService )
		);
	}

	private static WrapperArrayHandling pickWrapperArrayHandling(Dialect dialect) {
		if ( dialect.supportsStandardArrays()
			&& ( dialect.getPreferredSqlTypeCodeForArray() == SqlTypes.ARRAY
				|| dialect.getPreferredSqlTypeCodeForArray() == SqlTypes.SQLXML ) ) {
			return WrapperArrayHandling.ALLOW;
		}
		else {
			return WrapperArrayHandling.LEGACY;
		}
	}

	private static WrapperArrayHandling resolveFallbackWrapperArrayHandling(
			ConfigurationService configService) {
		return configService.getSetting( JpaComplianceSettings.JPA_COMPLIANCE, BOOLEAN, false )
				? WrapperArrayHandling.PICK // JPA compliance was enabled. Use PICK
				: WrapperArrayHandling.DISALLOW;
	}
}
