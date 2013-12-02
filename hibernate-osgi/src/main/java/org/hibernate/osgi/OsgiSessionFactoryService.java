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
package org.hibernate.osgi;

import java.util.Collection;

import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.registry.selector.StrategyRegistrationProvider;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.metamodel.spi.TypeContributor;
import org.hibernate.service.ServiceRegistry;
import org.jboss.logging.Logger;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.wiring.BundleWiring;

/**
 * Hibernate 4.2 and 4.3 still heavily rely on TCCL for ClassLoading.  Although
 * our ClassLoaderService removed some of the reliance, the service is
 * unfortunately not available during Configuration.  An OSGi
 * bundle manually creating a SessionFactory would require numerous ClassLoader
 * tricks (or may be impossible altogether).
 * <p/>
 * In order to fully control the TCCL issues and shield users from the
 * knowledge, we're requiring that bundles use this OSGi ServiceFactory.  It
 * configures and provides a SessionFactory as an OSGi service.
 * <p/>
 * Note that an OSGi ServiceFactory differs from a Service.  The ServiceFactory
 * allows individual instances of Services to be created and provided to
 * multiple client Bundles.
 *
 * @author Brett Meyer
 * @author Tim Ward
 */
public class OsgiSessionFactoryService implements ServiceFactory {
	private static final CoreMessageLogger LOG = Logger.getMessageLogger(CoreMessageLogger.class,
			OsgiSessionFactoryService.class.getName());
	
	private OsgiClassLoader osgiClassLoader;
	private OsgiJtaPlatform osgiJtaPlatform;
	private OsgiServiceUtil osgiServiceUtil;

	/**
	 * Constructs a OsgiSessionFactoryService
	 *
	 * @param osgiClassLoader The OSGi-specific ClassLoader created in HibernateBundleActivator
	 * @param osgiJtaPlatform The OSGi-specific JtaPlatform created in HibernateBundleActivator
	 * @param context The OSGi context
	 */
	public OsgiSessionFactoryService(
			OsgiClassLoader osgiClassLoader,
			OsgiJtaPlatform osgiJtaPlatform,
			OsgiServiceUtil osgiServiceUtil) {
		this.osgiClassLoader = osgiClassLoader;
		this.osgiJtaPlatform = osgiJtaPlatform;
		this.osgiServiceUtil = osgiServiceUtil;
	}

	@Override
	public Object getService(Bundle requestingBundle, ServiceRegistration registration) {
		osgiClassLoader.addBundle( requestingBundle );

		final Configuration configuration = new Configuration();
		configuration.getProperties().put( AvailableSettings.JTA_PLATFORM, osgiJtaPlatform );
		
		// Allow bundles to put the config file somewhere other than the root level.
		final BundleWiring bundleWiring = (BundleWiring) requestingBundle.adapt( BundleWiring.class );
		final Collection<String> cfgResources = bundleWiring.listResources( "/", "hibernate.cfg.xml",
				BundleWiring.LISTRESOURCES_RECURSE );
		if (cfgResources.size() == 0) {
			configuration.configure();
		}
		else {
			if (cfgResources.size() > 1) {
				LOG.warn( "Multiple hibernate.cfg.xml files found in the persistence bundle.  Using the first one discovered." );
			}
			String cfgResource = "/" + cfgResources.iterator().next();
			configuration.configure( cfgResource );
		}

		final BootstrapServiceRegistryBuilder builder = new BootstrapServiceRegistryBuilder();
		builder.with( osgiClassLoader );

		final Integrator[] integrators = osgiServiceUtil.getServiceImpls( Integrator.class );
		for ( Integrator integrator : integrators ) {
			builder.with( integrator );
		}

		final StrategyRegistrationProvider[] strategyRegistrationProviders
				= osgiServiceUtil.getServiceImpls( StrategyRegistrationProvider.class );
		for ( StrategyRegistrationProvider strategyRegistrationProvider : strategyRegistrationProviders ) {
			builder.withStrategySelectors( strategyRegistrationProvider );
		}
        
		final TypeContributor[] typeContributors = osgiServiceUtil.getServiceImpls( TypeContributor.class );
		for ( TypeContributor typeContributor : typeContributors ) {
			configuration.registerTypeContributor( typeContributor );
		}

		final ServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder( builder.build() )
				.applySettings( configuration.getProperties() ).build();
		return configuration.buildSessionFactory( serviceRegistry );
	}

	@Override
	public void ungetService(Bundle requestingBundle, ServiceRegistration registration, Object service) {
		((SessionFactory) service).close();
	}

}
