/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing;

import org.jboss.logging.Logger;

/**
 * Well-known-location lookup for the test-skip log...
 *
 * @author Steve Ebersole
 */
public final class SkipLog {
	private static final Logger log = Logger.getLogger( SkipLog.class );

	public static void reportSkip(String message) {
		log.info( "*** skipping test - " + message, new Exception() );
	}

	public static void reportSkip(String reason, String testDescription) {
		reportSkip( testDescription + " : " + reason  );
	}

	private SkipLog() {
	}
}
