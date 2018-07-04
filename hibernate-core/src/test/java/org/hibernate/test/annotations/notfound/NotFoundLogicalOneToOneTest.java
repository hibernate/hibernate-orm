/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.notfound;

import java.io.Serializable;
import javax.persistence.ConstraintMode;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;

import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertNull;

/**
 * @author Emmanuel Bernard
 * @author Gail Badner
 */
public class NotFoundLogicalOneToOneTest extends BaseCoreFunctionalTestCase {
	@Test
	public void testLogicalOneToOne() throws Exception {
		Currency euro = new Currency();
		euro.setName( "Euro" );
		Coin fiveC = new Coin();
		fiveC.setName( "Five cents" );
		fiveC.setCurrency( euro );

		doInHibernate(
				this::sessionFactory, session -> {
					session.persist( euro );
					session.persist( fiveC );
				}
		);

		doInHibernate(
				this::sessionFactory, session -> {
					session.delete( euro );
				}
		);

		doInHibernate(
				this::sessionFactory, session -> {
					Coin coin = session.get( Coin.class, fiveC.getId() );
					assertNull( coin.getCurrency() );

					session.delete( coin );
				}
		);
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] { Coin.class, Currency.class };
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

		@OneToOne(fetch = FetchType.LAZY)
		@JoinColumn(foreignKey = @ForeignKey(value = ConstraintMode.NO_CONSTRAINT))
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
