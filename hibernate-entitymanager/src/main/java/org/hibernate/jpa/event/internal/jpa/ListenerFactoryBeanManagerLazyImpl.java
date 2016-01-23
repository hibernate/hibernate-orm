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

import org.hibernate.HibernateException;
import org.hibernate.jpa.event.spi.jpa.ExtendedBeanManager;
import org.hibernate.jpa.event.spi.jpa.Listener;
import org.hibernate.jpa.event.spi.jpa.ListenerFactory;

/**
 * CDI-based implementation of the ListenerFactory contract.  Further, this
 * implementation leverages the ExtendedBeanManager contract to delay CDI calls.
 *
 * @author Steve Ebersole
 */
@SuppressWarnings("unused")
public class ListenerFactoryBeanManagerLazyImpl implements ListenerFactory, ExtendedBeanManager.LifecycleListener {
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
	public static ListenerFactoryBeanManagerLazyImpl fromBeanManagerReference(Object reference) {
		return new ListenerFactoryBeanManagerLazyImpl( (BeanManager) reference );
	}

	public ListenerFactoryBeanManagerLazyImpl(BeanManager beanManager) {
		if ( !ExtendedBeanManager.class.isInstance( beanManager ) ) {
			throw new IllegalArgumentException(
					"Expecting BeanManager reference that implements optional ExtendedBeanManager contract : " +
							beanManager
			);
		}
		( (ExtendedBeanManager) beanManager ).registerLifecycleListener( this );
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

	@Override
	public void beanManagerInitialized(BeanManager beanManager) {
		for ( ListenerImpl listenerImpl : listenerMap.values() ) {
			listenerImpl.initialize( beanManager );
		}
	}

	private static class ListenerImpl<T> implements Listener<T> {
		private final Class<T> listenerClass;

		private boolean initialized = false;

		private InjectionTarget<T> injectionTarget;
		private CreationalContext<T> creationalContext;
		private T listenerInstance;

		private ListenerImpl(Class<T> listenerClass) {
			this.listenerClass = listenerClass;
		}

		public void initialize(BeanManager beanManager) {
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
				throw new HibernateException( "CDI not initialized as expected" );
			}
			return listenerInstance;
		}

		public void release() {
			if ( !initialized ) {
				// log
				return;
			}

			injectionTarget.preDestroy( listenerInstance );
			injectionTarget.dispose( listenerInstance );
			creationalContext.release();
		}
	}
}
