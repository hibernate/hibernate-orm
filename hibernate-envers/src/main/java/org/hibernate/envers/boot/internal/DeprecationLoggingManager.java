/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.boot.internal;

import org.hibernate.internal.log.DeprecationLogger;

/**
 * Let's make sure we are only logging this once, rather than spamming.
 *
 * @author Steve Ebersole
 */
public class DeprecationLoggingManager {
	private static boolean logged = false;

	public static void logDeprecation() {
		if ( !logged ) {
			DeprecationLogger.DEPRECATION_LOGGER.envers();
			logged = true;
		}
	}

}
