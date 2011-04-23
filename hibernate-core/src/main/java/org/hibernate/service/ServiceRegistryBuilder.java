/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.cfg.Environment;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.service.internal.BasicServiceRegistryImpl;
import org.hibernate.service.internal.ProvidedService;
import org.hibernate.service.spi.BasicServiceInitiator;

/**
 * Builder for basic service registry instances.
 *
 * @author Steve Ebersole
 */
public class ServiceRegistryBuilder {
	public static final String DEFAULT_CFG_RESOURCE_NAME = "hibernate.cfg.xml";

	private final Map settings;
	private final List<BasicServiceInitiator> initiators = standardInitiatorList();
	private final List<ProvidedService> providedServices = new ArrayList<ProvidedService>();

	/**
	 * Create a default builder
	 */
	public ServiceRegistryBuilder() {
		this( Environment.getProperties() );
	}

	/**
	 * Create a builder with the specified settings
	 *
	 * @param settings The initial set of settings to use.
	 */
	public ServiceRegistryBuilder(Map settings) {
		this.settings = settings;
	}

	private static List<BasicServiceInitiator> standardInitiatorList() {
		final List<BasicServiceInitiator> initiators = new ArrayList<BasicServiceInitiator>();
		initiators.addAll( StandardServiceInitiators.LIST );
		return initiators;
	}

	/**
	 * Read setting information from the standard resource location
	 *
	 * @return this, for method chaining
	 *
	 * @see #DEFAULT_CFG_RESOURCE_NAME
	 */
	public ServiceRegistryBuilder configure() {
		return configure( DEFAULT_CFG_RESOURCE_NAME );
	}

	/**
	 * Read setting information from the named resource location
	 *
	 * @param resourceName The named resource
	 *
	 * @return this, for method chaining
	 */
	public ServiceRegistryBuilder configure(String resourceName) {
		// todo : parse and apply XML
		// we run into a chicken-egg problem here, in that we need the service registry in order to know how to do this
		// resource lookup (ClassLoaderService)
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
	public ServiceRegistryBuilder applySetting(String settingName, Object value) {
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
	public ServiceRegistryBuilder applySettings(Map settings) {
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
	public ServiceRegistryBuilder addInitiator(BasicServiceInitiator initiator) {
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
	public ServiceRegistryBuilder addService(final Class serviceRole, final Service service) {
		providedServices.add( new ProvidedService( serviceRole, service ) );
		return this;
	}

	/**
	 * Build the service registry accounting for all settings and service initiators and services.
	 *
	 * @return The built service registry
	 */
	public BasicServiceRegistry buildServiceRegistry() {
		Map<?,?> settingsCopy = new HashMap();
		settingsCopy.putAll( settings );
		Environment.verifyProperties( settingsCopy );
		ConfigurationHelper.resolvePlaceHolders( settingsCopy );
		return new BasicServiceRegistryImpl( initiators, providedServices, settingsCopy );
	}

	/**
	 * Destroy a service registry.  Applications should only destroy registries they have explicitly created.
	 *
	 * @param serviceRegistry The registry to be closed.
	 */
	public static void destroy(BasicServiceRegistry serviceRegistry) {
		( (BasicServiceRegistryImpl) serviceRegistry ).destroy();
	}
}
