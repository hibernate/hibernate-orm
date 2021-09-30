/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.discriminatedcollections;

import jakarta.persistence.criteria.JoinType;

import org.hibernate.query.criteria.JpaCriteriaQuery;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;


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

	@Test
	public void test(SessionFactoryScope scope) {
		Client c = new Client( "Gavin" );
		DebitAccount da = new DebitAccount( c );
		CreditAccount ca = new CreditAccount( c );
		c.getDebitAccounts().add( da );
		c.getCreditAccounts().add( ca );

		scope.inTransaction(
				session ->
						session.persist( c )
		);

		scope.inSession(
				session -> {
					Client client = session.find( Client.class, c.getId() );
					assertEquals( 1, client.getDebitAccounts().size() );
					assertEquals( 1, client.getCreditAccounts().size() );
					assertNotEquals(
							client.getDebitAccounts().iterator().next().getId(),
							client.getCreditAccounts().iterator().next().getId()
					);
				}
		);

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
}

