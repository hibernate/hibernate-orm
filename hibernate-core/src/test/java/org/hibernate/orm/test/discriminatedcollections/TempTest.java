/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.discriminatedcollections;

import java.util.List;

import jakarta.persistence.criteria.JoinType;

import org.hibernate.query.criteria.JpaCriteriaQuery;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * @author Gavin King
 */
@DomainModel(
		annotatedClasses = {
				Client.class, Account.class, DebitAccount.class, CreditAccount.class
		}
)
@SessionFactory
public class TempTest {

	@BeforeEach
	public void prepareTestData(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final Client c = new Client( 1, "Gavin" );
			final DebitAccount da = new DebitAccount( 10, c );
			final CreditAccount ca = new CreditAccount( 11, c );

			c.getDebitAccounts().add( da );
			c.getCreditAccounts().add( ca );

			session.persist( c );
		} );
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void findClientTest(SessionFactoryScope scope) {
		scope.inSession(
				session -> {
					final Client client = session.find( Client.class, 1 );
					assertEquals( 1, client.getDebitAccounts().size() );
					assertEquals( 1, client.getCreditAccounts().size() );
					assertNotEquals(
							client.getDebitAccounts().iterator().next().getId(),
							client.getCreditAccounts().iterator().next().getId()
					);
				}
		);
	}

	@Test
	public void hqlFromClientTest(SessionFactoryScope scope) {
		scope.inSession(
				session -> {
					Client client = session.createQuery( "from Client", Client.class ).getSingleResult();
					assertEquals( 1, client.getDebitAccounts().size() );
					assertEquals( 1, client.getCreditAccounts().size() );
					assertNotEquals(
							client.getDebitAccounts().iterator().next().getId(),
							client.getCreditAccounts().iterator().next().getId()
					);
				}
		);
	}

	@Test
	public void hqlFromClientFetchDebitTest(SessionFactoryScope scope) {
		scope.inSession(
				session -> {
					Client client = session.createQuery( "from Client c left join fetch c.debitAccounts", Client.class )
							.getSingleResult();
					assertEquals( 1, client.getDebitAccounts().size() );
					assertEquals( 1, client.getCreditAccounts().size() );
					assertNotEquals(
							client.getDebitAccounts().iterator().next().getId(),
							client.getCreditAccounts().iterator().next().getId()
					);
				}
		);
	}

	@Test
	public void hqlFromClientFetchDebitAndCreditTest(SessionFactoryScope scope) {
		scope.inSession(
				session -> {
					Client client = session.createQuery(
							"from Client c left join fetch c.debitAccounts left join fetch c.creditAccounts",
							Client.class
					).getSingleResult();
					assertEquals( 1, client.getDebitAccounts().size() );
					assertEquals( 1, client.getCreditAccounts().size() );
					assertNotEquals(
							client.getDebitAccounts().iterator().next().getId(),
							client.getCreditAccounts().iterator().next().getId()
					);
				}
		);
	}

	@Test
	public void criteriaFromClientFetchCreditTest(SessionFactoryScope scope) {
		scope.inSession(
				session -> {
					JpaCriteriaQuery<Client> query = scope.getSessionFactory()
							.getCriteriaBuilder()
							.createQuery( Client.class );
					query.from( Client.class ).fetch( Client_.creditAccounts, JoinType.LEFT );
					Client client = session.createQuery( query ).getSingleResult();
					assertEquals( 1, client.getDebitAccounts().size() );
					assertEquals( 1, client.getCreditAccounts().size() );
					assertNotEquals(
							client.getDebitAccounts().iterator().next().getId(),
							client.getCreditAccounts().iterator().next().getId()
					);
				}
		);
	}

	@Test
	public void hqlSelectAccountTest(SessionFactoryScope scope) {
		scope.inSession(
				session -> {
					List<Object[]> clients = session.createQuery( "select c, da from Client c inner join c.debitAccounts da", Object[].class)
							.getResultList();
					assertEquals( 1, clients.size() );
					assertTrue( clients.get(0)[1] instanceof DebitAccount );
					List<Object[]> accounts = session.createQuery( "select ca, da from Client c inner join c.creditAccounts ca inner join c.debitAccounts da", Object[].class)
							.getResultList();
					assertEquals( 1, accounts.size() );
					assertTrue( accounts.get(0)[0] instanceof CreditAccount );
					assertTrue( accounts.get(0)[1] instanceof DebitAccount );
				}
		);
	}
}
