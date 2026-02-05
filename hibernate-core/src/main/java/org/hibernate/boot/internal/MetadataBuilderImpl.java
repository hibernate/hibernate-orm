/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import jakarta.persistence.FetchType;
import org.hibernate.AnnotationException;
import org.hibernate.HibernateException;
import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.cfg.JpaComplianceSettings;
import org.hibernate.context.spi.MultiTenancy;
import org.hibernate.type.TimeZoneStorageStrategy;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.TimeZoneStorageType;
import org.hibernate.boot.CacheRegionDefinition;
import org.hibernate.boot.MetadataBuilder;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.archive.scan.spi.ScanEnvironment;
import org.hibernate.boot.archive.scan.spi.ScanOptions;
import org.hibernate.boot.archive.scan.spi.Scanner;
import org.hibernate.boot.archive.spi.ArchiveDescriptorFactory;
import org.hibernate.boot.cfgxml.spi.CfgXmlAccessService;
import org.hibernate.boot.jaxb.hbm.transform.HbmXmlTransformer;
import org.hibernate.boot.jaxb.hbm.transform.UnsupportedFeatureHandling;
import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.FunctionContributor;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.boot.model.TypeContributor;
import org.hibernate.boot.model.convert.internal.ConverterDescriptors;
import org.hibernate.boot.model.convert.spi.ConverterDescriptor;
import org.hibernate.boot.model.naming.ImplicitNamingStrategy;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl;
import org.hibernate.boot.model.process.spi.MetadataBuildingProcess;
import org.hibernate.boot.model.relational.AuxiliaryDatabaseObject;
import org.hibernate.boot.model.relational.ColumnOrderingStrategy;
import org.hibernate.boot.model.relational.ColumnOrderingStrategyStandard;
import org.hibernate.boot.models.xml.spi.PersistenceUnitMetadata;
import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.selector.spi.StrategySelector;
import org.hibernate.boot.spi.BasicTypeRegistration;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.boot.spi.JpaOrmXmlPersistenceUnitDefaultAware;
import org.hibernate.boot.spi.MappingDefaults;
import org.hibernate.boot.spi.MetadataBuilderImplementor;
import org.hibernate.boot.spi.MetadataBuilderInitializer;
import org.hibernate.boot.spi.MetadataBuildingOptions;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.boot.spi.MetadataSourcesContributor;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.TimeZoneSupport;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.metamodel.CollectionClassification;
import org.hibernate.query.sqm.function.SqmFunctionDescriptor;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.spi.ServiceException;
import org.hibernate.type.BasicType;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.WrapperArrayHandling;
import org.hibernate.type.spi.TypeConfiguration;
import org.hibernate.usertype.CompositeUserType;
import org.hibernate.usertype.UserType;


import jakarta.persistence.AttributeConverter;
import jakarta.persistence.ConstraintMode;
import jakarta.persistence.SharedCacheMode;

