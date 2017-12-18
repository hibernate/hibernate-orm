/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.resource.beans.internal;

import java.util.Set;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;

/**
 * @author Steve Ebersole
 */
public class Helper {
	/**
	 * Singleton access
	 */
	public static final Helper INSTANCE = new Helper();

	private Helper() {
	}

	@SuppressWarnings("unchecked")
	public <T> Bean<T> getNamedBean(String beanName, Class<T> beanContract, BeanManager beanManager) {
		Set<Bean<?>> beans = beanManager.getBeans( beanContract, new NamedBeanQualifier( beanName ) );

		if ( beans.isEmpty() ) {
			throw new IllegalArgumentException(
					"BeanManager returned no matching beans: name = " + beanName + "; contract = " + beanContract.getName()
			);
		}
		if ( beans.size() > 1 ) {
			throw new IllegalArgumentException(
					"BeanManager returned multiple matching beans: name = " + beanName + "; contract = " + beanContract.getName()
			);
		}

		return (Bean<T>) beans.iterator().next();
	}

	public CdiLifecycleManagementStrategy getLifecycleManagementStrategy(boolean shouldRegistryManageLifecycle) {
		if ( shouldRegistryManageLifecycle ) {
			return JpaCdiLifecycleManagementStrategy.INSTANCE;
		}
		else {
			return StandardCdiLifecycleManagementStrategy.INSTANCE;
		}
	}
}
