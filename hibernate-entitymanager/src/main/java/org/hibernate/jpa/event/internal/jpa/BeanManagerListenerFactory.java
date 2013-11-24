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
package org.hibernate.jpa.event.internal.jpa;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionTarget;

import org.hibernate.jpa.event.spi.jpa.ListenerFactory;

/**
 * CID-based implementation of the ListenerFactory contract.  Listener instances are kept in a map keyed by Class
 * to achieve singleton-ness.
 *
 * @author Steve Ebersole
 */
public class BeanManagerListenerFactory implements ListenerFactory {
	private final BeanManager beanManager;
	private final Map<Class,BeanMetaData> listeners = new ConcurrentHashMap<Class, BeanMetaData>();

	public static BeanManagerListenerFactory fromBeanManagerReference(Object beanManagerReference) {
		return new BeanManagerListenerFactory( ( BeanManager ) beanManagerReference );
	}

	public BeanManagerListenerFactory(BeanManager beanManager) {
		this.beanManager = beanManager;
	}

	@Override
	public <T> T buildListener(Class<T> listenerClass) {
		BeanMetaData<T> beanMetaData = listeners.get( listenerClass );
		if ( beanMetaData == null ) {
			beanMetaData = new BeanMetaData<T>( listenerClass );
			listeners.put( listenerClass, beanMetaData );
		}
		return beanMetaData.instance;
	}

	@Override
	public void release() {
		for ( BeanMetaData beanMetaData : listeners.values() ) {
			beanMetaData.release();
		}
		listeners.clear();
	}

	private class BeanMetaData<T> {
		private final InjectionTarget<T> injectionTarget;
		private final CreationalContext<T> creationalContext;
		private final T instance;

		private BeanMetaData(Class<T> listenerClass) {
			AnnotatedType<T> annotatedType = beanManager.createAnnotatedType( listenerClass );
			this.injectionTarget = beanManager.createInjectionTarget( annotatedType );
			this.creationalContext = beanManager.createCreationalContext( null );

			this.instance = injectionTarget.produce( creationalContext );
			injectionTarget.inject( this.instance, creationalContext );

			injectionTarget.postConstruct( this.instance );
		}

		private void release() {
			injectionTarget.preDestroy( instance );
			injectionTarget.dispose( instance );
			creationalContext.release();
		}
	}
}
