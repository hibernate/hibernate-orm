/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.cdi.nonregistrymanaged;

import org.hibernate.boot.Metadata;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.resource.beans.spi.ManagedBean;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;

/**
 * Simulates a Hibernate ORM integrator consuming beans whose lifecycle is not managed by the registry,
 * but by the CDI engine only.
 *
 * @author Yoann Rodiere
 */
public class NonRegistryManagedBeanConsumingIntegrator implements Integrator {

	private ManagedBean<TheApplicationScopedBean> applicationScopedBean1;
	private ManagedBean<TheApplicationScopedBean> applicationScopedBean2;
	private ManagedBean<TheDependentBean> dependentBean1;
	private ManagedBean<TheDependentBean> dependentBean2;
	private ManagedBean<TheNamedApplicationScopedBean> namedApplicationScopedBean1;
	private ManagedBean<TheNamedApplicationScopedBean> namedApplicationScopedBean2;
	private ManagedBean<TheNamedDependentBean> namedDependentBean1;
	private ManagedBean<TheNamedDependentBean> namedDependentBean2;

	@Override
	public void integrate(Metadata metadata, SessionFactoryImplementor sessionFactory, SessionFactoryServiceRegistry serviceRegistry) {
		ManagedBeanRegistry registry = sessionFactory.getServiceRegistry().getService( ManagedBeanRegistry.class );

		applicationScopedBean1 = registry.getBean( TheApplicationScopedBean.class, false );
		applicationScopedBean2 = registry.getBean( TheApplicationScopedBean.class, false );
		dependentBean1 = registry.getBean( TheDependentBean.class, false );
		dependentBean2 = registry.getBean( TheDependentBean.class, false );
		namedApplicationScopedBean1 = registry.getBean( TheMainNamedApplicationScopedBeanImpl.NAME,
				TheNamedApplicationScopedBean.class, false );
		namedApplicationScopedBean2 = registry.getBean( TheMainNamedApplicationScopedBeanImpl.NAME,
				TheNamedApplicationScopedBean.class, false );
		namedDependentBean1 = registry.getBean( TheMainNamedDependentBeanImpl.NAME, TheNamedDependentBean.class,
						false );
		namedDependentBean2 = registry.getBean( TheMainNamedDependentBeanImpl.NAME, TheNamedDependentBean.class,
						false );
	}

	/**
	 * Use one instance from each ManagedBean, ensuring that any lazy initialization is executed,
	 * be it in Hibernate ORM ({@link org.hibernate.resource.beans.spi.ExtendedBeanManager support})
	 * or in CDI (proxies).
	 */
	public void ensureInstancesInitialized() {
		applicationScopedBean1.getBeanInstance().ensureInitialized();
		applicationScopedBean2.getBeanInstance().ensureInitialized();
		dependentBean1.getBeanInstance().ensureInitialized();
		dependentBean2.getBeanInstance().ensureInitialized();
		namedApplicationScopedBean1.getBeanInstance().ensureInitialized();
		namedApplicationScopedBean2.getBeanInstance().ensureInitialized();
		namedDependentBean1.getBeanInstance().ensureInitialized();
		namedDependentBean2.getBeanInstance().ensureInitialized();
	}

	@Override
	public void disintegrate(SessionFactoryImplementor sessionFactory, SessionFactoryServiceRegistry serviceRegistry) {
		applicationScopedBean1.release();
		applicationScopedBean2.release();
		dependentBean1.release();
		dependentBean2.release();
		namedApplicationScopedBean1.release();
		namedApplicationScopedBean2.release();
		namedDependentBean1.release();
		namedDependentBean2.release();
	}
}
