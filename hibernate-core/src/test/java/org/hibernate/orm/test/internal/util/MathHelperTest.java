/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.internal.util;

import org.hibernate.internal.util.MathHelper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.stream.Stream;


/**
 * @author Vlad Mihalcea
 */
public class MathHelperTest {

	@Test
	public void ceilingPowerOfTwo() {
		assertThat( MathHelper.ceilingPowerOfTwo( 0 ) ).isEqualTo( 1 );
		assertThat( MathHelper.ceilingPowerOfTwo( 1 ) ).isEqualTo( 1 );
		assertThat( MathHelper.ceilingPowerOfTwo( 2 ) ).isEqualTo( 2 );
		assertThat( MathHelper.ceilingPowerOfTwo( 3 ) ).isEqualTo( 4 );
		assertThat( MathHelper.ceilingPowerOfTwo( 4 ) ).isEqualTo( 4 );
		assertThat( MathHelper.ceilingPowerOfTwo( 5 ) ).isEqualTo( 8 );
		assertThat( MathHelper.ceilingPowerOfTwo( 6 ) ).isEqualTo( 8 );
		assertThat( MathHelper.ceilingPowerOfTwo( 7 ) ).isEqualTo( 8 );
		assertThat( MathHelper.ceilingPowerOfTwo( 8 ) ).isEqualTo( 8 );

		assertThat( MathHelper.ceilingPowerOfTwo( 9 ) ).isEqualTo( 16 );
		assertThat( MathHelper.ceilingPowerOfTwo( 10 ) ).isEqualTo( 16 );
		assertThat( MathHelper.ceilingPowerOfTwo( 11 ) ).isEqualTo( 16 );
		assertThat( MathHelper.ceilingPowerOfTwo( 12 ) ).isEqualTo( 16 );
		assertThat( MathHelper.ceilingPowerOfTwo( 13 ) ).isEqualTo( 16 );
		assertThat( MathHelper.ceilingPowerOfTwo( 14 ) ).isEqualTo( 16 );
		assertThat( MathHelper.ceilingPowerOfTwo( 15 ) ).isEqualTo( 16 );
		assertThat( MathHelper.ceilingPowerOfTwo( 16 ) ).isEqualTo( 16 );
	}

	static Stream<Arguments> test_divideRoundingUp() {
		return Stream.of(
				arguments( 0, 1, 0 ),
				arguments( 1, 1, 1 ),
				arguments( 2, 1, 2 ),
				arguments( 0, 2, 0 ),
				arguments( 1, 2, 1 ),
				arguments( 2, 2, 1 ),
				arguments( 3, 2, 2 ),
				arguments( 4, 2, 2 ),
				arguments( 5, 2, 3 ),
				arguments( 9, 3, 3 ),
				arguments( 10, 3, 4 ) );
	}

	@ParameterizedTest
	@MethodSource
	public void test_divideRoundingUp(int numerator, int denominator, int expected) {
		assertThat( MathHelper.divideRoundingUp( numerator, denominator ) ).isEqualTo( expected );
	}
}
