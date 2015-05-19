/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
