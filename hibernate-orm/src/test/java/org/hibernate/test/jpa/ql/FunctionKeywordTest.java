/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.jpa.ql;

import org.hibernate.Session;

import org.junit.Test;

import org.hibernate.test.jpa.AbstractJPATest;

/**
 * Test use of the JPA 2.1 FUNCTION keyword.
 *
 * @author Steve Ebersole
 */
public class FunctionKeywordTest extends AbstractJPATest {

	@Test
	public void basicFixture() {
		Session s = openSession();
		s.createQuery( "select i from Item i where substring( i.name, 1, 3 ) = 'abc'" )
				.list();
		s.close();
	}

	@Test
	public void basicTest() {
		Session s = openSession();
		s.createQuery( "select i from Item i where function( 'substring', i.name, 1, 3 ) = 'abc'" )
				.list();
		s.close();
	}
}
