/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.refresh;

import org.hibernate.Session;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Before;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;

/**
 * @author Andrea Boriero
 */
@TestForIssue(jiraKey = "HHH-11217")
public class RefreshUsingEntityNameTest extends BaseCoreFunctionalTestCase {
	private Customer customer;

	@Override
	public String[] getMappings() {
		return new String[] {"refresh/Customer.hbm.xml"};
	}


	@Before
	public void setUp() {
		customer = new Customer();
		doInHibernate( this::sessionFactory, session -> {
			session.save( "CustomName", customer );
		} );
	}


	@Test
	public void testRefreshUsingEntityName() {
		try (final Session session = openSession();) {
			session.refresh( "CustomName", customer );
		}
	}
}
