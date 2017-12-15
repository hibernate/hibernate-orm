/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.resource.beans.spi;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.hibernate.resource.beans.internal.BeansMessageLogger;
import org.hibernate.service.spi.Stoppable;

/**
 * Abstract support (template pattern) for ManagedBeanRegistry implementations
 *
 * @author Steve Ebersole
 */
public abstract class AbstractManagedBeanRegistry implements ManagedBeanRegistry, Stoppable {
	private Map<String,ManagedBean<?>> registrations = new HashMap<>();

	@Override
	public <T> ManagedBean<T> getBean(Class<T> beanClass, boolean shouldRegistryManageLifecycle) {
		if ( shouldRegistryManageLifecycle ) {
			return getCacheableBean( beanClass );
		}
		else {
			return createBean( beanClass );
		}
	}

	@SuppressWarnings({"WeakerAccess", "unchecked"})
	protected <T> ManagedBean<T> getCacheableBean(Class<T> beanClass) {
		final String beanName = beanClass.getName();
		final ManagedBean existing = registrations.get( beanName );
		if ( existing != null ) {
			return existing;
		}

		final ManagedBean<T> bean = createBean( beanClass );
		registrations.put( beanName, bean );
		return bean;
	}

	@SuppressWarnings("WeakerAccess")
	protected abstract <T> ManagedBean<T> createBean(Class<T> beanClass);

	@Override
	public <T> ManagedBean<T> getBean(String beanName, Class<T> beanContract, boolean shouldRegistryManageLifecycle) {
		if ( shouldRegistryManageLifecycle ) {
			return getCacheableBean( beanName, beanContract );
		}
		else {
			return createBean( beanName, beanContract );
		}
	}

	@SuppressWarnings({"WeakerAccess", "unchecked"})
	protected  <T> ManagedBean<T> getCacheableBean(String beanName, Class<T> beanContract) {
		final ManagedBean existing = registrations.get( beanName );
		if ( existing != null ) {
			return existing;
		}

		final ManagedBean<T> bean = createBean( beanName, beanContract );
		registrations.put( beanName, bean );
		return bean;
	}

	@SuppressWarnings("WeakerAccess")
	protected abstract <T> ManagedBean<T> createBean(String beanName, Class<T> beanContract);

	@SuppressWarnings("WeakerAccess")
	protected void forEachBean(Consumer<ManagedBean<?>> consumer) {
		registrations.values().forEach( consumer );
	}

	@Override
	public void stop() {
		BeansMessageLogger.BEANS_LOGGER.stoppingManagedBeanRegistry( this );
		forEachBean( ManagedBean::release );
		registrations.clear();
	}
}