import static org.hibernate.cfg.CacheSettings.DEFAULT_CACHE_CONCURRENCY_STRATEGY;
import static org.hibernate.cfg.CacheSettings.JAKARTA_SHARED_CACHE_MODE;
import static org.hibernate.cfg.CacheSettings.JPA_SHARED_CACHE_MODE;
import static org.hibernate.cfg.ManagedBeanSettings.ALLOW_EXTENSIONS_IN_CDI;
import static org.hibernate.cfg.MappingSettings.COLUMN_ORDERING_STRATEGY;
import static org.hibernate.cfg.MappingSettings.DEFAULT_LIST_SEMANTICS;
import static org.hibernate.cfg.MappingSettings.FORCE_DISCRIMINATOR_IN_SELECTS_BY_DEFAULT;
import static org.hibernate.cfg.MappingSettings.GLOBALLY_QUOTED_IDENTIFIERS;
import static org.hibernate.cfg.MappingSettings.IGNORE_EXPLICIT_DISCRIMINATOR_COLUMNS_FOR_JOINED_SUBCLASS;
import static org.hibernate.cfg.MappingSettings.IMPLICIT_DISCRIMINATOR_COLUMNS_FOR_JOINED_SUBCLASS;
import static org.hibernate.cfg.MappingSettings.IMPLICIT_NAMING_STRATEGY;
import static org.hibernate.cfg.MappingSettings.PHYSICAL_NAMING_STRATEGY;
import static org.hibernate.cfg.MappingSettings.TRANSFORM_HBM_XML;
import static org.hibernate.cfg.MappingSettings.TRANSFORM_HBM_XML_FEATURE_HANDLING;
import static org.hibernate.cfg.MappingSettings.USE_NATIONALIZED_CHARACTER_DATA;
import static org.hibernate.cfg.MappingSettings.XML_FORMAT_MAPPER_LEGACY_FORMAT;
import static org.hibernate.cfg.MappingSettings.XML_MAPPING_ENABLED;
import static org.hibernate.cfg.SchemaToolingSettings.HBM2DDL_CHARSET_NAME;
import static org.hibernate.cfg.SchemaToolingSettings.HBM2DDL_DEFAULT_CONSTRAINT_MODE;
import static org.hibernate.engine.config.spi.StandardConverters.BOOLEAN;
import static org.hibernate.engine.config.spi.StandardConverters.STRING;
import static org.hibernate.boot.BootLogging.BOOT_LOGGER;
import static org.hibernate.internal.log.DeprecationLogger.DEPRECATION_LOGGER;
import static org.hibernate.internal.util.NullnessHelper.coalesceSuppliedValues;
import static org.hibernate.internal.util.StringHelper.nullIfEmpty;
import static org.hibernate.internal.util.collections.CollectionHelper.isNotEmpty;

/**
 * @author Steve Ebersole
 */
public class MetadataBuilderImpl implements MetadataBuilderImplementor, TypeContributions {

	private final MetadataSources sources;
	private final BootstrapContextImpl bootstrapContext;
	private final MetadataBuildingOptionsImpl options;

	public MetadataBuilderImpl(MetadataSources sources) {
		this( sources, getStandardServiceRegistry( sources.getServiceRegistry() ) );
	}

	public static StandardServiceRegistry getStandardServiceRegistry(ServiceRegistry serviceRegistry) {
		if ( serviceRegistry == null ) {
			throw new HibernateException( "ServiceRegistry passed to MetadataBuilder cannot be null" );
		}
		else if ( serviceRegistry instanceof StandardServiceRegistry standardServiceRegistry ) {
			return standardServiceRegistry;
		}
		else if ( serviceRegistry instanceof BootstrapServiceRegistry bootstrapServiceRegistry ) {
			BOOT_LOGGER.badServiceRegistry();
			return new StandardServiceRegistryBuilder( bootstrapServiceRegistry ).build();
		}
		else {
			throw new HibernateException(
					String.format(
							"Unexpected type of ServiceRegistry [%s] encountered in attempt to build MetadataBuilder",
							serviceRegistry.getClass().getName()
					)
			);
		}
	}

	public MetadataBuilderImpl(MetadataSources sources, StandardServiceRegistry serviceRegistry) {
		this.sources = sources;
		this.options = new MetadataBuildingOptionsImpl( serviceRegistry );
		this.bootstrapContext = new BootstrapContextImpl( serviceRegistry, options );
		//this is needed only for implementing deprecated method
		options.setBootstrapContext( bootstrapContext );

		for ( MetadataSourcesContributor contributor :
				sources.getServiceRegistry()
						.requireService( ClassLoaderService.class )
						.loadJavaServices( MetadataSourcesContributor.class ) ) {
			contributor.contribute( sources );
		}

		// todo : not so sure this is needed anymore.
		//		these should be set during the StandardServiceRegistryBuilder.configure call
		applyCfgXmlValues( serviceRegistry.requireService( CfgXmlAccessService.class ) );

		for ( var contributor :
				serviceRegistry.requireService( ClassLoaderService.class )
						.loadJavaServices( MetadataBuilderInitializer.class ) ) {
			contributor.contribute( this, serviceRegistry );
		}
	}

	private void applyCfgXmlValues(CfgXmlAccessService service) {
		final var aggregatedConfig = service.getAggregatedConfig();
		if ( aggregatedConfig != null ) {
			for ( var cacheRegionDefinition : aggregatedConfig.getCacheRegionDefinitions() ) {
				applyCacheRegionDefinition( cacheRegionDefinition );
			}
		}
	}

