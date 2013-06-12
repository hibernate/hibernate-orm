/*
 * JBoss, Home of Professional Open Source
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.test.arquillian.container.felix;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.InputStream;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceProvider;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.osgi.HibernateBundleActivator;
import org.hibernate.osgi.OsgiPersistenceProvider;
import org.hibernate.osgi.OsgiPersistenceProviderService;
import org.hibernate.osgi.test.entity.DataPoint;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
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
 * Test the embedded OSGi framework
 *
 * @author thomas.diesler@jboss.com
 */
@RunWith(Arquillian.class)
public class SimpleBundleTestCase {

    @ArquillianResource
    BundleContext context;

    @Deployment
    public static JavaArchive deployment() {
    	final JavaArchive archive = ShrinkWrap.create( JavaArchive.class, "hibernate-osgi-test.jar" )
    			.addClass( DataPoint.class )
				.addAsResource(new File("src/test/resources/hibernate.cfg.xml"))
				.addAsResource(new File("src/test/resources/META-INF/persistence.xml"), "META-INF/persistence.xml");
				
		archive.setManifest( new Asset() {
			@Override
			public InputStream openStream() {
				OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
				builder.addBundleSymbolicName( archive.getName() );
				builder.addBundleManifestVersion( 2 );
				builder.addExportPackages( DataPoint.class );
                builder.addImportPackages( EntityManager.class );
                builder.addImportPackages( PersistenceProvider.class );
                builder.addImportPackages( HibernateBundleActivator.class );
				return builder.openStream();
			}
		} );

		return archive;
    }
    
    /**
     * Tests that the real PersistenceProvider service, registered by hibernate-osgi, is able to create an EM.
     */
    @Test
    public void testRealEntityManager() {
    	ServiceReference serviceReference = context.getServiceReference( PersistenceProvider.class.getName() );
	    PersistenceProvider persistenceProvider = (PersistenceProvider) context.getService( serviceReference );
	    EntityManagerFactory emf = persistenceProvider.createEntityManagerFactory( "hibernate-osgi-test", null );
	    assertNotNull( emf );
	    EntityManager em = emf.createEntityManager();
	    assertNotNull( em );
    }
 
    /**
     * Tests the full functionality of hibernate-osgi's un-managed JPA.  However, we must "fake" it a bit.  If we
     * attempt to create an EMF through the real PersistenceProvider registered by hibernate-osgi
     * (see {@link #testRealEntityManager()}), the "requestingBundle" provided to the service is the entire Felix
     * framework.  When that "bundle" is added to the OsgiClassLoader and OsgiScanner, it does not work as expected
     * (the framework doesn't have all active bundles on its ClassLoader or listResources).
     * 
     * For that reason, we emulate the calls made to discover the services, since there's not an easy way to better
     * control the "requestingBundle" without shoving these tests into the bundle itself.
     * 
     * Note that the same issue is true with Pax-Exam -- PAXPROBE takes over "requestingBundle" as well.
     * 
     * @param bundle
     * @throws Exception
     */
    @Test
    public void testEntityManager(@ArquillianResource Bundle bundle) throws Exception {
    	assertNotNull("BundleContext injected", context);
        assertEquals("System Bundle ID", 0, context.getBundle().getBundleId());
    	
    	bundle.start();
    	assertEquals("Bundle ACTIVE", Bundle.ACTIVE, bundle.getState());
    	
    	HibernateBundleActivator activator = new HibernateBundleActivator();
    	activator.start( context );
    	OsgiPersistenceProviderService osgiPersistenceProviderService = activator.getOsgiPersistenceProviderService();
    	OsgiPersistenceProvider osgiPersistenceProvider = (OsgiPersistenceProvider) osgiPersistenceProviderService
    			.getService( bundle, null );
    	
    	EntityManagerFactory emf = osgiPersistenceProvider.createEntityManagerFactory( "hibernate-osgi-test", null );
    	assertNotNull( emf );
    	EntityManager em = emf.createEntityManager();
    	assertNotNull( em );
        
        DataPoint dp = new DataPoint();
        dp.setName( "Brett" );
        em.getTransaction().begin();
        em.persist( dp );
        em.getTransaction().commit();
        em.clear();
        
        em.getTransaction().begin();
        List<DataPoint> results = em.createQuery( "from DataPoint" ).getResultList();
        assertEquals(results.size(), 1);
        assertEquals("Brett", results.get(0).getName());
        em.getTransaction().commit();
        em.close();
    }
}
