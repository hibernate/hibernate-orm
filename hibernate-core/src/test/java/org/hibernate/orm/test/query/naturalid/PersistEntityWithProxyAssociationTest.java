package org.hibernate.orm.test.query.naturalid;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.NaturalId;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Jpa(
		annotatedClasses = {
				PersistEntityWithProxyAssociationTest.Currency.class,
				PersistEntityWithProxyAssociationTest.Position.class,
		}
)
@JiraKey("HHH-18147")
public class PersistEntityWithProxyAssociationTest {

	public static final long CURRENCY_ID = 1l;

	@BeforeEach
	public void setUp(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager ->
						entityManager.persist( new Currency( CURRENCY_ID, "USD" ) )
		);
	}

	@Test
	public void testPersistDoesNotThrowConstraintViolationException(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					// using getReference to obtain a Proxy
					Currency currency = entityManager.getReference( Currency.class, CURRENCY_ID );
					List<Holding> holdings = new ArrayList<>();
					holdings.add( new Holding( 20, currency ) );

					// Position#currency proxy has to be initialized in order to get its code
					// and insert it into POSITION_HOLDING table
					entityManager.persist( new Position( 10l, holdings ) );
				}
		);
	}

	@Entity
	@Table(name = "CURRENCY_TABLE")
	public static class Currency {
		@Id
		private Long id;

		@NaturalId
		private String code;

		public Currency() {
		}

		public Currency(Long id, String code) {
			this.id = id;
			this.code = code;
		}

		public String getCode() {
			return code;
		}
	}


	@Entity
	@Table(name = "POSITION_TABLE")
	public static class Position {
		@Id
		private Long id;

		private boolean active;

		@ElementCollection
		@JoinTable(name = "POSITION_HOLDING", joinColumns = { @JoinColumn(name = "POSITION_ID") })
		private List<Holding> holdings = new ArrayList<>();

		public Position() {
		}

		public Position(Long id) {
			this.id = id;
		}

		public Position(Long id, List<Holding> holdings) {
			this.id = id;
			this.holdings = holdings;
		}

		public Long getId() {
			return id;
		}

		public List<Holding> getHoldings() {
			return holdings;
		}

		public void addHolding(Holding holding) {
			holdings.add( holding );
		}
	}

	@Embeddable
	public static class Holding {
		@Column(nullable = false)
		private Integer quantity;

		@ManyToOne(optional = false, fetch = FetchType.LAZY)
		@JoinColumn(referencedColumnName = "code", nullable = false)
		private Currency currency;

		public Holding() {
		}

		public Holding(Integer quantity, Currency currency) {
			this.quantity = quantity;
			this.currency = currency;
		}

		public Integer getQuantity() {
			return quantity;
		}

		public Currency getCurrency() {
			return currency;
		}
	}

}
