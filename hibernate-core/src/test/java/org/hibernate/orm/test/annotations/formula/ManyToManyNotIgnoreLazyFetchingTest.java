/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.formula;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import org.hibernate.Hibernate;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.JiraKeyGroup;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@JiraKeyGroup(value = {
		@JiraKey(value = "HHH-12770"),
		@JiraKey(value = "HHH-15545")
})
@Jpa(
		annotatedClasses = {
				ManyToManyNotIgnoreLazyFetchingTest.Stock.class,
				ManyToManyNotIgnoreLazyFetchingTest.StockCode.class,
		}
)
public class ManyToManyNotIgnoreLazyFetchingTest {

	@BeforeAll
	public void setUp(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			StockCode code = new StockCode();
			code.setId( 1L );
			code.setCopeType( CodeType.TYPE_A );
			code.setRefNumber( "ABC" );
			entityManager.persist( code );

			Stock stock1 = new Stock();
			stock1.setId( 1L );
			stock1.getCodes().add( code );
			entityManager.persist( stock1 );

			Stock stock2 = new Stock();
			stock2.setId( 2L );
			entityManager.persist( stock2 );
			entityManager.flush();

			entityManager.remove( code );
			stock1.getCodes().remove( code );
		} );
	}

	@Test
	public void testLazyLoading(EntityManagerFactoryScope scope) {
		List<Stock> stocks = scope.fromTransaction(
				entityManager -> {
					List<Stock> list = entityManager.createQuery( "select s from Stock s order by id", Stock.class )
							.getResultList();
					for ( Stock s : list ) {
						assertThat( Hibernate.isInitialized( s.getCodes() ) ).isFalse();
						Hibernate.initialize( s.getCodes() );
					}
					return list;
				} );

		assertThat( stocks ).hasSize( 2 );

		final Stock firstStock = stocks.get( 0 );
		final Stock secondStock = stocks.get( 1 );

		assertThat( firstStock.getCodes() ).hasSize( 0 );
		assertThat( secondStock.getCodes() ).hasSize( 0 );
	}

	@Test
	public void testEagerLoading(EntityManagerFactoryScope scope) {
		List<Stock> stocks = scope.fromTransaction(
				entityManager -> entityManager
						.createQuery( "select s from Stock s left join fetch s.codes order by s.id", Stock.class )
						.getResultList()
		);

		assertThat( stocks ).hasSize( 2 );

		for ( Stock s : stocks ) {
			assertThat( Hibernate.isInitialized( s.getCodes() ) ).isTrue();
		}

		final Stock firstStock = stocks.get( 0 );
		final Stock secondStock = stocks.get( 1 );

		assertThat( firstStock.getCodes() ).hasSize( 0 );
		assertThat( secondStock.getCodes() ).hasSize( 0 );
	}

	@Entity(name = "Stock")
	public static class Stock implements Serializable {

		@Id
		@Column(name = "ID")
		private Long id;

		@ManyToMany
		@NotFound(action = NotFoundAction.IGNORE)
		@JoinTable(name = "STOCK_BY_CODE",
				joinColumns = @JoinColumn(name = "STOCK_ID", referencedColumnName = "ID"),
				inverseJoinColumns = {
						@JoinColumn(name = "CODE_ID", referencedColumnName = "ID"),
						@JoinColumn(name = "CODE_TYPE", referencedColumnName = "TYPE")
				})
		private List<StockCode> codes = new ArrayList<>();

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public List<StockCode> getCodes() {
			return codes;
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
