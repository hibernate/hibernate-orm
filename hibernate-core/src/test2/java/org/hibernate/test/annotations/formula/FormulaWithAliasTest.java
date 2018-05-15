/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.formula;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.annotations.Formula;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.query.Query;
import org.hibernate.test.annotations.formula.FormulaWithColumnTypesTest.ExtendedDialect;
import org.hibernate.test.locking.A;
import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

/**
 * @author Yanming Zhou*
 */
@RequiresDialect(H2Dialect.class)
public class FormulaWithAliasTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Customer.class };
	}

	@Override
	protected void configure(Configuration configuration) {
		configuration.setProperty(
				Environment.DIALECT,
				ExtendedDialect.class.getName()
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-12280")
	public void testFormulaWithAlias() throws Exception {
		doInHibernate( this::sessionFactory, session -> {
			Customer company1 = new Customer();
			company1.setBalance(new BigDecimal(100));
			company1.setVip(true);
			session.persist(company1);

			Customer company2 = new Customer();
			company2.setBalance(new BigDecimal(1000));
			company2.setVip(false);
			session.persist(company2);
		} );

		doInHibernate( this::sessionFactory, session -> {
			List<Customer> customers = session.createQuery(
				"select c " +
				"from Customer c ", Customer.class)
			.getResultList();

			assertEquals(2, customers.size());
			assertEquals(1d, customers.get(0).getPercentage().doubleValue(), 0);
			assertEquals(1d, customers.get(1).getPercentage().doubleValue(), 0);
		} );
	}
	
	@Entity(name = "Customer")
	public static class Customer implements Serializable{

		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		private Long id;

		private BigDecimal balance;

		@Formula("balance/(select sum(c.balance) from Customer c where c.vip = {alias}.vip)")
		private BigDecimal percentage;

		private boolean vip;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public BigDecimal getBalance() {
			return balance;
		}

		public void setBalance(BigDecimal balance) {
			this.balance = balance;
		}

		public BigDecimal getPercentage() {
			return percentage;
		}

		public void setPercentage(BigDecimal percentage) {
			this.percentage = percentage;
		}

		public boolean isVip() {
			return vip;
		}

		public void setVip(boolean vip) {
			this.vip = vip;
		}

	}

}
