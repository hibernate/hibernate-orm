/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bootstrap.scanning;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;

import org.hibernate.archive.scan.internal.ClassDescriptorImpl;
import org.hibernate.boot.archive.scan.internal.DisabledScanner;
import org.hibernate.archive.scan.internal.MappingFileDescriptorImpl;
import org.hibernate.archive.scan.internal.PackageDescriptorImpl;
import org.hibernate.archive.scan.internal.ScanResultImpl;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.archive.internal.ByteArrayInputStreamAccess;
import org.hibernate.boot.archive.scan.spi.ClassDescriptor;
import org.hibernate.boot.archive.scan.spi.MappingFileDescriptor;
import org.hibernate.boot.archive.scan.spi.PackageDescriptor;
import org.hibernate.boot.archive.scan.spi.ScanEnvironment;
import org.hibernate.boot.archive.scan.spi.ScanOptions;
import org.hibernate.boot.archive.scan.spi.ScanParameters;
import org.hibernate.boot.archive.scan.spi.ScanResult;
import org.hibernate.boot.archive.scan.spi.Scanner;
import org.hibernate.boot.archive.spi.InputStreamAccess;
import org.hibernate.boot.internal.ClassmateContext;
import org.hibernate.boot.model.convert.spi.ConverterDescriptor;
import org.hibernate.boot.model.process.internal.ManagedResourcesImpl;
import org.hibernate.boot.model.process.internal.ScanningCoordinator;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.boot.spi.MetadataBuildingOptions;
import org.hibernate.boot.spi.XmlMappingBinderAccess;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Logger;
import org.hibernate.testing.orm.junit.MessageKeyInspection;
import org.hibernate.testing.orm.junit.MessageKeyWatcher;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Vlad Mihalcea
 * @author Petteri Pitkanen
 */
@MessageKeyInspection(
		messageKey = "Unable to resolve class [a.b.C] named in persistence unit",
		logger = @Logger( loggerNameClass = ScanningCoordinator.class )
)
public class ScanningCoordinatorTest {

	private ManagedResourcesImpl managedResources = Mockito.mock( ManagedResourcesImpl.class );
	private ScanResult scanResult = Mockito.mock( ScanResult.class );
	private BootstrapContext bootstrapContext = Mockito.mock( BootstrapContext.class );
	private ClassmateContext classmateContext = new ClassmateContext();
	private XmlMappingBinderAccess xmlMappingBinderAccess = Mockito.mock( XmlMappingBinderAccess.class );
	private MetadataBuildingOptions metadataBuildingOptions = Mockito.mock( MetadataBuildingOptions.class );

	private ScanEnvironment scanEnvironment = Mockito.mock( ScanEnvironment.class );
	private StandardServiceRegistry serviceRegistry = Mockito.mock( StandardServiceRegistry.class );

	private ClassLoaderService classLoaderService = Mockito.mock( ClassLoaderService.class );

	@BeforeEach
	public void init() {
		Mockito.reset( managedResources );
		Mockito.reset( scanResult );
		Mockito.reset( bootstrapContext );
		Mockito.reset( scanEnvironment );
		Mockito.reset( classLoaderService );

		when( bootstrapContext.getScanEnvironment() ).thenReturn( scanEnvironment );
		when( bootstrapContext.getClassmateContext() ).thenReturn( classmateContext );
		when( bootstrapContext.getServiceRegistry() ).thenReturn( serviceRegistry );
		when( bootstrapContext.getMetadataBuildingOptions() ).thenReturn( metadataBuildingOptions );

		when( serviceRegistry.requireService( ClassLoaderService.class ) ).thenReturn( classLoaderService );

		when( metadataBuildingOptions.isXmlMappingEnabled() ).thenReturn( true );

		when( scanEnvironment.getExplicitlyListedClassNames() ).thenReturn(
				Arrays.asList( "a.b.C" ) );

		when( classLoaderService.classForName( eq( "a.b.C" ) ) ).thenThrow( ClassLoadingException.class );
		when( classLoaderService.locateResource( eq( "a/b/c.class" ) ) ).thenReturn( null );
		when( classLoaderService.locateResource( eq( "a/b/c/package-info.class" ) ) ).thenReturn( null );
	}

	@Test
	public void testApplyScanResultsToManagedResourcesWithNullRootUrl(MessageKeyWatcher watcher) {

		ScanningCoordinator.INSTANCE.applyScanResultsToManagedResources(
				managedResources,
				scanResult,
				bootstrapContext,
				xmlMappingBinderAccess
		);
		assertEquals( "Unable to resolve class [a.b.C] named in persistence unit [null]", watcher.getFirstTriggeredMessage() );
	}

	@Test
	public void testApplyScanResultsToManagedResourcesWithNotNullRootUrl(MessageKeyWatcher watcher)
			throws MalformedURLException {
		when( scanEnvironment.getRootUrl() ).thenReturn( new URL( "http://http://hibernate.org/" ) );

		ScanningCoordinator.INSTANCE.applyScanResultsToManagedResources(
				managedResources,
				scanResult,
				bootstrapContext,
				xmlMappingBinderAccess
		);
		assertEquals( "Unable to resolve class [a.b.C] named in persistence unit [http://http://hibernate.org/]", watcher.getFirstTriggeredMessage() );
	}

