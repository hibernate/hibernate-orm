/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.internal.util;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Vlad Mihalcea
 */
public class MathHelperTest {
	
	@Test
	public void ceilingPowerOfTwo() {
		assertEquals( 1, MathHelper.ceilingPowerOfTwo( 1 ) );
		assertEquals( 2, MathHelper.ceilingPowerOfTwo( 2 ) );
		assertEquals( 4, MathHelper.ceilingPowerOfTwo( 3 ) );
		assertEquals( 4, MathHelper.ceilingPowerOfTwo( 4 ) );
		assertEquals( 8, MathHelper.ceilingPowerOfTwo( 5 ) );
		assertEquals( 8, MathHelper.ceilingPowerOfTwo( 6 ) );
		assertEquals( 8, MathHelper.ceilingPowerOfTwo( 7 ) );
		assertEquals( 8, MathHelper.ceilingPowerOfTwo( 8 ) );
		assertEquals( 16, MathHelper.ceilingPowerOfTwo( 9 ) );
		assertEquals( 16, MathHelper.ceilingPowerOfTwo( 10 ) );
		assertEquals( 16, MathHelper.ceilingPowerOfTwo( 11 ) );
		assertEquals( 16, MathHelper.ceilingPowerOfTwo( 12 ) );
		assertEquals( 16, MathHelper.ceilingPowerOfTwo( 13 ) );
		assertEquals( 16, MathHelper.ceilingPowerOfTwo( 16 ) );
		assertEquals( 16, MathHelper.ceilingPowerOfTwo( 14 ) );
		assertEquals( 16, MathHelper.ceilingPowerOfTwo( 15 ) );
	}

}