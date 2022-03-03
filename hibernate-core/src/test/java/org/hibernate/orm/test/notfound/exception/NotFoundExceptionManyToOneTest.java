/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.notfound.exception;

import java.io.Serializable;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

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
	public void testProxy(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			// the non-existent Child
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
	@FailureExpected(
			reason = "ObjectNotFoundException is thrown but caught and null is returned - see " +
					"org.hibernate.internal.SessionImpl.IdentifierLoadAccessImpl#doLoad"
	)
	public void testGet(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();

		scope.inTransaction( (session) -> {
			try {
				// should fail here loading the Coin due to missing currency (see NOTE#1)
				session.get( Coin.class, 1 );
				fail( "Expecting ObjectNotFoundException for broken fk" );
			}
			catch (ObjectNotFoundException expected) {
				// technically we could use a subsequent-select rather than a join...
				assertThat( statementInspector.getSqlQueries() ).hasSize( 1 );
				assertThat( statementInspector.getSqlQueries().get( 0 ) ).contains( " join " );
				assertThat( statementInspector.getSqlQueries().get( 0 ) ).doesNotContain( " inner " );

				assertThat( expected.getEntityName() ).isEqualTo( Currency.class.getName() );
				assertThat( expected.getIdentifier() ).isEqualTo( 1 );
			}
		} );
	}

	@Test
	@JiraKey( "HHH-15060" )
	@FailureExpected(
			reason = "EntityNotFoundException thrown rather than ObjectNotFoundException; " +
					"ObjectNotFoundException is thrown but caught and then converted to EntityNotFoundException"
	)
	public void testQueryImplicitPathDereferencePredicate(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();

		scope.inTransaction( (session) -> {
			final String hql = "select c from Coin c where c.currency.id = 1";
			try {
				session.createQuery( hql, Coin.class ).getResultList();
				fail( "Expecting ObjectNotFoundException for broken fk" );
			}
			catch (ObjectNotFoundException expected) {
				assertThat( statementInspector.getSqlQueries() ).hasSize( 1 );
				assertThat( statementInspector.getSqlQueries().get( 0 ) ).contains( " join " );
				assertThat( statementInspector.getSqlQueries().get( 0 ) ).doesNotContain( " inner " );

				assertThat( expected.getEntityName() ).isEqualTo( Currency.class.getName() );
				assertThat( expected.getIdentifier() ).isEqualTo( 1 );
			}
		} );
	}

	@Test
	@JiraKey( "HHH-15060" )
	@FailureExpected(
			reason = "EntityNotFoundException thrown rather than ObjectNotFoundException; " +
					"ObjectNotFoundException is thrown but caught and then converted to EntityNotFoundException"
	)
	public void testQueryOwnerSelection(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();

		scope.inTransaction( (session) -> {
			final String hql = "select c from Coin c";
			try {
				session.createQuery( hql, Coin.class ).getResultList();
				fail( "Expecting ObjectNotFoundException for broken fk" );
			}
			catch (ObjectNotFoundException expected) {
				// technically we could use a subsequent-select rather than a join...
				assertThat( statementInspector.getSqlQueries() ).hasSize( 1 );
				assertThat( statementInspector.getSqlQueries().get( 0 ) ).contains( " join " );
				assertThat( statementInspector.getSqlQueries().get( 0 ) ).doesNotContain( " inner " );

				assertThat( expected.getEntityName() ).isEqualTo( Currency.class.getName() );
				assertThat( expected.getIdentifier() ).isEqualTo( 1 );
			}
		} );
	}

	@Test
	@JiraKey( "HHH-15060" )
	@FailureExpected(
			reason = "This one is somewhat debatable.  Is this selecting the association?  Or simply matching Currencies?"
	)
	public void testQueryAssociationSelection(SessionFactoryScope scope) {
		// NOTE: this one is not obvious
		//		- we are selecting the association so from that perspective, throwing the ObjectNotFoundException is nice
		//		- the other way to look at it is that there are simply no matching results, so nothing to return
		scope.inTransaction( (session) -> {
			final String hql = "select c.currency from Coin c";
			try {
				session.createQuery( hql, Currency.class ).getResultList();
				fail( "Expecting ObjectNotFoundException for broken fk" );
			}
			catch (ObjectNotFoundException expected) {
				assertThat( expected.getEntityName() ).isEqualTo( Currency.class.getName() );
				assertThat( expected.getIdentifier() ).isEqualTo( 1 );
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
		} );

		scope.inTransaction( (session) -> {
			session.createMutationQuery( "delete Currency where id = 1" ).executeUpdate();
		} );
	}

	@AfterEach
	protected void dropTestData(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			session.createMutationQuery( "delete Coin where id = 1" ).executeUpdate();
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
