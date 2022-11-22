/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;

import org.hibernate.AnnotationException;
import org.hibernate.HibernateException;
import org.hibernate.TimeZoneStorageStrategy;
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
import org.hibernate.boot.spi.MetadataBuildingOptions;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.boot.spi.MetadataSourcesContributor;
import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.MetadataSourceType;
import org.hibernate.dialect.TimeZoneSupport;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.config.spi.StandardConverters;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.id.factory.IdentifierGeneratorFactory;
import org.hibernate.id.factory.internal.StandardIdentifierGeneratorFactory;
import org.hibernate.id.factory.spi.MutableIdentifierGeneratorFactory;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.log.DeprecationLogger;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.CollectionClassification;
import org.hibernate.query.sqm.function.SqmFunctionDescriptor;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.BasicType;
import org.hibernate.type.spi.TypeConfiguration;
import org.hibernate.usertype.UserType;

import org.jboss.jandex.IndexView;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.ConstraintMode;
import jakarta.persistence.SharedCacheMode;

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

		if ( serviceRegistry instanceof StandardServiceRegistry ) {
			return (StandardServiceRegistry) serviceRegistry;
		}
		else if ( serviceRegistry instanceof BootstrapServiceRegistry ) {
			log.debug(
					"ServiceRegistry passed to MetadataBuilder was a BootstrapServiceRegistry; this likely won't end well " +
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
		//this is needed only for implementing deprecated method
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
	public void contributeType(BasicType<?> type) {
		options.basicTypeRegistrations.add( new BasicTypeRegistration( type ) );
	}

	@Override
	public void contributeType(BasicType<?> type, String... keys) {
		options.basicTypeRegistrations.add( new BasicTypeRegistration( type, keys ) );
	}

	@Override
	public void contributeType(UserType<?> type, String[] keys) {
		options.basicTypeRegistrations.add( new BasicTypeRegistration( type, keys, getTypeConfiguration() ) );
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
		Collections.addAll( options.sourceProcessOrdering, sourceTypes );
		return this;
	}

	public MetadataBuilder allowSpecjSyntax() {
		this.options.specjProprietarySyntaxEnabled = true;
		return this;
	}

	public MetadataBuilder noConstraintByDefault() {
		this.options.noConstraintByDefault = true;
		return this;
	}

	@Override
	public MetadataBuilder applySqlFunction(String functionName, SqmFunctionDescriptor function) {
		this.bootstrapContext.addSqlFunction( functionName, function );
		return this;
	}

	@Override
	public MetadataBuilder applyAuxiliaryDatabaseObject(AuxiliaryDatabaseObject auxiliaryDatabaseObject) {
		this.bootstrapContext.addAuxiliaryDatabaseObject( auxiliaryDatabaseObject );
		return this;
	}

	@Override
	public MetadataBuilder applyAttributeConverter(ConverterDescriptor descriptor) {
		this.bootstrapContext.addAttributeConverterDescriptor( descriptor );
		return this;
	}

	@Override
	public <O,R> MetadataBuilder applyAttributeConverter(Class<? extends AttributeConverter<O,R>> attributeConverterClass) {
		bootstrapContext.addAttributeConverterDescriptor(
				new ClassBasedConverterDescriptor( attributeConverterClass, bootstrapContext.getClassmateContext() )
		);
		return this;
	}

	@Override
	public <O,R> MetadataBuilder applyAttributeConverter(Class<? extends AttributeConverter<O,R>> attributeConverterClass, boolean autoApply) {
		this.bootstrapContext.addAttributeConverterDescriptor(
				new ClassBasedConverterDescriptor(
						attributeConverterClass,
						autoApply,
						bootstrapContext.getClassmateContext()
				)
		);
		return this;
	}

	@Override
	public <O,R> MetadataBuilder applyAttributeConverter(AttributeConverter<O,R> attributeConverter) {
		bootstrapContext.addAttributeConverterDescriptor(
				new InstanceBasedConverterDescriptor( attributeConverter, bootstrapContext.getClassmateContext() )
		);
		return this;
	}

	@Override
	public MetadataBuilder applyAttributeConverter(AttributeConverter<?,?> attributeConverter, boolean autoApply) {
		bootstrapContext.addAttributeConverterDescriptor(
				new InstanceBasedConverterDescriptor( attributeConverter, autoApply, bootstrapContext.getClassmateContext() )
		);
		return this;
	}

	@Override
	public MetadataBuilder applyIdGenerationTypeInterpreter(IdGeneratorStrategyInterpreter interpreter) {
		this.options.idGenerationTypeInterpreter.addInterpreterDelegate( interpreter );
		return this;
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
		private CollectionClassification implicitListClassification;

		public MappingDefaultsImpl(StandardServiceRegistry serviceRegistry) {
			final ConfigurationService configService = serviceRegistry.getService( ConfigurationService.class );

			// AvailableSettings.DEFAULT_SCHEMA and AvailableSettings.DEFAULT_CATALOG
			// are taken into account later, at runtime, when rendering table/sequence names.
			// These fields are exclusively about mapping defaults,
			// overridden in XML mappings or through setters in MetadataBuilder.
			this.implicitSchemaName = null;
			this.implicitCatalogName = null;

			this.implicitlyQuoteIdentifiers = configService.getSetting(
					AvailableSettings.GLOBALLY_QUOTED_IDENTIFIERS,
					StandardConverters.BOOLEAN,
					false
			);

			this.implicitCacheAccessType = configService.getSetting(
					AvailableSettings.DEFAULT_CACHE_CONCURRENCY_STRATEGY,
					value -> AccessType.fromExternalName( value.toString() )
			);

			this.implicitListClassification = configService.getSetting(
					AvailableSettings.DEFAULT_LIST_SEMANTICS,
					value -> {
						final CollectionClassification classification = CollectionClassification.interpretSetting( value );
						if ( classification != CollectionClassification.LIST && classification != CollectionClassification.BAG ) {
							throw new AnnotationException(
									String.format(
											Locale.ROOT,
											"'%s' should specify either '%s' or '%s' (was '%s')",
											AvailableSettings.DEFAULT_LIST_SEMANTICS,
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

		@Override
		public CollectionClassification getImplicitListClassification() {
			return implicitListClassification;
		}
	}

	public static class MetadataBuildingOptionsImpl
			implements MetadataBuildingOptions, JpaOrmXmlPersistenceUnitDefaultAware {
		private final StandardServiceRegistry serviceRegistry;
		private final MappingDefaultsImpl mappingDefaults;
		private final IdentifierGeneratorFactory identifierGeneratorFactory;
		private final TimeZoneStorageStrategy defaultTimezoneStorage;

		// todo (6.0) : remove bootstrapContext property along with the deprecated methods
		private BootstrapContext bootstrapContext;

		private final ArrayList<BasicTypeRegistration> basicTypeRegistrations = new ArrayList<>();

		private ImplicitNamingStrategy implicitNamingStrategy;
		private PhysicalNamingStrategy physicalNamingStrategy;

		private SharedCacheMode sharedCacheMode;
		private final AccessType defaultCacheAccessType;
		private final boolean multiTenancyEnabled;
		private boolean explicitDiscriminatorsForJoinedInheritanceSupported;
		private boolean implicitDiscriminatorsForJoinedInheritanceSupported;
		private boolean implicitlyForceDiscriminatorInSelect;
		private boolean useNationalizedCharacterData;
		private boolean specjProprietarySyntaxEnabled;
		private boolean noConstraintByDefault;
		private final ArrayList<MetadataSourceType> sourceProcessOrdering;

		private final IdGeneratorInterpreterImpl idGenerationTypeInterpreter = new IdGeneratorInterpreterImpl();

		private final String schemaCharset;
		private final boolean xmlMappingEnabled;

		public MetadataBuildingOptionsImpl(StandardServiceRegistry serviceRegistry) {
			this.serviceRegistry = serviceRegistry;
			this.identifierGeneratorFactory = serviceRegistry.getService( MutableIdentifierGeneratorFactory.class );

			final StrategySelector strategySelector = serviceRegistry.getService( StrategySelector.class );
			final ConfigurationService configService = serviceRegistry.getService( ConfigurationService.class );

			this.mappingDefaults = new MappingDefaultsImpl( serviceRegistry );

			this.defaultTimezoneStorage = resolveTimeZoneStorageStrategy( serviceRegistry, configService );
			this.multiTenancyEnabled = serviceRegistry.getService(MultiTenantConnectionProvider.class)!=null;

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
					AvailableSettings.JAKARTA_SHARED_CACHE_MODE,
					value -> {
						if ( value == null ) {
							return null;
						}

						if ( value instanceof SharedCacheMode ) {
							return (SharedCacheMode) value;
						}

						return SharedCacheMode.valueOf( value.toString() );
					},
					configService.getSetting(
							AvailableSettings.JPA_SHARED_CACHE_MODE,
							value -> {
								if ( value == null ) {
									return null;
								}

								DeprecationLogger.DEPRECATION_LOGGER.deprecatedSetting(
										AvailableSettings.JPA_SHARED_CACHE_MODE,
										AvailableSettings.JAKARTA_SHARED_CACHE_MODE
								);

								if ( value instanceof SharedCacheMode ) {
									return (SharedCacheMode) value;
								}

								return SharedCacheMode.valueOf( value.toString() );
							},
							SharedCacheMode.UNSPECIFIED
					)
			);

			this.defaultCacheAccessType = configService.getSetting(
					AvailableSettings.DEFAULT_CACHE_CONCURRENCY_STRATEGY,
					value -> {
						if ( value == null ) {
							return null;
						}

						if ( value instanceof CacheConcurrencyStrategy ) {
							return ( (CacheConcurrencyStrategy) value ).toAccessType();
						}

						if ( value instanceof AccessType ) {
							return (AccessType) value;
						}

						return AccessType.fromExternalName( value.toString() );
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

			this.noConstraintByDefault = ConstraintMode.NO_CONSTRAINT.name().equalsIgnoreCase( configService.getSetting(
					AvailableSettings.HBM2DDL_DEFAULT_CONSTRAINT_MODE,
					String.class,
					null
			) );

			this.implicitNamingStrategy = strategySelector.resolveDefaultableStrategy(
					ImplicitNamingStrategy.class,
					configService.getSettings().get( AvailableSettings.IMPLICIT_NAMING_STRATEGY ),
					new Callable<>() {
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
		public IdentifierGeneratorFactory getIdentifierGeneratorFactory() {
			return identifierGeneratorFactory;
		}

		@Override
		public TimeZoneStorageStrategy getDefaultTimeZoneStorage() {
			return defaultTimezoneStorage;
		}

		@Override
		public List<BasicTypeRegistration> getBasicTypeRegistrations() {
			return basicTypeRegistrations;
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
		public IdGeneratorStrategyInterpreter getIdGenerationTypeInterpreter() {
			return idGenerationTypeInterpreter;
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
		public boolean isNoConstraintByDefault() {
			return noConstraintByDefault;
		}

		@Override
		public List<MetadataSourceType> getSourceProcessOrdering() {
			return sourceProcessOrdering;
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

		public void setBootstrapContext(BootstrapContext bootstrapContext) {
			this.bootstrapContext = bootstrapContext;
		}
	}

	private static TimeZoneStorageStrategy resolveTimeZoneStorageStrategy(
			StandardServiceRegistry serviceRegistry,
			ConfigurationService configService) {
		final TimeZoneStorageType configuredTimeZoneStorageType = configService.getSetting(
				AvailableSettings.TIMEZONE_DEFAULT_STORAGE,
				value -> TimeZoneStorageType.valueOf( value.toString() ),
				null
		);
		final TimeZoneStorageStrategy resolvedTimezoneStorage;
		// For now, we default to NORMALIZE as that is the Hibernate 5.x behavior
		if ( configuredTimeZoneStorageType == null ) {
			resolvedTimezoneStorage = TimeZoneStorageStrategy.NORMALIZE;
		}
		else {
			final TimeZoneSupport timeZoneSupport = serviceRegistry.getService( JdbcServices.class )
					.getDialect()
					.getTimeZoneSupport();
			switch ( configuredTimeZoneStorageType ) {
				case NATIVE:
					if ( timeZoneSupport != TimeZoneSupport.NATIVE ) {
						throw new HibernateException( "The configured time zone storage type NATIVE is not supported with the configured dialect" );
					}
					resolvedTimezoneStorage = TimeZoneStorageStrategy.NATIVE;
					break;
				case COLUMN:
					resolvedTimezoneStorage = TimeZoneStorageStrategy.COLUMN;
					break;
				case NORMALIZE:
					resolvedTimezoneStorage = TimeZoneStorageStrategy.NORMALIZE;
					break;
				case NORMALIZE_UTC:
					resolvedTimezoneStorage = TimeZoneStorageStrategy.NORMALIZE_UTC;
					break;
				case AUTO:
					switch ( timeZoneSupport ) {
						case NATIVE:
							resolvedTimezoneStorage = TimeZoneStorageStrategy.NATIVE;
							break;
						case NORMALIZE:
						case NONE:
							resolvedTimezoneStorage = TimeZoneStorageStrategy.COLUMN;
							break;
						default:
							throw new HibernateException( "Unsupported time zone support: " + timeZoneSupport );
					}
					break;
				default:
					throw new HibernateException( "Unsupported time zone storage type: " + configuredTimeZoneStorageType );
			}
		}
		return resolvedTimezoneStorage;
	}
}
