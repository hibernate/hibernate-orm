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
package org.hibernate.osgi.internal;

import java.util.Properties;

import javax.persistence.spi.PersistenceProvider;

import org.hibernate.jpa.HibernatePersistenceProvider;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;

/**
 * @author Brett Meyer
 * @author Martin Neimeier
 */
public class HibernateBundleActivator implements
        BundleActivator,
//        ServiceListener,
        BundleListener {
	
    private OsgiClassLoaderService osgiClassLoaderService;

    @Override
    public void start(BundleContext context) throws Exception {
    	osgiClassLoaderService = new OsgiClassLoaderServiceImpl();
    	
    	Properties properties = new Properties();
        properties.put( "javax.persistence.provider", HibernatePersistenceProvider.class.getName() );
        context.registerService(
                PersistenceProvider.class.getName(),
                new HibernatePersistenceProvider( osgiClassLoaderService ), 
                properties
        );

        // register this instance as a service listener - we are only interested
        // on services which implement the interface
        // org.hibernate.service.Service
    	// TODO: Is this needed?  Do we care more about scanning the bundles instead?
//        context.addServiceListener(this, "(" + Constants.OBJECTCLASS
//                + "=" + org.hibernate.service.Service.class.getName() + ")");

        // register this instance as a bundle listener to get informed about all
        // bundle live cycle events
        context.addBundleListener(this);

        for ( Bundle bundle : context.getBundles() ) {
        	scanBundle( bundle );
        }
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        context.removeBundleListener(this);
//        context.removeServiceListener(this);

        // TODO: I'm assuming it'd be disastrous to blow away the 
        // osgiClassLoaderService.  Instead, unregister the bundles in it?
    }

    @Override
    public void bundleChanged(BundleEvent event) {
    	scanBundle( event.getBundle() );

    }

//    @Override
//    public void serviceChanged(ServiceEvent event) {
//        // Analyze the service-event and react
//        ServiceReference reference = event.getServiceReference();
//        if ((event.getType() & ServiceEvent.REGISTERED) != 0) {
//            // new interesting service .. register bundle implementing the
//            // service
//            osgiClassLoaderService.registerBundle(reference.getBundle());
//        } else if ((event.getType() & ServiceEvent.UNREGISTERING) != 0) {
//            // interesting service is nearly gone .. unregister the bundle
//            // implementing the service
//            osgiClassLoaderService.unregisterBundle(reference.getBundle());
//        }
//    }
    
    private void scanBundle( Bundle bundle ) {
    	if ( bundle.getState() == Bundle.ACTIVE ) {
    		// TODO: scan?
    		// TODO: register only if the bundle has a persistence context?
    		osgiClassLoaderService.registerBundle(bundle);
    	} else {
    		osgiClassLoaderService.unregisterBundle(bundle);
    		// TODO: since the env. is dynamic, will we need to "cleanup"
    		// any bundles that go down?
    	}
    }

}