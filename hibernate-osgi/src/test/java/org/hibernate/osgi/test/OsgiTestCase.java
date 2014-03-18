/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hibernate.osgi.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;

import org.hibernate.Hibernate;
import org.hibernate.boot.registry.selector.StrategyRegistrationProvider;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.metamodel.spi.TypeContributor;
import org.hibernate.osgi.OsgiPersistenceProviderService;
import org.hibernate.osgi.OsgiSessionFactoryService;
import org.hibernate.osgi.test.client.DataPoint;
import org.hibernate.osgi.test.client.TestService;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.osgi.metadata.OSGiManifestBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 * A separate sourceset, testClientBundle, contains a persistence unit and an OSGi service interface providing
 * multiple uses of Native and JPA functionality.  The use of a SF/EMF must occur in that separate bundle, rather than
 * attempting to programmatically create a bundle and obtain/use an SF/EMF here.  There are
 * MANY ClassLoader issues with that sort of setup.  JPA annotations are "stripped", since one ClassLoader is used here
 * to create the entity's stream and another is used to parse it within core.  Further, the entire Felix framework
 * is given to hibernate-osgi as the "requestingBundle" in that setup, regardless of Arquillian vs. Pax Exam.  That
 * causes another slew of ClassLoader issues as well.
 * 
 * This is the most "realistic" type of test anyway with a *real* client bundle.
 * 
 * IMPORTANT: There are a few maintenance points that need addressed for new versions of Hibernate and library upgrades:
 * 1.) Updated library versions in hibernate-osgi.gradle.  libraries.gradle is used wherever possible.  But, there
 *     may be a few manual updates needed.
 * 2.) If a new version of Felix is used, download and start it manually in the command line.  Run
 *     "felix:headers 0" to obtain the list of packages exported and used by the framework.  As of this writing,
 *     the framework has javax.transaction.* and javax.xml.stream.* in "uses" attributes.  I had to remove all instances
 *     of those packages in the "uses" to correct dependency conflicts that are fairly well documented in the community.
 *     "org.osgi.framework.BundleException: Uses constraint violation..." occurs when a specific version of a package is exported
 *     by a bundle (ex: our JPA 2.1), the same package is exported by another bundle (without a version), and the package appears in a
 *     "uses" attribute (without a version).  Rather than do something hacky in the hibernate-osgi manifest itself,
 *     src/test/resources/felix-framework.properties contains the entire list as a property ("org.osgi.framework.system.packages"),
 *     stripped of the javax.transaction nonsense.  This may need to be repeated if Felix is ever updated in ORM
 *     (should be rare).
 *     
 * This should largerly be considered an integration test, rather than a granular unit test.  Also, this is almost
 * guaranteed to not work in your IDE.
 *
 * @author Brett Meyer
 */
@RunWith(Arquillian.class)
public class OsgiTestCase {

	@ArquillianResource
	BundleContext context;

	/**
	 * Sets up the Arquillian "deployment", creating a bundle with this test class and the framework.
	 * 
	 * @return JavaArchive
	 */
	@Deployment
	public static JavaArchive deployment() {
		final JavaArchive archive = ShrinkWrap.create( JavaArchive.class, "hibernate-osgi-test" );

		archive.setManifest( new Asset() {
			@Override
			public InputStream openStream() {
				final OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
				builder.addBundleSymbolicName( archive.getName() );
				builder.addBundleManifestVersion( 2 );
				builder.addImportPackages( TestService.class );
				// ORM packages needed in the tests
				builder.addImportPackages( Hibernate.class );
				builder.addImportPackages( Integrator.class );
				builder.addImportPackages( StrategyRegistrationProvider.class );
				builder.addImportPackages( TypeContributor.class );
				builder.addImportPackages( OsgiSessionFactoryService.class );
				return builder.openStream();
			}
		} );

		return archive;
	}

	/**
	 * Test the persistence unit bundle.
	 * 
	 * @throws Exception
	 */
	@Test
	@InSequence(1)
	public void testClientBundle() throws Exception {
		commonTests();

		final Bundle testClientBundle = findHibernateBundle( "testClientBundle" );
		assertNotNull( "The test client bundle was not found!", testClientBundle );
		testClientBundle.start();
		assertEquals( "The test client bundle was not activated!", Bundle.ACTIVE, testClientBundle.getState() );
	}
	
