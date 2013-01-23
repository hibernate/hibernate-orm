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

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

/**
 * @author Martin Neimeier
 */
public class HibernateBundleActivator implements
        BundleActivator,
        ServiceListener,
        BundleListener {
    private OsgiClassLoaderService osgiClassLoaderService;

    public HibernateBundleActivator() {
        // create the class loader service
        osgiClassLoaderService = new OsgiClassLoaderServiceImpl();

    }

    @Override
    public void start(BundleContext context) throws Exception {
        // TODO: register this ClassLoaderService with Hibernate

        // register this instance as a service listener - we are only interested
        // on services which implement the interface
        // org.hibernate.service.Service
        context.addServiceListener(this, "(" + Constants.OBJECTCLASS
                + "="
                + org.hibernate.service.Service.class.getName()
                + ")");

        // register this instance as a bundle listener to get informed about all
        // bundle live cycle events
        context.addBundleListener(this);

        // TODO: do a initial scan for interesting services and bundles and
        // register the bundles with the class loader
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        context.removeBundleListener(this);
        context.removeServiceListener(this);

        // and finally
        // TODO: unregister this ClassLoaderService from Hibernate
    }

    @Override
    public void bundleChanged(BundleEvent event) {
        // Analyze the bundle-event and react

        // TODO: scan bundle for interesting resources - could be used to add
        // all bundles which contain persistence units to the class loader

    }

    @Override
    public void serviceChanged(ServiceEvent event) {
        // Analyze the service-event and react
        ServiceReference reference = event.getServiceReference();
        if ((event.getType() & ServiceEvent.REGISTERED) != 0) {
            // new interesting service .. register bundle implementing the
            // service
            osgiClassLoaderService.registerBundle(reference.getBundle());
        } else if ((event.getType() & ServiceEvent.UNREGISTERING) != 0) {
            // interesting service is nearly gone .. unregister the bundle
            // implementing the service
            osgiClassLoaderService.unregisterBundle(reference.getBundle());
        }
    }

}