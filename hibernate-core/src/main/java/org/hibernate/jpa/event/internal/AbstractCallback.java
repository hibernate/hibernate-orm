/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.jpa.event.internal;

import org.hibernate.event.jpa.spi.Callback;
import org.hibernate.jpa.event.spi.CallbackType;

/// Base support for [Callback] implementations.
///
/// @author Steve Ebersole
abstract class AbstractCallback implements Callback {
	private final CallbackType callbackType;

	AbstractCallback(CallbackType callbackType) {
		this.callbackType = callbackType;
	}

	@Override
	public CallbackType getCallbackType() {
		return callbackType;
	}
}
