/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.join.namingstrategy;

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
