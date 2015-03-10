/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.boot.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.persistence.SharedCacheMode;

import org.hibernate.HibernateException;
import org.hibernate.MultiTenancyStrategy;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.annotations.common.reflection.java.JavaReflectionManager;
import org.hibernate.boot.CacheRegionDefinition;
import org.hibernate.boot.MetadataBuilder;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.archive.scan.internal.StandardScanOptions;
import org.hibernate.boot.archive.scan.spi.ScanEnvironment;
import org.hibernate.boot.archive.scan.spi.ScanOptions;
import org.hibernate.boot.archive.scan.spi.Scanner;
import org.hibernate.boot.archive.spi.ArchiveDescriptorFactory;
import org.hibernate.boot.cfgxml.spi.CfgXmlAccessService;
import org.hibernate.boot.cfgxml.spi.LoadedConfig;
import org.hibernate.boot.cfgxml.spi.MappingReference;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.boot.model.TypeContributor;
import org.hibernate.boot.model.naming.ImplicitNamingStrategy;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyLegacyJpaImpl;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl;
import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.selector.spi.StrategySelector;
import org.hibernate.boot.spi.MappingDefaults;
import org.hibernate.boot.spi.MetadataBuildingOptions;
import org.hibernate.boot.spi.MetadataSourcesContributor;
import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.MetadataSourceType;
import org.hibernate.cfg.annotations.reflection.JPAMetadataProvider;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.config.spi.StandardConverters;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.BasicType;
import org.hibernate.type.CompositeCustomType;
import org.hibernate.type.CustomType;
import org.hibernate.usertype.CompositeUserType;
import org.hibernate.usertype.UserType;

import org.jboss.jandex.IndexView;

import static org.hibernate.internal.log.DeprecationLogger.DEPRECATION_LOGGER;

/**
 * @author Steve Ebersole
 */
public class MetadataBuilderImpl implements MetadataBuilder, TypeContributions {
	private static final CoreMessageLogger log = CoreLogging.messageLogger( MetadataBuilderImpl.class );

	private final MetadataSources sources;
	private final MetadataBuildingOptionsImpl options;

	public MetadataBuilderImpl(MetadataSources sources) {
		this(
				sources,
				getStandardServiceRegistry( sources.getServiceRegistry() )
		);
	}

