/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.resource.cdi.spi;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.resource.cdi.internal.CdiMessageLogger;
import org.hibernate.service.spi.Stoppable;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractManagedBeanRegistry implements ManagedBeanRegistry, Stoppable {
	private Map<Class,ManagedBean<?>> registrations = new HashMap<>();

	@Override
	@SuppressWarnings("unchecked")
	public <T> ManagedBean<T> getBean(Class<T> beanClass) {
		final ManagedBean<T> existing = (ManagedBean<T>) registrations.get( beanClass );
		if ( existing != null ) {
			return existing;
		}

		final ManagedBean<T> bean = createBean( beanClass );
		registrations.put( beanClass, bean );
		return bean;
	}

	protected abstract <T> ManagedBean<T> createBean(Class<T> beanClass);

	protected void forEachBean(Consumer<ManagedBean<?>> consumer) {
		registrations.values().forEach( consumer );
	}

	@Override
	public void stop() {
		CdiMessageLogger.CDI_LOGGER.stoppingManagedBeanRegistry( this );
		forEachBean( ManagedBean::release );
		registrations.clear();
	}
}