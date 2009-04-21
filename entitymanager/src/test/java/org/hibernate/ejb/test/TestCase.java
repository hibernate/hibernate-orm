//$Id$
package org.hibernate.ejb.test;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.Dialect;
import org.hibernate.ejb.HibernatePersistence;


/**
 * @author Emmanuel Bernard
 */
public abstract class TestCase extends junit.framework.TestCase {
	protected EntityManagerFactory factory;
	protected EntityManager em;
	private static Log log = LogFactory.getLog( TestCase.class );

	public TestCase() {
		super();
	}

	public TestCase(String name) {
		super( name );
	}

	public void setUp() {
		factory = new HibernatePersistence().createEntityManagerFactory( getConfig() );
	}

	public void tearDown() {
		factory.close();
	}
	
	@Override
	public void runTest() throws Throwable {
		try {
			em = getOrCreateEntityManager();
			super.runTest();
			if (em.getTransaction().isActive()) {
				em.getTransaction().rollback();
				fail("You left an open transaction! Fix your test case. For now, we are closing it for you.");
			}

		} catch (Throwable t) {
			if (em.getTransaction().isActive())  
				em.getTransaction().rollback();
			throw t;
		} finally {
			if (em.isOpen()) {
				// as we open an EM before the test runs, it will still be open if the test uses a custom EM.
				// or, the person may have forgotten to close. So, do not raise a "fail", but log the fact.
				em.close(); 
				log.warn("The EntityManager is not closed. Closing it.");
			}
		}
	}
	
	protected EntityManager getOrCreateEntityManager() {
		if (em == null || !em.isOpen()) 
			em = factory.createEntityManager();
		return em;
	}

	/** always reopen a new EM and clse the existing one */
	protected EntityManager createEntityManager(Map properties) {
		if (em != null && em.isOpen() ) {
			em.close();
		}
		em = factory.createEntityManager(properties);
		return em;
	}

	public abstract Class[] getAnnotatedClasses();

	public String[] getEjb3DD() {
		return new String[] {};
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
			catch (Exception e) {
				throw new RuntimeException( "could not load hibernate.properties" );
			}
			finally {
				try {
					stream.close();
				}
				catch (IOException ioe) {
				}
			}
		}
		props.setProperty( Environment.HBM2DDL_AUTO, "create-drop" );
		return props;
	}

	public Map getConfig() {
		Map config = loadProperties();
		ArrayList<Class> classes = new ArrayList<Class>();

		for ( Class clazz : getAnnotatedClasses() ) {
			classes.add( clazz );
		}
		config.put( HibernatePersistence.LOADED_CLASSES, classes );
		for ( Map.Entry<Class, String> entry : getCachedClasses().entrySet() ) {
			config.put(
					HibernatePersistence.CLASS_CACHE_PREFIX + "." + entry.getKey().getName(),
					entry.getValue()
			);
		}
		for ( Map.Entry<String, String> entry : getCachedCollections().entrySet() ) {
			config.put(
					HibernatePersistence.COLLECTION_CACHE_PREFIX + "." + entry.getKey(),
					entry.getValue()
			);
		}
		if ( getEjb3DD().length > 0 ) {
			ArrayList<String> dds = new ArrayList<String>();
			for ( String dd : getEjb3DD() ) {
				dds.add( dd );
			}
			config.put( HibernatePersistence.XML_FILE_NAMES, dds );
		}
		return config;
	}

	@Override
	public void runBare() throws Throwable {
		
		if (!appliesTo(Dialect.getDialect())) 
			return;
		
		Throwable exception = null;
		setUp();
		try {
			runTest();
		} catch (Throwable running) {
			exception = running;
		} finally {
			try {
				tearDown();
			} catch (Throwable tearingDown) {
				if (exception == null)
					exception = tearingDown;
			}
		}
		if (exception != null)
			throw exception;
	}

	public boolean appliesTo(Dialect dialect) {
		return true;
	}

}
