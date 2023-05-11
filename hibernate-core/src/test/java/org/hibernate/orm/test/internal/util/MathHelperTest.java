/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.internal.util;

import org.hibernate.internal.util.MathHelper;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


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

		assertThat( MathHelper.ceilingPowerOfTwo( Integer.MAX_VALUE - 1 ) ).isEqualTo( Integer.MAX_VALUE );
		assertThat( MathHelper.ceilingPowerOfTwo( Integer.MAX_VALUE ) ).isEqualTo( Integer.MAX_VALUE );
	}

	@Test
	public void divideRoundingUp() {
		assertThat( MathHelper.divideRoundingUp( 0, 1 ) ).isEqualTo( 0 );
		assertThat( MathHelper.divideRoundingUp( 1, 1 ) ).isEqualTo( 1 );
		assertThat( MathHelper.divideRoundingUp( 2, 1 ) ).isEqualTo( 2 );
		assertThat( MathHelper.divideRoundingUp( 0, 2 ) ).isEqualTo( 0 );
		assertThat( MathHelper.divideRoundingUp( 1, 2 ) ).isEqualTo( 1 );
		assertThat( MathHelper.divideRoundingUp( 2, 2 ) ).isEqualTo( 1 );
		assertThat( MathHelper.divideRoundingUp( 3, 2 ) ).isEqualTo( 2 );
		assertThat( MathHelper.divideRoundingUp( 4, 2 ) ).isEqualTo( 2 );
		assertThat( MathHelper.divideRoundingUp( 5, 2 ) ).isEqualTo( 3 );
		assertThat( MathHelper.divideRoundingUp( 9, 3 ) ).isEqualTo( 3 );
		assertThat( MathHelper.divideRoundingUp( 10, 3 ) ).isEqualTo( 4 );
		assertThat( MathHelper.divideRoundingUp( Integer.MAX_VALUE, 2 ) ).isEqualTo( 1_073_741_824 );
	}

}