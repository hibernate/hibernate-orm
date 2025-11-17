/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.formula;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import org.hibernate.annotations.JoinColumnOrFormula;
import org.hibernate.annotations.JoinFormula;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.annotations.processing.Exclude;
import org.hibernate.testing.logger.Triggerable;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.logger.LoggerInspectionExtension;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.Serializable;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.boot.BootLogging.BOOT_LOGGER;

@JiraKey(value = "HHH-12770")
@Exclude
@Jpa(
		annotatedClasses = {
				JoinFormulaOneToOneNotIgnoreLazyFetchingTest.Stock.class,
				JoinFormulaOneToOneNotIgnoreLazyFetchingTest.StockCode.class,
		}
)
public class JoinFormulaOneToOneNotIgnoreLazyFetchingTest {
	private Triggerable triggerable;

	@RegisterExtension
	public LoggerInspectionExtension logger =
			LoggerInspectionExtension.builder().setLogger( BOOT_LOGGER ).build();

	@BeforeAll
	public void beforeAll(EntityManagerFactoryScope scope) {
		triggerable = logger.watchForLogMessages( "HHH160133" );
		triggerable.reset();

		scope.inTransaction( entityManager -> {
			StockCode code = new StockCode();
			code.setId( 1L );
			code.setCopeType( CodeType.TYPE_A );
			code.setRefNumber( "ABC" );
			entityManager.persist( code );

			Stock stock1 = new Stock();
			stock1.setId( 1L );
			stock1.setCode( code );
			entityManager.persist( stock1 );

			Stock stock2 = new Stock();
			stock2.setId( 2L );
			entityManager.persist( stock2 );
		} );
	}

	@Test
	public void testLazyLoading(EntityManagerFactoryScope scope) {
		assertThat( triggerable.wasTriggered() )
				.describedAs( "Expecting WARN message to be logged" )
				.isTrue();

		List<Stock> stocks = scope.fromTransaction( entityManager ->
				entityManager.createQuery( "SELECT s FROM Stock s", Stock.class ).getResultList()
		);

		assertThat( stocks.size() ).isEqualTo( 2 );

		assertThat( stocks.get( 0 ).getCode().getRefNumber() ).isEqualTo( "ABC" );
		assertThat( stocks.get( 1 ).getCode() ).isNull();
	}

	@Entity(name = "Stock")
	public static class Stock implements Serializable {

		@Id
		@Column(name = "ID")
		private Long id;

		@OneToOne(fetch = FetchType.LAZY)
		@NotFound(action = NotFoundAction.IGNORE)
		@JoinColumnOrFormula(column = @JoinColumn(name = "CODE_ID", referencedColumnName = "ID"))
		@JoinColumnOrFormula(formula = @JoinFormula(referencedColumnName = "TYPE", value = "'TYPE_A'"))
		private StockCode code;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public StockCode getCode() {
			return code;
		}

		public void setCode(StockCode code) {
			this.code = code;
		}
	}

	@Entity(name = "StockCode")
	public static class StockCode implements Serializable {

		@Id
		@Column(name = "ID")
		private Long id;

		@Id
		@Enumerated(EnumType.STRING)
		@Column(name = "TYPE")
		private CodeType copeType;

		private String refNumber;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public CodeType getCopeType() {
			return copeType;
		}

		public void setCopeType(CodeType copeType) {
			this.copeType = copeType;
		}

		public String getRefNumber() {
			return refNumber;
		}

		public void setRefNumber(String refNumber) {
			this.refNumber = refNumber;
		}
	}

	public enum CodeType {
		TYPE_A, TYPE_B
	}

}
