/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.any.annotations;

import java.math.BigDecimal;

import org.hibernate.annotations.Any;
import org.hibernate.annotations.AnyDiscriminator;
import org.hibernate.annotations.AnyDiscriminatorValue;
import org.hibernate.annotations.AnyKeyJavaClass;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DomainModel(
		annotatedClasses = {
				AnyMergeTest.InvoicePosition.class,
				AnyMergeTest.Bonus.class,
				AnyMergeTest.Fee.class,
				AnyMergeTest.Amount.class
		}
)
@SessionFactory
@JiraKey("HHH-17621")
public class AnyMergeTest {


	@Test
	public void testMerge(SessionFactoryScope scope) {
		Amount amount = new Amount( new BigDecimal( 10 ) );
		Bonus bonus = new Bonus( "that's a bonus", amount );
		scope.inTransaction(
				session -> {
					session.persist( amount );
					session.persist( bonus );
				}
		);

		scope.inTransaction(
				session -> {
					InvoicePosition invoicePosition = new InvoicePosition();
					invoicePosition.setReference( bonus );
					InvoicePosition merged = session.merge( invoicePosition );

					Reference mergedReference = merged.getReference();
					assertThat( mergedReference ).isExactlyInstanceOf( Bonus.class );
					Bonus mergedBonus = (Bonus) mergedReference;
					// check the merged values are copies of the original ones
					assertThat( mergedBonus ).isNotEqualTo( bonus );
					assertThat( mergedBonus.amount ).isNotEqualTo( amount );

					assertThat( mergedBonus.amount.quantity.compareTo( new BigDecimal( 10 ) ) ).isEqualTo( 0 );
				}
		);
	}

	public interface Reference {
	}

	@Entity(name = "Bonus")
	public static class Bonus implements Reference {

		@Id
		@GeneratedValue
		private Long id;

		private String name;

		@OneToOne
		private Amount amount;

		public Bonus() {
		}

		public Bonus(String name, Amount amount) {
			this.name = name;
			this.amount = amount;
		}
	}

	@Entity(name = "Amount")
	public static class Amount {

		@Id
		@GeneratedValue
		private Long id;

		private BigDecimal quantity;

		public Amount() {
		}

		public Amount(BigDecimal quantity) {
			this.quantity = quantity;
		}
	}

	@Entity(name = "Fee")
	public static class Fee implements Reference {

		@Id
		@GeneratedValue
		private Long id;

		private String name;
	}

	@Entity(name = "InvoicePosition")
	public static class InvoicePosition {

		@Id
		@GeneratedValue
		private Long id;

		@Any
		@AnyDiscriminator(DiscriminatorType.STRING)
		@AnyDiscriminatorValue(discriminator = "BONUS", entity = Bonus.class)
		@AnyDiscriminatorValue(discriminator = "FEE", entity = Fee.class)
		@AnyKeyJavaClass(Long.class)
		@Column(name = "Type")
		@JoinColumn(name = "ReferenceId")
		private Reference reference;

		public Reference getReference() {
			return reference;
		}

		public void setReference(Reference reference) {
			this.reference = reference;
		}
	}


}
