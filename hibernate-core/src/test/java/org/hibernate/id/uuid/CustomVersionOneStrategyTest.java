/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USABasicFormatterTes
 */
package org.hibernate.id.uuid;

import java.util.UUID;

import org.junit.Test;

import org.hibernate.testing.junit4.BaseUnitTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * @author Steve Ebersole
 */
public class CustomVersionOneStrategyTest extends BaseUnitTestCase {
	@Test
	public void testUniqueCounter() {
		CustomVersionOneStrategy strategy = new CustomVersionOneStrategy();
		long now = System.currentTimeMillis();
		UUID uuid1 = new UUID(
				strategy.getMostSignificantBits(),
				CustomVersionOneStrategy.generateLeastSignificantBits( now )
		);
		assertEquals( 2, uuid1.variant() );
		assertEquals( 1, uuid1.version() );

		for ( int i = 0; i < 100; i++ ) {
			UUID uuidX = new UUID(
					strategy.getMostSignificantBits(),
					CustomVersionOneStrategy.generateLeastSignificantBits( now )
			);
			assertEquals( 2, uuidX.variant() );
			assertEquals( 1, uuidX.version() );
			assertFalse( uuid1.equals( uuidX ) );
			assertEquals( uuid1.getMostSignificantBits(), uuidX.getMostSignificantBits() );
		}
	}

	@Test
	public void testRangeOfValues() {
		CustomVersionOneStrategy strategy = new CustomVersionOneStrategy();

		UUID uuid = new UUID(
				strategy.getMostSignificantBits(),
				CustomVersionOneStrategy.generateLeastSignificantBits( 0 )
		);
		assertEquals( 2, uuid.variant() );
		assertEquals( 1, uuid.version() );

		uuid = new UUID(
				strategy.getMostSignificantBits(),
				CustomVersionOneStrategy.generateLeastSignificantBits( Long.MAX_VALUE )
		);
		assertEquals( 2, uuid.variant() );
		assertEquals( 1, uuid.version() );
	}
}
