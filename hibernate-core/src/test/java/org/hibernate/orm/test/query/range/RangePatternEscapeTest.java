/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.range;

import org.hibernate.query.range.Range;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The single-argument {@link Range#prefix}, {@link Range#suffix} and
 * {@link Range#containing} factories are case-sensitive shorthands for their
 * two-argument counterparts, so they must escape the wildcard characters
 * {@code %}, {@code _} and {@code \} in the supplied literal exactly the same
 * way. Verifies that {@code suffix} no longer forwards the raw literal as an
 * unescaped LIKE pattern.
 */
public class RangePatternEscapeTest {

	private static final String LITERAL = "a%b_c\\d";

	@Test
	void prefixMatchesExplicitForm() {
		assertEquals( Range.prefix( LITERAL, true ), Range.prefix( LITERAL ) );
	}

	@Test
	void suffixMatchesExplicitForm() {
		assertEquals( Range.suffix( LITERAL, true ), Range.suffix( LITERAL ) );
	}

	@Test
	void containingMatchesExplicitForm() {
		assertEquals( Range.containing( LITERAL, true ), Range.containing( LITERAL ) );
	}
}
