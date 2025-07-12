/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.internal;

import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.boot.registry.selector.spi.StrategySelector;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.event.spi.EntityCopyObserver;
import org.hibernate.event.spi.EntityCopyObserverFactory;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * Looks for the configuration property {@value AvailableSettings#MERGE_ENTITY_COPY_OBSERVER} and registers
 * the matching {@link EntityCopyObserverFactory} based on the configuration observerClass.
 * <p>
 * For known implementations some optimisations are possible, such as reusing a singleton for the stateless
 * implementations. When a user plugs in a custom {@link EntityCopyObserver} we take a defensive approach.
 * </p>
 */
public class EntityCopyObserverFactoryInitiator implements StandardServiceInitiator<EntityCopyObserverFactory> {

	public static final EntityCopyObserverFactoryInitiator INSTANCE = new EntityCopyObserverFactoryInitiator();
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( EntityCopyObserverFactoryInitiator.class );

	@Override
	public EntityCopyObserverFactory initiateService(final Map<String, Object> configurationValues, final ServiceRegistryImplementor registry) {
		final Object value = getConfigurationValue( configurationValues );
		if ( value instanceof EntityCopyObserverFactory factory ) {
			return factory;
		}
		else if ( value.equals( EntityCopyNotAllowedObserver.SHORT_NAME )
				|| value.equals( EntityCopyNotAllowedObserver.class.getName() ) ) {
			LOG.tracef( "Configured EntityCopyObserver strategy: %s",
					EntityCopyNotAllowedObserver.SHORT_NAME );
			return EntityCopyNotAllowedObserver.FACTORY_OF_SELF;
		}
		else if ( value.equals( EntityCopyAllowedObserver.SHORT_NAME )
				|| value.equals( EntityCopyAllowedObserver.class.getName() ) ) {
			LOG.tracef( "Configured EntityCopyObserver strategy: %s",
					EntityCopyAllowedObserver.SHORT_NAME );
			return EntityCopyAllowedObserver.FACTORY_OF_SELF;
		}
		else if ( value.equals( EntityCopyAllowedLoggedObserver.SHORT_NAME )
				|| value.equals( EntityCopyAllowedLoggedObserver.class.getName() ) ) {
			LOG.tracef( "Configured EntityCopyObserver strategy: %s",
					EntityCopyAllowedLoggedObserver.SHORT_NAME );
			return EntityCopyAllowedLoggedObserver.FACTORY_OF_SELF;
		}
		else {
			// We load an "example instance" just to get its Class;
			// this might look excessive, but it also happens to test eagerly
			// (at boot) that we can actually construct these and that they
			// are indeed of the right type.
			final EntityCopyObserver exampleInstance =
					registry.requireService( StrategySelector.class )
							.resolveStrategy( EntityCopyObserver.class, value );
			final Class<? extends EntityCopyObserver> observerType = exampleInstance.getClass();
			LOG.tracef( "Configured EntityCopyObserver is a custom implementation of type '%s'",
					observerType.getName() );
			return new EntityCopyObserverFactoryFromClass( observerType );
		}
	}

	private Object getConfigurationValue(final Map<?,?> configurationValues) {
		final Object value = configurationValues.get( AvailableSettings.MERGE_ENTITY_COPY_OBSERVER );
		if ( value == null ) {
			return EntityCopyNotAllowedObserver.SHORT_NAME; //default
		}
		else if ( value instanceof String string ) {
			return string.trim();
		}
		else {
			return value;
		}
	}

	@Override
	public Class<EntityCopyObserverFactory> getServiceInitiated() {
		return EntityCopyObserverFactory.class;
	}

	private record EntityCopyObserverFactoryFromClass(Class<? extends EntityCopyObserver> observerClass)
			implements EntityCopyObserverFactory {

		@Override
			public EntityCopyObserver createEntityCopyObserver() {
				try {
					return observerClass.newInstance();
				}
				catch (Exception e) {
					throw new HibernateException( "Could not instantiate class of type " + observerClass.getName() );
				}
			}
		}
}
