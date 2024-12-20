/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.resource.beans.spi;

/**
 * {@link ManagedBean} implementation for cases where we have been handed an actual
 * instance to use.
 *
 * @author Steve Ebersole
 */
public class ProvidedInstanceManagedBeanImpl<T> implements ManagedBean<T> {
	private final T instance;

	public ProvidedInstanceManagedBeanImpl(T instance) {
		if ( instance == null ) {
			throw new IllegalArgumentException( "Bean instance cannot be null" );
		}

		this.instance = instance;
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
