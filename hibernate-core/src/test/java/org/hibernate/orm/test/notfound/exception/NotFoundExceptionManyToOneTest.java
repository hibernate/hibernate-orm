/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.notfound.exception;

import java.io.Serializable;
import java.util.List;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import org.hibernate.FetchNotFoundException;
import org.hibernate.Hibernate;
import org.hibernate.ObjectNotFoundException;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.assertj.core.api.Assertions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests for `@ManyToOne @NotFound(EXCEPTION)`
 *
 * NOTES:<ol>
 *     <li>`@NotFound` should force the association to be eager - `Coin#currency` should be loaded immediately</li>
 *     <li>`EXCEPTION` should trigger an exception since the particular `Coin#currency` fk is broken</li>
 * </ol>
 *
 * @author Steve Ebersole
 */
@DomainModel( annotatedClasses = { NotFoundExceptionManyToOneTest.Coin.class, NotFoundExceptionManyToOneTest.Currency.class } )
@SessionFactory( useCollectingStatementInspector = true )
public class NotFoundExceptionManyToOneTest {

	@Test
	@JiraKey( "HHH-15060" )
	public void testProxyCurrency(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final Currency proxy = session.byId( Currency.class ).getReference( 1 );
			try {
				Hibernate.initialize( proxy );
				Assertions.fail( "Expecting ObjectNotFoundException" );
			}
			catch (ObjectNotFoundException expected) {
				assertThat( expected.getEntityName() ).endsWith( "Currency" );
				assertThat( expected.getIdentifier() ).isEqualTo( 1 );
			}
		} );
	}

	@Test
	@JiraKey( "HHH-15060" )
	public void testProxyCoin(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final Coin proxy = session.byId( Coin.class ).getReference( 1 );
			try {
				Hibernate.initialize( proxy );
				Assertions.fail( "Expecting ObjectNotFoundException" );
			}
			catch (FetchNotFoundException expected) {
				assertThat( expected.getEntityName() ).endsWith( "Currency" );
				assertThat( expected.getIdentifier() ).isEqualTo( 1 );
			}
		} );
	}

	@Test
	@JiraKey( "HHH-15060" )
	public void testGet(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();

		scope.inTransaction( (session) -> {
			try {
				// should fail here loading the Coin due to missing currency (see NOTE#1)
				final Coin coin = session.get( Coin.class, 1 );
				fail( "Expecting ObjectNotFoundException - " + coin.getCurrency() );
			}
			catch (FetchNotFoundException expected) {
				assertThat( statementInspector.getSqlQueries() ).hasSize( 1 );
				assertThat( statementInspector.getSqlQueries().get( 0 ) ).contains( " Coin " );
				assertThat( statementInspector.getSqlQueries().get( 0 ) ).contains( " Currency " );
				assertThat( statementInspector.getSqlQueries().get( 0 ) ).contains( " join " );
				assertThat( statementInspector.getSqlQueries().get( 0 ) ).contains( " left " );
				assertThat( statementInspector.getSqlQueries().get( 0 ) ).doesNotContain( " inner " );

				assertThat( expected.getEntityName() ).isEqualTo( Currency.class.getName() );
				assertThat( expected.getIdentifier() ).isEqualTo( 1 );
			}
		} );
	}

	/**
	 * Baseline for {@link  #testQueryImplicitPathDereferencePredicate}.  Ultimately, we want
	 * SQL generated there to behave exactly the same as this query - specifically forcing the
	 * join.  Because the
	 */
	@Test
	@JiraKey( "HHH-15060" )
	public void testQueryImplicitPathDereferencePredicateBaseline(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();

		scope.inTransaction( (session) -> {
			final String hql = "select c from Coin c where c.currency.name = 'Euro'";
			final List<Coin> coins = session.createQuery( hql, Coin.class ).getResultList();
			assertThat( coins ).isEmpty();
		} );

		// the problem, as with the rest of the failures here, is that the fetch
		// causes a left join to be used.  The where-clause path is either
		// 		1) not processed first
		//		2) not processed properly
		assertThat( statementInspector.getSqlQueries() ).hasSize( 1 );
		assertThat( statementInspector.getSqlQueries().get( 0 ) ).contains( " Coin " );
		assertThat( statementInspector.getSqlQueries().get( 0 ) ).contains( " Currency " );
		assertThat( statementInspector.getSqlQueries().get( 0 ) ).contains( " join " );
		assertThat( statementInspector.getSqlQueries().get( 0 ) ).doesNotContain( " left " );
		assertThat( statementInspector.getSqlQueries().get( 0 ) ).doesNotContain( " cross " );
	}

	/**
	 * Baseline for {@link  #testQueryImplicitPathDereferencePredicate}.  Ultimately, we want
	 * SQL generated there to behave exactly the same as this query - specifically forcing the
	 * join
	 */
	@Test
	@JiraKey( "HHH-15060" )
	public void testQueryImplicitPathDereferencePredicateBaseline2(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();

		scope.inTransaction( (session) -> {
			final String hql = "select c.id from Coin c where c.currency.id = 2";
			final List<Integer> coinIds = session.createQuery( hql, Integer.class ).getResultList();
			assertThat( coinIds ).hasSize( 1 );

			// this form works because we do not fetch the currency since
			// we select just the Coin id

			assertThat( statementInspector.getSqlQueries() ).hasSize( 1 );
			assertThat( statementInspector.getSqlQueries().get( 0 ) ).contains( " Coin " );
			assertThat( statementInspector.getSqlQueries().get( 0 ) ).contains( " Currency " );
			assertThat( statementInspector.getSqlQueries().get( 0 ) ).contains( " join " );
			assertThat( statementInspector.getSqlQueries().get( 0 ) ).doesNotContain( " left " );
			assertThat( statementInspector.getSqlQueries().get( 0 ) ).doesNotContain( " cross " );
		} );
	}

	/**
	 * Baseline for {@link  #testQueryImplicitPathDereferencePredicate}.  Ultimately, we want
	 * SQL generated there to behave exactly the same as this query
	 */
	@Test
	@JiraKey( "HHH-15060" )
	public void testQueryImplicitPathDereferencePredicateBaseline3(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();

		scope.inTransaction( (session) -> {
			final String hql = "select c from Coin c join fetch c.currency c2 where c2.name = 'USD'";
			final List<Coin> coins = session.createQuery( hql, Coin.class ).getResultList();
			assertThat( coins ).hasSize( 1 );
			assertThat( statementInspector.getSqlQueries() ).hasSize( 1 );
			assertThat( statementInspector.getSqlQueries().get( 0 ) ).contains( " Coin " );
			assertThat( statementInspector.getSqlQueries().get( 0 ) ).contains( " Currency " );
			assertThat( statementInspector.getSqlQueries().get( 0 ) ).contains( " join " );
			assertThat( statementInspector.getSqlQueries().get( 0 ) ).doesNotContain( " left " );
			assertThat( statementInspector.getSqlQueries().get( 0 ) ).doesNotContain( " cross " );
		} );

		statementInspector.clear();

		scope.inTransaction( (session) -> {
			final String hql = "select c from Coin c join fetch c.currency c2 where c2.name = 'Euro'";
			final List<Coin> coins = session.createQuery( hql, Coin.class ).getResultList();
			assertThat( coins ).hasSize( 0 );
			assertThat( statementInspector.getSqlQueries() ).hasSize( 1 );
		} );
	}

	@Test
	@JiraKey( "HHH-15060" )
	public void testQueryImplicitPathDereferencePredicate(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();

		// the problem, as with the rest of the failures here, is that the fetch
		// causes a left join to be used.  The where-clause path is either
		// 		1) not processed first
		//		2) not processed properly

		scope.inTransaction( (session) -> {
			final String hql = "select c from Coin c where c.currency.id = 1";
			final List<Coin> coins = session.createQuery( hql, Coin.class ).getResultList();
			assertThat( coins ).isEmpty();

			assertThat( statementInspector.getSqlQueries() ).hasSize( 1 );
			assertThat( statementInspector.getSqlQueries().get( 0 ) ).contains( " Coin " );
			assertThat( statementInspector.getSqlQueries().get( 0 ) ).contains( " Currency " );
			assertThat( statementInspector.getSqlQueries().get( 0 ) ).contains( " join " );
			assertThat( statementInspector.getSqlQueries().get( 0 ) ).doesNotContain( " left " );
			assertThat( statementInspector.getSqlQueries().get( 0 ) ).doesNotContain( " cross " );
		} );
	}

	@Test
	@JiraKey( "HHH-15060" )
	public void testQueryOwnerSelection(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();

		scope.inTransaction( (session) -> {
			final String hql = "select c from Coin c where c.id = 1";
			try {
				session.createQuery( hql, Coin.class ).getResultList();
				fail( "Expecting ObjectNotFoundException for broken fk" );
			}
			catch (FetchNotFoundException expected) {
				assertThat( expected.getEntityName() ).isEqualTo( Currency.class.getName() );
				assertThat( expected.getIdentifier() ).isEqualTo( 1 );
			}
		} );
	}

	@Test
	@JiraKey( "HHH-15060" )
	public void testQueryAssociationSelection(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();

		// NOTE: this one is not obvious
		//		- we are selecting the association so from that perspective, throwing the ObjectNotFoundException is nice
		//		- the other way to look at it is that there are simply no matching results, so nothing to return
		scope.inTransaction( (session) -> {
			final String hql = "select c.currency from Coin c where c.id = 1";
			final List<Currency> resultList = session.createQuery( hql, Currency.class ).getResultList();
			assertThat( resultList ).hasSize( 0 );

			assertThat( statementInspector.getSqlQueries().get( 0 ) ).contains( " join " );
			assertThat( statementInspector.getSqlQueries().get( 0 ) ).doesNotContain( " left " );
			assertThat( statementInspector.getSqlQueries().get( 0 ) ).doesNotContain( " cross " );
		} );
	}

	@BeforeEach
	public void prepareTestData(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			Currency euro = new Currency( 1, "Euro" );
			Coin fiveC = new Coin( 1, "Five cents", euro );
			session.persist( euro );
			session.persist( fiveC );

			Currency usd = new Currency( 2, "USD" );
			Coin penny = new Coin( 2, "Penny", usd );
			session.persist( usd );
			session.persist( penny );
		} );

		scope.inTransaction( (session) -> {
			session.createMutationQuery( "delete Currency where id = 1" ).executeUpdate();
		} );
	}

	@AfterEach
	public void cleanupTest(SessionFactoryScope scope) throws Exception {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Entity(name = "Coin")
	public static class Coin {
		private Integer id;
		private String name;
		private Currency currency;

		public Coin() {
		}

		public Coin(Integer id, String name, Currency currency) {
			this.id = id;
			this.name = name;
			this.currency = currency;
		}

		@Id
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		@ManyToOne(fetch = FetchType.EAGER)
		@NotFound(action = NotFoundAction.EXCEPTION)
		public Currency getCurrency() {
			return currency;
		}

		public void setCurrency(Currency currency) {
			this.currency = currency;
		}
	}

	@Entity(name = "Currency")
	public static class Currency implements Serializable {
		private Integer id;
		private String name;

		public Currency() {
		}

		public Currency(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		@Id
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

}
