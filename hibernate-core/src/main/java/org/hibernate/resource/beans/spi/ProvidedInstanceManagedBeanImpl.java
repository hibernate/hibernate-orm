/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.resource.beans.spi;

import static org.hibernate.internal.util.Validator.checkNotNullIAE;

/**
 * ManagedBean implementation for cases where we have been handed an actual
 * instance to use.
 *
 * @author Steve Ebersole
 */
public class ProvidedInstanceManagedBeanImpl<T> implements ManagedBean<T> {
	private final T instance;

	public ProvidedInstanceManagedBeanImpl(T instance) {
		this.instance = checkNotNullIAE( "instance", instance );
	}

	@Override
	@SuppressWarnings("unchecked")
	public Class<T> getBeanClass() {
		return (Class<T>) instance.getClass();
	}

	@Override
	public T getBeanInstance() {
		return instance;
	}
}
