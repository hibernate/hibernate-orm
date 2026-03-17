/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.scan.internal;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.hibernate.HibernateException;
import org.hibernate.boot.archive.internal.StandardArchiveDescriptorFactory;
import org.hibernate.boot.archive.spi.ArchiveDescriptorFactory;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.scan.spi.Scanner;
import org.hibernate.boot.scan.spi.ScanningProvider;
import org.hibernate.boot.scan.spi.ScanningResult;
import org.hibernate.cfg.PersistenceSettings;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.jpa.HibernatePersistenceConfiguration;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * @author Steve Ebersole
 */
public class ScanningHelper {
	public static ScanningResult performScanning(
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

	private ScanningHelper() {
	}
}
