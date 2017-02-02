package org.hibernate.boot.model.process.internal;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;

import org.hibernate.boot.archive.scan.spi.ScanEnvironment;
import org.hibernate.boot.archive.scan.spi.ScanResult;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.spi.MetadataBuildingOptions;
import org.hibernate.boot.spi.XmlMappingBinderAccess;
import org.hibernate.internal.CoreMessageLogger;

import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.testing.logger.LoggerInspectionRule;
import org.hibernate.testing.logger.Triggerable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.jboss.logging.Logger;

import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

/**
 * @author Vlad Mihalcea
 */
public class ScanningCoordinatorTest extends BaseUnitTestCase {

	private ManagedResourcesImpl managedResources = Mockito.mock( ManagedResourcesImpl.class );
	private ScanResult scanResult = Mockito.mock( ScanResult.class );
	private MetadataBuildingOptions options = Mockito.mock( MetadataBuildingOptions.class );
	private XmlMappingBinderAccess xmlMappingBinderAccess = Mockito.mock( XmlMappingBinderAccess.class );

	private ScanEnvironment scanEnvironment = Mockito.mock( ScanEnvironment.class );
	private StandardServiceRegistry serviceRegistry = Mockito.mock( StandardServiceRegistry.class );

	private ClassLoaderService classLoaderService = Mockito.mock( ClassLoaderService.class );

	private Triggerable triggerable;

	@Rule
	public LoggerInspectionRule logInspection = new LoggerInspectionRule(
			Logger.getMessageLogger( CoreMessageLogger.class, ScanningCoordinator.class.getName() ) );

	@Before
	public void init(){
		Mockito.reset( scanEnvironment );

		when( options.getScanEnvironment() ).thenReturn( scanEnvironment );
		when( options.getServiceRegistry() ).thenReturn( serviceRegistry );
		when( serviceRegistry.getService( ClassLoaderService.class ) ).thenReturn( classLoaderService );

		when( scanEnvironment.getExplicitlyListedClassNames() ).thenReturn(
				Arrays.asList( "a.b.C" ) );

		when( classLoaderService.classForName( "a.b.C" ) ).thenReturn( Object.class );

		triggerable = logInspection.watchForLogMessages( "Unable" );
		triggerable.reset();
	}

	@Test
	public void testApplyScanResultsToManagedResourcesWithNullRootUrl() {

		ScanningCoordinator.INSTANCE.applyScanResultsToManagedResources(
				managedResources,
				scanResult,
				options,
				xmlMappingBinderAccess
		);
		assertEquals( "Unable to resolve class [a.b.C] named in persistence unit [null]", triggerable.triggerMessage() );
	}

	@Test
	public void testApplyScanResultsToManagedResourcesWithNotNullRootUrl()
			throws MalformedURLException {
		when( scanEnvironment.getRootUrl() ).thenReturn( new URL( "http://http://hibernate.org/" ) );

		ScanningCoordinator.INSTANCE.applyScanResultsToManagedResources(
				managedResources,
				scanResult,
				options,
				xmlMappingBinderAccess
		);
		assertEquals( "Unable to resolve class [a.b.C] named in persistence unit [http://http://hibernate.org/]", triggerable.triggerMessage() );
	}

}