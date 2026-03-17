/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.jpa.boot.spi;

import jakarta.persistence.FetchType;
import jakarta.persistence.PersistenceConfiguration;
import jakarta.persistence.PersistenceUnitTransactionType;
import jakarta.persistence.SharedCacheMode;
import jakarta.persistence.ValidationMode;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.hibernate.HibernateException;
import org.hibernate.boot.archive.internal.StandardArchiveDescriptorFactory;
import org.hibernate.boot.archive.spi.ArchiveDescriptorFactory;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.scan.internal.ProvidedScannerProvider;
import org.hibernate.boot.scan.internal.ScannerLogger;
import org.hibernate.boot.scan.internal.ScanningContextImpl;
import org.hibernate.boot.scan.internal.StandardScanningProvider;
import org.hibernate.boot.scan.spi.Scanner;
import org.hibernate.boot.scan.spi.ScanningProvider;
import org.hibernate.boot.scan.spi.ScanningResult;
import org.hibernate.bytecode.enhance.spi.EnhancementContext;
import org.hibernate.bytecode.spi.ClassTransformer;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.PersistenceSettings;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.jpa.HibernatePersistenceProvider;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import static org.hibernate.jpa.internal.JpaLogger.JPA_LOGGER;

/**
 * PersistenceUnitDescriptor wrapper around {@linkplain PersistenceConfiguration}
 *
 * @author Steve Ebersole
 */
public class PersistenceConfigurationDescriptor implements PersistenceUnitDescriptor {
	private final PersistenceConfiguration persistenceConfiguration;

	private final Properties properties;
	private final List<String> managedClassNames;
	private final List<String> discoveredClassNames;

	public PersistenceConfigurationDescriptor(
			@NonNull HibernatePersistenceConfiguration persistenceConfiguration,
			@NonNull StandardServiceRegistry standardServiceRegistry) {
		this.persistenceConfiguration = persistenceConfiguration;
		this.properties = CollectionHelper.asProperties( persistenceConfiguration.properties() );
		this.managedClassNames = persistenceConfiguration.managedClasses().stream().map( Class::getName ).toList();

		final ScanningResult scanningResult = performScanning( persistenceConfiguration, standardServiceRegistry );
		this.discoveredClassNames = combineDiscoveredClasses( scanningResult );
	}

	private List<String> combineDiscoveredClasses(ScanningResult scanningResult) {
		final ArrayList<String> names = new ArrayList<>( scanningResult.discoveredClasses() );
		scanningResult.discoveredPackages().stream().map( packageName -> packageName + ".package-info" ).forEach( names::add );
		return names;
	}

	public PersistenceConfigurationDescriptor(@NonNull PersistenceConfiguration persistenceConfiguration) {
		this.persistenceConfiguration = persistenceConfiguration;
		this.properties = CollectionHelper.asProperties( persistenceConfiguration.properties() );
		this.managedClassNames = persistenceConfiguration.managedClasses().stream().map( Class::getName ).toList();
		this.discoveredClassNames = List.of();
	}

	@Override
	public Properties getProperties() {
		return properties;
	}

	@Override
	public String getName() {
		return persistenceConfiguration.name();
	}

	@Override
	public String getProviderClassName() {
		return persistenceConfiguration.provider();
	}

	@Override
	public boolean isUseQuotedIdentifiers() {
		return properties.get( AvailableSettings.GLOBALLY_QUOTED_IDENTIFIERS ) == Boolean.TRUE;
	}

	@Override
	public boolean isExcludeUnlistedClasses() {
		// if we do not know the root url nor jar files we cannot do scanning
		return !(persistenceConfiguration instanceof HibernatePersistenceConfiguration configuration)
			|| configuration.rootUrl() == null && configuration.jarFileUrls().isEmpty();
	}

	@Override
	public FetchType getDefaultToOneFetchType() {
		return persistenceConfiguration.defaultToOneFetchType();
	}

	@Override
	public PersistenceUnitTransactionType getPersistenceUnitTransactionType() {
		return persistenceConfiguration.transactionType();
	}

