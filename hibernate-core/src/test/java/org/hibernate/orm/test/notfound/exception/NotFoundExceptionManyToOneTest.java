/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.notfound.exception;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import org.hibernate.Hibernate;
import org.hibernate.ObjectNotFoundException;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.resource.jdbc.spi.StatementInspector;

import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import org.assertj.core.api.Assertions;

import static org.assertj.core.api.Assertions.assertThat;
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
public class NotFoundExceptionManyToOneTest extends BaseCoreFunctionalTestCase {

	@Test
	public void testProxy() {
		inTransaction( (session) -> {
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
	@FailureExpected(
			jiraKey = "HHH-15060",
			message = "ObjectNotFoundException is thrown but caught and null is returned - see " +
					"org.hibernate.internal.SessionImpl.IdentifierLoadAccessImpl#doLoad"
	)
	public void testGet() {
		inTransaction( (session) -> {
			try {
				final Coin coin = session.get( Coin.class, 1 );
				coin.getCurrency().getName();
				fail( "Expecting ObjectNotFoundException for broken fk" );
			}
			catch (ObjectNotFoundException expected) {
				assertThat( expected.getEntityName() ).isEqualTo( Currency.class.getName() );
				assertThat( expected.getIdentifier() ).isEqualTo( 1 );
			}
		} );
	}

	@Test
	@FailureExpected(
			jiraKey = "HHH-15060",
			message = "EntityNotFoundException thrown rather than ObjectNotFoundException; " +
					"ObjectNotFoundException is thrown but caught and then converted to EntityNotFoundException"
	)
	public void testQueryImplicitPathDereferencePredicate() {
		statementInspector.clear();

		inTransaction( (session) -> {
			final String hql = "select c from Coin c where c.currency.id = 1";
			try {
				session.createQuery( hql, Coin.class ).getResultList();
				fail( "Expecting ObjectNotFoundException for broken fk" );
			}
			catch (ObjectNotFoundException expected) {
				assertThat( expected.getEntityName() ).isEqualTo( Currency.class.getName() );
				assertThat( expected.getIdentifier() ).isEqualTo( 1 );
			}
		} );
	}

	@Test
	@FailureExpected(
			jiraKey = "HHH-15060",
			message = "EntityNotFoundException thrown rather than ObjectNotFoundException; " +
					"ObjectNotFoundException is thrown but caught and then converted to EntityNotFoundException"
	)
	public void testQueryOwnerSelection() {
		statementInspector.clear();

		inTransaction( (session) -> {
			final String hql = "select c from Coin c";
			try {
				session.createQuery( hql, Coin.class ).getResultList();
				fail( "Expecting ObjectNotFoundException for broken fk" );
			}
			catch (ObjectNotFoundException expected) {
				assertThat( expected.getEntityName() ).isEqualTo( Currency.class.getName() );
				assertThat( expected.getIdentifier() ).isEqualTo( 1 );
			}
		} );
	}

	@Test
	@FailureExpected(
			jiraKey = "HHH-15060",
			message = "This one is somewhat debatable.  Is this selecting the association?  Or simply matching Currencies?"
	)
	public void testQueryAssociationSelection() {
		// NOTE: this one is not obvious
		//		- we are selecting the association so from that perspective, throwing the ObjectNotFoundException is nice
		//		- the other way to look at it is that there are simply no matching results, so nothing to return
		statementInspector.clear();

		inTransaction( (session) -> {
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

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] { Coin.class, Currency.class };
	}

	public static class CollectionStatementInspector implements StatementInspector {
		private List<String> queries = new ArrayList<>();

		@Override
		public String inspect(String sql) {
			queries.add( sql );
			return sql;
		}

		public void clear() {
			queries.clear();
		}

		public List<String> getQueries() {
			return queries;
		}
	}

	private final CollectionStatementInspector statementInspector = new CollectionStatementInspector();

	@Override
	protected void configure(Configuration configuration) {
		super.configure( configuration );
		configuration.getProperties().put( AvailableSettings.STATEMENT_INSPECTOR, statementInspector );
	}

	@Override
	protected void prepareTest() throws Exception {
		super.prepareTest();

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

	@Override
	protected void cleanupTest() throws Exception {
		inTransaction( (session) -> {
			session.createQuery( "delete Coin where id = 1" ).executeUpdate();
		} );

		super.cleanupTest();
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