	@Test
	@InSequence(2)
	public void testJpa() throws Exception {
		commonTests();

		final TestService testService = getTestService();
		
		DataPoint dp = new DataPoint();
		dp.setName( "Brett" );
		testService.saveJpa( dp );

		dp = testService.getJpa(dp.getId());
		assertNotNull( dp );
		assertEquals( "Brett", dp.getName() );
		
		dp.setName( "Brett2" );
		testService.updateJpa( dp );

		dp = testService.getJpa(dp.getId());
		assertNotNull( dp );
		assertEquals( "Brett2", dp.getName() );

		testService.deleteJpa();

		dp = testService.getJpa(dp.getId());
		assertNull( dp );
	}
	
	@Test
	@InSequence(2)
	public void testNative() throws Exception {
		commonTests();

		final TestService testService = getTestService();
		
		DataPoint dp = new DataPoint();
		dp.setName( "Brett" );
		testService.saveNative( dp );

		dp = testService.getNative(dp.getId());
		assertNotNull( dp );
		assertEquals( "Brett", dp.getName() );
		
		dp.setName( "Brett2" );
		testService.updateNative( dp );

		dp = testService.getNative(dp.getId());
		assertNotNull( dp );
		assertEquals( "Brett2", dp.getName() );

		testService.deleteNative();

		dp = testService.getNative(dp.getId());
		assertNull( dp );
	}
	
	@Test
	@InSequence(2)
	public void testLazyLoading() throws Exception {
		commonTests();

		final TestService testService = getTestService();
		
		DataPoint dp = new DataPoint();
		dp.setName( "Brett" );
		testService.saveNative( dp );
		
		// #lazyLoad will init dp on its own
		dp = testService.lazyLoad( dp.getId() );
		assertNotNull( dp );
		assertTrue( Hibernate.isInitialized( dp ) );
		assertEquals( "Brett", dp.getName() );
	}
	
	@Test
	@InSequence(2)
	public void testExtensionPoints() throws Exception {
		commonTests();
		
		final TestService testService = getTestService();

		assertNotNull( testService.getTestIntegrator() );
		assertTrue( testService.getTestIntegrator().passed() );
		
		assertNotNull( testService.getTestStrategyRegistrationProvider() );
		assertTrue( testService.getTestStrategyRegistrationProvider().passed() );
		
		assertNotNull( testService.getTestTypeContributor() );
		assertTrue( testService.getTestTypeContributor().passed() );
	}
	
	/**
	 * Test that stopping the hibernate-osgi bundle happens cleanly.
	 * 
	 * TODO: This will be really simplistic at first, but should be expanded upon.
	 * 
	 * @throws Exception
	 */
	@Test
	// Arquillian does not restart the container between runs (afaik).  Without the ordering, the tests will
	// intermittently fail since this method stops the bundle.
	@InSequence(3)
	public void testStop() throws Exception {
		commonTests();

		findHibernateBundle( "org.hibernate.osgi" ).stop();
		testHibernateBundle( "org.hibernate.osgi", Bundle.RESOLVED );
		
		assertNull( context.getServiceReference( OsgiSessionFactoryService.class ) );
		assertNull( context.getServiceReference( OsgiPersistenceProviderService.class ) );
	}
	
	private void commonTests() {
		assertNotNull( "BundleContext injected", context );
		assertEquals( "System Bundle ID", 0, context.getBundle().getBundleId() );

		testHibernateBundle( "org.hibernate.core", Bundle.ACTIVE );
		testHibernateBundle( "org.hibernate.entitymanager", Bundle.ACTIVE );
		testHibernateBundle( "org.hibernate.osgi", Bundle.ACTIVE );
	}

	private Bundle findHibernateBundle(String symbolicName) {
		for ( Bundle bundle : context.getBundles() ) {
			if ( bundle.getSymbolicName().equals( symbolicName ) ) {
				return bundle;
			}
		}
		return null;
	}

	private void testHibernateBundle(String symbolicName, int state) {
		final Bundle bundle = findHibernateBundle( symbolicName );

		assertNotNull( "Bundle " + symbolicName + " was not found!", bundle );
		assertEquals( "Bundle " + symbolicName + " was not in the expected state!", state, bundle.getState() );
	}
	
	private TestService getTestService() {
		final ServiceReference<?> serviceReference = context.getServiceReference( TestService.class.getName() );
		return (TestService) context.getService( serviceReference );
	}
}
