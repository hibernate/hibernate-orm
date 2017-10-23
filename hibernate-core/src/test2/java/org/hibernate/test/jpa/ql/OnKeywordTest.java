/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.jpa.ql;

import java.math.BigDecimal;

import org.hibernate.Session;

import org.junit.Test;

import org.hibernate.test.jpa.AbstractJPATest;

/**
 * Tests of the JPA decision (ugh) to use ON as a keyword for what Hibernate/HQL termed WITH.
 *
 * @author Steve Ebersole
 */
public class OnKeywordTest extends AbstractJPATest {
	@Test
	public void basicTest() {
		Session s = openSession();
		s.createQuery( "select i from Item i join i.parts p on p.unitPrice > :filterPrice" )
				.setParameter( "filterPrice", new BigDecimal( 100 ) )
				.list();
		s.close();
	}
}
