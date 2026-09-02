/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate;

import jakarta.persistence.Timeout;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// Tests typed timeout conversion used by query execution and locking support.
///
/// @author Steve Ebersole
public class TimeoutsTest {
	@Test
	void typedTimeoutConversionRetainsWholeSecondRules() {
		assertThat( Timeouts.getTimeoutInSeconds( Timeout.milliseconds( 0 ) ) ).isZero();
		assertThat( Timeouts.getTimeoutInSeconds( Timeout.milliseconds( 1 ) ) ).isEqualTo( 1 );
		assertThat( Timeouts.getTimeoutInSeconds( Timeout.milliseconds( 499 ) ) ).isEqualTo( 1 );
		assertThat( Timeouts.getTimeoutInSeconds( Timeout.milliseconds( 1_499 ) ) ).isEqualTo( 1 );
		assertThat( Timeouts.getTimeoutInSeconds( Timeout.milliseconds( 1_500 ) ) ).isEqualTo( 2 );
	}

	@Test
	void magicTimeoutsRemainDistinctFromRealTimeouts() {
		assertThat( Timeouts.interpretMilliSeconds( Timeouts.NO_WAIT_MILLI ) ).isSameAs( Timeouts.NO_WAIT );
		assertThat( Timeouts.interpretMilliSeconds( Timeouts.SKIP_LOCKED_MILLI ) ).isSameAs( Timeouts.SKIP_LOCKED );
		assertThat( Timeouts.interpretMilliSeconds( Timeouts.WAIT_FOREVER_MILLI ) ).isSameAs( Timeouts.WAIT_FOREVER );
		assertThat( Timeouts.isRealTimeout( Timeouts.NO_WAIT ) ).isFalse();
		assertThat( Timeouts.isRealTimeout( Timeouts.SKIP_LOCKED ) ).isFalse();
		assertThat( Timeouts.isRealTimeout( Timeouts.WAIT_FOREVER ) ).isFalse();
	}
}
