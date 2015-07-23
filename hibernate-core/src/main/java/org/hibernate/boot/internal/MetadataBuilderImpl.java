/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.AttributeConverter;
import javax.persistence.SharedCacheMode;

import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.MultiTenancyStrategy;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.common.reflection.ClassLoaderDelegate;
import org.hibernate.annotations.common.reflection.ClassLoadingException;
import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.annotations.common.reflection.java.JavaReflectionManager;
import org.hibernate.annotations.common.util.StandardClassLoaderDelegateImpl;
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
import org.hibernate.boot.model.IdGeneratorStrategyInterpreter;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.boot.model.TypeContributor;
import org.hibernate.boot.model.naming.ImplicitNamingStrategy;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyLegacyJpaImpl;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl;
import org.hibernate.boot.model.process.spi.MetadataBuildingProcess;
import org.hibernate.boot.model.relational.AuxiliaryDatabaseObject;
import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.selector.spi.StrategySelector;
import org.hibernate.boot.spi.JpaOrmXmlPersistenceUnitDefaultAware;
import org.hibernate.boot.spi.MappingDefaults;
import org.hibernate.boot.spi.MetadataBuilderImplementor;
import org.hibernate.boot.spi.MetadataBuilderInitializer;
import org.hibernate.boot.spi.MetadataBuildingOptions;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.boot.spi.MetadataSourcesContributor;
import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cfg.AttributeConverterDefinition;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.MetadataSourceType;
import org.hibernate.cfg.annotations.reflection.JPAMetadataProvider;
import org.hibernate.dialect.function.SQLFunction;
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
public class MetadataBuilderImpl implements MetadataBuilderImplementor, TypeContributions {
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
		this.options = new MetadataBuildingOptionsImpl( serviceRegistry );

		for ( MetadataSourcesContributor contributor :
				sources.getServiceRegistry()
						.getService( ClassLoaderService.class )
						.loadJavaServices( MetadataSourcesContributor.class ) ) {
			contributor.contribute( sources );
		}

		// todo : not so sure this is needed anymore.
		//		these should be set during the StandardServiceRegistryBuilder.configure call
		applyCfgXmlValues( serviceRegistry.getService( CfgXmlAccessService.class ) );

