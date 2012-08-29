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
package org.hibernate.boot.registry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.hibernate.cfg.Environment;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.integrator.spi.IntegratorService;
import org.hibernate.integrator.spi.ServiceContributingIntegrator;
import org.hibernate.internal.jaxb.cfg.JaxbHibernateConfiguration;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.boot.registry.internal.BootstrapServiceRegistryImpl;
import org.hibernate.service.ConfigLoader;
import org.hibernate.service.Service;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.StandardServiceInitiators;
import org.hibernate.service.internal.ProvidedService;
import org.hibernate.boot.registry.internal.StandardServiceRegistryImpl;

/**
 * Builder for standard {@link org.hibernate.service.ServiceRegistry} instances.
 *
 * @author Steve Ebersole
 * 
 * @see StandardServiceRegistryImpl
 * @see org.hibernate.boot.registry.BootstrapServiceRegistryBuilder
 */
public class StandardServiceRegistryBuilder {
	public static final String DEFAULT_CFG_RESOURCE_NAME = "hibernate.cfg.xml";

	private final Map settings;
	private final List<StandardServiceInitiator> initiators = standardInitiatorList();
	private final List<ProvidedService> providedServices = new ArrayList<ProvidedService>();

	private final BootstrapServiceRegistry bootstrapServiceRegistry;
	private final ConfigLoader configLoader;

	/**
	 * Create a default builder
	 */
	public StandardServiceRegistryBuilder() {
		this( new BootstrapServiceRegistryImpl() );
	}

	/**
	 * Create a builder with the specified bootstrap services.
	 *
	 * @param bootstrapServiceRegistry Provided bootstrap registry to use.
	 */
	public StandardServiceRegistryBuilder(BootstrapServiceRegistry bootstrapServiceRegistry) {
		this.settings = Environment.getProperties();
		this.bootstrapServiceRegistry = bootstrapServiceRegistry;
		this.configLoader = new ConfigLoader( bootstrapServiceRegistry );
	}

	/**
	 * Used from the {@link #initiators} variable initializer
	 *
	 * @return List of standard initiators
	 */
	private static List<StandardServiceInitiator> standardInitiatorList() {
		final List<StandardServiceInitiator> initiators = new ArrayList<StandardServiceInitiator>();
		initiators.addAll( StandardServiceInitiators.LIST );
		return initiators;
	}

	/**
	 * Read settings from a {@link Properties} file.  Differs from {@link #configure()} and {@link #configure(String)}
	 * in that here we read a {@link Properties} file while for {@link #configure} we read the XML variant.
	 *
	 * @param resourceName The name by which to perform a resource look up for the properties file.
	 *
	 * @return this, for method chaining
	 *
	 * @see #configure()
	 * @see #configure(String)
	 */
	@SuppressWarnings( {"unchecked"})
	public StandardServiceRegistryBuilder loadProperties(String resourceName) {
		settings.putAll( configLoader.loadProperties( resourceName ) );
		return this;
	}

	/**
	 * Read setting information from an XML file using the standard resource location
	 *
	 * @return this, for method chaining
	 *
	 * @see #DEFAULT_CFG_RESOURCE_NAME
	 * @see #configure(String)
	 * @see #loadProperties(String)
	 */
	public StandardServiceRegistryBuilder configure() {
		return configure( DEFAULT_CFG_RESOURCE_NAME );
	}

	/**
	 * Read setting information from an XML file using the named resource location
	 *
	 * @param resourceName The named resource
	 *
	 * @return this, for method chaining
	 *
	 * @see #loadProperties(String)
	 */
	@SuppressWarnings( {"unchecked"})
	public StandardServiceRegistryBuilder configure(String resourceName) {
		JaxbHibernateConfiguration configurationElement = configLoader.loadConfigXmlResource( resourceName );
		for ( JaxbHibernateConfiguration.JaxbSessionFactory.JaxbProperty xmlProperty : configurationElement.getSessionFactory().getProperty() ) {
			settings.put( xmlProperty.getName(), xmlProperty.getValue() );
		}

		return this;
	}

	/**
	 * Apply a setting value
	 *
	 * @param settingName The name of the setting
	 * @param value The value to use.
	 *
	 * @return this, for method chaining
	 */
	@SuppressWarnings( {"unchecked", "UnusedDeclaration"})
	public StandardServiceRegistryBuilder applySetting(String settingName, Object value) {
		settings.put( settingName, value );
		return this;
	}

	/**
	 * Apply a groups of setting values
	 *
	 * @param settings The incoming settings to apply
	 *
	 * @return this, for method chaining
	 */
	@SuppressWarnings( {"unchecked", "UnusedDeclaration"})
	public StandardServiceRegistryBuilder applySettings(Map settings) {
		this.settings.putAll( settings );
		return this;
	}

	/**
	 * Adds a service initiator.
	 *
	 * @param initiator The initiator to be added
	 *
	 * @return this, for method chaining
	 */
	@SuppressWarnings( {"UnusedDeclaration"})
	public StandardServiceRegistryBuilder addInitiator(StandardServiceInitiator initiator) {
		initiators.add( initiator );
		return this;
	}

	/**
	 * Adds a user-provided service
	 *
	 * @param serviceRole The role of the service being added
	 * @param service The service implementation
	 *
	 * @return this, for method chaining
	 */
	@SuppressWarnings( {"unchecked"})
	public StandardServiceRegistryBuilder addService(final Class serviceRole, final Service service) {
		providedServices.add( new ProvidedService( serviceRole, service ) );
		return this;
	}

	/**
	 * Build the service registry accounting for all settings and service initiators and services.
	 *
	 * @return The built service registry
	 */
	public ServiceRegistry buildServiceRegistry() {
		Map<?,?> settingsCopy = new HashMap();
		settingsCopy.putAll( settings );
		Environment.verifyProperties( settingsCopy );
		ConfigurationHelper.resolvePlaceHolders( settingsCopy );

		for ( Integrator integrator : bootstrapServiceRegistry.getService( IntegratorService.class ).getIntegrators() ) {
			if ( ServiceContributingIntegrator.class.isInstance( integrator ) ) {
				ServiceContributingIntegrator.class.cast( integrator ).prepareServices( this );
			}
		}

		return new StandardServiceRegistryImpl( bootstrapServiceRegistry, initiators, providedServices, settingsCopy );
	}

	/**
	 * Destroy a service registry.  Applications should only destroy registries they have explicitly created.
	 *
	 * @param serviceRegistry The registry to be closed.
	 */
	public static void destroy(ServiceRegistry serviceRegistry) {
		( (StandardServiceRegistryImpl) serviceRegistry ).destroy();
	}
}
