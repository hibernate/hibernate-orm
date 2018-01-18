/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.resource.beans.internal;

import org.hibernate.resource.beans.container.spi.BeanLifecycleStrategy;
import org.hibernate.resource.beans.container.internal.ContainerManagedLifecycleStrategy;
import org.hibernate.resource.beans.container.internal.JpaCompliantLifecycleStrategy;

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

	public String determineBeanCacheKey(Class beanType) {
		return beanType.getName();
	}

	public String determineBeanCacheKey(String name, Class beanType) {
		return beanType.getName() + ':' + name;
	}

	@SuppressWarnings("unused")
	public BeanLifecycleStrategy getLifecycleStrategy(boolean shouldRegistryManageLifecycle) {
		if ( shouldRegistryManageLifecycle ) {
			return JpaCompliantLifecycleStrategy.INSTANCE;
		}
		else {
			return ContainerManagedLifecycleStrategy.INSTANCE;
		}
	}
}
