/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.event.spi;

import org.hibernate.event.service.spi.DuplicationStrategy;

/**
 * Event listener duplication strategy for envers
 *
 * @author Steve Ebersole
 */
public class EnversListenerDuplicationStrategy implements DuplicationStrategy {
	/**
	 * Singleton access
	 */
	public static final EnversListenerDuplicationStrategy INSTANCE = new EnversListenerDuplicationStrategy();

	@Override
	public boolean areMatch(Object listener, Object original) {
		return listener.getClass().equals( original.getClass() ) && EnversListener.class.isInstance( listener );
	}

	@Override
	public Action getAction() {
		return Action.KEEP_ORIGINAL;
	}
}
