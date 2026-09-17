/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cdi.general.hibernatesearch;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.resource.beans.container.spi.BeanContainer;
import org.hibernate.resource.beans.container.spi.ContainedBean;
import org.hibernate.resource.beans.container.spi.ExtendedBeanManager;
import org.hibernate.resource.beans.spi.BeanInstanceProducer;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;

import org.hamcrest.CoreMatchers;

import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Simulates Hibernate Search's implementation of {@link Integrator},
 * consuming beans whose lifecycle is not managed by the registry,
 * but by the CDI engine only.
 *
 * @author Yoann Rodiere
 */
public class HibernateSearchSimulatedIntegrator implements Integrator, BeanContainer.LifecycleOptions {

	private final BeanInstanceProducer fallbackBeanInstanceProducer;

	private ContainedBean<TheApplicationScopedBean> applicationScopedBean1;
	private ContainedBean<TheApplicationScopedBean> applicationScopedBean2;
	private ContainedBean<TheDependentBean> dependentBean1;
	private ContainedBean<TheDependentBean> dependentBean2;
	private ContainedBean<TheReflectionInstantiatedBean> reflectionInstantiatedBean1;
	private ContainedBean<TheReflectionInstantiatedBean> reflectionInstantiatedBean2;
	private ContainedBean<TheNamedApplicationScopedBean> namedApplicationScopedBean1;
	private ContainedBean<TheNamedApplicationScopedBean> namedApplicationScopedBean2;
	private ContainedBean<TheNamedDependentBean> namedDependentBean1;
	private ContainedBean<TheNamedDependentBean> namedDependentBean2;
	private ContainedBean<TheReflectionInstantiatedBean> namedReflectionInstantiatedBean1;
	private ContainedBean<TheReflectionInstantiatedBean> namedReflectionInstantiatedBean2;

	public HibernateSearchSimulatedIntegrator(BeanInstanceProducer fallbackBeanInstanceProducer) {
		this.fallbackBeanInstanceProducer = fallbackBeanInstanceProducer;
	}

	@Override
	public boolean canUseCachedReferences() {
		return false;
	}

	@Override
	public boolean useJpaCompliantCreation() {
		return false;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void integrate(
			Metadata metadata,
			BootstrapContext bootstrapContext,
			SessionFactoryImplementor sessionFactory) {
		ManagedBeanRegistry registry = bootstrapContext.getManagedBeanRegistry();

		BeanContainer beanContainer = registry.getBeanContainer();
		assertThat( beanContainer, CoreMatchers.notNullValue() );


		applicationScopedBean1 = beanContainer.getBean(
				TheApplicationScopedBean.class,
				this,
				fallbackBeanInstanceProducer
		);
		applicationScopedBean2 = beanContainer.getBean(
				TheApplicationScopedBean.class,
				this,
				fallbackBeanInstanceProducer
		);
		dependentBean1 = beanContainer.getBean(
				TheDependentBean.class,
				this,
				fallbackBeanInstanceProducer
		);
		dependentBean2 = beanContainer.getBean(
				TheDependentBean.class,
				this,
				fallbackBeanInstanceProducer
		);
		reflectionInstantiatedBean1 = beanContainer.getBean(
				TheReflectionInstantiatedBean.class,
				this,
				fallbackBeanInstanceProducer
		);
		reflectionInstantiatedBean2 = beanContainer.getBean(
				TheReflectionInstantiatedBean.class,
				this,
				fallbackBeanInstanceProducer
		);
		namedApplicationScopedBean1 = beanContainer.getBean(
				TheMainNamedApplicationScopedBeanImpl.NAME,
				TheNamedApplicationScopedBean.class,
				this,
				fallbackBeanInstanceProducer
		);
		namedApplicationScopedBean2 = beanContainer.getBean(
				TheMainNamedApplicationScopedBeanImpl.NAME,
				TheNamedApplicationScopedBean.class,
				this,
				fallbackBeanInstanceProducer
		);
		namedDependentBean1 = beanContainer.getBean(
				TheMainNamedDependentBeanImpl.NAME,
				TheNamedDependentBean.class,
				this,
				fallbackBeanInstanceProducer
		);
		namedDependentBean2 = beanContainer.getBean(
				TheMainNamedDependentBeanImpl.NAME,
				TheNamedDependentBean.class,
				this,
				fallbackBeanInstanceProducer
		);
		namedReflectionInstantiatedBean1 = beanContainer.getBean(
				TheReflectionInstantiatedBean.class.getName(),
				TheReflectionInstantiatedBean.class,
				this,
				fallbackBeanInstanceProducer
		);
		namedReflectionInstantiatedBean2 = beanContainer.getBean(
				TheReflectionInstantiatedBean.class.getName(),
				TheReflectionInstantiatedBean.class,
				this,
				fallbackBeanInstanceProducer
		);
	}

	/**
	 * Use one instance from each ManagedBean, ensuring that any lazy initialization is executed,
	 * be it in Hibernate ORM ({@link ExtendedBeanManager support})
	 * or in CDI (proxies).
	 */
	public void ensureInstancesInitialized() {
		applicationScopedBean1.getBeanInstance().ensureInitialized();
		applicationScopedBean2.getBeanInstance().ensureInitialized();
		dependentBean1.getBeanInstance().ensureInitialized();
		dependentBean2.getBeanInstance().ensureInitialized();
		reflectionInstantiatedBean1.getBeanInstance().ensureInitialized();
		reflectionInstantiatedBean2.getBeanInstance().ensureInitialized();
		namedApplicationScopedBean1.getBeanInstance().ensureInitialized();
		namedApplicationScopedBean2.getBeanInstance().ensureInitialized();
		namedDependentBean1.getBeanInstance().ensureInitialized();
		namedDependentBean2.getBeanInstance().ensureInitialized();
		namedReflectionInstantiatedBean1.getBeanInstance().ensureInitialized();
		namedReflectionInstantiatedBean2.getBeanInstance().ensureInitialized();
	}

	@Override
	public void disintegrate(SessionFactoryImplementor sessionFactory, SessionFactoryServiceRegistry serviceRegistry) {
		applicationScopedBean1.release();
		applicationScopedBean2.release();
		dependentBean1.release();
		dependentBean2.release();
		reflectionInstantiatedBean1.release();
		reflectionInstantiatedBean2.release();
		namedApplicationScopedBean1.release();
		namedApplicationScopedBean2.release();
		namedDependentBean1.release();
		namedDependentBean2.release();
		namedReflectionInstantiatedBean1.release();
		namedReflectionInstantiatedBean2.release();
	}
}