	@Override
	public MetadataBuilder applyImplicitSchemaName(String implicitSchemaName) {
		options.mappingDefaults.implicitSchemaName = implicitSchemaName;
		return this;
	}

	@Override
	public MetadataBuilder applyImplicitCatalogName(String implicitCatalogName) {
		options.mappingDefaults.implicitCatalogName = implicitCatalogName;
		return this;
	}

	@Override
	public MetadataBuilder applyImplicitNamingStrategy(ImplicitNamingStrategy namingStrategy) {
		options.implicitNamingStrategy = namingStrategy;
		return this;
	}

	@Override
	public MetadataBuilder applyPhysicalNamingStrategy(PhysicalNamingStrategy namingStrategy) {
		options.physicalNamingStrategy = namingStrategy;
		return this;
	}

	@Override
	public MetadataBuilder applyColumnOrderingStrategy(ColumnOrderingStrategy columnOrderingStrategy) {
		options.columnOrderingStrategy = columnOrderingStrategy;
		return this;
	}

	@Override
	public MetadataBuilder applySharedCacheMode(SharedCacheMode sharedCacheMode) {
		options.sharedCacheMode = sharedCacheMode;
		return this;
	}

	@Override
	public MetadataBuilder applyAccessType(AccessType implicitCacheAccessType) {
		options.mappingDefaults.implicitCacheAccessType = implicitCacheAccessType;
		return this;
	}

	@Override
	public MetadataBuilder applyIndexView(Object jandexView) {
		return this;
	}

	@Override
	public MetadataBuilder applyScanOptions(ScanOptions scanOptions) {
		bootstrapContext.injectScanOptions( scanOptions );
		return this;
	}

	@Override
	public MetadataBuilder applyScanEnvironment(ScanEnvironment scanEnvironment) {
		bootstrapContext.injectScanEnvironment( scanEnvironment );
		return this;
	}

	@Override
	public MetadataBuilder applyScanner(Scanner scanner) {
		bootstrapContext.injectScanner( scanner );
		return this;
	}

	@Override
	public MetadataBuilder applyArchiveDescriptorFactory(ArchiveDescriptorFactory factory) {
		bootstrapContext.injectArchiveDescriptorFactory( factory );
		return this;
	}

	@Override
	public MetadataBuilder applyImplicitListSemantics(CollectionClassification classification) {
		if ( classification != null ) {
			options.mappingDefaults.implicitListClassification = classification;
		}
		return this;
	}

	@Override
	public MetadataBuilder enableExplicitDiscriminatorsForJoinedSubclassSupport(boolean supported) {
		options.explicitDiscriminatorsForJoinedInheritanceSupported = supported;
		return this;
	}

	@Override
	public MetadataBuilder enableImplicitDiscriminatorsForJoinedSubclassSupport(boolean supported) {
		options.implicitDiscriminatorsForJoinedInheritanceSupported = supported;
		return this;
	}

	@Override
	public MetadataBuilder enableImplicitForcingOfDiscriminatorsInSelect(boolean supported) {
		options.implicitlyForceDiscriminatorInSelect = supported;
		return this;
	}

	@Override
	public MetadataBuilder enableGlobalNationalizedCharacterDataSupport(boolean enabled) {
		options.useNationalizedCharacterData = enabled;
		return this;
	}

	@Override
	public MetadataBuilder applyDefaultToOneFetchType(FetchType defaultToOneFetchType) {
		options.mappingDefaults.toOnesAreLazyByDefault = defaultToOneFetchType == FetchType.LAZY;
		return this;
	}

	@Override
	public MetadataBuilder applyBasicType(BasicType<?> type) {
		options.basicTypeRegistrations.add( new BasicTypeRegistration( type ) );
		return this;
	}

	@Override
	public MetadataBuilder applyBasicType(BasicType<?> type, String... keys) {
		options.basicTypeRegistrations.add( new BasicTypeRegistration( type, keys ) );
		return this;
	}

