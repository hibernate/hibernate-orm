/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.formula;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.Session;
import org.hibernate.annotations.Formula;

import org.junit.Test;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;

/**
 * @author Andrea Boriero
 */
public class CastInFormulaTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {CentsAndDollarsAccount.class};
	}

	@Test
	public void testRetrievingEntity() throws Exception {
		Session s = openSession();
		s.getTransaction().begin();

		CentsAndDollarsAccount centsAndDollarsAccount = new CentsAndDollarsAccount();
		centsAndDollarsAccount.setCents( 1245L );
		s.persist( centsAndDollarsAccount );

		s.getTransaction().commit();
		s.clear();

		centsAndDollarsAccount = s.get( CentsAndDollarsAccount.class, centsAndDollarsAccount.getId() );
		final Double dollars = centsAndDollarsAccount.getDollars();
		assertEquals( 12.45d, dollars, 0.01 );

		final long nickels = centsAndDollarsAccount.getNickel();
		assertEquals( 249L, nickels );

		s.close();
	}

	@Entity(name = "CentsAndDollarsAccount")
	public static class CentsAndDollarsAccount {

		@Id
		@GeneratedValue
		private Long id;

		private Long cents;

		@Formula("cast(cents as double) / 100")
		private Double dollars;

		@Formula("cast(cents as number(10,4)) / 5")
		private Long nickel;


		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Long getCents() {
			return cents;
		}

		public void setCents(Long cents) {
			this.cents = cents;
		}

		public Double getDollars() {
			return dollars;
		}

		public Long getNickel() {
			return nickel;
		}
	}
}
