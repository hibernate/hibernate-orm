/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
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
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.jpa.test.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.hibernate.jpa.internal.util.PessimisticNumberParser;
import org.junit.Test;

/**
 * @author Sanne Grinovero
 */
public class PessimisticNumberParserUnitTest {

	@Test
	public void testEmptyStringBehaviour() {
		assertNull( PessimisticNumberParser.toNumberOrNull( "" ) );
	}

	@Test
	public void testPlusStringBehaviour() {
		assertNull( PessimisticNumberParser.toNumberOrNull( "+" ) );
	}

	@Test
	public void testMinusStringBehaviour() {
		assertNull( PessimisticNumberParser.toNumberOrNull( "-" ) );
	}

	@Test
	public void testLetterStringBehaviour() {
		assertNull( PessimisticNumberParser.toNumberOrNull( "h" ) );
	}

	@Test
	public void testTextStringBehaviour() {
		assertNull( PessimisticNumberParser.toNumberOrNull( "hello world!" ) );
	}

	@Test
	public void testFoolingPrefixStringBehaviour() {
		assertNull( PessimisticNumberParser.toNumberOrNull( "+60000g" ) );
	}

	@Test
	public void testFiveStringBehaviour() {
		assertEquals( Integer.valueOf( 5 ), PessimisticNumberParser.toNumberOrNull( "5" ) );
	}

	@Test
	public void testNegativeStringBehaviour() {
		//technically illegal for the case of positional parameters, but we can parse it
		assertEquals( Integer.valueOf( -25 ), PessimisticNumberParser.toNumberOrNull( "-25" ) );
	}

	@Test
	public void testBigintegerStringBehaviour() {
		assertEquals( Integer.valueOf( 60000 ), PessimisticNumberParser.toNumberOrNull( "60000" ) );
	}

	@Test
	public void testPositiveStringBehaviour() {
		assertEquals( Integer.valueOf( 60000 ), PessimisticNumberParser.toNumberOrNull( "+60000" ) );
	}

}
