/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine;

import jakarta.persistence.FetchType;

/**
 * Enumeration of values describing <em>when</em> fetching should occur.
 *
 * @author Steve Ebersole
 * @see FetchStyle
 */
public enum FetchTiming {
	/**
	 * Perform fetching immediately.  Also called eager fetching
	 */
	IMMEDIATE,
	/**
	 * Performing fetching later, when needed.  Also called lazy fetching.
	 */
	DELAYED;

	public static FetchTiming forType(FetchType type) {
		return switch (type) {
			case EAGER -> IMMEDIATE;
			case LAZY -> DELAYED;
		};
	}
}
