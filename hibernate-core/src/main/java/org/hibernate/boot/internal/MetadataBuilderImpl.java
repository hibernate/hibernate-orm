/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import javax.persistence.AttributeConverter;
import javax.persistence.SharedCacheMode;

import org.hibernate.HibernateException;
import org.hibernate.MultiTenancyStrategy;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.boot.AttributeConverterInfo;
import org.hibernate.boot.CacheRegionDefinition;
import org.hibernate.boot.MetadataBuilder;
import org.hibernate.boot.MetadataSources;
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
import org.hibernate.boot.model.convert.internal.ClassBasedConverterDescriptor;
import org.hibernate.boot.model.convert.internal.InstanceBasedConverterDescriptor;
import org.hibernate.boot.model.convert.spi.ConverterDescriptor;
import org.hibernate.boot.model.naming.ImplicitNamingStrategy;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl;
import org.hibernate.boot.model.process.spi.MetadataBuildingProcess;
import org.hibernate.boot.model.relational.AuxiliaryDatabaseObject;
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
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.spi.MetadataBuildingOptions;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.boot.spi.MetadataSourcesContributor;
import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cfg.AttributeConverterDefinition;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.MetadataSourceType;
import org.hibernate.dialect.function.SQLFunction;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.config.spi.StandardConverters;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;
import org.hibernate.type.spi.TypeConfiguration;
import org.hibernate.usertype.CompositeUserType;
import org.hibernate.usertype.UserType;

import org.jboss.jandex.IndexView;

/**
 * @author Steve Ebersole
 */
public class MetadataBuilderImpl implements MetadataBuilderImplementor, TypeContributions {
	private static final CoreMessageLogger log = CoreLogging.messageLogger( MetadataBuilderImpl.class );

	private final MetadataSources sources;
	private final BootstrapContextImpl bootstrapContext;
	private final MetadataBuildingOptionsImpl options;

	public MetadataBuilderImpl(MetadataSources sources) {
		this( sources, getStandardServiceRegistry( sources.getServiceRegistry() ) );
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
		this.bootstrapContext = new BootstrapContextImpl( serviceRegistry, options );
		//this is needed only fro implementig deprecated method
		options.setBootstrapContext( bootstrapContext );

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
		this.bootstrapContext.injectJandexView( jandexView );
		return this;
	}

	@Override
	public MetadataBuilder applyScanOptions(ScanOptions scanOptions) {
		this.bootstrapContext.injectScanOptions( scanOptions );
		return this;
	}

	@Override
	public MetadataBuilder applyScanEnvironment(ScanEnvironment scanEnvironment) {
		this.bootstrapContext.injectScanEnvironment( scanEnvironment );
		return this;
	}

	@Override
	public MetadataBuilder applyScanner(Scanner scanner) {
		this.bootstrapContext.injectScanner( scanner );
		return this;
	}

	@Override
	public MetadataBuilder applyArchiveDescriptorFactory(ArchiveDescriptorFactory factory) {
		this.bootstrapContext.injectArchiveDescriptorFactory( factory );
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
		options.basicTypeRegistrations.add( new BasicTypeRegistration( type ) );
		return this;
	}

	@Override
	public MetadataBuilder applyBasicType(BasicType type, String... keys) {
		options.basicTypeRegistrations.add( new BasicTypeRegistration( type, keys ) );
		return this;
	}

	@Override
	public MetadataBuilder applyBasicType(UserType type, String... keys) {
		options.basicTypeRegistrations.add( new BasicTypeRegistration( type, keys ) );
		return this;
	}

	@Override
	public MetadataBuilder applyBasicType(CompositeUserType type, String... keys) {
		options.basicTypeRegistrations.add( new BasicTypeRegistration( type, keys ) );
		return this;
	}

	@Override
	public MetadataBuilder applyTypes(TypeContributor typeContributor) {
		typeContributor.contribute( this, options.serviceRegistry );
		return this;
	}

	@Override
	public void contributeType(BasicType type) {
		options.basicTypeRegistrations.add( new BasicTypeRegistration( type ) );
	}

