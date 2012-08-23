/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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
package org.hibernate.jpa.boot.spi;

import java.util.Map;

import org.hibernate.integrator.spi.Integrator;
import org.hibernate.jpa.event.spi.JpaIntegrator;
import org.hibernate.service.BootstrapServiceRegistry;
import org.hibernate.service.BootstrapServiceRegistryBuilder;

import static org.hibernate.cfg.AvailableSettings.APP_CLASSLOADER;
import static org.hibernate.jpa.AvailableSettings.INTEGRATOR_PROVIDER;

/**
 * Helper class for building a BootstrapServiceRegistry for JPA use.  Extracted to separate class so others can
 * leverage.
 */
public class JpaBootstrapServiceRegistryBuilder {
	/**
	 * Builds the {@link org.hibernate.service.BootstrapServiceRegistry} used by used as part of JPA boot-strapping.
	 *
	 * Mainly accounts for class-loading behavior and reading a (potentially) explicitly defined
	 * {@link IntegratorProvider}
	 *
	 * @param integrationSettings Any integration settings passed by the EE container or SE application
	 *
	 * @return The built BootstrapServiceRegistry
	 */
	public static BootstrapServiceRegistry buildBootstrapServiceRegistry(
			PersistenceUnitDescriptor persistenceUnit,
			Map integrationSettings) {
		final BootstrapServiceRegistryBuilder bootstrapServiceRegistryBuilder = new BootstrapServiceRegistryBuilder();
		bootstrapServiceRegistryBuilder.with( new JpaIntegrator() );

		final IntegratorProvider integratorProvider = (IntegratorProvider) integrationSettings.get( INTEGRATOR_PROVIDER );
		if ( integratorProvider != null ) {
			integrationSettings.remove( INTEGRATOR_PROVIDER );
			for ( Integrator integrator : integratorProvider.getIntegrators() ) {
				bootstrapServiceRegistryBuilder.with( integrator );
			}
		}

		ClassLoader classLoader = (ClassLoader) integrationSettings.get( APP_CLASSLOADER );
		if ( classLoader != null ) {
			integrationSettings.remove( APP_CLASSLOADER );
		}
		else {
			classLoader = persistenceUnit.getClassLoader();
		}
		bootstrapServiceRegistryBuilder.withApplicationClassLoader( classLoader );

		return bootstrapServiceRegistryBuilder.build();
	}
}