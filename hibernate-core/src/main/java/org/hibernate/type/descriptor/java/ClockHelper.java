/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.java;

import java.time.Clock;
import java.time.Duration;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.internal.CurrentTimestampGeneration;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Helper for determining the correct clock for precision
 */
public class ClockHelper {

	private static final Clock TICK_9 = Clock.systemDefaultZone();
	private static final Clock TICK_8 = forPrecision( TICK_9, 8, 9 );
	private static final Clock TICK_7 = forPrecision( TICK_9, 7, 9 );
	private static final Clock TICK_6 = forPrecision( TICK_9, 6, 9 );
	private static final Clock TICK_5 = forPrecision( TICK_9, 5, 9 );
	private static final Clock TICK_4 = forPrecision( TICK_9, 4, 9 );
	private static final Clock TICK_3 = forPrecision( TICK_9, 3, 9 );
	private static final Clock TICK_2 = forPrecision( TICK_9, 2, 9 );
	private static final Clock TICK_1 = forPrecision( TICK_9, 1, 9 );
	private static final Clock TICK_0 = forPrecision( TICK_9, 0, 9 );

	public static Clock forPrecision(Integer precision, SharedSessionContractImplementor session) {
		return forPrecision( precision, session, 9 );
	}

	public static Clock forPrecision(Integer precision, SharedSessionContractImplementor session, int maxPrecision) {
		final int resolvedPrecision =
				precision == null
						? session.getJdbcServices().getDialect().getDefaultTimestampPrecision()
						: precision;
		final var baseClock = (Clock)
				session.getFactory().getProperties()
						.get( CurrentTimestampGeneration.CLOCK_SETTING_NAME );
		return forPrecision( baseClock, resolvedPrecision, maxPrecision );
	}

	public static Clock forPrecision(int resolvedPrecision, int maxPrecision) {
		return forPrecision( null, resolvedPrecision, maxPrecision );
	}

	public static Clock forPrecision(@Nullable Clock baseClock, int resolvedPrecision, int maxPrecision) {
		return switch ( Math.min( resolvedPrecision, maxPrecision ) ) {
			case 0 -> baseClock == null ? TICK_0 : Clock.tick( baseClock, Duration.ofNanos( 1000000000L ) );
			case 1 -> baseClock == null ? TICK_1 : Clock.tick( baseClock, Duration.ofNanos( 100000000L ) );
			case 2 -> baseClock == null ? TICK_2 : Clock.tick( baseClock, Duration.ofNanos( 10000000L ) );
			case 3 -> baseClock == null ? TICK_3 : Clock.tick( baseClock, Duration.ofNanos( 1000000L ) );
			case 4 -> baseClock == null ? TICK_4 : Clock.tick( baseClock, Duration.ofNanos( 100000L ) );
			case 5 -> baseClock == null ? TICK_5 : Clock.tick( baseClock, Duration.ofNanos( 10000L ) );
			case 6 -> baseClock == null ? TICK_6 : Clock.tick( baseClock, Duration.ofNanos( 1000L ) );
			case 7 -> baseClock == null ? TICK_7 : Clock.tick( baseClock, Duration.ofNanos( 100L ) );
			case 8 -> baseClock == null ? TICK_8 : Clock.tick( baseClock, Duration.ofNanos( 10L ) );
			case 9 -> baseClock == null ? TICK_9 : baseClock;
			default -> throw new IllegalArgumentException( "Illegal precision: " + resolvedPrecision );
		};
	}
}
