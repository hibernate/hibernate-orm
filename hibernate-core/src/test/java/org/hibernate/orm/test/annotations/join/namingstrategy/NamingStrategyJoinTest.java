/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.join.namingstrategy;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

/**
 * @author Sergey Vasilyev
 */
public class NamingStrategyJoinTest extends BaseCoreFunctionalTestCase {

	@Test
	public void testJoinToSecondaryTable() throws Exception {
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		Life life = new Life();
		life.duration = 15;
		life.fullDescription = "Long long description";
		s.persist( life );
		tx.commit();
		s.close();

	}

	@Override
	public void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setPhysicalNamingStrategy( new TestNamingStrategy() );
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				Life.class,
				SimpleCat.class
		};
	}

}
