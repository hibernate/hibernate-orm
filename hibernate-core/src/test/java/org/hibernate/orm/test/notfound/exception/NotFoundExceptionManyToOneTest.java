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

import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.jdbc.SQLStatementInterceptor;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

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
public class NotFoundExceptionManyToOneTest extends BaseNonConfigCoreFunctionalTestCase {

	private SQLStatementInterceptor statementInspector;


	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[]{Coin.class, Currency.class};
	}

	@Override
	protected void configureSessionFactoryBuilder(SessionFactoryBuilder sfb) {
		statementInspector = new SQLStatementInterceptor( sfb );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-15060")
	public void testProxy() {
		inTransaction( (session) -> {
			// the non-existent Child
			//  - this is the one valid deviation from treating the broken fk as null
			try {
				final Currency proxy = session.byId( Currency.class ).getReference( 1 );
				Hibernate.initialize( proxy );
				fail( "Expecting ObjectNotFoundException" );
			}
			catch (ObjectNotFoundException expected) {
				assertThat( expected.getEntityName(), endsWith( "Currency"  ) );
				assertThat( expected.getIdentifier(), equalTo( 1 )  );
			}
		} );
	}
	@Test
	@TestForIssue(jiraKey = "HHH-15060")
	public void testGet() {
		statementInspector.clear();

		inTransaction( (session) -> {
			try {
				// should fail here loading the Coin due to missing currency (see NOTE#1)
				final Coin coin = session.get( Coin.class, 1 );
				fail( "Expecting FetchNotFoundException for broken fk" );
			}
			catch (FetchNotFoundException expected) {
				assertThat( expected.getEntityName(), equalTo(Currency.class.getName() ));
				assertThat( expected.getIdentifier(), equalTo( 1 ));

				// atm, 5.x generates 2 selects here; which wouldn't be bad, except that
				// the first one contains a join
				//
				// what "should" happen
//				assertThat( statementInspector.getSqlQueries() ).hasSize( 1 );
//				assertThat( statementInspector.getSqlQueries().get( 0 ) ).contains( " join " );
//				assertThat( statementInspector.getSqlQueries().get( 0 ) ).doesNotContain( " inner " );
				// what actually happens
				assertThat( statementInspector.getSqlQueries().size(), is( 2 ));
				assertThat( statementInspector.getSqlQueries().get( 0 ), containsString( " Coin " ));
				assertThat( statementInspector.getSqlQueries().get( 0 ), containsString( " join " ));
				assertThat( statementInspector.getSqlQueries().get( 0 ), containsString( " Currency " ));
				assertThat( statementInspector.getSqlQueries().get( 1 ), not(containsString(" coin ")));
			}
		} );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-15060")
	public void testQueryImplicitPathDereferencePredicate() {
		statementInspector.clear();

		inTransaction( (session) -> {
			try {
				final String hql = "select c from Coin c where c.currency.id = 1";
				session.createQuery( hql, Coin.class ).getResultList();

				fail( "Expecting FetchNotFoundException for broken fk" );
			}
			catch (FetchNotFoundException expected) {
				assertThat( expected.getEntityName(), equalTo( Currency.class.getName() ));
				assertThat( expected.getIdentifier(), equalTo( 1 ));

				// join may be better here.  but for now, 5.x generates 2 selects here
				// which is not wrong
//				assertThat( statementInspector.getSqlQueries() ).hasSize( 1 );
//				assertThat( statementInspector.getSqlQueries().get( 0 ) ).contains( " join " );
//				assertThat( statementInspector.getSqlQueries().get( 0 ) ).doesNotContain( " inner " );
				assertThat( statementInspector.getSqlQueries().size(), is(2));
				assertThat( statementInspector.getSqlQueries().get( 0 ), not(containsString(" Currency " )));
				assertThat( statementInspector.getSqlQueries().get( 1 ), not(containsString(" coin " )));
			}
		} );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-15060")
	public void testQueryOwnerSelection() {
		statementInspector.clear();

		inTransaction( (session) -> {
			final String hql = "select c from Coin c where c.id = 1";
			try {
				session.createQuery( hql, Coin.class ).getResultList();
				fail( "Expecting ObjectNotFoundException for broken fk" );
			}
			catch (FetchNotFoundException expected) {
				assertThat( expected.getEntityName(), equalTo( Currency.class.getName() ));
				assertThat( expected.getIdentifier(),equalTo( 1 ));

				// join may be better here.  but for now, 5.x generates 2 selects here
				// which is not wrong
//				assertThat( statementInspector.getSqlQueries() ).hasSize( 1 );
//				assertThat( statementInspector.getSqlQueries().get( 0 ) ).contains( " join " );
//				assertThat( statementInspector.getSqlQueries().get( 0 ) ).doesNotContain( " inner " );
				assertThat( statementInspector.getSqlQueries().size(), is(2));
				assertThat( statementInspector.getSqlQueries().get( 0 ), not(containsString(" Currency " )));
				assertThat( statementInspector.getSqlQueries().get( 1 ), not(containsString(" coin " )));
			}
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
