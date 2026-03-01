/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.dirty;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.hibernate.annotations.DynamicUpdate;

import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DomainModel(
		annotatedClasses = {
				DynamicUpdateTest.Payment.class,
				DynamicUpdateTest.StuffToPay.class
		}
)
@SessionFactory
@BytecodeEnhanced
public class DynamicUpdateTest {

	public static final Long STUFF_TO_PAY_ID = 1l;
	public static final Long PAYMENT_ID = 2l;

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Payment payment = new Payment(PAYMENT_ID);
					payment.setFee( new BigDecimal( 42 ) );
					payment.setTransactionNumber( "1234567890" );

					StuffToPay stuffToPay = new StuffToPay(STUFF_TO_PAY_ID);
					stuffToPay.setPayment( payment );
					session.persist( stuffToPay );
				}
		);
	}

	@Test
	@JiraKey("HHH-16577")
	public void testSetAttribute(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					StuffToPay stuffToPay = session.find( StuffToPay.class, STUFF_TO_PAY_ID );
					stuffToPay.confirmPayment();
					Payment payment = stuffToPay.getPayment();
					payment.getTransactionNumber();
					assertThat( payment.getPaidAt() ).isNotNull();				}
		);

		scope.inTransaction(
				session -> {
					Payment payment = session.find( Payment.class, PAYMENT_ID );
					assertThat(payment).isNotNull();
					assertThat(payment.getPaidAt()).isNotNull();
				}
		);
	}

	@Entity(name = "Payment")
	@DynamicUpdate
	public static class Payment {

		@Id
		private Long id;

		private BigDecimal fee;

		private LocalDate paidAt;

		private String transactionNumber;

		public Payment() {
		}

		public Payment(Long id) {
			this.id = id;
		}

		void confirmPayment(LocalDate when) {
			this.paidAt = when;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public BigDecimal getFee() {
			return fee;
		}

		public void setFee(BigDecimal fee) {
			this.fee = fee;
		}

		public LocalDate getPaidAt() {
			return paidAt;
		}

		public void setPaidAt(LocalDate paidAt) {
			this.paidAt = paidAt;
		}

		public String getTransactionNumber() {
			return transactionNumber;
		}

		public void setTransactionNumber(String transactionNumber) {
			this.transactionNumber = transactionNumber;
		}
	}

	@Entity(name = "StuffToPay")
	@DynamicUpdate
	public static class StuffToPay {

		@Id
		private Long id;

		private String description;

		@OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
		private Payment payment;

		public StuffToPay() {
		}

		public StuffToPay(Long id) {
			this.id = id;
		}

		public void confirmPayment() {
			payment.confirmPayment( LocalDate.now() );
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Payment getPayment() {
			return payment;
		}

		public void setPayment(Payment payment) {
			this.payment = payment;
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}
	}

}
