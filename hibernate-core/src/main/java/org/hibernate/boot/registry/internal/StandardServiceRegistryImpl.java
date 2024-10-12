/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.registry.internal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.service.Service;
import org.hibernate.service.internal.AbstractServiceRegistryImpl;
import org.hibernate.service.internal.ProvidedService;
import org.hibernate.service.spi.Configurable;
import org.hibernate.service.spi.ServiceBinding;
import org.hibernate.service.spi.ServiceInitiator;

/**
 * Standard Hibernate implementation of the standard service registry.
 *
 * @author Steve Ebersole
 */
public class StandardServiceRegistryImpl extends AbstractServiceRegistryImpl implements StandardServiceRegistry {

	//Access to this field requires synchronization on -this-
	private Map<String,Object> configurationValues;

	protected StandardServiceRegistryImpl(
			boolean autoCloseRegistry,
			BootstrapServiceRegistry bootstrapServiceRegistry,
			Map<String,Object> configurationValues) {
		super( bootstrapServiceRegistry, autoCloseRegistry );
		this.configurationValues = normalize( configurationValues);
	}

	/**
	 * Constructs a StandardServiceRegistryImpl.  Should not be instantiated directly; use
	 * {@link org.hibernate.boot.registry.StandardServiceRegistryBuilder} instead
	 *
	 * @param bootstrapServiceRegistry The bootstrap service registry.
	 * @param serviceInitiators Any StandardServiceInitiators provided by the user to the builder
	 * @param providedServices Any standard services provided directly to the builder
	 * @param configurationValues Configuration values
	 *
	 * @see org.hibernate.boot.registry.StandardServiceRegistryBuilder
	 */
	public static StandardServiceRegistryImpl create(
			BootstrapServiceRegistry bootstrapServiceRegistry,
			List<StandardServiceInitiator<?>> serviceInitiators,
			List<ProvidedService<?>> providedServices,
			Map<String,Object> configurationValues) {

		return create( true, bootstrapServiceRegistry, serviceInitiators, providedServices, configurationValues );
	}

	/**
	 * Constructs a StandardServiceRegistryImpl.  Should not be instantiated directly; use
	 * {@link org.hibernate.boot.registry.StandardServiceRegistryBuilder} instead
	 *
	 * @param autoCloseRegistry See discussion on
	 * {@link org.hibernate.boot.registry.StandardServiceRegistryBuilder#disableAutoClose}
	 * @param bootstrapServiceRegistry The bootstrap service registry.
	 * @param serviceInitiators Any StandardServiceInitiators provided by the user to the builder
	 * @param providedServices Any standard services provided directly to the builder
	 * @param configurationValues Configuration values
	 *
	 * @see org.hibernate.boot.registry.StandardServiceRegistryBuilder
	 */
	public static StandardServiceRegistryImpl create(
			boolean autoCloseRegistry,
			BootstrapServiceRegistry bootstrapServiceRegistry,
			List<StandardServiceInitiator<?>> serviceInitiators,
			List<ProvidedService<?>> providedServices,
			Map<String,Object> configurationValues) {

		StandardServiceRegistryImpl instance = new StandardServiceRegistryImpl( autoCloseRegistry, bootstrapServiceRegistry, configurationValues );
		instance.initialize();
		instance.applyServiceRegistrations( serviceInitiators, providedServices );

		return instance;
	}

	protected void applyServiceRegistrations(List<StandardServiceInitiator<?>> serviceInitiators, List<ProvidedService<?>> providedServices) {
		try {
			// process initiators
			for ( ServiceInitiator<?> initiator : serviceInitiators ) {
				createServiceBinding( initiator );
			}

			// then, explicitly provided service instances
			//noinspection rawtypes
			for ( ProvidedService providedService : providedServices ) {
				//noinspection unchecked
				createServiceBinding( providedService );
			}
		}
		catch (RuntimeException e) {
			visitServiceBindings( binding -> binding.getLifecycleOwner().stopService( binding ) );
			throw e;
		}
	}

	/**
	 * Not intended for general use. We need the ability to stop and "reactivate" a registry to allow
	 * experimentation with technologies such as GraalVM, Quarkus and Cri-O.
	 */
	public void resetAndReactivate(BootstrapServiceRegistry bootstrapServiceRegistry,
												List<StandardServiceInitiator<?>> serviceInitiators,
												List<ProvidedService<?>> providedServices,
												Map<?, ?> configurationValues) {
		thisLock.lock();
		try {
			if ( super.isActive() ) {
				throw new IllegalStateException( "Can't reactivate an active registry" );
			}
			super.resetParent( bootstrapServiceRegistry );
			this.configurationValues = new HashMap( configurationValues );
			super.reactivate();
			applyServiceRegistrations( serviceInitiators, providedServices );
		} finally {
			thisLock.unlock();
		}
	}


	@Override
	public <R extends Service> R initiateService(ServiceInitiator<R> serviceInitiator) {
		thisLock.lock();
		try {
			// todo : add check/error for unexpected initiator types?
			return ( (StandardServiceInitiator<R>) serviceInitiator ).initiateService( configurationValues, this );
		} finally {
			thisLock.unlock();
		}
	}

	@Override
	public <R extends Service> void configureService(ServiceBinding<R> serviceBinding) {
		thisLock.lock();
		try {
			if ( serviceBinding.getService() instanceof Configurable ) {
				( (Configurable) serviceBinding.getService() ).configure( configurationValues );
			}
		} finally {
			thisLock.unlock();
		}
	}

	@Override
	public void destroy() {
		thisLock.lock();
		try {
			super.destroy();
			this.configurationValues = null;
		} finally {
			thisLock.unlock();
		}
	}

	private static Map<String, Object> normalize(Map<String, Object> configurationValues) {
		final Object jdbcUrl = configurationValues.get( AvailableSettings.JAKARTA_JDBC_URL );
		if ( jdbcUrl != null ) {
			configurationValues.putIfAbsent( AvailableSettings.URL, jdbcUrl );
		}

		final Object username = configurationValues.get( AvailableSettings.JAKARTA_JDBC_USER );
		if ( username != null ) {
			configurationValues.putIfAbsent( AvailableSettings.USER, username );
		}

		final Object password = configurationValues.get( AvailableSettings.JAKARTA_JDBC_PASSWORD );
		if ( password != null ) {
			configurationValues.putIfAbsent( AvailableSettings.PASS, password );
		}

		final Object driver = configurationValues.get( AvailableSettings.JAKARTA_JDBC_DRIVER );
		if ( driver != null ) {
			configurationValues.putIfAbsent( AvailableSettings.DRIVER, driver );
		}

		final Object nonJtaDatasource = configurationValues.get( AvailableSettings.JAKARTA_NON_JTA_DATASOURCE );
		if ( nonJtaDatasource != null ) {
			configurationValues.putIfAbsent( AvailableSettings.DATASOURCE, nonJtaDatasource );
		}

		final Object jtaDatasource = configurationValues.get( AvailableSettings.JAKARTA_JTA_DATASOURCE );
		if ( jtaDatasource != null ) {
			configurationValues.putIfAbsent( AvailableSettings.DATASOURCE, jtaDatasource );
		}

		return configurationValues;
	}
}
