//$Id$
package org.hibernate.ejb.test.ejb3configuration;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.persistence.Persistence;

import org.hibernate.cfg.Environment;
import org.hibernate.ejb.Ejb3Configuration;
import org.hibernate.ejb.HibernatePersistence;

/**
 * @author Emmanuel Bernard
 */
public abstract class TestCase extends junit.framework.TestCase {
	protected Ejb3Configuration configuration;
	private Class lastTestClass;

	public TestCase() {
		super();
	}

	public TestCase(String name) {
		super( name );
	}

	public void setUp() {
		if ( configuration == null || lastTestClass != getClass() ) {
			buildConfiguration();
			lastTestClass = getClass();
		}
		//factory = new HibernatePersistence().createEntityManagerFactory( getConfig() );
	}

	protected boolean recreateSchema() {
		return true;
	}

	;

	private void buildConfiguration() {
		configuration = new Ejb3Configuration();
		configuration.addProperties( loadProperties() );
		if ( recreateSchema() ) {
			configuration.setProperty( Environment.HBM2DDL_AUTO, "create-drop" );
		}

		for ( Class clazz : getAnnotatedClasses() ) {
			configuration.addAnnotatedClass( clazz );
		}

		for ( Map.Entry<Class, String> entry : getCachedClasses().entrySet() ) {
			configuration.setProperty(
					HibernatePersistence.CLASS_CACHE_PREFIX + "." + entry.getKey().getName(),
					entry.getValue()
			);
		}
		for ( Map.Entry<String, String> entry : getCachedCollections().entrySet() ) {
			configuration.setProperty(
					HibernatePersistence.COLLECTION_CACHE_PREFIX + "." + entry.getKey(),
					entry.getValue()
			);
		}
	}

	public abstract Class[] getAnnotatedClasses();

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
		return props;
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		configuration = null; //avoid side effects
	}
}
