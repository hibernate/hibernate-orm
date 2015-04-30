/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2015, Red Hat Inc. or third-party contributors as
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
package org.hibernate.osgi.test;

import java.io.File;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Calendar;
import java.util.Locale;
import java.util.StringTokenizer;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceProvider;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.selector.spi.StrategySelector;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.osgi.OsgiSessionFactoryService;
import org.hibernate.osgi.test.client.DataPoint;
import org.hibernate.osgi.test.client.SomeService;
import org.hibernate.osgi.test.client.TestIntegrator;
import org.hibernate.osgi.test.client.TestStrategyRegistrationProvider;
import org.hibernate.osgi.test.client.TestTypeContributor;
import org.hibernate.type.BasicType;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.ProbeBuilder;
import org.ops4j.pax.exam.TestProbeBuilder;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.karaf.options.LogLevelOption;
import org.ops4j.pax.exam.options.MavenUrlReference;
import org.ops4j.pax.exam.options.UrlReference;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.when;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.configureConsole;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.debugConfiguration;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.features;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.karafDistributionConfiguration;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.keepRuntimeFolder;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.logLevel;

/**
 * Tests for hibernate-osgi running within a Karaf container via PaxExam.
 *
 * @author Steve Ebersole
 */
@RunWith( PaxExam.class )
@ExamReactorStrategy( PerClass.class )
public class OsgiIntegrationTest {

	private static final boolean DEBUG = false;

	@Inject
	@SuppressWarnings("UnusedDeclaration")
	private BundleContext bundleContext;

	@Configuration
	public Option[] config() throws Exception {
		final boolean debug = ConfigurationHelper.getBoolean(
				"org.hibernate.testing.osgi.paxExam.debug",
				Environment.getProperties(),
				DEBUG
		);

		return options(
				when( debug ).useOptions( debugConfiguration( "5005", true ) ),
				karafDistributionConfiguration()
						.frameworkUrl(
								maven()
										.groupId( "org.apache.karaf" )
										.artifactId( "apache-karaf" )
										.type( "tar.gz" )
										.versionAsInProject()
						)
						.unpackDirectory( new File( "target/exam" ) )
						.useDeployFolder( false ),
				configureConsole()
						.ignoreLocalConsole()
						.ignoreRemoteShell(),
				when( debug ).useOptions( keepRuntimeFolder() ),
				logLevel( LogLevelOption.LogLevel.INFO ),
				// avoiding additional boot features; specifically "enterprise"
				editConfigurationFilePut(
						"etc/org.apache.karaf.features.cfg",
						"featuresBoot",
						"standard"
				),
				features( hibernateKarafFeatureFile(), "hibernate-core", "hibernate-entitymanager", "hibernate-osgi" )
		);
	}

	private UrlReference hibernateKarafFeatureFile() throws Exception {
		// get a URL reference to something we now is part of the classpath (us)
		final URL classUrl = OsgiSessionFactoryService.class.getClassLoader().getResource(
				OsgiSessionFactoryService.class.getName().replace( '.', '/' ) + ".class"
		);
		if ( classUrl == null ) {
			fail( "Unable to setup hibernate-osgi feature for PaxExam : could not resolve 'known class' url" );
		}

		// and convert it to a File
		final File classFile = new File( classUrl.getFile() );
		if ( !classFile.exists() ) {
			fail( "Unable to setup hibernate-osgi feature for PaxExam : could not resolve 'known class' url to File" );
		}

		// DoTheRightThing(tm) in case of refactoring...
		final int packageCount = new StringTokenizer( OsgiSessionFactoryService.class.getPackage().getName(), ".", false ).countTokens();
		File dir = classFile.getParentFile();
		for ( int i = 0; i < packageCount; i++ ) {
			dir = dir.getParentFile();
		}

		// dir now points to the root classes output dir, go up one more...
		dir = dir.getParentFile();

		// go to the `libs` dir...
		dir = new File( dir, "libs" );

		// and look for the feature file there...
		final String featureFileName = String.format(
				Locale.ENGLISH,
				"hibernate-osgi-%s-karaf.xml",
				determineProjectVersion()
		);

		final URL url = new File( dir, featureFileName ).toURI().toURL();
		return new UrlReference() {
			@Override
			public String getURL() {
				return url.toExternalForm();
			}
		};
	}

	private String determineProjectVersion() throws Exception {
		URL url = getClass().getClassLoader().getResource( "META-INF/hibernate-osgi/Version.txt" );
		InputStreamReader reader = new InputStreamReader( url.openStream() );
		char[] buffer = new char[50];
		int count = reader.read( buffer );
		return String.valueOf( buffer, 0, count );
	}

