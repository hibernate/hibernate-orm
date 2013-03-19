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

import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceUnitInfo;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.ejb.Ejb3Configuration;
import org.hibernate.ejb.HibernatePersistence;
import org.hibernate.service.BootstrapServiceRegistryBuilder;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * @author Brett Meyer
 * @author Martin Neimeier
 * @author Tim Ward
 */
public class HibernateBundleActivator implements BundleActivator {

	private HibernateOsgiClassLoader osgiClassLoader = new HibernateOsgiClassLoader();

	/**
	 * <p>
	 * We have to do a little hacking here to fix Ejb3Configuration.
	 * </p>
	 * <p>
	 * For some reason the ClassLoaders set in the properties passed to hibernate are ignored after the "configure"
	 * phase. Realistically the HibernatePersistence/Ejb3Configuration code should be honouring these properties all the
	 * way through, particularly when creating the ClassLoaderService.
	 * </p>
	 * 
	 * <p>
	 * It may be that the decision is to merge this code into the HibernatePersistenceWrapper rather than fix the (imo)
	 * broken Ejb3Configuration. To that end I'm doing the work inline here. This should make it easy to move later.
	 * </p>
	 * 
	 * <p>
	 * The code in the various create... methods is simply copied from the real HibernatePersistence.
	 * </p>
	 */
	@Override
	public void start(BundleContext context) throws Exception {

		final HibernatePersistence hp = new HibernatePersistence() {

			@Override
			public EntityManagerFactory createContainerEntityManagerFactory(PersistenceUnitInfo info, Map properties) {
				Ejb3Configuration cfg = getClassLoaderFixedEjb3Configuration( properties );
				Ejb3Configuration configured = cfg.configure( info, properties );
				return configured != null ? configured.buildEntityManagerFactory() : null;
			}

			@Override
			@Deprecated
			public EntityManagerFactory createEntityManagerFactory(Map properties) {
				Ejb3Configuration cfg = getClassLoaderFixedEjb3Configuration( properties );
				return cfg.createEntityManagerFactory( properties );
			}

			@Override
			public EntityManagerFactory createEntityManagerFactory(String persistenceUnitName, Map properties) {
				Ejb3Configuration cfg = getClassLoaderFixedEjb3Configuration( properties );
				Ejb3Configuration configured = cfg.configure( persistenceUnitName, properties );
				return configured != null ? configured.buildEntityManagerFactory() : null;
			}

			/**
			 * Set the relevant classloaders in the BootStrapServiceRegistryBuilder
			 * 
			 * @param properties
			 * @return
			 */
			private Ejb3Configuration getClassLoaderFixedEjb3Configuration(Map properties) {

				final ClassLoader hibernateCL = (ClassLoader) properties.get( "hibernate.classLoader.hibernate" );
				final ClassLoader appCL = (ClassLoader) properties.get( "hibernate.classLoader.application" );
				final ClassLoader resCl = (ClassLoader) properties.get( "hibernate.classLoader.resources" );
				final ClassLoader envCL = (ClassLoader) properties.get( "hibernate.classLoader.environment" );

				return new Ejb3Configuration() {

					@Override
					public EntityManagerFactory buildEntityManagerFactory(BootstrapServiceRegistryBuilder builder) {
						return super.buildEntityManagerFactory( builder.with( hibernateCL ).with( appCL ).with( resCl )
								.with( envCL ) );
					}

				};
			}
		};

		// End Ejb3Configuration fix

		Map map = new HashMap();
		map.put( AvailableSettings.JTA_PLATFORM, new OsgiJtaPlatform( context ) );
		hp.setEnvironmentProperties( map );

		Properties properties = new Properties();
		properties.put( "javax.persistence.provider", HibernatePersistence.class.getName() );
		context.registerService( PersistenceProvider.class.getName(), new HibernatePersistenceWrapper( hp,
				osgiClassLoader ), properties );
	}

	@Override
	public void stop(BundleContext context) throws Exception {

		// Nothing else to do. When re-activated, this Activator will be
		// re-started and the PersistenceProvider re=registered
	}
}