	@Override
	public void contributeType(BasicType type, String... keys) {
		options.basicTypeRegistrations.add( new BasicTypeRegistration( type, keys ) );
	}

	@Override
	public void contributeType(UserType type, String[] keys) {
		options.basicTypeRegistrations.add( new BasicTypeRegistration( type, keys ) );
	}

	@Override
	public void contributeType(CompositeUserType type, String[] keys) {
		options.basicTypeRegistrations.add( new BasicTypeRegistration( type, keys ) );
	}

	@Override
	public void contributeJavaTypeDescriptor(JavaTypeDescriptor descriptor) {
		this.bootstrapContext.getTypeConfiguration().getJavaTypeDescriptorRegistry().addDescriptor( descriptor );
	}

	@Override
	public void contributeSqlTypeDescriptor(SqlTypeDescriptor descriptor) {
		this.bootstrapContext.getTypeConfiguration().getSqlTypeDescriptorRegistry().addDescriptor( descriptor );
	}

	@Override
	public TypeConfiguration getTypeConfiguration() {
		return bootstrapContext.getTypeConfiguration();
	}

	@Override
	public MetadataBuilder applyCacheRegionDefinition(CacheRegionDefinition cacheRegionDefinition) {
		this.bootstrapContext.addCacheRegionDefinition( cacheRegionDefinition );
		return this;
	}

	@Override
	public MetadataBuilder applyTempClassLoader(ClassLoader tempClassLoader) {
		this.bootstrapContext.injectJpaTempClassLoader( tempClassLoader );
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
		this.bootstrapContext.addSqlFunction( functionName, function );
		return this;
	}

	@Override
	public MetadataBuilder applyAuxiliaryDatabaseObject(AuxiliaryDatabaseObject auxiliaryDatabaseObject) {
		this.bootstrapContext.addAuxiliaryDatabaseObject( auxiliaryDatabaseObject );
		return this;
	}

	@Override
	public MetadataBuilder applyAttributeConverter(AttributeConverterDefinition definition) {
		this.bootstrapContext.addAttributeConverterInfo( definition );
		return this;
	}

	@Override
	public MetadataBuilder applyAttributeConverter(Class<? extends AttributeConverter> attributeConverterClass) {
		this.bootstrapContext.addAttributeConverterInfo(
				new AttributeConverterInfo() {
					@Override
					public Class<? extends AttributeConverter> getConverterClass() {
						return attributeConverterClass;
					}

					@Override
					public ConverterDescriptor toConverterDescriptor(MetadataBuildingContext context) {
						return new ClassBasedConverterDescriptor(
								attributeConverterClass,
								null,
								context.getBootstrapContext().getClassmateContext()
						);
					}
				}
		);
		return this;
	}

	@Override
	public MetadataBuilder applyAttributeConverter(Class<? extends AttributeConverter> attributeConverterClass, boolean autoApply) {
		this.bootstrapContext.addAttributeConverterInfo(
				new AttributeConverterInfo() {
					@Override
					public Class<? extends AttributeConverter> getConverterClass() {
						return attributeConverterClass;
					}

					@Override
					public ConverterDescriptor toConverterDescriptor(MetadataBuildingContext context) {
						return new ClassBasedConverterDescriptor(
								attributeConverterClass,
								autoApply,
								context.getBootstrapContext().getClassmateContext()
						);
					}
				}
		);
		return this;
	}

	@Override
	public MetadataBuilder applyAttributeConverter(AttributeConverter attributeConverter) {
		this.bootstrapContext.addAttributeConverterInfo(
				new AttributeConverterInfo() {
					@Override
					public Class<? extends AttributeConverter> getConverterClass() {
						return attributeConverter.getClass();
					}

					@Override
					public ConverterDescriptor toConverterDescriptor(MetadataBuildingContext context) {
						return new InstanceBasedConverterDescriptor(
								attributeConverter,
								null,
								context.getBootstrapContext().getClassmateContext()
						);
					}
				}
		);
		return this;
	}