	@Override
	public MetadataBuilder applyBasicType(UserType<?> type, String... keys) {
		options.basicTypeRegistrations.add( new BasicTypeRegistration( type, keys, getTypeConfiguration() ) );
		return this;
	}

	@Override
	public MetadataBuilder applyTypes(TypeContributor typeContributor) {
		typeContributor.contribute( this, options.serviceRegistry );
		return this;
	}

	@Override
	@Deprecated
	public void contributeType(BasicType<?> type) {
		options.basicTypeRegistrations.add( new BasicTypeRegistration( type ) );
	}

	@Override
	@Deprecated
	public void contributeType(BasicType<?> type, String... keys) {
		options.basicTypeRegistrations.add( new BasicTypeRegistration( type, keys ) );
	}

	@Override
	@Deprecated
	public void contributeType(UserType<?> type, String[] keys) {
		options.basicTypeRegistrations.add( new BasicTypeRegistration( type, keys, getTypeConfiguration() ) );
	}

	@Override
	public void contributeType(CompositeUserType<?> type) {
		options.compositeUserTypes.add( type );
	}

	@Override
	public TypeConfiguration getTypeConfiguration() {
		return bootstrapContext.getTypeConfiguration();
	}

	@Override
	public void contributeAttributeConverter(Class<? extends AttributeConverter<?,?>> converterClass) {
		bootstrapContext.addAttributeConverterDescriptor(
				ConverterDescriptors.of( converterClass )
		);
	}

	@Override
	public MetadataBuilder applyCacheRegionDefinition(CacheRegionDefinition cacheRegionDefinition) {
		bootstrapContext.addCacheRegionDefinition( cacheRegionDefinition );
		return this;
	}

	@Override
	public MetadataBuilder applyTempClassLoader(ClassLoader tempClassLoader) {
		bootstrapContext.injectJpaTempClassLoader( tempClassLoader );
		return this;
	}

	public MetadataBuilder noConstraintByDefault() {
		options.noConstraintByDefault = true;
		return this;
	}

	@Override
	public MetadataBuilder applyFunctions(FunctionContributor functionContributor) {
		functionContributor.contributeFunctions( new FunctionContributions() {
			@Override
			public SqmFunctionRegistry getFunctionRegistry() {
				return bootstrapContext.getFunctionRegistry();
			}

			@Override
			public TypeConfiguration getTypeConfiguration() {
				return bootstrapContext.getTypeConfiguration();
			}

			@Override
			public ServiceRegistry getServiceRegistry() {
				return bootstrapContext.getServiceRegistry();
			}
		}  );
		return this;
	}

	@Override
	public MetadataBuilder applySqlFunction(String functionName, SqmFunctionDescriptor function) {
		bootstrapContext.addSqlFunction( functionName, function );
		return this;
	}

	@Override
	public MetadataBuilder applyAuxiliaryDatabaseObject(AuxiliaryDatabaseObject auxiliaryDatabaseObject) {
		bootstrapContext.addAuxiliaryDatabaseObject( auxiliaryDatabaseObject );
		return this;
	}

	@Override
	public MetadataBuilder applyAttributeConverter(ConverterDescriptor<?,?> descriptor) {
		bootstrapContext.addAttributeConverterDescriptor( descriptor );
		return this;
	}

	@Override
	public <O,R> MetadataBuilder applyAttributeConverter(Class<? extends AttributeConverter<O,R>> attributeConverterClass) {
		bootstrapContext.addAttributeConverterDescriptor(
				ConverterDescriptors.of( attributeConverterClass )
		);
		return this;
	}

	@Override
	public <O,R> MetadataBuilder applyAttributeConverter(Class<? extends AttributeConverter<O,R>> attributeConverterClass, boolean autoApply) {
		bootstrapContext.addAttributeConverterDescriptor(
				ConverterDescriptors.of( attributeConverterClass, autoApply, false )
		);
		return this;
	}

	@Override
	public <O,R> MetadataBuilder applyAttributeConverter(AttributeConverter<O,R> attributeConverter) {
		bootstrapContext.addAttributeConverterDescriptor(
				ConverterDescriptors.of( attributeConverter )
		);
		return this;
	}

