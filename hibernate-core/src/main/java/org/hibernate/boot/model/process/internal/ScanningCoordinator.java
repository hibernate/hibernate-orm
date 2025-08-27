/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.process.internal;

import org.hibernate.boot.MappingException;
import org.hibernate.boot.archive.internal.StandardArchiveDescriptorFactory;
import org.hibernate.boot.archive.internal.UrlInputStreamAccess;
import org.hibernate.boot.archive.scan.internal.DisabledScanner;
import org.hibernate.boot.archive.scan.internal.StandardScanParameters;
import org.hibernate.boot.archive.scan.spi.ClassDescriptor;
import org.hibernate.boot.archive.scan.spi.MappingFileDescriptor;
import org.hibernate.boot.archive.scan.spi.PackageDescriptor;
import org.hibernate.boot.archive.scan.spi.ScanEnvironment;
import org.hibernate.boot.archive.scan.spi.ScanResult;
import org.hibernate.boot.archive.scan.spi.Scanner;
import org.hibernate.boot.archive.scan.spi.ScannerFactory;
import org.hibernate.boot.archive.spi.ArchiveDescriptorFactory;
import org.hibernate.boot.internal.ClassLoaderAccessImpl;
import org.hibernate.boot.jaxb.Origin;
import org.hibernate.boot.jaxb.SourceType;
import org.hibernate.boot.jaxb.internal.InputStreamAccessXmlSource;
import org.hibernate.boot.jaxb.spi.Binding;
import org.hibernate.boot.model.convert.internal.ConverterDescriptors;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.boot.spi.ClassLoaderAccess;
import org.hibernate.boot.spi.XmlMappingBinderAccess;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.hibernate.boot.archive.scan.internal.ScannerLogger.SCANNER_LOGGER;

/**
 * Coordinates the process of executing {@link Scanner} (if enabled)
 * and applying the resources (classes, packages and mappings) discovered.
 *
 * @author Steve Ebersole
 */
public class ScanningCoordinator {

	/**
	 * Singleton access
	 */
	public static final ScanningCoordinator INSTANCE = new ScanningCoordinator();

	private ScanningCoordinator() {
	}

	public void coordinateScan(
			ManagedResourcesImpl managedResources,
			BootstrapContext bootstrapContext,
			XmlMappingBinderAccess xmlMappingBinderAccess) {
		if ( bootstrapContext.getScanEnvironment() != null ) {
			// NOTE: the idea with JandexInitializer/JandexInitManager was to allow adding classes
			// to the index as we discovered them via scanning and .  Currently
			final Scanner scanner = buildScanner( bootstrapContext );
			final ScanResult scanResult = scanner.scan(
					bootstrapContext.getScanEnvironment(),
					bootstrapContext.getScanOptions(),
					StandardScanParameters.INSTANCE
			);

			applyScanResultsToManagedResources( managedResources, scanResult, bootstrapContext, xmlMappingBinderAccess );
		}
	}

	private static Scanner buildScanner(BootstrapContext bootstrapContext) {
		final Object scannerSetting = bootstrapContext.getScanner();
		final ArchiveDescriptorFactory archiveDescriptorFactory = bootstrapContext.getArchiveDescriptorFactory();
		if ( scannerSetting == null ) {
			return getStandardScanner( bootstrapContext, archiveDescriptorFactory );
		}
		else {
			if ( scannerSetting instanceof Scanner scanner ) {
				if ( archiveDescriptorFactory != null ) {
					throw new IllegalStateException(
							"A Scanner instance and an ArchiveDescriptorFactory were both specified; please " +
									"specify one or the other, or if you need to supply both, Scanner class to use " +
									"(assuming it has a constructor accepting a ArchiveDescriptorFactory).  " +
									"Alternatively, just pass the ArchiveDescriptorFactory during your own " +
									"Scanner constructor assuming it is statically known."
					);
				}
				return scanner;
			}
			else {
				return createScanner( archiveDescriptorFactory,
						scannerClass( bootstrapContext, scannerSetting ) );
			}
		}
	}

