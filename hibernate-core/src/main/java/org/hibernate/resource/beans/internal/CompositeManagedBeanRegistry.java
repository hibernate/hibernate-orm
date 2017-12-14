/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.resource.beans.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.hibernate.resource.beans.spi.ManagedBean;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;

/**
 * A ManagedBeanRegistry implementation that can delegate to multiple
 * other ManagedBeanRegistry instances, until one can handle the given call.
 *
 * @author Steve Ebersole
 */
public class CompositeManagedBeanRegistry implements ManagedBeanRegistry {

	private List<ManagedBeanRegistry> delegates;

	public void addDelegate(ManagedBeanRegistry beanRegistry) {
		if ( delegates == null ) {
			delegates = new ArrayList<>();
		}

		delegates.add( beanRegistry );
	}
	@Override
	public <T> ManagedBean<T> getBean(Class<T> beanClass) {
		return tryEachRegistry( registry -> registry.getBean( beanClass ) );
	}

	@Override
	public <T> ManagedBean<T> getBean(String beanName, Class<T> contract) {
		return tryEachRegistry( registry -> registry.getBean( beanName, contract ) );
	}

	private <T> ManagedBean<T> tryEachRegistry(Function<ManagedBeanRegistry, ManagedBean<T>> delegateAction) {
		if ( delegates != null ) {
			for ( ManagedBeanRegistry delegate : delegates ) {
				ManagedBean<T> bean = null;
				try {
					bean = delegateAction.apply( delegate );
				}
				catch (Exception ignore) {
				}

				if ( bean != null ) {
					return bean;
				}
			}
		}

		return delegateAction.apply( ManagedBeanRegistryDirectImpl.INSTANCE );
	}
}
