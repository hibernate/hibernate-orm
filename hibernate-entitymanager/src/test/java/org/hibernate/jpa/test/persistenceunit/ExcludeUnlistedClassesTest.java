/* 
 * Hibernate, Relational Persistence for Idiomatic Java
 * 
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.jpa.test.persistenceunit;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManagerFactory;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.internal.util.ConfigHelper;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

/**
 * @author Brett Meyer
 */
@TestForIssue(jiraKey = "HHH-8364")
public class ExcludeUnlistedClassesTest extends BaseUnitTestCase {
	
	@Test
	public void testExcludeUnlistedClasses() {
		// see src/test/resources/org/hibernate/jpa/test/persistenceunit/persistence.xml
		doTest( "ExcludeUnlistedClassesTest1", true );
		doTest( "ExcludeUnlistedClassesTest2", false );
		doTest( "ExcludeUnlistedClassesTest3", true );
		doTest( "ExcludeUnlistedClassesTest4", false );
	}
	
	private void doTest(String persistenceUnitName, boolean shouldScan) {
		final Map<String, Object> properties = new HashMap<String, Object>();
		properties.put( AvailableSettings.APP_CLASSLOADER, new TestClassLoader() );
		final HibernatePersistenceProvider provider = new HibernatePersistenceProvider();
		final EntityManagerFactory emf = provider.createEntityManagerFactory( persistenceUnitName, properties );
		assertNotNull( emf.getMetamodel().entity( DataPoint.class ) );
		if (shouldScan) {
			assertNull( emf.getMetamodel().entity( UnlistedDataPoint.class ) );
		}
		else {
			assertNotNull( emf.getMetamodel().entity( UnlistedDataPoint.class ) );
		}
	}

    private static class TestClassLoader extends ClassLoader {
    	
    	/**
    	 * testStoppableClassLoaderService() needs a custom JDK service implementation.  Rather than using a real one
    	 * on the test classpath, force it in here.
    	 */
    	@Override
        protected Enumeration<URL> findResources(String name) throws IOException {
    		if (name.equals( "META-INF/persistence.xml" )) {
    			final URL puUrl = ConfigHelper.findAsResource(
    					"org/hibernate/jpa/test/persistenceunit/META-INF/persistence.xml" );
    			return new Enumeration<URL>() {
        			boolean hasMore = true;
        			
    				@Override
    				public boolean hasMoreElements() {
    					return hasMore;
    				}

    				@Override
    				public URL nextElement() {
    					hasMore = false;
    					return puUrl;
    				}
    			};
    		}
    		else {
    			return java.util.Collections.emptyEnumeration();
    		}
        }
    }
}
