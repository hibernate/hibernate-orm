/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.jpa.event.internal;

import org.hibernate.jpa.event.spi.CallbackRegistry;
import org.hibernate.jpa.event.spi.CallbackType;

final class EmptyCallbackRegistryImpl implements CallbackRegistry {

	@Override
	public boolean hasRegisteredCallbacks(final Class<?> entityClass, final CallbackType callbackType) {
		return false;
	}

	@Override
	public void preCreate(final Object entity) {
		//no-op
	}

	@Override
	public void postCreate(final Object entity) {
		//no-op
	}

	@Override
	public boolean preUpdate(final Object entity) {
		return false;
	}

	@Override
	public void postUpdate(final Object entity) {
		//no-op
	}

	@Override
	public void preRemove(final Object entity) {
		//no-op
	}

	@Override
	public void postRemove(final Object entity) {
		//no-op
	}

	@Override
	public boolean postLoad(final Object entity) {
		return false;
	}

	@Override
	public void release() {
		//no-op
	}

}
