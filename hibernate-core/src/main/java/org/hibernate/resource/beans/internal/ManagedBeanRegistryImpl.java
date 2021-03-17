/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.resource.beans.internal;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.resource.beans.container.spi.BeanContainer;
import org.hibernate.resource.beans.container.spi.ContainedBean;
import org.hibernate.resource.beans.container.spi.FallbackContainedBean;
import org.hibernate.resource.beans.spi.ManagedBean;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;
import org.hibernate.service.spi.Stoppable;

/**
 * Abstract support (template pattern) for ManagedBeanRegistry implementations
 *
 * @author Steve Ebersole
 */
public class ManagedBeanRegistryImpl implements ManagedBeanRegistry, BeanContainer.LifecycleOptions, Stoppable {
	private Map<String,ManagedBean<?>> registrations = new HashMap<>();

	private final BeanContainer beanContainer;

	public ManagedBeanRegistryImpl(BeanContainer beanContainer) {
		this.beanContainer = beanContainer;
	}

	@Override
	public BeanContainer getBeanContainer() {
		return beanContainer;
	}

	@Override
	public boolean canUseCachedReferences() {
		return true;
	}

	@Override
	public boolean useJpaCompliantCreation() {
		return true;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> ManagedBean<T> getBean(Class<T> beanClass) {
		final ManagedBean existing = registrations.get( beanClass.getName() );
		if ( existing != null ) {
			return existing;
		}

		final ManagedBean bean;
		if ( beanContainer == null ) {
			bean = new FallbackContainedBean( beanClass, FallbackBeanInstanceProducer.INSTANCE );
		}
		else {
			final ContainedBean<T> containedBean = beanContainer.getBean(
					beanClass,
					this,
					FallbackBeanInstanceProducer.INSTANCE
			);

			if ( containedBean instanceof ManagedBean ) {
				bean = (ManagedBean) containedBean;
			}
			else {
				bean = new ContainedBeanManagedBeanAdapter( beanClass, containedBean );
			}
		}

		registrations.put( beanClass.getName(), bean );

		return bean;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> ManagedBean<T> getBean(String beanName, Class<T> beanContract) {
		final String key = beanContract.getName() + ':' + beanName;

		final ManagedBean existing = registrations.get( key );
		if ( existing != null ) {
			return existing;
		}

		final ManagedBean bean;
		if ( beanContainer == null ) {
			bean = new FallbackContainedBean( beanName, beanContract, FallbackBeanInstanceProducer.INSTANCE );
		}
		else {
			final ContainedBean<T> containedBean = beanContainer.getBean(
					beanName,
					beanContract,
					this,
					FallbackBeanInstanceProducer.INSTANCE
			);

			if ( containedBean instanceof ManagedBean ) {
				bean = (ManagedBean) containedBean;
			}
			else {
				bean = new ContainedBeanManagedBeanAdapter( beanContract, containedBean );
			}
		}

		registrations.put( key, bean );

		return bean;
	}

	@Override
	public void stop() {
		if ( beanContainer != null ) {
			beanContainer.stop();
		}
		registrations.clear();
	}

	private static class ContainedBeanManagedBeanAdapter<B> implements ManagedBean<B> {
		private final Class<B> beanClass;
		private final ContainedBean<B> containedBean;

		private ContainedBeanManagedBeanAdapter(Class<B> beanClass, ContainedBean<B> containedBean) {
			this.beanClass = beanClass;
			this.containedBean = containedBean;
		}

		@Override
		public Class<B> getBeanClass() {
			return beanClass;
		}

		@Override
		public B getBeanInstance() {
			return containedBean.getBeanInstance();
		}
	}
}
