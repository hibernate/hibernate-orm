/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.source;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import jakarta.annotation.Nonnull;
import org.hibernate.HibernateException;
import org.hibernate.boot.archive.internal.StandardArchiveDescriptorFactory;
import org.hibernate.boot.archive.spi.ArchiveDescriptorFactory;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.scan.internal.ProvidedScannerProvider;
import org.hibernate.boot.scan.internal.ScannerLogger;
import org.hibernate.boot.scan.internal.ScanningContextImpl;
import org.hibernate.boot.scan.internal.StandardScanningProvider;
import org.hibernate.boot.scan.spi.Scanner;
import org.hibernate.boot.scan.spi.ScanningProvider;
import org.hibernate.boot.scan.spi.ScanningResult;
import org.hibernate.boot.settings.ResolvedBootstrapSettings;
import org.hibernate.boot.settings.ResolvedMappingSettings;
import org.hibernate.cfg.PersistenceSettings;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.jpa.HibernatePersistenceConfiguration;

/// Scanning support for Hibernate's programmatic persistence configuration.
///
/// @since 9.0
/// @author Steve Ebersole
class HibernatePersistenceConfigurationScanner {
	/// Performs archive scanning for the URLs declared by the supplied persistence configuration.
	///
	/// Bootstrap settings are used only for scanner-related options, such as an explicitly supplied
	/// scanner, scanning provider, or archive descriptor factory.
	///
	/// If the configuration declares neither a root URL nor jar-file URLs, no
	/// scanning is attempted and [ScanningResult#NONE] is returned.
	///
	/// @param persistenceConfiguration The programmatic persistence configuration to scan
	/// @param bootstrapSettings Resolved bootstrap settings carrying scanner-related configuration values
	/// @param classLoaderService Class-loading access used to resolve scanner classes and service-loaded providers
	///
	/// @return The discovered classes, packages, and mapping files.  Never null, returns [ScanningResult#NONE] instead.
	@Nonnull
	static ScanningResult performScanning(
			HibernatePersistenceConfiguration persistenceConfiguration,
			ResolvedBootstrapSettings bootstrapSettings,
			ResolvedMappingSettings mappingSettings,
			ClassLoaderService classLoaderService) {
		final URL[] boundaries = collectUrls( persistenceConfiguration );
		if ( boundaries == null ) {
			return ScanningResult.NONE;
		}

		final Map<String, Object> configurationValues = bootstrapSettings.configurationValues();
		final var archiveDescriptorFactory = determineArchiveDescriptorFactory( configurationValues, classLoaderService );
		final var scanningContext = new ScanningContextImpl( archiveDescriptorFactory, configurationValues );
		final ScanningProvider scanningProvider = determineScanningProvider( configurationValues, classLoaderService );
		final Scanner scanner = scanningProvider.builderScanner( scanningContext );
		return scanner.scan( boundaries );
	}

	private static URL[] collectUrls(HibernatePersistenceConfiguration cfg) {
		if ( cfg.rootUrl() == null && CollectionHelper.isEmpty( cfg.jarFileUrls() ) ) {
			return null;
		}

		return combinedUrls( cfg.rootUrl(), cfg.jarFileUrls() ).toArray( new URL[0] );
	}

	private static List<URL> combinedUrls(URL rootUrl, List<URL> jarFileUrls) {
		final int size = CollectionHelper.size( jarFileUrls ) + ( rootUrl == null ? 0 : 1 );
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
			Map<?, ?> settings,
			ClassLoaderService classLoaderService) {
		var configuredProvider = determineScanningProviderFromSetting( settings, classLoaderService );
		if ( configuredProvider != null ) {
			return configuredProvider;
		}

		var configuredScanner = determineScannerFromSetting( settings, classLoaderService );
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
			Map<?, ?> settings,
			ClassLoaderService classLoaderService) {
		var providerSetting = settings.get( PersistenceSettings.SCANNING );
		if ( providerSetting == null ) {
			return null;
		}

		if ( providerSetting instanceof ScanningProvider instance ) {
			return instance;
		}
		else if ( providerSetting instanceof Class<?> implClass ) {
			return instantiateScanningProvider( implClass );
		}
		else {
			var implClassName = providerSetting.toString();
			var implClass = classLoaderService.classForName( implClassName );
			return instantiateScanningProvider( implClass );
		}
	}

	private static ScanningProvider instantiateScanningProvider(Class<?> implClass) {
		try {
			return (ScanningProvider) implClass.getDeclaredConstructor().newInstance();
		}
		catch (Exception e) {
			throw new HibernateException(
					String.format(
							Locale.ROOT,
							"Unable to instantiate ScanningProvider `%s`",
							implClass.getName()
					),
					e
			);
		}
	}

	private static Scanner determineScannerFromSetting(
			Map<?, ?> settings,
			ClassLoaderService classLoaderService) {
		var setting = settings.get( PersistenceSettings.SCANNER );
		if ( setting == null ) {
			return null;
		}

		if ( setting instanceof Scanner instance ) {
			return instance;
		}
		else if ( setting instanceof Class<?> implClass ) {
			return instantiateScanner( implClass );
		}
		else {
			var implClassName = setting.toString();
			var implClass = classLoaderService.classForName( implClassName );
			return instantiateScanner( implClass );
		}
	}

	private static Scanner instantiateScanner(Class<?> implClass) {
		try {
			return (Scanner) implClass.getDeclaredConstructor().newInstance();
		}
		catch (Exception e) {
			throw new HibernateException(
					String.format(
							Locale.ROOT,
							"Unable to instantiate Scanner `%s`",
							implClass.getName()
					),
					e
			);
		}
	}

	private static ArchiveDescriptorFactory determineArchiveDescriptorFactory(
			Map<?, ?> settings,
			ClassLoaderService classLoaderService) {
		final Object setting = settings.get( PersistenceSettings.SCANNER_ARCHIVE_INTERPRETER );
		if ( setting instanceof ArchiveDescriptorFactory ref ) {
			return ref;
		}
		else if ( setting instanceof Class<?> implClass ) {
			return instantiateArchiveDescriptorFactory( implClass );
		}
		else if ( setting != null ) {
			var implClassName = setting.toString();
			var implClass = classLoaderService.classForName( implClassName );
			return instantiateArchiveDescriptorFactory( implClass );
		}
		return new StandardArchiveDescriptorFactory();
	}

	private static ArchiveDescriptorFactory instantiateArchiveDescriptorFactory(Class<?> implClass) {
		try {
			return (ArchiveDescriptorFactory) implClass.getDeclaredConstructor().newInstance();
		}
		catch (Exception e) {
			throw new HibernateException( "Unable to instantiate configured ArchiveDescriptorFactory - " + implClass.getName(), e );
		}
	}

	private HibernatePersistenceConfigurationScanner() {
	}
}
