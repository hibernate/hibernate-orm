// $Id$
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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.hibernate.cfg.AnnotationConfiguration;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.Dialect;
import org.hibernate.ejb.AvailableSettings;
import org.hibernate.ejb.Ejb3Configuration;
import org.hibernate.testing.junit.functional.annotations.HibernateTestCase;

/**
 * A base class for all ejb tests.
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
public abstract class TestCase extends HibernateTestCase {
	private static final Logger log = LoggerFactory.getLogger( TestCase.class );

	protected static EntityManagerFactory factory;
	private EntityManager em;
	private ArrayList isolatedEms = new ArrayList();


	public TestCase() {
		super();
	}

	public TestCase(String name) {
		super( name );
	}


	public void tearDown() throws Exception {
		super.tearDown();
	}

	@Override
	protected void buildConfiguration() throws Exception {
		Ejb3Configuration ejbconfig = new Ejb3Configuration();
		TestCase.cfg = ejbconfig.getHibernateConfiguration();
		if ( recreateSchema() ) {
			cfg.setProperty( Environment.HBM2DDL_AUTO, "create-drop" );
		}
		cfg.setProperty( AnnotationConfiguration.USE_NEW_ID_GENERATOR_MAPPINGS, "true" );

		for ( String mappingFile : getMappings() ) {
			cfg.addResource( mappingFile );
		}

		factory = ejbconfig.createEntityManagerFactory( getConfig() );
	}

	private void cleanUnclosed(EntityManager em) {
		if ( em == null ) {
			return;
		}
		if ( em.getTransaction().isActive() ) {
			em.getTransaction().rollback();
			log.warn( "You left an open transaction! Fix your test case. For now, we are closing it for you." );
		}
		if ( em.isOpen() ) {
			// as we open an EM before the test runs, it will still be open if the test uses a custom EM.
			// or, the person may have forgotten to close. So, do not raise a "fail", but log the fact.
			em.close();
			log.warn( "The EntityManager is not closed. Closing it." );
		}
	}

	protected void handleUnclosedResources() {
		cleanUnclosed( this.em );
		for ( Iterator iter = isolatedEms.iterator(); iter.hasNext(); ) {
			cleanUnclosed( ( EntityManager ) iter.next() );
		}

		cfg = null;
	}

	protected void closeResources() {
		if ( factory != null ) {
			factory.close();
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

	public Map getConfig() {
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

	@Override
	public void runBare() throws Throwable {
		if ( !appliesTo( Dialect.getDialect() ) ) {
			return;
		}
		super.runBare();
	}

	public boolean appliesTo(Dialect dialect) {
		return true;
	}
}
