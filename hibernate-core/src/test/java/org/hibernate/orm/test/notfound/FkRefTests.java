/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.notfound;

import java.io.Serializable;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;

import org.hibernate.QueryException;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.jdbc.SQLStatementInterceptor;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;


/**
 * Tests for the new `{fk}` HQL token
 *
 * @author Steve Ebersole
 */
public class FkRefTests extends BaseNonConfigCoreFunctionalTestCase {

	private SQLStatementInterceptor statementInspector;


	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[]{FkRefTests.Coin.class, FkRefTests.Currency.class};
	}

	@Override
	protected void configureSessionFactoryBuilder(SessionFactoryBuilder sfb) {
		statementInspector = new SQLStatementInterceptor( sfb );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-15160")
	public void testSimplePredicateUse() {
		statementInspector.clear();

		// there is a Coin which has a currency_fk = 1
		inTransaction( (session) -> {
			final String hql = "select c from Coin c where fk(c.currency) = 1";
			final List<Coin> coins = session.createQuery( hql, Coin.class ).getResultList();
			assertThat( coins.size(), is(1));
			assertThat( coins.get( 0 ), nullValue());
			assertThat( coins.get( 0 ).getCurrency(), nullValue());

			assertThat( statementInspector.getSqlQueries().size(), is(2));
			assertThat( statementInspector.getSqlQueries().get( 0 ), not(containsString(" join ")));
		} );

		statementInspector.clear();

		// However, the "matching" Currency does not exist
		inTransaction( (session) -> {
			final String hql = "select c from Coin c where c.currency.id = 1";
			final List<Coin> coins = session.createQuery( hql, Coin.class ).getResultList();
			assertThat( coins.size(), is(0));
		} );

		statementInspector.clear();

		// check using `currency` as a naked "property-ref"
		inTransaction( (session) -> {
			final String hql = "select c from Coin c where fk(currency) = 1";
			final List<Coin> coins = session.createQuery( hql, Coin.class ).getResultList();
			assertThat( coins.size(), is(1));
			assertThat( coins.get( 0 ), nullValue());
			assertThat( coins.get( 0 ).getCurrency(), nullValue());

			assertThat( statementInspector.getSqlQueries().size(), is(2));
			assertThat( statementInspector.getSqlQueries().get( 0 ), not(containsString(" join " )));
		} );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-15160")
	public void testNullnessPredicateUse() {
		statementInspector.clear();

		// there is one Coin (id=3) which has a null currency_fk
		inTransaction( (session) -> {
			final String hql = "select c from Coin c where fk(c.currency) is null";
			final List<Coin> coins = session.createQuery( hql, Coin.class ).getResultList();
			assertThat( coins.size(), is(1));
			assertThat( coins.get( 0 ), nullValue());
			assertThat( coins.get( 0 ).getId(), equalTo(3));
			assertThat( coins.get( 0 ).getCurrency(), nullValue());

			assertThat( statementInspector.getSqlQueries().size(), is(1));
			assertThat( statementInspector.getSqlQueries().get( 0 ), not(containsString(" join " )));
		} );

		statementInspector.clear();

		// check using `currency` as a naked "property-ref"
		inTransaction( (session) -> {
			final String hql = "select c from Coin c where fk(currency) is null";
			final List<Coin> coins = session.createQuery( hql, Coin.class ).getResultList();
			assertThat( coins.size(), is(1));
			assertThat( coins.get( 0 ), nullValue());
			assertThat( coins.get( 0 ).getId(), equalTo(3));
			assertThat( coins.get( 0 ).getCurrency(), nullValue());

			assertThat( statementInspector.getSqlQueries().size(), is(1));
			assertThat( statementInspector.getSqlQueries().get( 0 ), not(containsString(" join " )));
		} );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-15160")
	public void testFkRefDereferenceNotAllowed() {
		statementInspector.clear();

		inTransaction( (session) -> {
			try {
				final String hql = "select c from Coin c where fk(c.currency).something";
				final List<Coin> coins = session.createQuery( hql, Coin.class ).getResultList();
				fail( "Expecting failure" );
			}
			catch (IllegalArgumentException expected) {
				assertThat( expected.getCause(), instanceOf(QueryException.class));
			}
			catch (Exception e) {
				fail( "Unexpected failure type : " + e );
			}
		} );

		inTransaction( (session) -> {
			try {
				final String hql = "select c from Coin c where currency.{fk}.something";
				final List<Coin> coins = session.createQuery( hql, Coin.class ).getResultList();
			}
			catch (IllegalArgumentException expected) {
				assertThat( expected.getCause(), instanceOf(QueryException.class));
			}
		} );
	}

	@Before
	public void prepareTestData() {
		inTransaction( (session) -> {
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

		inTransaction( (session) -> {
			session.createQuery( "delete Currency where id = 1" ).executeUpdate();
		} );
	}

	@After
	public void cleanupTest() throws Exception {
		inTransaction( (session) -> {
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
