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
package org.hibernate.ejb.test.ejb3configuration;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.persistence.Persistence;

import org.hibernate.cfg.Environment;
import org.hibernate.ejb.AvailableSettings;
import org.hibernate.ejb.Ejb3Configuration;

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
					AvailableSettings.CLASS_CACHE_PREFIX + "." + entry.getKey().getName(),
					entry.getValue()
			);
		}
		for ( Map.Entry<String, String> entry : getCachedCollections().entrySet() ) {
			configuration.setProperty(
					AvailableSettings.COLLECTION_CACHE_PREFIX + "." + entry.getKey(),
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
