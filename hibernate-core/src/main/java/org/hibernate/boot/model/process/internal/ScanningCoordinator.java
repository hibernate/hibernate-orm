/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.process.internal;

import org.hibernate.boot.MappingException;
import org.hibernate.boot.archive.internal.UrlInputStreamAccess;
import org.hibernate.boot.archive.scan.internal.StandardScanParameters;
import org.hibernate.boot.archive.scan.internal.StandardScanner;
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
import org.hibernate.boot.model.convert.internal.ClassBasedConverterDescriptor;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.boot.spi.ClassLoaderAccess;
import org.hibernate.boot.spi.XmlMappingBinderAccess;
import org.hibernate.service.ServiceRegistry;
import org.jboss.logging.Logger;

import java.lang.reflect.Constructor;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Coordinates the process of executing {@link Scanner} (if enabled)
 * and applying the resources (classes, packages and mappings) discovered.
 *
 * @author Steve Ebersole
 */
public class ScanningCoordinator {
	private static final Logger log = Logger.getLogger( ScanningCoordinator.class );

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
		if ( bootstrapContext.getScanEnvironment() == null ) {
			return;
		}

		final ClassLoaderAccess classLoaderAccess = new ClassLoaderAccessImpl(
				bootstrapContext.getJpaTempClassLoader(),
				bootstrapContext.getServiceRegistry().requireService( ClassLoaderService.class )
		);

		// NOTE : the idea with JandexInitializer/JandexInitManager was to allow adding classes
		// to the index as we discovered them via scanning and .  Currently
		final Scanner scanner = buildScanner( bootstrapContext, classLoaderAccess );
		final ScanResult scanResult = scanner.scan(
				bootstrapContext.getScanEnvironment(),
				bootstrapContext.getScanOptions(),
				StandardScanParameters.INSTANCE
		);

