/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.query.hql;

import java.math.BigDecimal;

import org.hibernate.Session;

import org.hibernate.orm.test.jpa.model.AbstractJPATest;
import org.junit.jupiter.api.Test;

/**
 * Tests of the JPA decision (ugh) to use ON as a keyword for what Hibernate/HQL termed WITH.
 *
 * @author Steve Ebersole
 */
public class OnKeywordTest extends AbstractJPATest {
	@Test
	public void basicTest() {
		try (Session s = sessionFactory().openSession()) {
			s.createQuery( "select i from Item i join i.parts p on p.unitPrice > :filterPrice" )
					.setParameter( "filterPrice", new BigDecimal( 100 ) )
					.list();
		}
	}
}
