/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.jpa.event.internal;

import org.hibernate.jpa.event.spi.Callback;
import org.hibernate.jpa.event.spi.CallbackType;

/**
 * Abstract support for Callback implementations
 *
 * @author Steve Ebersole
 */
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
