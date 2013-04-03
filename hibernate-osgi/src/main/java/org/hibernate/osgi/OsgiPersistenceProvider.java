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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceUnitInfo;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.ejb.Ejb3Configuration;
import org.hibernate.ejb.HibernatePersistence;
import org.hibernate.service.BootstrapServiceRegistryBuilder;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleReference;

/**
 * @author Brett Meyer
 * @author Tim Ward
 */
public class OsgiPersistenceProvider extends HibernatePersistence {

	private OsgiClassLoader osgiClassLoader;

	private OsgiJtaPlatform osgiJtaPlatform;
	
	private Bundle requestingBundle;
	
	public OsgiPersistenceProvider (OsgiClassLoader osgiClassLoader,
			OsgiJtaPlatform osgiJtaPlatform,
			Bundle requestingBundle ) {
		this.osgiClassLoader = osgiClassLoader;
		this.osgiJtaPlatform = osgiJtaPlatform;
		this.requestingBundle = requestingBundle;
	}

	@Override
	public EntityManagerFactory createEntityManagerFactory(String persistenceUnitName, Map properties) {
		if ( properties == null ) {
			properties = new HashMap();
		}
		properties.put( AvailableSettings.JTA_PLATFORM, osgiJtaPlatform );
		// TODO: This needs tested.
		properties.put( org.hibernate.ejb.AvailableSettings.SCANNER,
				new OsgiScanner( requestingBundle ) );
		
		osgiClassLoader.addBundle( requestingBundle );

		Ejb3Configuration cfg = new Ejb3Configuration();
		Ejb3Configuration configured = cfg.configure( persistenceUnitName, properties );
		return configured != null ? configured.buildEntityManagerFactory( getBuilder( properties ) ) : null;
	}

	@Override
	public EntityManagerFactory createContainerEntityManagerFactory(PersistenceUnitInfo info, Map properties) {
		if ( properties == null ) {
			properties = new HashMap();
		}
		properties.put( AvailableSettings.JTA_PLATFORM, osgiJtaPlatform );
		// OSGi ClassLoaders must implement BundleReference 
		properties.put( org.hibernate.ejb.AvailableSettings.SCANNER,
				new OsgiScanner( ( (BundleReference) info.getClassLoader() ).getBundle() ) );

		osgiClassLoader.addClassLoader( info.getClassLoader() );

		Ejb3Configuration cfg = new Ejb3Configuration();
		Ejb3Configuration configured = cfg.configure( info, properties );
		return configured != null ? configured.buildEntityManagerFactory( getBuilder( properties ) ) : null;
	}

	private BootstrapServiceRegistryBuilder getBuilder(Map properties) {
		BootstrapServiceRegistryBuilder builder = new BootstrapServiceRegistryBuilder();
		final Collection<ClassLoader> classLoaders = (Collection<ClassLoader>) properties
				.get( AvailableSettings.CLASSLOADERS );
		if ( classLoaders != null ) {
			for ( ClassLoader classLoader : classLoaders ) {
				osgiClassLoader.addClassLoader( classLoader );
			}
		}
		builder.with( osgiClassLoader );
		return builder;
	}
}
