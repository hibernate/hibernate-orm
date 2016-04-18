/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.event.internal.jpa;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionTarget;

import org.hibernate.jpa.event.spi.jpa.Listener;
import org.hibernate.jpa.event.spi.jpa.ListenerFactory;

import org.jboss.logging.Logger;

/**
 * CDI-based implementation of the ListenerFactory contract.  This implementation
 * delayes access to the CDI BeanManager until first need.
 *
 * @author Steve Ebersole
 */
@SuppressWarnings("unused")
public class ListenerFactoryBeanManagerDelayedImpl implements ListenerFactory {
	private static final Logger log = Logger.getLogger( ListenerFactoryBeanManagerDelayedImpl.class );

	private final BeanManager beanManager;
	private final Map<Class,ListenerImpl> listenerMap = new ConcurrentHashMap<Class, ListenerImpl>();

	/**
	 * Used via reflection from JpaIntegrator, the intent being to isolate CDI dependency
	 * to just this class and its delegates in the case that a BeanManager is passed.
	 *
	 * @param reference The BeanManager reference
	 *
	 * @return A instantiated ListenerFactoryBeanManagerImpl
	 */
	@SuppressWarnings("unused")
	public static ListenerFactoryBeanManagerDelayedImpl fromBeanManagerReference(Object reference) {
		if ( !BeanManager.class.isInstance( reference ) ) {
			throw new IllegalArgumentException(
					"Expecting BeanManager reference that implements CDI BeanManager contract : " +
							reference
			);
		}
		return new ListenerFactoryBeanManagerDelayedImpl( (BeanManager) reference );
	}

	public ListenerFactoryBeanManagerDelayedImpl(BeanManager beanManager) {
		this.beanManager = beanManager;
		log.debugf( "Delayed access requested to CDI BeanManager : " + beanManager );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> Listener<T> buildListener(Class<T> listenerClass) {
		ListenerImpl listenerImpl = listenerMap.get( listenerClass );
		if ( listenerImpl == null ) {
			listenerImpl = new ListenerImpl( listenerClass );
			listenerMap.put( listenerClass, listenerImpl );
		}
		return (Listener<T>) listenerImpl;
	}

	@Override
	public void release() {
		for ( ListenerImpl listenerImpl : listenerMap.values() ) {
			listenerImpl.release();
		}
		listenerMap.clear();
	}

	private class ListenerImpl<T> implements Listener<T> {
		private final Class<T> listenerClass;

		private boolean initialized = false;

		private InjectionTarget<T> injectionTarget;
		private CreationalContext<T> creationalContext;
		private T listenerInstance;

		private ListenerImpl(Class<T> listenerClass) {
			this.listenerClass = listenerClass;
			log.debugf( "Delayed CDI listener built : " + listenerClass );
		}

		public void initialize() {
			log.debug( "Initializing delayed CDI listener on first use : " + listenerClass );
			AnnotatedType<T> annotatedType = beanManager.createAnnotatedType( listenerClass );
			this.injectionTarget = beanManager.createInjectionTarget( annotatedType );
			this.creationalContext = beanManager.createCreationalContext( null );

			this.listenerInstance = injectionTarget.produce( creationalContext );
			injectionTarget.inject( this.listenerInstance, creationalContext );

			injectionTarget.postConstruct( this.listenerInstance );

			this.initialized = true;
		}

		@Override
		public T getListener() {
			if ( !initialized ) {
				initialize();
			}
			return listenerInstance;
		}

		public void release() {
			if ( !initialized ) {
				log.debug( "Skipping release for CDI listener [" + listenerClass + "] as it was not initialized" );
				return;
			}

			log.debug( "Releasing CDI listener : " + listenerClass );

			injectionTarget.preDestroy( listenerInstance );
			injectionTarget.dispose( listenerInstance );
			creationalContext.release();
		}
	}
}
