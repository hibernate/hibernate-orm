/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.type;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.hibernate.query.Query;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Ales Justin
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(annotatedClasses = BigDecimalTypeTest.Account.class)
@SessionFactory
public class BigDecimalTypeTest {
	@AfterEach
	void dropTestData(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	public void testBigDecimalInSum(SessionFactoryScope factoryScope) {
		final BigDecimal balance = new BigDecimal("1000000000.00000004");

		factoryScope.inTransaction( (session) -> {
			Account account = new Account();
			account.id = 1L;
			account.balance = balance;

			session.persist( account );
		} );

		factoryScope.inTransaction( (s) -> {
			CriteriaBuilder b = s.getCriteriaBuilder();

			CriteriaQuery<BigDecimal> cq = b.createQuery(BigDecimal.class);
			Root<Account> account_ = cq.from(Account.class);

			cq.select(b.sum(account_.get("balance")));

			Query<BigDecimal> query = s.createQuery(cq);

			BigDecimal result = s.createQuery("SELECT SUM(a.balance) FROM Account a", BigDecimal.class).uniqueResult();
			assertEquals(0, balance.compareTo(result));

			result = query.uniqueResult();
			assertEquals(0, balance.compareTo(result));
		} );
	}

	@Entity(name = "Account")
	public static class Account {

		@Id
		private Long id;

		@Basic(optional = false)
		@Column(precision = 20, scale = 8)
		private BigDecimal balance;
	}
}
