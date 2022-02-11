/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.notfound.exception;

import java.io.Serializable;
import java.util.List;

import org.hibernate.FetchNotFoundException;
import org.hibernate.Hibernate;
import org.hibernate.ObjectNotFoundException;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.FailureExpected;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.persistence.Entity;
import javax.persistence.EntityNotFoundException;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToOne;
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
		// test handling of a proxy for the missing Coin
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
//				assertThat( statementInspector.getSqlQueries() ).hasSize( 1 );
//				assertThat( statementInspector.getSqlQueries().get( 0 ) ).contains( " join " );
//				assertThat( statementInspector.getSqlQueries().get( 0 ) ).doesNotContain( " inner " );
			assertThat( statementInspector.getSqlQueries() ).hasSize( 2 );
			assertThat( statementInspector.getSqlQueries().get( 0 ) ).doesNotContain( " Currency " );
			assertThat( statementInspector.getSqlQueries().get( 1 ) ).doesNotContain( " Coin " );
		} );

		scope.inTransaction( (session) -> {
			try {
				session.get( Coin.class, 1 );
				fail( "Expecting FetchNotFoundException" );
			}
			catch (FetchNotFoundException expected) {
				assertThat( expected.getEntityName() ).isEqualTo( Currency.class.getName() );
				assertThat( expected.getIdentifier() ).isEqualTo( 1 );
			}
		} );
	}

	@Test
	@JiraKey( "HHH-15060" )
	@FailureExpected(
			reason = "At the moment, throws FetchNotFoundException.  But the correct outcome is " +
					"simply no results.  This needs to trigger the join to use the fk target as part " +
					"of the predicate, not the fk value"
	)
	public void testQueryImplicitPathDereferencePredicate(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();

		scope.inTransaction( (session) -> {
			final String hql = "select c from Coin c where c.currency.id = 1";
			final List<Coin> coins = session.createQuery( hql, Coin.class ).getResultList();
			assertThat( coins ).isEmpty();

			// join may be better here.  but for now, 5.x generates 2 selects here
			// which is not wrong
//				assertThat( statementInspector.getSqlQueries() ).hasSize( 1 );
//				assertThat( statementInspector.getSqlQueries().get( 0 ) ).contains( " join " );
//				assertThat( statementInspector.getSqlQueries().get( 0 ) ).doesNotContain( " inner " );
			assertThat( statementInspector.getSqlQueries() ).hasSize( 2 );
			assertThat( statementInspector.getSqlQueries().get( 0 ) ).doesNotContain( " Currency " );
			assertThat( statementInspector.getSqlQueries().get( 1 ) ).doesNotContain( " Coin " );
		} );

		statementInspector.clear();

		scope.inTransaction( (session) -> {
			final String hql = "select c from Coin c where c.currency.id = 2";
			final List<Coin> coins = session.createQuery( hql, Coin.class ).getResultList();
			assertThat( coins ).hasSize( 1 );

			assertThat( statementInspector.getSqlQueries() ).hasSize( 1 );
			assertThat( statementInspector.getSqlQueries().get( 0 ) ).contains( " join " );
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
				fail( "Expecting FetchNotFoundException for broken fk" );
			}
			catch (FetchNotFoundException expected) {
				assertThat( expected.getEntityName() ).endsWith( "NotFoundExceptionLogicalOneToOneTest$Currency" );
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
