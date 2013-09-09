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
import static org.junit.Assert.fail;

import java.io.InputStream;

import org.hibernate.osgi.OsgiPersistenceProviderService;
import org.hibernate.osgi.OsgiSessionFactoryService;
import org.hibernate.osgi.test.result.OsgiTestResults;
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
 * A separate sourceset, testClientBundle, contains a persistence unit and multiple uses of Native and JPA functionality.
 * Any failures that occur are logged in the OsgiTestResult service, contained in another sourceset (testResult).
 * 
 * The "unit tests" MUST reside in testClientBundle, rather than attempting to programmatically create a bundle and obtain an SF/EMF here.  There are
 * MANY ClassLoader issues with that sort of setup.  JPA annotations are "stripped", since one ClassLoader is used here
 * to create the entity's stream and another is used to parse it within core.  Further, the entire Felix framework
 * is given to hibernate-osgi as the "requestingBundle" in that setup, regardless of Arquillian vs. Pax Exam.  That
 * causes another slew of ClassLoader issues as well.
 * 
 * It is also important to keep OsgiTestResult in a third sourceset, rather than attempting to put it in test or
 * testClientBundle.  Adding testClientBundle to test's classpath causes more ClassLoader issues during runtime (and
 * vice versa), similar to the above.
 * 
 * The bottom line is that many, many alternatives were prototyped and all of them eventually hit brick walls.
 * Regardless, this is the most "realistic" type of test anyway with a *real* client bundle.
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
 * This should largerly be considered an integration test, rather than a granular unit test.  Depending on how you setup
 * the source directories and classpaths, this may not work in your IDE.
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
				builder.addImportPackages( OsgiTestResults.class );
				// needed primarily to test service cleanup in #testStop
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

		final ServiceReference<?> serviceReference = context.getServiceReference( OsgiTestResults.class.getName() );
		final OsgiTestResults testResults = (OsgiTestResults) context.getService( serviceReference );

		if ( testResults.getFailures().size() > 0 ) {
			fail( testResults.getFailures().get( 0 ).getFailure() );
		}
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
	@InSequence(2)
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
}
