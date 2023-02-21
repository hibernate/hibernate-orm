/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.notfound;

import java.io.Serializable;
import java.util.List;

import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.query.sqm.ParsingException;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the new `{fk}` HQL token
 *
 * @author Steve Ebersole
 */
@DomainModel( annotatedClasses = { FkRefTests.Coin.class, FkRefTests.Currency.class } )
@SessionFactory( useCollectingStatementInspector = true )
public class FkRefTests {

	@Test
	@JiraKey( "HHH-15099" )
	@JiraKey( "HHH-15106" )
	public void testSimplePredicateUse(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();

		// there is a Coin which has a currency_fk = 1
		scope.inTransaction( (session) -> {
			final String hql = "select c from Coin c where fk(c.currency) = 1";
			final List<Coin> coins = session.createQuery( hql, Coin.class ).getResultList();
			assertThat( coins ).hasSize( 1 );
			assertThat( coins.get( 0 ) ).isNotNull();
			assertThat( coins.get( 0 ).getCurrency() ).isNull();

			assertThat( statementInspector.getSqlQueries() ).hasSize( 2 );
			assertThat( statementInspector.getSqlQueries().get( 0 ) ).doesNotContain( " join " );
		} );

		statementInspector.clear();

		// However, the "matching" Currency does not exist
		scope.inTransaction( (session) -> {
			final String hql = "select c from Coin c where c.currency.id = 1";
			final List<Coin> coins = session.createQuery( hql, Coin.class ).getResultList();
			assertThat( coins ).hasSize( 0 );
		} );

		statementInspector.clear();

		// check using `currency` as a naked "property-ref"
		scope.inTransaction( (session) -> {
			final String hql = "select c from Coin c where fk(currency) = 1";
			final List<Coin> coins = session.createQuery( hql, Coin.class ).getResultList();
			assertThat( coins ).hasSize( 1 );
			assertThat( coins.get( 0 ) ).isNotNull();
			assertThat( coins.get( 0 ).getCurrency() ).isNull();

			assertThat( statementInspector.getSqlQueries() ).hasSize( 2 );
			assertThat( statementInspector.getSqlQueries().get( 0 ) ).doesNotContain( " join " );
		} );
	}

	/**
	 * Baseline test for {@link  #testNullnessPredicateUse2}.  Here we use the
	 * normal "target" reference, which for a not-found mapping should trigger
	 * a join to the association table and use the fk-target column
	 */
	@Test
	@JiraKey( "HHH-15099" )
	@JiraKey( "HHH-15106" )
	public void testNullnessPredicateUseBaseline(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();

		// there is one Coin (id=3) which has a null currency_fk, however its
		// target is missing (broken "fk").  this should return no results
		scope.inTransaction( (session) -> {
			final String hql = "select c from Coin c where c.currency.id is null";
			final List<Coin> coins = session.createQuery( hql, Coin.class ).getResultList();
			assertThat( coins ).hasSize( 0 );

			assertThat( statementInspector.getSqlQueries() ).hasSize( 1 );
			assertThat( statementInspector.getSqlQueries().get( 0 ) ).contains( " join " );
			assertThat( statementInspector.getSqlQueries().get( 0 ) ).doesNotContain( " left " );
			assertThat( statementInspector.getSqlQueries().get( 0 ) ).doesNotContain( " cross " );
		} );
	}

