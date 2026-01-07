/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.jpa.internal;

import org.hibernate.event.jpa.spi.Callback;
import org.hibernate.jpa.event.spi.CallbackType;

import java.util.function.Consumer;

/**
 * @author Steve Ebersole
 */
public class AddedCallbackImpl<E> implements Callback<E> {
	private final CallbackType callbackType;
	private final Consumer<E> listener;

	public AddedCallbackImpl(CallbackType callbackType, Consumer<E> listener) {
		this.callbackType = callbackType;
		this.listener = listener;
	}

	@Override
	public CallbackType getCallbackType() {
		return callbackType;
	}

	@Override
	public <S extends E> void performCallback(S entity) {
		listener.accept( entity );
	}
}
