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
@SuppressWarnings("rawtypes")
public class NoCallbacks implements EntityCallbacks {
	public static NoCallbacks NO_CALLBACKS = new NoCallbacks();

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
	public boolean preUpdate(Object entity) {
		return false;
	}

	@Override
	public boolean postUpdate(Object entity) {
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
	public boolean postLoad(Object entity) {
		return false;
	}

	@Override
	public EntityListenerRegistration addListener(CallbackType type, Consumer listener) {
		return RegistrationImpl.NO_REG;
	}

	@Override
	public EntityListenerRegistration addListener(CallbackType type, Callback callback) {
		return RegistrationImpl.NO_REG;
	}

	@Override
	public void release() {
	}

	public static class RegistrationImpl implements EntityListenerRegistration {
		public static RegistrationImpl NO_REG = new RegistrationImpl();

		@Override
		public void cancel() {
		}
	}
}
