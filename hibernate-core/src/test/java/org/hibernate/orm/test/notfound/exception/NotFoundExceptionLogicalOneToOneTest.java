/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.notfound.exception;

import java.io.Serializable;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;

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
 * Tests for `@OneToOne @NotFound(EXCEPTION)`
 *
 * NOTES:<ol>
 *     <li>`@NotFound` should force the association to be eager - `Coin#currency` should be loaded immediately</li>
 *     <li>When loading the `Coin#currency`, `EXCEPTION` should trigger an exception since the particular `Coin#currency` fk is broken</li>
 * </ol>
 *
 * @author Steve Ebersole
 */
@DomainModel( annotatedClasses = { NotFoundExceptionLogicalOneToOneTest.Coin.class, NotFoundExceptionLogicalOneToOneTest.Currency.class } )
@SessionFactory( useCollectingStatementInspector = true )
public class NotFoundExceptionLogicalOneToOneTest {
	@Test
	@JiraKey( "HHH-15060" )
	public void testProxy(SessionFactoryScope scope) {
		// test handling of a proxy for the Coin pointing to the missing Currency
		scope.inTransaction( (session) -> {
			final Coin proxy = session.byId( Coin.class ).getReference( 1 );
			try {
				Hibernate.initialize( proxy );
				Assertions.fail( "Expecting FetchNotFoundException" );
			}
			catch (ObjectNotFoundException expected) {
				assertThat( expected.getEntityName() ).endsWith( "Currency" );
				assertThat( expected.getIdentifier() ).isEqualTo( 1 );
			}
		} );

		// test handling of a proxy for the missing Currency
		scope.inTransaction( (session) -> {
			final Currency proxy = session.byId( Currency.class ).getReference( 1 );
			try {
				Hibernate.initialize( proxy );
				Assertions.fail( "Expecting FetchNotFoundException" );
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
			final Coin coin = session.get( Coin.class, 2 );

			// most importantly, the currency should not be uninitialized
			assertThat( Hibernate.isPropertyInitialized( coin, "currency" ) )
					.describedAs( "Expecting `Coin#currency` to be eagerly fetched (bytecode) due to `@NotFound`" )
					.isTrue();
			assertThat( Hibernate.isInitialized( coin.getCurrency() ) )
					.describedAs( "Expecting `Coin#currency` to be eagerly fetched due to `@NotFound`" )
					.isTrue();

			// join may be better here.  but for now, 5.x generates 2 selects here
			// which is not wrong
			assertThat( statementInspector.getSqlQueries() ).hasSize( 2 );
			assertThat( statementInspector.getSqlQueries().get( 0 ) ).doesNotContain( " Currency " );
			assertThat( statementInspector.getSqlQueries().get( 1 ) ).doesNotContain( " Coin " );
		} );

		statementInspector.clear();

		scope.inTransaction( (session) -> {
			try {
				session.get( Coin.class, 1 );
				fail( "Expecting ObjectNotFoundException" );
			}
			catch (FetchNotFoundException expected) {
				assertThat( expected.getEntityName() ).isEqualTo( Currency.class.getName() );
				assertThat( expected.getIdentifier() ).isEqualTo( 1 );
				assertThat( statementInspector.getSqlQueries() ).hasSize( 2 );
				assertThat( statementInspector.getSqlQueries().get( 0 ) ).doesNotContain( " Currency " );
				assertThat( statementInspector.getSqlQueries().get( 1 ) ).doesNotContain( " Coin " );
			}
		} );
	}

	@Test
	@JiraKey( "HHH-15060" )
	public void testQueryImplicitPathDereferencePredicateBaseline(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();

		// Baseline for comparison with `#testQueryImplicitPathDereferencePredicate`
		// We ultimately want the `.id` reference to behave exactly the same as
		// this query - specifically forcing the join
		scope.inTransaction( (session) -> {
			final String hql = "select c from Coin c where c.currency.name = 'Euro'";
			final List<Coin> coins = session.createQuery( hql, Coin.class ).getResultList();
			assertThat( coins ).isEmpty();
		} );

		assertThat( statementInspector.getSqlQueries() ).hasSize( 1 );
		assertThat( statementInspector.getSqlQueries().get( 0 ) ).contains( " join " );
		// unfortunately, versions of Hibernate prior to 6 used restricted cross joins
		// (i.e. `x cross join y where x.y_fk = y.id`) to handle implicit query joins
		assertThat( statementInspector.getSqlQueries().get( 0 ) ).contains( " cross " );
		assertThat( statementInspector.getSqlQueries().get( 0 ) ).doesNotContain( " inner " );
	}

	@Test
	@JiraKey( "HHH-15060" )
	public void testQueryImplicitPathDereferencePredicateBaseline2(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();

		scope.inTransaction( (session) -> {
			// NOTE : this query is conceptually the same as the one from
			// `#testQueryImplicitPathDereferencePredicateBaseline` in that we want
			// a join and we want to use the fk target column (here, `Currency.id`)
			// rather than the normal perf-opt strategy of using the fk key column
			// (here, `Coin.currency_fk`).
			final String hql = "select c from Coin c where c.currency.id = 2";
			final List<Coin> coins = session.createQuery( hql, Coin.class ).getResultList();
			assertThat( coins ).hasSize( 1 );

			assertThat( statementInspector.getSqlQueries() ).hasSize( 1 );
			assertThat( statementInspector.getSqlQueries().get( 0 ) ).contains( " join " );
			// unfortunately, versions of Hibernate prior to 6 used restricted cross joins
			// (i.e. `x cross join y where x.y_fk = y.id`) to handle implicit query joins
			assertThat( statementInspector.getSqlQueries().get( 0 ) ).contains( " cross " );
			assertThat( statementInspector.getSqlQueries().get( 0 ) ).doesNotContain( " inner " );
		} );
	}

	@Test
	@JiraKey( "HHH-15060" )
	public void testQueryImplicitPathDereferencePredicateBaseline3(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();

		scope.inTransaction( (session) -> {
			// NOTE : this query is conceptually the same as the one from
			// `#testQueryImplicitPathDereferencePredicateBaseline` in that we want
			// a join and we want to use the fk target column (here, `Currency.id`)
			// rather than the normal perf-opt strategy of using the fk key column
			// (here, `Coin.currency_fk`).
			final String hql = "select c from Coin c join fetch c.currency c2 where c2.name = 'USD'";
			final List<Coin> coins = session.createQuery( hql, Coin.class ).getResultList();
			assertThat( coins ).hasSize( 1 );
			assertThat( statementInspector.getSqlQueries() ).hasSize( 1 );
		} );

		statementInspector.clear();

		scope.inTransaction( (session) -> {
			// NOTE : this query is conceptually the same as the one from
			// `#testQueryImplicitPathDereferencePredicateBaseline` in that we want
			// a join and we want to use the fk target column (here, `Currency.id`)
			// rather than the normal perf-opt strategy of using the fk key column
			// (here, `Coin.currency_fk`).
			final String hql = "select c from Coin c join fetch c.currency c2 where c2.name = 'Euro'";
			final List<Coin> coins = session.createQuery( hql, Coin.class ).getResultList();
			assertThat( coins ).hasSize( 0 );
			assertThat( statementInspector.getSqlQueries() ).hasSize( 1 );
		} );
	}

	@Test
	@JiraKey( "HHH-15060" )
//	@FailureExpected(
//			reason = "When we have a dangling key (as in the `c.currency.id = 1` case), the outcome " +
//					"ought to simply be no results. At the moment, however, FetchNotFoundException is " +
//					"thrown. The underlying problem is that we use the FK key rather than the FK " +
//					"target for selecting the association" +
//					"" +
//					"  But the correct outcome is " +
//					"simply no results.  This needs to trigger the join to use the fk target as part " +
//					"of the predicate, not the fk value"
//	)
	public void testQueryImplicitPathDereferencePredicate(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();

		scope.inTransaction( (session) -> {
			final String hql = "select c from Coin c where c.currency.id = 1";
			final List<Coin> coins = session.createQuery( hql, Coin.class ).getResultList();
			// there is no Currency with id=1 (Euro)
			assertThat( coins ).isEmpty();

			assertThat( statementInspector.getSqlQueries() ).hasSize( 1 );
			assertThat( statementInspector.getSqlQueries().get( 0 ) ).contains( " join " );
			// unfortunately, versions of Hibernate prior to 6 used restricted cross joins
			// (i.e. `x cross join y where x.y_fk = y.id`) to handle implicit query joins
			assertThat( statementInspector.getSqlQueries().get( 0 ) ).contains( " cross " );
			assertThat( statementInspector.getSqlQueries().get( 0 ) ).doesNotContain( " inner " );
		} );
	}

	@Test
	@JiraKey( "HHH-15060" )
	public void testQueryOwnerSelection(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final String hql = "select c from Coin c where c.id = 1";
			try {
				//noinspection unused (debugging)
				final Coin coin = session.createQuery( hql, Coin.class ).uniqueResult();
				fail( "Expecting FetchNotFoundException" );
			}
			catch (FetchNotFoundException expected) {
				assertThat( expected.getEntityName() ).endsWith( "Currency" );
				assertThat( expected.getIdentifier() ).isEqualTo( 1 );
			}
		} );

		scope.inTransaction( (session) -> {
			final String hql = "select c from Coin c where c.id = 2";
			final Coin coin = session.createQuery( hql, Coin.class ).uniqueResult();
			assertThat( Hibernate.isPropertyInitialized( coin, "currency" ) ).isTrue();
			assertThat( Hibernate.isInitialized( coin.getCurrency() ) ).isTrue();
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
			session.createQuery( "delete Currency where id = 1" ).executeUpdate();
		} );
	}

	@AfterEach
	public void cleanupTest(SessionFactoryScope scope) throws Exception {
		scope.inTransaction( (session) -> {
			session.createQuery( "delete Coin" ).executeUpdate();
			session.createQuery( "delete Currency" ).executeUpdate();
		} );
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

		@OneToOne(fetch = FetchType.LAZY)
		@NotFound(action = NotFoundAction.EXCEPTION)
		@JoinColumn( name = "currency_fk" )
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
