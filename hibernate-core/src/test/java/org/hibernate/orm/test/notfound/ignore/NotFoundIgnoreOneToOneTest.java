/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.notfound.ignore;

import java.io.Serializable;
import java.util.List;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

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

/**
 * Tests for `@OneToOne @NotFound(IGNORE)`
 *
 * NOTES:<ol>
 *     <li>`@NotFound` should force the association to be eager - `Coin#currency` should be loaded immediately</li>
 *     <li>`IGNORE` says to treat the broken fk as null</li>
 * </ol>
 *
 * @author Steve Ebersole
 */
@DomainModel( annotatedClasses = { NotFoundIgnoreOneToOneTest.Coin.class, NotFoundIgnoreOneToOneTest.Currency.class } )
@SessionFactory( useCollectingStatementInspector = true )
public class NotFoundIgnoreOneToOneTest {

	@Test
	@JiraKey( "HHH-15060" )
	public void testProxyCoin(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			// Coin#1 has the broken fk
			final Coin proxy = session.byId( Coin.class ).getReference( 1 );
			assertThat( proxy ).isNotNull();
			Hibernate.initialize( proxy );
			assertThat( Hibernate.isInitialized( proxy ) ).isTrue();
			assertThat( proxy.getCurrency() ).isNull();
		} );
	}

	@Test
	@JiraKey( "HHH-15060" )
	public void testProxyCurrency(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			// Currency#1 does not exist
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
	public void testGet(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();

		scope.inTransaction( (session) -> {
			final Coin coin = session.get( Coin.class, 1 );
			assertThat( coin.getCurrency() ).isNull();
			assertThat( statementInspector.getSqlQueries() ).hasSize( 1 );
			assertThat( statementInspector.getSqlQueries().get( 0 ) ).contains( " Coin " );
			assertThat( statementInspector.getSqlQueries().get( 0 ) ).contains( " Currency " );
			assertThat( statementInspector.getSqlQueries().get( 0 ) ).contains( " join " );
			assertThat( statementInspector.getSqlQueries().get( 0 ) ).contains( " left " );
			assertThat( statementInspector.getSqlQueries().get( 0 ) ).doesNotContain( " inner " );
		} );

		statementInspector.clear();

		scope.inTransaction( (session) -> {
			final Coin coin = session.get( Coin.class, 1 );
			assertThat( coin.getCurrency() ).isNull();
			assertThat( statementInspector.getSqlQueries() ).hasSize( 1 );
			assertThat( statementInspector.getSqlQueries().get( 0 ) ).contains( " join " );
			assertThat( statementInspector.getSqlQueries().get( 0 ) ).doesNotContain( " inner " );
		} );
	}

	@Test
	@JiraKey( "HHH-15060" )
	public void testQueryImplicitPathDereferencePredicateBaseline(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();

		scope.inTransaction( (session) -> {
			final String hql = "select c from Coin c where c.currency.name = 'Euro'";
			final List<Coin> coins = session.createSelectionQuery( hql, Coin.class ).getResultList();
			assertThat( coins ).hasSize( 0 );
			assertThat( statementInspector.getSqlQueries() ).hasSize( 1 );
			assertThat( statementInspector.getSqlQueries().get( 0 ) ).contains( " join " );
			assertThat( statementInspector.getSqlQueries().get( 0 ) ).doesNotContain( " left " );
			assertThat( statementInspector.getSqlQueries().get( 0 ) ).doesNotContain( " cross " );
		} );
	}

	@Test
	@JiraKey( "HHH-15060" )
	public void testQueryImplicitPathDereferencePredicateBaseline2(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();

		scope.inTransaction( (session) -> {
			final String hql = "select c.id from Coin c where c.currency.id = 1";
			final List<Integer> coins = session.createSelectionQuery( hql, Integer.class ).getResultList();
			assertThat( coins ).isEmpty();

			// technically we could use a subsequent-select rather than a join...
			assertThat( statementInspector.getSqlQueries() ).hasSize( 1 );
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
			final String hql = "select c from Coin c join fetch c.currency c2 where c2.id = 1";
			final List<Coin> coins = session.createQuery( hql, Coin.class ).getResultList();
			assertThat( coins ).hasSize( 0 );
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
	public void testQueryImplicitPathDereferencePredicate(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();

		scope.inTransaction( (session) -> {
			final String hql = "select c from Coin c where c.currency.id = 1";
			final List<Coin> coins = session.createQuery( hql, Coin.class ).getResultList();
			assertThat( coins ).isEmpty();

			assertThat( statementInspector.getSqlQueries() ).hasSize( 1 );
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
			final Coin coin = session.createQuery( hql, Coin.class ).uniqueResult();
			assertThat( coin ).isNotNull();
			assertThat( Hibernate.isPropertyInitialized( coin, "currency" ) ).isTrue();
			assertThat( Hibernate.isInitialized( coin.getCurrency() ) ).isTrue();
			assertThat( coin.getCurrency() ).isNull();
		} );

		scope.inTransaction( (session) -> {
			final String hql = "select c from Coin c where c.id = 2";
			final Coin coin = session.createQuery( hql, Coin.class ).uniqueResult();
			assertThat( Hibernate.isPropertyInitialized( coin, "currency" ) ).isTrue();
			assertThat( Hibernate.isInitialized( coin.getCurrency() ) ).isTrue();
		} );
	}

	@Test
	@JiraKey( "HHH-15060" )
	public void testQueryAssociationSelection(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();

		scope.inTransaction( (session) -> {
			final String hql = "select c.currency from Coin c where c.id = 1";
			final List<Currency> currencies = session.createSelectionQuery( hql, Currency.class ).getResultList();
			assertThat( currencies ).hasSize( 0 );

			assertThat( statementInspector.getSqlQueries() ).hasSize( 1 );
			assertThat( statementInspector.getSqlQueries().get( 0 ) ).contains( " Coin " );
			assertThat( statementInspector.getSqlQueries().get( 0 ) ).contains( " Currency " );
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
	public void dropTestData(SessionFactoryScope scope) {
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

		@OneToOne(fetch = FetchType.EAGER)
		@NotFound(action = NotFoundAction.IGNORE)
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