	@Override
	public MetadataBuilder applyAttributeConverter(AttributeConverter<?,?> attributeConverter, boolean autoApply) {
		bootstrapContext.addAttributeConverterDescriptor(
				ConverterDescriptors.of( attributeConverter, autoApply )
		);
		return this;
	}

	@Override
	public MetadataImplementor build() {
		final var aggregatedConfig =
				options.serviceRegistry.requireService( CfgXmlAccessService.class )
						.getAggregatedConfig();
		if ( aggregatedConfig != null ) {
			final var mappingReferences = aggregatedConfig.getMappingReferences();
			if ( mappingReferences != null ) {
				for ( var mappingReference : mappingReferences ) {
					mappingReference.apply( sources );
				}
			}
		}

		final var bootModel = MetadataBuildingProcess.build( sources, bootstrapContext, options );

		if ( isNotEmpty( sources.getHbmXmlBindings() ) ) {
			final var configurationService = bootstrapContext.getConfigurationService();
			final boolean transformHbm = configurationService != null
					&& configurationService.getSetting( TRANSFORM_HBM_XML, BOOLEAN,false );

			if ( !transformHbm ) {
				for ( var hbmXmlBinding : sources.getHbmXmlBindings() ) {
					final var origin = hbmXmlBinding.getOrigin();
					DEPRECATION_LOGGER.logDeprecatedHbmXmlProcessing( origin.getType(), origin.getName() );
				}
			}
			else {
				final var transformed = HbmXmlTransformer.transform(
						sources.getHbmXmlBindings(),
						bootModel,
						UnsupportedFeatureHandling.fromSetting(
								configurationService.getSettings().get( TRANSFORM_HBM_XML_FEATURE_HANDLING ),
								UnsupportedFeatureHandling.ERROR
						)
				);

				final var newSources = new MetadataSources( bootstrapContext.getServiceRegistry() );
				if ( sources.getAnnotatedClasses() != null ) {
					sources.getAnnotatedClasses().forEach( newSources::addAnnotatedClass );
				}
				if ( sources.getAnnotatedClassNames() != null ) {
					sources.getAnnotatedClassNames().forEach( newSources::addAnnotatedClassName );
				}
				if ( sources.getAnnotatedPackages() != null ) {
					sources.getAnnotatedPackages().forEach( newSources::addPackage );
				}
				if ( sources.getExtraQueryImports() != null ) {
					sources.getExtraQueryImports().forEach( newSources::addQueryImport );
				}
				for ( var mappingXmlBinding : transformed ) {
					newSources.addMappingXmlBinding( mappingXmlBinding );
				}

				return (MetadataImplementor) newSources.buildMetadata();
			}
		}

		return bootModel;
	}

	@Override
	public BootstrapContext getBootstrapContext() {
		return bootstrapContext;
	}

	@Override
	public MetadataBuildingOptions getMetadataBuildingOptions() {
		return options;
	}

	public static class MappingDefaultsImpl implements MappingDefaults {
		private String implicitSchemaName;
		private String implicitCatalogName;
		private boolean implicitlyQuoteIdentifiers;

		private boolean toOnesAreLazyByDefault = false;

		private AccessType implicitCacheAccessType;
		private CollectionClassification implicitListClassification;

		public MappingDefaultsImpl(StandardServiceRegistry serviceRegistry) {
			final var configService = serviceRegistry.requireService( ConfigurationService.class );

			// AvailableSettings.DEFAULT_SCHEMA and AvailableSettings.DEFAULT_CATALOG
			// are taken into account later, at runtime, when rendering table/sequence names.
			// These fields are exclusively about mapping defaults,
			// overridden in XML mappings or through setters in MetadataBuilder.
			implicitSchemaName = null;
			implicitCatalogName = null;

			implicitlyQuoteIdentifiers = configService.getSetting(
					GLOBALLY_QUOTED_IDENTIFIERS,
					BOOLEAN,
					false
			);

			implicitCacheAccessType = configService.getSetting(
					DEFAULT_CACHE_CONCURRENCY_STRATEGY,
					value -> AccessType.fromExternalName( value.toString() )
			);

			implicitListClassification = configService.getSetting(
					DEFAULT_LIST_SEMANTICS,
					value -> {
						final var classification = CollectionClassification.interpretSetting( value );
						if ( classification != CollectionClassification.LIST
							&& classification != CollectionClassification.BAG ) {
							throw new AnnotationException(
									String.format(
											Locale.ROOT,
											"'%s' should specify either '%s' or '%s' (was '%s')",
											DEFAULT_LIST_SEMANTICS,
											java.util.List.class.getName(),
											java.util.Collection.class.getName(),
											classification.name()
									)
							);
						}
						return classification;
					},
					CollectionClassification.BAG
			);
		}

