/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.type;

import org.hibernate.Session;
import org.hibernate.query.Query;
import org.hibernate.resource.transaction.spi.TransactionStatus;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.fail;

import java.math.BigDecimal;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

/**
 * @author Ales Justin
 */
public class BigDecimalTypeTest extends BaseNonConfigCoreFunctionalTestCase {
	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {Account.class};
	}

	@Test
	public void testBigDecimalInSum() {
		final BigDecimal balance = new BigDecimal("1000000000.00000004");

		Session s = openSession();
		s.getTransaction().begin();
		try {
			Account account = new Account();
			account.id = 1L;
			account.balance = balance;

			s.persist( account );
			s.getTransaction().commit();
		}
		catch (Exception e) {
			if ( s.getTransaction() != null && s.getTransaction().getStatus() == TransactionStatus.ACTIVE ) {
				s.getTransaction().rollback();
			}
			fail( e.getMessage() );
		}
		finally {
			s.close();
		}

		s = openSession();
		s.getTransaction().begin();
		try {
			CriteriaBuilder b = s.getCriteriaBuilder();

			CriteriaQuery<BigDecimal> cq = b.createQuery(BigDecimal.class);
			Root<Account> account_ = cq.from(Account.class);

			cq.select(b.sum(account_.get("balance")));

			Query<BigDecimal> query = s.createQuery(cq);

			BigDecimal result = s.createQuery("SELECT SUM(a.balance) FROM Account a", BigDecimal.class).uniqueResult();
			Assert.assertEquals(0, balance.compareTo(result));

			result = query.uniqueResult();
			Assert.assertEquals(0, balance.compareTo(result));

			s.getTransaction().commit();
		}
		catch (Exception e) {
			if ( s.getTransaction() != null && s.getTransaction().getStatus() == TransactionStatus.ACTIVE ) {
				s.getTransaction().rollback();
			}
			fail( e.getMessage() );
		}
		finally {
			s.close();
		}
	}

	@Entity(name = "Account")
	public static class Account {

		@Id
		private Long id;

		@Basic(optional = false)
		@Column(precision = 20, scale = 8)
		private BigDecimal balance;
	}

	@Override
	protected boolean isCleanupTestDataRequired() {
		return true;
	}
}
