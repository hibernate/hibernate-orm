package org.hibernate.orm.test.annotations.naturalid;

import java.math.BigDecimal;

import org.hibernate.annotations.NaturalId;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@DomainModel(
		annotatedClasses = {
				NaturalIdAndAssociationTest.PositionEntity.class,
				NaturalIdAndAssociationTest.ZCurrencyEntity1.class
		})
@SessionFactory
@Jira("HHH-18338")
public class NaturalIdAndAssociationTest {

	@Test
	public void testPersist(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					ZCurrencyEntity1 currency = new ZCurrencyEntity1( 1l, "USD" );
					Amount amount = new Amount( new BigDecimal( "100.00" ), currency );
					Holding holding = new Holding( amount );
					PositionEntity positionEntity = new PositionEntity( 1l, "1", holding );

					session.persist( currency );
					session.persist( positionEntity );
				}
		);
	}

	@Entity(name = "PositionEntity")
	@Table(name = "POSITION_TABLE")
	public static class PositionEntity {
		@Id
		private Long id;

		private String name;

		@Embedded
		private Holding holding;

		public PositionEntity() {
		}

		public PositionEntity(Long id, String name, Holding holding) {
			this.id = id;
			this.name = name;
			this.holding = holding;
		}
	}

	@Embeddable
	public static class Holding {
		@Embedded
		private Amount amount;

		public Holding() {
		}

		public Holding(Amount amount) {
			this.amount = amount;
		}
	}

	@Embeddable
	public static class Amount {

		private BigDecimal quantity;

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(referencedColumnName = "isoCode", nullable = false)
		private ZCurrencyEntity1 currency;

		public Amount() {
		}

		public Amount(BigDecimal quantity, ZCurrencyEntity1 currency) {
			this.quantity = quantity;
			this.currency = currency;
		}

	}

	@Entity(name = "ZCurrencyEntity")
	@Table(name = "CURRENCY")
	public static class ZCurrencyEntity1 {
		@Id
		private Long id;

		@NaturalId
		private String isoCode;

		public ZCurrencyEntity1() {
		}

		public ZCurrencyEntity1(Long id, String isoCode) {
			this.id = id;
			this.isoCode = isoCode;
		}
	}

}
