/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.ejb.test;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.Dialect;
import org.hibernate.ejb.AvailableSettings;
import org.hibernate.ejb.Ejb3Configuration;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.service.internal.ServiceRegistryImpl;

import org.hibernate.testing.AfterClassOnce;
import org.hibernate.testing.BeforeClassOnce;
import org.hibernate.testing.junit4.BaseUnitTestCase;

import static org.hibernate.testing.TestLogger.LOG;

/**
 * A base class for all ejb tests.
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
public abstract class BaseEntityManagerFunctionalTestCase extends BaseUnitTestCase {
	private static final Dialect dialect = Dialect.getDialect();
	private static ServiceRegistryImpl serviceRegistry;
	private static EntityManagerFactory factory;
	private EntityManager em;
	private ArrayList<EntityManager> isolatedEms = new ArrayList<EntityManager>();

	public static Dialect getDialect() {
		return dialect;
	}

	@BeforeClassOnce
	private void buildSessionFactory() throws Exception {
		LOG.trace( "Building session factory" );
		Ejb3Configuration ejb3Configuration = buildConfiguration();
		serviceRegistry = buildServiceRegistry( ejb3Configuration.getHibernateConfiguration() );
		factory = ejb3Configuration.createEntityManagerFactory( getConfig(), serviceRegistry );
		afterEntityManagerFactoryBuilt();
	}

	protected Ejb3Configuration buildConfiguration() {
		Ejb3Configuration ejb3Cfg = constructConfiguration();
		configure( ejb3Cfg.getHibernateConfiguration() );
		addMappings( ejb3Cfg.getHibernateConfiguration() );
		ejb3Cfg.getHibernateConfiguration().buildMappings();
		afterConfigurationBuilt( ejb3Cfg.getHibernateConfiguration() );
		return ejb3Cfg;
	}

	protected Ejb3Configuration constructConfiguration() {
		Ejb3Configuration ejb3Configuration = new Ejb3Configuration();
		if ( createSchema() ) {
			ejb3Configuration.getHibernateConfiguration().setProperty( Environment.HBM2DDL_AUTO, "create-drop" );
		}
		ejb3Configuration
				.getHibernateConfiguration()
				.setProperty( Configuration.USE_NEW_ID_GENERATOR_MAPPINGS, "true" );
		ejb3Configuration
				.getHibernateConfiguration()
				.setProperty( Environment.DIALECT, getDialect().getClass().getName() );
		return ejb3Configuration;
	}

	protected void configure(Configuration hibernateConfiguration) {
	}

	protected Map getConfig() {
		Map<Object, Object> config = loadProperties();
		ArrayList<Class> classes = new ArrayList<Class>();

		classes.addAll( Arrays.asList( getAnnotatedClasses() ) );
		config.put( AvailableSettings.LOADED_CLASSES, classes );
		for ( Map.Entry<Class, String> entry : getCachedClasses().entrySet() ) {
			config.put(
					AvailableSettings.CLASS_CACHE_PREFIX + "." + entry.getKey().getName(),
					entry.getValue()
			);
		}
		for ( Map.Entry<String, String> entry : getCachedCollections().entrySet() ) {
			config.put(
					AvailableSettings.COLLECTION_CACHE_PREFIX + "." + entry.getKey(),
					entry.getValue()
			);
		}
		if ( getEjb3DD().length > 0 ) {
			ArrayList<String> dds = new ArrayList<String>();
			dds.addAll( Arrays.asList( getEjb3DD() ) );
			config.put( AvailableSettings.XML_FILE_NAMES, dds );
		}

		addConfigOptions( config );
		return config;
	}

	protected void addConfigOptions(Map options) {
	}

	protected void addMappings(Configuration configuration) {
		String[] mappings = getMappings();
		if ( mappings != null ) {
			for ( String mapping : mappings ) {
				configuration.addResource(
						getBaseForMappings() + mapping,
						getClass().getClassLoader()
				);
			}
		}
//		Class<?>[] annotatedClasses = getAnnotatedClasses();
//		if ( annotatedClasses != null ) {
//			for ( Class<?> annotatedClass : annotatedClasses ) {
//				configuration.addAnnotatedClass( annotatedClass );
//			}
//		}
//		String[] annotatedPackages = getAnnotatedPackages();
//		if ( annotatedPackages != null ) {
//			for ( String annotatedPackage : annotatedPackages ) {
//				configuration.addPackage( annotatedPackage );
//			}
//		}
//		String[] xmlFiles = getOrmXmlFiles();
//		if ( xmlFiles != null ) {
//			for ( String xmlFile : xmlFiles ) {
//				configuration.addResource( xmlFile );
//			}
//		}
	}

	protected static final String[] NO_MAPPINGS = new String[0];

	protected String[] getMappings() {
		return NO_MAPPINGS;
	}

	protected String getBaseForMappings() {
		return "org/hibernate/test/";
	}

	protected static final Class<?>[] NO_CLASSES = new Class[0];

	protected Class<?>[] getAnnotatedClasses() {
		return NO_CLASSES;
	}

	protected String[] getAnnotatedPackages() {
		return NO_MAPPINGS;
	}

	protected String[] getOrmXmlFiles() {
		return NO_MAPPINGS;
	}

	protected void afterConfigurationBuilt(Configuration hibernateConfiguration) {
	}

	protected ServiceRegistryImpl buildServiceRegistry(Configuration configuration) {
		Properties properties = new Properties();
		properties.putAll( configuration.getProperties() );
		Environment.verifyProperties( properties );
		ConfigurationHelper.resolvePlaceHolders( properties );
		ServiceRegistryImpl serviceRegistry = new ServiceRegistryImpl( properties );
		applyServices( serviceRegistry );
		return serviceRegistry;
	}

	protected void applyServices(ServiceRegistryImpl serviceRegistry) {
	}

	protected void afterEntityManagerFactoryBuilt() {
	}

	protected boolean createSchema() {
		return true;
	}


	@AfterClassOnce
	public void releaseResources() {
		cleanUnclosed( this.em );
		for ( EntityManager isolatedEm : isolatedEms ) {
			cleanUnclosed( isolatedEm );
		}

		if ( factory != null ) {
			factory.close();
		}
		if ( serviceRegistry != null ) {
			serviceRegistry.destroy();
		}
	}

	private void cleanUnclosed(EntityManager em) {
		if ( em == null ) {
			return;
		}
		if ( em.getTransaction().isActive() ) {
			em.getTransaction().rollback();
            LOG.warn("You left an open transaction! Fix your test case. For now, we are closing it for you.");
		}
		if ( em.isOpen() ) {
			// as we open an EM before the test runs, it will still be open if the test uses a custom EM.
			// or, the person may have forgotten to close. So, do not raise a "fail", but log the fact.
			em.close();
            LOG.warn("The EntityManager is not closed. Closing it.");
		}
	}

	protected EntityManager getOrCreateEntityManager() {
		if ( em == null || !em.isOpen() ) {
			em = factory.createEntityManager();
		}
		return em;
	}

	protected EntityManager createIsolatedEntityManager() {
		EntityManager isolatedEm = factory.createEntityManager();
		isolatedEms.add( isolatedEm );
		return isolatedEm;
	}

	protected EntityManager createIsolatedEntityManager(Map props) {
		EntityManager isolatedEm = factory.createEntityManager(props);
		isolatedEms.add( isolatedEm );
		return isolatedEm;
	}

	/**
	 * always reopen a new EM and clse the existing one
	 */
	protected EntityManager createEntityManager(Map properties) {
		if ( em != null && em.isOpen() ) {
			em.close();
		}
		em = factory.createEntityManager( properties );
		return em;
	}

	public String[] getEjb3DD() {
		return new String[] { };
	}

	public Map<Class, String> getCachedClasses() {
		return new HashMap<Class, String>();
	}

	public Map<String, String> getCachedCollections() {
		return new HashMap<String, String>();
	}

	public static Properties loadProperties() {
		Properties props = new Properties();
		InputStream stream = Persistence.class.getResourceAsStream( "/hibernate.properties" );
		if ( stream != null ) {
			try {
				props.load( stream );
			}
			catch ( Exception e ) {
				throw new RuntimeException( "could not load hibernate.properties" );
			}
			finally {
				try {
					stream.close();
				}
				catch ( IOException ioe ) {
				}
			}
		}
		props.setProperty( Environment.HBM2DDL_AUTO, "create-drop" );
		return props;
	}
}
