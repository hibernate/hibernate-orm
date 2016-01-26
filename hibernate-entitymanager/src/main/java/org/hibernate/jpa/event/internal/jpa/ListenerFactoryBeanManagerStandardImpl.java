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
 * CDI-based implementation of the ListenerFactory contract.  This CDI-based
 * implementation works in the JPA standard prescribed manner.
 * <p/>
 * See {@link ListenerFactoryBeanManagerExtendedImpl} for an alt implementation that
 * is still JPA compliant, but that works based on delayed CDI calls.  Works
 * on a non-CDI-defined CDI extension; we plan to propose this extension to the
 * CDI expert group for the next CDI iteration
 *
 * @author Steve Ebersole
 */
@SuppressWarnings("unused")
public class ListenerFactoryBeanManagerStandardImpl implements ListenerFactory {
	private static final Logger log = Logger.getLogger( ListenerFactoryBeanManagerStandardImpl.class );

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
	public static ListenerFactoryBeanManagerStandardImpl fromBeanManagerReference(Object reference) {
		if ( !BeanManager.class.isInstance( reference ) ) {
			throw new IllegalArgumentException(
					"Expecting BeanManager reference that implements CDI BeanManager contract : " +
							reference
			);
		}
		return new ListenerFactoryBeanManagerStandardImpl( (BeanManager) reference );
	}

	public ListenerFactoryBeanManagerStandardImpl(BeanManager beanManager) {
		this.beanManager = beanManager;
		log.debugf( "Standard access requested to CDI BeanManager : " + beanManager );
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
		private final InjectionTarget<T> injectionTarget;
		private final CreationalContext<T> creationalContext;
		private final T listenerInstance;

		private ListenerImpl(Class<T> listenerClass) {
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
}
