/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.notfound.ignore;

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
 * Tests for `@ManyToOne @NotFound(IGNORE)`
 *
 * NOTES:<ol>
 *     <li>`@NotFound` should force the association to be eager - `Coin#currency` should be loaded immediately</li>
 *     <li>`IGNORE` says to treat the broken fk as null</li>
 * </ol>
 *
 * @author Steve Ebersole
 */
public class NotFoundIgnoreManyToOneTest extends BaseCoreFunctionalTestCase {

	@Test
	public void testProxy() {
		inTransaction( (session) -> {
			// the non-existent Child
			// 	- this is the one valid deviation from treating the broken fk as null
			try {
				final Currency proxy = session.byId( Currency.class ).getReference( 1 );
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
	public void testGet() {
		inTransaction( (session) -> {
			final Coin coin = session.get( Coin.class, 1 );
			assertThat( coin.getCurrency() ).isNull();
		} );
	}

	@Test
	@FailureExpected(
			jiraKey = "HHH-15060",
			message = "Bad results due to cross-join"
	)
	public void testQueryImplicitPathDereferencePredicate() {
		inTransaction( (session) -> {
			final String hql = "select c from Coin c where c.currency.id = 1";
			final List<Coin> coins = session.createQuery( hql, Coin.class ).getResultList();
			assertThat( coins ).hasSize( 1 );
			assertThat( coins.get( 0 ).getCurrency() ).isNull();
		} );
	}

	@Test
	public void testQueryOwnerSelection() {
		inTransaction( (session) -> {
			final String hql = "select c from Coin c";
			final List<Coin> coins = session.createQuery( hql, Coin.class ).getResultList();
			assertThat( coins ).hasSize( 1 );
			assertThat( coins.get( 0 ).getCurrency() ).isNull();
		} );
	}

	@Test
	@FailureExpected(
			jiraKey = "HHH-15060",
			message = "Has zero results because of inner-join - the select w/ inner-join is executed twice for some odd reason"
	)
	public void testQueryAssociationSelection() {
		inTransaction( (session) -> {
			final String hql = "select c.currency from Coin c";
			session.createQuery( hql, Currency.class ).getResultList();
			final List<Currency> currencies = session.createQuery( hql, Currency.class ).getResultList();
			assertThat( currencies ).hasSize( 1 );
			assertThat( currencies.get( 0 ) ).isNull();
		} );
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] { Coin.class, Currency.class };
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
