/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.notfound;

import java.io.Serializable;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertNull;

/**
 * @author Emmanuel Bernard
 */
@RequiresDialectFeature(value = DialectChecks.SupportsIdentityColumns.class)
public class NotFoundTest extends BaseCoreFunctionalTestCase {

	@Test
	public void testManyToOne() throws Exception {
		final Currency euro = new Currency();
		euro.setName( "Euro" );

		final Coin fiveCents = new Coin();
		fiveCents.setName( "Five cents" );
		fiveCents.setCurrency( euro );

		doInHibernate( this::sessionFactory, session -> {
			session.persist( euro );
			session.persist( fiveCents );
		} );

		doInHibernate( this::sessionFactory, session -> {
			Currency _euro = session.get( Currency.class, euro.getId() );
			session.delete( _euro );
		} );

		doInHibernate( this::sessionFactory, session -> {
			Coin _fiveCents = session.get( Coin.class, fiveCents.getId() );
			assertNull( _fiveCents.getCurrency() );
			session.delete( _fiveCents );
		} );
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {Coin.class, Currency.class};
	}

	@Entity(name = "Coin")
	public static class Coin {

		private Integer id;

		private String name;

		private Currency currency;

		@Id
		@GeneratedValue
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

		@ManyToOne
		@JoinColumn(name = "currency", referencedColumnName = "name")
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

		@Id
		@GeneratedValue
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
