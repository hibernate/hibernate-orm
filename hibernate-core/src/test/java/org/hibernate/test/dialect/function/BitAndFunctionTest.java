/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.dialect.function;

import java.util.Arrays;
import java.util.List;

import org.hibernate.dialect.HSQLDialect;
import org.hibernate.dialect.MySQL5Dialect;
import org.hibernate.dialect.function.SQLFunction;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Felipe Lorenz <felipe.lorenz@uxpsystems.com>
 */
public class BitAndFunctionTest {

	@Test
	public void testMySQL5BitAndFunction() {
		//given
		MySQL5Dialect dialect = new MySQL5Dialect();
		SQLFunction func = dialect.getFunctions().get( "bitand" );

		//when
		String mySqlFunc = func.render( null, argList( "10", "p.value" ), null );

		//then
		assertEquals( "(10&p.value)", mySqlFunc );
	}

	@Test
	public void testHSQLBitAndFunction() {
		//given
		HSQLDialect dialect = new HSQLDialect();
		SQLFunction func = dialect.getFunctions().get( "bitand" );

		//when
		String mySqlFunc = func.render( null, argList( "10", "p.value" ), null );

		//then
		assertEquals( "bitand(10, p.value)", mySqlFunc );
	}

	private List argList(String... args) {
		return Arrays.asList( args );
	}
}
