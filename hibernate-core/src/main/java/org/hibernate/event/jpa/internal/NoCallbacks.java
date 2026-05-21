/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.jpa.internal;

import jakarta.persistence.EntityListenerRegistration;
import org.hibernate.event.jpa.spi.EntityCallbacks;
import org.hibernate.event.jpa.spi.Callback;
import org.hibernate.jpa.event.spi.CallbackType;

import java.util.function.Consumer;

/**
 * @author Steve Ebersole
 */
public class NoCallbacks implements EntityCallbacks<Object> {
	public static final NoCallbacks NO_CALLBACKS = new NoCallbacks();

	@Override
	public boolean hasRegisteredCallbacks(CallbackType callbackType) {
		return false;
	}

	@Override
	public boolean preCreate(Object entity) {
		return false;
	}

	@Override
	public boolean postCreate(Object entity) {
		return false;
	}

	@Override
	public boolean preMerge(Object entity) {
		return false;
	}

	@Override
	public boolean preInsert(Object entity) {
		return false;
	}

	@Override
	public boolean postInsert(Object entity) {
		return false;
	}

	@Override
	public boolean preUpdate(Object entity) {
		return false;
	}

	@Override
	public boolean postUpdate(Object entity) {
		return false;
	}

	@Override
	public boolean preUpsert(Object entity) {
		return false;
	}

	@Override
	public boolean postUpsert(Object entity) {
		return false;
	}

	@Override
	public boolean preRemove(Object entity) {
		return false;
	}

	@Override
	public boolean postRemove(Object entity) {
		return false;
	}

	@Override
	public boolean preDelete(Object entity) {
		return false;
	}

	@Override
	public boolean postDelete(Object entity) {
		return false;
	}

	@Override
	public boolean postLoad(Object entity) {
		return false;
	}

	@Override
	public EntityListenerRegistration addListener(CallbackType type, Consumer<? super Object> listener) {
		return RegistrationImpl.NO_REG;
	}

	@Override
	public EntityListenerRegistration addListener(CallbackType type, Callback<? super Object> callback) {
		return RegistrationImpl.NO_REG;
	}

	public static class RegistrationImpl implements EntityListenerRegistration {
		public static RegistrationImpl NO_REG = new RegistrationImpl();

		@Override
		public void cancel() {
		}
	}
}