	private static Class<? extends Scanner> scannerClass(
			BootstrapContext bootstrapContext, Object scannerSetting) {
		if ( scannerSetting instanceof Class<?> scannerSettingClass ) {
			if ( !Scanner.class.isAssignableFrom( scannerSettingClass ) ) {
				throw new IllegalArgumentException(
						"Configuration provided a custom scanner class '" +
								scannerSettingClass.getName() +
								"' which does not implement 'Scanner'"
				);
			}
			return scannerSettingClass.asSubclass( Scanner.class );
		}
		else {
			final ClassLoaderAccess classLoaderAccess = new ClassLoaderAccessImpl(
					bootstrapContext.getJpaTempClassLoader(),
					bootstrapContext.getClassLoaderService()
			);
			return classLoaderAccess.classForName( scannerSetting.toString() );
		}
	}

	private static Scanner createScanner(
			ArchiveDescriptorFactory archiveDescriptorFactory,
			Class<? extends Scanner> scannerImplClass) {

		final var SINGLE_ARG = new Class[] { ArchiveDescriptorFactory.class };

		if ( archiveDescriptorFactory != null ) {
			// find the single-arg constructor - it's an error if none exists
			try {
				final var constructor = scannerImplClass.getConstructor( SINGLE_ARG );
				try {
					return constructor.newInstance( archiveDescriptorFactory );
				}
				catch (Exception e) {
					throw new IllegalStateException(
							"Error instantiating custom scanner class '" +
							scannerImplClass.getName() + "'",
							e
					);
				}
			}
			catch (NoSuchMethodException e) {
				throw new IllegalArgumentException(
						"Configuration specified a custom scanner class and a custom ArchiveDescriptorFactory, but " +
								"Scanner implementation does not have a constructor accepting ArchiveDescriptorFactory"
				);
			}
		}
		else {
			// could be either ctor form...
			// find the single-arg constructor - it's an error if none exists
			try {
				final var constructor = scannerImplClass.getConstructor( SINGLE_ARG );
				try {
					return constructor.newInstance( StandardArchiveDescriptorFactory.INSTANCE );
				}
				catch (Exception e) {
					throw new IllegalStateException(
							"Error instantiating custom scanner class '" +
							scannerImplClass.getName() + "'",
							e
					);
				}
			}
			catch (NoSuchMethodException e) {
				try {
					final var constructor = scannerImplClass.getConstructor();
					try {
						return constructor.newInstance();
					}
					catch (Exception e2) {
						throw new IllegalStateException(
								"Error instantiating custom scanner class '" +
								scannerImplClass.getName() + "'",
								e2
						);
					}
				}
				catch (NoSuchMethodException ignore) {
					throw new IllegalArgumentException(
							"Configuration specified a custom scanner class with no appropriate constructor"
					);
				}
			}
		}
	}

	private static Scanner getStandardScanner(
			BootstrapContext bootstrapContext,
			ArchiveDescriptorFactory archiveDescriptorFactory) {
		// No custom Scanner specified, use the StandardScanner
		final var scannerFactories =
				bootstrapContext.getClassLoaderService()
						.loadJavaServices( ScannerFactory.class );
		for ( ScannerFactory scannerFactory : scannerFactories ) {
			final Scanner scanner = scannerFactory.getScanner( archiveDescriptorFactory );
			if ( scannerFactories.size() > 1 ) {
				SCANNER_LOGGER.multipleScannerFactoriesAvailable(scanner.getClass().getName());
			}
			return scanner;
		}

		SCANNER_LOGGER.noScannerFactoryAvailable();
		return new DisabledScanner();
	}

