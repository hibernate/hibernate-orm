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
import org.hibernate.orm.test.notfound.exception.NotFoundExceptionLogicalOneToOneTest;

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
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Tuple;
import org.assertj.core.api.Assertions;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for `@ManyToOne @NotFound(IGNORE)`
 *
 * NOTES:<ol>
 *     <li>`@NotFound` should force the association to be eager - `Coin#currency` should be loaded immediately</li>
 *     <li>`IGNORE` says to treat the broken fk as null</li>
 * </ol>
 *
 * @author Steve Ebersole
 */
@DomainModel( annotatedClasses = { NotFoundIgnoreManyToOneTest.Coin.class, NotFoundIgnoreManyToOneTest.Currency.class } )
@SessionFactory( useCollectingStatementInspector = true )
public class NotFoundIgnoreManyToOneTest {

	@Test
	@JiraKey( "HHH-15060" )
	public void testProxy(SessionFactoryScope scope) {
		// test handling of a proxy for the Coin pointing to the missing Currency
		scope.inTransaction( (session) -> {
			final Coin proxy = session.byId( Coin.class ).getReference( 1 );
			Hibernate.initialize( proxy );
			assertThat( proxy.getCurrency() ).isNull();
		} );

		scope.inTransaction( (session) -> {
			// the non-existent Currency
			// 	- this is the one valid deviation from treating the broken fk as null
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

			// atm, 5.x generates 2 selects here; which wouldn't be bad, except that
			// the first one contains a join
			//
			// what "should" happen
//				assertThat( statementInspector.getSqlQueries() ).hasSize( 1 );
//				assertThat( statementInspector.getSqlQueries().get( 0 ) ).contains( " join " );
//				assertThat( statementInspector.getSqlQueries().get( 0 ) ).doesNotContain( " inner " );
			// what actually happens
			assertThat( statementInspector.getSqlQueries() ).hasSize( 2 );
			assertThat( statementInspector.getSqlQueries().get( 0 ) ).contains( " Coin " );
			assertThat( statementInspector.getSqlQueries().get( 0 ) ).contains( " join " );
			assertThat( statementInspector.getSqlQueries().get( 0 ) ).contains( " Currency " );
			assertThat( statementInspector.getSqlQueries().get( 1 ) ).doesNotContain( " Coin " );
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
			// there is no Currency with id=1 (Euro)
			assertThat( coins ).isEmpty();

			assertThat( statementInspector.getSqlQueries() ).hasSize( 1 );
			assertThat( statementInspector.getSqlQueries().get( 0 ) ).contains( " join " );
			assertThat( statementInspector.getSqlQueries().get( 0 ) ).contains( " cross " );
			assertThat( statementInspector.getSqlQueries().get( 0 ) ).doesNotContain( " inner " );
		} );
	}

	@Test
	@JiraKey( "HHH-15060" )
	public void testQueryOwnerSelection(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();

		scope.inTransaction( (session) -> {
			final String hql = "select c from Coin c";
			final List<Coin> coins = session.createQuery( hql, Coin.class ).getResultList();
			assertThat( coins ).hasSize( 1 );
			assertThat( coins.get( 0 ).getCurrency() ).isNull();

			// at the moment this uses a subsequent-select.  on the bright side, it is at least eagerly fetched.
			assertThat( statementInspector.getSqlQueries() ).hasSize( 2 );
			assertThat( statementInspector.getSqlQueries().get( 0 ) ).contains( " Coin " );
			assertThat( statementInspector.getSqlQueries().get( 0 ) ).doesNotContain( " Currency " );
			assertThat( statementInspector.getSqlQueries().get( 0 ) ).doesNotContain( " join " );
			assertThat( statementInspector.getSqlQueries().get( 1 ) ).doesNotContain( " Coin " );
			assertThat( statementInspector.getSqlQueries().get( 1 ) ).contains( " Currency " );
			assertThat( statementInspector.getSqlQueries().get( 1 ) ).doesNotContain( " join " );
		} );
	}

	@Test
	@JiraKey( "HHH-15060" )
	@FailureExpected(
			reason = "Has zero results because of inner-join due to being defined in the select-clause. " +
					"Not sure the best outcome here - no results or null elements within the results?"
	)
	public void testQueryAssociationSelection(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();

		// I guess this one is somewhat debatable, but for consistency I think this makes the most sense
		scope.inTransaction( (session) -> {
			final String hql = "select c.currency from Coin c";
			session.createQuery( hql, Currency.class ).getResultList();
			final List<Currency> currencies = session.createQuery( hql, Currency.class ).getResultList();
			assertThat( currencies ).hasSize( 1 );
			assertThat( currencies.get( 0 ) ).isNull();

			assertThat( statementInspector.getSqlQueries() ).hasSize( 1 );
			assertThat( statementInspector.getSqlQueries().get( 0 ) ).contains( " join " );
			assertThat( statementInspector.getSqlQueries().get( 0 ) ).doesNotContain( " inner " );
		} );
	}

	@Test
	@JiraKey( "HHH-15060" )
	public void testSubqueryUse(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();

		scope.inTransaction( (session) -> {
			final String hql = "select c " +
					"from Coin c " +
					"where exists (" +
					"	select 1" +
					"	from Coin other" +
					")";
		} );
	}

	@Test
	@JiraKey( "HHH-15060" )
	@FailureExpected(
			reason = "Has zero results because of inner-join due to being defined in the select-clause. " +
					"Not sure the best outcome here - no results or null elements within the results?"
	)
	public void testQueryAssociationSelection2(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();

		scope.inTransaction( (session) -> {
			final String hql = "select c.id, c.currency from Coin c";
			final List<Tuple> tuples = session.createQuery( hql, Tuple.class ).getResultList();
			assertThat( tuples ).hasSize( 1 );
			final Tuple tuple = tuples.get( 0 );
			assertThat( tuple.get( 0 ) ).isEqualTo( 1 );
			assertThat( tuple.get( 1 ) ).isNull();

			assertThat( statementInspector.getSqlQueries() ).hasSize( 1 );
			assertThat( statementInspector.getSqlQueries().get( 0 ) ).contains( " join " );
			assertThat( statementInspector.getSqlQueries().get( 0 ) ).doesNotContain( " inner " );
		} );
	}

	@BeforeEach
	protected void prepareTestData(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			Currency euro = new Currency( 1, "Euro" );
			Coin fiveC = new Coin( 1, "Five cents", euro );

			session.persist( euro );
			session.persist( fiveC );
		} );

		scope.inTransaction( (session) -> {
			session.createQuery( "delete Currency where id = 1" ).executeUpdate();
		} );
	}

	@AfterEach
	protected void dropTestData(SessionFactoryScope scope) throws Exception {
		scope.inTransaction( (session) -> {
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

		@ManyToOne(fetch = FetchType.EAGER)
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