	@Test
	@JiraKey( "HHH-15099" )
	@JiraKey( "HHH-15106" )
	public void testNullnessPredicateUse1(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();

		// there is one Coin (id=3) which has a null currency_fk
		scope.inTransaction( (session) -> {
			final String hql = "select c.id from Coin c where fk(c.currency) is null";
			final List<Integer> coinIds = session.createQuery( hql, Integer.class ).getResultList();
			assertThat( coinIds ).hasSize( 1 );
			assertThat( coinIds.get( 0 ) ).isNotNull();
			assertThat( coinIds.get( 0 ) ).isEqualTo( 3 );

			assertThat( statementInspector.getSqlQueries() ).hasSize( 1 );
			assertThat( statementInspector.getSqlQueries().get( 0 ) ).doesNotContain( " join " );
		} );

		statementInspector.clear();

		// check using `currency` as a naked "property-ref"
		scope.inTransaction( (session) -> {
			final String hql = "select c.id from Coin c where fk(currency) is null";
			final List<Integer> coinIds = session.createQuery( hql, Integer.class ).getResultList();
			assertThat( coinIds ).hasSize( 1 );
			assertThat( coinIds.get( 0 ) ).isNotNull();

			assertThat( statementInspector.getSqlQueries() ).hasSize( 1 );
			assertThat( statementInspector.getSqlQueries().get( 0 ) ).doesNotContain( " join " );
		} );
	}

	/**
	 * It is not ideal that we render the join for these.
	 *
	 * Ideally we'd perform a subsequent-select, not sure if that is feasible as it requires
	 * understanding the overall query structure.
	 *
	 * Compare with {@link #testNullnessPredicateUse1}.  There, because we perform a scalar select,
	 * the currency does not need to be fetched.  So it works there
	 */
	@Test
	@JiraKey( "HHH-15099" )
	@JiraKey( "HHH-15106" )
	public void testNullnessPredicateUse2(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();

		// there is one Coin (id=3) which has a null currency_fk
		scope.inTransaction( (session) -> {
			final String hql = "select c from Coin c where fk(c.currency) is null";
			final List<Coin> coins = session.createQuery( hql, Coin.class ).getResultList();
			assertThat( coins ).hasSize( 1 );
			assertThat( coins.get( 0 ) ).isNotNull();
			assertThat( coins.get( 0 ).getId() ).isEqualTo( 3 );

			assertThat( statementInspector.getSqlQueries() ).hasSize( 1 );
			assertThat( statementInspector.getSqlQueries().get( 0 ) ).doesNotContain( " join " );
		} );

		statementInspector.clear();

		// check using `currency` as a naked "property-ref"
		scope.inTransaction( (session) -> {
			final String hql = "select c from Coin c where fk(currency) is null";
			final List<Coin> coins = session.createQuery( hql, Coin.class ).getResultList();
			assertThat( coins ).hasSize( 1 );
			assertThat( coins.get( 0 ) ).isNotNull();
			assertThat( coins.get( 0 ).getId() ).isEqualTo( 3 );

			assertThat( statementInspector.getSqlQueries() ).hasSize( 1 );
			assertThat( statementInspector.getSqlQueries().get( 0 ) ).doesNotContain( " join " );
		} );
	}

	@Test
	@JiraKey( "HHH-15099" )
	@JiraKey( "HHH-15106" )
	public void testFkRefDereferenceNotAllowed(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();

		scope.inTransaction( (session) -> {
			try {
				final String hql = "select c from Coin c where fk(c.currency).something";
				final List<Coin> coins = session.createQuery( hql, Coin.class ).getResultList();
			}
			catch (IllegalArgumentException expected) {
				assertThat( expected.getCause() ).isInstanceOf( ParsingException.class );
			}
		} );

		scope.inTransaction( (session) -> {
			try {
				final String hql = "select c from Coin c where fk(currency).something";
				final List<Coin> coins = session.createQuery( hql, Coin.class ).getResultList();
			}
			catch (IllegalArgumentException expected) {
				assertThat( expected.getCause() ).isInstanceOf( ParsingException.class );
			}
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

			Coin noCurrency = new Coin( 3, "N/A", null );
			session.persist( noCurrency );
		} );

		scope.inTransaction( (session) -> {
			session.createQuery( "delete Currency where id = 1" ).executeUpdate();
		} );
	}

	@AfterEach
	public void cleanupTest(SessionFactoryScope scope) throws Exception {
		scope.inTransaction( (session) -> {
			session.createMutationQuery( "delete Coin" ).executeUpdate();
			session.createMutationQuery( "delete Currency" ).executeUpdate();
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
		@NotFound(action = NotFoundAction.IGNORE)
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