	@ProbeBuilder
	 public TestProbeBuilder probeConfiguration(TestProbeBuilder probe) {
		// attempt to override PaxExam's default of dynamically importing everything
		probe.setHeader( Constants.DYNAMICIMPORT_PACKAGE, "" );
		// and use defined imports instead
		probe.setHeader(
				Constants.IMPORT_PACKAGE,
				"javassist.util.proxy" +
						",javax.persistence" +
						",javax.persistence.spi" +
						",org.h2" +
						",org.osgi.framework" +
						",org.hibernate" +
						",org.hibernate.boot.model" +
						",org.hibernate.boot.registry.selector" +
						",org.hibernate.boot.registry.selector.spi" +
						",org.hibernate.cfg" +
						",org.hibernate.engine.spi" +
						",org.hibernate.integrator.spi" +
						",org.hibernate.proxy" +
						",org.hibernate.service" +
						",org.hibernate.service.spi"
		);
		probe.setHeader( Constants.BUNDLE_ACTIVATOR, "org.hibernate.osgi.test.client.OsgiTestActivator" );
		return probe;
	}

	@BeforeClass
	public static void setLocaleToEnglish() {
		Locale.setDefault( Locale.ENGLISH );
	}

	@Test
	public void testJpa() throws Exception {
		final ServiceReference serviceReference = bundleContext.getServiceReference( PersistenceProvider.class.getName() );
		final PersistenceProvider persistenceProvider = (PersistenceProvider) bundleContext.getService( serviceReference );
		final EntityManagerFactory emf = persistenceProvider.createEntityManagerFactory( "hibernate-osgi-test", null );

		EntityManager em = emf.createEntityManager();
		em.getTransaction().begin();
		em.persist( new DataPoint( "Brett" ) );
		em.getTransaction().commit();
		em.close();

		em = emf.createEntityManager();
		em.getTransaction().begin();
		DataPoint dp = em.find( DataPoint.class, 1 );
		assertNotNull( dp );
		assertEquals( "Brett", dp.getName() );
		em.getTransaction().commit();
		em.close();

		em = emf.createEntityManager();
		em.getTransaction().begin();
		dp = em.find( DataPoint.class, 1 );
		dp.setName( "Brett2" );
		em.getTransaction().commit();
		em.close();

		em = emf.createEntityManager();
		em.getTransaction().begin();
		em.createQuery( "delete from DataPoint" ).executeUpdate();
		em.getTransaction().commit();
		em.close();

		em = emf.createEntityManager();
		em.getTransaction().begin();
		dp = em.find( DataPoint.class, 1 );
		assertNull( dp );
		em.getTransaction().commit();
		em.close();
	}

	@Test
	public void testNative() throws Exception {
		final ServiceReference sr = bundleContext.getServiceReference( SessionFactory.class.getName() );
		final SessionFactory sf = (SessionFactory) bundleContext.getService( sr );

		Session s = sf.openSession();
		s.getTransaction().begin();
		s.persist( new DataPoint( "Brett" ) );
		s.getTransaction().commit();
		s.close();

		s = sf.openSession();
		s.getTransaction().begin();
		DataPoint dp = (DataPoint) s.get( DataPoint.class, 1 );
		assertNotNull( dp );
		assertEquals( "Brett", dp.getName() );
		s.getTransaction().commit();
		s.close();

		dp.setName( "Brett2" );

		s = sf.openSession();
		s.getTransaction().begin();
		s.update( dp );
		s.getTransaction().commit();
		s.close();

		s = sf.openSession();
		s.getTransaction().begin();
		dp = (DataPoint) s.get( DataPoint.class, 1 );
		assertNotNull( dp );
		assertEquals( "Brett2", dp.getName() );
		s.getTransaction().commit();
		s.close();

		s = sf.openSession();
		s.getTransaction().begin();
		s.createQuery( "delete from DataPoint" ).executeUpdate();
		s.getTransaction().commit();
		s.close();

		s = sf.openSession();
		s.getTransaction().begin();
		dp = (DataPoint) s.get( DataPoint.class, 1 );
		assertNull( dp );
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testExtensionPoints() throws Exception {
		final ServiceReference sr = bundleContext.getServiceReference( SessionFactory.class.getName() );
		final SessionFactoryImplementor sfi = (SessionFactoryImplementor) bundleContext.getService( sr );

		assertTrue( TestIntegrator.passed() );

		Class impl = sfi.getServiceRegistry().getService( StrategySelector.class ).selectStrategyImplementor( Calendar.class, TestStrategyRegistrationProvider.GREGORIAN );
		assertNotNull( impl );

		BasicType basicType = sfi.getTypeResolver().basic( TestTypeContributor.NAME );
		assertNotNull( basicType );
	}

	@Test
	public void testServiceContributorDiscovery() throws Exception {
		final ServiceReference sr = bundleContext.getServiceReference( SessionFactory.class.getName() );
		final SessionFactoryImplementor sfi = (SessionFactoryImplementor) bundleContext.getService( sr );

		assertNotNull( sfi.getServiceRegistry().getService( SomeService.class ) );
	}
}
