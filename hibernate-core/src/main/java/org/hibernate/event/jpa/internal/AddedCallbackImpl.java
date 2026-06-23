/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.jpa.internal;

import org.hibernate.event.jpa.spi.Callback;
import org.hibernate.jpa.event.spi.CallbackType;

import java.util.function.Consumer;
import jakarta.annotation.Nonnull;

/**
 * @author Steve Ebersole
 */
public class AddedCallbackImpl<E> implements Callback<E> {
	private final CallbackType callbackType;
	private final Consumer<? super E> listener;

	public AddedCallbackImpl(@Nonnull CallbackType callbackType, @Nonnull Consumer<? super E> listener) {
		this.callbackType = callbackType;
		this.listener = listener;
	}

	@Override
	@Nonnull
	public CallbackType getCallbackType() {
		return callbackType;
	}

	@Override
	public <S extends E> void performCallback(@Nonnull S entity) {
		listener.accept( entity );
	}
}
