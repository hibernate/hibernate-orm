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
 * CDI-based implementation of the ListenerFactory contract.  Listener instances are
 * kept in a map keyed by Class to ensure single-instance-ness.
 *
 * @author Steve Ebersole
 */
public class ListenerFactoryBeanManagerImpl implements ListenerFactory, ExtendedBeanManager.LifecycleListener {
	private final BeanManager beanManager;
	private final boolean extendedBm;

	private final Map<Class,ListenerImplementor> listenerMap = new ConcurrentHashMap<Class, ListenerImplementor>();

	public static ListenerFactoryBeanManagerImpl fromBeanManagerReference(Object reference) {
		return new ListenerFactoryBeanManagerImpl( (BeanManager) reference );
	}

	public ListenerFactoryBeanManagerImpl(BeanManager beanManager) {
		this.beanManager = beanManager;
		if ( beanManager instanceof ExtendedBeanManager ) {
			( (ExtendedBeanManager) beanManager ).registerLifecycleListener( this );
			extendedBm = true;
		}
		else {
			extendedBm = false;
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> Listener<T> buildListener(Class<T> listenerClass) {
		ListenerImplementor listenerImpl = listenerMap.get( listenerClass );
		if ( listenerImpl == null ) {
			listenerImpl = makeListener( listenerClass );
			listenerMap.put( listenerClass, listenerImpl );
		}
		return (Listener<T>) listenerImpl;
	}

	private <T> ListenerImplementor makeListener(Class<T> listenerClass) {
		if ( extendedBm ) {
			return new ListenerExtendedImpl<T>( listenerClass );
		}
		else {
			return new ListenerBasicImpl<T>( listenerClass );
		}
	}

	@Override
	public void release() {
		for ( ListenerImplementor listenerImpl : listenerMap.values() ) {
			listenerImpl.release();
		}
		listenerMap.clear();
	}

	@Override
	public void beanManagerInitialized() {
		for ( ListenerImplementor listenerImpl : listenerMap.values() ) {
			// if the entries are not ListenerExtendedImpl instances we have serious issues...
			( (ListenerExtendedImpl) listenerImpl ).initialize();
		}
	}

	private interface ListenerImplementor<T> extends Listener<T> {
		void release();
	}

	private class ListenerBasicImpl<T> implements ListenerImplementor<T> {
		private final InjectionTarget<T> injectionTarget;
		private final CreationalContext<T> creationalContext;
		private final T listenerInstance;

		private ListenerBasicImpl(Class<T> listenerClass) {
			AnnotatedType<T> annotatedType = beanManager.createAnnotatedType( listenerClass );
			this.injectionTarget = beanManager.createInjectionTarget( annotatedType );
			this.creationalContext = beanManager.createCreationalContext( null );

			this.listenerInstance = injectionTarget.produce( creationalContext );
			injectionTarget.inject( this.listenerInstance, creationalContext );

			injectionTarget.postConstruct( this.listenerInstance );
		}

		@Override
		public T getListener() {
			return listenerInstance;
		}

		public void release() {
			injectionTarget.preDestroy( listenerInstance );
			injectionTarget.dispose( listenerInstance );
			creationalContext.release();
		}
	}

	private class ListenerExtendedImpl<T> implements ListenerImplementor<T> {
		private final Class<T> listenerClass;

		private boolean initialized = false;

		private InjectionTarget<T> injectionTarget;
		private CreationalContext<T> creationalContext;
		private T listenerInstance;

		private ListenerExtendedImpl(Class<T> listenerClass) {
			this.listenerClass = listenerClass;
		}

		public void initialize() {
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
