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
package org.hibernate.osgi;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.persistence.spi.PersistenceProvider;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.internal.util.ClassLoaderHelper;
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
public class HibernateBundleActivator
		implements BundleActivator, /*ServiceListener,*/ BundleListener {
	
	private OsgiClassLoader osgiClassLoader;

    @Override
    public void start(BundleContext context) throws Exception {
    	
    	// register this instance as a bundle listener to get informed about all
        // bundle live cycle events
        context.addBundleListener(this);
        
    	osgiClassLoader = new OsgiClassLoader();
    	
    	ClassLoaderHelper.overridenClassLoader = osgiClassLoader;

        for ( Bundle bundle : context.getBundles() ) {
        	handleBundleChange( bundle );
        }
        
        HibernatePersistenceProvider hpp = new HibernatePersistenceProvider();
        Map map = new HashMap();
        map.put( AvailableSettings.JTA_PLATFORM, new OsgiJtaPlatform( context ) );
        hpp.setEnvironmentProperties( map );
    	
        Properties properties = new Properties();
        properties.put( "javax.persistence.provider", HibernatePersistenceProvider.class.getName() );
        context.registerService( PersistenceProvider.class.getName(), hpp, properties );
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        context.removeBundleListener(this);
        
        // Nothing else to do.  When re-activated, this Activator will be
        // re-started and the EMF re-created.
    }

    @Override
    public void bundleChanged(BundleEvent event) {
    	handleBundleChange( event.getBundle() );

    }
    
    private void handleBundleChange( Bundle bundle ) {
    	if ( bundle.getState() == Bundle.ACTIVE ) {
    		osgiClassLoader.registerBundle(bundle);
    	} else {
    		osgiClassLoader.unregisterBundle(bundle);
    	}
    }
}