	@Override
	public List<String> getManagedClassNames() {
		return managedClassNames;
	}

	@Override
	public List<String> getAllClassNames() {
		return CollectionHelper.combine(  managedClassNames, discoveredClassNames );
	}

	@Override
	public List<String> getMappingFileNames() {
		return persistenceConfiguration.mappingFiles();
	}

	@Override
	public Object getNonJtaDataSource() {
		return persistenceConfiguration.nonJtaDataSource();
	}

	@Override
	public Object getJtaDataSource() {
		return persistenceConfiguration.jtaDataSource();
	}

	@Override
	public ValidationMode getValidationMode() {
		return persistenceConfiguration.validationMode();
	}

	@Override
	public SharedCacheMode getSharedCacheMode() {
		return persistenceConfiguration.sharedCacheMode();
	}

	@Override
	public ClassLoader getClassLoader() {
		return HibernatePersistenceProvider.class.getClassLoader();
	}

	@Override
	public ClassLoader getTempClassLoader() {
		return null;
	}

	@Override
	public boolean isClassTransformerRegistrationDisabled() {
		return true;
	}

	@Override
	public ClassTransformer pushClassTransformer(EnhancementContext enhancementContext) {
		if ( JPA_LOGGER.isDebugEnabled() ) {
			JPA_LOGGER.pushingClassTransformerUnsupported( getName() );
		}
		return null;
	}

	@Override
	public URL getPersistenceUnitRootUrl() {
		return persistenceConfiguration instanceof HibernatePersistenceConfiguration configuration
				? configuration.rootUrl()
				: null;
	}

	@Override
	public List<URL> getJarFileUrls() {
		return persistenceConfiguration instanceof HibernatePersistenceConfiguration configuration
				? configuration.jarFileUrls()
				: null;
	}

	private static ScanningResult performScanning(
			HibernatePersistenceConfiguration persistenceConfiguration,
			StandardServiceRegistry serviceRegistry) {
		final URL[] boundaries = collectUrls( persistenceConfiguration );
		if ( boundaries == null ) {
			return ScanningResult.NONE;
		}

		final var classLoaderService = serviceRegistry.requireService( ClassLoaderService.class );
		final var configurationService = serviceRegistry.requireService( ConfigurationService.class );
		final var archiveDescriptorFactory = determineArchiveDescriptorFactory( configurationService, classLoaderService );
		final var scanningContext = new ScanningContextImpl(
				archiveDescriptorFactory,
				configurationService.getSettings()
		);
		final ScanningProvider scanningProvider = determineScanningProvider( configurationService, classLoaderService );
		final Scanner scanner = scanningProvider.builderScanner( scanningContext );
		return scanner.scan( boundaries );
	}

	private static URL[] collectUrls(HibernatePersistenceConfiguration cfg) {
		if ( cfg.rootUrl() == null && CollectionHelper.isEmpty( cfg.jarFileUrls() ) ) {
			return null;
		}

		return combinedUrls( cfg.rootUrl(), cfg.jarFileUrls() ).toArray(new URL[0]);
	}

	private static List<URL> combinedUrls(URL rootUrl, List<URL> jarFileUrls) {
		final int size = CollectionHelper.size( jarFileUrls ) + ( rootUrl == null ? 0 : 1);
		final List<URL> combined = new ArrayList<>( size );
		if ( rootUrl != null ) {
			combined.add( rootUrl );
		}
		if ( jarFileUrls != null ) {
			combined.addAll( jarFileUrls );
		}
		return combined;
	}

	private static ScanningProvider determineScanningProvider(
			@NonNull ConfigurationService configurationService,
			@NonNull ClassLoaderService classLoaderService) {
		var configuredProvider = determineScanningProviderFromSetting( configurationService, classLoaderService );
		if ( configuredProvider != null ) {
			return configuredProvider;
		}

		var configuredScanner = determineScannerFromSetting( configurationService, classLoaderService );
		if ( configuredScanner != null ) {
			return new ProvidedScannerProvider( configuredScanner );
		}

		final Collection<ScanningProvider> scanningProviders = classLoaderService.loadJavaServices( ScanningProvider.class );
		if ( scanningProviders.isEmpty() ) {
			ScannerLogger.SCANNER_LOGGER.noScannerFactoryAvailable();
			return new StandardScanningProvider();
		}
		else {
			final ScanningProvider first = scanningProviders.iterator().next();
			if ( scanningProviders.size() > 1 ) {
				ScannerLogger.SCANNER_LOGGER.multipleScannerFactoriesAvailable( first.getClass().getName() );
			}
			return first;
		}
	}

