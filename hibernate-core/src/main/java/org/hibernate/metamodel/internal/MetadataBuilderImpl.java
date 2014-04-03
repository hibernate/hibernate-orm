/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.internal;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.SharedCacheMode;

import org.hibernate.HibernateException;
import org.hibernate.MultiTenancyStrategy;
import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.selector.spi.StrategySelector;
import org.hibernate.boot.spi.CacheRegionDefinition;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.EJB3NamingStrategy;
import org.hibernate.cfg.NamingStrategy;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.config.spi.StandardConverters;
import org.hibernate.metamodel.MetadataBuilder;
import org.hibernate.metamodel.MetadataSources;
import org.hibernate.metamodel.archive.scan.internal.StandardScanOptions;
import org.hibernate.metamodel.archive.scan.spi.ScanEnvironment;
import org.hibernate.metamodel.archive.scan.spi.ScanOptions;
import org.hibernate.metamodel.archive.scan.spi.Scanner;
import org.hibernate.metamodel.archive.spi.ArchiveDescriptorFactory;
import org.hibernate.metamodel.spi.MetadataSourcesContributor;
import org.hibernate.metamodel.spi.PersistentAttributeMemberResolver;
import org.hibernate.metamodel.spi.StandardPersistentAttributeMemberResolver;
import org.hibernate.metamodel.spi.TypeContributions;
import org.hibernate.metamodel.spi.TypeContributor;
import org.hibernate.metamodel.spi.relational.Database;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.BasicType;
import org.hibernate.type.CompositeCustomType;
import org.hibernate.type.CustomType;
import org.hibernate.usertype.CompositeUserType;
import org.hibernate.usertype.UserType;
import org.jboss.jandex.IndexView;
import org.jboss.logging.Logger;

import static org.hibernate.internal.DeprecationLogger.DEPRECATION_LOGGER;

/**
 * The implementation of the {@link MetadataBuilder} contract.
 *
 * @author Steve Ebersole
 */
public class MetadataBuilderImpl implements MetadataBuilder, TypeContributions {
	private static final Logger log = Logger.getLogger( MetadataBuilderImpl.class );

	private final MetadataSources sources;
	private final Options options;

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
				sources.getServiceRegistry().getService( ClassLoaderService.class )
						.loadJavaServices( MetadataSourcesContributor.class ) ) {
			contributor.contribute( sources, null );
		}
		this.options = new Options( serviceRegistry );
	}

	@Override
	public MetadataBuilder with(NamingStrategy namingStrategy) {
		this.options.namingStrategy = namingStrategy;
		return this;
	}

	@Override
	public MetadataBuilder with(SharedCacheMode sharedCacheMode) {
		this.options.sharedCacheMode = sharedCacheMode;
		return this;
	}

	@Override
	public MetadataBuilder with(AccessType accessType) {
		this.options.defaultCacheAccessType = accessType;
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
	public MetadataBuilder with(PersistentAttributeMemberResolver resolver) {
		options.persistentAttributeMemberResolver = resolver;
		return this;
	}

	@Override
	public MetadataImpl build() {
		return MetadataBuildingProcess.build( sources, options );
	}

	/**
	 * Implementation of the Database.Defaults contract
	 */
	public static class DatabaseDefaults implements Database.Defaults {
		private boolean globallyQuotedIdentifiers;
		private String defaultSchemaName;
		private String defaultCatalogName;

		public DatabaseDefaults(ConfigurationService configurationService) {
			defaultSchemaName = configurationService.getSetting(
					AvailableSettings.DEFAULT_SCHEMA,
					StandardConverters.STRING,
					null
			);

			defaultCatalogName = configurationService.getSetting(
					AvailableSettings.DEFAULT_CATALOG,
					StandardConverters.STRING,
					null
			);

			globallyQuotedIdentifiers = configurationService.getSetting(
					AvailableSettings.GLOBALLY_QUOTED_IDENTIFIERS,
					StandardConverters.BOOLEAN,
					false
			);
		}

		@Override
		public String getDefaultSchemaName() {
			return defaultSchemaName;
		}

		@Override
		public String getDefaultCatalogName() {
			return defaultCatalogName;
		}

		@Override
		public boolean isGloballyQuotedIdentifiers() {
			return globallyQuotedIdentifiers;
		}
	}

	public static class Options implements org.hibernate.metamodel.spi.MetadataBuildingOptions {
		private final StandardServiceRegistry serviceRegistry;
		private final DatabaseDefaults databaseDefaults;

		private List<BasicType> basicTypeRegistrations = new ArrayList<BasicType>();

		private IndexView jandexView;
		private ClassLoader tempClassLoader;

		private ScanOptions scanOptions;
		private ScanEnvironment scanEnvironment;
		private Object scannerSetting;
		private ArchiveDescriptorFactory archiveDescriptorFactory;

		private NamingStrategy namingStrategy = EJB3NamingStrategy.INSTANCE;
		private SharedCacheMode sharedCacheMode = SharedCacheMode.ENABLE_SELECTIVE;
		private AccessType defaultCacheAccessType;
		private boolean useNewIdentifierGenerators;
		private MultiTenancyStrategy multiTenancyStrategy;
		private List<CacheRegionDefinition> cacheRegionDefinitions;
		private boolean explicitDiscriminatorsForJoinedInheritanceSupported;
		private boolean implicitDiscriminatorsForJoinedInheritanceSupported;

		private PersistentAttributeMemberResolver persistentAttributeMemberResolver =
				StandardPersistentAttributeMemberResolver.INSTANCE;

		public Options(StandardServiceRegistry serviceRegistry) {
			this.serviceRegistry = serviceRegistry;

			final StrategySelector strategySelector = serviceRegistry.getService( StrategySelector.class );
			final ConfigurationService configService = serviceRegistry.getService( ConfigurationService.class );

			this.databaseDefaults = new DatabaseDefaults( configService );

			// cache access type
			defaultCacheAccessType = configService.getSetting(
					AvailableSettings.DEFAULT_CACHE_CONCURRENCY_STRATEGY,
					new ConfigurationService.Converter<AccessType>() {
						@Override
						public AccessType convert(Object value) {
							return AccessType.fromExternalName( value.toString() );
						}
					}
			);

			jandexView = (IndexView) configService.getSettings().get( AvailableSettings.JANDEX_INDEX );

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
		}

		@Override
		public StandardServiceRegistry getServiceRegistry() {
			return serviceRegistry;
		}

		@Override
		public DatabaseDefaults getDatabaseDefaults() {
			return databaseDefaults;
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
		public NamingStrategy getNamingStrategy() {
			return namingStrategy;
		}

		@Override
		public SharedCacheMode getSharedCacheMode() {
			return sharedCacheMode;
		}

		@Override
		public AccessType getDefaultCacheAccessType() {
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
		public PersistentAttributeMemberResolver getPersistentAttributeMemberResolver() {
			return persistentAttributeMemberResolver;
		}
	}

}
