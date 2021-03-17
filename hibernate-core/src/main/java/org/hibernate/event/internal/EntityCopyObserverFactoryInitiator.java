/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
 * Looks for the configuration property {@link AvailableSettings#MERGE_ENTITY_COPY_OBSERVER} and registers
 * the matching {@link EntityCopyObserverFactory} based on the configuration value.
 * <p>
 * For known implementations some optimisations are possible, such as reusing a singleton for the stateless
 * implementations. When a user plugs in a custom {@link EntityCopyObserver} we take a defensive approach.
 * </p>
 */
public class EntityCopyObserverFactoryInitiator implements StandardServiceInitiator<EntityCopyObserverFactory> {

	public static final EntityCopyObserverFactoryInitiator INSTANCE = new EntityCopyObserverFactoryInitiator();
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( EntityCopyObserverFactoryInitiator.class );

	@Override
	public EntityCopyObserverFactory initiateService(final Map configurationValues, final ServiceRegistryImplementor registry) {
		final Object value = getConfigurationValue( configurationValues );
		if ( value.equals( EntityCopyNotAllowedObserver.SHORT_NAME ) || value.equals( EntityCopyNotAllowedObserver.class.getName() ) ) {
			LOG.debugf( "Configured EntityCopyObserver strategy: %s", EntityCopyNotAllowedObserver.SHORT_NAME );
			return EntityCopyNotAllowedObserver.FACTORY_OF_SELF;
		}
		else if ( value.equals( EntityCopyAllowedObserver.SHORT_NAME ) || value.equals( EntityCopyAllowedObserver.class.getName() ) ) {
			LOG.debugf( "Configured EntityCopyObserver strategy: %s", EntityCopyAllowedObserver.SHORT_NAME );
			return EntityCopyAllowedObserver.FACTORY_OF_SELF;
		}
		else if ( value.equals( EntityCopyAllowedLoggedObserver.SHORT_NAME ) || value.equals( EntityCopyAllowedLoggedObserver.class.getName() ) ) {
			LOG.debugf( "Configured EntityCopyObserver strategy: %s",  EntityCopyAllowedLoggedObserver.SHORT_NAME );
			return EntityCopyAllowedLoggedObserver.FACTORY_OF_SELF;
		}
		else {
			//We load an "example instance" just to get its Class;
			//this might look excessive, but it also happens to test eagerly (at boot) that we can actually construct these
			//and that they are indeed of the right type.
			EntityCopyObserver exampleInstance = registry.getService( StrategySelector.class ).resolveStrategy( EntityCopyObserver.class, value );
			Class observerType = exampleInstance.getClass();
			LOG.debugf( "Configured EntityCopyObserver is a custom implementation of type %s", observerType.getName() );
			return new EntityObserversFactoryFromClass( observerType );
		}
	}

	private Object getConfigurationValue(final Map configurationValues) {
		final Object o = configurationValues.get( AvailableSettings.MERGE_ENTITY_COPY_OBSERVER );
		if ( o == null ) {
			return EntityCopyNotAllowedObserver.SHORT_NAME; //default
		}
		else if ( o instanceof String ) {
			return o.toString().trim();
		}
		else {
			return o;
		}
	}

	@Override
	public Class<EntityCopyObserverFactory> getServiceInitiated() {
		return EntityCopyObserverFactory.class;
	}

	private static class EntityObserversFactoryFromClass implements EntityCopyObserverFactory {

		private final Class value;

		public EntityObserversFactoryFromClass(Class value) {
			this.value = value;
		}

		@Override
		public EntityCopyObserver createEntityCopyObserver() {
			try {
				return (EntityCopyObserver) value.newInstance();
			}
			catch (Exception e) {
				throw new HibernateException( "Could not instantiate class of type " + value.getName() );
			}
		}
	}

}
