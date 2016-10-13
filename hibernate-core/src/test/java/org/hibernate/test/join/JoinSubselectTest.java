/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.join;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.query.Query;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.notNull;

/**
 * @author Andrea Boriero
 */
@TestForIssue(jiraKey = "HHH-10998")
public class JoinSubselectTest extends BaseCoreFunctionalTestCase {

	@Override
	public String[] getMappings() {
		return new String[] {"join/Order.hbm.xml"};
	}

	@Before
	public void setUp() {
		try (Session s = openSession()) {
			Order order = new Order();
			OrderEntry orderEntry = new OrderEntry();
			orderEntry.setType( 0 );


			Order secondOrder = new Order();
			OrderEntry secondOrderEntry = new OrderEntry();
			secondOrderEntry.setType( 1 );
			session.getTransaction().begin();
			try {
				session.save( orderEntry );
				order.addFirstOrder( orderEntry );
				session.save( order );
				session.save( secondOrderEntry );
				secondOrder.addFirstOrder( secondOrderEntry );
				session.save( secondOrder );
				session.getTransaction().commit();
			}
			catch (Exception e) {
				if ( session.getTransaction().isActive() ) {
					session.getTransaction().rollback();
				}
				throw e;
			}
		}
	}

	@Test
	public void testJoin() {
		try (Session s = openSession()) {
			final Query query =
					s.createQuery( "from Order o order by o.orderId " );
			final List<Order> orders = query.list();
			final Order order = orders.get( 0 );
			final Order secondOrder = orders.get( 1 );
			assertThat( order.getFirstOrder(), not( nullValue() ) );
			assertThat( secondOrder.getFirstOrder(), is( nullValue() ) );
		}
	}
}
