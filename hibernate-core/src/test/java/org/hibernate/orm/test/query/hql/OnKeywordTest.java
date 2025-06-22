/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
