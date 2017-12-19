/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.resource.beans.internal;

import org.hibernate.resource.beans.spi.AbstractManagedBeanRegistry;
import org.hibernate.resource.beans.spi.DirectInstantiationManagedBeanImpl;
import org.hibernate.resource.beans.spi.ManagedBean;

/**
 * ManagedBeanRegistry implementation using direct instantiation of the beans.  Used
 * when there is no backing "injection framework" in use as well as the fallback
 * for {@link CompositeManagedBeanRegistry}
 *
 * @author Steve Ebersole
 */
public class ManagedBeanRegistryDirectImpl extends AbstractManagedBeanRegistry {
	@SuppressWarnings("WeakerAccess")
	public ManagedBeanRegistryDirectImpl() {
	}

	@Override
	protected <T> ManagedBean<T> createBean(Class<T> beanClass) {
		return new DirectInstantiationManagedBeanImpl<>( beanClass );
	}

	@Override
	protected <T> ManagedBean<T> createBean(String beanName, Class<T> beanContract) {
		return new DirectInstantiationManagedBeanImpl<>( beanContract );
	}
}
