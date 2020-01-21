/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.osgi.test;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Calendar;
import java.util.Locale;
import java.util.Properties;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceProvider;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.selector.spi.StrategySelector;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.osgi.test.client.AuditedDataPoint;
import org.hibernate.osgi.test.client.DataPoint;
import org.hibernate.osgi.test.client.SomeService;
import org.hibernate.osgi.test.client.TestIntegrator;
import org.hibernate.osgi.test.client.TestStrategyRegistrationProvider;
import org.hibernate.osgi.test.client.TestTypeContributor;
import org.hibernate.type.BasicType;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

import org.apache.karaf.features.BootFinished;
import org.apache.karaf.features.FeaturesService;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.ProbeBuilder;
import org.ops4j.pax.exam.TestProbeBuilder;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.karaf.options.LogLevelOption;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.when;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.configureConsole;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.debugConfiguration;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFileExtend;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.features;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.karafDistributionConfiguration;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.keepRuntimeFolder;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.logLevel;

/**
 * Tests for hibernate-osgi running within a Karaf container via PaxExam.
 *
 * @author Steve Ebersole
 * @author Brett Meyer
 */
@RunWith( PaxExam.class )
@ExamReactorStrategy( PerClass.class )
public class OsgiIntegrationTest {

	private static final boolean DEBUG = false;
	private static final String jbossPublicRepository = "https://repository.jboss.org/nexus/content/groups/public-jboss/";
	private static final String mavenCentralRepository = "https://repo.maven.apache.org/maven2/";

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Prepare the Karaf container

	@Configuration
	public Option[] config() throws Exception {
		final Properties paxExamEnvironment = loadPaxExamEnvironmentProperties();

		final boolean debug = ConfigurationHelper.getBoolean(
				"org.hibernate.testing.osgi.paxExam.debug",
				Environment.getProperties(),
				DEBUG
		);

		return options(
				when( debug ).useOptions( debugConfiguration( "5005", true ) ),
				karafDistributionConfiguration()
						.frameworkUrl( paxExamEnvironment.getProperty( "org.ops4j.pax.exam.container.karaf.distroUrl" ) )
						.karafVersion( paxExamEnvironment.getProperty( "org.ops4j.pax.exam.container.karaf.version" ) )
						.name( "Apache Karaf" )
						.unpackDirectory(
								new File(
										paxExamEnvironment.getProperty(
												"org.ops4j.pax.exam.container.karaf.unpackDir"
										)
								)
						)
						.useDeployFolder( false ),
				editConfigurationFilePut( // Erase the defaults: Maven Central uses HTTP by default, but HTTPS is required now.
						"etc/org.ops4j.pax.url.mvn.cfg",
						"org.ops4j.pax.url.mvn.repositories",
						mavenCentralRepository
								+ "@id=central"
								+ ", "
							+ jbossPublicRepository
								+ "@id=jboss-public-repository"
						+ "https://repository.jboss.org/nexus/content/groups/public/"
				),
				configureConsole().ignoreLocalConsole().ignoreRemoteShell(),
				when( debug ).useOptions( keepRuntimeFolder() ),
				logLevel( LogLevelOption.LogLevel.INFO ),
				// also log to the console, so that the logs are writtten to the test output file
				editConfigurationFilePut(
						"etc/org.ops4j.pax.logging.cfg",
						"log4j2.rootLogger.appenderRef.Console.filter.threshold.level",
						"TRACE" // Means "whatever the root logger level is"
				),

				features( featureXmlUrl( paxExamEnvironment ), "hibernate-orm" ),
				features( featureXmlUrl( paxExamEnvironment ), "hibernate-envers" ),
				features( testingFeatureXmlUrl(), "hibernate-osgi-testing" )
		);
	}

	private static Properties loadPaxExamEnvironmentProperties() throws IOException {
		Properties props = new Properties();
		props.load( OsgiIntegrationTest.class.getResourceAsStream( "/pax-exam-environment.properties" ) );
		return props;
	}

	private static String featureXmlUrl(Properties paxExamEnvironment) throws MalformedURLException {
		return new File( paxExamEnvironment.getProperty( "org.hibernate.osgi.test.karafFeatureFile" ) ).toURI().toURL().toExternalForm();
	}