	private static ScanningProvider determineScanningProviderFromSetting(
			@NonNull ConfigurationService configurationService,
			@NonNull ClassLoaderService classLoaderService) {
		var providerSetting = configurationService.getSettings().get( PersistenceSettings.SCANNING );
		if ( providerSetting == null ) {
			return null;
		}

		// might be any of the 3 standard forms
		if ( providerSetting instanceof ScanningProvider instance ) {
			return instance;
		}
		else if ( providerSetting instanceof Class<?> implClass ) {
			try {
				return (ScanningProvider) implClass.getDeclaredConstructor().newInstance();
			}
			catch (Exception e) {
				throw new HibernateException(
						String.format( Locale.ROOT,
								"Unable to instantiate ScanningProvider `%s`",
								implClass.getName()
						),
						e
				);
			}
		}
		else {
			var implClassName = providerSetting.toString();
			var implClass = classLoaderService.classForName( implClassName );
			try {
				return (ScanningProvider) implClass.getDeclaredConstructor().newInstance();
			}
			catch (Exception e) {
				throw new HibernateException(
						String.format( Locale.ROOT,
								"Unable to instantiate ScanningProvider `%s`",
								implClass.getName()
						),
						e
				);
			}
		}
	}

	private static Scanner determineScannerFromSetting(
			@NonNull ConfigurationService configurationService,
			@NonNull ClassLoaderService classLoaderService) {
		var setting = configurationService.getSettings().get( PersistenceSettings.SCANNER );
		if ( setting == null ) {
			return null;
		}

		// might be any of the 3 standard forms
		if ( setting instanceof Scanner instance ) {
			return instance;
		}
		else if ( setting instanceof Class<?> implClass ) {
			try {
				return (Scanner) implClass.getDeclaredConstructor().newInstance();
			}
			catch (Exception e) {
				throw new HibernateException(
						String.format( Locale.ROOT,
								"Unable to instantiate Scanner `%s`",
								implClass.getName()
						),
						e
				);
			}
		}
		else {
			var implClassName = setting.toString();
			var implClass = classLoaderService.classForName( implClassName );
			try {
				return (Scanner) implClass.getDeclaredConstructor().newInstance();
			}
			catch (Exception e) {
				throw new HibernateException(
						String.format( Locale.ROOT,
								"Unable to instantiate Scanner `%s`",
								implClass.getName()
						),
						e
				);
			}
		}
	}

	private static ArchiveDescriptorFactory determineArchiveDescriptorFactory(
			@NonNull ConfigurationService configurationService,
			@NonNull ClassLoaderService classLoaderService) {
		final Object setting = configurationService.getSettings().get( PersistenceSettings.SCANNER_ARCHIVE_INTERPRETER );
		if ( setting instanceof ArchiveDescriptorFactory ref ) {
			return ref;
		}
		else if ( setting instanceof Class<?> implClass ) {
			try {
				return (ArchiveDescriptorFactory) implClass.getDeclaredConstructor().newInstance();
			}
			catch (Exception e) {
				throw new HibernateException( "Unable to instantiate configured ArchiveDescriptorFactory - " + implClass.getName(), e );
			}
		}
		else if ( setting != null ) {
			var implClassName = setting.toString();
			var implClass = classLoaderService.classForName( implClassName );
			try {
				return (ArchiveDescriptorFactory) implClass.getDeclaredConstructor().newInstance();
			}
			catch (Exception e) {
				throw new HibernateException( "Unable to instantiate configured ArchiveDescriptorFactory - " + implClass.getName(), e );
			}
		}
		return new StandardArchiveDescriptorFactory();
	}

}
