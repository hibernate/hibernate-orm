/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.formula;

import java.io.Serializable;
import java.util.List;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import org.hibernate.LazyInitializationException;
import org.hibernate.annotations.JoinColumnOrFormula;
import org.hibernate.annotations.JoinFormula;
import org.hibernate.annotations.processing.Exclude;
import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

@JiraKey(value = "HHH-12770")
@Exclude
public class JoinFormulaManyToOneLazyFetchingTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Stock.class,
				StockCode.class,
		};
	}

	@Override
	protected void afterEntityManagerFactoryBuilt() {
		doInJPA( this::entityManagerFactory, entityManager -> {
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
	public void testLazyLoading() {
		List<Stock> stocks = doInJPA( this::entityManagerFactory, entityManager -> {
			return entityManager.createQuery(
					"SELECT s FROM Stock s", Stock.class )
					.getResultList();
		} );
		assertEquals( 2, stocks.size() );

		try {
			assertEquals( "ABC", stocks.get( 0 ).getCode().getRefNumber() );

			fail( "Should have thrown LazyInitializationException" );
		}
		catch (LazyInitializationException expected) {

		}
	}

	@Test
	public void testEagerLoading() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			List<Stock> stocks =  entityManager.createQuery(
					"SELECT s FROM Stock s", Stock.class )
					.getResultList();

			assertEquals( 2, stocks.size() );
			assertEquals( "ABC", stocks.get( 0 ).getCode().getRefNumber() );

			// In 5.x, for some reason, we didn't understand that a component is a FK,
			// hence a partial null was wrongly handled, but in 6.0 we handle this in a unified way
			assertNull( stocks.get( 1 ).getCode() );
		} );
	}

	@Entity(name = "Stock")
	public static class Stock implements Serializable {

		@Id
		@Column(name = "ID")
		private Long id;

		@ManyToOne(fetch = FetchType.LAZY)
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
		TYPE_A, TYPE_B;
	}

}
