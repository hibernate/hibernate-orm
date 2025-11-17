/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.criteria.valuehandlingmode.inline;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import static org.junit.jupiter.api.Assertions.assertEquals;


@Jpa(
		annotatedClasses = DateCompositeCustomTypeTest.Payment.class,
		properties = @Setting(name = AvailableSettings.CRITERIA_VALUE_HANDLING_MODE, value = "inline")
)
public class DateCompositeCustomTypeTest {

	@Test
	public void testDateCompositeCustomType(EntityManagerFactoryScope scope) {
		final Date date = Date.from( Instant.now() );
		final Payment payment = new Payment();
		payment.setAmount( new BigDecimal( 1000 ) );
		payment.setDate( date );
		scope.inTransaction(
				entityManager -> {
					entityManager.persist( payment );

					CriteriaQuery<Payment> criteria = entityManager.getCriteriaBuilder().createQuery( Payment.class );
					Root<Payment> rp = criteria.from( Payment.class );
					Predicate predicate = entityManager.getCriteriaBuilder().equal( rp.get( "date" ), date );
					criteria.where( predicate );

					TypedQuery<Payment> q = entityManager.createQuery( criteria );
					List<Payment> payments = q.getResultList();

					assertEquals( 1, payments.size() );
				}
		);
	}

	@Entity
	@Table(name = "crit_basic_payment")
	public static class Payment {

		private Long id;
		private BigDecimal amount;
		private Date date;

		@Id
		@GeneratedValue
		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public BigDecimal getAmount() {
			return amount;
		}

		public void setAmount(BigDecimal amount) {
			this.amount = amount;
		}

		@Column(name = "payment_date")
		public Date getDate() {
			return date;
		}

		public void setDate(Date date) {
			this.date = date;
		}
	}

}