		applyScanResultsToManagedResources( managedResources, scanResult, bootstrapContext, xmlMappingBinderAccess );
	}

	private static final Class<?>[] FULL_ARGS = new Class[] { ArchiveDescriptorFactory.class, ServiceRegistry.class };
	private static final Class<?>[] ARCHIVING_ARG = new Class[] { ArchiveDescriptorFactory.class };
	private static final Class<?>[] REGISTRY_ARG = new Class[] { ServiceRegistry.class };

	@SuppressWarnings("unchecked")
	private static Scanner buildScanner(BootstrapContext bootstrapContext, ClassLoaderAccess classLoaderAccess) {
		final Object scannerSetting = bootstrapContext.getScanner();
		final ArchiveDescriptorFactory archiveDescriptorFactory = bootstrapContext.getArchiveDescriptorFactory();
		final StandardServiceRegistry serviceRegistry = bootstrapContext.getServiceRegistry();

		if ( scannerSetting == null ) {
			final Iterator<ScannerFactory> iterator = serviceRegistry
					.requireService( ClassLoaderService.class )
					.loadJavaServices( ScannerFactory.class )
					.iterator();
			if ( iterator.hasNext() ) {
				final ScannerFactory factory = iterator.next();
				log.debugf( "Using ScannerFactory : %s", factory );
				if ( iterator.hasNext() ) {
					log.debugf( "More than one ScannerFactory discovered" );
				}
				return factory.createScanner( archiveDescriptorFactory, serviceRegistry );
			}

			// No custom Scanner specified, use the StandardScanner
			return new StandardScanner( archiveDescriptorFactory, serviceRegistry );
		}

		if ( scannerSetting instanceof Scanner ) {
			if ( archiveDescriptorFactory != null ) {
				throw new IllegalStateException(
						"A Scanner instance and an ArchiveDescriptorFactory were both specified; please " +
								"specify one or the other, or if you need to supply both, Scanner class to use " +
								"(assuming it has a constructor accepting a ArchiveDescriptorFactory).  " +
								"Alternatively, just pass the ArchiveDescriptorFactory during your own " +
								"Scanner constructor assuming it is statically known."
				);
			}
			return (Scanner) scannerSetting;
		}

		final Class<? extends Scanner> scannerImplClass;
		if ( scannerSetting instanceof Class ) {
			scannerImplClass = (Class<? extends Scanner>) scannerSetting;
		}
		else {
			scannerImplClass = classLoaderAccess.classForName( scannerSetting.toString() );
		}

		final Scanner fromFullConstructor = fromFullConstructor( scannerImplClass, archiveDescriptorFactory, serviceRegistry );
		if ( fromFullConstructor != null ) {
			return fromFullConstructor;
		}

		final Scanner fromArchivingConstructor = fromArchivingConstructor( scannerImplClass, archiveDescriptorFactory );
		if ( fromArchivingConstructor != null ) {
			return fromArchivingConstructor;
		}

		final Scanner fromServiceRegistryConstructor = fromServiceRegistryConstructor( scannerImplClass, serviceRegistry );
		if ( fromServiceRegistryConstructor != null ) {
			return fromServiceRegistryConstructor;
		}

		throw new IllegalArgumentException(
				String.format(
						Locale.ROOT,
						"Configuration named a custom Scanner [%s] which did not define an expected constructor.",
						scannerImplClass.getName()
				)
		);
	}

	private static Scanner fromFullConstructor(
			Class<? extends Scanner> scannerImplClass,
			ArchiveDescriptorFactory archiveDescriptorFactory,
			StandardServiceRegistry serviceRegistry) {
		try {
			final Constructor<? extends Scanner> constructor = scannerImplClass.getConstructor( FULL_ARGS );
			try {
				return constructor.newInstance( archiveDescriptorFactory, serviceRegistry );
			}
			catch (Exception e) {
				return null;
			}
		}
		catch (NoSuchMethodException e) {
			return null;
		}
	}

	private static Scanner fromArchivingConstructor(Class<? extends Scanner> scannerImplClass, ArchiveDescriptorFactory archiveDescriptorFactory) {
		try {
			final Constructor<? extends Scanner> constructor = scannerImplClass.getConstructor( ARCHIVING_ARG );
			try {
				return constructor.newInstance( archiveDescriptorFactory );
			}
			catch (Exception e) {
				return null;
			}
		}
		catch (NoSuchMethodException e) {
			return null;
		}
	}

	private static Scanner fromServiceRegistryConstructor(Class<? extends Scanner> scannerImplClass, StandardServiceRegistry serviceRegistry) {
		try {
			final Constructor<? extends Scanner> constructor = scannerImplClass.getConstructor( REGISTRY_ARG );
			try {
				return constructor.newInstance( serviceRegistry );
			}
			catch (Exception e) {
				return null;
			}
		}
		catch (NoSuchMethodException e) {
			return null;
		}
	}

	public void applyScanResultsToManagedResources(
			ManagedResourcesImpl managedResources,
			ScanResult scanResult,
			BootstrapContext bootstrapContext,
			XmlMappingBinderAccess xmlMappingBinderAccess) {

		final ScanEnvironment scanEnvironment = bootstrapContext.getScanEnvironment();
		final ClassLoaderService classLoaderService =
				bootstrapContext.getServiceRegistry().requireService( ClassLoaderService.class );


		// mapping files ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

		final Set<String> nonLocatedMappingFileNames = new HashSet<>();
		final List<String> explicitMappingFileNames = scanEnvironment.getExplicitlyListedMappingFiles();
		if ( explicitMappingFileNames != null ) {
			nonLocatedMappingFileNames.addAll( explicitMappingFileNames );
		}

		if ( xmlMappingBinderAccess != null ) { // xml mapping is not disabled
			for ( MappingFileDescriptor mappingFileDescriptor : scanResult.getLocatedMappingFiles() ) {
				managedResources.addXmlBinding( xmlMappingBinderAccess.bind( mappingFileDescriptor.getStreamAccess() ) );
				nonLocatedMappingFileNames.remove( mappingFileDescriptor.getName() );
			}

			for ( String name : nonLocatedMappingFileNames ) {
				final URL url = classLoaderService.locateResource( name );
				if ( url == null ) {
					throw new MappingException(
							"Unable to resolve explicitly named mapping-file : " + name,
							new Origin( SourceType.RESOURCE, name )
					);
				}
				final UrlInputStreamAccess inputStreamAccess = new UrlInputStreamAccess( url );
				managedResources.addXmlBinding( xmlMappingBinderAccess.bind( inputStreamAccess ) );
			}
		}

		// classes and packages ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

		final List<String> unresolvedListedClassNames = scanEnvironment.getExplicitlyListedClassNames() == null
				? new ArrayList<>()
				: new ArrayList<>( scanEnvironment.getExplicitlyListedClassNames() );

		for ( ClassDescriptor classDescriptor : scanResult.getLocatedClasses() ) {
			if ( classDescriptor.categorization() == ClassDescriptor.Categorization.CONVERTER ) {
				// converter classes are safe to load because we never enhance them,
				// and notice we use the ClassLoaderService specifically, not the temp ClassLoader (if any)
				managedResources.addAttributeConverterDefinition(
						new ClassBasedConverterDescriptor(
								classLoaderService.classForName( classDescriptor.name() ),
								bootstrapContext.getClassmateContext()
						)
				);
			}
			else if ( classDescriptor.categorization() == ClassDescriptor.Categorization.MODEL ) {
				managedResources.addAnnotatedClassName( classDescriptor.name() );
			}
			unresolvedListedClassNames.remove( classDescriptor.name() );
		}

		// IMPL NOTE : "explicitlyListedClassNames" can contain class or package names...
		for ( PackageDescriptor packageDescriptor : scanResult.getLocatedPackages() ) {
			managedResources.addAnnotatedPackageName( packageDescriptor.name() );
			unresolvedListedClassNames.remove( packageDescriptor.name() );
		}

		for ( String unresolvedListedClassName : unresolvedListedClassNames ) {
			// because the explicit list can contain either class names or package names
			// we need to check for both here...

			// First, try it as a class name
			final String classFileName = unresolvedListedClassName.replace( '.', '/' ) + ".class";
			final URL classFileUrl = classLoaderService.locateResource( classFileName );
			if ( classFileUrl != null ) {
				managedResources.addAnnotatedClassName( unresolvedListedClassName );
				continue;
			}

			// Then, try it as a package name
			final String packageInfoFileName = unresolvedListedClassName.replace( '.', '/' ) + "/package-info.class";
			final URL packageInfoFileUrl = classLoaderService.locateResource( packageInfoFileName );
			if ( packageInfoFileUrl != null ) {
				managedResources.addAnnotatedPackageName( unresolvedListedClassName );
				continue;
			}

			// Last, try it by loading the class
			try {
				Class<?> clazz = classLoaderService.classForName( unresolvedListedClassName );
				managedResources.addAnnotatedClassReference( clazz );
				continue;
			}
			catch (ClassLoadingException ignore) {
				// ignore this error
			}

			if ( log.isDebugEnabled() ) {
				log.debugf(
						"Unable to resolve class [%s] named in persistence unit [%s]",
						unresolvedListedClassName,
						scanEnvironment.getRootUrl()
				);
			}
		}
	}
}
