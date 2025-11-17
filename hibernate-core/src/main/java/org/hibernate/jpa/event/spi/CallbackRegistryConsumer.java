/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.jpa.event.spi;

/**
 * Contract for injecting the registry of Callbacks into event listeners.
 *
 * @author Emmanuel Bernard
 * @author Steve Ebersole
 */
public interface CallbackRegistryConsumer {
	/**
	 * Injection of the CallbackRegistry
	 *
	 * @param callbackRegistry The CallbackRegistry
	 */
	void injectCallbackRegistry(CallbackRegistry callbackRegistry);
}
