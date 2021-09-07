/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.discriminatedcollections;

import org.hibernate.Session;
import org.hibernate.query.criteria.JpaCriteriaQuery;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import javax.persistence.criteria.JoinType;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * @author Gavin King
 */
public class TempTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Client.class, Account.class, DebitAccount.class, CreditAccount.class };
	}

	@Test
	public void test() {
		Client c = new Client("Gavin");
		DebitAccount da = new DebitAccount(c);
		CreditAccount ca = new CreditAccount(c);
		c.getDebitAccounts().add(da);
		c.getCreditAccounts().add(ca);
		Session session = openSession();
		session.beginTransaction();
		session.persist(c);
		session.getTransaction().commit();
		session.close();

		session = openSession();
		Client client = session.find(Client.class, c.getId());
		assertEquals(1, client.getDebitAccounts().size());
		assertEquals(1, client.getCreditAccounts().size());
		assertNotEquals(client.getDebitAccounts().iterator().next().getId(),
				client.getCreditAccounts().iterator().next().getId());
		session.close();

		session = openSession();
		client = session.createQuery("from Client", Client.class).getSingleResult();
		assertEquals(1, client.getDebitAccounts().size());
		assertEquals(1, client.getCreditAccounts().size());
		assertNotEquals(client.getDebitAccounts().iterator().next().getId(),
				client.getCreditAccounts().iterator().next().getId());
		session.close();

		session = openSession();
		client = session.createQuery("from Client c left join fetch c.debitAccounts", Client.class).getSingleResult();
		assertEquals(1, client.getDebitAccounts().size());
		assertEquals(1, client.getCreditAccounts().size());
		assertNotEquals(client.getDebitAccounts().iterator().next().getId(),
				client.getCreditAccounts().iterator().next().getId());
		session.close();

		session = openSession();
		client = session.createQuery("from Client c left join fetch c.debitAccounts left join fetch c.creditAccounts", Client.class).getSingleResult();
		assertEquals(1, client.getDebitAccounts().size());
		assertEquals(1, client.getCreditAccounts().size());
		assertNotEquals(client.getDebitAccounts().iterator().next().getId(),
				client.getCreditAccounts().iterator().next().getId());
		session.close();

		session = openSession();
		JpaCriteriaQuery<Client> query = sessionFactory().getCriteriaBuilder().createQuery(Client.class);
		query.from(Client.class).fetch(Client_.creditAccounts, JoinType.LEFT);
		client = session.createQuery(query).getSingleResult();
		assertEquals(1, client.getDebitAccounts().size());
		assertEquals(1, client.getCreditAccounts().size());
		assertNotEquals(client.getDebitAccounts().iterator().next().getId(),
				client.getCreditAccounts().iterator().next().getId());
		session.close();
	}
}

