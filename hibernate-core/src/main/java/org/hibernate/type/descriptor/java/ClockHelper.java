/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.descriptor.java;

import java.time.Clock;
import java.time.Duration;

import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * Helper for determining the correct clock for precision
 */
public class ClockHelper {

	private static final Clock TICK_9 = Clock.systemDefaultZone();
	private static final Clock TICK_8 = Clock.tick( TICK_9, Duration.ofNanos( 10L ) );
	private static final Clock TICK_7 = Clock.tick( TICK_9, Duration.ofNanos( 100L ) );
	private static final Clock TICK_6 = Clock.tick( TICK_9, Duration.ofNanos( 1000L ) );
	private static final Clock TICK_5 = Clock.tick( TICK_9, Duration.ofNanos( 10000L ) );
	private static final Clock TICK_4 = Clock.tick( TICK_9, Duration.ofNanos( 100000L ) );
	private static final Clock TICK_3 = Clock.tick( TICK_9, Duration.ofNanos( 1000000L ) );
	private static final Clock TICK_2 = Clock.tick( TICK_9, Duration.ofNanos( 10000000L ) );
	private static final Clock TICK_1 = Clock.tick( TICK_9, Duration.ofNanos( 100000000L ) );
	private static final Clock TICK_0 = Clock.tick( TICK_9, Duration.ofNanos( 1000000000L ) );

	public static Clock forPrecision(Integer precision, SharedSessionContractImplementor session) {
		return forPrecision( precision, session, 9 );
	}

	public static Clock forPrecision(Integer precision, SharedSessionContractImplementor session, int maxPrecision) {
		final int resolvedPrecision;
		if ( precision == null ) {
			resolvedPrecision = session.getJdbcServices().getDialect().getDefaultTimestampPrecision();
		}
		else {
			resolvedPrecision = precision;
		}
		return forPrecision( resolvedPrecision, maxPrecision );
	}

	public static Clock forPrecision(int resolvedPrecision, int maxPrecision) {
		switch ( Math.min( resolvedPrecision, maxPrecision ) ) {
			case 0:
				return TICK_0;
			case 1:
				return TICK_1;
			case 2:
				return TICK_2;
			case 3:
				return TICK_3;
			case 4:
				return TICK_4;
			case 5:
				return TICK_5;
			case 6:
				return TICK_6;
			case 7:
				return TICK_7;
			case 8:
				return TICK_8;
			case 9:
				return TICK_9;
		}
		throw new IllegalArgumentException( "Illegal precision: " + resolvedPrecision );
	}
}