	private String testingFeatureXmlUrl() {
		return OsgiIntegrationTest.class.getClassLoader().getResource( "org/hibernate/osgi/test/testing-bundles.xml" )
				.toExternalForm();
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Prepare the PaxExam probe (the bundle to deploy)


	@ProbeBuilder
	public TestProbeBuilder probeConfiguration(TestProbeBuilder probe) {
		System.out.println( "Configuring probe..." );

		// Note : I found locally that this part is not needed.  But I am leaving this here as I might
		// 		someday have a need for tweaking the probe and I want to remember how it is done...

//		// attempt to override PaxExam's default of dynamically importing everything
//		probe.setHeader( Constants.DYNAMICIMPORT_PACKAGE, "" );
//		// and use defined imports instead
//		probe.setHeader(
//				Constants.IMPORT_PACKAGE,
//				"javassist.util.proxy"
//						+ ",javax.persistence"
//						+ ",javax.persistence.spi"
//						+ ",org.h2"
//						+ ",org.osgi.framework"
//						+ ",org.hibernate"
//						+ ",org.hibernate.envers"
////						+ ",org.hibernate.boot.model"
////						+ ",org.hibernate.boot.registry.selector"
////						+ ",org.hibernate.boot.registry.selector.spi"
////						+ ",org.hibernate.cfg"
////						+ ",org.hibernate.engine.spi"
////						+ ",org.hibernate.integrator.spi"
////						+ ",org.hibernate.proxy"
////						+ ",org.hibernate.service"
////						+ ",org.hibernate.service.spi"
////						+ ",org.ops4j.pax.exam.options"
////						+ ",org.ops4j.pax.exam"
//		);
		probe.setHeader( Constants.BUNDLE_ACTIVATOR, "org.hibernate.osgi.test.client.OsgiTestActivator" );
		return probe;
	}

	@BeforeClass
	public static void setLocaleToEnglish() {
		Locale.setDefault( Locale.ENGLISH );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// The tests

	@Inject
	protected FeaturesService featuresService;

	@Inject
	BootFinished bootFinished;

	@Inject
	@SuppressWarnings("UnusedDeclaration")
	private BundleContext bundleContext;

	@Test
	public void testActivation() throws Exception {
		assertTrue( featuresService.isInstalled( featuresService.getFeature( "hibernate-orm" ) ) );
		assertTrue( featuresService.isInstalled( featuresService.getFeature( "hibernate-envers" ) ) );

		assertActiveBundle( "org.hibernate.orm.core" );
		assertActiveBundle( "org.hibernate.orm.envers" );
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
	public void testNativeEnvers() throws Exception {
		final ServiceReference sr = bundleContext.getServiceReference( SessionFactory.class.getName() );
		final SessionFactory sf = ( SessionFactory )bundleContext.getService( sr );

		final Integer adpId;

		Session s = sf.openSession();
		s.getTransaction().begin();
		AuditedDataPoint adp = new AuditedDataPoint( "Chris" );
		s.persist( adp );
		s.getTransaction().commit();
		adpId = adp.getId();
		s.close();

		s = sf.openSession();
		s.getTransaction().begin();
		adp = s.get( AuditedDataPoint.class, adpId );
		adp.setName( "Chris2" );
		s.getTransaction().commit();
		s.close();

		s = sf.openSession();
		AuditReader ar = AuditReaderFactory.get( s );
		assertEquals( 2, ar.getRevisions( AuditedDataPoint.class, adpId ).size() );
		AuditedDataPoint rev1 = ar.find( AuditedDataPoint.class, adpId, 1 );
		AuditedDataPoint rev2 = ar.find( AuditedDataPoint.class, adpId, 2 );
		assertEquals( new AuditedDataPoint( adpId, "Chris" ), rev1 );
		assertEquals( new AuditedDataPoint( adpId, "Chris2" ), rev2 );
		s.close();
	}

	@Test
	public void testJpaEnvers() throws Exception {
		final ServiceReference serviceReference = bundleContext.getServiceReference( PersistenceProvider.class.getName() );
		final PersistenceProvider persistenceProvider = (PersistenceProvider) bundleContext.getService( serviceReference );
		final EntityManagerFactory emf = persistenceProvider.createEntityManagerFactory( "hibernate-osgi-test", null );

		final Integer adpId;

		EntityManager em = emf.createEntityManager();
		em.getTransaction().begin();
		AuditedDataPoint adp = new AuditedDataPoint( "Chris" );
		em.persist( adp );
		em.getTransaction().commit();
		adpId = adp.getId();
		em.close();

		em = emf.createEntityManager();
		em.getTransaction().begin();
		adp = em.find( AuditedDataPoint.class, adpId );
		adp.setName( "Chris2" );
		em.getTransaction().commit();
		em.close();

		em = emf.createEntityManager();
		AuditReader ar = AuditReaderFactory.get( em );
		assertEquals( 2, ar.getRevisions( AuditedDataPoint.class, adpId ).size() );
		AuditedDataPoint rev1 = ar.find( AuditedDataPoint.class, adpId, 1 );
		AuditedDataPoint rev2 = ar.find( AuditedDataPoint.class, adpId, 2 );
		assertEquals( new AuditedDataPoint( adpId, "Chris" ), rev1 );
		assertEquals( new AuditedDataPoint( adpId, "Chris2" ), rev2 );
		em.close();
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

	private void assertActiveBundle(String symbolicName) {
		for (Bundle bundle : bundleContext.getBundles()) {
			if (bundle.getSymbolicName().equals( symbolicName )) {
				Assert.assertEquals(
						symbolicName + " was found, but not in an ACTIVE state.", Bundle.ACTIVE, bundle.getState());
				return;
			}
		}
		Assert.fail("Could not find bundle: " + symbolicName);
	}
}
