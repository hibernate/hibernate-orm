/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.beanvalidation;

import org.hibernate.event.service.spi.DuplicationStrategy;

/**
 * @author Steve Ebersole
 */
public class DuplicationStrategyImpl implements DuplicationStrategy {
	public static final DuplicationStrategyImpl INSTANCE = new DuplicationStrategyImpl();

	@Override
	public boolean areMatch(Object listener, Object original) {
		return listener.getClass().equals( original.getClass() )
			&& BeanValidationEventListener.class.equals( listener.getClass() );
	}

	@Override
	public Action getAction() {
		return Action.KEEP_ORIGINAL;
	}
}