		@Override
		public String getImplicitSchemaName() {
			return implicitSchemaName;
		}

		@Override
		public String getImplicitCatalogName() {
			return implicitCatalogName;
		}

		@Override
		public boolean shouldImplicitlyQuoteIdentifiers() {
			return implicitlyQuoteIdentifiers;
		}

		@Override
		public String getImplicitIdColumnName() {
			return DEFAULT_IDENTIFIER_COLUMN_NAME;
		}

		@Override
		public String getImplicitTenantIdColumnName() {
			return DEFAULT_TENANT_IDENTIFIER_COLUMN_NAME;
		}

		@Override
		public String getImplicitDiscriminatorColumnName() {
			return DEFAULT_DISCRIMINATOR_COLUMN_NAME;
		}

		@Override
		public String getImplicitPackageName() {
			return null;
		}

		@Override
		public boolean isAutoImportEnabled() {
			return true;
		}

		@Override
		public String getImplicitCascadeStyleName() {
			return DEFAULT_CASCADE_NAME;
		}

		@Override
		public String getImplicitPropertyAccessorName() {
			return DEFAULT_PROPERTY_ACCESS_NAME;
		}

		@Override
		public boolean areEntitiesImplicitlyLazy() {
			return toOnesAreLazyByDefault;
		}

		@Override
		public boolean areCollectionsImplicitlyLazy() {
			// for now, just hard-code
			return true;
		}

		@Override
		public AccessType getImplicitCacheAccessType() {
			return implicitCacheAccessType;
		}

		@Override
		public CollectionClassification getImplicitListClassification() {
			return implicitListClassification;
		}
	}

	public static class MetadataBuildingOptionsImpl
			implements MetadataBuildingOptions, JpaOrmXmlPersistenceUnitDefaultAware {
		private final StandardServiceRegistry serviceRegistry;
		private final MappingDefaultsImpl mappingDefaults;
		private final TimeZoneStorageType defaultTimezoneStorage;
		private final WrapperArrayHandling wrapperArrayHandling;

		// todo (6.0) : remove bootstrapContext property along with the deprecated methods
		private BootstrapContext bootstrapContext;

		private final ArrayList<BasicTypeRegistration> basicTypeRegistrations = new ArrayList<>();
		private final ArrayList<CompositeUserType<?>> compositeUserTypes = new ArrayList<>();

		private ImplicitNamingStrategy implicitNamingStrategy;
		private PhysicalNamingStrategy physicalNamingStrategy;
		private ColumnOrderingStrategy columnOrderingStrategy;

		private SharedCacheMode sharedCacheMode;
		private final AccessType defaultCacheAccessType;
		private final boolean multiTenancyEnabled;
		private boolean explicitDiscriminatorsForJoinedInheritanceSupported;
		private boolean implicitDiscriminatorsForJoinedInheritanceSupported;
		private boolean implicitlyForceDiscriminatorInSelect;
		private boolean useNationalizedCharacterData;
		private boolean noConstraintByDefault;

		private final String schemaCharset;
		private final boolean xmlMappingEnabled;
		private final boolean allowExtensionsInCdi;
		private final boolean xmlFormatMapperLegacyFormat;

		public MetadataBuildingOptionsImpl(StandardServiceRegistry serviceRegistry) {
			this.serviceRegistry = serviceRegistry;

			final var strategySelector = serviceRegistry.requireService( StrategySelector.class );
			final var configService = serviceRegistry.requireService( ConfigurationService.class );

			mappingDefaults = new MappingDefaultsImpl( serviceRegistry );

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
					STRING,
					null
			);

			allowExtensionsInCdi = configService.getSetting(
					ALLOW_EXTENSIONS_IN_CDI,
					BOOLEAN,
					false
			);
		}

