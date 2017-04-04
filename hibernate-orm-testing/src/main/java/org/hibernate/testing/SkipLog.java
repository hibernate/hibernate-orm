/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