	@Test
	@JiraKey(value = "HHH-14473")
	public void testApplyScanResultsToManagedResultsWhileExplicitClassNameLoadable() {
		Class<Object> expectedClass = Object.class;
		when( classLoaderService.classForName( eq( "a.b.C" ) ) ).thenReturn( expectedClass );

		ScanningCoordinator.INSTANCE.applyScanResultsToManagedResources(
				managedResources,
				scanResult,
				bootstrapContext,
				xmlMappingBinderAccess
		);

		verify( managedResources, times( 0 ) ).addAnnotatedClassName( any() );
		verify( managedResources, times( 1 ) ).addAnnotatedClassReference( same( expectedClass ) );
		verify( classLoaderService, times( 1 ) ).classForName( eq( "a.b.C" ) );
	}

	@Test
	@JiraKey(value = "HHH-12505")
	public void testManagedResourcesAfterCoordinateScanWithDisabledScanner() {
		assertManagedResourcesAfterCoordinateScanWithScanner( new DisabledScanner(), true );
	}

	@Test
	@JiraKey(value = "HHH-12505")
	public void testManagedResourcesAfterCoordinateScanWithCustomEnabledScanner() {
		final Scanner scanner = new Scanner() {
			@Override
			public ScanResult scan(final ScanEnvironment environment, final ScanOptions options, final ScanParameters parameters) {
				final InputStreamAccess dummyInputStreamAccess = new ByteArrayInputStreamAccess( "dummy", new byte[0] );
				return new ScanResultImpl(
						Collections.<PackageDescriptor>singleton( new PackageDescriptorImpl( "dummy", dummyInputStreamAccess ) ),
						Collections.<ClassDescriptor>singleton( new ClassDescriptorImpl( "dummy", ClassDescriptor.Categorization.MODEL, dummyInputStreamAccess ) ),
						Collections.<MappingFileDescriptor>singleton( new MappingFileDescriptorImpl( "dummy", dummyInputStreamAccess ) )
				);
			}
		};
		assertManagedResourcesAfterCoordinateScanWithScanner( scanner, false );
	}

	@Test
	@JiraKey(value = "HHH-10778")
	public void testManagedResourcesAfterCoordinateScanWithConverterScanner() {

		when( classLoaderService.classForName( "converter" ) ).thenReturn( (Class) IntegerToVarcharConverter.class );

		final Scanner scanner = (ScanEnvironment environment, ScanOptions options, ScanParameters parameters) -> {
			final InputStreamAccess dummyInputStreamAccess = new ByteArrayInputStreamAccess( "dummy", new byte[0] );

			return new ScanResultImpl(
					Collections.singleton( new PackageDescriptorImpl( "dummy", dummyInputStreamAccess ) ),
					Collections.singleton( new ClassDescriptorImpl(
							"converter",
							ClassDescriptor.Categorization.CONVERTER,
							dummyInputStreamAccess
					) ),
					Collections.singleton( new MappingFileDescriptorImpl( "dummy", dummyInputStreamAccess ) )
			);
		};

		when( bootstrapContext.getScanner() ).thenReturn( scanner );

		final ManagedResourcesImpl managedResources = ManagedResourcesImpl.baseline(
				new MetadataSources(),
				bootstrapContext
		);

		ScanningCoordinator.INSTANCE.coordinateScan( managedResources, bootstrapContext, xmlMappingBinderAccess );

		assertEquals( 1, scanEnvironment.getExplicitlyListedClassNames().size() );
		assertEquals( "a.b.C", scanEnvironment.getExplicitlyListedClassNames().get( 0 ) );

		assertEquals( 1, managedResources.getAttributeConverterDescriptors().size() );
		ConverterDescriptor attributeConverterInfo = managedResources.getAttributeConverterDescriptors()
				.iterator()
				.next();
		assertEquals( IntegerToVarcharConverter.class, attributeConverterInfo.getAttributeConverterClass() );
	}

	/**
	 * Run coordinateScan() with the given Scanner and assert the emptiness
	 * of ManagedResources.
	 */
	private void assertManagedResourcesAfterCoordinateScanWithScanner(final Scanner scanner, final boolean expectedIsManagedResourcesEmpty) {
		when( bootstrapContext.getScanner() ).thenReturn( scanner );

		final ManagedResourcesImpl managedResources = ManagedResourcesImpl.baseline( new MetadataSources(), bootstrapContext );

		ScanningCoordinator.INSTANCE.coordinateScan( managedResources, bootstrapContext, xmlMappingBinderAccess );

		assertEquals( 1, scanEnvironment.getExplicitlyListedClassNames().size() );
		assertEquals( "a.b.C", scanEnvironment.getExplicitlyListedClassNames().get( 0 ) );

		assertEquals( true, managedResources.getAttributeConverterDescriptors().isEmpty() );
		assertEquals( true, managedResources.getAnnotatedClassReferences().isEmpty() );
		assertEquals( expectedIsManagedResourcesEmpty, managedResources.getAnnotatedClassNames().isEmpty() );
		assertEquals( expectedIsManagedResourcesEmpty, managedResources.getAnnotatedPackageNames().isEmpty() );
		assertEquals( expectedIsManagedResourcesEmpty, managedResources.getXmlMappingBindings().isEmpty() );
	}
}