	public void applyScanResultsToManagedResources(
			ManagedResourcesImpl managedResources,
			ScanResult scanResult,
			BootstrapContext bootstrapContext,
			XmlMappingBinderAccess xmlMappingBinderAccess) {

		final ScanEnvironment scanEnvironment = bootstrapContext.getScanEnvironment();
		final ClassLoaderService classLoaderService = bootstrapContext.getClassLoaderService();


		// mapping files ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

		final Set<String> nonLocatedMappingFileNames = new HashSet<>();
		final List<String> explicitMappingFileNames = scanEnvironment.getExplicitlyListedMappingFiles();
		if ( explicitMappingFileNames != null ) {
			nonLocatedMappingFileNames.addAll( explicitMappingFileNames );
		}

		if ( xmlMappingBinderAccess != null ) { // xml mapping is not disabled
			for ( MappingFileDescriptor mappingFileDescriptor : scanResult.getLocatedMappingFiles() ) {
				//noinspection unchecked,rawtypes
				managedResources.addXmlBinding( (Binding) InputStreamAccessXmlSource.fromStreamAccess(
						mappingFileDescriptor.getStreamAccess(),
						xmlMappingBinderAccess.getMappingBinder()
				) );
				nonLocatedMappingFileNames.remove( mappingFileDescriptor.getName() );
			}

			for ( String name : nonLocatedMappingFileNames ) {
				final Origin origin = new Origin( SourceType.RESOURCE, name );
				final URL url = classLoaderService.locateResource( name );
				if ( url == null ) {
					throw new MappingException( "Unable to resolve explicitly named mapping file: " + name, origin );
				}
				final UrlInputStreamAccess urlInputStreamAccess = new UrlInputStreamAccess( url );
				//noinspection unchecked,rawtypes
				managedResources.addXmlBinding( (Binding) InputStreamAccessXmlSource.fromStreamAccess(
						urlInputStreamAccess,
						origin,
						xmlMappingBinderAccess.getMappingBinder()
				) );
			}
		}

		// classes and packages ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

		final List<String> explicitlyListedClassNames = scanEnvironment.getExplicitlyListedClassNames();
		final List<String> unresolvedListedClassNames =
				explicitlyListedClassNames == null
						? new ArrayList<>()
						: new ArrayList<>( explicitlyListedClassNames );

		for ( ClassDescriptor classDescriptor : scanResult.getLocatedClasses() ) {
			if ( classDescriptor.getCategorization() == ClassDescriptor.Categorization.CONVERTER ) {
				// converter classes are safe to load because we never enhance them,
				// and notice we use the ClassLoaderService specifically, not the temp ClassLoader (if any)
				managedResources.addAttributeConverterDefinition(
						ConverterDescriptors.of( classLoaderService.classForName( classDescriptor.getName() ),
								bootstrapContext.getClassmateContext() )
				);
			}
			else if ( classDescriptor.getCategorization() == ClassDescriptor.Categorization.MODEL ) {
				managedResources.addAnnotatedClassName( classDescriptor.getName() );
			}
			unresolvedListedClassNames.remove( classDescriptor.getName() );
		}

		// IMPL NOTE: 'explicitlyListedClassNames' can contain both class and package names
		for ( PackageDescriptor packageDescriptor : scanResult.getLocatedPackages() ) {
			managedResources.addAnnotatedPackageName( packageDescriptor.getName() );
			unresolvedListedClassNames.remove( packageDescriptor.getName() );
		}

		for ( String unresolvedListedClassName : unresolvedListedClassNames ) {
			// because the explicit list can contain both class names and package names,
			// we need to check for both possibilities here

			// First, try it as a class name
			final URL classFileUrl =
					classLoaderService.locateResource( classFileName( unresolvedListedClassName ) );
			if ( classFileUrl != null ) {
				managedResources.addAnnotatedClassName( unresolvedListedClassName );
			}
			else {
				// Then, try it as a package name
				final URL packageInfoFileUrl =
						classLoaderService.locateResource( packageInfoFileName( unresolvedListedClassName ) );
				if ( packageInfoFileUrl != null ) {
					managedResources.addAnnotatedPackageName( unresolvedListedClassName );
				}
				else {
					// Last, try it by loading the class
					try {
						final Class<?> clazz = classLoaderService.classForName( unresolvedListedClassName );
						managedResources.addAnnotatedClassReference( clazz );
					}
					catch (ClassLoadingException ignore) {
						// ignore this error
						SCANNER_LOGGER.unableToResolveClass( unresolvedListedClassName, scanEnvironment.getRootUrl() );
					}
				}
			}
		}
	}

	private static String packageInfoFileName(String unresolvedListedClassName) {
		return unresolvedListedClassName.replace( '.', '/' ) + "/package-info.class";
	}

	private static String classFileName(String unresolvedListedClassName) {
		return unresolvedListedClassName.replace( '.', '/' ) + ".class";
	}
}
