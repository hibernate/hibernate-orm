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
import jakarta.annotation.Nonnull;

/**
 * @author Steve Ebersole
 */
public class NoCallbacks implements EntityCallbacks<Object> {
	public static final NoCallbacks NO_CALLBACKS = new NoCallbacks();

	@Override
	public boolean hasRegisteredCallbacks(@Nonnull CallbackType callbackType) {
		return false;
	}

	@Override
	public boolean preCreate(@Nonnull Object entity) {
		return false;
	}

	@Override
	public boolean postCreate(@Nonnull Object entity) {
		return false;
	}

	@Override
	public boolean preMerge(@Nonnull Object entity) {
		return false;
	}

	@Override
	public boolean preInsert(@Nonnull Object entity) {
		return false;
	}

	@Override
	public boolean postInsert(@Nonnull Object entity) {
		return false;
	}

	@Override
	public boolean preUpdate(@Nonnull Object entity) {
		return false;
	}

	@Override
	public boolean postUpdate(@Nonnull Object entity) {
		return false;
	}

	@Override
	public boolean preUpsert(@Nonnull Object entity) {
		return false;
	}

	@Override
	public boolean postUpsert(@Nonnull Object entity) {
		return false;
	}

	@Override
	public boolean preRemove(@Nonnull Object entity) {
		return false;
	}

	@Override
	public boolean postRemove(@Nonnull Object entity) {
		return false;
	}

	@Override
	public boolean preDelete(@Nonnull Object entity) {
		return false;
	}

	@Override
	public boolean postDelete(@Nonnull Object entity) {
		return false;
	}

	@Override
	public boolean postLoad(@Nonnull Object entity) {
		return false;
	}

	@Override
	@Nonnull
	public EntityListenerRegistration addListener(@Nonnull CallbackType type, @Nonnull Consumer<? super Object> listener) {
		return RegistrationImpl.NO_REG;
	}

	@Override
	@Nonnull
	public EntityListenerRegistration addListener(@Nonnull CallbackType type, @Nonnull Callback<? super Object> callback) {
		return RegistrationImpl.NO_REG;
	}

	public static class RegistrationImpl implements EntityListenerRegistration {
		public static RegistrationImpl NO_REG = new RegistrationImpl();

		@Override
		public void cancel() {
		}
	}
}