	private static StandardServiceRegistry getStandardServiceRegistry(ServiceRegistry serviceRegistry) {
		if ( serviceRegistry == null ) {
			throw new HibernateException( "ServiceRegistry passed to MetadataBuilder cannot be null" );
		}

		if ( StandardServiceRegistry.class.isInstance( serviceRegistry ) ) {
			return ( StandardServiceRegistry ) serviceRegistry;
		}
		else if ( BootstrapServiceRegistry.class.isInstance( serviceRegistry ) ) {
			log.debugf(
					"ServiceRegistry passed to MetadataBuilder was a BootstrapServiceRegistry; this likely wont end well" +
							"if attempt is made to build SessionFactory"
			);
			return new StandardServiceRegistryBuilder( (BootstrapServiceRegistry) serviceRegistry ).build();
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

		for ( MetadataSourcesContributor contributor :
				sources.getServiceRegistry()
						.getService( ClassLoaderService.class )
						.loadJavaServices( MetadataSourcesContributor.class ) ) {
			contributor.contribute( sources, null );
		}

		this.options = new MetadataBuildingOptionsImpl( serviceRegistry );

		applyCfgXmlValues( serviceRegistry.getService( CfgXmlAccessService.class ) );
	}

	private void applyCfgXmlValues(CfgXmlAccessService service) {
		final LoadedConfig aggregatedConfig = service.getAggregatedConfig();
		if ( aggregatedConfig == null ) {
			return;
		}

		for ( CacheRegionDefinition cacheRegionDefinition : aggregatedConfig.getCacheRegionDefinitions() ) {
			with( cacheRegionDefinition );
		}
	}

	@Override
	public MetadataBuilder withImplicitSchemaName(String implicitSchemaName) {
		options.mappingDefaults.implicitSchemaName = implicitSchemaName;
		return this;
	}

	@Override
	public MetadataBuilder withImplicitCatalogName(String implicitCatalogName) {
		options.mappingDefaults.implicitCatalogName = implicitCatalogName;
		return this;
	}

	@Override
	public MetadataBuilder with(ImplicitNamingStrategy namingStrategy) {
		this.options.implicitNamingStrategy = namingStrategy;
		return this;
	}

	@Override
	public MetadataBuilder with(PhysicalNamingStrategy namingStrategy) {
		this.options.physicalNamingStrategy = namingStrategy;
		return this;
	}

	@Override
	public MetadataBuilder with(ReflectionManager reflectionManager) {
		this.options.reflectionManager = reflectionManager;
		return this;
	}

	@Override
	public MetadataBuilder with(SharedCacheMode sharedCacheMode) {
		this.options.sharedCacheMode = sharedCacheMode;
		return this;
	}

	@Override
	public MetadataBuilder with(AccessType implicitCacheAccessType) {
		this.options.mappingDefaults.implicitCacheAccessType = implicitCacheAccessType;
		return this;
	}

	@Override
	public MetadataBuilder with(IndexView jandexView) {
		this.options.jandexView = jandexView;
		return this;
	}

	@Override
	public MetadataBuilder with(ScanOptions scanOptions) {
		this.options.scanOptions = scanOptions;
		return this;
	}

	@Override
	public MetadataBuilder with(ScanEnvironment scanEnvironment) {
		this.options.scanEnvironment = scanEnvironment;
		return this;
	}

	@Override
	public MetadataBuilder with(Scanner scanner) {
		this.options.scannerSetting = scanner;
		return this;
	}

	@Override
	public MetadataBuilder with(ArchiveDescriptorFactory factory) {
		this.options.archiveDescriptorFactory = factory;
		return this;
	}

	@Override
	public MetadataBuilder withNewIdentifierGeneratorsEnabled(boolean enabled) {
		this.options.useNewIdentifierGenerators = enabled;
		return this;
	}

	@Override
	public MetadataBuilder withExplicitDiscriminatorsForJoinedSubclassSupport(boolean supported) {
		options.explicitDiscriminatorsForJoinedInheritanceSupported = supported;
		return this;
	}

	@Override
	public MetadataBuilder withImplicitDiscriminatorsForJoinedSubclassSupport(boolean supported) {
		options.implicitDiscriminatorsForJoinedInheritanceSupported = supported;
		return this;
	}

	@Override
	public MetadataBuilder withImplicitForcingOfDiscriminatorsInSelect(boolean supported) {
		options.implicitlyForceDiscriminatorInSelect = supported;
		return this;
	}

	@Override
	public MetadataBuilder withNationalizedCharacterData(boolean enabled) {
		options.useNationalizedCharacterData = enabled;
		return this;
	}

	@Override
	public MetadataBuilder with(BasicType type) {
		options.basicTypeRegistrations.add( type );
		return this;
	}

	@Override
	public MetadataBuilder with(UserType type, String[] keys) {
		options.basicTypeRegistrations.add( new CustomType( type, keys ) );
		return this;
	}

	@Override
	public MetadataBuilder with(CompositeUserType type, String[] keys) {
		options.basicTypeRegistrations.add( new CompositeCustomType( type, keys ) );
		return this;
	}

	@Override
	public MetadataBuilder with(TypeContributor typeContributor) {
		typeContributor.contribute( this, options.serviceRegistry );
		return this;
	}

	@Override
	public void contributeType(BasicType type) {
		options.basicTypeRegistrations.add( type );
	}

	@Override
	public void contributeType(UserType type, String[] keys) {
		options.basicTypeRegistrations.add( new CustomType( type, keys ) );
	}

	@Override
	public void contributeType(CompositeUserType type, String[] keys) {
		options.basicTypeRegistrations.add( new CompositeCustomType( type, keys ) );
	}

	@Override
	public MetadataBuilder with(CacheRegionDefinition cacheRegionDefinition) {
		if ( options.cacheRegionDefinitions == null ) {
			options.cacheRegionDefinitions = new ArrayList<CacheRegionDefinition>();
		}
		options.cacheRegionDefinitions.add( cacheRegionDefinition );
		return this;
	}

	@Override
	public MetadataBuilder with(ClassLoader tempClassLoader) {
		options.tempClassLoader = tempClassLoader;
		return this;
	}

	@Override
	public MetadataBuilder setSourceProcessOrdering(List<MetadataSourceType> sourceProcessOrdering) {
		options.sourceProcessOrdering = sourceProcessOrdering;
		return this;
	}

	public MetadataBuilder allowSpecjSyntax() {
		this.options.specjProprietarySyntaxEnabled = true;
		return this;
	}

//	public MetadataBuilder with(PersistentAttributeMemberResolver resolver) {
//		options.persistentAttributeMemberResolver = resolver;
//		return this;
//	}

	@Override
	public MetadataImpl build() {
		final CfgXmlAccessService cfgXmlAccessService = options.serviceRegistry.getService( CfgXmlAccessService.class );
		if ( cfgXmlAccessService.getAggregatedConfig() != null ) {
			if ( cfgXmlAccessService.getAggregatedConfig().getMappingReferences() != null ) {
				for ( MappingReference mappingReference : cfgXmlAccessService.getAggregatedConfig().getMappingReferences() ) {
					mappingReference.apply( sources );
				}
			}
		}

		return MetadataBuildingProcess.build( sources, options );
	}

	public static class MappingDefaultsImpl implements MappingDefaults {
		private String implicitSchemaName;
		private String implicitCatalogName;
		private boolean implicitlyQuoteIdentifiers;

		private AccessType implicitCacheAccessType;

		public MappingDefaultsImpl(StandardServiceRegistry serviceRegistry) {
			final ConfigurationService configService = serviceRegistry.getService( ConfigurationService.class );

			this.implicitSchemaName = configService.getSetting(
					AvailableSettings.DEFAULT_SCHEMA,
					StandardConverters.STRING,
					null
			);

			this.implicitCatalogName = configService.getSetting(
					AvailableSettings.DEFAULT_CATALOG,
					StandardConverters.STRING,
					null
			);

			this.implicitlyQuoteIdentifiers = configService.getSetting(
					AvailableSettings.GLOBALLY_QUOTED_IDENTIFIERS,
					StandardConverters.BOOLEAN,
					false
			);

			this.implicitCacheAccessType = configService.getSetting(
					AvailableSettings.DEFAULT_CACHE_CONCURRENCY_STRATEGY,
					new ConfigurationService.Converter<AccessType>() {
						@Override
						public AccessType convert(Object value) {
							return AccessType.fromExternalName( value.toString() );
						}
					}
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
			// for now, just hard-code
			return false;
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
	}

	public static class MetadataBuildingOptionsImpl implements MetadataBuildingOptions {
		private final StandardServiceRegistry serviceRegistry;
		private final MappingDefaultsImpl mappingDefaults;

		private List<BasicType> basicTypeRegistrations = new ArrayList<BasicType>();

		private IndexView jandexView;
		private ClassLoader tempClassLoader;

		private ScanOptions scanOptions;
		private ScanEnvironment scanEnvironment;
		private Object scannerSetting;
		private ArchiveDescriptorFactory archiveDescriptorFactory;

		private ImplicitNamingStrategy implicitNamingStrategy;
		private PhysicalNamingStrategy physicalNamingStrategy;

		private ReflectionManager reflectionManager = generateDefaultReflectionManager();

		private SharedCacheMode sharedCacheMode;
		private AccessType defaultCacheAccessType;
		private boolean useNewIdentifierGenerators;
		private MultiTenancyStrategy multiTenancyStrategy;
		private List<CacheRegionDefinition> cacheRegionDefinitions;
		private boolean explicitDiscriminatorsForJoinedInheritanceSupported;
		private boolean implicitDiscriminatorsForJoinedInheritanceSupported;
		private boolean implicitlyForceDiscriminatorInSelect;
		private boolean useNationalizedCharacterData;
		private boolean specjProprietarySyntaxEnabled;
		private List<MetadataSourceType> sourceProcessOrdering;

		private static ReflectionManager generateDefaultReflectionManager() {
			final JavaReflectionManager reflectionManager = new JavaReflectionManager();
			reflectionManager.setMetadataProvider( new JPAMetadataProvider() );
			return reflectionManager;
		}
//		private PersistentAttributeMemberResolver persistentAttributeMemberResolver =
//				StandardPersistentAttributeMemberResolver.INSTANCE;

		public MetadataBuildingOptionsImpl(StandardServiceRegistry serviceRegistry) {
			this.serviceRegistry = serviceRegistry;

			final StrategySelector strategySelector = serviceRegistry.getService( StrategySelector.class );
			final ConfigurationService configService = serviceRegistry.getService( ConfigurationService.class );

			this.mappingDefaults = new MappingDefaultsImpl( serviceRegistry );

//			jandexView = (IndexView) configService.getSettings().get( AvailableSettings.JANDEX_INDEX );

			scanOptions = new StandardScanOptions(
					(String) configService.getSettings().get( AvailableSettings.SCANNER_DISCOVERY ),
					false
			);
			// ScanEnvironment must be set explicitly
			scannerSetting = configService.getSettings().get( AvailableSettings.SCANNER );
			if ( scannerSetting == null ) {
				scannerSetting = configService.getSettings().get( AvailableSettings.SCANNER_DEPRECATED );
				if ( scannerSetting != null ) {
					DEPRECATION_LOGGER.logDeprecatedScannerSetting();
				}
			}
			archiveDescriptorFactory = strategySelector.resolveStrategy(
					ArchiveDescriptorFactory.class,
					configService.getSettings().get( AvailableSettings.SCANNER_ARCHIVE_INTERPRETER )
			);

			useNewIdentifierGenerators = configService.getSetting(
					AvailableSettings.USE_NEW_ID_GENERATOR_MAPPINGS,
					StandardConverters.BOOLEAN,
					false
			);

			multiTenancyStrategy =  MultiTenancyStrategy.determineMultiTenancyStrategy( configService.getSettings() );

			implicitDiscriminatorsForJoinedInheritanceSupported = configService.getSetting(
					AvailableSettings.IMPLICIT_DISCRIMINATOR_COLUMNS_FOR_JOINED_SUBCLASS,
					StandardConverters.BOOLEAN,
					false
			);

			explicitDiscriminatorsForJoinedInheritanceSupported = !configService.getSetting(
					AvailableSettings.IGNORE_EXPLICIT_DISCRIMINATOR_COLUMNS_FOR_JOINED_SUBCLASS,
					StandardConverters.BOOLEAN,
					false
			);

			implicitlyForceDiscriminatorInSelect = configService.getSetting(
					AvailableSettings.FORCE_DISCRIMINATOR_IN_SELECTS_BY_DEFAULT,
					StandardConverters.BOOLEAN,
					false
			);

			sharedCacheMode = configService.getSetting(
					"javax.persistence.sharedCache.mode",
					new ConfigurationService.Converter<SharedCacheMode>() {
						@Override
						public SharedCacheMode convert(Object value) {
							if ( value == null ) {
								return null;
							}

							if ( SharedCacheMode.class.isInstance( value ) ) {
								return (SharedCacheMode) value;
							}

							return SharedCacheMode.valueOf( value.toString() );
						}
					},
					SharedCacheMode.UNSPECIFIED
			);

			defaultCacheAccessType = configService.getSetting(
					AvailableSettings.DEFAULT_CACHE_CONCURRENCY_STRATEGY,
					new ConfigurationService.Converter<AccessType>() {
						@Override
						public AccessType convert(Object value) {
							if ( value == null ) {
								return null;
							}

							if ( CacheConcurrencyStrategy.class.isInstance( value ) ) {
								return ( (CacheConcurrencyStrategy) value ).toAccessType();
							}

							if ( AccessType.class.isInstance( value ) ) {
								return (AccessType) value;
							}

							return AccessType.fromExternalName( value.toString() );
						}
					},
					// by default, see if the defined RegionFactory (if one) defines a default
					serviceRegistry.getService( RegionFactory.class ) == null
							? null
							: serviceRegistry.getService( RegionFactory.class ).getDefaultAccessType()
			);

			specjProprietarySyntaxEnabled = configService.getSetting(
					"hibernate.enable_specj_proprietary_syntax",
					StandardConverters.BOOLEAN,
					false
			);

			implicitNamingStrategy = strategySelector.resolveDefaultableStrategy(
					ImplicitNamingStrategy.class,
					configService.getSettings().get( AvailableSettings.IMPLICIT_NAMING_STRATEGY ),
					ImplicitNamingStrategyLegacyJpaImpl.INSTANCE
			);

			physicalNamingStrategy = strategySelector.resolveDefaultableStrategy(
					PhysicalNamingStrategy.class,
					configService.getSettings().get( AvailableSettings.PHYSICAL_NAMING_STRATEGY ),
					PhysicalNamingStrategyStandardImpl.INSTANCE
			);

			sourceProcessOrdering = resolveInitialSourceProcessOrdering( configService );
		}

		private List<MetadataSourceType> resolveInitialSourceProcessOrdering(ConfigurationService configService) {
			List<MetadataSourceType> initialSelections = null;

			final String sourceProcessOrderingSetting = configService.getSetting(
					AvailableSettings.ARTIFACT_PROCESSING_ORDER,
					StandardConverters.STRING
			);
			if ( sourceProcessOrderingSetting != null ) {
				final String[] orderChoices = StringHelper.split( ",; ", sourceProcessOrderingSetting, false );
				initialSelections = CollectionHelper.arrayList( orderChoices.length );
				for ( String orderChoice : orderChoices ) {
					initialSelections.add( MetadataSourceType.parsePrecedence( orderChoice ) );
				}
			}
			if ( initialSelections == null || initialSelections.isEmpty() ) {
				initialSelections = Arrays.asList(  MetadataSourceType.HBM, MetadataSourceType.CLASS );
			}

			return initialSelections;
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
		public List<BasicType> getBasicTypeRegistrations() {
			return basicTypeRegistrations;
		}

		@Override
		public IndexView getJandexView() {
			return jandexView;
		}

		@Override
		public ScanOptions getScanOptions() {
			return scanOptions;
		}

		@Override
		public ScanEnvironment getScanEnvironment() {
			return scanEnvironment;
		}

		@Override
		public Object getScanner() {
			return scannerSetting;
		}

		@Override
		public ArchiveDescriptorFactory getArchiveDescriptorFactory() {
			return archiveDescriptorFactory;
		}

		@Override
		public ClassLoader getTempClassLoader() {
			return tempClassLoader;
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
		public ReflectionManager getReflectionManager() {
			return reflectionManager;
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
		public boolean isUseNewIdentifierGenerators() {
			return useNewIdentifierGenerators;
		}

		@Override
		public MultiTenancyStrategy getMultiTenancyStrategy() {
			return multiTenancyStrategy;
		}

		@Override
		public List<CacheRegionDefinition> getCacheRegionDefinitions() {
			return cacheRegionDefinitions;
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
		public boolean isSpecjProprietarySyntaxEnabled() {
			return specjProprietarySyntaxEnabled;
		}

		@Override
		public List<MetadataSourceType> getSourceProcessOrdering() {
			return sourceProcessOrdering;
		}

		public static interface JpaOrmXmlPersistenceUnitDefaults {
			public String getDefaultSchemaName();
			public String getDefaultCatalogName();
			public boolean shouldImplicitlyQuoteIdentifiers();
		}

		/**
		 * Yuck.  This is needed because JPA lets users define "global building options"
		 * in {@code orm.xml} mappings.  Forget that there are generally multiple
		 * {@code orm.xml} mappings if using XML approach...  Ugh
		 */
		public void apply(JpaOrmXmlPersistenceUnitDefaults jpaOrmXmlPersistenceUnitDefaults) {
			if ( !mappingDefaults.shouldImplicitlyQuoteIdentifiers() ) {
				mappingDefaults.implicitlyQuoteIdentifiers = jpaOrmXmlPersistenceUnitDefaults.shouldImplicitlyQuoteIdentifiers();
			}

			if ( mappingDefaults.getImplicitCatalogName() == null ) {
				mappingDefaults.implicitCatalogName = StringHelper.nullIfEmpty(
						jpaOrmXmlPersistenceUnitDefaults.getDefaultCatalogName()
				);
			}

			if ( mappingDefaults.getImplicitSchemaName() == null ) {
				mappingDefaults.implicitSchemaName = StringHelper.nullIfEmpty(
						jpaOrmXmlPersistenceUnitDefaults.getDefaultSchemaName()
				);
			}
		}

		//		@Override
//		public PersistentAttributeMemberResolver getPersistentAttributeMemberResolver() {
//			return persistentAttributeMemberResolver;
//		}
	}
}