	@Override
	public MetadataBuilder applyAttributeConverter(AttributeConverter attributeConverter, boolean autoApply) {
		this.bootstrapContext.addAttributeConverterInfo(
				new AttributeConverterInfo() {
					@Override
					public Class<? extends AttributeConverter> getConverterClass() {
						return attributeConverter.getClass();
					}

					@Override
					public ConverterDescriptor toConverterDescriptor(MetadataBuildingContext context) {
						return new InstanceBasedConverterDescriptor(
								attributeConverter,
								autoApply,
								context.getBootstrapContext().getClassmateContext()
						);
					}
				}
		);
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

		return MetadataBuildingProcess.build( sources, bootstrapContext, options );
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
		// todo (6.0) : remove bootstrapContext property along with the deprecated methods
		private BootstrapContext bootstrapContext;

		private ArrayList<BasicTypeRegistration> basicTypeRegistrations = new ArrayList<>();

		private ImplicitNamingStrategy implicitNamingStrategy;
		private PhysicalNamingStrategy physicalNamingStrategy;

		private SharedCacheMode sharedCacheMode;
		private AccessType defaultCacheAccessType;
		private MultiTenancyStrategy multiTenancyStrategy;
		private boolean explicitDiscriminatorsForJoinedInheritanceSupported;
		private boolean implicitDiscriminatorsForJoinedInheritanceSupported;
		private boolean implicitlyForceDiscriminatorInSelect;
		private boolean useNationalizedCharacterData;
		private boolean specjProprietarySyntaxEnabled;
		private ArrayList<MetadataSourceType> sourceProcessOrdering;

		private IdGeneratorInterpreterImpl idGenerationTypeInterpreter = new IdGeneratorInterpreterImpl();

		private String schemaCharset;
		private boolean xmlMappingEnabled;

		public MetadataBuildingOptionsImpl(StandardServiceRegistry serviceRegistry) {
			this.serviceRegistry = serviceRegistry;

			final StrategySelector strategySelector = serviceRegistry.getService( StrategySelector.class );
			final ConfigurationService configService = serviceRegistry.getService( ConfigurationService.class );

			this.mappingDefaults = new MappingDefaultsImpl( serviceRegistry );

			this.multiTenancyStrategy =  MultiTenancyStrategy.determineMultiTenancyStrategy( configService.getSettings() );

			this.xmlMappingEnabled = configService.getSetting(
					AvailableSettings.XML_MAPPING_ENABLED,
					StandardConverters.BOOLEAN,
					true
			);

			this.implicitDiscriminatorsForJoinedInheritanceSupported = configService.getSetting(
					AvailableSettings.IMPLICIT_DISCRIMINATOR_COLUMNS_FOR_JOINED_SUBCLASS,
					StandardConverters.BOOLEAN,
					false
			);

			this.explicitDiscriminatorsForJoinedInheritanceSupported = !configService.getSetting(
					AvailableSettings.IGNORE_EXPLICIT_DISCRIMINATOR_COLUMNS_FOR_JOINED_SUBCLASS,
					StandardConverters.BOOLEAN,
					false
			);

			this.implicitlyForceDiscriminatorInSelect = configService.getSetting(
					AvailableSettings.FORCE_DISCRIMINATOR_IN_SELECTS_BY_DEFAULT,
					StandardConverters.BOOLEAN,
					false
			);

			this.sharedCacheMode = configService.getSetting(
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

			this.defaultCacheAccessType = configService.getSetting(
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

			this.specjProprietarySyntaxEnabled = configService.getSetting(
					"hibernate.enable_specj_proprietary_syntax",
					StandardConverters.BOOLEAN,
					false
			);

			this.implicitNamingStrategy = strategySelector.resolveDefaultableStrategy(
					ImplicitNamingStrategy.class,
					configService.getSettings().get( AvailableSettings.IMPLICIT_NAMING_STRATEGY ),
					new Callable<ImplicitNamingStrategy>() {
						@Override
						public ImplicitNamingStrategy call() {
							return strategySelector.resolveDefaultableStrategy(
									ImplicitNamingStrategy.class,
									"default",
									ImplicitNamingStrategyJpaCompliantImpl.INSTANCE
							);
						}
					}
			);

			this.physicalNamingStrategy = strategySelector.resolveDefaultableStrategy(
					PhysicalNamingStrategy.class,
					configService.getSettings().get( AvailableSettings.PHYSICAL_NAMING_STRATEGY ),
					PhysicalNamingStrategyStandardImpl.INSTANCE
			);

			this.sourceProcessOrdering = resolveInitialSourceProcessOrdering( configService );

			final boolean useNewIdentifierGenerators = configService.getSetting(
					AvailableSettings.USE_NEW_ID_GENERATOR_MAPPINGS,
					StandardConverters.BOOLEAN,
					true
			);
			if ( useNewIdentifierGenerators ) {
				this.idGenerationTypeInterpreter.disableLegacyFallback();
			}
			else {
				this.idGenerationTypeInterpreter.enableLegacyFallback();
			}

			this.useNationalizedCharacterData = configService.getSetting(
					AvailableSettings.USE_NATIONALIZED_CHARACTER_DATA,
					StandardConverters.BOOLEAN,
					false
			);

			this.schemaCharset = configService.getSetting(
					AvailableSettings.HBM2DDL_CHARSET_NAME,
					String.class,
					null
			);
		}

		private ArrayList<MetadataSourceType> resolveInitialSourceProcessOrdering(ConfigurationService configService) {
			final ArrayList<MetadataSourceType> initialSelections = new ArrayList<>();

			final String sourceProcessOrderingSetting = configService.getSetting(
					AvailableSettings.ARTIFACT_PROCESSING_ORDER,
					StandardConverters.STRING
			);
			if ( sourceProcessOrderingSetting != null ) {
				final String[] orderChoices = StringHelper.split( ",; ", sourceProcessOrderingSetting, false );
				initialSelections.addAll( CollectionHelper.arrayList( orderChoices.length ) );
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

		@Override
		public StandardServiceRegistry getServiceRegistry() {
			return serviceRegistry;
		}

		@Override
		public MappingDefaults getMappingDefaults() {
			return mappingDefaults;
		}

		@Override
		public List<BasicTypeRegistration> getBasicTypeRegistrations() {
			return basicTypeRegistrations;
		}

		@Override
		public ReflectionManager getReflectionManager() {
			return bootstrapContext.getReflectionManager();
		}

		@Override
		public IndexView getJandexView() {
			return bootstrapContext.getJandexView();
		}

		@Override
		public ScanOptions getScanOptions() {
			return bootstrapContext.getScanOptions();
		}

		@Override
		public ScanEnvironment getScanEnvironment() {
			return bootstrapContext.getScanEnvironment();
		}

		@Override
		public Object getScanner() {
			return bootstrapContext.getScanner();
		}

		@Override
		public ArchiveDescriptorFactory getArchiveDescriptorFactory() {
			return bootstrapContext.getArchiveDescriptorFactory();
		}

		@Override
		public ClassLoader getTempClassLoader() {
			return bootstrapContext.getJpaTempClassLoader();
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
			return new ArrayList<>( bootstrapContext.getCacheRegionDefinitions() );
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
			return bootstrapContext.getSqlFunctions();
		}

		@Override
		public List<AuxiliaryDatabaseObject> getAuxiliaryDatabaseObjectList() {
			return new ArrayList<>( bootstrapContext.getAuxiliaryDatabaseObjectList());
		}

		@Override
		public List<AttributeConverterInfo> getAttributeConverters() {
			return new ArrayList<>( bootstrapContext.getAttributeConverters() );
		}

		@Override
		public String getSchemaCharset() {
			return schemaCharset;
		}

		@Override
		public boolean isXmlMappingEnabled() {
			return xmlMappingEnabled;
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

		public void setBootstrapContext(BootstrapContextImpl bootstrapContext) {
			this.bootstrapContext = bootstrapContext;
		}
	}
}
