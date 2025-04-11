/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.resource.beans.spi;

import org.hibernate.internal.util.ReflectHelper;

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
	public Class<T> getBeanClass() {
		return ReflectHelper.getClass( instance );
	}

	@Override
	public T getBeanInstance() {
		return instance;
	}
}
