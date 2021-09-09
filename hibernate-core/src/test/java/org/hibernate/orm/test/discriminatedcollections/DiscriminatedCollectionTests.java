/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.discriminatedcollections;

import java.util.List;
import javax.persistence.criteria.JoinType;

import org.hibernate.query.criteria.JpaCriteriaQuery;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;


/**
 * @author Gavin King
 */
@DomainModel(
		annotatedClasses = { Client.class, Account.class, DebitAccount.class, CreditAccount.class }
)
@SessionFactory
public class DiscriminatedCollectionTests {

	@BeforeEach
	public void prepareTestData(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final Client c = new Client( 1, "Gavin" );
			final DebitAccount da = new DebitAccount( 1, c );
			final CreditAccount ca = new CreditAccount( 2, c );
			c.getDebitAccounts().add( da );
			c.getCreditAccounts().add( ca );

			session.persist( c );
		} );
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			session.remove( session.load( Client.class, 1 ) );
		} );
	}

	@Test
	public void testHqlNonFetchJoins(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final String queryString = "select c, da from Client c inner join c.debitAccounts da";
			final List<Object[]> clients = session.createQuery( queryString, Object[].class ).getResultList();
			assertThat( clients ).hasSize( 1 );
		} );
	}

	@Test
	public void test(SessionFactoryScope scope) {
		scope.inSession(
				session -> {
					Client client = session.find( Client.class, 1 );
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

