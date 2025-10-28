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
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.JiraKeyGroup;
import org.junit.Test;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.hibernate.testing.transaction.TransactionUtil2.fromTransaction;
import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@JiraKeyGroup( value = {
				@JiraKey( value = "HHH-12770" ),
				@JiraKey( value = "HHH-15545" )
} )
public class ManyToManyNotIgnoreLazyFetchingTest extends BaseEntityManagerFunctionalTestCase {

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
			stock1.getCodes().add( code );
			entityManager.persist( stock1 );

			Stock stock2 = new Stock();
			stock2.setId( 2L );
			entityManager.persist( stock2 );
			entityManager.flush();

			entityManager.remove(code);
			stock1.getCodes().remove( code );
		} );
	}

	@Test
	public void testLazyLoading() {
		List<Stock> stocks = fromTransaction( entityManagerFactory().unwrap( SessionFactoryImplementor.class ), session -> {
			List<Stock> list = session.createQuery("select s from Stock s order by id", Stock.class).getResultList();
			for (Stock s: list) {
				assertFalse( Hibernate.isInitialized( s.getCodes() ) );
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
	public void testEagerLoading() {
		List<Stock> stocks = fromTransaction( entityManagerFactory().unwrap( SessionFactoryImplementor.class ),
				session -> session.createQuery("select s from Stock s left join fetch s.codes order by s.id", Stock.class)
						.getResultList()
		);

		assertThat( stocks ).hasSize( 2 );

		for (Stock s: stocks) {
			assertTrue( Hibernate.isInitialized( s.getCodes() ) );
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
		TYPE_A, TYPE_B;
	}

}
