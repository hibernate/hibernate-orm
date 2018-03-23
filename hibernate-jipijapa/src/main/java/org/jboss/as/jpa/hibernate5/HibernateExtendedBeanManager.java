/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.jboss.as.jpa.hibernate5;

import java.util.ArrayList;

import javax.enterprise.inject.spi.BeanManager;

import org.hibernate.resource.beans.container.spi.ExtendedBeanManager;

/**
 * HibernateExtendedBeanManager helps defer the registering of entity listeners, with the CDI BeanManager until
 * after the persistence unit is available for lookup by CDI bean(s).
 * This solves the WFLY-2387 issue of JPA entity listeners referencing the CDI bean, when the bean cycles back
 * to the persistence unit, or a different persistence unit.
 *
 * @author Scott Marlow
 */
public class HibernateExtendedBeanManager implements ExtendedBeanManager {
	private final ArrayList<LifecycleListener> lifecycleListeners = new ArrayList<>();
	private final BeanManager beanManager;

	public HibernateExtendedBeanManager(BeanManager beanManager) {
		this.beanManager = beanManager;
	}

	/**
	 * Hibernate calls registerLifecycleListener to register N callbacks to be notified
	 * when the CDI BeanManager can safely be used.  The CDI BeanManager can safely be used
	 * when the CDI AfterDeploymentValidation event is reached.
	 *
	 * @param lifecycleListener Note: Caller (BeanManagerAfterDeploymentValidation) is expected to synchronize calls to
	 * registerLifecycleListener() + beanManagerIsAvailableForUse(), which protects
	 * HibernateExtendedBeanManager.lifecycleListeners from being read/written from multiple concurrent threads.
	 * There are many writer threads (one per deployed persistence unit) and one reader/writer thread expected
	 * to be triggered by one AfterDeploymentValidation event per deployment.
	 */
	@Override
	public void registerLifecycleListener(LifecycleListener lifecycleListener) {
		lifecycleListeners.add( lifecycleListener );
	}

	public void beanManagerIsAvailableForUse() {
		for ( LifecycleListener hibernateCallback : lifecycleListeners ) {
			hibernateCallback.beanManagerInitialized( beanManager );
		}
	}

}
