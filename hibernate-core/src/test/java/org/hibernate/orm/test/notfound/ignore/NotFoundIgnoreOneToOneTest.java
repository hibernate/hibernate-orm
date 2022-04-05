/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.notfound.ignore;

import java.io.Serializable;
import java.util.List;

import org.hibernate.FetchNotFoundException;
import org.hibernate.Hibernate;
import org.hibernate.ObjectNotFoundException;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.orm.test.notfound.exception.NotFoundExceptionManyToOneTest;
import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.jdbc.SQLStatementInterceptor;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;

import static com.sun.tools.internal.ws.wsdl.parser.Util.fail;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.Tuple;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;
import static org.hamcrest.CoreMatchers.containsString;

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
public class NotFoundIgnoreOneToOneTest extends BaseNonConfigCoreFunctionalTestCase {

	private SQLStatementInterceptor statementInspector;


	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[]{NotFoundExceptionManyToOneTest.Coin.class, NotFoundExceptionManyToOneTest.Currency.class};
	}

	@Override
	protected void configureSessionFactoryBuilder(SessionFactoryBuilder sfb) {
		statementInspector = new SQLStatementInterceptor( sfb );
	}

	@Test
	public void testProxy() {
		inTransaction( (session) -> {
			// the non-existent Child
			//  - this is the one valid deviation from treating the broken fk as null
			try {
				final NotFoundExceptionManyToOneTest.Currency proxy = session.byId( NotFoundExceptionManyToOneTest.Currency.class ).getReference( 1 );
				Hibernate.initialize( proxy );
				fail( "Expecting ObjectNotFoundException" );
			}
			catch (ObjectNotFoundException expected) {
				assertThat( expected.getEntityName(),endsWith( "Currency"  ) );
				assertThat( expected.getIdentifier(),equalTo( 1 )  );
			}
		} );
	}

	@Test
	public void testGet() {
		statementInspector.clear();

		inTransaction( (session) -> {
			try {
				final Coin coin = session.get( Coin.class, 1 );
			}
			catch (FetchNotFoundException expected) {
				// technically we could use a subsequent-select rather than a join...
				assertThat( statementInspector.getSqlQueries().size(),is(1) );
				assertThat( statementInspector.getSqlQueries().get( 0 ), containsString(" join "));
				assertThat( statementInspector.getSqlQueries().get( 0 ), not(containsString(" inner ")));
			}
		} );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-15060")
	@FailureExpected( jiraKey = "HHH-15060", message = "No results due to bad join")
	public void testQueryImplicitPathDereferencePredicate() {
		statementInspector.clear();

		inTransaction( (session) -> {
			final String hql = "select c from Coin c where c.currency.id = 1";
			final List<Coin> coins = session.createQuery( hql, Coin.class ).getResultList();
			assertThat( coins.size(), is( 1 ));
			assertThat( coins.get( 0 ).getCurrency(), nullValue()) ;

			// technically we could use a subsequent-select rather than a join...
			assertThat( statementInspector.getSqlQueries().size(),is(1) );
			assertThat( statementInspector.getSqlQueries().get( 0 ), containsString(" join "));
			assertThat( statementInspector.getSqlQueries().get( 0 ), not(containsString(" inner ")));
		} );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-15060")
	public void testQueryOwnerSelection() {
		statementInspector.clear();

		inTransaction( (session) -> {
			final String hql = "select c from Coin c";
			final List<Coin> coins = session.createQuery( hql, Coin.class ).getResultList();
			assertThat( coins.size(), is(1));
			assertThat( coins.get( 0 ).getCurrency(), nullValue());

			// at the moment this uses a subsequent-select.  on the bright side, it is at least eagerly fetched.
			assertThat( statementInspector.getSqlQueries().size(), is(2));
			assertThat( statementInspector.getSqlQueries().get( 0 ), containsString( " from Coin " ));
			assertThat( statementInspector.getSqlQueries().get( 0 ), not(containsString(" join " )));
			assertThat( statementInspector.getSqlQueries().get( 1 ), containsString( " from Currency " ));
			assertThat( statementInspector.getSqlQueries().get( 1 ), not(containsString(" join " )));
		} );
	}

	@Test
	@FailureExpected(jiraKey = "HHH-15060", message = "No results because of join" )
	public void testQueryAssociationSelection() {
		inTransaction( (session) -> {
			final String hql = "select c.id, c.currency from Coin c";
			final List<Tuple> tuples = session.createQuery( hql, Tuple.class ).getResultList();
			assertThat( tuples.size(), is( 1 ));
			final Tuple tuple = tuples.get( 0 );
			assertThat( tuple.get( 0 ), equalTo( 1 ));
			assertThat( tuple.get( 1 ), nullValue());
		} );

		inTransaction( (session) -> {
			final String hql = "select c.currency from Coin c";
			final List<Currency> currencies = session.createQuery( hql, Currency.class ).getResultList();
			assertThat( currencies.size(), is( 1 ));
			assertThat( currencies.get( 0 ), nullValue());
		} );
	}

	@Before
	public void prepareTestData() {
		inTransaction( (session) -> {
			Currency euro = new Currency( 1, "Euro" );
			Coin fiveC = new Coin( 1, "Five cents", euro );

			session.persist( euro );
			session.persist( fiveC );
		} );

		inTransaction( (session) -> {
			session.createQuery( "delete Currency where id = 1" ).executeUpdate();
		} );
	}

	@After
	public void dropTestData() {
		inTransaction( (session) -> {
			session.createQuery( "delete Coin where id = 1" ).executeUpdate();
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