		final ClassLoaderService classLoaderService = serviceRegistry.getService( ClassLoaderService.class );
		for ( MetadataBuilderInitializer contributor : classLoaderService.loadJavaServices( MetadataBuilderInitializer.class ) ) {
			contributor.contribute( this, serviceRegistry );
		}
	}

	private void applyCfgXmlValues(CfgXmlAccessService service) {
		final LoadedConfig aggregatedConfig = service.getAggregatedConfig();
		if ( aggregatedConfig == null ) {
			return;
		}

		for ( CacheRegionDefinition cacheRegionDefinition : aggregatedConfig.getCacheRegionDefinitions() ) {
			applyCacheRegionDefinition( cacheRegionDefinition );
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
		this.options.implicitNamingStrategy = namingStrategy;
		return this;
	}

	@Override
	public MetadataBuilder applyPhysicalNamingStrategy(PhysicalNamingStrategy namingStrategy) {
		this.options.physicalNamingStrategy = namingStrategy;
		return this;
	}

	@Override
	public MetadataBuilder applyReflectionManager(ReflectionManager reflectionManager) {
		this.options.reflectionManager = reflectionManager;
		this.options.reflectionManager.injectClassLoaderDelegate( this.options.getHcannClassLoaderDelegate() );
		return this;
	}

	@Override
	public MetadataBuilder applySharedCacheMode(SharedCacheMode sharedCacheMode) {
		this.options.sharedCacheMode = sharedCacheMode;
		return this;
	}

	@Override
	public MetadataBuilder applyAccessType(AccessType implicitCacheAccessType) {
		this.options.mappingDefaults.implicitCacheAccessType = implicitCacheAccessType;
		return this;
	}

	@Override
	public MetadataBuilder applyIndexView(IndexView jandexView) {
		this.options.jandexView = jandexView;
		return this;
	}

	@Override
	public MetadataBuilder applyScanOptions(ScanOptions scanOptions) {
		this.options.scanOptions = scanOptions;
		return this;
	}

	@Override
	public MetadataBuilder applyScanEnvironment(ScanEnvironment scanEnvironment) {
		this.options.scanEnvironment = scanEnvironment;
		return this;
	}

	@Override
	public MetadataBuilder applyScanner(Scanner scanner) {
		this.options.scannerSetting = scanner;
		return this;
	}

	@Override
	public MetadataBuilder applyArchiveDescriptorFactory(ArchiveDescriptorFactory factory) {
		this.options.archiveDescriptorFactory = factory;
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
	public MetadataBuilder applyBasicType(BasicType type) {
		options.basicTypeRegistrations.add( type );
		return this;
	}

	@Override
	public MetadataBuilder applyBasicType(UserType type, String[] keys) {
		options.basicTypeRegistrations.add( new CustomType( type, keys ) );
		return this;
	}

	@Override
	public MetadataBuilder applyBasicType(CompositeUserType type, String[] keys) {
		options.basicTypeRegistrations.add( new CompositeCustomType( type, keys ) );
		return this;
	}

	@Override
	public MetadataBuilder applyTypes(TypeContributor typeContributor) {
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
	public MetadataBuilder applyCacheRegionDefinition(CacheRegionDefinition cacheRegionDefinition) {
		if ( options.cacheRegionDefinitions == null ) {
			options.cacheRegionDefinitions = new ArrayList<CacheRegionDefinition>();
		}
		options.cacheRegionDefinitions.add( cacheRegionDefinition );
		return this;
	}

	@Override
	public MetadataBuilder applyTempClassLoader(ClassLoader tempClassLoader) {
		options.tempClassLoader = tempClassLoader;
		return this;
	}

	@Override
	public MetadataBuilder applySourceProcessOrdering(MetadataSourceType... sourceTypes) {
		options.sourceProcessOrdering.addAll( Arrays.asList( sourceTypes ) );
		return this;
	}

	public MetadataBuilder allowSpecjSyntax() {
		this.options.specjProprietarySyntaxEnabled = true;
		return this;
	}


	@Override
	public MetadataBuilder applySqlFunction(String functionName, SQLFunction function) {
		if ( this.options.sqlFunctionMap == null ) {
			this.options.sqlFunctionMap = new HashMap<String, SQLFunction>();
		}
		this.options.sqlFunctionMap.put( functionName, function );
		return this;
	}

	@Override
	public MetadataBuilder applyAuxiliaryDatabaseObject(AuxiliaryDatabaseObject auxiliaryDatabaseObject) {
		if ( this.options.auxiliaryDatabaseObjectList == null ) {
			this.options.auxiliaryDatabaseObjectList = new ArrayList<AuxiliaryDatabaseObject>();
		}
		this.options.auxiliaryDatabaseObjectList.add( auxiliaryDatabaseObject );
		return this;
	}

	@Override
	public MetadataBuilder applyAttributeConverter(AttributeConverterDefinition definition) {
		this.options.addAttributeConverterDefinition( definition );
		return this;
	}

	@Override
	public MetadataBuilder applyAttributeConverter(Class<? extends AttributeConverter> attributeConverterClass) {
		applyAttributeConverter( AttributeConverterDefinition.from( attributeConverterClass ) );
		return this;
	}

	@Override
	public MetadataBuilder applyAttributeConverter(Class<? extends AttributeConverter> attributeConverterClass, boolean autoApply) {
		applyAttributeConverter( AttributeConverterDefinition.from( attributeConverterClass, autoApply ) );
		return this;
	}

	@Override
	public MetadataBuilder applyAttributeConverter(AttributeConverter attributeConverter) {
		applyAttributeConverter( AttributeConverterDefinition.from( attributeConverter ) );
		return this;
	}

	@Override
	public MetadataBuilder applyAttributeConverter(AttributeConverter attributeConverter, boolean autoApply) {
		applyAttributeConverter( AttributeConverterDefinition.from( attributeConverter, autoApply ) );
		return this;
	}

	@Override
	public MetadataBuilder enableNewIdentifierGeneratorSupport(boolean enabled) {
		if ( enabled ) {
			this.options.idGenerationTypeInterpreter.disableLegacyFallback();
		}
		else {
			this.options.idGenerationTypeInterpreter.enableLegacyFallback();
		}
		return this;
	}

	@Override
	public MetadataBuilder applyIdGenerationTypeInterpreter(IdGeneratorStrategyInterpreter interpreter) {
		this.options.idGenerationTypeInterpreter.addInterpreterDelegate( interpreter );
		return this;
	}

//	public MetadataBuilder with(PersistentAttributeMemberResolver resolver) {
//		options.persistentAttributeMemberResolver = resolver;
//		return this;
//	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends MetadataBuilder> T unwrap(Class<T> type) {
		return (T) this;
	}

	@Override
	public MetadataImplementor build() {
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

	@Override
	public MetadataBuildingOptions getMetadataBuildingOptions() {
		return options;
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

	public static class MetadataBuildingOptionsImpl
			implements MetadataBuildingOptions, JpaOrmXmlPersistenceUnitDefaultAware {
		private final StandardServiceRegistry serviceRegistry;
		private final MappingDefaultsImpl mappingDefaults;

		private ArrayList<BasicType> basicTypeRegistrations = new ArrayList<BasicType>();

		private IndexView jandexView;
		private ClassLoader tempClassLoader;

		private ScanOptions scanOptions;
		private ScanEnvironment scanEnvironment;
		private Object scannerSetting;
		private ArchiveDescriptorFactory archiveDescriptorFactory;

		private ImplicitNamingStrategy implicitNamingStrategy;
		private PhysicalNamingStrategy physicalNamingStrategy;

		private ReflectionManager reflectionManager;
		private ClassLoaderDelegate hcannClassLoaderDelegate;

		private SharedCacheMode sharedCacheMode;
		private AccessType defaultCacheAccessType;
		private MultiTenancyStrategy multiTenancyStrategy;
		private ArrayList<CacheRegionDefinition> cacheRegionDefinitions;
		private boolean explicitDiscriminatorsForJoinedInheritanceSupported;
		private boolean implicitDiscriminatorsForJoinedInheritanceSupported;
		private boolean implicitlyForceDiscriminatorInSelect;
		private boolean useNationalizedCharacterData;
		private boolean specjProprietarySyntaxEnabled;
		private ArrayList<MetadataSourceType> sourceProcessOrdering;

		private HashMap<String,SQLFunction> sqlFunctionMap;
		private ArrayList<AuxiliaryDatabaseObject> auxiliaryDatabaseObjectList;
		private HashMap<Class,AttributeConverterDefinition> attributeConverterDefinitionsByClass;

		private IdGeneratorInterpreterImpl idGenerationTypeInterpreter = new IdGeneratorInterpreterImpl();

		private boolean autoQuoteKeywords;

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

			final boolean useNewIdentifierGenerators = configService.getSetting(
					AvailableSettings.USE_NEW_ID_GENERATOR_MAPPINGS,
					StandardConverters.BOOLEAN,
					false
			);
			if ( useNewIdentifierGenerators ) {
				idGenerationTypeInterpreter.disableLegacyFallback();
			}
			else {
				idGenerationTypeInterpreter.enableLegacyFallback();
			}

			reflectionManager = generateDefaultReflectionManager();
		}

		private ArrayList<MetadataSourceType> resolveInitialSourceProcessOrdering(ConfigurationService configService) {
			final ArrayList<MetadataSourceType> initialSelections = new ArrayList<MetadataSourceType>();

			final String sourceProcessOrderingSetting = configService.getSetting(
					AvailableSettings.ARTIFACT_PROCESSING_ORDER,
					StandardConverters.STRING
			);
			if ( sourceProcessOrderingSetting != null ) {
				final String[] orderChoices = StringHelper.split( ",; ", sourceProcessOrderingSetting, false );
				initialSelections.addAll( CollectionHelper.<MetadataSourceType>arrayList( orderChoices.length ) );
				for ( String orderChoice : orderChoices ) {
					initialSelections.add( MetadataSourceType.parsePrecedence( orderChoice ) );
				}
			}
			if ( initialSelections.isEmpty() ) {
				initialSelections.add( MetadataSourceType.HBM );
				initialSelections.add( MetadataSourceType.CLASS );
			}

			return initialSelections;
		}

		private ReflectionManager generateDefaultReflectionManager() {
			final JavaReflectionManager reflectionManager = new JavaReflectionManager();
			reflectionManager.setMetadataProvider( new JPAMetadataProvider( this ) );
			reflectionManager.injectClassLoaderDelegate( getHcannClassLoaderDelegate() );
			return reflectionManager;
		}

		public ClassLoaderDelegate getHcannClassLoaderDelegate() {
			if ( hcannClassLoaderDelegate == null ) {
				hcannClassLoaderDelegate = new ClassLoaderDelegate() {
					private final  ClassLoaderService classLoaderService = getServiceRegistry().getService( ClassLoaderService.class );

					@Override
					public <T> Class<T> classForName(String className) throws ClassLoadingException {
						try {
							return classLoaderService.classForName( className );
						}
						catch (org.hibernate.boot.registry.classloading.spi.ClassLoadingException e) {
							return StandardClassLoaderDelegateImpl.INSTANCE.classForName( className );
						}
					}
				};
			}
			return hcannClassLoaderDelegate;
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
		public MultiTenancyStrategy getMultiTenancyStrategy() {
			return multiTenancyStrategy;
		}

		@Override
		public IdGeneratorStrategyInterpreter getIdGenerationTypeInterpreter() {
			return idGenerationTypeInterpreter;
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

		@Override
		public Map<String, SQLFunction> getSqlFunctions() {
			return sqlFunctionMap == null ? Collections.<String, SQLFunction>emptyMap() : sqlFunctionMap;
		}

		@Override
		public List<AuxiliaryDatabaseObject> getAuxiliaryDatabaseObjectList() {
			return auxiliaryDatabaseObjectList == null
					? Collections.<AuxiliaryDatabaseObject>emptyList()
					: auxiliaryDatabaseObjectList;
		}

		@Override
		public List<AttributeConverterDefinition> getAttributeConverters() {
			return attributeConverterDefinitionsByClass == null
					? Collections.<AttributeConverterDefinition>emptyList()
					: new ArrayList<AttributeConverterDefinition>( attributeConverterDefinitionsByClass.values() );
		}

		public void addAttributeConverterDefinition(AttributeConverterDefinition definition) {
			if ( this.attributeConverterDefinitionsByClass == null ) {
				this.attributeConverterDefinitionsByClass = new HashMap<Class, AttributeConverterDefinition>();
			}

			final Object old = this.attributeConverterDefinitionsByClass.put( definition.getAttributeConverter().getClass(), definition );

			if ( old != null ) {
				throw new AssertionFailure(
						String.format(
								"AttributeConverter class [%s] registered multiple times",
								definition.getAttributeConverter().getClass()
						)
				);
			}
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