		private static boolean isNoConstraintByDefault(ConfigurationService configService) {
			return ConstraintMode.NO_CONSTRAINT.name()
					.equalsIgnoreCase( configService.getSetting(
							HBM2DDL_DEFAULT_CONSTRAINT_MODE,
							STRING,
							null
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

		private AccessType getDefaultCacheAccessType(ConfigurationService configService) {
			final var regionFactory = serviceRegistry.getService( RegionFactory.class );
			return configService.getSetting(
					DEFAULT_CACHE_CONCURRENCY_STRATEGY,
					value -> {
						if ( value == null ) {
							return null;
						}
						else if ( value instanceof CacheConcurrencyStrategy cacheConcurrencyStrategy ) {
							return cacheConcurrencyStrategy.toAccessType();
						}
						else if ( value instanceof AccessType accessType ) {
							return accessType;
						}
						else {
							return AccessType.fromExternalName( value.toString() );
						}
					},
					// by default, see if the defined RegionFactory (if one) defines a default
					regionFactory == null ? null : regionFactory.getDefaultAccessType()
			);
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
								if ( value == null ) {
									return null;
								}
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
		public MappingDefaults getMappingDefaults() {
			return mappingDefaults;
		}

		@Override
		public TimeZoneStorageStrategy getDefaultTimeZoneStorage() {
			return toTimeZoneStorageStrategy( getTimeZoneSupport() );
		}

		private Dialect getDialect() {
			return serviceRegistry.requireService( JdbcServices.class ).getDialect();
		}

		@Override
		public TimeZoneSupport getTimeZoneSupport() {
			try {
				return getDialect().getTimeZoneSupport();
			}
			catch ( ServiceException se ) {
				return TimeZoneSupport.NONE;
			}
		}

		private TimeZoneStorageStrategy toTimeZoneStorageStrategy(TimeZoneSupport timeZoneSupport) {
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
		 * Yuck. This is needed because JPA lets users define "global building options"
		 * in {@code orm.xml} mappings. Forget that there are generally multiple
		 * {@code orm.xml} mappings if using XML approach...  Ugh
		 */
		public void apply(JpaOrmXmlPersistenceUnitDefaults jpaOrmXmlPersistenceUnitDefaults) {
			if ( !mappingDefaults.shouldImplicitlyQuoteIdentifiers() ) {
				mappingDefaults.implicitlyQuoteIdentifiers =
						jpaOrmXmlPersistenceUnitDefaults.shouldImplicitlyQuoteIdentifiers();
			}

			if ( mappingDefaults.getImplicitCatalogName() == null ) {
				mappingDefaults.implicitCatalogName =
						nullIfEmpty( jpaOrmXmlPersistenceUnitDefaults.getDefaultCatalogName() );
			}

			if ( mappingDefaults.getImplicitSchemaName() == null ) {
				mappingDefaults.implicitSchemaName =
						nullIfEmpty( jpaOrmXmlPersistenceUnitDefaults.getDefaultSchemaName() );
			}
		}

		@Override
		public void apply(PersistenceUnitMetadata persistenceUnitMetadata) {
			if ( !mappingDefaults.implicitlyQuoteIdentifiers ) {
				mappingDefaults.implicitlyQuoteIdentifiers =
						persistenceUnitMetadata.useQuotedIdentifiers();
			}

			if ( mappingDefaults.getImplicitCatalogName() == null ) {
				mappingDefaults.implicitCatalogName =
						nullIfEmpty( persistenceUnitMetadata.getDefaultCatalog() );
			}

			if ( mappingDefaults.getImplicitSchemaName() == null ) {
				mappingDefaults.implicitSchemaName =
						nullIfEmpty( persistenceUnitMetadata.getDefaultSchema() );
			}
		}

		public void setBootstrapContext(BootstrapContext bootstrapContext) {
			this.bootstrapContext = bootstrapContext;
		}
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
