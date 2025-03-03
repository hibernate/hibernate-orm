/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.collections;

import java.util.Currency;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MapKeyColumn;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = {
		ElementCollectionMapUpdateTest.Company.class,
		ElementCollectionMapUpdateTest.MarketData.class,
		ElementCollectionMapUpdateTest.Amount.class,
} )
@SessionFactory
@Jira( "https://hibernate.atlassian.net/browse/HHH-17334" )
public class ElementCollectionMapUpdateTest {
	private static final String NAME = "S&P500";
	private static final Currency USD = Currency.getInstance( "USD" );

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Company parent = new Company( 1L );
			parent.add( new MarketData( NAME, new Amount( 1d, USD ), new Amount( 1_000d, USD ) ) );
			session.persist( parent );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.createMutationQuery( "delete from Company" ).executeUpdate() );
	}

	@Test
	public void testElementCollectionUpdate(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Company company = session.find( Company.class, 1L );
			company.add( new MarketData( NAME, new Amount( 1d, USD ), new Amount( 1_000d, USD ) ) );
		} );
		scope.inTransaction( session -> {
			final Company company = session.find( Company.class, 1L );
			assertThat( company.getData() ).hasSize( 1 );
			assertThat( company.getData() ).containsKey( NAME );
			assertThat( company.getData().get( NAME ).getPrice().getValue() ).isEqualTo( 1d );
			assertThat( company.getData().get( NAME ).getCapitalization().getValue() ).isEqualTo( 1_000d );
		} );
	}

	@Entity( name = "Company" )
	public static class Company {
		@Id
		private Long id;

		@ElementCollection
		@MapKeyColumn( name = "name", insertable = false, updatable = false )
		private final Map<String, MarketData> data = new HashMap<>();

		public Company() {
		}

		public Company(Long id) {
			this.id = id;
		}

		public void add(MarketData data) {
			this.data.put( data.getName(), data );
		}

		public Map<String, MarketData> getData() {
			return data;
		}
	}

	@Embeddable
	public static class MarketData {
		private String name;

		@AttributeOverride( name = "value", column = @Column( name = "price" ) )
		private Amount price;

		@AttributeOverrides( {
				@AttributeOverride( name = "value", column = @Column( name = "capitalization" ) ),
				@AttributeOverride( name = "currency", column = @Column( name = "currency", insertable = false, updatable = false ) ),
		} )
		private Amount capitalization;

		protected MarketData() {
		}

		public MarketData(String name, Amount price, Amount capitalization) {
			this.name = name;
			this.price = price;
			this.capitalization = capitalization;
		}

		public String getName() {
			return name;
		}

		public Amount getPrice() {
			return price;
		}

		public Amount getCapitalization() {
			return capitalization;
		}
	}

	@Embeddable
	public static class Amount {
		private Double value;
		private Currency currency;

		protected Amount() {
		}

		public Amount(Double value, Currency currency) {
			this.value = value;
			this.currency = currency;
		}

		public Double getValue() {
			return value;
		}

		public Currency getCurrency() {
			return currency;
		}
	}
}
